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
package org.jkiss.dbeaver.model.datadam.sync;

import com.dbeaver.datadam.share.api.exception.DDShareException;
import com.dbeaver.datadam.share.api.model.DDSharedProject;
import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import com.dbeaver.datadam.share.api.model.DDSharedProjectFile;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import com.dbeaver.datadam.share.api.utils.DDFingerprintUtils;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSharedProjectPullResult;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DDProjectShareService {
    public static final String BINDING_PROPERTY = "datadam.share";
    private static final List<String> FILE_NAMES = List.of(
        DBPDataSourceRegistry.MODERN_CONFIG_FILE_NAME,
        DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_NAME,
        "tasks.json");
    private static final Object SYNC_LOCK = new Object();

    private final DDShareClient client;
    private final String accountId;

    public DDProjectShareService(@NotNull DDShareClient client, @NotNull String accountId) {
        this.client = client;
        this.accountId = accountId;
    }

    @NotNull
    public List<DDSharedProject> listProjects(@NotNull DBRProgressMonitor monitor) throws DBException {
        checkCancelled(monitor);
        try {
            return client.listProjects();
        } catch (DDShareException e) {
            throw new DBException("Cannot list shared projects", e);
        }
    }

    @Nullable
    public UUID getRemoteProjectId(@NotNull DBPProject project) throws DBException {
        Map<?, ?> binding = binding(project);
        if (binding == null) {
            return null;
        }
        try {
            return UUID.fromString((String) binding.get("projectId"));
        } catch (RuntimeException e) {
            throw new DBException("Invalid project sharing settings", e);
        }
    }

    public void upload(@NotNull DBRProgressMonitor monitor, @NotNull DBPProject project) throws DBException {
        synchronized (SYNC_LOCK) {
            checkCancelled(monitor);
            try {
                Map<String, byte[]> files;
                synchronized (project.getDataSourceRegistry()) {
                    flush(project);
                    files = DDProjectConfigurationCodec.toPortable(project, readFiles(project));
                }
                UUID projectId = getRemoteProjectId(project);
                if (projectId == null) {
                    UUID userId = client.getCurrentShareUserId();
                    projectId = UUID.randomUUID();
                    client.createProject(projectId, project.getName(), null);
                    saveBinding(project, projectId, userId,
                        DDFingerprintUtils.calculateConfigurationFingerprint(projectId, List.of()),
                        fingerprint(projectId, files), null);
                }
                String previous = (String) binding(project).get("lastSyncChecksum");
                checkCancelled(monitor);
                DDSharedProjectRevision revision = client.pushFiles(
                    projectId, files, previous, DDSharedProjectConfiguration.Format.PORTABLE_JSON);
                saveBinding(project, projectId, revision.userId(), revision.configurationFingerprint(),
                    fingerprint(projectId, files), Instant.now());
            } catch (DDShareException | IOException e) {
                throw new DBException("Cannot upload shared project", e);
            }
        }
    }

    public void download(
        @NotNull DBRProgressMonitor monitor, @NotNull DBPProject project, @NotNull UUID remoteProjectId, boolean replaceLocalChanges
    ) throws DBException {
        synchronized (SYNC_LOCK) {
            checkCancelled(monitor);
            try {
                UUID boundId = getRemoteProjectId(project);
                String localFingerprint;
                synchronized (project.getDataSourceRegistry()) {
                    flush(project);
                    localFingerprint = fingerprint(remoteProjectId, DDProjectConfigurationCodec.toPortable(project, readFiles(project)));
                }
                Map<?, ?> binding = binding(project);
                if (!replaceLocalChanges && (!remoteProjectId.equals(boundId) || binding == null
                    || !localFingerprint.equals(binding.get("lastLocalChecksum")))) {
                    throw new DBException("Local project has changes; confirm replacement before downloading");
                }
                UUID userId = client.getCurrentShareUserId();
                DDSharedProjectPullResult remote = client.pullFiles(remoteProjectId);
                if (!FILE_NAMES.containsAll(remote.files().keySet())) {
                    throw new DBException("Shared project contains unsupported files");
                }
                Map<String, byte[]> portable = remote.format() == DDSharedProjectConfiguration.Format.PORTABLE_JSON
                    ? remote.files() : DDProjectConfigurationCodec.toPortable(project, remote.files());
                Map<String, byte[]> local = DDProjectConfigurationCodec.toLocal(project, portable);
                DBTTaskManager taskManager = project.getTaskManager(true);
                byte[] tasks = portable.get("tasks.json");
                if (taskManager != null) {
                    taskManager.validateConfiguration(tasks == null ? null : new String(tasks, StandardCharsets.UTF_8));
                } else if (tasks != null) {
                    throw new DBException("Project does not support task configuration");
                }
                checkCancelled(monitor);
                synchronized (project.getDataSourceRegistry()) {
                    flush(project);
                    Map<String, byte[]> before = readFiles(project);
                    if (!localFingerprint.equals(fingerprint(remoteProjectId, DDProjectConfigurationCodec.toPortable(project, before)))) {
                        throw new DBException("Local project changed while downloading");
                    }
                    String importedChecksum;
                    try {
                        writeFiles(project, local);
                        refresh(project);
                        flush(project);
                        importedChecksum = fingerprint(remoteProjectId,
                            DDProjectConfigurationCodec.toPortable(project, readFiles(project)));
                    } catch (IOException | DBException | RuntimeException e) {
                        try {
                            writeFiles(project, before);
                            refresh(project);
                        } catch (IOException | DBException | RuntimeException restoreError) {
                            e.addSuppressed(restoreError);
                        }
                        throw new DBException("Cannot apply shared project configuration", e);
                    }
                    saveBinding(project, remoteProjectId, userId, remote.configurationFingerprint(), importedChecksum, Instant.now());
                }
            } catch (DDShareException | IOException e) {
                throw new DBException("Cannot download shared project", e);
            }
        }
    }

    @Nullable
    private Map<?, ?> binding(@NotNull DBPProject project) throws DBException {
        Object value = project.getProjectProperty(BINDING_PROPERTY);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?> binding) || !accountId.equals(binding.get("accountId"))
            || !(binding.get("lastSyncChecksum") instanceof String)) {
            throw new DBException("Project is bound to another account or has invalid sharing settings");
        }
        return binding;
    }

    private void saveBinding(
        @NotNull DBPProject project, @NotNull UUID projectId, @NotNull UUID userId,
        @NotNull String checksum, @NotNull String localChecksum, @Nullable Instant syncTime
    ) throws IOException {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("projectId", projectId.toString());
        binding.put("accountId", accountId);
        binding.put("userId", userId.toString());
        binding.put("lastSyncChecksum", checksum);
        binding.put("lastLocalChecksum", localChecksum);
        binding.put("lastSyncTime", syncTime == null ? null : syncTime.toString());
        long modified = 0;
        for (String name : FILE_NAMES) {
            Path path = project.getMetadataFolder(false).resolve(name);
            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                modified = Math.max(modified, Files.getLastModifiedTime(path).toMillis());
            }
        }
        binding.put("lastUpdateTime", Instant.ofEpochMilli(modified).toString());
        project.setProjectProperty(BINDING_PROPERTY, binding);
    }

    @NotNull
    private static Map<String, byte[]> readFiles(@NotNull DBPProject project) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        Path folder = project.getMetadataFolder(false);
        if (Files.isSymbolicLink(folder)) {
            throw new IOException("Project metadata must not be a symbolic link");
        }
        for (String name : FILE_NAMES) {
            Path path = folder.resolve(name);
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 7_000_000) {
                    throw new IOException("Unsupported project file: " + name);
                }
                files.put(name, Files.readAllBytes(path));
            }
        }
        return files;
    }

    private static void writeFiles(@NotNull DBPProject project, @NotNull Map<String, byte[]> files) throws IOException {
        Path folder = project.getMetadataFolder(true);
        for (String name : FILE_NAMES) {
            Path path = folder.resolve(name);
            byte[] contents = files.get(name);
            if (contents == null) {
                Files.deleteIfExists(path);
            } else {
                Path temporary = Files.createTempFile(folder, ".share-", ".tmp");
                try {
                    Files.write(temporary, contents);
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
        }
    }

    @NotNull
    private static String fingerprint(@NotNull UUID projectId, @NotNull Map<String, byte[]> files) {
        return DDFingerprintUtils.calculateConfigurationFingerprint(projectId, files.entrySet().stream()
            .map(file -> new DDSharedProjectFile(file.getKey(), "",
                DDFingerprintUtils.calculateFileFingerprint(projectId, file.getKey(), file.getValue())))
            .toList());
    }

    private static void flush(@NotNull DBPProject project) throws DBException {
        if (project.isUseSecretStorage()) {
            throw new DBException("Project sharing requires credentials stored in project files");
        }
        DBTTaskManager taskManager = project.getTaskManager(false);
        if (taskManager != null && taskManager.hasRunningTasks()) {
            throw new DBException("Cannot synchronize a project while tasks are running");
        }
        project.getDataSourceRegistry().flushConfigSync();
    }

    private static void refresh(@NotNull DBPProject project) throws DBException {
        project.getDataSourceRegistry().refreshConfig();
        project.getDataSourceRegistry().checkForErrors();
        DBTTaskManager taskManager = project.getTaskManager(false);
        if (taskManager != null) {
            taskManager.refreshConfiguration();
        }
    }

    private static void checkCancelled(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (monitor.isCanceled()) {
            throw new DBException("Project sharing cancelled");
        }
    }
}
