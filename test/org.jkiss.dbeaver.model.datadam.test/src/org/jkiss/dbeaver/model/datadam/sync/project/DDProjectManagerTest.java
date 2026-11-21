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
package org.jkiss.dbeaver.model.datadam.sync.project;

import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.project.sync.DDProjectSyncBindingStore;
import org.jkiss.dbeaver.model.datadam.sync.project.sync.DDProjectSyncLocalBinding;
import org.jkiss.dbeaver.model.datadam.sync.project.sync.DDProjectSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Timeout(5)
class DDProjectManagerTest {
    @TempDir
    Path directory;

    @Test
    void keyRecoveryDeletesOnlySharedProjectsForTheOldAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        DBPProject shared = project("shared", accountId);
        DBPProject otherAccount = project("other-account", UUID.randomUUID());
        DBPProject local = project("local", null);
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        Mockito.when(workspace.getProjects()).thenAnswer(invocation -> List.of(shared, otherAccount, local));
        var access = Mockito.mock(DDDesktopAccessService.class);
        var client = new DDShareClient("https://example.invalid", access);
        var manager = new DDProjectManager(workspace, client, new DDProjectSyncService(client), access);

        manager.deleteAccountProjects(accountId);

        Mockito.verify(workspace).deleteProject(shared);
        Mockito.verify(workspace, Mockito.never()).deleteProject(otherAccount);
        Mockito.verify(workspace, Mockito.never()).deleteProject(local);
        Mockito.verifyNoInteractions(access);
    }

    @NotNull
    private DBPProject project(@NotNull String name, @Nullable UUID accountId) throws Exception {
        DBPProject project = Mockito.mock(DBPProject.class);
        Path metadata = Files.createDirectory(directory.resolve(name));
        Mockito.when(project.getMetadataFolder(false)).thenReturn(metadata);
        if (accountId != null) {
            var revision = new DDSharedProjectRevision(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), "fingerprint");
            var binding = new DDProjectSyncLocalBinding(UUID.randomUUID(), accountId, revision, Set.of("connections"));
            Files.writeString(DDProjectSyncBindingStore.getBindingFile(project), JSONUtils.PRETTY_GSON.toJson(binding));
        }
        return project;
    }
}
