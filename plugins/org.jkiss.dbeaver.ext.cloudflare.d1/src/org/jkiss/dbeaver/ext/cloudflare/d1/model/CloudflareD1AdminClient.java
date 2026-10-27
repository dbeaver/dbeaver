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
package org.jkiss.dbeaver.ext.cloudflare.d1.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CloudflareD1AdminClient {
    private static final String DEFAULT_API_URL = "https://api.cloudflare.com/client/v4";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_-]+");
    private static final int DATABASES_PER_PAGE = 10000;
    private final String accountId;
    private final String apiToken;
    private final URI apiUrl;
    private final Duration requestTimeout;
    private final HttpClient httpClient;

    protected CloudflareD1AdminClient(
        @NotNull String accountId, @NotNull String apiToken, @NotNull URI apiUrl,
        @NotNull Duration requestTimeout, @NotNull HttpClient httpClient
    ) {
        this.accountId = accountId;
        this.apiToken = apiToken;
        this.apiUrl = apiUrl;
        this.requestTimeout = requestTimeout;
        this.httpClient = httpClient;
    }

    @NotNull
    static CloudflareD1AdminClient from(@NotNull DBPConnectionConfiguration configuration) throws SQLException {
        return new CloudflareD1AdminClient(configuration);
    }

    protected CloudflareD1AdminClient(@NotNull DBPConnectionConfiguration configuration) throws SQLException {
        this(requiredIdentifier("account ID", configuration.getUserName()),
            required("API token", configuration.getUserPassword()), apiUrl(configuration.getProperty("apiUrl")),
            timeout(configuration, "queryTimeout", 30000),
            HttpClient.newBuilder().connectTimeout(timeout(configuration, "connectTimeout", 10000)).build());
    }

    @NotNull
    List<CloudflareD1Database> listDatabases() throws SQLException {
        List<CloudflareD1Database> databases = new ArrayList<>();
        int page = 1;
        while (true) {
            JsonObject response = request("GET", databasesEndpoint() + "?page=" + page + "&per_page=" + DATABASES_PER_PAGE, null);
            JsonArray result = array(response, "result");
            if (result == null) {
                throw invalidResponse("database list");
            }
            for (JsonElement item : result) {
                if (!item.isJsonObject()) {
                    throw invalidResponse("database list");
                }
                databases.add(database(item.getAsJsonObject()));
            }
            JsonObject pageInfo = object(response, "result_info");
            Integer total = integer(pageInfo, "total_count");
            Integer totalPages = integer(pageInfo, "total_pages");
            if (result.isEmpty() || total != null && databases.size() >= total
                || totalPages != null && page >= totalPages) {
                return List.copyOf(databases);
            }
            // Without totals, request the next page: the server may clamp per_page.
            page++;
        }
    }

    @NotNull
    CloudflareD1Database getDatabase(@NotNull String databaseId) throws SQLException {
        return databaseResult(request("GET", databaseEndpoint(databaseId), null), "database details");
    }

    @NotNull
    CloudflareD1Database createDatabase(@NotNull String name, @NotNull CloudflareD1ConnectionAdapter.CreateOptions options)
            throws SQLException {
        if (name.isBlank()) {
            throw new SQLException("Cloudflare D1 database name must not be blank", "42000");
        }
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        addNonBlank(body, "jurisdiction", options.jurisdiction());
        addNonBlank(body, "primary_location_hint", options.primaryLocationHint());
        return databaseResult(request("POST", databasesEndpoint(), body), "database creation");
    }

    void deleteDatabase(@NotNull String databaseId) throws SQLException {
        request("DELETE", databaseEndpoint(databaseId), null);
    }

    @NotNull
    CloudflareD1Database setReadReplication(@NotNull String databaseId, @NotNull CloudflareD1ConnectionAdapter.ReplicationMode mode)
            throws SQLException {
        JsonObject replication = new JsonObject();
        replication.addProperty("mode", mode.name().toLowerCase(Locale.ROOT));
        JsonObject body = new JsonObject();
        body.add("read_replication", replication);
        return databaseResult(request("PUT", databaseEndpoint(databaseId), body), "read-replication update");
    }

    @NotNull
    protected JsonObject request(@NotNull String method, @NotNull String endpoint, @Nullable JsonObject body) throws SQLException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + apiToken)
            .header("User-Agent", "DBeaver Cloudflare D1 administration")
            .timeout(requestTimeout);
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        }
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonObject result = parse(response.body(), response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || !success(result)) {
                throw apiError(response.statusCode());
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Cloudflare D1 request interrupted", "HY008", e);
        } catch (HttpTimeoutException e) {
            throw new SQLTimeoutException("Cloudflare D1 request timed out", "HYT00", e);
        } catch (IOException e) {
            throw new SQLException("Cloudflare D1 API request failed", "08001", e);
        }
    }

    @NotNull
    private String databasesEndpoint() {
        return apiUrl + "/accounts/" + accountId + "/d1/database";
    }

    @NotNull
    protected String databaseEndpoint(@NotNull String databaseId) throws SQLException {
        return databasesEndpoint() + "/" + requiredIdentifier("database ID", databaseId);
    }

    @NotNull
    private static CloudflareD1Database databaseResult(@NotNull JsonObject response, @NotNull String operation) throws SQLException {
        return database(requiredResult(response, operation));
    }

    @NotNull
    protected static JsonObject requiredResult(@NotNull JsonObject response, @NotNull String operation) throws SQLException {
        JsonObject result = object(response, "result");
        if (result == null) {
            throw invalidResponse(operation);
        }
        return result;
    }

    @NotNull
    private static CloudflareD1Database database(@NotNull JsonObject object) throws SQLException {
        String id = string(object, "uuid");
        String name = string(object, "name");
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw invalidResponse("database");
        }
        return new CloudflareD1Database(id, name, string(object, "created_at"), string(object, "jurisdiction"),
            string(object, "version"), longValue(object, "file_size"), string(object, "running_in_region"),
            replicationMode(object(object, "read_replication")));
    }

    @Nullable
    private static CloudflareD1ConnectionAdapter.ReplicationMode replicationMode(@Nullable JsonObject object) throws SQLException {
        String mode = string(object, "mode");
        if (mode == null) {
            return null;
        }
        try {
            return CloudflareD1ConnectionAdapter.ReplicationMode.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw invalidResponse("read-replication mode");
        }
    }

    @NotNull
    private static JsonObject parse(@NotNull String body, int statusCode) throws SQLException {
        try {
            JsonElement value = JsonParser.parseString(body);
            if (value.isJsonObject()) {
                return value.getAsJsonObject();
            }
        } catch (RuntimeException ignored) {
            // Do not include response content in errors: it may contain sensitive information.
        }
        throw new SQLException("Invalid response from Cloudflare D1 API (HTTP " + statusCode + ")", "08006");
    }

    private static boolean success(@NotNull JsonObject object) {
        JsonElement value = object.get("success");
        return value == null || !value.isJsonPrimitive() || value.getAsBoolean();
    }

    @Nullable
    private static JsonObject object(@Nullable JsonObject parent, @NotNull String name) {
        JsonElement value = parent == null ? null : parent.get(name);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    @Nullable
    private static JsonArray array(@NotNull JsonObject parent, @NotNull String name) {
        JsonElement value = parent.get(name);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    @Nullable
    protected static String string(@Nullable JsonObject parent, @NotNull String name) {
        JsonElement value = parent == null ? null : parent.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
            ? value.getAsString() : null;
    }

    @Nullable
    private static Long longValue(@NotNull JsonObject parent, @NotNull String name) throws SQLException {
        JsonElement value = parent.get(name);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        try {
            return value.getAsLong();
        } catch (RuntimeException e) {
            throw invalidResponse("database file size");
        }
    }

    @Nullable
    private static Integer integer(@Nullable JsonObject parent, @NotNull String name) {
        JsonElement value = parent == null ? null : parent.get(name);
        try {
            return value == null || value.isJsonNull() ? null : value.getAsInt();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void addNonBlank(@NotNull JsonObject object, @NotNull String name, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            object.addProperty(name, value);
        }
    }

    @NotNull
    private static SQLException apiError(int statusCode) {
        String state = statusCode == 401 || statusCode == 403 ? "28000" : (statusCode == 404 ? "3D000" : "08006");
        return new SQLException("Cloudflare D1 API request failed (HTTP " + statusCode + ")", state);
    }

    @NotNull
    protected static SQLException invalidResponse(@NotNull String operation) {
        return new SQLException("Cloudflare D1 " + operation + " response is invalid", "08006");
    }

    @NotNull
    private static String required(@NotNull String name, @Nullable String value) throws SQLException {
        if (value == null || value.isBlank()) {
            throw new SQLException("Cloudflare D1 " + name + " is required", "08001");
        }
        return value;
    }

    @NotNull
    private static String requiredIdentifier(@NotNull String name, @Nullable String value) throws SQLException {
        String identifier = required(name, value);
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new SQLException("Invalid Cloudflare D1 " + name, "08001");
        }
        return identifier;
    }

    @NotNull
    private static URI apiUrl(@Nullable String value) throws SQLException {
        String endpoint = value == null || value.isBlank() ? DEFAULT_API_URL : value.trim();
        try {
            URI uri = URI.create(endpoint);
            if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null) {
                return URI.create(endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint);
            }
        } catch (IllegalArgumentException ignored) {
            // Report a sanitized error below rather than echoing a potentially sensitive URL.
        }
        throw new SQLException("Invalid Cloudflare D1 API URL", "08001");
    }

    @NotNull
    private static Duration timeout(@NotNull DBPConnectionConfiguration configuration, @NotNull String property, int defaultMillis)
            throws SQLException {
        String value = configuration.getProperty(property);
        if (value == null || value.isBlank()) {
            return Duration.ofMillis(defaultMillis);
        }
        try {
            int millis = Integer.parseInt(value);
            if (millis <= 0) {
                throw new NumberFormatException();
            }
            return Duration.ofMillis(millis);
        } catch (NumberFormatException e) {
            throw new SQLException("Invalid Cloudflare D1 " + property, "08001", e);
        }
    }
}
