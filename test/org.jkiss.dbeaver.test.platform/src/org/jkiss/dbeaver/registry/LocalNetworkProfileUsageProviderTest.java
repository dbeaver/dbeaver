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
package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.secret.DBSValueEncryptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class LocalNetworkProfileUsageProviderTest {
    @TempDir
    Path projectFolder;

    @Test
    void skipsUnreadableEncryptedProjectOnlyForCreation() throws Exception {
        DBPProject readable = project("readable", "Readable", "{\"network-profiles\":{\"shared\":{}}}");
        DBPProject encrypted = project("encrypted", "Encrypted", "\"encrypted configuration\"");
        Mockito.when(encrypted.getProjectProperty("secureProject")).thenReturn(true);
        LocalNetworkProfileUsageProvider provider = provider(readable, encrypted);

        Assertions.assertTrue(provider(encrypted).findLocalProfileConflicts("shared").isEmpty());
        Assertions.assertEquals(Map.of("readable", "Readable"), provider.findLocalProfileConflicts("shared"));
        Mockito.verify(encrypted, Mockito.never()).getMetadataFolder(false);
        Assertions.assertThrows(DBException.class, () -> provider.findGlobalProfileConnections("shared"));
    }

    @Test
    void checksEncryptedProjectsWhenDecryptionIsAvailable() throws Exception {
        DBPProject encrypted = project("encrypted", "Encrypted", "encrypted configuration");
        DBSValueEncryptor encryptor = Mockito.mock(DBSValueEncryptor.class);
        Mockito.when(encrypted.getProjectProperty("secureProject")).thenReturn(true);
        Mockito.when(encrypted.isEncryptedProject()).thenReturn(true);
        Mockito.when(encrypted.getValueEncryptor()).thenReturn(encryptor);
        Mockito.when(encryptor.decryptValue(ArgumentMatchers.any())).thenReturn(
            "{\"network-profiles\":{\"shared\":{}}}".getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(Map.of("encrypted", "Encrypted"),
            provider(encrypted).findLocalProfileConflicts("shared"));
        Mockito.verify(encryptor).decryptValue(ArgumentMatchers.any());
    }

    @Test
    void failsForInvalidConfigurationInReadableProject() throws Exception {
        DBPProject readable = project("readable", "Readable", "{\"network-profiles\": ");

        Assertions.assertThrows(DBException.class, () -> provider(readable).findLocalProfileConflicts("shared"));
    }

    private DBPProject project(String id, String name, String configuration) throws Exception {
        Path metadata = Files.createDirectories(projectFolder.resolve(id).resolve(".dbeaver"));
        Files.writeString(metadata.resolve(DBPDataSourceRegistry.MODERN_CONFIG_FILE_NAME), configuration);
        DBPProject project = Mockito.mock(DBPProject.class);
        Mockito.when(project.getId()).thenReturn(id);
        Mockito.when(project.getName()).thenReturn(name);
        Mockito.when(project.getMetadataFolder(false)).thenReturn(metadata);
        return project;
    }

    private static LocalNetworkProfileUsageProvider provider(DBPProject... projects) {
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        Mockito.when(workspace.getProjects()).thenAnswer(invocation -> List.of(projects));
        return new LocalNetworkProfileUsageProvider(workspace);
    }
}
