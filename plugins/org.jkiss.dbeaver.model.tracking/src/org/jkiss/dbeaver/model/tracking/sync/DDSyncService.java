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

import com.dbeaver.datadam.gateway.model.DDConfigurationPartKind;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sync.DBPSyncRegistry;
import org.jkiss.dbeaver.model.sync.DBPSyncScope;
import org.jkiss.dbeaver.model.sync.DBPSyncSettings;
import org.jkiss.dbeaver.model.sync.DBPSyncTarget;
import org.jkiss.dbeaver.model.sync.DBPSyncUnit;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfiguration;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationPart;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationSummary;
import org.jkiss.dbeaver.model.tracking.sync.core.DDSyncCredentials;
import org.jkiss.dbeaver.model.tracking.sync.core.DDSyncStore;
import org.jkiss.dbeaver.model.tracking.sync.core.DDUpdateConfigurationResult;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DDSyncService {

    private static final Log log = Log.getLog(DDSyncService.class);

    public static final String BINDING_FILE = ".synchronize";
    public static final String PROP_PROJECT_ID = "datadam.project-id";

    private static final String KEY_ACCOUNT_PREFIX = "account:";
    private static final String KEY_PROJECT_PREFIX = "project:";
    private static final Object SYNC_LOCK = new Object();

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
            parts.add(new DDPartSelection(KEY_ACCOUNT_PREFIX + unit.getId(), unit.getName(), DBPSyncScope.WORKSPACE));
        }
        if (!enabledUnits(DBPSyncScope.PROJECT).isEmpty()) {
            for (DBPProject project : workspace.getProjects()) {
                if (DBPSyncSettings.isEnabled(project)) {
                    parts.add(new DDPartSelection(
                        KEY_PROJECT_PREFIX + getProjectId(project), project.getName(), DBPSyncScope.PROJECT));
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
            DDSyncBinding binding = JSONUtils.GSON.fromJson(Files.readString(file), DDSyncBinding.class);
            return binding != null
                && !CommonUtils.isEmpty(binding.configurationId())
                && !CommonUtils.isEmpty(binding.accountId())
                && binding.parts() != null
                ? binding
                : null;
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
        synchronized (SYNC_LOCK) {
            List<DDConfigurationPart> parts = new ArrayList<>();
            for (String key : selectedKeys) {
                DDConfigurationPart part = readLocalPart(key, null);
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
                    baselines.put(part.key(), new DDSyncPartState(part.version(), fingerprint, new LinkedHashSet<>(part.units().keySet())));
                }
            }
            bind(configuration.configurationId(), configuration.name(), configuration.version(), baselines);
            return new DDSyncResult(configuration.name(), parts.stream().map(DDConfigurationPart::name).toList());
        }
    }

    @NotNull
    public DDSyncResult upload(@NotNull DBRProgressMonitor monitor) throws DBException {
        synchronized (SYNC_LOCK) {
            DDSyncBinding binding = requireBinding();
            List<DDConfigurationPart> candidates = new ArrayList<>();
            Map<String, String> localFingerprints = new LinkedHashMap<>();

            for (Map.Entry<String, DDSyncPartState> entry : binding.parts().entrySet()) {
                String key = entry.getKey();
                DDConfigurationPart local = readLocalPart(key, entry.getValue().unitIds());
                if (local == null) {
                    continue;
                }
                String fingerprint = store.fingerprint(local);
                if (fingerprint.equals(entry.getValue().fingerprint())) {
                    continue;
                }
                localFingerprints.put(key, fingerprint);
                candidates.add(new DDConfigurationPart(
                    local.key(), local.kind(), local.projectId(), entry.getValue().version(), local.name(), local.units()));
            }

            List<String> uploaded = new ArrayList<>();
            List<String> conflicts = new ArrayList<>();
            String configurationName = binding.name();

            if (!candidates.isEmpty()) {
                if (monitor.isCanceled()) {
                    return new DDSyncResult(configurationName, uploaded);
                }
                DDUpdateConfigurationResult result = store.updateConfiguration(
                    binding.configurationId(), candidates, binding.configurationVersion());
                DDConfiguration configuration = result.configuration();
                configurationName = configuration.name();
                Set<String> conflictingKeys = new HashSet<>(result.conflictingKeys());
                Map<String, DDSyncPartState> baselines = new LinkedHashMap<>(binding.parts());
                for (DDConfigurationPart candidate : candidates) {
                    DDConfigurationPart updated = findPart(configuration, candidate.key());
                    String localFingerprint = localFingerprints.get(candidate.key());
                    if (conflictingKeys.contains(candidate.key()) && !store.fingerprint(updated).equals(localFingerprint)) {
                        conflicts.add(candidate.name());
                        continue;
                    }
                    baselines.put(candidate.key(), new DDSyncPartState(
                        updated.version(), localFingerprint, new LinkedHashSet<>(candidate.units().keySet())));
                    uploaded.add(candidate.name());
                }
                bind(configuration.configurationId(), configuration.name(), configuration.version(), baselines);
            }

            if (!conflicts.isEmpty()) {
                throw new DDLocalSyncConflictException(conflicts);
            }
            return new DDSyncResult(configurationName, uploaded);
        }
    }

    @NotNull
    public DDSyncResult download(@NotNull DBRProgressMonitor monitor) throws DBException {
        synchronized (SYNC_LOCK) {
            DDSyncBinding binding = requireBinding();
            if (monitor.isCanceled()) {
                return new DDSyncResult(binding.name(), List.of());
            }
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

            validateParts(toApply);
            List<String> applied = new ArrayList<>();
            try {
                for (DDConfigurationPart part : toApply) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    apply(part);
                    baselines.put(part.key(), new DDSyncPartState(
                        part.version(), fingerprintAfterApply(part), new LinkedHashSet<>(part.units().keySet())));
                    applied.add(part.name());
                }
            } finally {
                bind(remote.configurationId(), remote.name(), remote.version(), baselines);
            }

            if (!conflicts.isEmpty()) {
                throw new DDLocalSyncConflictException(conflicts);
            }
            return new DDSyncResult(remote.name(), applied);
        }
    }

    @NotNull
    public DDSyncResult downloadAndBind(@NotNull String configurationId) throws DBException {
        synchronized (SYNC_LOCK) {
            DDConfiguration remote = store.getConfiguration(configurationId);
            validateParts(remote.parts());
            Map<String, DDSyncPartState> baselines = new LinkedHashMap<>();
            try {
                for (DDConfigurationPart part : remote.parts()) {
                    apply(part);
                    baselines.put(part.key(), new DDSyncPartState(
                        part.version(), fingerprintAfterApply(part), new LinkedHashSet<>(part.units().keySet())));
                }
            } finally {
                bind(remote.configurationId(), remote.name(), remote.version(), baselines);
            }
            return new DDSyncResult(remote.name(), remote.parts().stream().map(DDConfigurationPart::name).toList());
        }
    }

    @NotNull
    public List<DDSyncConflict> getConflicts() throws DBException {
        synchronized (SYNC_LOCK) {
            DDSyncBinding binding = getBinding();
            if (binding == null) {
                return List.of();
            }
            DDConfiguration remote = store.getConfiguration(binding.configurationId());
            List<DDSyncConflict> conflicts = new ArrayList<>();
            for (DDConfigurationPart remotePart : remote.parts()) {
                DDConfigurationPart local = readCurrentPart(remotePart);
                if (local == null) {
                    continue;
                }
                DDSyncChange change = classify(
                    binding.parts().get(remotePart.key()), store.fingerprint(local), remotePart.version());
                if (change == DDSyncChange.CONFLICT) {
                    conflicts.add(new DDSyncConflict(remotePart.key(), remotePart.name()));
                }
            }
            return conflicts;
        }
    }

    @NotNull
    public DDSyncResult forceUpload(@NotNull String partKey) throws DBException {
        synchronized (SYNC_LOCK) {
            DDSyncBinding binding = requireBinding();
            DDConfiguration remote = store.getConfiguration(binding.configurationId());
            DDConfigurationPart remotePart = findPart(remote, partKey);
            DDSyncPartState baseline = binding.parts().get(partKey);
            DDConfigurationPart local = readLocalPart(partKey, baseline != null ? baseline.unitIds() : null);
            if (local == null) {
                throw new DBException("Local data is not available: " + remotePart.name());
            }
            String fingerprint = store.fingerprint(local);
            requireConflict(binding, remotePart, fingerprint);
            DDConfigurationPart toUpload = new DDConfigurationPart(
                local.key(), local.kind(), local.projectId(), remotePart.version(), local.name(), local.units());
            DDUpdateConfigurationResult result = store.updateConfiguration(
                remote.configurationId(), List.of(toUpload), remote.version());
            if (result.conflictingKeys().contains(partKey)) {
                throw new DDLocalSyncConflictException(List.of(remotePart.name()));
            }
            DDConfiguration configuration = result.configuration();
            DDConfigurationPart updated = findPart(configuration, partKey);
            Map<String, DDSyncPartState> baselines = new LinkedHashMap<>(binding.parts());
            baselines.put(partKey, new DDSyncPartState(updated.version(), fingerprint, new LinkedHashSet<>(local.units().keySet())));
            bind(configuration.configurationId(), configuration.name(), configuration.version(), baselines);
            return new DDSyncResult(configuration.name(), List.of(remotePart.name()));
        }
    }

    @NotNull
    public DDSyncResult forceDownload(@NotNull String partKey) throws DBException {
        synchronized (SYNC_LOCK) {
            DDSyncBinding binding = requireBinding();
            DDConfiguration remote = store.getConfiguration(binding.configurationId());
            DDConfigurationPart remotePart = findPart(remote, partKey);
            DDConfigurationPart local = readCurrentPart(remotePart);
            if (local != null) {
                requireConflict(binding, remotePart, store.fingerprint(local));
            }
            apply(remotePart);
            Map<String, DDSyncPartState> baselines = new LinkedHashMap<>(binding.parts());
            baselines.put(partKey, new DDSyncPartState(
                remotePart.version(), fingerprintAfterApply(remotePart), new LinkedHashSet<>(remotePart.units().keySet())));
            bind(remote.configurationId(), remote.name(), remote.version(), baselines);
            return new DDSyncResult(remote.name(), List.of(remotePart.name()));
        }
    }

    @NotNull
    private static DDConfigurationPart findPart(@NotNull DDConfiguration configuration, @NotNull String key) throws DBException {
        for (DDConfigurationPart part : configuration.parts()) {
            if (part.key().equals(key)) {
                return part;
            }
        }
        throw new DBException("Unknown synchronization part: " + key);
    }

    private void requireConflict(
        @NotNull DDSyncBinding binding,
        @NotNull DDConfigurationPart remote,
        @NotNull String localFingerprint
    ) throws DBException {
        DDSyncChange change = classify(binding.parts().get(remote.key()), localFingerprint, remote.version());
        if (change != DDSyncChange.CONFLICT) {
            throw new DBException("Synchronization part is no longer in conflict: " + remote.name());
        }
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

    @NotNull
    private String fingerprintAfterApply(@NotNull DDConfigurationPart remote) throws DBException {
        DDConfigurationPart applied = readCurrentPart(remote);
        return store.fingerprint(applied != null ? applied : remote);
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
                remote.key(), DDConfigurationPartKind.ACCOUNT, null, 0, unit.getName(), Map.of(unitId, resources));
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
    private DDConfigurationPart readLocalPart(@NotNull String key, @Nullable Set<String> unitIds) throws DBException {
        if (key.startsWith(KEY_ACCOUNT_PREFIX)) {
            String unitId = key.substring(KEY_ACCOUNT_PREFIX.length());
            DBPSyncUnit unit = DBPSyncRegistry.getInstance().findById(unitId);
            if (unit == null || unit.getScope() != DBPSyncScope.WORKSPACE) {
                return null;
            }
            Map<String, byte[]> resources = unit.read(new DBPSyncTarget(workspace, null));
            return new DDConfigurationPart(
                key, DDConfigurationPartKind.ACCOUNT, null, 0, unit.getName(), Map.of(unitId, resources));
        }
        String projectId = key.substring(KEY_PROJECT_PREFIX.length());
        DBPProject project = findProjectById(projectId);
        if (project == null) {
            return null;
        }
        Map<String, Map<String, byte[]>> units = new LinkedHashMap<>();
        DBPSyncTarget target = new DBPSyncTarget(workspace, project);
        if (unitIds != null) {
            for (String unitId : unitIds) {
                DBPSyncUnit unit = requireUnit(unitId, DDConfigurationPartKind.PROJECT);
                units.put(unitId, unit.read(target));
            }
        } else {
            for (DBPSyncUnit unit : enabledUnits(DBPSyncScope.PROJECT)) {
                units.put(unit.getId(), unit.read(target));
            }
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
