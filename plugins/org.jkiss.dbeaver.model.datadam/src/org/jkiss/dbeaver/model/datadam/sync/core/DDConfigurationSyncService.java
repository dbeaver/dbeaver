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
import com.dbeaver.datadam.share.api.model.DDConfigurationPartKind;
import com.dbeaver.datadam.share.api.model.DDConfigurationSummary;
import com.dbeaver.datadam.share.api.model.DDCreateConfigurationPartRequest;
import com.dbeaver.datadam.share.api.model.DDCreateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationPartRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationResult;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncChange;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncUtils;
import org.jkiss.dbeaver.model.datadam.sync.core.DDConfigurationSyncStateStore.State;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sync.DBPSyncScope;
import org.jkiss.dbeaver.model.sync.DBPSyncTarget;
import org.jkiss.dbeaver.model.sync.DBPSyncUnit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class DDConfigurationSyncService {

    public enum Resolution {
        USE_LOCAL_VERSION,
        USE_SERVER_VERSION
    }

    private static final int MAX_SYNC_ATTEMPTS = 2;

    private final DDConfigurationClient client;
    private final String storageUrl;
    private final UUID accountId;

    public DDConfigurationSyncService(
        @NotNull DDConfigurationClient client,
        @NotNull String storageUrl,
        @NotNull UUID accountId
    ) {
        this.client = client;
        this.storageUrl = storageUrl;
        this.accountId = accountId;
    }

    @NotNull
    public synchronized DDSyncChange synchronize(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPSyncUnit unit,
        @NotNull DBPSyncTarget target
    ) throws DBException {
        return synchronize(monitor, unit, target, null);
    }

    @NotNull
    public synchronized DDSyncChange resolveConflict(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPSyncUnit unit,
        @NotNull DBPSyncTarget target,
        @NotNull Resolution resolution
    ) throws DBException {
        return synchronize(monitor, unit, target, resolution);
    }

    @NotNull
    private DDSyncChange synchronize(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPSyncUnit unit,
        @NotNull DBPSyncTarget target,
        @Nullable Resolution resolution
    ) throws DBException {
        checkCanceled(monitor);
        if (unit.getScope() != DBPSyncScope.WORKSPACE || target.project() != null || unit.getId().isEmpty()) {
            throw new DBException("A workspace synchronization unit is required");
        }
        DDConfigurationSyncStateStore store = stateStore(unit, target);
        State previous = store.load();
        Map<String, byte[]> local = copyResources(unit.read(target));
        String localFingerprint = fingerprint(local);
        String confirmedServerFingerprint = null;
        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            checkCanceled(monitor);
            DDConfiguration configuration = findConfiguration(monitor, unit.getId(), previous);
            checkCanceled(monitor);
            if (configuration == null) {
                ensureLocalUnchanged(unit, target, localFingerprint);
                configuration = client.createConfiguration(new DDCreateConfigurationRequest(unit.getName(), List.of(
                    new DDCreateConfigurationPartRequest(
                        unit.getId(), DDConfigurationPartKind.ACCOUNT, null, client.encryptPart(unit.getId(), encode(local))))));
                saveState(store, previous, configuration, unit.getId(), localFingerprint);
                return DDSyncChange.LOCAL;
            }

            DDConfigurationPart part = requirePart(configuration, unit.getId());
            Map<String, byte[]> remote = decode(client.decryptPart(part));
            String serverFingerprint = fingerprint(remote);
            if (localFingerprint.equals(serverFingerprint)) {
                saveState(store, previous, configuration, unit.getId(), serverFingerprint);
                return DDSyncChange.UNCHANGED;
            }
            DDSyncChange change = previous == null ? DDSyncChange.SERVER : DDSyncUtils.classify(
                localFingerprint, previous.fingerprint(), serverFingerprint);
            if (change == DDSyncChange.CONFLICT) {
                if (resolution == null || (attempt > 0 && !serverFingerprint.equals(confirmedServerFingerprint))) {
                    return DDSyncChange.CONFLICT;
                }
                confirmedServerFingerprint = serverFingerprint;
                change = resolution == Resolution.USE_LOCAL_VERSION ? DDSyncChange.LOCAL : DDSyncChange.SERVER;
            }

            checkCanceled(monitor);
            ensureLocalUnchanged(unit, target, localFingerprint);
            if (change == DDSyncChange.LOCAL) {
                DDUpdateConfigurationResult result = client.updateConfiguration(configuration.configurationId(),
                    new DDUpdateConfigurationRequest(configuration.version(), List.of(
                        new DDUpdateConfigurationPartRequest(part.key(), part.version(), client.encryptPart(part.key(), encode(local))))));
                if (result == null) {
                    throw new DBException("DataDam configuration no longer exists: " + configuration.configurationId());
                }
                if (!result.conflictingKeys().isEmpty()) {
                    continue;
                }
                if (!configuration.configurationId().equals(result.configuration().configurationId())) {
                    throw new DBException("Unexpected DataDam configuration update response");
                }
                saveState(store, previous, result.configuration(), unit.getId(), localFingerprint);
            } else {
                unit.write(target, remote);
                saveState(store, previous, configuration, unit.getId(), serverFingerprint);
            }
            return change;
        }
        throw new DBException("DataDam configuration changed repeatedly during synchronization; retry synchronization");
    }

    @Nullable
    private DDConfiguration findConfiguration(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String unitId,
        @Nullable State previous
    ) throws DBException {
        if (previous != null) {
            DDConfiguration configuration = client.getConfiguration(previous.configurationId());
            if (configuration == null || !previous.configurationId().equals(configuration.configurationId())) {
                throw new DBException("DataDam configuration no longer exists: " + previous.configurationId());
            }
            return configuration;
        }
        DDConfiguration found = null;
        for (DDConfigurationSummary summary : client.listConfigurations()) {
            checkCanceled(monitor);
            DDConfiguration configuration = client.getConfiguration(summary.configurationId());
            if (configuration == null) {
                continue;
            }
            validateConfiguration(configuration);
            if (!summary.configurationId().equals(configuration.configurationId())) {
                throw new DBException("Unexpected DataDam configuration response");
            }
            if (configuration.parts().stream().noneMatch(part ->
                part.key().equals(unitId) && part.kind() == DDConfigurationPartKind.ACCOUNT && part.projectId() == null)) {
                continue;
            }
            if (found != null) {
                throw new DBException("Multiple DataDam configurations contain synchronization unit: " + unitId);
            }
            found = configuration;
        }
        return found;
    }

    @NotNull
    private DDConfigurationSyncStateStore stateStore(@NotNull DBPSyncUnit unit, @NotNull DBPSyncTarget target) throws DBException {
        String identity = storageUrl + '\0' + accountId + '\0' + unit.getId();
        String fileName = digest(identity.getBytes(StandardCharsets.UTF_8)) + ".json";
        return new DDConfigurationSyncStateStore(target.workspace().getMetadataFolder().resolve("datadam-sync").resolve(fileName));
    }

    private static void saveState(
        @NotNull DDConfigurationSyncStateStore store,
        @Nullable State previous,
        @NotNull DDConfiguration configuration,
        @NotNull String unitId,
        @NotNull String fingerprint
    ) throws DBException {
        DDConfigurationPart part = requirePart(configuration, unitId);
        State next = new State(configuration.configurationId(), configuration.version(), part.version(), fingerprint);
        if (!next.equals(previous)) {
            store.save(next);
        }
    }

    @NotNull
    private static DDConfigurationPart requirePart(@NotNull DDConfiguration configuration, @NotNull String unitId) throws DBException {
        validateConfiguration(configuration);
        for (DDConfigurationPart part : configuration.parts()) {
            if (part.key().equals(unitId)) {
                if (part.kind() != DDConfigurationPartKind.ACCOUNT || part.projectId() != null || part.version() < 1) {
                    throw new DBException("Invalid DataDam workspace configuration part: " + unitId);
                }
                return part;
            }
        }
        throw new DBException("DataDam configuration does not contain synchronization unit: " + unitId);
    }

    private static void validateConfiguration(@NotNull DDConfiguration configuration) throws DBException {
        if (configuration.configurationId() == null || configuration.version() < 0 || configuration.parts() == null ||
            configuration.parts().stream().anyMatch(part -> part == null || part.key() == null || part.kind() == null)) {
            throw new DBException("Invalid DataDam configuration response");
        }
    }

    private static void checkCanceled(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (monitor.isCanceled()) {
            throw new DBException("DataDam configuration synchronization was canceled");
        }
    }

    private static void ensureLocalUnchanged(
        @NotNull DBPSyncUnit unit,
        @NotNull DBPSyncTarget target,
        @NotNull String expectedFingerprint
    ) throws DBException {
        if (!expectedFingerprint.equals(fingerprint(unit.read(target)))) {
            throw new DBException("Local configuration changed during synchronization");
        }
    }

    @NotNull
    private static Map<String, byte[]> copyResources(@NotNull Map<String, byte[]> resources) {
        Map<String, byte[]> copy = new TreeMap<>();
        for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    @NotNull
    private static byte[] encode(@NotNull Map<String, byte[]> resources) {
        Map<String, String> encoded = new TreeMap<>();
        for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
            encoded.put(entry.getKey(), Base64.getEncoder().encodeToString(entry.getValue()));
        }
        return JSONUtils.GSON.toJson(new Payload(1, encoded)).getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    private static Map<String, byte[]> decode(@NotNull byte[] content) throws DBException {
        try {
            Payload payload = JSONUtils.GSON.fromJson(new String(content, StandardCharsets.UTF_8), Payload.class);
            if (payload == null || payload.version() != 1 || payload.resources() == null) {
                throw new DBException("Unsupported DataDam configuration content");
            }
            Map<String, byte[]> resources = new TreeMap<>();
            for (Map.Entry<String, String> entry : payload.resources().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new DBException("Invalid DataDam configuration resource");
                }
                resources.put(entry.getKey(), Base64.getDecoder().decode(entry.getValue()));
            }
            return resources;
        } catch (RuntimeException e) {
            throw new DBException("Invalid DataDam configuration content", e);
        }
    }

    @NotNull
    private static String fingerprint(@NotNull Map<String, byte[]> resources) throws DBException {
        return "sha256-v1:" + digest(encode(resources));
    }

    @NotNull
    private static String digest(@NotNull byte[] content) throws DBException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new DBException("SHA-256 is unavailable", e);
        }
    }

    private record Payload(int version, @NotNull Map<String, String> resources) {
    }
}
