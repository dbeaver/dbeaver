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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DataSourceDescriptorTest extends DBeaverUnitTest {
    private static final String TEST_EXTENSION = "test.settings";

    @Test
    public void testExtensionModelDoesNotWaitForDatasourceMonitor() throws Exception {
        DataSourceDescriptor dataSource = createDataSource(new DBPConnectionConfiguration());
        dataSource.setExtension(TEST_EXTENSION, Map.of("enabled", true));

        try (var executor = Executors.newSingleThreadExecutor()) {
            synchronized (dataSource) {
                ExtensionSettings model = executor.submit(() ->
                    dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class)).get(1, TimeUnit.SECONDS);
                Assertions.assertNotNull(model);
                Assertions.assertTrue(model.enabled);
            }
        }
    }

    @Test
    public void testExtensionModelIsCachedAndInvalidatedOnReplacement() {
        DataSourceDescriptor dataSource = createDataSource(new DBPConnectionConfiguration());
        dataSource.setExtension(TEST_EXTENSION, Map.of("enabled", true));

        ExtensionSettings model = dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class);
        Assertions.assertNotNull(model);
        Assertions.assertTrue(model.enabled);
        Assertions.assertSame(model, dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class));

        dataSource.setExtension(TEST_EXTENSION, Map.of("enabled", false));
        ExtensionSettings replacement = dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class);
        Assertions.assertNotNull(replacement);
        Assertions.assertNotSame(model, replacement);
        Assertions.assertFalse(replacement.enabled);

        dataSource.setExtension(TEST_EXTENSION, null);
        Assertions.assertNull(dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class));
    }

    @Test
    public void testSetExtensionModelStoresSnapshotInMapFormat() {
        DataSourceDescriptor dataSource = createDataSource(new DBPConnectionConfiguration());
        ExtensionSettings settings = new ExtensionSettings();
        settings.enabled = true;
        dataSource.setExtensionModel(TEST_EXTENSION, settings);
        settings.enabled = false;

        Assertions.assertEquals(Map.of("enabled", true), dataSource.getExtension(TEST_EXTENSION));
        ExtensionSettings model = dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class);
        Assertions.assertNotNull(model);
        Assertions.assertTrue(model.enabled);

        dataSource.setExtensionModel(TEST_EXTENSION, settings);
        Assertions.assertFalse(dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class).enabled);
        dataSource.setExtensionModel(TEST_EXTENSION, null);
        Assertions.assertNull(dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class));
        Assertions.assertFalse(dataSource.hasExtension(TEST_EXTENSION));
    }

    @Test
    public void testExtensionModelSupportsLegacyStrings() {
        DataSourceDescriptor dataSource = createDataSource(new DBPConnectionConfiguration());
        for (String config : new String[] {"{\"enabled\":true}", "{enabled=true}"}) {
            dataSource.setExtension(TEST_EXTENSION, config);
            ExtensionSettings model = dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class);
            Assertions.assertNotNull(model);
            Assertions.assertTrue(model.enabled);
            Assertions.assertSame(model, dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class));
            Assertions.assertEquals(config, dataSource.getExtension(TEST_EXTENSION));
        }
    }

    @Test
    public void testExtensionModelsAreInvalidatedOnBulkReplacementAndCopy() {
        DataSourceDescriptor dataSource = createDataSource(new DBPConnectionConfiguration());
        Assertions.assertNull(dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class));
        dataSource.setExtensions(Map.of(TEST_EXTENSION, Map.of("enabled", true)));
        ExtensionSettings original = dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class);
        Assertions.assertNotNull(original);
        Assertions.assertTrue(original.enabled);

        DataSourceDescriptor copy = new DataSourceDescriptor(dataSource, dataSource.getRegistry(), false);
        Assertions.assertNotSame(original, copy.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class));
        copy.setExtension(TEST_EXTENSION, Map.of("enabled", false));
        dataSource.copyFrom(copy);
        Assertions.assertFalse(dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class).enabled);

        dataSource.setExtensions(Map.of());
        Assertions.assertNull(dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class));
    }

    @Test
    public void testInvalidExtensionModelDoesNotPreventReplacement() {
        DataSourceDescriptor dataSource = createDataSource(new DBPConnectionConfiguration());
        dataSource.setExtension(TEST_EXTENSION, "{\"enabled\":");
        Assertions.assertNull(dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class));
        Assertions.assertNull(dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class));
        dataSource.setExtension(TEST_EXTENSION, Map.of("enabled", true));
        Assertions.assertTrue(dataSource.getExtensionModel(TEST_EXTENSION, ExtensionSettings.class).enabled);
    }

    public static class ExtensionSettings {
        public boolean enabled;
    }

    @Test
    public void testResetRevokedSharedCredentials() {
        DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
        connectionInfo.setUserName("shared-user");
        connectionInfo.setUserPassword("shared-password");
        connectionInfo.setAuthProperty("token", "shared-token");

        DataSourceDescriptor dataSource = Mockito.spy(createDataSource(connectionInfo));
        dataSource.setName("Test datasource");
        dataSource.setSharedCredentials(true);
        dataSource.setSavePassword(true);
        String secret = dataSource.saveToSecret();
        Assertions.assertNotNull(secret);
        dataSource.setSelectedSharedCredentials(new DBSSecretValue("test-team", "test-secret", "Test secret", secret));

        Mockito.doReturn(true, false).when(dataSource).isConnected();
        dataSource.resetAllSecrets();

        Assertions.assertAll(
            () -> Assertions.assertEquals("shared-user", connectionInfo.getUserName()),
            () -> Assertions.assertEquals("shared-password", connectionInfo.getUserPassword()),
            () -> Assertions.assertEquals("shared-token", connectionInfo.getAuthProperty("token")),
            () -> Assertions.assertTrue(dataSource.isSavePassword())
        );

        dataSource.setSharedCredentials(false);
        DataSourceDescriptor dataSourceCopy = new DataSourceDescriptor(dataSource, dataSource.getRegistry(), false);

        Assertions.assertAll(
            () -> Assertions.assertNull(dataSourceCopy.getConnectionConfiguration().getUserName()),
            () -> Assertions.assertNull(dataSourceCopy.getConnectionConfiguration().getUserPassword()),
            () -> Assertions.assertTrue(dataSourceCopy.getConnectionConfiguration().getAuthProperties().isEmpty()),
            () -> Assertions.assertFalse(dataSourceCopy.isSavePassword())
        );

        dataSource.resetAllSecrets();

        Assertions.assertAll(
            () -> Assertions.assertNull(connectionInfo.getUserName()),
            () -> Assertions.assertNull(connectionInfo.getUserPassword()),
            () -> Assertions.assertTrue(connectionInfo.getAuthProperties().isEmpty()),
            () -> Assertions.assertFalse(dataSource.isSavePassword()),
            () -> Assertions.assertNull(dataSource.getSelectedSharedCredentials())
        );
    }

    @Test
    public void testRevokedSharedCredentialsAreNotPersistedBeforeDisconnect() {
        DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
        connectionInfo.setUserName("shared-user");
        connectionInfo.setUserPassword("shared-password");
        connectionInfo.setAuthProperty("token", "shared-token");

        DataSourceDescriptor dataSource = Mockito.spy(createDataSource(connectionInfo));
        dataSource.setSharedCredentials(true);
        dataSource.setSavePassword(true);
        Mockito.doReturn(true).when(dataSource).isConnected();

        dataSource.setSharedCredentials(false);

        SecureCredentials persistedCredentials = new SecureCredentials(dataSource);
        Assertions.assertAll(
            () -> Assertions.assertNull(persistedCredentials.getUserName()),
            () -> Assertions.assertNull(persistedCredentials.getUserPassword()),
            () -> Assertions.assertNull(persistedCredentials.getProperties()),
            () -> Assertions.assertNull(dataSource.saveToSecret()),
            () -> Assertions.assertEquals("shared-user", connectionInfo.getUserName()),
            () -> Assertions.assertEquals("shared-password", connectionInfo.getUserPassword()),
            () -> Assertions.assertEquals("shared-token", connectionInfo.getAuthProperty("token")),
            () -> Assertions.assertTrue(dataSource.isSavePassword())
        );
    }

    @Test
    public void testRevokedSharedCredentialsAreClearedImmediatelyWhenDisconnected() {
        DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
        connectionInfo.setUserName("shared-user");
        connectionInfo.setUserPassword("shared-password");
        connectionInfo.setAuthProperty("token", "shared-token");

        DataSourceDescriptor dataSource = createDataSource(connectionInfo);
        dataSource.setSharedCredentials(true);
        dataSource.setSavePassword(true);

        dataSource.setSharedCredentials(false);

        Assertions.assertAll(
            () -> Assertions.assertNull(connectionInfo.getUserName()),
            () -> Assertions.assertNull(connectionInfo.getUserPassword()),
            () -> Assertions.assertTrue(connectionInfo.getAuthProperties().isEmpty()),
            () -> Assertions.assertFalse(dataSource.isSavePassword())
        );
    }

    @Test
    public void testForceSetSharedCredentialsKeepsReloadedPrivateCredentials() {
        DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
        connectionInfo.setUserName("shared-user");
        connectionInfo.setUserPassword("shared-password");
        connectionInfo.setAuthProperty("token", "shared-token");
        DataSourceDescriptor dataSource = createDataSource(connectionInfo);
        dataSource.setSharedCredentials(true);
        dataSource.setSavePassword(true);

        dataSource.forceSetSharedCredentials(false);
        connectionInfo.setUserName("private-user");
        connectionInfo.setUserPassword("private-password");
        connectionInfo.setAuthProperty("token", "private-token");
        dataSource.resetAllSecrets();

        Assertions.assertAll(
            () -> Assertions.assertEquals("private-user", connectionInfo.getUserName()),
            () -> Assertions.assertEquals("private-password", connectionInfo.getUserPassword()),
            () -> Assertions.assertEquals("private-token", connectionInfo.getAuthProperty("token")),
            () -> Assertions.assertTrue(dataSource.isSavePassword())
        );
    }

    @Test
    public void testResetPrivateCredentialsKeepsCredentials() {
        DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
        connectionInfo.setUserName("private-user");
        connectionInfo.setUserPassword("private-password");
        connectionInfo.setAuthProperty("token", "private-token");
        DataSourceDescriptor dataSource = createDataSource(connectionInfo);
        dataSource.setSavePassword(true);

        dataSource.resetAllSecrets();

        Assertions.assertAll(
            () -> Assertions.assertEquals("private-user", connectionInfo.getUserName()),
            () -> Assertions.assertEquals("private-password", connectionInfo.getUserPassword()),
            () -> Assertions.assertEquals("private-token", connectionInfo.getAuthProperty("token")),
            () -> Assertions.assertTrue(dataSource.isSavePassword())
        );
    }

    @NotNull
    private static DataSourceDescriptor createDataSource(@NotNull DBPConnectionConfiguration connectionInfo) {
        DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
        DBPDataSourceConfigurationStorage storage = Mockito.mock(DBPDataSourceConfigurationStorage.class);
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        Mockito.when(registry.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        Mockito.when(driver.getDefaultConnectionProperties()).thenReturn(Map.of());
        Mockito.when(driver.getFullId()).thenReturn("test-driver");
        return new DataSourceDescriptor(
            registry,
            storage,
            DataSourceOriginLocal.INSTANCE,
            "test-datasource",
            driver,
            connectionInfo
        );
    }
}
