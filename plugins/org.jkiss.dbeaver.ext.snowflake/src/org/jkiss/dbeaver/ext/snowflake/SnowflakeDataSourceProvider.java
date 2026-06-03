/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.snowflake;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.ext.snowflake.model.SnowflakeDataSource;
import org.jkiss.dbeaver.ext.snowflake.model.SnowflakeMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class SnowflakeDataSourceProvider extends GenericDataSourceProvider<SnowflakeDataSource> {

    private static final Map<String, String> tokenCache = new LinkedHashMap<>();
    private static final Map<String, Long> tokenFetchTimes = new LinkedHashMap<>();
    private static final long CACHE_DURATION_MS = 180000; // 3 minutes

    public SnowflakeDataSourceProvider() {
        super(SnowflakeDataSource.class);
    }

    protected SnowflakeDataSourceProvider(@NotNull Class<? extends SnowflakeDataSource> dsClass) {
        super(dsClass);
    }

    @Override
    public long getFeatures()
    {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @NotNull
    @Override
    public String getConnectionURL(@NotNull DBPDriver driver, @NotNull DBPConnectionConfiguration connectionInfo)
    {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:snowflake://").append(connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":").append(connectionInfo.getHostPort());
        }
        url.append("/?");

        String warehouse = connectionInfo.getServerName();
        if (CommonUtils.isEmpty(warehouse)) {
            warehouse = connectionInfo.getProviderProperty(SnowflakeConstants.PROP_WAREHOUSE);
        }
        String schemaName = connectionInfo.getProviderProperty(SnowflakeConstants.PROP_SCHEMA);
        if (CommonUtils.isEmpty(schemaName)) {
            schemaName = connectionInfo.getProviderProperty(SnowflakeConstants.PROP_SCHEMA2);
        }

        boolean hasParam = addParameter(url, "db", connectionInfo.getDatabaseName(), false);
        hasParam = addParameter(url, "warehouse", warehouse, hasParam);
        hasParam = addParameter(url, "schema", schemaName, hasParam);

        // Backward compatibility
        hasParam = addParameter(url, "role", connectionInfo.getProviderProperty(SnowflakeConstants.PROP_ROLE_LEGACY), hasParam);

        // --- OAUTH URL INJECTOR ---
        Map<String, String> properties = connectionInfo.getProperties();
        String tokenEndpoint = properties.get("oauth.token.endpoint");
        
        if (tokenEndpoint == null || tokenEndpoint.isEmpty()) {
            tokenEndpoint = properties.get("oauthTokenEndpoint");
        }

        if (tokenEndpoint != null && !tokenEndpoint.isEmpty()) {
            try {
                // Determine username early to construct a unique cache key per user
                String uUsername = properties.get("oauth.username");
                if (uUsername == null || uUsername.isEmpty()) {
                    uUsername = connectionInfo.getUserName();
                }
                if (uUsername == null) {
                    uUsername = "";
                }

                // Append username to cache key to prevent cross-account token pollution
                String cacheKey = connectionInfo.getHostName() + ":" + uUsername + ":" + tokenEndpoint;
                String accessToken = null;

                synchronized (tokenCache) {
                    Long fetchTime = tokenFetchTimes.get(cacheKey);
                    if (fetchTime != null && (System.currentTimeMillis() - fetchTime) < CACHE_DURATION_MS) {
                        accessToken = tokenCache.get(cacheKey);
                    }
                }

                if (accessToken == null || accessToken.isEmpty()) {
                    String uClientId = properties.get("oauth.client.id");
                    if (uClientId == null) uClientId = properties.get("oauthClientId");
                    if (uClientId == null) uClientId = ""; 

                    String uClientSecret = properties.get("oauth.client.secret");
                    if (uClientSecret == null) uClientSecret = properties.get("oauthClientSecret");
                    if (uClientSecret == null) uClientSecret = ""; 

                    String uScope = properties.get("oauth.scope");
                    if (uScope == null) uScope = properties.get("oauth.scopes");
                    if (uScope == null) uScope = properties.get("scope");
                    if (uScope == null) uScope = "session:role-any";

                    String uPassword = properties.get("oauth.password");
                    if (uPassword == null || uPassword.isEmpty()) {
                        System.err.println("[SSO ERROR] Missing 'oauth.password' in Driver Properties.");
                    } else {
                        System.out.println("[SSO DEBUG] Fetching token for URL injection. User: " + uUsername);
                        accessToken = fetchOAuthToken(tokenEndpoint, uClientId, uClientSecret, uUsername, uPassword, uScope);
                        accessToken = accessToken.trim().replace("\n", "").replace("\r", "");

                        synchronized (tokenCache) {
                            tokenCache.put(cacheKey, accessToken);
                            tokenFetchTimes.put(cacheKey, System.currentTimeMillis());
                        }
                    }
                }

                if (accessToken != null && !accessToken.isEmpty()) {
                    hasParam = addParameter(url, "authenticator", "oauth", hasParam);
                    hasParam = addParameter(url, "token", URLEncoder.encode(accessToken, "UTF-8"), hasParam);
                    
                    if (!uUsername.isEmpty()) {
                        hasParam = addParameter(url, "user", URLEncoder.encode(uUsername, "UTF-8"), hasParam);
                    }
                    
                    // Clear passwords to prevent conflicts
                    connectionInfo.setUserPassword(null);
                    properties.remove("password");
                    properties.remove("PASSWORD");
                    
                    System.out.println("[SSO DEBUG] Token directly embedded into the JDBC URL.");
                }
            } catch (Exception e) {
                System.err.println("[SSO ERROR] Token acquisition failed: " + e.getMessage());
            }
        }

        return url.toString();
    }

    private static boolean addParameter(StringBuilder url, String name, String value, boolean hasParam) {
        if (!CommonUtils.isEmpty(value)) {
            if (hasParam) url.append("&");
            url.append(name).append("=").append(value);
            return true;
        }
        return hasParam;
    }

    @NotNull
    @Override
    public SnowflakeDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new SnowflakeDataSource(monitor, container, new SnowflakeMetaModel());
    }

    private String fetchOAuthToken(
        String endpoint, 
        String cId, 
        String cSec, 
        String user, 
        String pass, 
        String sc) throws Exception 
    {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(12000);

        Map<String, String> bodyMap = new LinkedHashMap<>();
        bodyMap.put("grant_type", "password");
        bodyMap.put("client_id", cId != null ? cId : "");
        bodyMap.put("client_secret", cSec != null ? cSec : "");
        bodyMap.put("username", user);
        bodyMap.put("password", pass);
        bodyMap.put("scope", sc != null ? sc : "");

        StringBuilder dataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : bodyMap.entrySet()) {
            if (dataBuilder.length() != 0) {
                dataBuilder.append('&');
            }
            dataBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8").replace("+", "%20"));
            dataBuilder.append('=');
            String val = entry.getValue();
            String encodedVal = URLEncoder.encode(val, "UTF-8").replace("+", "%20");
            dataBuilder.append(encodedVal);
        }

        byte[] postBytes = dataBuilder.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = conn.getOutputStream()) {
            outputStream.write(postBytes);
            outputStream.flush();
        }

        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 350) ? conn.getInputStream() : conn.getErrorStream();

        ByteArrayOutputStream responseOutput = new ByteArrayOutputStream();
        byte[] readBuffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = stream.read(readBuffer)) != -1) {
            responseOutput.write(readBuffer, 0, bytesRead);
        }
        
        String responseRaw = responseOutput.toString(StandardCharsets.UTF_8.name());

        if (code < 200 || code >= 350) {
            throw new Exception("IDP server returned HTTP " + code + ": " + responseRaw);
        }

        int tokenIndex = responseRaw.indexOf("access_token");
        if (tokenIndex != -1) {
            int colonIndex = responseRaw.indexOf(":", tokenIndex);
            if (colonIndex != -1) {
                int firstQuote = -1;
                for (int i = colonIndex + 1; i < responseRaw.length(); i++) {
                    char c = responseRaw.charAt(i);
                    if (c == 34 || c == 39) {
                        firstQuote = i;
                        break;
                    }
                }
                if (firstQuote != -1) {
                    char quoteChar = responseRaw.charAt(firstQuote);
                    int lastQuote = responseRaw.indexOf(quoteChar, firstQuote + 1);
                    if (lastQuote != -1) {
                        return responseRaw.substring(firstQuote + 1, lastQuote);
                    }
                }
            }
        }
        throw new Exception("JSON response does not contain 'access_token' field.");
    }
}
