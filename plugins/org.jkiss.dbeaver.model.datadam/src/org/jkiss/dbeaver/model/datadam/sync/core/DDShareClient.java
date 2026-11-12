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
import com.dbeaver.datadam.share.api.service.DDSharedProjectService;
import com.google.gson.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDClient;
import org.jkiss.dbeaver.model.datadam.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.utils.GsonUtils;
import org.jkiss.utils.Pair;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import javax.crypto.SecretKey;

public class DDShareClient extends DDClient implements DDSharedProjectService {

    private static final int TIMEOUT_MS = 30000;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";

    static final String PROJECT_FIELDS = """
        id: projectId
        name
        description
        projectOwner: ownerAccountId
        createTime
        updateTime""";
    private static final String REVISION_FIELDS = """
        id: revisionId
        userId
        updateTime
        configurationFingerprint""";
    private static final String LIST_PROJECTS_QUERY = """
        query {
            projects {
        %s
            }
        }""".formatted(PROJECT_FIELDS.indent(8));
    private static final String CREATE_PROJECT_MUTATION = """
        mutation($apiKeyId: ID!, $projectId: ID!, $name: String!, $description: String) {
            project: createProject(apiKeyId: $apiKeyId, projectId: $projectId, name: $name, description: $description) {
        %s
            }
        }""".formatted(PROJECT_FIELDS.indent(8));
    private static final String UPDATE_PROJECT_MUTATION = """
        mutation($projectId: ID!, $name: String!, $description: String) {
            project: updateProject(projectId: $projectId, name: $name, description: $description) {
        %s
            }
        }""".formatted(PROJECT_FIELDS.indent(8));
    private static final String DELETE_PROJECT_MUTATION = """
        mutation($projectId: ID!) {
            deleteProject(projectId: $projectId)
        }""";
    private static final String CURRENT_REVISION_QUERY = """
        query($projectId: ID!) {
            revision: projectCurrentRevision(projectId: $projectId) {
        %s
            }
        }""".formatted(REVISION_FIELDS.indent(8));
    private static final String PULL_CONFIGURATION_QUERY = """
        query($projectId: ID!) {
            projectPullConfiguration(projectId: $projectId) {
                files {
                    fileName
                    encryptedContents
                    fingerprint
                }
                currentRevision {
        %s
                }
            }
        }""".formatted(REVISION_FIELDS.indent(12));
    private static final String PUSH_CONFIGURATION_MUTATION = """
        mutation(
            $apiKeyId: ID!, $projectId: ID!,
            $projectContent: ProjectConfigurationInput!, $lastKnownConfigurationFingerprint: String!
        ) {
            revision: pushProjectConfiguration(
                apiKeyId: $apiKeyId
                projectId: $projectId
                projectContent: $projectContent
                lastKnownConfigurationFingerprint: $lastKnownConfigurationFingerprint
            ) {
        %s
            }
        }""".formatted(REVISION_FIELDS.indent(8));
    private static final String PROJECT_REVISIONS_QUERY = """
        query($projectId: ID!, $startTime: DateTime, $endTime: DateTime) {
            revisions: projectRevisions(projectId: $projectId, startTime: $startTime, endTime: $endTime) {
        %s
            }
        }""".formatted(REVISION_FIELDS.indent(8));

    public DDShareClient(@NotNull String url, @NotNull DDDesktopAccessService accessService) {
        super(url, DEFAULT_CONNECT_TIMEOUT, TIMEOUT_MS, List.of(), HttpClient.Redirect.NEVER, accessService);
        this.gson = GsonUtils.gsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeIsoAdapter())
            .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
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
    public List<DDSharedProject> listProjects() throws DDShareException {
        try {
            ProjectsData data = executeGraphQl(LIST_PROJECTS_QUERY, ProjectsData.class, "GraphQL request failed");
            return List.of(data.projects());
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
            variables.put("apiKeyId", requireCredentialBundle().apiKeyId());
            variables.put(FIELD_NAME, name);
            variables.put(FIELD_DESCRIPTION, description);
            ProjectData data = executeGraphQl(CREATE_PROJECT_MUTATION, variables, ProjectData.class, "GraphQL request failed");
            return data.project();
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
            variables.put(FIELD_NAME, name);
            variables.put(FIELD_DESCRIPTION, description);
            ProjectData data = executeGraphQl(UPDATE_PROJECT_MUTATION, variables, ProjectData.class, "GraphQL request failed");
            if (data.project() == null) {
                throw new DDShareException("Project not found: " + projectId);
            }
            return data.project();
        } catch (DBException e) {
            throw new DDShareException("Failed to update project", e);
        }
    }

    @Override
    public boolean deleteProject(@NotNull UUID projectId) throws DDShareException {
        try {
            DeleteData data = executeGraphQl(DELETE_PROJECT_MUTATION, Map.of("projectId", projectId.toString()),
                DeleteData.class, "GraphQL request failed");
            return Boolean.TRUE.equals(data.deleteProject());
        } catch (DBException e) {
            throw new DDShareException("Failed to delete project", e);
        }
    }

    @NotNull
    @Override
    public DDSharedProjectRevision getCurrentProjectRevision(@NotNull UUID projectId) throws DDShareException {
        try {
            RevisionData data = executeGraphQl(CURRENT_REVISION_QUERY, Map.of("projectId", projectId.toString()),
                RevisionData.class, "GraphQL request failed");
            if (data.revision() == null) {
                throw new DDShareException("Project not found or has no current revision: " + projectId);
            }
            return data.revision();
        } catch (DBException e) {
            throw new DDShareException("Failed to get current project revision", e);
        }
    }

    @NotNull
    @Override
    public DDSharedProjectPullResponse pullProjectConfiguration(@NotNull UUID projectId) throws DDShareException {
        try {
            PullData data = executeGraphQl(PULL_CONFIGURATION_QUERY, Map.of("projectId", projectId.toString()),
                PullData.class, "GraphQL request failed");
            if (data.projectPullConfiguration() == null) {
                throw new DDShareException("Project not found or has no current revision: " + projectId);
            }
            return data.projectPullConfiguration();
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
            if (!response.files().isEmpty()) {
                SecretKey key = getDataKey();
                for (DDSharedProjectFile file : response.files()) {
                    files.put(file.fileName(), decryptBytes(key, projectId.toString(), file.fileName(), file.encryptedContents()));
                }
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
            return pushProjectConfiguration(projectId, projectContent, lastKnownConfigurationFingerprint, requireCredentialBundle());
        } catch (DBException e) {
            throw new DDShareException("Failed to push project configuration", e);
        }
    }

    @NotNull
    private DDSharedProjectRevision pushProjectConfiguration(
        @NotNull UUID projectId,
        @NotNull DDSharedProjectConfiguration projectContent,
        @NotNull String lastKnownConfigurationFingerprint,
        @NotNull DDKeyBundle bundle
    ) throws DDShareException {
        try {
            RevisionData data = executeGraphQl(PUSH_CONFIGURATION_MUTATION,
                Map.of(
                    "projectId", projectId.toString(),
                    "projectContent", projectContent,
                    "lastKnownConfigurationFingerprint", lastKnownConfigurationFingerprint,
                    "apiKeyId", Objects.requireNonNull(bundle.apiKeyId())),
                RevisionData.class, "GraphQL request failed");
            if (data.revision() == null) {
                throw new DDShareException("Project not found or configuration fingerprint is stale: " + projectId);
            }
            return data.revision();
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
            DDKeyBundle bundle = requireCredentialBundle();
            List<DDSharedProjectFile> projectFiles = new ArrayList<>();
            if (!preparedFiles.files().isEmpty()) {
                SecretKey key = new DDBundleCredentials(bundle).getDataKey();
                for (Map.Entry<String, Pair<String, byte[]>> file : preparedFiles.files().entrySet()) {
                    Pair<String, byte[]> preparedFile = file.getValue();
                    projectFiles.add(new DDSharedProjectFile(
                        file.getKey(),
                        encryptBytes(key, projectId.toString(), file.getKey(), preparedFile.getSecond()),
                        preparedFile.getFirst()));
                }
            }
            return pushProjectConfiguration(
                projectId,
                new DDSharedProjectConfiguration(preparedFiles.configurationFingerprint(), projectFiles),
                lastKnownConfigurationFingerprint, bundle);
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
            RevisionsData data = executeGraphQl(PROJECT_REVISIONS_QUERY, variables, RevisionsData.class, "GraphQL request failed");
            return List.of(data.revisions());
        } catch (DBException e) {
            throw new DDShareException("Failed to get project revisions", e);
        }
    }

    @NotNull
    private String encryptBytes(
        @NotNull SecretKey key, @NotNull String projectId, @NotNull String field, @NotNull byte[] data
    ) throws DBException {
        return Base64.getEncoder().encodeToString(DDCrypto.encrypt(key, data, aad(projectId, field)));
    }

    @NotNull
    private byte[] decryptBytes(
        @NotNull SecretKey key, @NotNull String projectId, @NotNull String field, @NotNull String ciphertext
    ) throws DBException {
        return DDCrypto.decrypt(key, decodeBase64(ciphertext), aad(projectId, field));
    }

    @NotNull
    private DDKeyBundle requireCredentialBundle() throws DBException {
        DDKeyBundle bundle = accessService.getKeyBundle();
        if (bundle == null || bundle.apiKeyId() == null) {
            throw new DBException("Import the current DataDam recovery phrase before sharing projects");
        }
        return bundle;
    }

    @NotNull
    private SecretKey getDataKey() throws DBException {
        DDKeyBundle bundle = accessService.getKeyBundle();
        if (bundle == null) {
            throw new DBException("Import encryption keys for the current DataDam account before transferring files");
        }
        return new DDBundleCredentials(bundle).getDataKey();
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

    private record ProjectsData(@NotNull DDSharedProject[] projects) {
    }

    private record ProjectData(@Nullable DDSharedProject project) {
    }

    private record DeleteData(@Nullable Boolean deleteProject) {
    }

    private record RevisionData(@Nullable DDSharedProjectRevision revision) {
    }

    private record PullData(@Nullable DDSharedProjectPullResponse projectPullConfiguration) {
    }

    private record RevisionsData(@NotNull DDSharedProjectRevision[] revisions) {
    }

    @NotNull
    @Override
    protected DBException mapGraphQlError(@NotNull String fallback, @Nullable List<?> errors) {
        if (errors == null || errors.isEmpty() || !(errors.get(0) instanceof Map<?, ?> first)) {
            return new DBException(fallback);
        }
        String message = first.get("message") instanceof String detail ? detail : fallback;
        if ("INTERNAL_ERROR".equals(extractClassification(first))) {
            return new DDTransportException(message);
        }
        return new DBException(message);
    }

    @Nullable
    private String extractClassification(@NotNull Map<?, ?> error) {
        if (!(error.get("extensions") instanceof Map<?, ?> extensions)) {
            return null;
        }
        return extensions.get("classification") instanceof String classification ? classification : null;
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
}
