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

import com.dbeaver.datadam.external.api.DDExternalDamProtocol;
import com.dbeaver.datadam.external.api.model.DDConnectionInfo;
import com.dbeaver.datadam.external.api.model.DDHello;
import com.dbeaver.datadam.external.api.model.DDMessage;
import com.dbeaver.datadam.external.api.transport.DDConnectionListener;
import com.dbeaver.datadam.external.api.transport.DDMessageChannel;
import com.dbeaver.datadam.external.api.transport.DDMessageListener;
import com.dbeaver.datadam.external.api.transport.DDSubscription;
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

public final class DDDesktopMessageChannel implements DDMessageChannel {
    private static final Log log = Log.getLog(DDDesktopMessageChannel.class);
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
    private final DDHello hello;
    private final String helloMessage;
    private final TokenProvider tokens;
    private final Supplier<BaseWebSocketClient> clients;
    private final List<Registration<DDMessageListener>> messageListeners = new ArrayList<>();
    private final List<Registration<DDConnectionListener>> connectionListeners = new ArrayList<>();
    private boolean running;
    private boolean closed;
    private long retryMillis = MIN_RETRY_MILLIS;
    @Nullable
    private Attempt current;

    @FunctionalInterface
    public interface TokenProvider {
        @Nullable
        String getAccessToken(@NotNull URI resource) throws DBException;
    }

    public DDDesktopMessageChannel(@NotNull URI endpoint, @NotNull DDHello hello, @NotNull TokenProvider tokens)
            throws DBException {
        this(endpoint, hello, tokens, BaseWebSocketClient::new);
    }

    public DDDesktopMessageChannel(
        @NotNull URI endpoint,
        @NotNull DDHello hello,
        @NotNull TokenProvider tokens,
        @NotNull Supplier<BaseWebSocketClient> clients
    ) throws DBException {
        DDEndpoints.requireSecureWebSocketTransport(endpoint);
        this.endpoint = endpoint;
        this.hello = hello;
        this.helloMessage = DDExternalDamProtocol.encodeHello(hello);
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
            if (terminal) {
                messageListeners.clear();
                connectionListeners.clear();
            }
        }
        if (stopped != null) {
            cleanup(stopped);
        }
    }

    @Override
    @NotNull
    public CompletionStage<Void> send(@NotNull DDMessage message) {
        String encoded = DDExternalDamProtocol.encode(message);
        if (encoded.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) {
            return failed("Desktop message exceeds the message size limit");
        }
        Outgoing outgoing = new Outgoing(encoded, new CompletableFuture<>());
        String rejection = null;
        synchronized (lock) {
            Attempt attempt = current;
            if (attempt == null || attempt.info == null) {
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
    public DDSubscription subscribe(@NotNull String topic, @NotNull DDMessageListener listener) {
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
    public DDSubscription addConnectionListener(@NotNull DDConnectionListener listener) {
        synchronized (lock) {
            var registration = new Registration<>(null, listener, connectionListeners);
            if (!closed) {
                connectionListeners.add(registration);
            }
            return registration;
        }
    }

    private void scheduleAttempt(long delay) {
        Attempt attempt = new Attempt();
        current = attempt;
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
            } catch (Exception e) {
                finish(attempt, true);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
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
                wakeDelivery(attempt);
            }
        }
        if (overflow) {
            finish(attempt, true);
        }
    }

    private void wakeDelivery(@NotNull Attempt attempt) {
        if (!attempt.deliveryScheduled) {
            attempt.deliveryScheduled = true;
            attempt.delivery.schedule();
        }
    }

    private void deliver(@NotNull Attempt attempt) {
        while (true) {
            String text;
            DDConnectionInfo disconnected;
            List<Registration<DDConnectionListener>> listeners;
            synchronized (lock) {
                disconnected = attempt.stopped ? attempt.info : null;
                listeners = attempt.disconnectedListeners;
                if (attempt.stopped || attempt.incoming.isEmpty()) {
                    attempt.deliveryScheduled = false;
                    attempt.disconnectedListeners = List.of();
                    text = null;
                } else {
                    text = attempt.incoming.removeFirst();
                }
            }
            if (text == null) {
                if (disconnected != null) {
                    notifyListeners(listeners, listener -> listener.onDisconnected(disconnected));
                }
                return;
            }
            try {
                deliverText(attempt, text);
            } catch (RuntimeException e) {
                finish(attempt, true);
            }
        }
    }

    private void deliverText(@NotNull Attempt attempt, @NotNull String text) {
        DDConnectionInfo info;
        synchronized (lock) {
            if (current != attempt) {
                return;
            }
            info = attempt.info;
        }
        if (info == null) {
            String connectionId = DDExternalDamProtocol.decodeReady(text).connectionId();
            var ready = new DDConnectionInfo(hello.deploymentId(), hello.applicationRunId(), connectionId);
            List<Registration<DDConnectionListener>> listeners;
            synchronized (lock) {
                if (current != attempt) {
                    return;
                }
                attempt.info = ready;
                retryMillis = MIN_RETRY_MILLIS;
                listeners = List.copyOf(connectionListeners);
            }
            attempt.connectDeadline.cancel();
            attempt.readyDeadline.cancel();
            notifyListeners(listeners, listener -> listener.onConnected(ready));
        } else {
            DDMessage message = DDExternalDamProtocol.decode(text);
            List<Registration<DDMessageListener>> listeners;
            synchronized (lock) {
                if (current != attempt) {
                    return;
                }
                listeners = messageListeners.stream().filter(registration -> message.topic().equals(registration.topic)).toList();
            }
            notifyListeners(listeners, listener -> {
                if (isCurrent(attempt)) {
                    listener.onMessage(info, message);
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
            if (current != attempt || attempt.info != null || !waitingForReady && attempt.socket != null) {
                return;
            }
            detach(attempt, true);
        }
        cleanup(attempt);
    }

    private void finish(@NotNull Attempt attempt, boolean retry) {
        synchronized (lock) {
            if (current != attempt) {
                return;
            }
            detach(attempt, retry);
        }
        cleanup(attempt);
    }

    private void detach(@NotNull Attempt attempt, boolean retry) {
        current = null;
        attempt.stopped = true;
        attempt.incoming.clear();
        attempt.failed.addAll(attempt.outgoing);
        attempt.outgoing.clear();
        if (attempt.sending != null) {
            attempt.failed.add(attempt.sending);
            attempt.sending = null;
        }
        if (attempt.info != null) {
            attempt.disconnectedListeners = List.copyOf(connectionListeners);
            wakeDelivery(attempt);
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
        attempt.delivery.interruptRunner();
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
        DBException failure = new DBException("Desktop message connection closed");
        for (Outgoing outgoing : attempt.failed) {
            outgoing.result.completeExceptionally(failure);
        }
        attempt.failed.clear();
    }

    private final class Attempt {
        private final AttemptListener listener = new AttemptListener(this);
        private final ChannelJob connect = new ChannelJob("Connect Desktop message channel", () -> connect(this));
        private final ChannelJob connectDeadline = new ChannelJob("Desktop connection deadline", () -> deadline(this, false));
        private final ChannelJob readyDeadline = new ChannelJob("Desktop ready deadline", () -> deadline(this, true));
        private final ChannelJob sender = new ChannelJob("Send Desktop messages", () -> sendPending(this));
        private final ChannelJob delivery = new ChannelJob("Deliver Desktop messages", () -> deliver(this));
        private final ArrayDeque<String> incoming = new ArrayDeque<>();
        private final ArrayDeque<Outgoing> outgoing = new ArrayDeque<>();
        private final List<Outgoing> failed = new ArrayList<>();
        private List<Registration<DDConnectionListener>> disconnectedListeners = List.of();
        @Nullable
        private BaseWebSocketClient client;
        @Nullable
        private WebSocket socket;
        @Nullable
        private DDConnectionInfo info;
        @Nullable
        private Outgoing sending;
        private boolean senderScheduled;
        private boolean deliveryScheduled;
        private boolean stopped;
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

    private final class Registration<T> implements DDSubscription {
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
                if (runner != null && runner != Thread.currentThread()) {
                    interrupted = true;
                    runner.interrupt();
                }
            }
        }
    }
}
