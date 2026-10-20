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
import com.dbeaver.datadam.external.api.model.DDReady;
import com.dbeaver.datadam.external.api.transport.DDConnectionListener;
import com.dbeaver.datadam.external.api.transport.DDSubscription;
import com.google.gson.JsonObject;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.BaseWebSocketClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Timeout(20)
class DDDesktopMessageChannelTest extends DBeaverUnitTest {
    private static final URI ENDPOINT = URI.create("wss://external.example/ws");
    private static final DDHello HELLO = new DDHello("deployment", "application-run", "DBeaver", "26.3");
    private static final DDMessage MESSAGE = message("topic", "value");

    @Test
    void sendsHelloBeforeMessagesAndWaitsForReady() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            fails(channel.send(MESSAGE));
            channel.start();
            channel.start();
            Peer peer = clients.next();
            peer.open();
            Assertions.assertEquals(HELLO, DDExternalDamProtocol.decodeHello(peer.nextSent().text));
            Assertions.assertEquals(Map.of("Authorization", "Bearer token"), peer.headers);
            Assertions.assertEquals(ENDPOINT, peer.endpoint);
            Assertions.assertTrue(events.connected.isEmpty());
            fails(channel.send(MESSAGE));
            peer.ready("first");
            Assertions.assertEquals(new DDConnectionInfo("deployment", "application-run", "first"), take(events.connected));
            channel.send(MESSAGE).toCompletableFuture().get(3, TimeUnit.SECONDS);
            Assertions.assertEquals(MESSAGE, DDExternalDamProtocol.decode(peer.nextSent().text));
            Assertions.assertTrue(clients.attempts.isEmpty());
            channel.pause();
            Assertions.assertEquals("first", take(events.disconnected).connectionId());
            peer.listener.onClose(peer.socket, 1000, "late");
            peer.listener.onError(peer.socket, new IOException("late"));
            Assertions.assertTrue(events.disconnected.isEmpty());
            Mockito.verify(peer.client).close();
            Mockito.verify(peer.socket, Mockito.atLeastOnce()).abort();
        }
    }

    @Test
    void reconnectReacquiresTokenAndPreservesIdentityAndSubscriptions() throws Exception {
        Clients clients = new Clients();
        AtomicInteger tokens = new AtomicInteger();
        Events events = new Events();
        BlockingQueue<DDConnectionInfo> messages = new LinkedBlockingQueue<>();
        try (var channel = channel(clients, resource -> "token-" + tokens.incrementAndGet())) {
            channel.addConnectionListener(events);
            channel.subscribe("topic", (info, message) -> messages.add(info));
            channel.start();
            Peer first = ready(clients, events, "first");
            first.listener.onClose(first.socket, 1008, "expired");
            Assertions.assertEquals("first", take(events.disconnected).connectionId());
            Peer second = ready(clients, events, "second");
            Assertions.assertEquals("Bearer token-2", second.headers.get("Authorization"));
            second.text(DDExternalDamProtocol.encode(MESSAGE));
            DDConnectionInfo connection = take(messages);
            Assertions.assertEquals("second", connection.connectionId());
            Assertions.assertEquals(HELLO.deploymentId(), connection.deploymentId());
            Assertions.assertEquals(HELLO.applicationRunId(), connection.applicationRunId());
            Assertions.assertEquals(2, tokens.get());
            Mockito.verify(first.client).close();
        }
    }

    @Test
    void pauseDuringHandshakeDisposesLateSocketAndResumeKeepsListeners() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer first = clients.next();
            channel.pause();
            channel.pause();
            first.open();
            first.ready("stale");
            Assertions.assertTrue(first.sent.isEmpty());
            Assertions.assertTrue(events.connected.isEmpty());
            Assertions.assertTrue(events.disconnected.isEmpty());
            Mockito.verify(first.client).close();
            Mockito.verify(first.socket, Mockito.atLeastOnce()).abort();
            channel.start();
            ready(clients, events, "resumed");
            channel.close();
            Assertions.assertEquals("resumed", take(events.disconnected).connectionId());
            channel.close();
            channel.start();
            clients.expectNoAttempt();
            fails(channel.send(MESSAGE));
        }
    }

    @Test
    void lateTokenLookupCannotOpenASocketAfterPauseAndResume() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch lookingUp = new CountDownLatch(1);
        CompletableFuture<String> oldToken = new CompletableFuture<>();
        try (var channel = channel(clients, resource -> {
            if (calls.incrementAndGet() == 1) {
                lookingUp.countDown();
                return oldToken.join();
            }
            return "current-token";
        })) {
            channel.addConnectionListener(events);
            channel.start();
            Assertions.assertTrue(lookingUp.await(3, TimeUnit.SECONDS));
            channel.pause();
            channel.start();
            Peer current = ready(clients, events, "current");
            Assertions.assertEquals("Bearer current-token", current.headers.get("Authorization"));

            oldToken.complete("old-token");

            clients.expectNoAttempt();
            Assertions.assertEquals(2, calls.get());
            Mockito.verify(current.client, Mockito.never()).close();
        } finally {
            oldToken.complete("old-token");
        }
    }

    @Test
    void canceledConnectDeadlineCannotCloseAnAlreadyUpgradedSocket() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer peer = clients.next();
            AbstractJob deadline = Arrays.stream(Job.getJobManager().find(null))
                .filter(job -> job.getName().equals("Desktop connection deadline"))
                .map(AbstractJob.class::cast)
                .findFirst().orElseThrow();
            peer.open();
            peer.nextSent();

            deadline.runDirectly(monitor);
            peer.ready("ready");

            Assertions.assertEquals("ready", take(events.connected).connectionId());
            Assertions.assertTrue(events.disconnected.isEmpty());
            Mockito.verify(peer.client, Mockito.never()).close();
        }
    }

    @Test
    void lateCallbacksAndSendCompletionCannotAffectReplacement() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer first = ready(clients, events, "first");
            first.holdSends = true;
            CompletionStage<Void> pending = channel.send(MESSAGE);
            final Sent oldSend = first.nextSent();
            channel.pause();
            fails(pending);
            Assertions.assertEquals("first", take(events.disconnected).connectionId());
            channel.start();
            final Peer second = ready(clients, events, "second");
            first.ready("late-ready");
            first.listener.onClose(first.socket, 1008, "late-close");
            first.listener.onError(first.socket, new IOException("late-error"));
            oldSend.result.complete(first.socket);
            channel.send(MESSAGE).toCompletableFuture().get(3, TimeUnit.SECONDS);
            Assertions.assertEquals(MESSAGE, DDExternalDamProtocol.decode(second.nextSent().text));
            Assertions.assertTrue(events.connected.isEmpty());
            Assertions.assertTrue(events.disconnected.isEmpty());
            Mockito.verify(second.client, Mockito.never()).close();
        }
    }

    @Test
    void unavailableAuthorizationSuspendsUntilExplicitStart() throws Exception {
        Clients clients = new Clients();
        AtomicReference<String> token = new AtomicReference<>();
        CountDownLatch lookedUp = new CountDownLatch(1);
        try (var channel = channel(clients, resource -> {
            String value = token.get();
            lookedUp.countDown();
            return value;
        })) {
            channel.start();
            Assertions.assertTrue(lookedUp.await(3, TimeUnit.SECONDS));
            clients.expectNoAttempt();
            token.set("new-token");
            channel.start();
            Peer peer = clients.next();
            peer.open();
            Assertions.assertEquals("Bearer new-token", peer.headers.get("Authorization"));
        }
    }

    @Test
    void temporaryTokenLookupFailureRetries() throws Exception {
        Clients clients = new Clients();
        AtomicInteger calls = new AtomicInteger();
        try (var channel = channel(clients, resource -> {
            if (calls.incrementAndGet() == 1) {
                throw new DBException("Temporary token lookup failure");
            }
            return "token";
        })) {
            channel.start();
            clients.next().open();
            Assertions.assertEquals(2, calls.get());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void authorizationHandshakeFailureSuspendsUntilExplicitStart(int status) throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        AtomicInteger tokens = new AtomicInteger();
        try (var channel = channel(clients, resource -> "token-" + tokens.incrementAndGet())) {
            channel.addConnectionListener(events);
            channel.start();
            Peer rejected = clients.next();
            HttpResponse<?> response = Mockito.mock(HttpResponse.class);
            Mockito.when(response.statusCode()).thenReturn(status);
            rejected.handshake.completeExceptionally(new WebSocketHandshakeException(response));
            Mockito.verify(rejected.client, Mockito.timeout(3_000)).close();
            clients.expectNoAttempt();
            Assertions.assertTrue(events.disconnected.isEmpty());
            channel.start();
            ready(clients, events, "authorized");
            Assertions.assertEquals(2, tokens.get());
        }
    }

    @Test
    void networkHandshakeFailureRetriesWithoutDisconnectEvent() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            clients.next().handshake.completeExceptionally(new IOException("Offline"));
            ready(clients, events, "reconnected");
            Assertions.assertTrue(events.disconnected.isEmpty());
        }
    }

    @Test
    void pauseCancelsDelayedRetryAndStartResumesImmediately() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer failed = clients.next();
            failed.handshake.completeExceptionally(new IOException("Offline"));
            Mockito.verify(failed.client, Mockito.timeout(3_000)).close();
            channel.pause();
            clients.expectNoAttempt();
            channel.start();
            ready(clients, events, "resumed");
        }
    }

    @Test
    void readyDeadlineClosesHandshakeAndRetriesWithoutDisconnectEvent() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer silent = clients.next();
            silent.open();
            silent.nextSent();
            Mockito.verify(silent.socket, Mockito.timeout(12_000).atLeastOnce()).abort();
            Assertions.assertTrue(events.connected.isEmpty());
            Assertions.assertTrue(events.disconnected.isEmpty());
            ready(clients, events, "after-timeout");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{}", "{\"type\":\"ready\",\"payload\":{}}"})
    void malformedHandshakeRetriesWithoutReportingConnected(@NotNull String text) throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer first = clients.next();
            first.open();
            first.nextSent();
            first.text(text);
            ready(clients, events, "valid");
            Assertions.assertTrue(events.disconnected.isEmpty());
        }
    }

    @Test
    void binaryFramesAndMalformedMessagesTerminateReadyConnections() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer first = ready(clients, events, "binary");
            first.listener.onBinary(first.socket, ByteBuffer.wrap(new byte[] {1}), false);
            Assertions.assertEquals("binary", take(events.disconnected).connectionId());
            Peer second = ready(clients, events, "malformed");
            second.text("{}");
            Assertions.assertEquals("malformed", take(events.disconnected).connectionId());
        }
    }

    @Test
    void limitsUtf8BytesAcrossFragmentsAndAllowsExactlyTheLimit() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        BlockingQueue<DDMessage> received = new LinkedBlockingQueue<>();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.subscribe("topic", (info, message) -> received.add(message));
            channel.start();
            Peer peer = ready(clients, events, "bounded");
            String empty = DDExternalDamProtocol.encode(message("topic", ""));
            int remaining = 65_536 - empty.getBytes(StandardCharsets.UTF_8).length;
            DDMessage maximum = message("topic", "é".repeat(remaining / 2) + "x".repeat(remaining % 2));
            String encoded = DDExternalDamProtocol.encode(maximum);
            Assertions.assertEquals(65_536, encoded.getBytes(StandardCharsets.UTF_8).length);
            int middle = encoded.length() / 2;
            peer.listener.onText(peer.socket, encoded.substring(0, middle), false);
            peer.listener.onText(peer.socket, encoded.substring(middle), true);
            Assertions.assertEquals(maximum, take(received));
            channel.send(maximum).toCompletableFuture().get(3, TimeUnit.SECONDS);
            fails(channel.send(message("topic", "x".repeat(65_536))));
            peer.listener.onText(peer.socket, "é".repeat(20_000), false);
            peer.listener.onText(peer.socket, "é".repeat(20_000), false);
            Assertions.assertEquals("bounded", take(events.disconnected).connectionId());
        }
    }

    @Test
    void acceptsSurrogatePairsSplitBetweenFragments() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        BlockingQueue<DDMessage> received = new LinkedBlockingQueue<>();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.subscribe("topic", (info, message) -> received.add(message));
            channel.start();
            Peer peer = ready(clients, events, "unicode");
            String beaver = "🦫";
            DDMessage message = message("topic", beaver);
            String encoded = DDExternalDamProtocol.encode(message);
            int split = encoded.indexOf(beaver) + 1;
            Assertions.assertTrue(split > 0);
            peer.listener.onText(peer.socket, encoded.substring(0, split), false);
            peer.listener.onText(peer.socket, encoded.substring(split), true);
            Assertions.assertEquals(message, take(received));
        }
    }

    @Test
    void isolatesListenersRoutesTopicsAndHonorsUnsubscribe() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        BlockingQueue<DDMessage> received = new LinkedBlockingQueue<>();
        BlockingQueue<DDMessage> other = new LinkedBlockingQueue<>();
        Events removedConnections = new Events();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();
        AtomicInteger removed = new AtomicInteger();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(new DDConnectionListener() {
                @Override
                public void onConnected(@NotNull DDConnectionInfo connection) {
                    throw new IllegalStateException("Broken connection listener");
                }

                @Override
                public void onDisconnected(@NotNull DDConnectionInfo connection) {
                    throw new IllegalStateException("Broken disconnect listener");
                }
            });
            channel.addConnectionListener(events);
            channel.addConnectionListener(removedConnections).close();
            channel.subscribe("topic", (info, message) -> {
                throw new IllegalStateException("Broken message listener");
            });
            channel.subscribe("topic", (info, message) -> {
                deliveryThread.set(Thread.currentThread());
                received.add(message);
            });
            channel.subscribe("other", (info, message) -> other.add(message));
            DDSubscription subscription = channel.subscribe("topic", (info, message) -> removed.incrementAndGet());
            subscription.close();
            subscription.close();
            channel.start();
            Peer peer = ready(clients, events, "listeners");
            peer.text(DDExternalDamProtocol.encode(MESSAGE));
            DDMessage otherMessage = message("other", "second");
            peer.text(DDExternalDamProtocol.encode(otherMessage));
            Assertions.assertEquals(MESSAGE, take(received));
            Assertions.assertEquals(otherMessage, take(other));
            Assertions.assertNotEquals(Thread.currentThread(), deliveryThread.get());
            Assertions.assertEquals(0, removed.get());
            Assertions.assertTrue(removedConnections.connected.isEmpty());
            Assertions.assertTrue(received.isEmpty());
            channel.pause();
            Assertions.assertEquals("listeners", take(events.disconnected).connectionId());
        }
    }

    @Test
    void serializesHelloAndApplicationSendsAndFailsTheEntireQueueOnSendFailure() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer peer = clients.next();
            peer.holdSends = true;
            peer.open();
            final Sent hello = peer.nextSent();
            peer.ready("serialized");
            take(events.connected);
            final CompletionStage<Void> first = channel.send(MESSAGE);
            final CompletionStage<Void> second = channel.send(message("topic", "second"));
            Assertions.assertTrue(peer.sent.isEmpty());
            hello.result.complete(peer.socket);
            Sent sending = peer.nextSent();
            Assertions.assertEquals(MESSAGE, DDExternalDamProtocol.decode(sending.text));
            Assertions.assertTrue(peer.sent.isEmpty());
            sending.result.completeExceptionally(new IOException("Send failed"));
            fails(first);
            fails(second);
            Assertions.assertEquals("serialized", take(events.disconnected).connectionId());
            Peer replacement = ready(clients, events, "replacement");
            Assertions.assertTrue(replacement.sent.isEmpty());
        }
    }

    @Test
    void outgoingQueueIsBoundedAndPauseFailsAllPendingSends() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer peer = ready(clients, events, "outgoing");
            peer.holdSends = true;
            List<CompletionStage<Void>> pending = new ArrayList<>();
            pending.add(channel.send(MESSAGE));
            final Sent sending = peer.nextSent();
            for (int i = 1; i < 64; i++) {
                pending.add(channel.send(MESSAGE));
            }
            fails(channel.send(MESSAGE));
            Assertions.assertTrue(peer.sent.isEmpty());
            channel.pause();
            for (CompletionStage<Void> result : pending) {
                fails(result);
            }
            sending.result.complete(peer.socket);
            Assertions.assertTrue(peer.sent.isEmpty());
        }
    }

    @Test
    void stalledSendHasADeadlineAndDoesNotRetainQueuedMessages() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.start();
            Peer peer = ready(clients, events, "stalled-send");
            peer.holdSends = true;
            CompletionStage<Void> first = channel.send(MESSAGE);
            peer.nextSent();
            CompletionStage<Void> second = channel.send(MESSAGE);
            Assertions.assertThrows(ExecutionException.class, () -> first.toCompletableFuture().get(12, TimeUnit.SECONDS));
            fails(second);
            Assertions.assertEquals("stalled-send", take(events.disconnected).connectionId());
            ready(clients, events, "recovered");
        }
    }

    @Test
    void incomingQueueIsBoundedWhileAListenerIsBlocked() throws Exception {
        Clients clients = new Clients();
        Events events = new Events();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger deliveries = new AtomicInteger();
        try (var channel = channel(clients, resource -> "token")) {
            channel.addConnectionListener(events);
            channel.subscribe("topic", (info, message) -> {
                deliveries.incrementAndGet();
                entered.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            channel.start();
            Peer peer = ready(clients, events, "incoming");
            peer.text(DDExternalDamProtocol.encode(MESSAGE));
            Assertions.assertTrue(entered.await(3, TimeUnit.SECONDS));
            try {
                for (int i = 0; i < 65; i++) {
                    peer.text(DDExternalDamProtocol.encode(MESSAGE));
                }
                Mockito.verify(peer.socket, Mockito.atLeastOnce()).abort();
            } finally {
                release.countDown();
            }
            Assertions.assertEquals("incoming", take(events.disconnected).connectionId());
            Assertions.assertEquals(1, deliveries.get());
        } finally {
            release.countDown();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ws://external.example/ws", "https://external.example/ws", "wss://user:password@external.example/ws",
        "wss://external.example/ws?token=secret", "wss://external.example/ws#fragment"
    })
    void rejectsUnsafeEndpointsBeforeCreatingClients(@NotNull String endpoint) {
        Clients clients = new Clients();
        Assertions.assertThrows(DBException.class, () -> new DDDesktopMessageChannel(
            URI.create(endpoint), HELLO, resource -> "token", clients));
        Assertions.assertTrue(clients.attempts.isEmpty());
    }

    @NotNull
    private static DDDesktopMessageChannel channel(
        @NotNull Clients clients,
        @NotNull DDDesktopMessageChannel.TokenProvider tokens
    ) throws DBException {
        return new DDDesktopMessageChannel(ENDPOINT, HELLO, tokens, clients);
    }

    @NotNull
    private static DDMessage message(@NotNull String topic, @NotNull String value) {
        JsonObject payload = new JsonObject();
        payload.addProperty("value", value);
        return new DDMessage(topic, "changed", payload);
    }

    @NotNull
    private static Peer ready(@NotNull Clients clients, @NotNull Events events, @NotNull String connectionId) throws Exception {
        Peer peer = clients.next();
        peer.open();
        Assertions.assertEquals(HELLO, DDExternalDamProtocol.decodeHello(peer.nextSent().text));
        peer.ready(connectionId);
        Assertions.assertEquals(connectionId, take(events.connected).connectionId());
        return peer;
    }

    private static void fails(@NotNull CompletionStage<Void> result) {
        ExecutionException failure = Assertions.assertThrows(ExecutionException.class,
            () -> result.toCompletableFuture().get(3, TimeUnit.SECONDS));
        Assertions.assertInstanceOf(DBException.class, failure.getCause());
    }

    @NotNull
    private static <T> T take(@NotNull BlockingQueue<T> queue) throws InterruptedException {
        T result = queue.poll(3, TimeUnit.SECONDS);
        Assertions.assertNotNull(result, "Timed out waiting for transport activity");
        return result;
    }

    private static final class Events implements DDConnectionListener {
        private final BlockingQueue<DDConnectionInfo> connected = new LinkedBlockingQueue<>();
        private final BlockingQueue<DDConnectionInfo> disconnected = new LinkedBlockingQueue<>();

        @Override
        public void onConnected(@NotNull DDConnectionInfo connection) {
            connected.add(connection);
        }

        @Override
        public void onDisconnected(@NotNull DDConnectionInfo connection) {
            disconnected.add(connection);
        }
    }

    private static final class Clients implements Supplier<BaseWebSocketClient> {
        private final BlockingQueue<Peer> attempts = new LinkedBlockingQueue<>();

        @Override
        @NotNull
        public BaseWebSocketClient get() {
            BaseWebSocketClient client = Mockito.mock(BaseWebSocketClient.class);
            AtomicReference<Peer> pending = new AtomicReference<>();
            AtomicBoolean closed = new AtomicBoolean();
            try {
                Mockito.when(client.connect(ArgumentMatchers.any(), ArgumentMatchers.anyMap(), ArgumentMatchers.any()))
                    .thenAnswer(invocation -> {
                        if (closed.get()) {
                            throw new IOException("Client closed");
                        }
                        Peer peer = new Peer(client, invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
                        pending.set(peer);
                        attempts.add(peer);
                        if (closed.get()) {
                            peer.handshake.completeExceptionally(new IOException("Client closed"));
                        }
                        return peer.handshake.get();
                    });
            } catch (ExecutionException | InterruptedException e) {
                throw new AssertionError(e);
            }
            Mockito.doAnswer(invocation -> {
                closed.set(true);
                Peer peer = pending.get();
                if (peer != null) {
                    peer.handshake.completeExceptionally(new IOException("Client closed"));
                }
                return null;
            }).when(client).close();
            return client;
        }

        @NotNull
        private Peer next() throws InterruptedException {
            return take(attempts);
        }

        private void expectNoAttempt() throws InterruptedException {
            Assertions.assertNull(attempts.poll(1_500, TimeUnit.MILLISECONDS));
        }
    }

    private static final class Peer {
        private final BaseWebSocketClient client;
        private final URI endpoint;
        private final Map<String, String> headers;
        private final BaseWebSocketClient.Listener listener;
        private final WebSocket socket = Mockito.mock(WebSocket.class);
        private final CompletableFuture<WebSocket> handshake = new CompletableFuture<>();
        private final BlockingQueue<Sent> sent = new LinkedBlockingQueue<>();
        private volatile boolean holdSends;

        private Peer(
            @NotNull BaseWebSocketClient client,
            @NotNull URI endpoint,
            @NotNull Map<String, String> headers,
            @NotNull BaseWebSocketClient.Listener listener
        ) {
            this.client = client;
            this.endpoint = endpoint;
            this.headers = Map.copyOf(headers);
            this.listener = listener;
            Mockito.when(socket.sendText(ArgumentMatchers.any(), ArgumentMatchers.eq(true))).thenAnswer(invocation -> {
                CompletableFuture<WebSocket> result = new CompletableFuture<>();
                if (!holdSends) {
                    result.complete(socket);
                }
                sent.add(new Sent(invocation.getArgument(0).toString(), result));
                return result;
            });
            Mockito.when(socket.sendClose(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString()))
                .thenReturn(CompletableFuture.completedFuture(socket));
        }

        private void open() {
            listener.onOpen(socket);
            handshake.complete(socket);
        }

        private void ready(@NotNull String connectionId) {
            text(DDExternalDamProtocol.encodeReady(new DDReady(connectionId)));
        }

        private void text(@NotNull String text) {
            listener.onText(socket, text, true);
        }

        @NotNull
        private Sent nextSent() throws InterruptedException {
            return take(sent);
        }
    }

    private record Sent(@NotNull String text, @NotNull CompletableFuture<WebSocket> result) {
    }
}
