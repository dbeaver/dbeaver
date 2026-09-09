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

import com.dbeaver.datadam.share.api.model.DDCreateProjectRequest;
import com.dbeaver.datadam.share.api.model.DDPushProjectConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDSharedProject;
import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import com.dbeaver.datadam.share.api.model.DDSharedProjectFile;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import com.dbeaver.datadam.share.api.model.DDUpdateProjectRequest;
import com.dbeaver.datadam.share.api.utils.DDFingerprintUtils;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.jkiss.dbeaver.model.meta.ForTest;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;

public class DDProjectSyncStore {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";

    private final DDProjectSyncTransport transport;
    private final DDSyncCredentials credentials;

    private SecretKey dataKey;

    public DDProjectSyncStore(@NotNull String url, @NotNull DDSyncCredentials credentials) {
        this(new DDGraphQlTransport(url, credentials), credentials);
    }

    public DDProjectSyncStore(@NotNull DDProjectSyncTransport transport, @NotNull DDSyncCredentials credentials) {
        this.transport = transport;
        this.credentials = credentials;
    }

    @NotNull
    public List<DDProjectInfo> listProjects() throws DBException {
        List<DDProjectInfo> result = new ArrayList<>();
        for (DDSharedProject project : transport.listProjects()) {
            result.add(decode(project));
        }
        return result;
    }

    @NotNull
    public DDProjectInfo createProject(
        @NotNull String projectId,
        @NotNull String name,
        @Nullable String description
    ) throws DBException {
        DDSharedProject created = transport.createProject(new DDCreateProjectRequest(
            UUID.fromString(projectId),
            encryptText(projectId, FIELD_NAME, name),
            description == null ? null : encryptText(projectId, FIELD_DESCRIPTION, description)));
        return decode(created);
    }

    @Nullable
    public DDProjectInfo updateProject(
        @NotNull String projectId,
        @NotNull String name,
        @Nullable String description
    ) throws DBException {
        DDSharedProject updated = transport.updateProject(projectId, new DDUpdateProjectRequest(
            encryptText(projectId, FIELD_NAME, name),
            description == null ? null : encryptText(projectId, FIELD_DESCRIPTION, description)));
        return updated == null ? null : decode(updated);
    }

    public boolean deleteProject(@NotNull String projectId) throws DBException {
        return transport.deleteProject(projectId);
    }

    @Nullable
    public DDProjectPullResult pullFiles(@NotNull String projectId) throws DBException {
        DDSharedProjectConfiguration remote = transport.pullProjectConfiguration(projectId);
        if (remote == null) {
            return null;
        }
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (DDSharedProjectFile file : remote.files()) {
            files.put(file.fileName(), decryptBytes(projectId, file.fileName(), file.encryptedContents()));
        }
        return new DDProjectPullResult(remote.configurationFingerprint(), files);
    }

    /**
     * Encrypts and pushes the whole file set as one atomic, checksum-guarded replace. Returns
     * null if the project does not exist, or if lastKnownConfigurationFingerprint no longer
     * matches the server's current one - the caller should pull and reconcile.
     */
    @Nullable
    public DDSharedProjectRevision pushFiles(
        @NotNull String projectId,
        @NotNull Map<String, byte[]> files,
        @NotNull String lastKnownConfigurationFingerprint
    ) throws DBException {
        UUID id = parseProjectId(projectId);
        List<DDSharedProjectFile> projectFiles = new ArrayList<>();
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            String fingerprint = DDFingerprintUtils.calculateFileFingerprint(id, file.getKey(), file.getValue());
            projectFiles.add(new DDSharedProjectFile(
                file.getKey(), encryptBytes(projectId, file.getKey(), file.getValue()), fingerprint));
        }
        String configurationFingerprint = DDFingerprintUtils.calculateConfigurationFingerprint(id, projectFiles);
        return transport.pushProjectConfiguration(projectId, new DDPushProjectConfigurationRequest(
            new DDSharedProjectConfiguration(configurationFingerprint, projectFiles),
            lastKnownConfigurationFingerprint));
    }

    @NotNull
    private DDProjectInfo decode(@NotNull DDSharedProject project) throws DBException {
        String projectId = project.id().toString();
        return new DDProjectInfo(
            projectId,
            decryptText(projectId, FIELD_NAME, project.name()),
            project.description() == null ? null : decryptText(projectId, FIELD_DESCRIPTION, project.description()),
            project.projectOwner().toString(),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(project.createTime()),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(project.updateTime()));
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

    @NotNull
    private static UUID parseProjectId(@NotNull String projectId) throws DBException {
        try {
            return UUID.fromString(projectId);
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid project id", e);
        }
    }

    /**
     * Binds projectId + field/file name to the ciphertext via AES-GCM AAD, so a valid ciphertext
     * can't be relabeled as belonging to another project or field.
     */
    @NotNull
    @ForTest
    static byte[] aad(@NotNull String projectId, @NotNull String field) {
        return (projectId + '\u0000' + field).getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    private SecretKey getDataKey() throws DBException {
        if (dataKey == null) {
            dataKey = credentials.getDataKey();
        }
        return dataKey;
    }
}
