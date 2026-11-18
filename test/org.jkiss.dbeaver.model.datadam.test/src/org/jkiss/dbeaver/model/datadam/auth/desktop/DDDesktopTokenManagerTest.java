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
package org.jkiss.dbeaver.model.datadam.auth.desktop;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoLogin;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Timeout(5)
class DDDesktopTokenManagerTest {
    private static final URI RESOURCE = URI.create("https://storage.example.test");
    private final AtomicReference<String> secret = new AtomicReference<>();
    private final AtomicReference<String> error = new AtomicReference<>();
    private final AtomicReference<Map<String, String>> form = new AtomicReference<>();
    private final AtomicReference<String> successor = new AtomicReference<>();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicInteger responseStatus = new AtomicInteger();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.now());
    private final Clock clock = Mockito.mock(Clock.class);
    private final DBSSecretController secrets = Mockito.mock(DBSSecretController.class);
    private final DDDesktopTokenStore store = new DDDesktopTokenStore(secrets);
    private HttpServer server;
    private String issuer;
    private MockedStatic<DBWorkbench> workbench;
    private MockedStatic<DDEndpoints> endpoints;
    private MockedStatic<GeneralUtils> productInfo;

    @BeforeEach
    void setUp() throws Exception {
        var preferences = new SimplePreferenceStore() {
            @Override
            public void save() {
            }
        };
        preferences.setValue("datadam.desktop.sso.logged-in", true);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(preferences);
        workbench = Mockito.mockStatic(DBWorkbench.class);
        workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
        productInfo = Mockito.mockStatic(GeneralUtils.class);
        productInfo.when(GeneralUtils::getProductUserAgent).thenReturn("DBeaver Test");
        Mockito.when(clock.instant()).thenAnswer(invocation -> now.get());
        Mockito.when(secrets.getPrivateSecretValue(Mockito.anyString())).thenAnswer(invocation -> secret.get());
        Mockito.doAnswer(invocation -> {
            secret.set(invocation.getArgument(1));
            return null;
        }).when(secrets).setPrivateSecretValue(Mockito.anyString(), Mockito.any());
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        issuer = "http://127.0.0.1:" + server.getAddress().getPort();
        endpoints = Mockito.mockStatic(DDEndpoints.class, Mockito.CALLS_REAL_METHODS);
        endpoints.when(DDEndpoints::getSsoBaseUrl).thenReturn(issuer);
        server.createContext("/token", exchange -> {
            requests.incrementAndGet();
            Map<String, String> parameters = parameters(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            form.set(parameters);
            int status = error.get() == null ? 200 : error.get().equals("invalid_grant") ? 400 : 503;
            if (responseStatus.get() != 0) {
                status = responseStatus.get();
            }
            Map<String, Object> body;
            if (status == 200) {
                successor.compareAndSet(null, parameters.get("next_refresh_token"));
                Assertions.assertEquals(successor.get(), parameters.get("next_refresh_token"));
                body = Map.of("access_token", accessToken(now.get().plusSeconds(3600)), "refresh_token", successor.get(),
                    "scope", "account:full_access", "token_type", "Bearer", "expires_in", 3600);
            } else {
                body = Map.of("error", error.get());
            }
            byte[] response = new Gson().toJson(body).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        store.save(new DDDesktopUserToken(accessToken(now.get().minusSeconds(60)), issuer,
            Set.of(RESOURCE.toString()), Set.of("account:full_access"), "A".repeat(43)));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        endpoints.close();
        productInfo.close();
        workbench.close();
    }

    @Test
    void serverFailurePreservesCredentialsAcrossRestartAndThenRefreshes() throws Exception {
        error.set("temporarily_unavailable");
        DDDesktopTokenManager manager = manager();
        manager.load(RESOURCE);
        Assertions.assertThrows(DBException.class, () -> manager.getAccessTokenForResource(RESOURCE));
        DDDesktopUserToken pending = store.load();
        Assertions.assertEquals("A".repeat(43), pending.refreshToken());
        Assertions.assertNotNull(pending.nextRefreshToken());
        Assertions.assertEquals(DDDesktopTokenManager.Status.SIGNED_IN, manager.getStatus());
        Assertions.assertEquals(now.get().plusMillis(1000), manager.getRefreshAt());
        Assertions.assertThrows(DBException.class, () -> manager.getAccessTokenForResource(RESOURCE));
        Assertions.assertEquals(1, requests.get());

        error.set(null);
        DDDesktopTokenManager restarted = manager();
        restarted.load(RESOURCE);
        Assertions.assertEquals(accessToken(now.get().plusSeconds(3600)), restarted.getAccessTokenForResource(RESOURCE));
        Assertions.assertEquals(pending.nextRefreshToken(), store.load().refreshToken());
        Assertions.assertNull(store.load().nextRefreshToken());
        Assertions.assertEquals(pending.nextRefreshToken(), form.get().get("next_refresh_token"));
    }

    @Test
    void connectionFailureDoesNotInvalidateSavedRefreshToken() throws Exception {
        server.stop(0);
        DDDesktopTokenManager manager = manager();
        manager.load(RESOURCE);
        Assertions.assertThrows(DBException.class, () -> manager.getAccessTokenForResource(RESOURCE));
        Assertions.assertEquals("A".repeat(43), store.load().refreshToken());
        Assertions.assertEquals(DDDesktopTokenManager.Status.SIGNED_IN, manager.getStatus());
    }

    @Test
    void expiredRefreshTokenRetainsTheReloginBehavior() throws Exception {
        error.set("invalid_grant");
        DDDesktopTokenManager manager = manager();
        manager.load(RESOURCE);
        Assertions.assertThrows(DBException.class, () -> manager.getAccessTokenForResource(RESOURCE));
        Assertions.assertEquals(DDDesktopTokenManager.Status.INVALID_TOKEN, manager.getStatus());
        Assertions.assertNull(store.load().refreshToken());
        DDDesktopTokenManager restarted = manager();
        restarted.load(RESOURCE);
        Assertions.assertEquals(DDDesktopTokenManager.Status.INVALID_TOKEN, restarted.getStatus());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500, 503})
    void otherHttpFailuresNeverRemoveSavedAuthorization(int status) throws Exception {
        error.set(status == 400 ? "invalid_request" : "invalid_grant");
        responseStatus.set(status);
        DDDesktopTokenManager manager = manager();
        manager.load(RESOURCE);
        Assertions.assertThrows(DBException.class, () -> manager.getAccessTokenForResource(RESOURCE));
        Assertions.assertEquals(DDDesktopTokenManager.Status.SIGNED_IN, manager.getStatus());
        Assertions.assertEquals("A".repeat(43), store.load().refreshToken());
    }

    @Test
    void persistenceFailureBeforeRotationDoesNotSendAnUnrecoverableRequest() throws Exception {
        DDDesktopTokenManager manager = manager();
        manager.load(RESOURCE);
        Mockito.doThrow(new DBException("Secret storage unavailable")).doNothing().when(secrets).flushChanges();
        Assertions.assertThrows(DBException.class, () -> manager.getAccessTokenForResource(RESOURCE));
        Assertions.assertEquals(0, requests.get());
        now.set(manager.getRefreshAt());
        manager.getAccessTokenForResource(RESOURCE);
        Assertions.assertEquals(1, requests.get());
        Assertions.assertEquals(successor.get(), store.load().refreshToken());
    }

    @Test
    void backoffIsBoundedAndSharedByAllTokenReads() throws Exception {
        error.set("server_error");
        DDDesktopTokenManager manager = manager();
        manager.load(RESOURCE);
        for (long delay : new long[] {1000, 2000, 4000, 8000, 16000, 32000, 60000, 60000}) {
            Assertions.assertThrows(DBException.class, () -> manager.getAccessTokenForResource(RESOURCE));
            Assertions.assertEquals(now.get().plusMillis(delay), manager.getRefreshAt());
            int count = requests.get();
            Assertions.assertThrows(DBException.class, () -> manager.getAccessTokenForResource(RESOURCE));
            Assertions.assertEquals(count, requests.get());
            now.set(manager.getRefreshAt());
        }
        error.set(null);
        manager.getAccessTokenForResource(RESOURCE);
        Assertions.assertEquals(9, requests.get());
    }

    @Test
    void failedPersistenceAfterRotationRetainsTheNewTokenAndRetriesSavingIt() throws Exception {
        DDDesktopTokenManager manager = manager();
        manager.load(RESOURCE);
        Mockito.doNothing().doThrow(new DBException("Secret storage unavailable")).doNothing().when(secrets).flushChanges();
        Assertions.assertThrows(DBException.class, () -> manager.getAccessTokenForResource(RESOURCE));
        Assertions.assertEquals(DDDesktopTokenManager.Status.SIGNED_IN, manager.getStatus());
        now.set(manager.getRefreshAt());
        Assertions.assertNotNull(manager.getAccessTokenForResource(RESOURCE));
        Assertions.assertEquals(successor.get(), store.load().refreshToken());
        Assertions.assertEquals(1, requests.get());
    }

    @Test
    void persistedPendingRotationIsReusedAfterItsResponseWasLost() throws Exception {
        DDDesktopTokenManager manager = manager();
        manager.load(RESOURCE);
        // A committed rotation whose response was lost leaves this durable request on the client.
        String next = "B".repeat(43);
        DDDesktopUserToken saved = store.load();
        store.save(new DDDesktopUserToken(saved.accessToken(), saved.issuer(), saved.endpoints(), saved.scopes(),
            saved.refreshToken(), next));
        successor.set(next);
        DDDesktopTokenManager restarted = manager();
        restarted.load(RESOURCE);
        restarted.getAccessTokenForResource(RESOURCE);
        Assertions.assertEquals(next, form.get().get("next_refresh_token"));
        Assertions.assertEquals(next, store.load().refreshToken());
    }

    @NotNull
    private DDDesktopTokenManager manager() {
        return new DDDesktopTokenManager(store, new DDDesktopTokenValidator(), new DDDesktopSsoLogin(), clock);
    }

    @NotNull
    private String accessToken(@NotNull Instant expires) {
        String claims = new Gson().toJson(Map.of("iss", issuer, "exp", expires.getEpochSecond()));
        return "e30." + Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + ".c2ln";
    }

    @NotNull
    private static Map<String, String> parameters(@NotNull String form) {
        Map<String, String> values = new HashMap<>();
        for (String parameter : form.split("&")) {
            String[] pair = parameter.split("=", 2);
            values.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        return values;
    }
}
