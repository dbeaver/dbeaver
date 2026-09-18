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

import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSharedProjectPullResult;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceConfigurationManager;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceParseResults;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DataSourceSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class DDProjectSharePersistenceTest {
    @TempDir
    Path folder;

    @Test
    void uploadWaitsForActualRegistryPersistence() throws Exception {
        DBPProject project = project();
        Registry registry = registry(project);
        registry.contents.set("{\"pending\":true}");
        DDShareClient client = client(project);
        Mockito.when(client.pushFiles(Mockito.any(), Mockito.anyMap(), Mockito.anyString(), Mockito.any())).thenAnswer(call -> {
            Map<String, byte[]> files = call.getArgument(1);
            Assertions.assertEquals("{\"pending\":true}", new String(files.get("data-sources.json"), StandardCharsets.UTF_8));
            return new DDSharedProjectRevision(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), "saved");
        });
        new DDProjectShareService(client, "account").upload(Mockito.mock(DBRProgressMonitor.class), project);
        Assertions.assertEquals(registry.contents.get(), Files.readString(folder.resolve("data-sources.json")));
    }

    @Test
    void downloadWaitsForOverlappingConfigSaverAndQueuedSaveUsesReloadedState() throws Exception {
        DBPProject project = project();
        Registry registry = registry(project);
        DDShareClient client = client(project);
        UUID id = UUID.randomUUID();
        Mockito.when(client.pullFiles(id)).thenAnswer(call -> {
            registry.blockNextSave.set(true);
            registry.flushConfig();
            Assertions.assertTrue(registry.saveEntered.await(5, TimeUnit.SECONDS));
            return new DDSharedProjectPullResult("remote",
                Map.of("data-sources.json", "{\"remote\":true}".getBytes(StandardCharsets.UTF_8)),
                DDSharedProjectConfiguration.Format.PORTABLE_JSON);
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var download = executor.submit(() -> {
                new DDProjectShareService(client, "account").download(Mockito.mock(DBRProgressMonitor.class), project, id, true);
                return null;
            });
            try {
                Assertions.assertTrue(registry.saveEntered.await(5, TimeUnit.SECONDS));
                Assertions.assertThrows(TimeoutException.class, () -> download.get(150, TimeUnit.MILLISECONDS));
            } finally {
                registry.releaseSave.countDown();
            }
            download.get(10, TimeUnit.SECONDS);
        }
        registry.nextSaveCompleted = new CountDownLatch(1);
        registry.flushConfig();
        Assertions.assertTrue(registry.nextSaveCompleted.await(5, TimeUnit.SECONDS));
        synchronized (registry) {
            Assertions.assertEquals("{\"remote\":true}", Files.readString(folder.resolve("data-sources.json")));
            Assertions.assertEquals("{\"remote\":true}", registry.contents.get());
        }
    }

    private DBPProject project() throws Exception {
        DBPProject project = Mockito.mock(DBPProject.class);
        Mockito.when(project.isOpen()).thenReturn(true);
        Mockito.when(project.getName()).thenReturn("test");
        Mockito.when(project.getMetadataFolder(Mockito.anyBoolean())).thenReturn(folder);
        Files.writeString(folder.resolve("data-sources.json"), "{}");
        return project;
    }

    private DDShareClient client(DBPProject project) throws Exception {
        AtomicReference<Object> binding = new AtomicReference<>();
        Mockito.when(project.getProjectProperty(DDProjectShareService.BINDING_PROPERTY)).thenAnswer(call -> binding.get());
        Mockito.doAnswer(call -> { binding.set(call.getArgument(1)); return null; })
            .when(project).setProjectProperty(Mockito.eq(DDProjectShareService.BINDING_PROPERTY), Mockito.any());
        DDShareClient client = Mockito.mock(DDShareClient.class);
        Mockito.when(client.getCurrentShareUserId()).thenReturn(UUID.randomUUID());
        return client;
    }

    private Registry registry(DBPProject project) throws Exception {
        DataSourceConfigurationManager manager = Mockito.mock(DataSourceConfigurationManager.class);
        DBPDataSourceConfigurationStorage storage = Mockito.mock(DBPDataSourceConfigurationStorage.class);
        Mockito.when(storage.isDefault()).thenReturn(true);
        Mockito.when(manager.isSecure()).thenReturn(true);
        Mockito.when(manager.getConfigurationStorages()).thenReturn(List.of(storage));
        Registry registry = new Registry(project, manager, folder.resolve("data-sources.json"));
        Mockito.when(project.getDataSourceRegistry()).thenReturn(registry);
        registry.saveConfigurationToManager(Mockito.mock(DBRProgressMonitor.class), manager, null);
        return registry;
    }

    private static class Registry extends DataSourceRegistry<DataSourceDescriptor> {
        private final AtomicReference<String> contents = new AtomicReference<>("{}");
        private final AtomicBoolean blockNextSave = new AtomicBoolean();
        private final CountDownLatch saveEntered = new CountDownLatch(1);
        private final CountDownLatch releaseSave = new CountDownLatch(1);
        private volatile CountDownLatch nextSaveCompleted = new CountDownLatch(0);
        private final Path file;
        private final DataSourceSerializer<DataSourceDescriptor> serializer;

        Registry(DBPProject project, DataSourceConfigurationManager manager, Path file) throws Exception {
            super(project, manager, Mockito.mock(DBPPreferenceStore.class));
            this.file = file;
            serializer = Mockito.mock(DataSourceSerializer.class);
            Mockito.doAnswer(call -> {
                if (blockNextSave.compareAndSet(true, false)) {
                    saveEntered.countDown();
                    Assertions.assertTrue(releaseSave.await(10, TimeUnit.SECONDS));
                }
                Files.writeString(file, contents.get());
                nextSaveCompleted.countDown();
                return null;
            }).when(serializer).saveDataSources(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyList());
        }

        @NotNull
        @Override
        protected DataSourceSerializer<DataSourceDescriptor> createModernSerializer() {
            return serializer;
        }

        @Override
        protected boolean loadDataSources(
            @NotNull DBPDataSourceConfigurationStorage storage,
            @NotNull DataSourceConfigurationManager manager,
            @Nullable Collection<String> ids,
            @NotNull DataSourceParseResults results
        ) {
            try {
                contents.set(Files.readString(file));
                return true;
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }
}
