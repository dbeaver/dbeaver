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
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoLogin;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Timeout(20)
class DDDesktopRefreshTest {
    private static final String OLD_REFRESH = "a".repeat(43);
    private static final String NEW_REFRESH = "b".repeat(43);
    private static final URI RESOURCE = URI.create("https://storage.example.test/projects");
    private static final String SCOPE = "account:full_access";
    private final Gson gson = new Gson();
    private final AtomicReference<String> disk = new AtomicReference<>();
    private final AtomicReference<String> pending = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicBoolean loggedIn = new AtomicBoolean(true);
    private final AtomicBoolean lostResponse = new AtomicBoolean();
    private final AtomicBoolean rejectGrant = new AtomicBoolean();
    private final AtomicBoolean failSave = new AtomicBoolean();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicInteger revocations = new AtomicInteger();
    private final DBPPlatform platform = Mockito.mock(DBPPlatform.class);
    private MockedStatic<DBWorkbench> workbench;
    private HttpServer server;
    private Object store;
    private Object manager;
    private Method access;
    private Method load;
    private Method signOut;
    private String newAccess;

    @BeforeEach
    void setUp() throws Exception {
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(preferences.getBoolean("datadam.desktop.sso.logged-in")).thenAnswer(invocation -> loggedIn.get());
        Mockito.doAnswer(invocation -> {
            loggedIn.set(invocation.getArgument(1));
            return null;
        }).when(preferences).setValue(Mockito.eq("datadam.desktop.sso.logged-in"), Mockito.anyBoolean());
        workbench = Mockito.mockStatic(DBWorkbench.class);
        workbench.when(DBWorkbench::getPlatform).thenReturn(platform);

        DBSSecretController secrets = Mockito.mock(DBSSecretController.class);
        Mockito.when(secrets.getPrivateSecretValue(Mockito.anyString())).thenAnswer(invocation -> disk.get());
        Mockito.doAnswer(invocation -> {
            pending.set(invocation.getArgument(1));
            return null;
        }).when(secrets).setPrivateSecretValue(Mockito.anyString(), Mockito.any());
        Mockito.doAnswer(invocation -> {
            if (failSave.get() && pending.get() != null && pending.get().contains(NEW_REFRESH)) {
                throw new DBException("test secure-store failure");
            }
            disk.set(pending.get());
            return null;
        }).when(secrets).flushChanges();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final String issuer = "http://127.0.0.1:" + server.getAddress().getPort();
        newAccess = accessToken(Instant.now().plusSeconds(14_400));
        server.createContext("/token", exchange -> {
            requests.incrementAndGet();
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            Assertions.assertNull(gson.fromJson(disk.get(), DDDesktopUserToken.class).refreshToken());
            Assertions.assertNull(exchange.getRequestURI().getQuery());
            Assertions.assertNull(exchange.getRequestHeaders().getFirst("Authorization"));
            if (lostResponse.get()) {
                exchange.close();
                return;
            }
            byte[] response = (rejectGrant.get() ? "{\"error\":\"invalid_grant\"}" : gson.toJson(Map.of(
                "access_token", newAccess, "refresh_token", NEW_REFRESH, "scope", SCOPE,
                "token_type", "Bearer", "expires_in", 14_400))).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(rejectGrant.get() ? 400 : 200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.createContext("/revoke", exchange -> {
            revocations.incrementAndGet();
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        disk.set(gson.toJson(new DDDesktopUserToken(accessToken(Instant.now().minusSeconds(60)), issuer,
            Set.of("https://storage.example.test"), Set.of(SCOPE), OLD_REFRESH)));

        Class<?> storeType = Class.forName(DDDesktopUserToken.class.getPackageName() + ".DDDesktopTokenStore",
            true, DDDesktopUserToken.class.getClassLoader());
        var storeConstructor = storeType.getDeclaredConstructor(DBSSecretController.class);
        storeConstructor.setAccessible(true);
        store = storeConstructor.newInstance(secrets);
        manager = newManager();
        load.invoke(manager, RESOURCE);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        if (workbench != null) {
            workbench.close();
        }
    }

    @Test
    void concurrentAccessRotatesOnceAndRestartReadsTheNewPair() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> concurrentAccess(start));
            var second = executor.submit(() -> concurrentAccess(start));
            start.countDown();
            Assertions.assertEquals(newAccess, first.get(10, TimeUnit.SECONDS));
            Assertions.assertEquals(newAccess, second.get(10, TimeUnit.SECONDS));
        }
        Assertions.assertEquals(1, requests.get());
        DDDesktopUserToken saved = gson.fromJson(disk.get(), DDDesktopUserToken.class);
        Assertions.assertEquals(newAccess, saved.accessToken());
        Assertions.assertEquals(NEW_REFRESH, saved.refreshToken());
        Assertions.assertFalse(saved.toString().contains(NEW_REFRESH));
        Assertions.assertTrue(requestBody.get().contains("grant_type=refresh_token"));
        for (String forbidden : List.of("redirect_uri", "code_verifier", "client_secret", "access_token")) {
            Assertions.assertFalse(requestBody.get().contains(forbidden));
        }
        Object restarted = newManager();
        load.invoke(restarted, RESOURCE);
        Assertions.assertEquals(newAccess, access.invoke(restarted, RESOURCE));
        Assertions.assertEquals(1, requests.get());
    }

    @Test
    void lostResponseNeverRetriesTheOldRefresh() throws Exception {
        lostResponse.set(true);
        assertRefreshFailsAndCannotRetry();
        Assertions.assertEquals(1, requests.get());
    }

    @Test
    void invalidGrantRequiresSignInAgain() throws Exception {
        rejectGrant.set(true);
        assertRefreshFailsAndCannotRetry();
        Assertions.assertEquals(1, requests.get());
    }

    @Test
    void failedPairPersistenceNeverReturnsTheNewAccessOrReplaysTheOldRefresh() throws Exception {
        failSave.set(true);
        assertRefreshFailsAndCannotRetry();
        Assertions.assertEquals(1, requests.get());
    }

    @Test
    void logoutRevokesTheLatestRefreshAndClearsThePair() throws Exception {
        access.invoke(manager, RESOURCE);
        signOut.invoke(manager);
        Assertions.assertNull(disk.get());
        Assertions.assertFalse(loggedIn.get());
        Assertions.assertEquals(1, revocations.get());
        Assertions.assertTrue(requestBody.get().contains("token=" + NEW_REFRESH));
    }

    private void assertRefreshFailsAndCannotRetry() throws Exception {
        var failure = Assertions.assertThrows(InvocationTargetException.class, () -> access.invoke(manager, RESOURCE));
        Assertions.assertInstanceOf(DBException.class, failure.getCause());
        Assertions.assertFalse(failure.getCause().getMessage().contains(OLD_REFRESH));
        Assertions.assertFalse(loggedIn.get());
        Assertions.assertNull(disk.get());
        Object restarted = newManager();
        load.invoke(restarted, RESOURCE);
        Assertions.assertThrows(InvocationTargetException.class, () -> access.invoke(restarted, RESOURCE));
    }

    @NotNull
    private Object newManager() throws Exception {
        Class<?> type = Class.forName(DDDesktopUserToken.class.getPackageName() + ".DDDesktopTokenManager",
            true, DDDesktopUserToken.class.getClassLoader());
        var constructor = type.getDeclaredConstructor(store.getClass(), DDDesktopTokenValidator.class, DDDesktopSsoLogin.class);
        constructor.setAccessible(true);
        access = type.getMethod("getAccessTokenForResource", URI.class);
        load = type.getMethod("load", URI.class);
        signOut = type.getMethod("signOut");
        access.setAccessible(true);
        load.setAccessible(true);
        signOut.setAccessible(true);
        return constructor.newInstance(store, new DDDesktopTokenValidator(), new DDDesktopSsoLogin());
    }

    @NotNull
    private Object concurrentAccess(@NotNull CountDownLatch start) throws Exception {
        try (var localWorkbench = Mockito.mockStatic(DBWorkbench.class)) {
            localWorkbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            Assertions.assertTrue(start.await(10, TimeUnit.SECONDS));
            return access.invoke(manager, RESOURCE);
        }
    }

    @NotNull
    private String accessToken(@NotNull Instant expires) {
        String claims = gson.toJson(Map.of("exp", expires.getEpochSecond()));
        return "e30." + Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + ".eA";
    }
}
