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
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
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
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class DDConfigurationSyncServiceTest {

    private static final UUID CONFIGURATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
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
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        Mockito.when(workspace.getMetadataFolder()).thenReturn(metadata);
        target = new DBPSyncTarget(workspace, null);
        Mockito.when(client.encryptPart(Mockito.anyString(), Mockito.any(byte[].class)))
            .thenAnswer(call -> Base64.getEncoder().encodeToString(call.getArgument(1, byte[].class)));
        Mockito.when(client.decryptPart(Mockito.any())).thenAnswer(call ->
            Base64.getDecoder().decode(call.getArgument(0, DDConfigurationPart.class).encryptedValue()));
        Mockito.when(client.listConfigurations()).thenAnswer(call -> server == null ? List.of() : List.of(
            new DDConfigurationSummary(server.configurationId(), server.name(), server.version(), TIME, TIME)));
        Mockito.when(client.getConfiguration(Mockito.any())).thenAnswer(call -> server);
        Mockito.when(client.createConfiguration(Mockito.any())).thenAnswer(call -> {
            DDCreateConfigurationRequest input = call.getArgument(0);
            var part = input.parts().getFirst();
            server = configuration(0, new DDConfigurationPart(part.key(), part.kind(), part.projectId(), 1, part.encryptedValue()));
            return server;
        });
        Mockito.when(client.updateConfiguration(Mockito.any(), Mockito.any())).thenAnswer(call -> {
            DDUpdateConfigurationRequest input = call.getArgument(1);
            var update = input.parts().getFirst();
            DDConfigurationPart current = server.parts().getFirst();
            server = configuration(server.version() + 1,
                new DDConfigurationPart(UNIT_ID, DDConfigurationPartKind.ACCOUNT, null,
                    current.version() + 1, update.encryptedValue()));
            return new DDUpdateConfigurationResult(server, List.of());
        });
    }

    @Test
    void firstSyncPublishesLocalSettingsAndPersistsTheBaseline() throws Exception {
        Assertions.assertEquals(DDSyncChange.LOCAL, service.synchronize(MONITOR, unit, target));
        Assertions.assertEquals("original", serverValue());
        Assertions.assertEquals(0, unit.writes);

        changeServer("original");
        Assertions.assertEquals(DDSyncChange.UNCHANGED, service().synchronize(MONITOR, unit, target));
        unit.value = "local change";

        Assertions.assertEquals(DDSyncChange.LOCAL, service().synchronize(MONITOR, unit, target));
        Assertions.assertEquals("local change", serverValue());
        Mockito.verify(client).createConfiguration(Mockito.any());
        Mockito.verify(client).updateConfiguration(Mockito.eq(CONFIGURATION_ID), Mockito.argThat(input ->
            input.expectedConfigurationVersion() == 1 && input.parts().getFirst().expectedVersion() == 2));
    }

    @Test
    void firstSyncDownloadsExistingServerSettings() throws Exception {
        changeServer("server settings");

        Assertions.assertEquals(DDSyncChange.SERVER, service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals("server settings", unit.value);
        Assertions.assertEquals(1, unit.writes);
        Mockito.verify(client, Mockito.never()).createConfiguration(Mockito.any());
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
    void aStaleServerVersionReturnsConflictWithoutRetryingOrAdvancingTheBaseline() throws Exception {
        service.synchronize(MONITOR, unit, target);
        unit.value = "local change";
        String previous = state();
        Mockito.doAnswer(call -> {
            changeServer("concurrent remote change");
            return new DDUpdateConfigurationResult(server, List.of(UNIT_ID));
        }).when(client).updateConfiguration(Mockito.any(), Mockito.any());

        Assertions.assertEquals(DDSyncChange.CONFLICT, service.synchronize(MONITOR, unit, target));

        Assertions.assertEquals(previous, state());
        Assertions.assertEquals("local change", unit.value);
        Assertions.assertEquals("concurrent remote change", serverValue());
        Mockito.verify(client).updateConfiguration(Mockito.any(), Mockito.any());
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

    @NotNull
    private DDConfigurationSyncService service() {
        return new DDConfigurationSyncService(client, "https://storage.example.test", ACCOUNT_ID);
    }

    private void changeServer(@NotNull String value) {
        String resource = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        String payload = "{\"version\":1,\"resources\":{\"settings.json\":\"" + resource + "\"}}";
        long version = server == null ? 1 : server.parts().getFirst().version() + 1;
        server = configuration(server == null ? 0 : server.version() + 1,
            new DDConfigurationPart(UNIT_ID, DDConfigurationPartKind.ACCOUNT, null, version,
                Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8))));
    }

    @NotNull
    private static DDConfiguration configuration(long version, @NotNull DDConfigurationPart part) {
        return new DDConfiguration(CONFIGURATION_ID, "Test settings", version, TIME, TIME, List.of(part));
    }

    @NotNull
    private String serverValue() {
        String payload = new String(Base64.getDecoder().decode(server.parts().getFirst().encryptedValue()), StandardCharsets.UTF_8);
        String resource = JsonParser.parseString(payload).getAsJsonObject().getAsJsonObject("resources")
            .get("settings.json").getAsString();
        return new String(Base64.getDecoder().decode(resource), StandardCharsets.UTF_8);
    }

    @NotNull
    private String state() throws Exception {
        try (var files = Files.list(metadata.resolve("datadam-sync"))) {
            return Files.readString(files.findFirst().orElseThrow());
        }
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
    }
}
