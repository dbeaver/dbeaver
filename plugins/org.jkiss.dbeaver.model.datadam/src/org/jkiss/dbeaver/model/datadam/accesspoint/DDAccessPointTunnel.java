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
package org.jkiss.dbeaver.model.datadam.accesspoint;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSyncCredentials;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLSocketFactory;

public class DDAccessPointTunnel implements DBWTunnel {

    private static final Log log = Log.getLog(DDAccessPointTunnel.class);

    public static final String PROP_AP_ID = "apId";
    private static final String LOCAL_HOST = DBConstants.HOST_LOCALHOST;

    // Same pref/env var as DDSyncPreferencePage (ui.datadam) - duplicated, model can't depend on ui.
    private static final String ENV_URL = "DATADAM_URL";
    private static final String PREF_SERVER_URL = "datadam.server-url";
    private static final int GATEWAY_PORT = 9000;
    private static final String SERVER_TIME_HEADER = "X-DD-Server-Time";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private final List<Runnable> closeListeners = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Set<Closeable> activeConnections = ConcurrentHashMap.newKeySet();

    private ServerSocketChannel localServer;
    private volatile boolean closed;

    private DDSyncCredentials credentials;
    private String gatewayUrl;
    private String apId;
    private String target;

    @NotNull
    @Override
    public DBPConnectionConfiguration initializeHandler(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull DBPConnectionConfiguration info
    ) throws DBException, IOException {
        log.info("DDAccessPointTunnel.initializeHandler called, enabled=" + configuration.isEnabled());
        apId = configuration.getStringProperty(PROP_AP_ID);
        if (CommonUtils.isEmpty(apId)) {
            throw new DBException("Access Point name is not configured");
        }

        DDKeyBundle bundle = DDKeyStore.load();
        if (bundle == null) {
            throw new DBException(
                "Not logged in to the DataDam Gateway - log in first (Synchronize menu), then retry the connection");
        }
        credentials = new DDBundleCredentials(bundle);

        gatewayUrl = getGatewayUrl();
        if (CommonUtils.isEmpty(gatewayUrl)) {
            throw new DBException("DataDam Gateway URL is not configured");
        }

        target = info.getHostName() + ":" + info.getHostPort();

        localServer = ServerSocketChannel.open();
        localServer.bind(new InetSocketAddress(LOCAL_HOST, 0));
        int localPort = ((InetSocketAddress) localServer.getLocalAddress()).getPort();

        executor.submit(this::acceptLoop);

        info.setHostName(LOCAL_HOST);
        info.setHostPort(String.valueOf(localPort));
        info.setUrl(configuration.getDataSource().getDriver().getConnectionURL(info));
        log.info("DDAccessPointTunnel: rewritten to " + info.getHostName() + ":" + info.getHostPort() + ", url=" + info.getUrl());
        return info;
    }

    private void acceptLoop() {
        while (!closed) {
            SocketChannel local;
            try {
                local = localServer.accept();
            } catch (IOException e) {
                if (!closed) {
                    log.error("Access Point " + apId + ": accept failed", e);
                }
                return;
            }
            executor.submit(() -> bridge(local));
        }
    }

    private void bridge(@NotNull SocketChannel local) {
        activeConnections.add(local);
        try {
            BridgeTicket ticket = requestBridgeTicket();
            Socket bridgeSocket = openBridgeSocket(ticket);
            activeConnections.add(bridgeSocket);
            try {
                performUpgrade(bridgeSocket, ticket.token());
                splice(local, bridgeSocket);
            } finally {
                activeConnections.remove(bridgeSocket);
                bridgeSocket.close();
            }
        } catch (Exception e) {
            log.error("Access Point " + apId + ": bridge failed", e);
        } finally {
            activeConnections.remove(local);
            try {
                local.close();
            } catch (IOException ignored) {
                // no op
            }
        }
    }

    @NotNull
    private BridgeTicket requestBridgeTicket() throws Exception {
        byte[] bodyBytes = ("{\"apId\":" + JSONUtils.GSON.toJson(apId) + ",\"target\":" + JSONUtils.GSON.toJson(target) + "}")
            .getBytes(StandardCharsets.UTF_8);
        URI uri = URI.create(gatewayUrl + "/ap/request");

        HttpResponse<String> response = sendSignedRequest(uri, bodyBytes);
        String serverTime = response.headers().firstValue(SERVER_TIME_HEADER).orElse(null);
        if (serverTime != null) {
            credentials.updateServerTime(Long.parseLong(serverTime));
            response = sendSignedRequest(uri, bodyBytes);
        }
        if (response.statusCode() != 200) {
            throw new DBException("Access Point bridge request failed: HTTP " + response.statusCode() + " " + response.body());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> json = JSONUtils.GSON.fromJson(response.body(), Map.class);
        String token = (String) json.get("token");
        String bridgeAddr = (String) json.get("bridgeAddr");
        int port = ((Number) json.get("port")).intValue();
        return new BridgeTicket(token, bridgeAddr, port);
    }

    @NotNull
    private HttpResponse<String> sendSignedRequest(@NotNull URI uri, @NotNull byte[] bodyBytes) throws Exception {
        String pathAndQuery = uri.getRawQuery() == null ? uri.getRawPath() : uri.getRawPath() + "?" + uri.getRawQuery();
        String bearer = credentials.buildToken("POST", pathAndQuery, bodyBytes);
        return HTTP.send(
            HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearer)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    @NotNull
    private Socket openBridgeSocket(@NotNull BridgeTicket ticket) throws IOException {
        boolean explicit = !CommonUtils.isEmpty(ticket.bridgeAddr());
        String addr = explicit ? ticket.bridgeAddr() : URI.create(gatewayUrl).getHost() + ":" + ticket.port();
        boolean tls = explicit ? addr.startsWith("https://") : gatewayUrl.startsWith("https://");
        if (addr.contains("://")) {
            addr = addr.substring(addr.indexOf("://") + 3);
        }
        int sep = addr.lastIndexOf(':');
        String host = addr.substring(0, sep);
        int port = Integer.parseInt(addr.substring(sep + 1));
        return tls ? SSLSocketFactory.getDefault().createSocket(host, port) : new Socket(host, port);
    }

    // Matches accesspoint/server/bridge.go's upgrade. A BufferedReader would over-read past the
    // headers and swallow bridged bytes, so this reads one byte at a time instead.
    private void performUpgrade(@NotNull Socket bridgeSocket, @NotNull String token) throws IOException {
        String host = bridgeSocket.getInetAddress().getHostName();
        String request = "GET /ap/bridge?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) + " HTTP/1.1\r\n"
            + "Host: " + host + "\r\n"
            + "Connection: Upgrade\r\n"
            + "Upgrade: dd-ap-bridge\r\n"
            + "\r\n";
        bridgeSocket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));

        String statusLine = readLineRaw(bridgeSocket.getInputStream());
        if (statusLine == null || !statusLine.contains("101")) {
            throw new IOException("Access Point bridge upgrade failed: " + statusLine);
        }
        String header;
        while ((header = readLineRaw(bridgeSocket.getInputStream())) != null && !header.isEmpty()) {
            // drain the rest of the 101 response headers before switching to raw piping
        }
    }

    private void splice(@NotNull SocketChannel local, @NotNull Socket bridgeSocket) throws IOException {
        Socket localSocket = local.socket();
        Thread outbound = new Thread(() -> pipe(localSocket, bridgeSocket), "DataDam AP bridge " + apId + " out");
        outbound.start();
        pipe(bridgeSocket, localSocket);
        try {
            outbound.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void pipe(@NotNull Socket from, @NotNull Socket to) {
        try {
            from.getInputStream().transferTo(to.getOutputStream());
        } catch (IOException ignored) {
            // one side closed - normal end of a bridged session
        } finally {
            try {
                if (!to.isClosed()) {
                    to.shutdownOutput();
                }
            } catch (IOException ignored) {
                // no op
            }
        }
    }

    @Nullable
    private static String readLineRaw(@NotNull InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1 && b != '\n') {
            if (b != '\r') {
                line.write(b);
            }
        }
        if (b == -1 && line.size() == 0) {
            return null;
        }
        return line.toString(StandardCharsets.US_ASCII);
    }

    @NotNull
    private static String getServerUrl() {
        String url = DBWorkbench.getPlatform().getPreferenceStore().getString(PREF_SERVER_URL);
        return CommonUtils.isEmpty(url) ? CommonUtils.notEmpty(System.getenv(ENV_URL)) : url;
    }

    @NotNull
    private static String getGatewayUrl() {
        String url = getServerUrl();
        if (CommonUtils.isEmpty(url)) {
            return url;
        }
        String normalized = CommonUtils.removeTrailingSlash(url);
        try {
            URI uri = URI.create(normalized);
            if (uri.getHost() != null && uri.getPort() == -1) {
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), GATEWAY_PORT,
                    uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
            }
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            // fall through to the un-ported URL below
        }
        return normalized;
    }

    @Override
    public void invalidateHandler(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull DBCInvalidatePhase phase
    ) {
        // Nothing to refresh - unlike UDBT's WS/LP transport, this handler holds no session:
        // every accepted local connection signs and requests its own bridge token fresh.
    }

    @Override
    public void closeTunnel(@NotNull DBRProgressMonitor monitor) {
        if (closed) {
            return;
        }
        closed = true;
        closeListeners.forEach(Runnable::run);
        closeListeners.clear();
        if (localServer != null) {
            try {
                localServer.close();
            } catch (IOException e) {
                log.debug("Access Point " + apId + ": error closing local listener", e);
            }
        }
        for (Closeable connection : activeConnections) {
            try {
                connection.close();
            } catch (IOException ignored) {
                // no op
            }
        }
        executor.shutdownNow();
    }

    @Override
    public void addCloseListener(@NotNull Runnable listener) {
        closeListeners.add(listener);
    }

    @Override
    public boolean matchesParameters(@NotNull String host, int port) {
        return false;
    }

    @NotNull
    @Override
    public AuthCredentials getRequiredCredentials(@NotNull DBWHandlerConfiguration configuration) {
        return AuthCredentials.NONE;
    }

    @Nullable
    @Override
    public Object getImplementation() {
        return this;
    }

    private record BridgeTicket(@NotNull String token, String bridgeAddr, int port) {
    }
}
