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

import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSharedProjectPullResult;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.impl.app.DefaultValueEncryptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.KeyGenerator;

class DDProjectShareServiceTest {
    @TempDir
    Path folder;
    private DDShareClient client;
    private DDProjectShareService service;
    private DBPProject project;
    private DBRProgressMonitor monitor;
    private final UUID userId = UUID.randomUUID();
    private final AtomicReference<Object> binding = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        client = Mockito.mock(DDShareClient.class);
        project = Mockito.mock(DBPProject.class);
        monitor = Mockito.mock(DBRProgressMonitor.class);
        service = new DDProjectShareService(client, "account");
        Mockito.when(client.getCurrentShareUserId()).thenReturn(userId);
        Mockito.when(project.getName()).thenReturn("Local");
        Mockito.when(project.getMetadataFolder(Mockito.anyBoolean())).thenReturn(folder);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(Mockito.mock(DBPDataSourceRegistry.class));
        KeyGenerator keys = KeyGenerator.getInstance("AES");
        keys.init(256);
        Mockito.when(project.getValueEncryptor()).thenReturn(new DefaultValueEncryptor(keys.generateKey()));
        Mockito.when(project.getProjectProperty(DDProjectShareService.BINDING_PROPERTY)).thenAnswer(call -> binding.get());
        Mockito.doAnswer(call -> { binding.set(call.getArgument(1)); return null; })
            .when(project).setProjectProperty(Mockito.eq(DDProjectShareService.BINDING_PROPERTY), Mockito.any());
    }

    @Test
    void uploadPersistsIdentityAndSyncBaselineAndExcludesLocalSettings() throws Exception {
        Files.writeString(folder.resolve("data-sources.json"), "{}");
        Files.writeString(folder.resolve("project-settings.json"), "local-only");
        Mockito.when(client.pushFiles(Mockito.any(), Mockito.anyMap(), Mockito.anyString(),
            Mockito.eq(DDSharedProjectConfiguration.Format.PORTABLE_JSON))).thenAnswer(call -> {
            Map<String, byte[]> files = call.getArgument(1);
            Assertions.assertEquals(java.util.Set.of("data-sources.json"), files.keySet());
            return new DDSharedProjectRevision(UUID.randomUUID(), userId, LocalDateTime.now(), "uploaded-checksum");
        });
        service.upload(monitor, project);
        Map<?, ?> saved = (Map<?, ?>) binding.get();
        Assertions.assertEquals(userId.toString(), saved.get("userId"));
        Assertions.assertEquals("uploaded-checksum", saved.get("lastSyncChecksum"));
        Assertions.assertNotNull(saved.get("lastSyncTime"));
        Assertions.assertNotNull(service.getRemoteProjectId(project));
        Assertions.assertEquals("local-only", Files.readString(folder.resolve("project-settings.json")));
    }

    @Test
    void downloadRejectsUnexpectedFilesWithoutModifyingLocalData() throws Exception {
        UUID id = UUID.randomUUID();
        Files.writeString(folder.resolve("data-sources.json"), "{\"local\":true}");
        Mockito.when(client.pullFiles(id)).thenReturn(new DDSharedProjectPullResult("remote",
            Map.of("../unexpected.json", new byte[0])));
        Assertions.assertThrows(DBException.class, () -> service.download(monitor, project, id, true));
        Assertions.assertEquals("{\"local\":true}", Files.readString(folder.resolve("data-sources.json")));
        Assertions.assertNull(binding.get());
    }

    @Test
    void downloadDoesNotOverwriteChangesMadeDuringNetworkRequest() throws Exception {
        UUID id = UUID.randomUUID();
        Files.writeString(folder.resolve("data-sources.json"), "{\"local\":true}");
        Mockito.when(client.pullFiles(id)).thenAnswer(call -> {
            Files.writeString(folder.resolve("data-sources.json"), "{\"changed\":true}");
            return new DDSharedProjectPullResult("remote", Map.of());
        });
        Assertions.assertThrows(DBException.class, () -> service.download(monitor, project, id, true));
        Assertions.assertEquals("{\"changed\":true}", Files.readString(folder.resolve("data-sources.json")));
    }

    @Test
    void downloadRemovesMissingManagedFilesAndPreservesOtherFiles() throws Exception {
        UUID id = UUID.randomUUID();
        Files.write(folder.resolve("credentials-config.json"),
            project.getValueEncryptor().encryptValue("{}".getBytes(StandardCharsets.UTF_8)));
        Files.writeString(folder.resolve("unrelated.json"), "keep");
        Mockito.when(client.pullFiles(id)).thenReturn(new DDSharedProjectPullResult("remote",
            Map.of("data-sources.json", new byte[] { '{', '}' })));
        service.download(monitor, project, id, true);
        Assertions.assertFalse(Files.exists(folder.resolve("credentials-config.json")));
        Assertions.assertEquals("{}", Files.readString(folder.resolve("data-sources.json")));
        Assertions.assertEquals("keep", Files.readString(folder.resolve("unrelated.json")));
        Assertions.assertEquals(id, service.getRemoteProjectId(project));
        Mockito.verify(project.getDataSourceRegistry()).refreshConfig();
    }

    @Test
    void cancellationAndOtherAccountBindingPreventNetworkAccess() throws Exception {
        Mockito.when(monitor.isCanceled()).thenReturn(true);
        Assertions.assertThrows(DBException.class, () -> service.upload(monitor, project));
        Mockito.when(monitor.isCanceled()).thenReturn(false);
        binding.set(Map.of("accountId", "other", "projectId", UUID.randomUUID().toString(), "lastSyncChecksum", "old"));
        Assertions.assertThrows(DBException.class, () -> service.upload(monitor, project));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void registryReloadFailureRestoresOriginalFilesAndDoesNotAdvanceBinding() throws Exception {
        UUID id = UUID.randomUUID();
        Files.writeString(folder.resolve("data-sources.json"), "{\"original\":true}");
        Mockito.when(client.pullFiles(id)).thenReturn(new DDSharedProjectPullResult("remote",
            Map.of("data-sources.json", new byte[] { '{', '}' })));
        DBPDataSourceRegistry registry = project.getDataSourceRegistry();
        Mockito.doThrow(new DBException("reload failed")).doNothing()
            .when(registry).checkForErrors();
        Assertions.assertThrows(DBException.class, () -> service.download(monitor, project, id, true));
        Assertions.assertEquals("{\"original\":true}", Files.readString(folder.resolve("data-sources.json")));
        Assertions.assertNull(binding.get());
    }

    @Test
    void externalSecretStorageIsRejectedBeforeCreatingBindingOrDownloading() throws Exception {
        Mockito.when(project.isUseSecretStorage()).thenReturn(true);
        Assertions.assertThrows(DBException.class, () -> service.upload(monitor, project));
        Assertions.assertThrows(DBException.class, () -> service.download(monitor, project, UUID.randomUUID(), true));
        Assertions.assertNull(binding.get());
        Mockito.verifyNoInteractions(client);
    }
}
