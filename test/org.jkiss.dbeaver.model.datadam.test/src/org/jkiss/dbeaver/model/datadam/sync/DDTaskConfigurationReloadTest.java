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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSharedProjectPullResult;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskRegistry;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.registry.task.TaskFolderImpl;
import org.jkiss.dbeaver.registry.task.TaskImpl;
import org.jkiss.dbeaver.registry.task.TaskManagerImpl;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

class DDTaskConfigurationReloadTest {
    @TempDir
    Path folder;

    @Test
    void malformedAndPartiallyInvalidConfigurationsPreserveLiveTasks() throws Exception {
        BaseProjectImpl project = project();
        Files.writeString(folder.resolve("tasks.json"), validTask());
        Manager manager = new Manager(project, folder);
        var original = manager.getAllTasks();
        Assertions.assertEquals(1, original.length);
        for (String invalid : new String[] { "{", "null", "[]", "{\"broken\":7}",
            validTask().substring(0, validTask().length() - 1) + ",\"broken\":{\"task\":\"unknown\"}}" }) {
            Files.writeString(folder.resolve("tasks.json"), invalid);
            Assertions.assertThrows(DBException.class, manager::refreshConfiguration, invalid);
            Assertions.assertArrayEquals(original, manager.getAllTasks());
        }
        manager.validateConfiguration("{}");
        Assertions.assertArrayEquals(original, manager.getAllTasks());
        Files.writeString(folder.resolve("tasks.json"), "{}");
        manager.refreshConfiguration();
        Assertions.assertEquals(0, manager.getAllTasks().length);
    }

    @Test
    void downloadValidatesTaskSemanticsBeforeReplacingAnyFiles() throws Exception {
        BaseProjectImpl project = project();
        Files.writeString(folder.resolve("data-sources.json"), "{}");
        String originalTasks = validTask();
        Files.writeString(folder.resolve("tasks.json"), originalTasks);
        Manager manager = new Manager(project, folder);
        Mockito.when(project.getTaskManager(Mockito.anyBoolean())).thenReturn(manager);
        var original = manager.getAllTasks();
        DDShareClient client = Mockito.mock(DDShareClient.class);
        UUID id = UUID.randomUUID();
        Mockito.when(client.getCurrentShareUserId()).thenReturn(UUID.randomUUID());
        Mockito.when(client.pullFiles(id)).thenReturn(new DDSharedProjectPullResult("remote", Map.of(
            "data-sources.json", "{\"changed\":true}".getBytes(StandardCharsets.UTF_8),
            "tasks.json", "{\"broken\":7}".getBytes(StandardCharsets.UTF_8)),
            DDSharedProjectConfiguration.Format.PORTABLE_JSON));
        Assertions.assertThrows(DBException.class, () ->
            new DDProjectShareService(client, "account").download(Mockito.mock(DBRProgressMonitor.class), project, id, true));
        Assertions.assertEquals("{}", Files.readString(folder.resolve("data-sources.json")));
        Assertions.assertEquals(originalTasks, Files.readString(folder.resolve("tasks.json")));
        Assertions.assertArrayEquals(original, manager.getAllTasks());
        Mockito.verify(project, Mockito.never()).setProjectProperty(Mockito.eq(DDProjectShareService.BINDING_PROPERTY), Mockito.any());
    }

    private BaseProjectImpl project() {
        BaseProjectImpl project = Mockito.mock(BaseProjectImpl.class);
        Mockito.when(project.getMetadataFolder(Mockito.anyBoolean())).thenReturn(folder);
        Mockito.when(project.hasRealmPermission(Mockito.anyString())).thenReturn(true);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(Mockito.mock(DBPDataSourceRegistry.class));
        DBTTaskRegistry registry = Mockito.mock(DBTTaskRegistry.class);
        Mockito.when(registry.getTaskType("known")).thenReturn(Mockito.mock(DBTTaskType.class));
        Mockito.when(project.getProjectProperty("test.taskRegistry")).thenReturn(registry);
        return project;
    }

    private String validTask() {
        String time = new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN).format(new Date());
        return """
            {"original":{"task":"known","label":"Original","createTime":"%s","updateTime":"%s","state":{}}}
            """.formatted(time, time).trim();
    }

    private static class Manager extends TaskManagerImpl {
        Manager(BaseProjectImpl project, Path folder) {
            super(project, folder);
        }

        @NotNull
        @Override
        public DBTTaskRegistry getRegistry() {
            return (DBTTaskRegistry) getProject().getProjectProperty("test.taskRegistry");
        }

        @Override
        protected String loadConfigFile() throws DBException {
            try {
                return Files.readString(getProject().getMetadataFolder(false).resolve("tasks.json"));
            } catch (IOException e) {
                throw new DBException("Cannot read test tasks", e);
            }
        }

        @NotNull
        @Override
        protected TaskImpl createTask(
            @NotNull DBTTaskType type, @NotNull String id, @NotNull String label, @Nullable String description,
            @Nullable Date created, @Nullable Date updated, @Nullable TaskFolderImpl folder, @NotNull Map<String, Object> properties
        ) {
            TaskImpl task = Mockito.mock(TaskImpl.class);
            Mockito.when(task.getId()).thenReturn(id);
            return task;
        }
    }
}
