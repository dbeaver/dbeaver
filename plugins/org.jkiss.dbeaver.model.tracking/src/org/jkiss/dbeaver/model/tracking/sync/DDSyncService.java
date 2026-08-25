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
package org.jkiss.dbeaver.model.tracking.sync;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.sync.DBPSyncRegistry;
import org.jkiss.dbeaver.model.sync.DBPSyncScope;
import org.jkiss.dbeaver.model.sync.DBPSyncSettings;
import org.jkiss.dbeaver.model.sync.DBPSyncTarget;
import org.jkiss.dbeaver.model.sync.DBPSyncUnit;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfiguration;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationPart;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationPartKind;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationSummary;
import org.jkiss.dbeaver.model.tracking.sync.core.DDSyncCredentials;
import org.jkiss.dbeaver.model.tracking.sync.core.DDSyncStore;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DDSyncService {

    private static final Log log = Log.getLog(DDSyncService.class);

    public static final String BINDING_FILE = ".synchronize";
    public static final String PROP_PROJECT_ID = "datadam.project-id";

    private static final String KEY_ACCOUNT_PREFIX = "account:";
    private static final String KEY_PROJECT_PREFIX = "project:";

    private final DDSyncStore store;
    private final DBPWorkspace workspace;
    private final String accountId;

    public DDSyncService(
        @NotNull String url,
        @NotNull DDSyncCredentials credentials,
        @NotNull DBPWorkspace workspace,
        @NotNull String accountId
    ) {
        this.store = new DDSyncStore(url, credentials);
        this.workspace = workspace;
        this.accountId = accountId;
    }

    @NotNull
    public List<DDConfigurationSummary> listConfigurations() throws DBException {
        return store.listConfigurations();
    }

    @NotNull
    public List<DDPartSelection> getAvailableParts() {
        List<DDPartSelection> parts = new ArrayList<>();
        for (DBPSyncUnit unit : enabledUnits(DBPSyncScope.WORKSPACE)) {
            parts.add(new DDPartSelection(KEY_ACCOUNT_PREFIX + unit.getId(), unit.getDisplayName()));
        }
        if (!enabledUnits(DBPSyncScope.PROJECT).isEmpty()) {
            for (DBPProject project : workspace.getProjects()) {
                if (DBPSyncSettings.isEnabled(project)) {
                    parts.add(new DDPartSelection(KEY_PROJECT_PREFIX + getProjectId(project), project.getName()));
                }
            }
        }
        return parts;
    }

    @Nullable
    public DDSyncBinding getBinding() {
        DDSyncBinding binding = readBinding(workspace.getAbsolutePath());
        return binding != null && accountId.equals(binding.accountId()) ? binding : null;
    }

    @Nullable
    public static DDSyncBinding readBinding(@NotNull Path workspacePath) {
        Path file = workspacePath.resolve(BINDING_FILE);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return JSONUtils.GSON.fromJson(Files.readString(file), DDSyncBinding.class);
        } catch (IOException | RuntimeException e) {
            log.debug("Error reading synchronization binding", e);
            return null;
        }
    }

    @NotNull
    public DDSyncResult createConfiguration(
        @NotNull String name,
        @NotNull List<String> selectedKeys
    ) throws DBException {
        List<DDConfigurationPart> parts = new ArrayList<>();
        for (String key : selectedKeys) {
            DDConfigurationPart part = buildNewPart(key);
            if (part == null) {
                throw new DBException("Synchronization part is not available: " + key);
            }
            parts.add(part);
        }
        if (parts.isEmpty()) {
            throw new DBException("Select at least one synchronization part");
        }
        DDConfiguration configuration = store.createConfiguration(name, parts);
        Map<String, String> localFingerprints = new LinkedHashMap<>();
        for (DDConfigurationPart part : parts) {
            localFingerprints.put(part.key(), store.fingerprint(part));
        }
        Map<String, DDSyncPartState> baselines = new LinkedHashMap<>();
        for (DDConfigurationPart part : configuration.parts()) {
            String fingerprint = localFingerprints.get(part.key());
            if (fingerprint != null) {
                baselines.put(part.key(), new DDSyncPartState(part.version(), fingerprint));
            }
        }
        bind(configuration.configurationId(), configuration.name(), configuration.version(), baselines);
        return new DDSyncResult(configuration.name(), parts.stream().map(DDConfigurationPart::name).toList());
    }

    @NotNull
    public DDSyncResult upload() throws DBException {
        DDSyncBinding binding = requireBinding();
        DDConfiguration remote = store.getConfiguration(binding.configurationId());
        List<DDConfigurationPart> changed = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        Map<String, String> localFingerprints = new LinkedHashMap<>();

        for (DDConfigurationPart remotePart : remote.parts()) {
            DDConfigurationPart local = readCurrentPart(remotePart);
            if (local == null) {
                continue;
            }
            String fingerprint = store.fingerprint(local);
            localFingerprints.put(remotePart.key(), fingerprint);
            DDSyncChange change = classify(
                binding.parts().get(remotePart.key()), fingerprint, remotePart.version());
            if (change == DDSyncChange.CONFLICT) {
                conflicts.add(remotePart.name());
            } else if (change == DDSyncChange.LOCAL) {
                changed.add(new DDConfigurationPart(
                    local.key(), local.kind(), local.projectId(), remotePart.version(), local.name(), local.units()));
            }
        }
        if (!conflicts.isEmpty()) {
            throw new DDLocalSyncConflictException(conflicts);
        }

        DDConfiguration result = changed.isEmpty()
            ? remote
            : store.updateConfiguration(remote.configurationId(), remote.version(), changed);

        Map<String, DDSyncPartState> baselines = new LinkedHashMap<>();
        for (DDConfigurationPart part : result.parts()) {
            String fingerprint = localFingerprints.get(part.key());
            DDSyncPartState state = fingerprint == null
                ? binding.parts().get(part.key())
                : new DDSyncPartState(part.version(), fingerprint);
            if (state != null) {
                baselines.put(part.key(), state);
            }
        }
        bind(result.configurationId(), result.name(), result.version(), baselines);
        return new DDSyncResult(result.name(), changed.stream().map(DDConfigurationPart::name).toList());
    }

    @NotNull
    public DDSyncResult download() throws DBException {
        DDSyncBinding binding = requireBinding();
        DDConfiguration remote = store.getConfiguration(binding.configurationId());
        List<DDConfigurationPart> toApply = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        Map<String, DDSyncPartState> baselines = new LinkedHashMap<>(binding.parts());

        for (DDConfigurationPart remotePart : remote.parts()) {
            DDConfigurationPart local = readCurrentPart(remotePart);
            if (local == null) {
                toApply.add(remotePart);
                continue;
            }
            DDSyncChange change = classify(
                binding.parts().get(remotePart.key()), store.fingerprint(local), remotePart.version());
            if (change == DDSyncChange.CONFLICT) {
                conflicts.add(remotePart.name());
            } else if (change == DDSyncChange.SERVER) {
                toApply.add(remotePart);
            }
        }
        if (!conflicts.isEmpty()) {
            throw new DDLocalSyncConflictException(conflicts);
        }

        validateParts(toApply);
        for (DDConfigurationPart part : toApply) {
            apply(part);
            baselines.put(part.key(), new DDSyncPartState(part.version(), store.fingerprint(part)));
        }
        bind(remote.configurationId(), remote.name(), remote.version(), baselines);
        return new DDSyncResult(remote.name(), toApply.stream().map(DDConfigurationPart::name).toList());
    }

    @NotNull
    public DDSyncResult downloadAndBind(@NotNull String configurationId) throws DBException {
        DDConfiguration remote = store.getConfiguration(configurationId);
        validateParts(remote.parts());
        Map<String, DDSyncPartState> baselines = new LinkedHashMap<>();
        for (DDConfigurationPart part : remote.parts()) {
            apply(part);
            baselines.put(part.key(), new DDSyncPartState(part.version(), store.fingerprint(part)));
        }
        bind(remote.configurationId(), remote.name(), remote.version(), baselines);
        return new DDSyncResult(remote.name(), remote.parts().stream().map(DDConfigurationPart::name).toList());
    }

    @NotNull
    public static DDSyncChange classify(
        @Nullable DDSyncPartState baseline,
        @NotNull String localFingerprint,
        long remoteVersion
    ) {
        if (baseline == null) {
            return DDSyncChange.CONFLICT;
        }
        boolean localChanged = !baseline.fingerprint().equals(localFingerprint);
        boolean remoteChanged = baseline.version() != remoteVersion;
        if (localChanged && remoteChanged) {
            return DDSyncChange.CONFLICT;
        }
        if (localChanged) {
            return DDSyncChange.LOCAL;
        }
        return remoteChanged ? DDSyncChange.SERVER : DDSyncChange.UNCHANGED;
    }

    private void apply(@NotNull DDConfigurationPart part) throws DBException {
        DBPProject project = part.kind() == DDConfigurationPartKind.PROJECT
            ? resolveProject(part.projectId(), part.name())
            : null;
        DBPSyncTarget target = new DBPSyncTarget(workspace, project);
        for (Map.Entry<String, Map<String, byte[]>> unitResources : part.units().entrySet()) {
            DBPSyncUnit unit = requireUnit(unitResources.getKey(), part.kind());
            unit.write(target, unitResources.getValue());
        }
    }

    private void validateParts(@NotNull List<DDConfigurationPart> parts) throws DBException {
        for (DDConfigurationPart part : parts) {
            if (part.kind() == DDConfigurationPartKind.PROJECT && CommonUtils.isEmpty(part.projectId())) {
                throw new DBException("Missing project id in synchronization part " + part.name());
            }
            for (String unitId : part.units().keySet()) {
                requireUnit(unitId, part.kind());
            }
        }
    }

    @Nullable
    private DDConfigurationPart readCurrentPart(@NotNull DDConfigurationPart remote) throws DBException {
        if (remote.kind() == DDConfigurationPartKind.ACCOUNT) {
            String unitId = remote.key().substring(KEY_ACCOUNT_PREFIX.length());
            DBPSyncUnit unit = requireUnit(unitId, DDConfigurationPartKind.ACCOUNT);
            Map<String, byte[]> resources = unit.read(new DBPSyncTarget(workspace, null));
            return new DDConfigurationPart(
                remote.key(), DDConfigurationPartKind.ACCOUNT, null, 0, unit.getDisplayName(), Map.of(unitId, resources));
        }
        DBPProject project = findProjectById(remote.projectId());
        if (project == null) {
            return null;
        }
        Map<String, Map<String, byte[]>> units = new LinkedHashMap<>();
        DBPSyncTarget target = new DBPSyncTarget(workspace, project);
        for (String unitId : remote.units().keySet()) {
            DBPSyncUnit unit = requireUnit(unitId, DDConfigurationPartKind.PROJECT);
            units.put(unitId, unit.read(target));
        }
        return new DDConfigurationPart(
            remote.key(), DDConfigurationPartKind.PROJECT, remote.projectId(), 0, project.getName(), units);
    }

    @Nullable
    private DDConfigurationPart buildNewPart(@NotNull String key) throws DBException {
        if (key.startsWith(KEY_ACCOUNT_PREFIX)) {
            String unitId = key.substring(KEY_ACCOUNT_PREFIX.length());
            DBPSyncUnit unit = DBPSyncRegistry.getInstance().findById(unitId);
            if (unit == null || unit.getScope() != DBPSyncScope.WORKSPACE) {
                return null;
            }
            Map<String, byte[]> resources = unit.read(new DBPSyncTarget(workspace, null));
            return new DDConfigurationPart(
                key, DDConfigurationPartKind.ACCOUNT, null, 0, unit.getDisplayName(), Map.of(unitId, resources));
        }
        String projectId = key.substring(KEY_PROJECT_PREFIX.length());
        DBPProject project = findProjectById(projectId);
        if (project == null) {
            return null;
        }
        Map<String, Map<String, byte[]>> units = new LinkedHashMap<>();
        DBPSyncTarget target = new DBPSyncTarget(workspace, project);
        for (DBPSyncUnit unit : enabledUnits(DBPSyncScope.PROJECT)) {
            units.put(unit.getId(), unit.read(target));
        }
        return new DDConfigurationPart(key, DDConfigurationPartKind.PROJECT, projectId, 0, project.getName(), units);
    }

    @NotNull
    private List<DBPSyncUnit> enabledUnits(@NotNull DBPSyncScope scope) {
        return DBPSyncRegistry.getInstance().getUnits().stream()
            .filter(unit -> unit.getScope() == scope && DBPSyncSettings.isEnabled(unit))
            .toList();
    }

    @NotNull
    private DBPSyncUnit requireUnit(@NotNull String unitId, @NotNull DDConfigurationPartKind kind) throws DBException {
        DBPSyncUnit unit = DBPSyncRegistry.getInstance().findById(unitId);
        DBPSyncScope scope = kind == DDConfigurationPartKind.PROJECT ? DBPSyncScope.PROJECT : DBPSyncScope.WORKSPACE;
        if (unit == null || unit.getScope() != scope) {
            throw new DBException("Unknown synchronization unit: " + unitId);
        }
        return unit;
    }

    @NotNull
    private DDSyncBinding requireBinding() throws DBException {
        DDSyncBinding binding = getBinding();
        if (binding == null) {
            throw new DBException("No configuration is bound to this workspace");
        }
        return binding;
    }

    private void bind(
        @NotNull String configurationId,
        @Nullable String name,
        long configurationVersion,
        @NotNull Map<String, DDSyncPartState> parts
    ) throws DBException {
        DDSyncBinding binding = new DDSyncBinding(configurationId, name, accountId, configurationVersion, parts);
        Path file = workspace.getAbsolutePath().resolve(BINDING_FILE);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(workspace.getAbsolutePath(), BINDING_FILE + ".", ".tmp");
            Files.writeString(temporary, JSONUtils.GSON.toJson(binding));
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DBException("Error writing synchronization binding", e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    log.debug("Error deleting temporary synchronization binding", e);
                }
            }
        }
    }

    @Nullable
    private DBPProject findProjectById(@Nullable String projectId) {
        if (projectId == null) {
            return null;
        }
        for (DBPProject project : workspace.getProjects()) {
            if (projectId.equals(CommonUtils.toString(project.getProjectProperty(PROP_PROJECT_ID), null))) {
                return project;
            }
        }
        return null;
    }

    @NotNull
    private DBPProject resolveProject(@Nullable String projectId, @Nullable String projectName) throws DBException {
        if (projectId == null) {
            throw new DBException("Project id is missing in synchronization data");
        }
        DBPProject existing = findProjectById(projectId);
        if (existing != null) {
            return existing;
        }
        String baseName = CommonUtils.isEmpty(projectName) ? projectId : projectName;
        String name = baseName;
        for (int suffix = 2; workspace.getProject(name) != null; suffix++) {
            name = baseName + " (" + suffix + ")";
        }
        DBPProject created = workspace.createProject(name, null);
        created.setProjectProperty(PROP_PROJECT_ID, projectId);
        return created;
    }

    @NotNull
    private static String getProjectId(@NotNull DBPProject project) {
        String projectId = CommonUtils.toString(project.getProjectProperty(PROP_PROJECT_ID), null);
        if (CommonUtils.isEmpty(projectId)) {
            projectId = UUID.randomUUID().toString();
            project.setProjectProperty(PROP_PROJECT_ID, projectId);
        }
        return projectId;
    }
}
