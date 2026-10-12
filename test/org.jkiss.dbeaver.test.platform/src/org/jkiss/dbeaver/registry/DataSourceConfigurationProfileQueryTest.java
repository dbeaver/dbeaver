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
import org.jkiss.dbeaver.model.secret.DBSValueEncryptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class DataSourceConfigurationProfileQueryTest {
    @TempDir
    Path projectFolder;

    @Test
    void readsPersistedProfilesAndConnectionsAcrossStorages() throws Exception {
        Path metadata = Files.createDirectory(projectFolder.resolve(".dbeaver"));
        Files.writeString(
            metadata.resolve(DBPDataSourceRegistry.MODERN_CONFIG_FILE_NAME), """
                {"network-profiles":{"local":{}},"connections":{
                    "global-id":{"name":"Global connection","configuration":{"config-profile":"global"}},
                    "external-id":{"name":"External","configuration":{"config-profile":"global","config-profile-source":"remote"}},
                    "local-id":{"name":"Local connection","configuration":{"config-profile":"local"}}
                }}
                """
        );
        Files.writeString(
            metadata.resolve("data-sources-extra.json"), """
                {"connections":{"another-id":{"name":"Another connection","configuration":{"config-profile":"global"}}}}
                """
        );
        DBPProject project = Mockito.mock(DBPProject.class);
        Mockito.when(project.getMetadataFolder(false)).thenReturn(metadata);
        Mockito.when(project.getAbsolutePath()).thenReturn(projectFolder);

        Assertions.assertTrue(DataSourceConfigurationProfileQuery.hasLocalProfile(project, "local"));
        Assertions.assertTrue(DataSourceConfigurationProfileQuery.findGlobalProfileConnections(project, "local").isEmpty());
        Assertions.assertFalse(DataSourceConfigurationProfileQuery.hasLocalProfile(project, "global"));
        Assertions.assertEquals(
            Map.of("global-id", "Global connection", "another-id", "Another connection"),
            DataSourceConfigurationProfileQuery.findGlobalProfileConnections(project, "global")
        );
        Mockito.verify(project, Mockito.never()).getDataSourceRegistry();
    }

    @Test
    void readsEncryptedConfigurationThroughSerializer() throws Exception {
        Path metadata = Files.createDirectory(projectFolder.resolve(".dbeaver"));
        Files.write(metadata.resolve(DBPDataSourceRegistry.MODERN_CONFIG_FILE_NAME), new byte[] {1, 2, 3});
        DBPProject project = Mockito.mock(DBPProject.class);
        DBSValueEncryptor encryptor = Mockito.mock(DBSValueEncryptor.class);
        Mockito.when(project.getMetadataFolder(false)).thenReturn(metadata);
        Mockito.when(project.isEncryptedProject()).thenReturn(true);
        Mockito.when(project.getValueEncryptor()).thenReturn(encryptor);
        Mockito.when(encryptor.decryptValue(ArgumentMatchers.any())).thenReturn("""
            {"network-profiles":{"local":{}},"connections":{}}
            """.getBytes(StandardCharsets.UTF_8));

        Assertions.assertTrue(DataSourceConfigurationProfileQuery.hasLocalProfile(project, "local"));
        Mockito.verify(encryptor).decryptValue(ArgumentMatchers.any());
        Mockito.verify(project, Mockito.never()).getDataSourceRegistry();
    }

    @Test
    void skipsLegacyStoragesWithoutLoadingTheRegistry() throws Exception {
        Files.writeString(projectFolder.resolve(DBPDataSourceRegistry.LEGACY_CONFIG_FILE_NAME), "not JSON");
        Files.writeString(projectFolder.resolve(".dbeaver-data-sources-extra.xml"), "not JSON");
        DBPProject project = Mockito.mock(DBPProject.class);
        Mockito.when(project.getAbsolutePath()).thenReturn(projectFolder);
        Mockito.when(project.getMetadataFolder(false)).thenReturn(projectFolder.resolve(".dbeaver"));
        Mockito.when(project.getName()).thenReturn("Legacy project");

        Assertions.assertFalse(DataSourceConfigurationProfileQuery.hasLocalProfile(project, "profile"));
        Assertions.assertTrue(DataSourceConfigurationProfileQuery.findGlobalProfileConnections(project, "profile").isEmpty());
        Mockito.verify(project, Mockito.never()).getDataSourceRegistry();
    }

    @Test
    void doesNotSkipInvalidJson() throws Exception {
        Path metadata = Files.createDirectory(projectFolder.resolve(".dbeaver"));
        Files.writeString(metadata.resolve(DBPDataSourceRegistry.MODERN_CONFIG_FILE_NAME), "{\"network-profiles\": ");
        DBPProject project = Mockito.mock(DBPProject.class);
        Mockito.when(project.getMetadataFolder(false)).thenReturn(metadata);

        Assertions.assertThrows(DBException.class, () -> DataSourceConfigurationProfileQuery.hasLocalProfile(project, "profile"));
    }
}
