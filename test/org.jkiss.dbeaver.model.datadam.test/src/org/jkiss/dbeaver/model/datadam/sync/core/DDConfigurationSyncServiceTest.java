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
import com.dbeaver.datadam.share.api.model.DDCreateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.core.runtime.OperationCanceledException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncChange;
import org.jkiss.dbeaver.model.datadam.sync.core.DDConfigurationSyncService.Resolution;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sync.DBPSyncScope;
import org.jkiss.dbeaver.model.sync.DBPSyncTarget;
import org.jkiss.dbeaver.model.sync.DBPSyncUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class DDConfigurationSyncServiceTest {

    private static final UUID CONFIGURATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_CONFIGURATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 6, 12, 0);
    private static final String UNIT_ID = "test-settings";
    private static final DBRProgressMonitor MONITOR = new VoidProgressMonitor();

    @TempDir
    Path metadata;

    private final DDConfigurationClient client = Mockito.mock(DDConfigurationClient.class);
    private final MemoryUnit unit = new MemoryUnit();
    private final DDConfigurationSyncService service = service();
    private DBPSyncTarget target;
    private DDConfiguration server;

    @BeforeEach
    void setUp() throws Exception {
        target = target(metadata);
        Mockito.when(client.encryptPart(Mockito.anyString(), Mockito.any(byte[].class)))
            .thenAnswer(call -> Base64.getEncoder().encodeToString(call.getArgument(1, byte[].class)));
        Mockito.when(client.decryptPart(Mockito.any())).thenAnswer(call ->
            Base64.getDecoder().decode(call.getArgument(0, DDConfigurationPart.class).encryptedValue()));
        Mockito.when(client.listConfigurations()).thenAnswer(call -> server == null ? List.of() : List.of(
            new DDConfigurationSummary(server.configurationId(), server.name(), TIME, TIME)));
        Mockito.when(client.getConfiguration(Mockito.any())).thenAnswer(call ->
            server != null && server.configurationId().equals(call.getArgument(0)) ? server : null);
        Mockito.when(client.getDefaultConfiguration()).thenAnswer(call ->
            server != null && DEFAULT_CONFIGURATION_ID.equals(server.configurationId()) ? server : null);
        Mockito.when(client.createDefaultConfiguration(Mockito.any())).thenAnswer(call -> {
            DDCreateConfigurationRequest input = call.getArgument(0);
            var part = input.parts().getFirst();
            server = configuration(DEFAULT_CONFIGURATION_ID, new DDConfigurationPart(
                part.key(), part.kind(), part.projectId(), part.fingerprint(), part.encryptedValue()));
            return server;
        });
        Mockito.when(client.updateConfiguration(Mockito.any(), Mockito.any())).thenAnswer(call -> applyUpdate(call.getArgument(1)));
    }

    @Test
    void firstSyncPublishesLocalSettingsAndPersistsTheBaseline() throws Exception {
        Assertions.assertEquals(DDSyncChange.LOCAL, service.synchronize(MONITOR, unit, target));
        Assertions.assertEquals("original", serverValue());
        Assertions.assertEquals(0, unit.writes);

        changeServer("original");
        Assertions.assertEquals(DDSyncChange.UNCHANGED, service().synchronize(MONITOR, unit, target));
        String baseline = server.parts().getFirst().fingerprint();
        unit.value = "local change";

        Assertions.assertEquals(DDSyncChange.LOCAL, service().synchronize(MONITOR, unit, target));
        Assertions.assertEquals("local change", serverValue());
        Mockito.verify(client).createDefaultConfiguration(Mockito.any());
        Mockito.verify(client).updateConfiguration(Mockito.eq(server.configurationId()), Mockito.argThat(input ->
            baseline.equals(input.parts().getFirst().lastKnownFingerprint())
                && server.parts().getFirst().fingerprint().equals(input.parts().getFirst().fingerprint())));
    }

    @Test
    void firstSyncDownloadsExistingServerSettings() throws Exception {
        changeServer("server settings");

        Assertions.assertEquals(DDSyncChange.SERVER, service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals("server settings", unit.value);
        Assertions.assertEquals(1, unit.writes);
        Mockito.verify(client, Mockito.never()).createDefaultConfiguration(Mockito.any());
        Mockito.verify(client, Mockito.never()).updateConfiguration(Mockito.any(), Mockito.any());
    }

    @Test
    void remoteChangesAreDownloadedButIdenticalContentIsNotReapplied() throws Exception {
        service.synchronize(MONITOR, unit, target);
        changeServer("remote change");

        Assertions.assertEquals(DDSyncChange.SERVER, service().synchronize(MONITOR, unit, target));
        Assertions.assertEquals("remote change", unit.value);
        Assertions.assertEquals(DDSyncChange.UNCHANGED, service().synchronize(MONITOR, unit, target));

        Assertions.assertEquals(1, unit.writes);
        Mockito.verify(client, Mockito.never()).updateConfiguration(Mockito.any(), Mockito.any());
    }

    @Test
    void divergedChangesWaitForAnExplicitConflictResolution() throws Exception {
        service.synchronize(MONITOR, unit, target);
        unit.value = "local change";
        changeServer("remote change");
        String previous = state();

        Assertions.assertEquals(DDSyncChange.CONFLICT, service.synchronize(MONITOR, unit, target));
        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("local change", unit.value);
        Assertions.assertEquals("remote change", serverValue());
        Mockito.verify(client, Mockito.never()).updateConfiguration(Mockito.any(), Mockito.any());

        Assertions.assertEquals(DDSyncChange.LOCAL, service.resolveConflict(MONITOR, unit, target, Resolution.USE_LOCAL_VERSION));
        Assertions.assertEquals("local change", serverValue());

        unit.value = "another local change";
        changeServer("another remote change");
        Assertions.assertEquals(DDSyncChange.CONFLICT, service.synchronize(MONITOR, unit, target));
        Assertions.assertEquals(DDSyncChange.SERVER, service.resolveConflict(MONITOR, unit, target, Resolution.USE_SERVER_VERSION));
        Assertions.assertEquals("another remote change", unit.value);
    }

    @Test
    void failedUploadDoesNotAdvanceTheBaseline() throws Exception {
        service.synchronize(MONITOR, unit, target);
        unit.value = "local change";
        String previous = state();
        Mockito.doThrow(new DBException("Upload failed")).when(client).updateConfiguration(Mockito.any(), Mockito.any());

        Assertions.assertThrows(DBException.class, () -> service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("original", serverValue());
        Assertions.assertEquals("local change", unit.value);
    }

    @Test
    void failedLocalApplicationDoesNotAdvanceTheBaseline() throws Exception {
        service.synchronize(MONITOR, unit, target);
        changeServer("remote change");
        String previous = state();
        unit.failWrite = true;

        Assertions.assertThrows(DBException.class, () -> service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("original", unit.value);
        unit.failWrite = false;
        Assertions.assertEquals(DDSyncChange.SERVER, service.synchronize(MONITOR, unit, target));
        Assertions.assertEquals("remote change", unit.value);
    }

    @Test
    void concurrentServerChangesRequireAFreshConflictResolution() throws Exception {
        service.synchronize(MONITOR, unit, target);
        unit.value = "local change";
        String previous = state();
        Mockito.doAnswer(call -> conflictWithServer("concurrent remote change 1"))
            .doAnswer(call -> conflictWithServer("concurrent remote change 2"))
            .when(client).updateConfiguration(Mockito.any(), Mockito.any());

        Assertions.assertEquals(DDSyncChange.CONFLICT, service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("local change", unit.value);
        Assertions.assertEquals("concurrent remote change 1", serverValue());
        Mockito.verify(client).updateConfiguration(Mockito.any(), Mockito.any());

        Assertions.assertEquals(DDSyncChange.CONFLICT, service.resolveConflict(MONITOR, unit, target, Resolution.USE_LOCAL_VERSION));

        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("local change", unit.value);
        Assertions.assertEquals("concurrent remote change 2", serverValue());
        Mockito.verify(client, Mockito.times(2)).updateConfiguration(Mockito.any(), Mockito.any());
    }

    @Test
    void legacyFingerprintAdoptionRetriesTheUploadWithTheCurrentFingerprint() throws Exception {
        service.synchronize(MONITOR, unit, target);
        String baseline = server.parts().getFirst().fingerprint();
        markServerFingerprintAsLegacy();
        unit.value = "local change";
        Mockito.doAnswer(call -> conflictWithServer("original"))
            .doAnswer(call -> applyUpdate(call.getArgument(1)))
            .when(client).updateConfiguration(Mockito.any(), Mockito.any());

        Assertions.assertEquals(DDSyncChange.LOCAL, service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals("local change", serverValue());
        Assertions.assertEquals(0, unit.writes);
        Assertions.assertEquals(DDSyncChange.UNCHANGED, service().synchronize(MONITOR, unit, target));
        Mockito.verify(client, Mockito.times(2)).updateConfiguration(Mockito.any(), Mockito.any());
        ArgumentCaptor<DDUpdateConfigurationRequest> requests = ArgumentCaptor.forClass(DDUpdateConfigurationRequest.class);
        Mockito.verify(client, Mockito.times(2)).updateConfiguration(Mockito.eq(server.configurationId()), requests.capture());
        Assertions.assertEquals("legacy-version:7", requests.getAllValues().getFirst().parts().getFirst().lastKnownFingerprint());
        Assertions.assertEquals(baseline, requests.getAllValues().getLast().parts().getFirst().lastKnownFingerprint());
    }

    @Test
    void concurrentIdenticalContentIsAcknowledgedWithoutAnotherUpload() throws Exception {
        service.synchronize(MONITOR, unit, target);
        unit.value = "local change";
        String previous = state();
        Mockito.doAnswer(call -> conflictWithServer(unit.value)).when(client).updateConfiguration(Mockito.any(), Mockito.any());

        Assertions.assertEquals(DDSyncChange.UNCHANGED, service.synchronize(MONITOR, unit, target));

        Assertions.assertNotEquals(previous, state());
        Assertions.assertEquals(0, unit.writes);
        Assertions.assertEquals(DDSyncChange.UNCHANGED, service().synchronize(MONITOR, unit, target));
        Mockito.verify(client).updateConfiguration(Mockito.any(), Mockito.any());
    }

    @Test
    void legacyFingerprintAdoptionPreservesAnExplicitResolution() throws Exception {
        service.synchronize(MONITOR, unit, target);
        unit.value = "local change";
        changeServer("remote change");
        String baseline = server.parts().getFirst().fingerprint();
        markServerFingerprintAsLegacy();
        Mockito.doAnswer(call -> conflictWithServer("remote change"))
            .doAnswer(call -> applyUpdate(call.getArgument(1)))
            .when(client).updateConfiguration(Mockito.any(), Mockito.any());

        Assertions.assertEquals(DDSyncChange.LOCAL, service.resolveConflict(MONITOR, unit, target, Resolution.USE_LOCAL_VERSION));

        Assertions.assertEquals("local change", serverValue());
        Assertions.assertEquals(DDSyncChange.UNCHANGED, service().synchronize(MONITOR, unit, target));
        Mockito.verify(client, Mockito.times(2)).updateConfiguration(Mockito.any(), Mockito.any());
        ArgumentCaptor<DDUpdateConfigurationRequest> requests = ArgumentCaptor.forClass(DDUpdateConfigurationRequest.class);
        Mockito.verify(client, Mockito.times(2)).updateConfiguration(Mockito.eq(server.configurationId()), requests.capture());
        Assertions.assertEquals("legacy-version:7", requests.getAllValues().getFirst().parts().getFirst().lastKnownFingerprint());
        Assertions.assertEquals(baseline, requests.getAllValues().getLast().parts().getFirst().lastKnownFingerprint());
    }

    @Test
    void repeatedWriteConflictsFailWithoutReportingAContentConflictOrAdvancingTheBaseline() throws Exception {
        service.synchronize(MONITOR, unit, target);
        unit.value = "local change";
        String previous = state();
        Mockito.doAnswer(call -> conflictWithServer("original"))
            .doAnswer(call -> conflictWithServer("original"))
            .doThrow(new AssertionError("Synchronization retried more than once"))
            .when(client).updateConfiguration(Mockito.any(), Mockito.any());

        Assertions.assertThrows(DBException.class, () -> service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("local change", unit.value);
        Assertions.assertEquals("original", serverValue());
        Mockito.verify(client, Mockito.times(2)).updateConfiguration(Mockito.any(), Mockito.any());
    }

    @Test
    void localEditsDuringTheServerReadAreNotOverwritten() throws Exception {
        service.synchronize(MONITOR, unit, target);
        changeServer("remote change");
        String previous = state();
        Mockito.when(client.getConfiguration(Mockito.any())).thenAnswer(call -> {
            unit.value = "concurrent local change";
            return server;
        });

        Assertions.assertThrows(DBException.class, () -> service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("concurrent local change", unit.value);
        Assertions.assertEquals(0, unit.writes);
    }

    @Test
    void localEditsBeforeConditionalApplicationAreNotOverwritten() throws Exception {
        service.synchronize(MONITOR, unit, target);
        changeServer("remote change");
        String previous = state();
        MemoryUnit guarded = Mockito.spy(unit);
        Mockito.doAnswer(call -> {
            guarded.value = "new local settings";
            return call.callRealMethod();
        }).when(guarded).writeIfUnchanged(Mockito.same(MONITOR), Mockito.same(target), Mockito.anyMap(), Mockito.anyMap());

        Assertions.assertThrows(DBException.class, () -> service.synchronize(MONITOR, guarded, target));

        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("new local settings", guarded.value);
        Assertions.assertEquals(0, guarded.writes);
    }

    @Test
    void cancellationBeforeConditionalApplicationDoesNotAdvanceTheBaseline() throws Exception {
        service.synchronize(MONITOR, unit, target);
        changeServer("remote change");
        String previous = state();
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        MemoryUnit guarded = Mockito.spy(unit);
        Mockito.doAnswer(call -> {
            Mockito.when(monitor.isCanceled()).thenReturn(true);
            return call.callRealMethod();
        }).when(guarded).writeIfUnchanged(Mockito.same(monitor), Mockito.same(target), Mockito.anyMap(), Mockito.anyMap());

        Assertions.assertThrows(OperationCanceledException.class, () -> service.synchronize(monitor, guarded, target));

        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("original", guarded.value);
        Assertions.assertEquals(0, guarded.writes);
    }

    @Test
    void cancellationAfterApplicationPreservesTheSuccessfulResultAndBaseline() throws Exception {
        service.synchronize(MONITOR, unit, target);
        changeServer("remote change");
        String previous = state();
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        MemoryUnit guarded = Mockito.spy(unit);
        Mockito.doAnswer(call -> {
            Object result = call.callRealMethod();
            Mockito.when(monitor.isCanceled()).thenReturn(true);
            return result;
        }).when(guarded).writeIfUnchanged(Mockito.same(monitor), Mockito.same(target), Mockito.anyMap(), Mockito.anyMap());

        Assertions.assertEquals(DDSyncChange.SERVER, service.synchronize(monitor, guarded, target));

        Assertions.assertEquals("remote change", guarded.value);
        Assertions.assertEquals(1, guarded.writes);
        Assertions.assertNotEquals(previous, state());
        Assertions.assertEquals(DDSyncChange.UNCHANGED, service().synchronize(MONITOR, guarded, target));
    }

    @Test
    void serverAssignedDefaultConfigurationIsSharedAcrossWorkspaces() throws Exception {
        service.synchronize(MONITOR, unit, target);
        MemoryUnit secondUnit = new MemoryUnit();
        secondUnit.value = "other local settings";

        Assertions.assertEquals(DDSyncChange.SERVER,
            service().synchronize(MONITOR, secondUnit, target(metadata.resolve("second"))));

        Assertions.assertEquals(DEFAULT_CONFIGURATION_ID, server.configurationId());
        Assertions.assertEquals("original", secondUnit.value);
        Mockito.verify(client).createDefaultConfiguration(Mockito.any());
    }

    @Test
    void rejectedCreationDownloadsTheConfigurationCreatedByAnotherClient() throws Exception {
        Mockito.doAnswer(call -> {
            changeServer("winning settings");
            server = configuration(DEFAULT_CONFIGURATION_ID, server.parts().getFirst());
            return null;
        }).when(client).createDefaultConfiguration(Mockito.any());

        Assertions.assertEquals(DDSyncChange.SERVER, service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals("winning settings", unit.value);
        Assertions.assertEquals(server.configurationId().toString(),
            JsonParser.parseString(state()).getAsJsonObject().get("configurationId").getAsString());
        Mockito.verify(client).createDefaultConfiguration(Mockito.any());
        Mockito.verify(client, Mockito.never()).updateConfiguration(Mockito.any(), Mockito.any());
    }

    @Test
    void repeatedCreationRejectionsDoNotSaveASuccessfulBaseline() throws Exception {
        Mockito.doReturn(null).when(client).createDefaultConfiguration(Mockito.any());

        Assertions.assertThrows(DBException.class, () -> service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals("original", unit.value);
        Assertions.assertEquals(0, unit.writes);
        Assertions.assertTrue(Files.notExists(metadata.resolve("datadam-sync")));
        Mockito.verify(client, Mockito.times(2)).createDefaultConfiguration(Mockito.any());
    }

    @Test
    void existingDefaultConfigurationReceivesOnlyTheMissingPart() throws Exception {
        service.synchronize(MONITOR, unit, target);
        DDConfigurationPart original = server.parts().getFirst();
        DDConfigurationPart other = new DDConfigurationPart(
            "other-settings", original.kind(), original.projectId(), original.fingerprint(), original.encryptedValue());
        server = configuration(server.configurationId(), other);
        MemoryUnit secondUnit = new MemoryUnit();
        secondUnit.value = "new local settings";

        Assertions.assertEquals(DDSyncChange.LOCAL,
            service().synchronize(MONITOR, secondUnit, target(metadata.resolve("second"))));

        Assertions.assertTrue(server.parts().contains(other));
        Assertions.assertEquals("new local settings", serverValue());
        Mockito.verify(client).createDefaultConfiguration(Mockito.any());
        Mockito.verify(client).updateConfiguration(Mockito.eq(server.configurationId()), Mockito.argThat(input ->
            input.parts().getFirst().lastKnownFingerprint() == null));
    }

    @Test
    void oldStateAndLegacyFingerprintKeepTheirExistingConfigurationBinding() throws Exception {
        changeServer("original");
        markServerFingerprintAsLegacy();
        service.synchronize(MONITOR, unit, target);
        JsonObject oldState = JsonParser.parseString(state()).getAsJsonObject();
        oldState.addProperty("configurationVersion", 8);
        oldState.addProperty("partVersion", 7);
        Files.writeString(stateFile(), oldState.toString());
        unit.value = "local change";

        Assertions.assertEquals(DDSyncChange.LOCAL, service().synchronize(MONITOR, unit, target));

        Assertions.assertEquals(CONFIGURATION_ID, server.configurationId());
        Assertions.assertEquals("local change", serverValue());
        Mockito.verify(client, Mockito.never()).createDefaultConfiguration(Mockito.any());
        Mockito.verify(client).updateConfiguration(Mockito.eq(CONFIGURATION_ID), Mockito.argThat(input ->
            "legacy-version:7".equals(input.parts().getFirst().lastKnownFingerprint())));
        JsonObject saved = JsonParser.parseString(state()).getAsJsonObject();
        Assertions.assertFalse(saved.has("configurationVersion"));
        Assertions.assertFalse(saved.has("partVersion"));
    }

    @Test
    void mismatchedServerFingerprintDoesNotApplyOrSaveTheContent() throws Exception {
        changeServer("remote settings");
        DDConfigurationPart original = server.parts().getFirst();
        server = configuration(server.configurationId(), new DDConfigurationPart(
            original.key(), original.kind(), original.projectId(), "sha256-v1:" + "0".repeat(64), original.encryptedValue()));

        Assertions.assertThrows(DBException.class, () -> service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals("original", unit.value);
        Assertions.assertEquals(0, unit.writes);
        Assertions.assertTrue(Files.notExists(metadata.resolve("datadam-sync")));
    }

    @NotNull
    private DDConfigurationSyncService service() {
        return new DDConfigurationSyncService(client, "https://storage.example.test", ACCOUNT_ID);
    }

    @NotNull
    private DDUpdateConfigurationResult applyUpdate(@NotNull DDUpdateConfigurationRequest input) {
        var update = input.parts().getFirst();
        DDConfigurationPart current = server.parts().stream().filter(part -> part.key().equals(update.key())).findFirst().orElse(null);
        if (current == null ? update.lastKnownFingerprint() != null : !current.fingerprint().equals(update.lastKnownFingerprint())) {
            return new DDUpdateConfigurationResult(server, List.of(update.key()));
        }
        List<DDConfigurationPart> parts = new ArrayList<>(server.parts());
        parts.remove(current);
        parts.add(new DDConfigurationPart(update.key(), update.kind(), update.projectId(), update.fingerprint(), update.encryptedValue()));
        server = new DDConfiguration(server.configurationId(), server.name(), TIME, TIME, List.copyOf(parts));
        return new DDUpdateConfigurationResult(server, List.of());
    }

    @NotNull
    private DDUpdateConfigurationResult conflictWithServer(@NotNull String value) throws Exception {
        changeServer(value);
        return new DDUpdateConfigurationResult(server, List.of(UNIT_ID));
    }

    private void changeServer(@NotNull String value) throws Exception {
        String resource = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        String payload = JSONUtils.GSON.toJson(JsonParser.parseString(
            "{\"version\":1,\"resources\":{\"settings.json\":\"" + resource + "\"}}"));
        String fingerprint = "sha256-v1:" + HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        server = configuration(server == null ? CONFIGURATION_ID : server.configurationId(),
            new DDConfigurationPart(UNIT_ID, DDConfigurationPartKind.ACCOUNT, null, fingerprint,
                Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8))));
    }

    private void markServerFingerprintAsLegacy() {
        DDConfigurationPart part = server.parts().getFirst();
        server = configuration(server.configurationId(), new DDConfigurationPart(
            part.key(), part.kind(), part.projectId(), "legacy-version:7", part.encryptedValue()));
    }

    @NotNull
    private static DDConfiguration configuration(@NotNull UUID configurationId, @NotNull DDConfigurationPart part) {
        return new DDConfiguration(configurationId, "Test settings", TIME, TIME, List.of(part));
    }

    @NotNull
    private String serverValue() {
        DDConfigurationPart part = server.parts().stream().filter(value -> value.key().equals(UNIT_ID)).findFirst().orElseThrow();
        String payload = new String(Base64.getDecoder().decode(part.encryptedValue()), StandardCharsets.UTF_8);
        String resource = JsonParser.parseString(payload).getAsJsonObject().getAsJsonObject("resources")
            .get("settings.json").getAsString();
        return new String(Base64.getDecoder().decode(resource), StandardCharsets.UTF_8);
    }

    @NotNull
    private String state() throws Exception {
        return Files.readString(stateFile());
    }

    @NotNull
    private Path stateFile() throws Exception {
        try (var files = Files.list(metadata.resolve("datadam-sync"))) {
            return files.findFirst().orElseThrow();
        }
    }

    @NotNull
    private static DBPSyncTarget target(@NotNull Path metadata) {
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        Mockito.when(workspace.getMetadataFolder()).thenReturn(metadata);
        return new DBPSyncTarget(workspace, null);
    }

    private static final class MemoryUnit implements DBPSyncUnit {
        private String value = "original";
        private boolean failWrite;
        private int writes;

        @NotNull
        @Override
        public String getId() {
            return UNIT_ID;
        }

        @NotNull
        @Override
        public DBPSyncScope getScope() {
            return DBPSyncScope.WORKSPACE;
        }

        @NotNull
        @Override
        public Map<String, byte[]> read(@NotNull DBPSyncTarget target) {
            return Map.of("settings.json", value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void write(@NotNull DBPSyncTarget target, @NotNull Map<String, byte[]> resources) throws DBException {
            if (failWrite) {
                throw new DBException("Local configuration application failed");
            }
            value = new String(resources.get("settings.json"), StandardCharsets.UTF_8);
            writes++;
        }

        @Override
        public void writeIfUnchanged(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPSyncTarget target,
            @NotNull Map<String, byte[]> expected,
            @NotNull Map<String, byte[]> resources
        ) throws DBException {
            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            if (!Arrays.equals(expected.get("settings.json"), read(target).get("settings.json"))) {
                throw new DBException("Local configuration changed during synchronization");
            }
            write(target, resources);
        }
    }
}
