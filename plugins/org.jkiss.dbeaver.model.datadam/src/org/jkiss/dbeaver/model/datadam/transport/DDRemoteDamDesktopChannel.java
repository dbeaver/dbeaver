/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.datadam.transport;

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamProtocol;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamHandshakeRequest;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamMessage;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannel;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamMessageListener;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSessionListener;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.BaseWebSocketClient;
import org.jkiss.utils.HttpConstants;

import java.net.URI;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class DDRemoteDamDesktopChannel implements DDRemoteDamChannel {

    private static final Log log = Log.getLog(DDRemoteDamDesktopChannel.class);

    private static final int MAX_MESSAGE_BYTES = 65_536;
    private static final int MAX_PENDING_MESSAGES = 64;
    private static final long CONNECT_TIMEOUT_MILLIS = 30_000;
    private static final long READY_TIMEOUT_MILLIS = 10_000;
    private static final long SEND_TIMEOUT_MILLIS = 10_000;
    private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(90);
    private static final long MIN_RETRY_MILLIS = 1_000;
    private static final long MAX_RETRY_MILLIS = 60_000;

    private final Object lock = new Object();
    private final URI endpoint;
    private final DDRemoteDamHandshakeRequest hello;
    private final String helloMessage;
    private final TokenProvider tokens;
    private final Supplier<BaseWebSocketClient> clients;
    private final List<Registration<DDRemoteDamMessageListener>> messageListeners = new ArrayList<>();
    private final List<Registration<DDRemoteDamSessionListener>> sessionListeners = new ArrayList<>();
    private final List<StateObservation> stateObservers = new ArrayList<>();
    private final ChannelJob delivery = new ChannelJob("Deliver Desktop messages", this::deliver);
    @Nullable
    private PendingDisconnect pendingDisconnect;
    private DDRemoteDamChannelStatus status = new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null);
    private boolean running;
    private boolean closed;
    private boolean deliveryScheduled;
    private long retryMillis = MIN_RETRY_MILLIS;

    private Attempt current;

    @FunctionalInterface
    public interface TokenProvider {
        @Nullable
        String getAccessToken(@NotNull URI resource) throws DBException;
    }

    public DDRemoteDamDesktopChannel(@NotNull URI endpoint, @NotNull DDRemoteDamHandshakeRequest hello, @NotNull TokenProvider tokens)
            throws DBException {
        this(endpoint, hello, tokens, BaseWebSocketClient::new);
    }

    public DDRemoteDamDesktopChannel(
        @NotNull URI endpoint,
        @NotNull DDRemoteDamHandshakeRequest hello,
        @NotNull TokenProvider tokens,
        @NotNull Supplier<BaseWebSocketClient> clients
    ) throws DBException {
        DDEndpoints.requireSecureWebSocketTransport(endpoint);
        this.endpoint = endpoint;
        this.hello = hello;
        this.helloMessage = DDRemoteDamProtocol.encodeHandshakeRequest(hello);
        this.tokens = tokens;
        this.clients = clients;
        if (helloMessage.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) {
            throw new DBException("Desktop hello exceeds the message size limit");
        }
    }

    public void start() {
        synchronized (lock) {
            if (closed || running) {
                return;
            }
            running = true;
            scheduleAttempt(0);
        }
        notifyStateObservers();
    }

    public void pause() {
        stop(false);
    }

    @Override
    public void close() {
        stop(true);
    }

    private void stop(boolean terminal) {
        Attempt stopped;
        synchronized (lock) {
            running = false;
            closed |= terminal;
            stopped = current;
            if (stopped != null) {
                detach(stopped, false);
            }
            updateStatus(new DDRemoteDamChannelStatus(
                closed ? DDRemoteDamChannelState.CLOSED : DDRemoteDamChannelState.DISCONNECTED, null));
            if (terminal) {
                messageListeners.clear();
                sessionListeners.clear();
            }
        }
        if (stopped != null) {
            cleanup(stopped);
        }
        notifyStateObservers();
    }

    @Override
    @NotNull
    public DDRemoteDamChannelStatus getStatus() {
        synchronized (lock) {
            return status;
        }
    }

    @Override
    @NotNull
    public DDRemoteDamSubscription observeState(@NotNull Consumer<DDRemoteDamChannelStatus> observer) {
        StateObservation observation = new StateObservation(observer);
        synchronized (lock) {
            observation.pending.add(status);
            if (!closed) {
                stateObservers.add(observation);
            }
        }
        observation.deliverPendingStates();
        return observation;
    }

    private void updateStatus(@NotNull DDRemoteDamChannelStatus next) {
        if (status.state() != DDRemoteDamChannelState.CLOSED && !status.equals(next)) {
            status = next;
            for (StateObservation observation : stateObservers) {
                observation.pending.add(next);
            }
        }
    }

    private void notifyStateObservers() {
        List<StateObservation> observers;
        synchronized (lock) {
            observers = List.copyOf(stateObservers);
        }
        for (StateObservation observation : observers) {
            observation.deliverPendingStates();
        }
    }

    @Override
    @NotNull
    public CompletionStage<Void> send(@NotNull DDRemoteDamMessage message) {
        String encoded = DDRemoteDamProtocol.encode(message);
        if (encoded.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) {
            return failed("Desktop message exceeds the message size limit");
        }
        Outgoing outgoing = new Outgoing(encoded, new CompletableFuture<>());
        String rejection = null;
        synchronized (lock) {
            Attempt attempt = current;
            if (attempt == null || attempt.session == null) {
                rejection = "Desktop message channel is not connected";
            } else if (attempt.outgoing.size() + (attempt.sending == null ? 0 : 1) >= MAX_PENDING_MESSAGES) {
                rejection = "Desktop message send queue is full";
            } else {
                attempt.outgoing.add(outgoing);
                wakeSender(attempt);
            }
        }
        return rejection == null ? outgoing.result : failed(rejection);
    }

    @NotNull
    private static CompletionStage<Void> failed(@NotNull String message) {
        return CompletableFuture.failedFuture(new DBException(message));
    }

    @Override
    @NotNull
    public DDRemoteDamSubscription subscribe(@NotNull String topic, @NotNull DDRemoteDamMessageListener listener) {
        synchronized (lock) {
            var registration = new Registration<>(topic, listener, messageListeners);
            if (!closed) {
                messageListeners.add(registration);
            }
            return registration;
        }
    }

    @Override
    @NotNull
    public DDRemoteDamSubscription addSessionListener(@NotNull DDRemoteDamSessionListener listener) {
        synchronized (lock) {
            var registration = new Registration<>(null, listener, sessionListeners);
            if (!closed) {
                sessionListeners.add(registration);
            }
            return registration;
        }
    }

    private void scheduleAttempt(long delay) {
        Attempt attempt = new Attempt();
        current = attempt;
        updateStatus(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.CONNECTING, null));
        attempt.connect.schedule(delay);
    }

    private boolean isCurrent(@NotNull Attempt attempt) {
        synchronized (lock) {
            return current == attempt;
        }
    }

    private void connect(@NotNull Attempt attempt) {
        synchronized (lock) {
            if (current != attempt) {
                return;
            }
            attempt.connectDeadline.schedule(CONNECT_TIMEOUT_MILLIS);
        }
        try {
            String token = tokens.getAccessToken(endpoint);
            if (token == null || token.isBlank()) {
                finish(attempt, false);
                return;
            }
            if (!isCurrent(attempt)) {
                return;
            }
            BaseWebSocketClient client = clients.get();
            boolean accepted;
            synchronized (lock) {
                accepted = current == attempt;
                if (accepted) {
                    attempt.client = client;
                }
            }
            if (!accepted) {
                client.close();
                return;
            }
            WebSocket socket = client.connect(endpoint,
                Map.of(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + token), attempt.listener);
            if (!isCurrent(attempt)) {
                socket.abort();
            }
        } catch (DBException e) {
            finish(attempt, true);
        } catch (InterruptedException e) {
            finish(attempt, true);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            finish(attempt, !isAuthorizationFailure(e));
        }
    }

    private static boolean isAuthorizationFailure(@NotNull Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof WebSocketHandshakeException handshake) {
                int status = handshake.getResponse().statusCode();
                return status == 401 || status == 403;
            }
        }
        return false;
    }

    private void opened(@NotNull Attempt attempt, @NotNull WebSocket socket) {
        boolean accepted;
        synchronized (lock) {
            accepted = current == attempt;
            if (accepted) {
                attempt.socket = socket;
                attempt.outgoing.add(new Outgoing(helloMessage, new CompletableFuture<>()));
                attempt.readyDeadline.schedule(READY_TIMEOUT_MILLIS);
                wakeSender(attempt);
            }
        }
        if (!accepted) {
            attempt.listener.dispose(socket, false);
        } else {
            attempt.connectDeadline.cancel();
        }
    }

    private void wakeSender(@NotNull Attempt attempt) {
        if (!attempt.senderScheduled) {
            attempt.senderScheduled = true;
            attempt.sender.schedule();
        }
    }

    private void sendPending(@NotNull Attempt attempt) {
        while (true) {
            Outgoing outgoing;
            WebSocket socket;
            synchronized (lock) {
                if (current != attempt || attempt.outgoing.isEmpty()) {
                    attempt.senderScheduled = false;
                    return;
                }
                outgoing = attempt.outgoing.removeFirst();
                attempt.sending = outgoing;
                socket = attempt.socket;
            }
            try {
                if (socket == null) {
                    throw new DBException("Desktop socket is unavailable");
                }
                socket.sendText(outgoing.text, true).get(SEND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                finish(attempt, true);
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                finish(attempt, true);
                return;
            }
            boolean accepted;
            synchronized (lock) {
                accepted = current == attempt;
                if (accepted) {
                    attempt.sending = null;
                }
            }
            if (accepted) {
                outgoing.result.complete(null);
            }
        }
    }

    private void received(@NotNull Attempt attempt, @NotNull String text) {
        boolean overflow;
        synchronized (lock) {
            if (current != attempt) {
                return;
            }
            overflow = attempt.incoming.size() >= MAX_PENDING_MESSAGES;
            if (!overflow) {
                attempt.incoming.add(text);
                wakeDelivery();
            }
        }
        if (overflow) {
            finish(attempt, true);
        }
    }

    private void wakeDelivery() {
        if (!deliveryScheduled) {
            deliveryScheduled = true;
            delivery.schedule();
        }
    }

    private void deliver() {
        while (true) {
            Attempt attempt;
            String text;
            PendingDisconnect disconnected;
            synchronized (lock) {
                disconnected = pendingDisconnect;
                pendingDisconnect = null;
                attempt = current;
                text = disconnected == null && attempt != null ? attempt.incoming.pollFirst() : null;
                if (disconnected == null && text == null) {
                    deliveryScheduled = false;
                    return;
                }
            }
            if (disconnected != null) {
                notifyListeners(disconnected.listeners(), listener -> listener.onDisconnected(disconnected.session()));
            } else if (attempt != null && text != null) {
                try {
                    deliverText(attempt, text);
                } catch (RuntimeException e) {
                    finish(attempt, true);
                }
            }
        }
    }

    private void deliverText(@NotNull Attempt attempt, @NotNull String text) {
        DDRemoteDamSessionInfo session;
        synchronized (lock) {
            if (current != attempt) {
                return;
            }
            session = attempt.session;
        }
        if (session == null) {
            String damSessionId = DDRemoteDamProtocol.decodeHandshakeResponse(text).damSessionId();
            var ready = new DDRemoteDamSessionInfo(hello.appDeploymentId(), hello.appInstanceId(), damSessionId);
            List<Registration<DDRemoteDamSessionListener>> listeners;
            synchronized (lock) {
                if (current != attempt) {
                    return;
                }
                attempt.session = ready;
                updateStatus(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.CONNECTED, ready));
                retryMillis = MIN_RETRY_MILLIS;
                listeners = List.copyOf(sessionListeners);
            }
            attempt.connectDeadline.cancel();
            attempt.readyDeadline.cancel();
            notifyListeners(listeners, listener -> listener.onConnected(ready));
            notifyStateObservers();
        } else {
            DDRemoteDamMessage message = DDRemoteDamProtocol.decode(text);
            List<Registration<DDRemoteDamMessageListener>> listeners;
            synchronized (lock) {
                if (current != attempt) {
                    return;
                }
                listeners = messageListeners.stream().filter(registration -> message.topic().equals(registration.topic)).toList();
            }
            notifyListeners(listeners, listener -> {
                if (isCurrent(attempt)) {
                    listener.onMessage(session, message);
                }
            });
        }
    }

    private static <T> void notifyListeners(
        @NotNull List<Registration<T>> listeners,
        @NotNull Consumer<T> notification
    ) {
        for (Registration<T> registration : listeners) {
            if (registration.active) {
                try {
                    notification.accept(registration.listener);
                } catch (RuntimeException e) {
                    log.error("Desktop message listener failed", e);
                }
            }
        }
    }

    private void deadline(@NotNull Attempt attempt, boolean waitingForReady) {
        synchronized (lock) {
            if (current != attempt || attempt.session != null || !waitingForReady && attempt.socket != null) {
                return;
            }
            detach(attempt, true);
        }
        cleanup(attempt);
        notifyStateObservers();
    }

    private void finish(@NotNull Attempt attempt, boolean retry) {
        synchronized (lock) {
            if (current != attempt) {
                return;
            }
            detach(attempt, retry);
        }
        cleanup(attempt);
        notifyStateObservers();
    }

    private void detach(@NotNull Attempt attempt, boolean retry) {
        current = null;
        updateStatus(new DDRemoteDamChannelStatus(
            closed ? DDRemoteDamChannelState.CLOSED : DDRemoteDamChannelState.DISCONNECTED, null));
        attempt.incoming.clear();
        attempt.failed.addAll(attempt.outgoing);
        attempt.outgoing.clear();
        if (attempt.sending != null) {
            attempt.failed.add(attempt.sending);
            attempt.sending = null;
        }
        if (attempt.session != null) {
            pendingDisconnect = new PendingDisconnect(attempt.session, List.copyOf(sessionListeners));
            wakeDelivery();
        }
        if (retry && running && !closed) {
            long delay = Math.min(MAX_RETRY_MILLIS, retryMillis + ThreadLocalRandom.current().nextLong(retryMillis / 4 + 1));
            retryMillis = Math.min(MAX_RETRY_MILLIS, retryMillis * 2);
            scheduleAttempt(delay);
        } else {
            running = false;
        }
    }

    private void cleanup(@NotNull Attempt attempt) {
        attempt.connect.cancel();
        attempt.connectDeadline.cancel();
        attempt.readyDeadline.cancel();
        attempt.sender.cancel();
        if (attempt.socket != null) {
            try {
                attempt.listener.dispose(attempt.socket, attempt.failed.isEmpty());
            } catch (RuntimeException e) {
                log.debug("Desktop message socket could not be aborted");
            }
        }
        if (attempt.client != null) {
            try {
                attempt.client.close();
            } catch (RuntimeException e) {
                log.debug("Desktop message client could not be closed");
            }
        }
        DBException failure = new DBException("Desktop DAM session closed");
        for (Outgoing outgoing : attempt.failed) {
            outgoing.result.completeExceptionally(failure);
        }
        attempt.failed.clear();
    }

    private final class Attempt {
        private final AttemptListener listener = new AttemptListener(this);
        private final ChannelJob connect = new ChannelJob("Connect Desktop message channel", () -> connect(this));
        private final ChannelJob connectDeadline = new ChannelJob("Desktop DAM session deadline", () -> deadline(this, false));
        private final ChannelJob readyDeadline = new ChannelJob("Desktop ready deadline", () -> deadline(this, true));
        private final ChannelJob sender = new ChannelJob("Send Desktop messages", () -> sendPending(this));
        private final ArrayDeque<String> incoming = new ArrayDeque<>();
        private final ArrayDeque<Outgoing> outgoing = new ArrayDeque<>();
        private final List<Outgoing> failed = new ArrayList<>();
        @Nullable
        private BaseWebSocketClient client;
        @Nullable
        private WebSocket socket;
        @Nullable
        private DDRemoteDamSessionInfo session;
        @Nullable
        private Outgoing sending;
        private boolean senderScheduled;
    }

    private final class AttemptListener extends BaseWebSocketClient.Listener {
        private final Attempt attempt;
        private final StringBuilder text = new StringBuilder();
        private int bytes;
        private boolean highSurrogate;

        private AttemptListener(@NotNull Attempt attempt) {
            this.attempt = attempt;
            setIdleTimeout(IDLE_TIMEOUT);
        }

        @Override
        public void onOpen(@NotNull WebSocket socket) {
            super.onOpen(socket);
            opened(attempt, socket);
        }

        @Override
        @Nullable
        public CompletionStage<?> onText(@NotNull WebSocket socket, @NotNull CharSequence data, boolean last) {
            if (!isCurrent(attempt)) {
                return null;
            }
            activity(socket);
            for (int i = 0; i < data.length(); i++) {
                char value = data.charAt(i);
                if (highSurrogate) {
                    if (!Character.isLowSurrogate(value)) {
                        finish(attempt, true);
                        return null;
                    }
                    highSurrogate = false;
                } else if (Character.isHighSurrogate(value)) {
                    bytes += 4;
                    highSurrogate = true;
                } else if (Character.isLowSurrogate(value)) {
                    finish(attempt, true);
                    return null;
                } else {
                    bytes += value <= 0x7f ? 1 : value <= 0x7ff ? 2 : 3;
                }
                if (bytes > MAX_MESSAGE_BYTES) {
                    finish(attempt, true);
                    return null;
                }
            }
            if (last && highSurrogate) {
                finish(attempt, true);
                return null;
            }
            text.append(data);
            if (last) {
                String message = text.toString();
                text.setLength(0);
                bytes = 0;
                received(attempt, message);
            }
            if (isCurrent(attempt)) {
                socket.request(1);
            }
            return null;
        }

        @Override
        @Nullable
        public CompletionStage<?> onBinary(@NotNull WebSocket socket, @NotNull ByteBuffer data, boolean last) {
            finish(attempt, true);
            return null;
        }

        @Override
        protected void closed(int statusCode, @NotNull String reason) {
            finish(attempt, true);
        }

        @Override
        protected void error(@NotNull Throwable cause) {
            finish(attempt, !isAuthorizationFailure(cause));
        }

        private void dispose(@NotNull WebSocket socket, boolean graceful) {
            try {
                if (graceful) {
                    socket.sendClose(WebSocket.NORMAL_CLOSURE, "");
                }
            } catch (RuntimeException e) {
                log.debug("Desktop message socket could not close gracefully");
            } finally {
                try {
                    socket.abort();
                } finally {
                    super.onClose(socket, WebSocket.NORMAL_CLOSURE, "");
                }
            }
        }
    }

    private final class StateObservation implements DDRemoteDamSubscription {
        private final Consumer<DDRemoteDamChannelStatus> observer;
        private final ArrayDeque<DDRemoteDamChannelStatus> pending = new ArrayDeque<>();
        private boolean active = true;
        private boolean delivering;

        private StateObservation(@NotNull Consumer<DDRemoteDamChannelStatus> observer) {
            this.observer = observer;
        }

        private void deliverPendingStates() {
            synchronized (lock) {
                if (!active || delivering) {
                    return;
                }
                delivering = true;
            }
            while (true) {
                DDRemoteDamChannelStatus next;
                synchronized (lock) {
                    next = active ? pending.pollFirst() : null;
                    if (next == null) {
                        delivering = false;
                        if (closed) {
                            stateObservers.remove(this);
                        }
                        return;
                    }
                }
                try {
                    observer.accept(next);
                } catch (RuntimeException e) {
                    log.error("Desktop channel state observer failed", e);
                }
            }
        }

        @Override
        public void close() {
            synchronized (lock) {
                active = false;
                pending.clear();
                stateObservers.remove(this);
            }
        }
    }

    private final class Registration<T> implements DDRemoteDamSubscription {
        @Nullable
        private final String topic;
        private final T listener;
        private final List<Registration<T>> owner;
        private volatile boolean active = true;

        private Registration(@Nullable String topic, @NotNull T listener, @NotNull List<Registration<T>> owner) {
            this.topic = topic;
            this.listener = listener;
            this.owner = owner;
        }

        @Override
        public void close() {
            synchronized (lock) {
                active = false;
                owner.remove(this);
            }
        }
    }

    private record Outgoing(@NotNull String text, @NotNull CompletableFuture<Void> result) {
    }

    private record PendingDisconnect(
        @NotNull DDRemoteDamSessionInfo session,
        @NotNull List<Registration<DDRemoteDamSessionListener>> listeners
    ) {
    }

    private static final class ChannelJob extends AbstractJob {
        private final Object runnerLock = new Object();
        private final Runnable action;
        @Nullable
        private Thread runner;
        private boolean interrupted;

        private ChannelJob(@NotNull String name, @NotNull Runnable action) {
            super(name);
            this.action = action;
            setSystem(true);
        }

        @Override
        @NotNull
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            synchronized (runnerLock) {
                runner = Thread.currentThread();
            }
            try {
                if (!monitor.isCanceled()) {
                    action.run();
                }
                return Status.OK_STATUS;
            } finally {
                synchronized (runnerLock) {
                    runner = null;
                    if (interrupted) {
                        Thread.interrupted();
                        interrupted = false;
                    }
                }
            }
        }

        @Override
        protected void canceling() {
            interruptRunner();
        }

        private void interruptRunner() {
            synchronized (runnerLock) {
                if (runner != null && runner.threadId() != Thread.currentThread().threadId()) {
                    interrupted = true;
                    runner.interrupt();
                }
            }
        }
    }
}
