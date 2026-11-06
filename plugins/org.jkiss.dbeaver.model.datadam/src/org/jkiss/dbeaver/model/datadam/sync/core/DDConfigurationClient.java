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
package org.jkiss.dbeaver.model.datadam.sync.core;

import com.dbeaver.datadam.share.api.model.DDConfiguration;
import com.dbeaver.datadam.share.api.model.DDConfigurationPart;
import com.dbeaver.datadam.share.api.model.DDConfigurationSummary;
import com.dbeaver.datadam.share.api.model.DDCreateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationResult;
import com.dbeaver.rest.client.MediaType;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.utils.GsonUtils;
import org.jkiss.utils.HttpConstants;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DDConfigurationClient extends DDClient {

    private static final int TIMEOUT_MS = 30000;
    private static final String SUMMARY_FIELDS = """
        configurationId
        name
        createTime: createdAt
        lastSyncTime: lastSyncAt""";
    private static final String CONFIGURATION_FIELDS = SUMMARY_FIELDS + """

        parts {
            key
            kind
            projectId
            fingerprint
            encryptedValue
        }""";

    private final DDDesktopAccessService accessService;

    public DDConfigurationClient(@NotNull String url, @NotNull DDDesktopAccessService accessService) {
        super(url, DEFAULT_CONNECT_TIMEOUT, TIMEOUT_MS, List.of(), HttpClient.Redirect.NEVER);
        this.accessService = accessService;
        this.gson = GsonUtils.gsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                OffsetDateTime.parse(json.getAsString()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
            .create();
    }

    @NotNull
    public List<DDConfigurationSummary> listConfigurations() throws DBException {
        JsonElement result = call("""
            query {
                configurations {
            %s
                }
            }""".formatted(SUMMARY_FIELDS.indent(8)), Map.of(), "configurations");
        if (result.isJsonNull()) {
            throw new DBException("DataDam configuration list is missing");
        }
        return List.of(gson.fromJson(result, DDConfigurationSummary[].class));
    }

    @Nullable
    public DDConfiguration getConfiguration(@NotNull UUID configurationId) throws DBException {
        JsonElement result = call("""
            query($configurationId: ID!) {
                configuration(configurationId: $configurationId) {
            %s
                }
            }""".formatted(CONFIGURATION_FIELDS.indent(8)),
            Map.of("configurationId", configurationId.toString()), "configuration");
        return gson.fromJson(result, DDConfiguration.class);
    }

    @Nullable
    public DDConfiguration createConfiguration(@NotNull DDCreateConfigurationRequest input) throws DBException {
        JsonElement result = call("""
            mutation($input: CreateConfigurationInput!) {
                createConfiguration(input: $input) {
            %s
                }
            }""".formatted(CONFIGURATION_FIELDS.indent(8)), Map.of("input", input), "createConfiguration");
        return gson.fromJson(result, DDConfiguration.class);
    }

    @Nullable
    public DDUpdateConfigurationResult updateConfiguration(
        @NotNull UUID configurationId,
        @NotNull DDUpdateConfigurationRequest input
    ) throws DBException {
        JsonElement result = call("""
            mutation($configurationId: ID!, $input: UpdateConfigurationInput!) {
                updateConfiguration(configurationId: $configurationId, input: $input) {
                    configuration {
            %s
                    }
                    conflictingKeys
                }
            }""".formatted(CONFIGURATION_FIELDS.indent(12)),
            Map.of("configurationId", configurationId.toString(), "input", input), "updateConfiguration");
        return gson.fromJson(result, DDUpdateConfigurationResult.class);
    }

    @NotNull
    public String encryptPart(@NotNull String partKey, @NotNull byte[] content) throws DBException {
        DDKeyBundle bundle = getKeyBundle();
        byte[] encrypted = DDCrypto.encrypt(new DDBundleCredentials(bundle).getDataKey(), content, aad(bundle, partKey));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    @NotNull
    public byte[] decryptPart(@NotNull DDConfigurationPart part) throws DBException {
        DDKeyBundle bundle = getKeyBundle();
        byte[] encrypted;
        try {
            encrypted = Base64.getDecoder().decode(part.encryptedValue());
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid encrypted DataDam configuration part: " + part.key(), e);
        }
        return DDCrypto.decrypt(new DDBundleCredentials(bundle).getDataKey(), encrypted, aad(bundle, part.key()));
    }

    @NotNull
    private DDKeyBundle getKeyBundle() throws DBException {
        DDKeyBundle bundle = accessService.getKeyBundle();
        if (bundle == null) {
            throw new DBException("Import encryption keys for the current DataDam account before transferring configuration");
        }
        return bundle;
    }

    @NotNull
    private static byte[] aad(@NotNull DDKeyBundle bundle, @NotNull String partKey) {
        return ("configuration" + '\0' + bundle.accountId() + '\0' + partKey).getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    private JsonElement call(
        @NotNull String query,
        @NotNull Map<String, Object> variables,
        @NotNull String field
    ) throws DBException {
        URI uri = buildUri("graphql", Map.of());
        DDEndpoints.requireSecureTransport(uri);
        String token = accessService.getAccessTokenForResource(uri);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
            .header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.JSON.toString())
            .header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + token)
            .POST(createBodyPublisher(Map.of("query", query, "variables", variables), MediaType.JSON));
        JsonObject response = execute(request, JsonObject.class);
        if (response == null) {
            throw new DBException("DataDam configuration response is missing");
        }
        if (response.get("errors") instanceof JsonArray errors && !errors.isEmpty()) {
            throw mapGraphQlError(errors);
        }
        if (!(response.get("data") instanceof JsonObject data) || !data.has(field)) {
            throw new DBException("Invalid DataDam configuration response");
        }
        return data.get(field);
    }

    @NotNull
    private static DBException mapGraphQlError(@NotNull JsonArray errors) {
        if (!(errors.get(0) instanceof JsonObject error)) {
            return new DBException("DataDam configuration request failed");
        }
        String message = error.get("message") instanceof JsonPrimitive value
            ? value.getAsString() : "DataDam configuration request failed";
        if (error.get("extensions") instanceof JsonObject extensions &&
            extensions.get("classification") instanceof JsonPrimitive classification &&
            "INTERNAL_ERROR".equals(classification.getAsString())) {
            return new DDTransportException(message);
        }
        return new DBException(message);
    }

    @NotNull
    @Override
    protected DBException mapErrorResponse(int code, @NotNull String message, @NotNull URI uri) {
        return code >= 500 ? new DDTransportException(message) : super.mapErrorResponse(code, message, uri);
    }

    @Override
    protected void handleRequestException(@NotNull String message, @NotNull Throwable error) throws DBException {
        if (error instanceof DBException exception) {
            throw exception;
        }
        if (error instanceof IOException) {
            throw new DDTransportException(message, error);
        }
        super.handleRequestException(message, error);
    }
}
