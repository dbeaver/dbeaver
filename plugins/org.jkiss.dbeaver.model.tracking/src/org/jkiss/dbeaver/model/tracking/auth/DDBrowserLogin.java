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
package org.jkiss.dbeaver.model.tracking.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Logs in through the browser. The site posts the crypto state back to a local port.
 */
public class DDBrowserLogin {

    private static final Log log = Log.getLog(DDBrowserLogin.class);

    private static final String CALLBACK_PATH = "/callback";
    private static final String LOGIN_PATH = "/index.html";
    private static final int STATE_SIZE_BYTES = 32;
    private static final int MAX_BODY_SIZE = 64 * 1024;
    private static final Duration TIMEOUT = Duration.ofMinutes(3);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String DONE_PAGE = """
        <html><body><h3>You are logged in</h3><p>Return to DBeaver.</p></body></html>
        """;

    private final String siteUrl;

    public DDBrowserLogin(@NotNull String siteUrl) {
        this.siteUrl = CommonUtils.removeTrailingSlash(siteUrl);
    }

    @NotNull
    public DDCryptoState login() throws DBException {
        String state = randomState();
        CompletableFuture<Map<String, String>> result = new CompletableFuture<>();

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new DBException("Cannot open local port for login", e);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        server.createContext(CALLBACK_PATH, exchange -> handleCallback(exchange, state, result));
        server.setExecutor(executor);
        server.start();
        try {
            int port = server.getAddress().getPort();
            openBrowser(buildLoginUrl(port, state));
            return toCryptoState(result.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            throw new DBException("Login was not completed in time", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DBException("Login was interrupted", e);
        } catch (ExecutionException e) {
            throw new DBException("Login failed: " + e.getCause().getMessage(), e.getCause());
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private void handleCallback(
        @NotNull HttpExchange exchange,
        @NotNull String expectedState,
        @NotNull CompletableFuture<Map<String, String>> result
    ) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY_SIZE);
            Map<String, String> form = parseForm(new String(body, StandardCharsets.UTF_8));
            if (!Objects.equals(expectedState, form.get("state"))) {
                log.debug("Skip login callback with unexpected state");
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            byte[] page = DONE_PAGE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, page.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(page);
            }
            result.complete(form);
        } finally {
            exchange.close();
        }
    }

    @NotNull
    private String buildLoginUrl(int port, @NotNull String state) {
        String redirectUri = "http://127.0.0.1:" + port + CALLBACK_PATH;
        return siteUrl + LOGIN_PATH
            + "?redirectUri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
            + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
    }

    private static void openBrowser(@NotNull String url) throws DBException {
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
    private static DDCryptoState toCryptoState(@NotNull Map<String, String> form) throws DBException {
        String accountId = form.get("accountId");
        if (CommonUtils.isEmpty(accountId)) {
            throw new DBException("Login response has no account");
        }
        return new DDCryptoState(
            accountId,
            CommonUtils.toBoolean(form.get("cryptoConfigured")),
            form.get("encryptedBundle"),
            CommonUtils.isEmpty(form.get("generation")) ? null : CommonUtils.toLong(form.get("generation"))
        );
    }

    @NotNull
    private static Map<String, String> parseForm(@NotNull String body) {
        Map<String, String> values = new HashMap<>();
        for (String pair : body.split("&")) {
            int index = pair.indexOf('=');
            if (index <= 0) {
                continue;
            }
            values.put(
                URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8),
                URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8));
        }
        return values;
    }

    @NotNull
    private static String randomState() {
        byte[] bytes = new byte[STATE_SIZE_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
