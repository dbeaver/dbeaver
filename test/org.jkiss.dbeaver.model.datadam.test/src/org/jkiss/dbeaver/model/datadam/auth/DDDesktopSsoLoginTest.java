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
package org.jkiss.dbeaver.model.datadam.auth;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Timeout(10)
class DDDesktopSsoLoginTest {
    private final AtomicReference<String> form = new AtomicReference<>();
    private final AtomicReference<String> tokenRedirect = new AtomicReference<>();
    private final AtomicInteger redirectedRequests = new AtomicInteger();
    private final DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
    private HttpServer sso;
    private URI issuer;

    @BeforeEach
    void startSso() throws IOException {
        DDDesktopSsoSession.logout();
        sso = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        issuer = URI.create("http://127.0.0.1:" + sso.getAddress().getPort());
        sso.createContext("/sso/desktop/config", exchange -> {
            byte[] response = new Gson().toJson(Map.of("issuer", issuer.toString())).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        sso.createContext("/token", exchange -> {
            Assertions.assertNull(exchange.getRequestHeaders().getFirst("Authorization"));
            form.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (tokenRedirect.get() != null) {
                exchange.getResponseHeaders().set("Location", tokenRedirect.get());
                exchange.sendResponseHeaders(307, -1);
                exchange.close();
                return;
            }
            byte[] response = new Gson().toJson(Map.of("access_token", "access-value",
                "id_token", "identity-value", "token_type", "Bearer", "expires_in", 86_400)).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        sso.createContext("/redirected-token", exchange -> {
            redirectedRequests.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        sso.start();
    }

    @AfterEach
    void stopSso() {
        sso.stop(0);
        DDDesktopSsoSession.logout();
    }

    @Test
    void usesLoopbackPkceAndExchangesCodeWithoutBackendCredentials() throws Exception {
        AtomicReference<Map<String, String>> authorization = new AtomicReference<>();
        var result = new DDDesktopSsoLogin().login(issuer, monitor, url -> {
            URI opened = URI.create(url);
            Map<String, String> params = parameters(opened.getRawQuery());
            authorization.set(params);
            Assertions.assertEquals("/authorize", opened.getPath());
            Assertions.assertEquals("dbeaver-desktop", params.get("client_id"));
            URI callback = URI.create(params.get("redirect_uri"));
            Assertions.assertEquals("127.0.0.1", callback.getHost());
            Assertions.assertTrue(callback.getPort() > 0);
            String query = "?code=" + "B".repeat(43) + "&state=" + params.get("state");
            Assertions.assertEquals(400, get(callback + query + "&state=other"));
            Assertions.assertEquals(400, get(callback + "/other" + query));
            Assertions.assertEquals(400, get(callback + "?code=" + "B".repeat(43) + "&state=wrong"));
            Assertions.assertEquals(200, get(callback + query));
        });
        Assertions.assertEquals("access-value", result.accessToken());
        Map<String, String> exchange = parameters(form.get());
        Assertions.assertEquals("B".repeat(43), exchange.get("code"));
        Assertions.assertEquals(authorization.get().get("redirect_uri"), exchange.get("redirect_uri"));
        Assertions.assertEquals("S256", authorization.get().get("code_challenge_method"));
        Assertions.assertEquals(authorization.get().get("code_challenge"), Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(exchange.get("code_verifier").getBytes(StandardCharsets.US_ASCII))));
        Assertions.assertFalse(exchange.containsKey("client_secret"));
        Assertions.assertFalse(exchange.containsKey("refresh_token"));
    }

    @Test
    void doesNotFollowTokenEndpointRedirects() {
        tokenRedirect.set(issuer + "/redirected-token");
        Assertions.assertThrows(DBException.class, () -> new DDDesktopSsoLogin().login(issuer, monitor, this::complete));
        Assertions.assertEquals(0, redirectedRequests.get());
    }

    @Test
    void sessionAuthorizesAccountAndStorageOnlyUntilLogout() throws Exception {
        Assertions.assertTrue(DDDesktopSsoSession.needsLogin());
        DDDesktopSsoSession.login(issuer, URI.create("https://storage.example.test:443"), monitor, this::complete);
        Assertions.assertFalse(DDDesktopSsoSession.needsLogin());
        Assertions.assertEquals("access-value", DDDesktopSsoSession.accessFor(issuer.resolve("/graphql?query=account")));
        Assertions.assertEquals("access-value", DDDesktopSsoSession.accessFor(URI.create("https://storage.example.test/graphql")));
        Assertions.assertThrows(DBException.class, () -> DDDesktopSsoSession.accessFor(URI.create("https://other.example.test/graphql")));
        DDDesktopSsoSession.logout();
        Assertions.assertFalse(DDDesktopSsoSession.hasLogin());
        Assertions.assertThrows(DBException.class, () -> DDDesktopSsoSession.accessFor(issuer));
    }

    @Test
    void lateBrowserCallbackCannotRestoreSessionAfterLogout() {
        Assertions.assertThrows(InterruptedException.class, () -> DDDesktopSsoSession.login(
            issuer, URI.create("https://storage.example.test"), monitor, url -> {
                DDDesktopSsoSession.logout();
                complete(url);
            }));
        Assertions.assertFalse(DDDesktopSsoSession.hasLogin());
    }

    @Test
    void cancellationClosesLoopbackListenerWithoutExchangingToken() {
        AtomicReference<URI> callback = new AtomicReference<>();
        Assertions.assertThrows(InterruptedException.class, () -> new DDDesktopSsoLogin().login(issuer, monitor, url -> {
            callback.set(URI.create(parameters(URI.create(url).getRawQuery()).get("redirect_uri")));
            Mockito.when(monitor.isCanceled()).thenReturn(true);
        }));
        Assertions.assertNull(form.get());
        Assertions.assertThrows(DBException.class, () -> get(callback.get().toString()));
    }

    @Test
    void authorizationErrorCompletesLoginInsteadOfWaitingForTimeout() {
        Assertions.assertThrows(DBException.class, () -> new DDDesktopSsoLogin().login(issuer, monitor, url -> {
            Map<String, String> params = parameters(URI.create(url).getRawQuery());
            Assertions.assertEquals(400, get(params.get("redirect_uri") + "?error=access_denied&state=" + params.get("state")));
        }));
        Assertions.assertNull(form.get());
    }

    private void complete(@NotNull String url) throws DBException {
        Map<String, String> params = parameters(URI.create(url).getRawQuery());
        Assertions.assertEquals(200, get(params.get("redirect_uri") + "?code=" + "B".repeat(43) + "&state=" + params.get("state")));
    }

    private static int get(@NotNull String url) throws DBException {
        try (HttpClient browser = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).GET().build();
            return browser.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException | InterruptedException e) {
            throw new DBException("Browser callback failed", e);
        }
    }

    @NotNull
    private static Map<String, String> parameters(@NotNull String query) {
        Map<String, String> result = new HashMap<>();
        for (String part : query.split("&")) {
            String[] entry = part.split("=", 2);
            result.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8), URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
        }
        return result;
    }
}
