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

import com.dbeaver.datadam.sso.api.DDSsoConstants;
import com.dbeaver.datadam.sso.api.model.DDSsoDesktopTokenResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.HttpConstants;
import org.jkiss.utils.oauth.OAuthConstants;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** System-browser authorization code + PKCE; the loopback listener receives only a one-time code. */
public class DDDesktopSsoLogin {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TIMEOUT = Duration.ofMinutes(15);
    private static final Duration CANCEL_POLL_INTERVAL = Duration.ofMillis(100);
    private static final String CALLBACK_PATH = URI.create(DDSsoConstants.DESKTOP_CALLBACK_TEMPLATE).getPath();
    private static final int MAX_CALLBACK_QUERY_LENGTH = 4096;
    private static final String DONE_PAGE = """
        <html><body><h3>You are logged in</h3><p>Return to DBeaver.</p></body></html>
        """;


    @NotNull
    public DDSsoDesktopTokenResponse login(
        @NotNull URI issuer,
        @NotNull DBRProgressMonitor monitor,
        @NotNull BrowserOpener browser
    ) throws DBException, InterruptedException {
        String state = random();
        String verifier = random();
        String challenge;
        try {
            challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance(DDSsoConstants.ALGORITHM_SHA256).digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new DBException("PKCE SHA-256 unavailable", e);
        }
        CompletableFuture<String> callback = new CompletableFuture<>();
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new DBException("Cannot open loopback callback", e);
        }
        var executor = Executors.newSingleThreadExecutor();
        String redirectUri = "http://127.0.0.1:" + server.getAddress().getPort() + CALLBACK_PATH;
        server.createContext(CALLBACK_PATH, exchange -> receive(exchange, state, callback, server.getAddress().getPort()));
        server.setExecutor(executor);
        server.start();
        try (DDDesktopSsoClient client = new DDDesktopSsoClient(issuer)) {
            browser.open(client.authorizationUrl(redirectUri, state, challenge).toString());
            String code = awaitResult(callback, monitor);
            return client.exchange(code, verifier, redirectUri);
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private static void receive(@NotNull HttpExchange exchange, @NotNull String state, @NotNull CompletableFuture<String> result, int port)
        throws IOException {
        try {
            if (!HttpConstants.METHOD_GET.equals(exchange.getRequestMethod())
                || !CALLBACK_PATH.equals(exchange.getRequestURI().getRawPath())
                || !("127.0.0.1:" + port).equals(exchange.getRequestHeaders().getFirst(HttpConstants.HEADER_HOST))) {
                exchange.sendResponseHeaders(HttpConstants.CODE_BAD_REQUEST, -1);
                return;
            }
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            if (params == null || !state.equals(params.get(OAuthConstants.PARAM_STATE))) {
                exchange.sendResponseHeaders(HttpConstants.CODE_BAD_REQUEST, -1);
                return;
            }
            if (params.containsKey(OAuthConstants.PARAM_ERROR)) {
                exchange.sendResponseHeaders(HttpConstants.CODE_BAD_REQUEST, -1);
                result.completeExceptionally(new DBException("DataDam authorization failed"));
                return;
            }
            if (!DDSsoConstants.BASE64URL_256_PATTERN.matcher(params.getOrDefault(OAuthConstants.PARAM_CODE, "")).matches()) {
                exchange.sendResponseHeaders(HttpConstants.CODE_BAD_REQUEST, -1);
                return;
            }
            byte[] body = DONE_PAGE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_TEXT_HTML_UTF8);
            exchange.getResponseHeaders().set(HttpConstants.HEADER_CACHE_CONTROL, HttpConstants.CACHE_CONTROL_NO_STORE);
            exchange.getResponseHeaders().set(DDSsoConstants.HEADER_REFERRER_POLICY, DDSsoConstants.REFERRER_POLICY_NO_REFERRER);
            exchange.sendResponseHeaders(HttpConstants.CODE_OK, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
            result.complete(params.get(OAuthConstants.PARAM_CODE));
        } finally {
            exchange.close();
        }
    }

    @Nullable
    private static Map<String, String> parseQuery(@Nullable String query) {
        if (query == null || query.length() > MAX_CALLBACK_QUERY_LENGTH) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        try {
            for (String pair : query.split("&")) {
                int delimiter = pair.indexOf('=');
                if (delimiter < 0 || params.putIfAbsent(URLDecoder.decode(pair.substring(0, delimiter), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(delimiter + 1), StandardCharsets.UTF_8)) != null) {
                    return null;
                }
            }
            return params.size() == 2 ? params : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static void openBrowser(@NotNull String url) throws DBException {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new DBException("Cannot open web browser on this system");
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException e) {
            throw new DBException("Cannot open web browser", e);
        }
    }

    @NotNull
    private static String awaitResult(@NotNull CompletableFuture<String> result, @NotNull DBRProgressMonitor monitor)
        throws DBException, InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!monitor.isCanceled()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new DBException("Login was not completed in time");
            }
            try {
                return result.get(Math.min(remaining, CANCEL_POLL_INTERVAL.toNanos()), TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                // Poll the monitor while waiting for the browser callback.
            } catch (ExecutionException e) {
                throw new DBException("Login failed: " + e.getCause().getMessage(), e.getCause());
            }
        }
        throw new InterruptedException("Login was canceled");
    }

    @NotNull
    private static String random() {
        byte[] bytes = new byte[DDSsoConstants.OPAQUE_VALUE_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @FunctionalInterface
    public interface BrowserOpener {
        void open(@NotNull String url) throws DBException;
    }
}
