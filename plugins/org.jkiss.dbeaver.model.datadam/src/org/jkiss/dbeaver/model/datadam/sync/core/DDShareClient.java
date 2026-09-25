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
import com.dbeaver.datadam.share.api.model.DDConfiguration;
import com.dbeaver.datadam.share.api.model.DDConfigurationSummary;
import com.dbeaver.datadam.share.api.model.DDCreateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDProjectStatistics;
import com.dbeaver.datadam.share.api.model.DDSharedProject;
import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import com.dbeaver.datadam.share.api.model.DDSharedProjectFile;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import com.dbeaver.datadam.share.api.model.DDSharedWorkspace;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationResult;
import com.dbeaver.datadam.share.api.service.DDProjectStatisticsService;
import com.dbeaver.datadam.share.api.service.DDSharedProjectService;
import com.dbeaver.datadam.share.api.service.DDSharedWorkspaceService;
import com.dbeaver.datadam.share.api.utils.DDFingerprintUtils;
import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.MediaType;
import com.dbeaver.rest.client.interceptor.HttpRequestWrapper;
import com.dbeaver.rest.client.interceptor.HttpResponseWrapper;
import com.dbeaver.rest.client.interceptor.InterceptorChain;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.GsonUtils;
import org.jkiss.utils.HttpConstants;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;

public class DDShareClient extends AbstractRestClient
    implements DDSyncTransport, DDSharedProjectService, DDProjectStatisticsService, DDSharedWorkspaceService {

    private static final int TIMEOUT_MS = 30000;
    private static final String SERVER_TIME_HEADER = "X-DD-Server-Time";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String WORKSPACE_FIELDS = "id name encryptedKey createTime updateTime";

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
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeIsoAdapter())
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

    private static final class OffsetDateTimeIsoAdapter
        implements JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {
        @NotNull
        @Override
        public OffsetDateTime deserialize(
            @NotNull JsonElement json, @NotNull Type type, @NotNull JsonDeserializationContext context
        ) {
            return OffsetDateTime.parse(json.getAsString());
        }

        @NotNull
        @Override
        public JsonElement serialize(
            @NotNull OffsetDateTime value, @NotNull Type type, @NotNull JsonSerializationContext context
        ) {
            return new JsonPrimitive(value.toString());
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
    public DDSharedProjectConfiguration pullProjectConfiguration(@NotNull UUID projectId) throws DDShareException {
        return pullProjectConfiguration(projectId, List.of(DDSharedProjectConfiguration.Format.LEGACY_LOCAL_FILES));
    }

    @NotNull
    @Override
    public DDSharedProjectConfiguration pullProjectConfiguration(
        @NotNull UUID projectId, @NotNull List<DDSharedProjectConfiguration.Format> supportedFormats
    ) throws DDShareException {
        try {
            JsonObject data = call("""
                query($projectId: ID!, $supportedFormats: [ProjectConfigurationFormat!]!) {
                    pullProjectConfiguration(projectId: $projectId, supportedFormats: $supportedFormats) {
                        format
                        configurationFingerprint
                        files {
                            fileName
                            encryptedContents
                            fingerprint
                        }
                    }
                }""", Map.of("projectId", projectId.toString(), "supportedFormats", supportedFormats));
            JsonElement result = data.get("pullProjectConfiguration");
            if (result == null || result.isJsonNull()) {
                throw new DDShareException("Project not found: " + projectId);
            }
            DDSharedProjectConfiguration configuration = gson.fromJson(result, DDSharedProjectConfiguration.class);
            if (!supportedFormats.contains(configuration.format())) {
                throw new DDShareException("Unsupported project configuration format");
            }
            return configuration;
        } catch (DBException e) {
            throw new DDShareException("Failed to pull project configuration", e);
        }
    }

    /**
     * Convenience wrapper over pullProjectConfiguration that also decrypts each file's contents.
     */
    @NotNull
    public DDSharedProjectPullResult pullFiles(@NotNull UUID projectId) throws DDShareException {
        DDSharedProjectConfiguration remote = pullProjectConfiguration(projectId, List.of(DDSharedProjectConfiguration.Format.values()));
        try {
            Map<String, byte[]> files = new LinkedHashMap<>();
            for (DDSharedProjectFile file : remote.files()) {
                byte[] contents = decryptBytes(projectId.toString(), file.fileName(), file.encryptedContents());
                if (!DDFingerprintUtils.calculateFileFingerprint(projectId, file.fileName(), contents).equals(file.fingerprint())
                    || files.putIfAbsent(file.fileName(), contents) != null) {
                    throw new DBException("Invalid project file fingerprint");
                }
            }
            if (!DDFingerprintUtils.calculateConfigurationFingerprint(projectId, remote.files())
                .equals(remote.configurationFingerprint())) {
                throw new DBException("Invalid project configuration fingerprint");
            }
            return new DDSharedProjectPullResult(remote.configurationFingerprint(), files, remote.format());
        } catch (DBException | IllegalArgumentException e) {
            throw new DDShareException("Failed to decrypt project files", e);
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
     * Convenience wrapper over pushProjectConfiguration that also encrypts each file's contents
     * and computes the fingerprints.
     */
    @NotNull
    public DDSharedProjectRevision pushFiles(
        @NotNull UUID projectId,
        @NotNull Map<String, byte[]> files,
        @NotNull String lastKnownConfigurationFingerprint
    ) throws DDShareException {
        return pushFiles(projectId, files, lastKnownConfigurationFingerprint, DDSharedProjectConfiguration.Format.LEGACY_LOCAL_FILES);
    }

    @NotNull
    public DDSharedProjectRevision pushFiles(
        @NotNull UUID projectId,
        @NotNull Map<String, byte[]> files,
        @NotNull String lastKnownConfigurationFingerprint,
        @NotNull DDSharedProjectConfiguration.Format format
    ) throws DDShareException {
        try {
            List<DDSharedProjectFile> projectFiles = new ArrayList<>();
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                String fingerprint = DDFingerprintUtils.calculateFileFingerprint(projectId, file.getKey(), file.getValue());
                projectFiles.add(new DDSharedProjectFile(
                    file.getKey(), encryptBytes(projectId.toString(), file.getKey(), file.getValue()), fingerprint));
            }
            String configurationFingerprint = DDFingerprintUtils.calculateConfigurationFingerprint(projectId, projectFiles);
            return pushProjectConfiguration(
                projectId,
                new DDSharedProjectConfiguration(configurationFingerprint, projectFiles, format),
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
    public UUID getCurrentShareUserId() throws DDShareException {
        return UUID.fromString(shareCall("query { currentShareUserId }", Map.of()).get("currentShareUserId").getAsString());
    }

    @NotNull
    @Override
    public List<DDProjectStatistics> getProjectStatistics(
        @NotNull UUID projectId, @Nullable OffsetDateTime startTime, @Nullable OffsetDateTime endTime, int offset, int limit
    ) throws DDShareException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("projectId", projectId.toString());
        variables.put("startTime", startTime);
        variables.put("endTime", endTime);
        variables.put("offset", offset);
        variables.put("limit", limit);
        JsonObject data = shareCall("""
            query($projectId: ID!, $startTime: DateTime, $endTime: DateTime, $offset: Int!, $limit: Int!) {
                getProjectStatistics(projectId: $projectId, startTime: $startTime, endTime: $endTime, offset: $offset, limit: $limit) {
                    id userId time type ipAddress userAgent configurationFingerprint
                }
            }""", variables);
        return List.of(gson.fromJson(data.get("getProjectStatistics"), DDProjectStatistics[].class));
    }

    @NotNull
    @Override
    public List<DDSharedWorkspace> listWorkspaces() throws DDShareException {
        JsonObject data = shareCall("query { listWorkspaces { " + WORKSPACE_FIELDS + " } }", Map.of());
        return List.of(gson.fromJson(data.get("listWorkspaces"), DDSharedWorkspace[].class));
    }

    @NotNull
    @Override
    public DDSharedWorkspace createWorkspace(@NotNull String name, @NotNull String encryptedKey) throws DDShareException {
        JsonObject data = shareCall("""
            mutation($name: String!, $encryptedKey: String!) {
                createWorkspace(name: $name, encryptedKey: $encryptedKey) { %s }
            }""".formatted(WORKSPACE_FIELDS), Map.of("name", name, "encryptedKey", encryptedKey));
        return gson.fromJson(data.get("createWorkspace"), DDSharedWorkspace.class);
    }

    @NotNull
    @Override
    public DDSharedWorkspace updateWorkspace(@NotNull UUID workspaceId, @NotNull String name) throws DDShareException {
        JsonObject data = shareCall("""
            mutation($workspaceId: ID!, $name: String!) {
                updateWorkspace(workspaceId: $workspaceId, name: $name) { %s }
            }""".formatted(WORKSPACE_FIELDS), Map.of("workspaceId", workspaceId.toString(), "name", name));
        return gson.fromJson(data.get("updateWorkspace"), DDSharedWorkspace.class);
    }

    @Override
    public boolean deleteWorkspace(@NotNull UUID workspaceId) throws DDShareException {
        return shareCall("mutation($workspaceId: ID!) { deleteWorkspace(workspaceId: $workspaceId) }",
            Map.of("workspaceId", workspaceId.toString())).get("deleteWorkspace").getAsBoolean();
    }

    @NotNull
    @Override
    public List<String> listConfigFiles(@NotNull UUID workspaceId) throws DDShareException {
        JsonObject data = shareCall("query($workspaceId: ID!) { listConfigFiles(workspaceId: $workspaceId) }",
            Map.of("workspaceId", workspaceId.toString()));
        return List.of(gson.fromJson(data.get("listConfigFiles"), String[].class));
    }

    @Nullable
    @Override
    public String readConfigFile(@NotNull UUID workspaceId, @NotNull String fileName) throws DDShareException {
        JsonElement contents = shareCall("""
            query($workspaceId: ID!, $fileName: String!) { readConfigFile(workspaceId: $workspaceId, fileName: $fileName) }
            """, Map.of("workspaceId", workspaceId.toString(), "fileName", fileName)).get("readConfigFile");
        return contents == null || contents.isJsonNull() ? null : contents.getAsString();
    }

    @Override
    public boolean writeConfigFile(@NotNull UUID workspaceId, @NotNull String fileName, @NotNull String encryptedContents)
        throws DDShareException {
        return shareCall("""
            mutation($workspaceId: ID!, $fileName: String!, $encryptedContents: String!) {
                writeConfigFile(workspaceId: $workspaceId, fileName: $fileName, encryptedContents: $encryptedContents)
            }""", Map.of("workspaceId", workspaceId.toString(), "fileName", fileName, "encryptedContents", encryptedContents))
            .get("writeConfigFile").getAsBoolean();
    }

    @Override
    public boolean deleteConfigFile(@NotNull UUID workspaceId, @NotNull String fileName) throws DDShareException {
        return shareCall("""
            mutation($workspaceId: ID!, $fileName: String!) { deleteConfigFile(workspaceId: $workspaceId, fileName: $fileName) }
            """, Map.of("workspaceId", workspaceId.toString(), "fileName", fileName)).get("deleteConfigFile").getAsBoolean();
    }

    @NotNull
    private JsonObject shareCall(@NotNull String query, @NotNull Map<String, Object> variables) throws DDShareException {
        try {
            return call(query, variables);
        } catch (DBException e) {
            throw new DDShareException("Share request failed", e);
        }
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
