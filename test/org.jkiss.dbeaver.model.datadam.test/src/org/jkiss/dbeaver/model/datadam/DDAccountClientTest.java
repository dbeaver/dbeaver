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

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class DDAccountClientTest {
    private static final UUID ACCOUNT_ID = UUID.fromString("8decb064-2709-4914-b6b6-68eaef98cac3");
    private static final String CRYPTO_RESPONSE = """
        {"data":{"gatewayCryptoState":{"accountId":"8decb064-2709-4914-b6b6-68eaef98cac3","cryptoConfigured":true,
        "encryptedBundle":"encrypted-bundle","generation":2,"salt":"salt","iterations":100000}}}
        """;
    private final AtomicReference<String> response = new AtomicReference<>(CRYPTO_RESPONSE);
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> posted = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicInteger requests = new AtomicInteger();
    private HttpServer server;
    private URI account;
    private MockedStatic<DDDesktopAccessService> session;
    private DDDesktopAccessService manager;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        account = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            method.set(exchange.getRequestMethod());
            posted.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Location", account + "/redirected");
            exchange.sendResponseHeaders(status.get(), body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        manager = Mockito.mock(DDDesktopAccessService.class);
        session = Mockito.mockStatic(DDDesktopAccessService.class);
        session.when(DDDesktopAccessService::getInstance).thenReturn(manager);
        Mockito.when(manager.getAccessTokenForResource(account.resolve("/graphql"))).thenReturn("test-access-token");
    }

    @AfterEach
    void tearDown() {
        session.close();
        server.stop(0);
    }

    @Test
    void readsEncryptedKeyMaterialWithTheCurrentSsoToken() throws Exception {
        var state = new DDAccountClient(account).getCryptoState();

        Assertions.assertEquals("Bearer test-access-token", authorization.get());
        Assertions.assertEquals("POST", method.get());
        var body = JsonParser.parseString(posted.get()).getAsJsonObject();
        Assertions.assertEquals(Set.of("query"), body.keySet());
        Assertions.assertTrue(body.get("query").getAsString().contains("gatewayCryptoState"));
        Assertions.assertEquals(ACCOUNT_ID, state.accountId());
        Assertions.assertTrue(state.cryptoConfigured());
        Assertions.assertEquals("encrypted-bundle", state.encryptedBundle());
        Assertions.assertEquals(2L, state.generation());
        Assertions.assertEquals("salt", state.salt());
        Assertions.assertEquals(100000, state.iterations());
    }

    @Test
    void requiresSsoBeforeSendingAnyRequest() throws Exception {
        Mockito.when(manager.getAccessTokenForResource(account.resolve("/graphql"))).thenThrow(new DBException("Sign in to DataDam first"));
        Assertions.assertThrows(DBException.class, () -> new DDAccountClient(account).getCryptoState());
        Assertions.assertEquals(0, requests.get());
    }

    @Test
    void rejectsPartialDataWithGraphQlErrors() {
        var partial = JsonParser.parseString(CRYPTO_RESPONSE).getAsJsonObject();
        partial.add("errors", JsonParser.parseString("[{\"message\":\"Access denied\"}]"));
        response.set(partial.toString());
        Assertions.assertThrows(DBException.class, () -> new DDAccountClient(account).getCryptoState());
    }

    @Test
    void rejectsHttpErrorsAndDoesNotFollowRedirects() {
        for (int code : new int[] {401, 403, 307}) {
            status.set(code);
            int before = requests.get();
            Assertions.assertThrows(DBException.class, () -> new DDAccountClient(account).getCryptoState());
            Assertions.assertEquals(before + 1, requests.get());
        }
    }

    @Test
    void rejectsMissingOrMalformedCryptoState() {
        for (String invalid : List.of("null", "{\"data\":null}", "{\"data\":{\"gatewayCryptoState\":null}}",
            "{\"data\":{\"gatewayCryptoState\":{\"cryptoConfigured\":false}}}", "not-json")) {
            response.set(invalid);
            Assertions.assertThrows(DBException.class, () -> new DDAccountClient(account).getCryptoState());
        }
    }
}
