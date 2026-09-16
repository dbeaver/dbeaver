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

import com.dbeaver.datadam.share.api.exception.DDShareException;
import com.dbeaver.datadam.share.api.model.*;
import com.dbeaver.datadam.share.api.model.DDConfiguration;
import com.dbeaver.datadam.share.api.model.DDConfigurationSummary;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationResult;
import com.dbeaver.datadam.share.api.service.DDSharedProjectService;
import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.MediaType;
import com.dbeaver.rest.client.interceptor.HttpRequestWrapper;
import com.dbeaver.rest.client.interceptor.HttpResponseWrapper;
import com.dbeaver.rest.client.interceptor.InterceptorChain;
import com.google.gson.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.GsonUtils;
import org.jkiss.utils.HttpConstants;
import org.jkiss.utils.Pair;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import javax.crypto.SecretKey;

public class DDShareClient extends AbstractRestClient implements DDSyncTransport, DDSharedProjectService {

    private static final int TIMEOUT_MS = 30000;
    private static final String SERVER_TIME_HEADER = "X-DD-Server-Time";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";

    static final String PROJECT_FIELDS = """
        id: projectId
        name
        description
        projectOwner: ownerAccountId
        createTime
        updateTime""";

    private final DDSyncCredentials credentials;
    private final ThreadLocal<byte[]> requestBody = new ThreadLocal<>();
    private SecretKey dataKey;

    public DDShareClient(@NotNull String url, @NotNull DDSyncCredentials credentials) {
        super(url, DEFAULT_CONNECT_TIMEOUT, TIMEOUT_MS, List.of());
        this.credentials = credentials;
        this.gson = GsonUtils.gsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeIsoAdapter())
            .create();
    }

    /**
     * The server's DateTime scalar is an OffsetDateTime (UTC); DDSharedProject/DDSharedProjectRevision
     * carry it as a zone-less LocalDateTime, so it round-trips through the UTC offset.
     */
    private static final class LocalDateTimeIsoAdapter
        implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        @NotNull
        @Override
        public LocalDateTime deserialize(
            @NotNull JsonElement json, @NotNull Type typeOfT, @NotNull JsonDeserializationContext context
        ) throws JsonParseException {
            return OffsetDateTime.parse(json.getAsString()).toLocalDateTime();
        }

        @NotNull
        @Override
        public JsonElement serialize(
            @NotNull LocalDateTime src, @NotNull Type typeOfSrc, @NotNull JsonSerializationContext context
        ) {
            return new JsonPrimitive(src.atOffset(ZoneOffset.UTC).toString());
        }
    }

    @NotNull
    @Override
    public List<DDConfigurationSummary> listConfigurations() throws DBException {
        JsonObject data = call(DDSyncApi.QUERY_LIST_CONFIGURATIONS, Map.of());
        return List.of(gson.fromJson(data.get("configurations"), DDConfigurationSummary[].class));
    }

    @NotNull
    @Override
    public DDConfiguration getConfiguration(@NotNull String configurationId) throws DBException {
        JsonObject data = call(DDSyncApi.QUERY_GET_CONFIGURATION, Map.of("configurationId", configurationId));
        JsonElement configuration = data.get("configuration");
        if (configuration == null || configuration.isJsonNull()) {
            throw new DDConfigurationNotFoundException("Configuration '" + configurationId + "' not found");
        }
        return gson.fromJson(configuration, DDConfiguration.class);
    }

    @NotNull
    @Override
    public DDConfiguration createConfiguration(@NotNull DDCreateConfigurationRequest request) throws DBException {
        JsonObject data = call(DDSyncApi.MUTATION_CREATE_CONFIGURATION, Map.of("input", request));
        return gson.fromJson(data.get("createConfiguration"), DDConfiguration.class);
    }

    @NotNull
    @Override
    public DDUpdateConfigurationResult updateConfiguration(
        @NotNull String configurationId,
        @NotNull DDUpdateConfigurationRequest request
    ) throws DBException {
        JsonObject data = call(
            DDSyncApi.MUTATION_UPDATE_CONFIGURATION,
            Map.of("configurationId", configurationId, "input", request));
        JsonElement result = data.get("updateConfiguration");
        if (result == null || result.isJsonNull()) {
            throw new DDConfigurationNotFoundException("Configuration '" + configurationId + "' not found");
        }
        return gson.fromJson(result, DDUpdateConfigurationResult.class);
    }

    @NotNull
    @Override
    public List<DDSharedProject> listProjects() throws DDShareException {
        try {
            JsonObject data = call("""
                query {
                    projects {
                %s
                    }
                }""".formatted(PROJECT_FIELDS.indent(8)), Map.of());
            List<DDSharedProject> result = new ArrayList<>();
            for (DDSharedProject project : gson.fromJson(data.get("projects"), DDSharedProject[].class)) {
                result.add(decryptProject(project));
            }
            return result;
        } catch (DBException e) {
            throw new DDShareException("Failed to list projects", e);
        }
    }

    @NotNull
    @Override
    public DDSharedProject createProject(
        @NotNull UUID projectId, @NotNull String name, @Nullable String description
    ) throws DDShareException {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectId", projectId.toString());
            variables.put("name", encryptText(projectId.toString(), FIELD_NAME, name));
            variables.put(
                "description",
                description == null ? null : encryptText(projectId.toString(), FIELD_DESCRIPTION, description));
            JsonObject data = call("""
                mutation($projectId: ID!, $name: String!, $description: String) {
                    createProject(projectId: $projectId, name: $name, description: $description) {
                %s
                    }
                }""".formatted(PROJECT_FIELDS.indent(8)), variables);
            return decryptProject(gson.fromJson(data.get("createProject"), DDSharedProject.class));
        } catch (DBException e) {
            throw new DDShareException("Failed to create project", e);
        }
    }

    @NotNull
    @Override
    public DDSharedProject updateProject(
        @NotNull UUID projectId, @NotNull String name, @Nullable String description
    ) throws DDShareException {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectId", projectId.toString());
            variables.put("name", encryptText(projectId.toString(), FIELD_NAME, name));
            variables.put(
                "description",
                description == null ? null : encryptText(projectId.toString(), FIELD_DESCRIPTION, description));
            JsonObject data = call("""
                mutation($projectId: ID!, $name: String!, $description: String) {
                    updateProject(projectId: $projectId, name: $name, description: $description) {
                %s
                    }
                }""".formatted(PROJECT_FIELDS.indent(8)), variables);
            JsonElement result = data.get("updateProject");
            if (result == null || result.isJsonNull()) {
                throw new DDShareException("Project not found: " + projectId);
            }
            return decryptProject(gson.fromJson(result, DDSharedProject.class));
        } catch (DBException e) {
            throw new DDShareException("Failed to update project", e);
        }
    }

    @Override
    public boolean deleteProject(@NotNull UUID projectId) throws DDShareException {
        try {
            JsonObject data = call("""
                mutation($projectId: ID!) {
                    deleteProject(projectId: $projectId)
                }""", Map.of("projectId", projectId.toString()));
            return data.get("deleteProject") instanceof JsonPrimitive p && p.getAsBoolean();
        } catch (DBException e) {
            throw new DDShareException("Failed to delete project", e);
        }
    }

    @NotNull
    @Override
    public DDSharedProjectRevision getCurrentProjectRevision(@NotNull UUID projectId) throws DDShareException {
        try {
            JsonObject data = call(
                """
                    query($projectId: ID!) {
                        currentProjectRevision(projectId: $projectId) {
                            id: revisionId
                            userId
                            updateTime
                            configurationFingerprint
                        }
                    }""", Map.of("projectId", projectId.toString())
            );
            JsonElement result = data.get("currentProjectRevision");
            if (result == null || result.isJsonNull()) {
                throw new DDShareException("Project not found or has no current revision: " + projectId);
            }
            return gson.fromJson(result, DDSharedProjectRevision.class);
        } catch (DBException e) {
            throw new DDShareException("Failed to get current project revision", e);
        }
    }

    @NotNull
    @Override
    public DDSharedProjectPullResponse pullProjectConfiguration(@NotNull UUID projectId) throws DDShareException {
        try {
            JsonObject data = call("""
                query($projectId: ID!) {
                    pullProjectConfiguration(projectId: $projectId) {
                        files {
                            fileName
                            encryptedContents
                            fingerprint
                        }
                        currentRevision {
                            id: revisionId
                            userId
                            updateTime
                            configurationFingerprint
                        }
                    }
                }""", Map.of("projectId", projectId.toString()));
            JsonElement result = data.get("pullProjectConfiguration");
            if (result == null || result.isJsonNull()) {
                throw new DDShareException("Project not found or has no current revision: " + projectId);
            }
            return gson.fromJson(result, DDSharedProjectPullResponse.class);
        } catch (DBException e) {
            throw new DDShareException("Failed to pull project configuration", e);
        }
    }

    /**
     * Pulls a consistent project revision and decrypts its files.
     */
    @NotNull
    public DDSharedProjectPullResult pullFiles(@NotNull UUID projectId) throws DDShareException {
        try {
            DDSharedProjectPullResponse response = pullProjectConfiguration(projectId);
            Map<String, byte[]> files = new LinkedHashMap<>();
            for (DDSharedProjectFile file : response.files()) {
                files.put(file.fileName(), decryptBytes(projectId.toString(), file.fileName(), file.encryptedContents()));
            }
            return new DDSharedProjectPullResult(files, response.currentRevision());
        } catch (DBException e) {
            throw new DDShareException("Failed to pull project files", e);
        }
    }

    @NotNull
    @Override
    public DDSharedProjectRevision pushProjectConfiguration(
        @NotNull UUID projectId,
        @NotNull DDSharedProjectConfiguration projectContent,
        @NotNull String lastKnownConfigurationFingerprint
    ) throws DDShareException {
        try {
            JsonObject data = call("""
                mutation($projectId: ID!, $projectContent: ProjectConfigurationInput!, $lastKnownConfigurationFingerprint: String!) {
                    pushProjectConfiguration(
                        projectId: $projectId
                        projectContent: $projectContent
                        lastKnownConfigurationFingerprint: $lastKnownConfigurationFingerprint
                    ) {
                        id: revisionId
                        userId
                        updateTime
                        configurationFingerprint
                    }
                }""",
                Map.of(
                    "projectId", projectId.toString(),
                    "projectContent", projectContent,
                    "lastKnownConfigurationFingerprint", lastKnownConfigurationFingerprint));
            JsonElement result = data.get("pushProjectConfiguration");
            if (result == null || result.isJsonNull()) {
                throw new DDShareException("Project not found or configuration fingerprint is stale: " + projectId);
            }
            return gson.fromJson(result, DDSharedProjectRevision.class);
        } catch (DBException e) {
            throw new DDShareException("Failed to push project configuration", e);
        }
    }

    /**
     * Convenience wrapper over pushProjectConfiguration that encrypts prepared project files.
     */
    @NotNull
    public DDSharedProjectRevision pushFiles(
        @NotNull UUID projectId,
        @NotNull PreparedFiles preparedFiles,
        @NotNull String lastKnownConfigurationFingerprint
    ) throws DDShareException {
        try {
            List<DDSharedProjectFile> projectFiles = new ArrayList<>();
            for (Map.Entry<String, Pair<String, byte[]>> file : preparedFiles.files().entrySet()) {
                Pair<String, byte[]> preparedFile = file.getValue();
                projectFiles.add(new DDSharedProjectFile(
                    file.getKey(),
                    encryptBytes(projectId.toString(), file.getKey(), preparedFile.getSecond()),
                    preparedFile.getFirst()));
            }
            return pushProjectConfiguration(
                projectId,
                new DDSharedProjectConfiguration(preparedFiles.configurationFingerprint(), projectFiles),
                lastKnownConfigurationFingerprint);
        } catch (DBException e) {
            throw new DDShareException("Failed to encrypt project files", e);
        }
    }

    @NotNull
    @Override
    public List<DDSharedProjectRevision> getProjectRevisions(
        @NotNull UUID projectId, @Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime
    ) throws DDShareException {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectId", projectId.toString());
            variables.put("startTime", startTime);
            variables.put("endTime", endTime);
            JsonObject data = call("""
                query($projectId: ID!, $startTime: DateTime, $endTime: DateTime) {
                    projectRevisions(projectId: $projectId, startTime: $startTime, endTime: $endTime) {
                        id: revisionId
                        userId
                        updateTime
                        configurationFingerprint
                    }
                }""", variables);
            return List.of(gson.fromJson(data.get("projectRevisions"), DDSharedProjectRevision[].class));
        } catch (DBException e) {
            throw new DDShareException("Failed to get project revisions", e);
        }
    }

    @NotNull
    private DDSharedProject decryptProject(@NotNull DDSharedProject project) throws DBException {
        String projectId = project.id().toString();
        return new DDSharedProject(
            project.id(),
            decryptText(projectId, FIELD_NAME, project.name()),
            project.description() == null ? null : decryptText(projectId, FIELD_DESCRIPTION, project.description()),
            project.createTime(),
            project.updateTime(),
            project.projectOwner());
    }

    @NotNull
    private String encryptText(@NotNull String projectId, @NotNull String field, @NotNull String plaintext) throws DBException {
        return Base64.getEncoder().encodeToString(
            DDCrypto.encrypt(getDataKey(), plaintext.getBytes(StandardCharsets.UTF_8), aad(projectId, field)));
    }

    @NotNull
    private String decryptText(@NotNull String projectId, @NotNull String field, @NotNull String ciphertext) throws DBException {
        return new String(decryptBytes(projectId, field, ciphertext), StandardCharsets.UTF_8);
    }

    @NotNull
    private String encryptBytes(@NotNull String projectId, @NotNull String field, @NotNull byte[] data) throws DBException {
        return Base64.getEncoder().encodeToString(DDCrypto.encrypt(getDataKey(), data, aad(projectId, field)));
    }

    @NotNull
    private byte[] decryptBytes(@NotNull String projectId, @NotNull String field, @NotNull String ciphertext) throws DBException {
        return DDCrypto.decrypt(getDataKey(), decodeBase64(ciphertext), aad(projectId, field));
    }

    @NotNull
    private static byte[] decodeBase64(@NotNull String value) throws DBException {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid ciphertext encoding", e);
        }
    }

    /**
     * Binds projectId + field/file name to the ciphertext via AES-GCM AAD, so a valid ciphertext
     * can't be relabeled as belonging to another project or field.
     */
    @NotNull
    public static byte[] aad(@NotNull String projectId, @NotNull String field) {
        return (projectId + '\u0000' + field).getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    private SecretKey getDataKey() throws DBException {
        if (dataKey == null) {
            dataKey = credentials.getDataKey();
        }
        return dataKey;
    }

    @NotNull
    private JsonObject call(@NotNull String query, @NotNull Map<String, Object> variables) throws DBException {
        byte[] body = gson.toJson(Map.of("query", query, "variables", variables)).getBytes(StandardCharsets.UTF_8);
        JsonObject response = execute(request(body), JsonObject.class);

        if (response.get("errors") instanceof JsonArray errors && !errors.isEmpty()) {
            throw mapGraphQlError(errors);
        }
        return response.getAsJsonObject("data");
    }

    @NotNull
    private DBException mapGraphQlError(@NotNull JsonArray errors) {
        if (!(errors.get(0) instanceof JsonObject first)) {
            return new DBException("GraphQL request failed");
        }
        String message = first.get("message") instanceof JsonPrimitive m ? m.getAsString() : "GraphQL request failed";
        if ("INTERNAL_ERROR".equals(extractClassification(first))) {
            return new DDTransportException(message);
        }
        return new DBException(message);
    }

    @Nullable
    private static String extractClassification(@NotNull JsonObject error) {
        if (!(error.get("extensions") instanceof JsonObject extensions)) {
            return null;
        }
        return extensions.get("classification") instanceof JsonPrimitive c ? c.getAsString() : null;
    }

    @NotNull
    private HttpRequest.Builder request(@NotNull byte[] body) throws DBException {
        URI uri = buildUri(CommonUtils.removeLeadingSlash(DDSyncApi.GRAPHQL_ENDPOINT), Map.of());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.JSON.toString())
            .header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + buildToken("POST", uri, body))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        requestBody.set(body);
        return builder;
    }

    @NotNull
    @Override
    protected <T> T execute(@NotNull HttpRequest.Builder builder, @NotNull Type type) throws DBException {
        try {
            return super.execute(builder, type);
        } finally {
            requestBody.remove();
        }
    }

    @NotNull
    @Override
    protected HttpResponseWrapper executeChain(
        @NotNull InterceptorChain chain,
        @NotNull HttpRequestWrapper request,
        @NotNull URI uri
    ) throws Exception {
        HttpResponseWrapper response = chain.proceed(request);
        String serverTime = header(response, SERVER_TIME_HEADER);
        if (serverTime == null) {
            return response;
        }
        credentials.updateServerTime(Long.parseLong(serverTime));
        request.withHeader(
            HttpConstants.HEADER_AUTHORIZATION,
            HttpConstants.BEARER_PREFIX + buildToken("POST", uri, requestBody.get()));
        return chain.proceed(request);
    }

    @NotNull
    private String buildToken(@NotNull String method, @NotNull URI uri, @NotNull byte[] body) throws DBException {
        String pathAndQuery = uri.getRawQuery() == null ? uri.getRawPath() : uri.getRawPath() + "?" + uri.getRawQuery();
        return credentials.buildToken(method, pathAndQuery, body);
    }

    @NotNull
    @Override
    protected DBException mapErrorResponse(int code, @NotNull String message, @NotNull URI uri) {
        if (code >= 500) {
            return new DDTransportException(message);
        }
        return super.mapErrorResponse(code, message, uri);
    }

    @Override
    protected void handleRequestException(@NotNull String message, @NotNull Throwable e) throws DBException {
        if (e instanceof DBException exception) {
            throw exception;
        }
        if (e instanceof IOException) {
            throw new DDTransportException(message, e);
        }
        super.handleRequestException(message, e);
    }

    @Nullable
    private static String header(@NotNull HttpResponseWrapper response, @NotNull String name) {
        return response.headers().entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .flatMap(entry -> entry.getValue().stream())
            .findFirst()
            .orElse(null);
    }
}
