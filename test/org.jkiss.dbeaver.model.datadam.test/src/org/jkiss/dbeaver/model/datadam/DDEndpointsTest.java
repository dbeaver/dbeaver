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
package org.jkiss.dbeaver.model.datadam;

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamProtocol;
import org.jkiss.dbeaver.DBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

class DDEndpointsTest {
    @Test
    void selectsRemoteDamEndpointForTheConfiguredEnvironment() {
        String remoteDamEndpoint = DDEndpoints.getAccountBaseUrl().replace("https://account.", "wss://dam.")
            + DDRemoteDamProtocol.WEBSOCKET_PATH;

        Assertions.assertEquals(remoteDamEndpoint, DDEndpoints.getRemoteDamWebSocketUrl());
        Assertions.assertDoesNotThrow(
            () -> DDEndpoints.requireSecureWebSocketTransport(URI.create(DDEndpoints.getRemoteDamWebSocketUrl())));
    }

    @Test
    void acceptsSecureWebSocketsAndExplicitLoopbackExceptions() {
        for (String url : List.of(
            "wss://remote-dam.staging.datadam.invalid/ws",
            "wss://remote-dam.staging.datadam.invalid:1/ws",
            "wss://remote-dam.staging.datadam.invalid:443/ws",
            "wss://remote-dam.staging.datadam.invalid:8443/ws",
            "wss://remote-dam.staging.datadam.invalid:65535/ws",
            "WSS://REMOTE-DAM.STAGING.DATADAM.INVALID/ws",
            "wss://[::1]/ws",
            "ws://localhost/ws",
            "ws://localhost:80/ws",
            "ws://127.0.0.1:8080/ws"
        )) {
            Assertions.assertDoesNotThrow(() -> DDEndpoints.requireSecureWebSocketTransport(URI.create(url)), url);
        }
    }

    @Test
    void rejectsUnsafeOrNonProtocolWebSocketEndpoints() {
        for (String url : List.of(
            "ws://remote-dam.staging.datadam.invalid/ws",
            "ws://localhost.example/ws",
            "ws://127.0.0.2/ws",
            "ws://[::1]/ws",
            "https://remote-dam.staging.datadam.invalid/ws",
            "http://localhost/ws",
            "ftp://remote-dam.staging.datadam.invalid/ws",
            "/ws",
            "wss:/ws",
            "wss:///ws",
            "wss://user@remote-dam.staging.datadam.invalid/ws",
            "wss://user:password@remote-dam.staging.datadam.invalid/ws",
            "wss://@remote-dam.staging.datadam.invalid/ws",
            "wss://remote-dam.staging.datadam.invalid:0/ws",
            "wss://remote-dam.staging.datadam.invalid:65536/ws",
            "wss://remote-dam.staging.datadam.invalid:/ws",
            "wss://remote-dam.staging.datadam.invalid:-1/ws",
            "wss://remote-dam.staging.datadam.invalid",
            "wss://remote-dam.staging.datadam.invalid/",
            "wss://remote-dam.staging.datadam.invalid/ws/",
            "wss://remote-dam.staging.datadam.invalid/other",
            "wss://remote-dam.staging.datadam.invalid/w%73",
            "wss://remote-dam.staging.datadam.invalid/other/../ws",
            "wss://remote-dam.staging.datadam.invalid/ws?token=value",
            "wss://remote-dam.staging.datadam.invalid/ws?",
            "wss://remote-dam.staging.datadam.invalid/ws#fragment",
            "wss://remote-dam.staging.datadam.invalid/ws#"
        )) {
            Assertions.assertThrows(DBException.class, () -> DDEndpoints.requireSecureWebSocketTransport(URI.create(url)), url);
        }
    }

    @Test
    void preservesHttpTransportValidation() {
        for (String url : List.of("https://account.example/graphql?query=value", "http://localhost:8080/graphql", "http://127.0.0.1/graphql")) {
            Assertions.assertDoesNotThrow(() -> DDEndpoints.requireSecureTransport(URI.create(url)), url);
        }
        for (String url : List.of("http://account.example/graphql", "https://user@account.example/graphql", "wss://account.example/ws")) {
            Assertions.assertThrows(DBException.class, () -> DDEndpoints.requireSecureTransport(URI.create(url)), url);
        }
    }
}
