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

public class DataSourceDescriptorTest extends DBeaverUnitTest {
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
