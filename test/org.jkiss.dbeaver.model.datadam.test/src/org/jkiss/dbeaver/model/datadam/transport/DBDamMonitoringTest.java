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
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamHandshakeResponse;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamMessage;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.internal.DDConnectionMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.BaseWebSocketClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Timeout(5)
public class DBDamMonitoringTest extends DBeaverUnitTest {
    private static final DDRemoteDamHandshakeRequest HELLO = new DDRemoteDamHandshakeRequest(
        "deployment", "instance", "workspace", "client", "DBeaver", "1.0.0");

    @Test
    void keepsCollectingAcrossObserverDisposalAndReopening() throws Exception {
        try (var fixture = new Fixture()) {
            var channel = fixture.channel;
            fixture.ready("first");
            var monitor = channel.getMonitor();
            var previousEvents = monitor.getEvents();
            fixture.stateObservation.close();

            channel.pause();
            channel.start();
            var second = fixture.handshake("second");
            second.listener.onText(second.socket, DDRemoteDamProtocol.encode(message("after reopening")), true);
            take(fixture.received);

            var events = monitor.getEvents();
            Assertions.assertTrue(events.containsAll(previousEvents));
            Assertions.assertTrue(events.stream().anyMatch(event -> event.type() == DDConnectionMonitor.EventType.DISCONNECTED));
            Assertions.assertTrue(events.stream().anyMatch(event -> event.type() == DDConnectionMonitor.EventType.CONNECTED
                && "second".equals(event.details())));
            try (var reopened = channel.observeState(status -> Assertions.assertTrue(status.isReady()))) {
                Assertions.assertSame(monitor, channel.getMonitor());
                Assertions.assertEquals(events, monitor.getEvents());
            }
            Assertions.assertEquals(events, monitor.getEvents());
        }
    }

    @Test
    void measuresUtf8TrafficAcrossFragmentsAndIgnoresStaleSocketsAfterReconnect() throws Exception {
        try (var fixture = new Fixture()) {
            var channel = fixture.channel;
            var first = fixture.ready("first");
            long handshakeReceived = channel.getMonitor().snapshot().bytesReceived();
            var message = message("é中😀");
            String encoded = DDRemoteDamProtocol.encode(message);
            int split = encoded.indexOf("😀") + 1;
            first.listener.onText(first.socket, encoded.substring(0, split), false);
            first.listener.onText(first.socket, encoded.substring(split), true);
            take(fixture.received);
            Assertions.assertEquals(handshakeReceived + utf8Length(encoded), channel.getMonitor().snapshot().bytesReceived());
            channel.send(message).toCompletableFuture().get(1, TimeUnit.SECONDS);
            Assertions.assertEquals(utf8Length(DDRemoteDamProtocol.encodeHandshakeRequest(HELLO)) + utf8Length(encoded),
                channel.getMonitor().snapshot().bytesSent());

            first.listener.onClose(first.socket, 1008, "expired");
            Assertions.assertNotNull(channel.getMonitor().snapshot().error());
            fixture.ready("second");
            var afterReconnect = channel.getMonitor().snapshot();
            Assertions.assertNull(afterReconnect.error());
            first.listener.onText(first.socket, encoded, true);
            Assertions.assertEquals(afterReconnect.bytesReceived(), channel.getMonitor().snapshot().bytesReceived());
            Assertions.assertEquals(utf8Length(DDRemoteDamProtocol.encodeHandshakeRequest(HELLO)), afterReconnect.bytesSent());
        }
    }

    @Test
    void malformedAndOversizedTextReportsAnInvalidMessage() throws Exception {
        for (String text : List.of("\uD800x", "\uDC00", "\uD800", "x".repeat(65_537))) {
            try (var fixture = new Fixture()) {
                var peer = fixture.ready("session");
                peer.listener.onText(peer.socket, text, true);
                var monitor = fixture.channel.getMonitor();
                Assertions.assertEquals(DDConnectionMessages.invalid_message, monitor.snapshot().error());
                Assertions.assertTrue(monitor.getEvents().stream().anyMatch(event ->
                    event.type() == DDConnectionMonitor.EventType.ERROR && DDConnectionMessages.invalid_message.equals(event.details())));
            }
        }
    }

    @Test
    void failedSendsDoNotIncreaseTrafficAndIntentionalPauseDoesNotReportAnError() throws Exception {
        try (var fixture = new Fixture()) {
            var channel = fixture.channel;
            var peer = fixture.ready("session");
            long sentBefore = channel.getMonitor().snapshot().bytesSent();
            Mockito.when(peer.socket.sendText(Mockito.any(), Mockito.eq(true)))
                .thenReturn(CompletableFuture.failedFuture(new IOException("Send failed")));
            var sending = channel.send(message("value"));
            Assertions.assertThrows(ExecutionException.class, () -> sending.toCompletableFuture().get(1, TimeUnit.SECONDS));
            Assertions.assertEquals(sentBefore, channel.getMonitor().snapshot().bytesSent());
            Assertions.assertNotNull(channel.getMonitor().snapshot().error());
            fixture.ready("reconnected");
            channel.pause();
            Assertions.assertNull(channel.getMonitor().snapshot().error());
            Assertions.assertEquals(DDRemoteDamChannelState.DISCONNECTED, channel.getMonitor().snapshot().state());
        }
    }

    private static int utf8Length(@NotNull String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    @NotNull
    private static DDRemoteDamMessage message(@NotNull String value) {
        return new DDRemoteDamMessage("test", "message", Map.of("value", value));
    }

    @NotNull
    private static <T> T take(@NotNull BlockingQueue<T> queue) throws InterruptedException {
        T result = queue.poll(2, TimeUnit.SECONDS);
        Assertions.assertNotNull(result, "Timed out waiting for channel activity");
        return result;
    }

    private static class Fixture implements AutoCloseable {
        private final BlockingQueue<Peer> peers = new LinkedBlockingQueue<>();
        private final BlockingQueue<DDRemoteDamMessage> received = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> connected = new LinkedBlockingQueue<>();
        private final DBDamChannel channel;
        private final DDRemoteDamSubscription stateObservation;

        private Fixture() throws DBException {
            // Platform startup and cold instrumentation must complete before the activity wait starts.
            DBWorkbench.getPlatform();
            var clients = new LinkedBlockingQueue<BaseWebSocketClient>();
            clients.add(client());
            clients.add(client());
            channel = new DBDamChannel(URI.create("wss://example.com/ws"), HELLO, resource -> "token", clients::remove,
                Duration.ofSeconds(90), Duration.ofMillis(10));
            channel.subscribe("test", (session, message) -> received.add(message));
            stateObservation = channel.observeState(status -> {
                if (status.isReady()) {
                    connected.add(status.session().damSessionId());
                }
            });
            channel.start();
        }

        @NotNull
        private BaseWebSocketClient client() {
            var client = Mockito.mock(BaseWebSocketClient.class);
            var socket = Mockito.mock(WebSocket.class);
            var sent = new LinkedBlockingQueue<String>();
            Mockito.when(socket.sendText(Mockito.any(), Mockito.eq(true))).thenAnswer(send -> {
                sent.add(send.getArgument(0).toString());
                return CompletableFuture.completedFuture(socket);
            });
            try {
                Mockito.when(client.connect(Mockito.any(), Mockito.anyMap(), Mockito.any())).thenAnswer(invocation -> {
                    var listener = (BaseWebSocketClient.Listener) invocation.getArgument(2);
                    listener.onOpen(socket);
                    peers.add(new Peer(socket, listener, sent));
                    return socket;
                });
            } catch (ExecutionException | InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return client;
        }

        @NotNull
        private Peer ready(@NotNull String id) throws InterruptedException {
            var peer = handshake(id);
            Assertions.assertEquals(id, take(connected));
            return peer;
        }

        @NotNull
        private Peer handshake(@NotNull String id) throws InterruptedException {
            var peer = take(peers);
            Assertions.assertEquals(DDRemoteDamProtocol.encodeHandshakeRequest(HELLO), take(peer.sent));
            peer.listener.onText(peer.socket, DDRemoteDamProtocol.encodeHandshakeResponse(new DDRemoteDamHandshakeResponse(id)), true);
            return peer;
        }

        @Override
        public void close() {
            channel.close();
        }
    }

    private record Peer(
        @NotNull WebSocket socket,
        @NotNull BaseWebSocketClient.Listener listener,
        @NotNull BlockingQueue<String> sent
    ) {
    }
}
