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

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamSettingKeys;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        "apiKeyId":"00000000-0000-0000-0000-000000000001",
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
        var state = client().getCryptoState();

        Assertions.assertEquals("Bearer test-access-token", authorization.get());
        Assertions.assertEquals("POST", method.get());
        var body = JsonParser.parseString(posted.get()).getAsJsonObject();
        Assertions.assertEquals(Set.of("query"), body.keySet());
        Assertions.assertTrue(body.get("query").getAsString().contains("gatewayCryptoState"));
        Assertions.assertEquals(ACCOUNT_ID, state.accountId());
        Assertions.assertTrue(state.cryptoConfigured());
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), state.apiKeyId());
        Assertions.assertEquals("encrypted-bundle", state.encryptedBundle());
        Assertions.assertEquals(2L, state.generation());
        Assertions.assertEquals("salt", state.salt());
        Assertions.assertEquals(100000, state.iterations());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void readsSettingsAndWritesThroughAccountWithIdentityGuard(boolean canManage) throws Exception {
        var client = client();
        response.set("""
            {"data":{"accountInfo":{"accountId":"8decb064-2709-4914-b6b6-68eaef98cac3"},
            "damSettings":{"account":{"mcp.enabled":"false"},"dam":{"mcp.enabled":"true"}},"canManageSettings":%s}}
            """.formatted(canManage));
        var state = client.getDamSettings("desktop");
        Assertions.assertEquals(ACCOUNT_ID, state.accountId());
        Assertions.assertEquals(canManage, state.canManage());
        var request = JsonParser.parseString(posted.get()).getAsJsonObject();
        Assertions.assertEquals(Set.of("clientDamId"), request.getAsJsonObject("variables").keySet());
        Assertions.assertTrue(request.get("query").getAsString().contains("canManageSettings"));
        Assertions.assertFalse(request.get("query").getAsString().contains("$key"));
        Assertions.assertFalse(state.settings().isEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true, false));
        Assertions.assertTrue(state.settings().isDamEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, false));
        response.set("""
            {"data":{"setDamSetting":{"account":{"mcp.enabled":"false"},"dam":{"mcp.enabled":"false"}}}}
            """);
        Assertions.assertFalse(client.setDamSetting("desktop", DDRemoteDamSettingKeys.MCP_ENABLED, "false", ACCOUNT_ID)
            .isDamEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true));
        var variables = JsonParser.parseString(posted.get()).getAsJsonObject().getAsJsonObject("variables");
        Assertions.assertEquals(ACCOUNT_ID.toString(), variables.get("expectedAccountId").getAsString());
        Assertions.assertEquals("desktop", variables.get("clientDamId").getAsString());
        Assertions.assertEquals("mcp.enabled", variables.get("key").getAsString());
        Assertions.assertEquals("Bearer test-access-token", authorization.get());
    }

    @Test
    void readsRenamePermissionFromAccount() throws Exception {
        var client = client();
        response.set("{\"data\":{\"myPermissions\":[\"ACCOUNT_READ\",\"REMOTE_DAM_MANAGE\"]}}");
        Assertions.assertTrue(client.canRenameDam());

        response.set("{\"data\":{\"myPermissions\":[\"ACCOUNT_READ\"]}}");
        Assertions.assertFalse(client.canRenameDam());

        response.set("{\"data\":{\"myPermissions\":null}}");
        Assertions.assertThrows(DBException.class, client::canRenameDam);
    }

    @Test
    void rejectsMissingSettingsAndGraphQlErrors() {
        var client = client();
        for (String invalid : List.of("null", "{\"data\":{}}", "{\"data\":null}",
            "{\"errors\":[{\"message\":\"Access denied\"}],\"data\":{\"setDamSetting\":{\"account\":{},\"dam\":{}}}}")) {
            response.set(invalid);
            Assertions.assertThrows(DBException.class, () -> client.getDamSettings("desktop"));
            Assertions.assertThrows(DBException.class,
                () -> client.setDamSetting("desktop", DDRemoteDamSettingKeys.MCP_ENABLED, "true", ACCOUNT_ID));
        }
    }

    @Test
    void requiresSsoBeforeSendingAnyRequest() throws Exception {
        Mockito.when(manager.getAccessTokenForResource(account.resolve("/graphql"))).thenThrow(new DBException("Sign in to DataDam first"));
        Assertions.assertThrows(DBException.class, () -> client().getCryptoState());
        Assertions.assertEquals(0, requests.get());
    }

    @Test
    void rejectsPartialDataWithGraphQlErrors() {
        var partial = JsonParser.parseString(CRYPTO_RESPONSE).getAsJsonObject();
        partial.add("errors", JsonParser.parseString("[{\"message\":\"Access denied\"}]"));
        response.set(partial.toString());
        Assertions.assertThrows(DBException.class, () -> client().getCryptoState());
    }

    @Test
    void rejectsHttpErrorsAndDoesNotFollowRedirects() {
        for (int code : new int[] {401, 403, 307}) {
            status.set(code);
            int before = requests.get();
            Assertions.assertThrows(DBException.class, () -> client().getCryptoState());
            Assertions.assertEquals(before + 1, requests.get());
        }
    }

    @Test
    void rejectsMissingOrMalformedCryptoState() {
        for (String invalid : List.of("null", "{\"data\":null}", "{\"data\":{\"gatewayCryptoState\":null}}",
            "{\"data\":{\"gatewayCryptoState\":{\"cryptoConfigured\":false}}}", "not-json")) {
            response.set(invalid);
            Assertions.assertThrows(DBException.class, () -> client().getCryptoState());
        }
    }

    @NotNull
    private DDAccountClient client() {
        return new DDAccountClient(account) {
            @NotNull
            @Override
            protected String getUserAgent() {
                return "DataDam Account test";
            }
        };
    }

}
