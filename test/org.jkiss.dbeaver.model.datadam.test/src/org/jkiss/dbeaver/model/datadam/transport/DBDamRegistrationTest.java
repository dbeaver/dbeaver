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
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamNameChanged;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.BaseWebSocketClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.net.URI;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Timeout(5)
public class DBDamRegistrationTest extends DBeaverUnitTest {
    @Test
    void registrationCarriesTheNameButOnlyServerConfirmationRecordsAChange() throws Exception {
        var hello = new DDRemoteDamHandshakeRequest(
            "deployment", "instance", "workspace", "client", "DBeaver", "1", "DBeaver: workstation");
        var client = Mockito.mock(BaseWebSocketClient.class);
        var socket = Mockito.mock(WebSocket.class);
        var listeners = new LinkedBlockingQueue<BaseWebSocketClient.Listener>();
        var sent = new LinkedBlockingQueue<String>();
        var received = new LinkedBlockingQueue<DDRemoteDamMessage>();
        var ready = new CountDownLatch(1);
        Mockito.when(client.connect(Mockito.any(), Mockito.anyMap(), Mockito.any())).thenAnswer(invocation -> {
            BaseWebSocketClient.Listener listener = invocation.getArgument(2);
            listener.onOpen(socket);
            listeners.add(listener);
            return socket;
        });
        Mockito.when(socket.sendText(Mockito.any(), Mockito.eq(true))).thenAnswer(invocation -> {
            sent.add(invocation.getArgument(0).toString());
            return CompletableFuture.completedFuture(socket);
        });
        Mockito.when(socket.sendClose(Mockito.anyInt(), Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(socket));
        try (var channel = new DBDamChannel(URI.create("wss://external.example/ws"), hello, resource -> "token", () -> client)) {
            channel.observeState(status -> {
                if (status.isReady()) {
                    ready.countDown();
                }
            });
            channel.subscribe("barrier", (session, message) -> received.add(message));
            channel.start();
            var listener = listeners.poll(2, TimeUnit.SECONDS);
            Assertions.assertNotNull(listener);
            Assertions.assertEquals(hello, DDRemoteDamProtocol.decodeHandshakeRequest(sent.poll(2, TimeUnit.SECONDS)));
            listener.onText(socket, DDRemoteDamProtocol.encodeHandshakeResponse(new DDRemoteDamHandshakeResponse("session")), true);
            Assertions.assertTrue(ready.await(2, TimeUnit.SECONDS));
            Assertions.assertTrue(channel.getMonitor().getEvents().stream()
                .noneMatch(event -> event.type() == DDConnectionMonitor.EventType.NAME_CHANGED));

            channel.getMonitor().damNameLoaded(hello.suggestedName());
            String confirmed = DDRemoteDamProtocol.encode(new DDRemoteDamNameChanged(hello.suggestedName()).toMessage());
            listener.onText(socket, confirmed, true);
            listener.onText(socket, confirmed, true);
            listener.onText(socket, DDRemoteDamProtocol.encode(new DDRemoteDamMessage("barrier", "done", Map.of())), true);
            Assertions.assertNotNull(received.poll(2, TimeUnit.SECONDS));
            Assertions.assertEquals(List.of("DBeaver: workstation"), channel.getMonitor().getEvents().stream()
                .filter(event -> event.type() == DDConnectionMonitor.EventType.NAME_CHANGED)
                .map(DDConnectionMonitor.Event::details).toList());
            Assertions.assertTrue(sent.isEmpty(), "Registration must not issue follow-up naming requests");
        }
    }
}
