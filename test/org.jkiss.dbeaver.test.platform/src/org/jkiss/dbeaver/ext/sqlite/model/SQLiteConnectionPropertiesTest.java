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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionConfirmation;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionLoading;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionPermissionStore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public class SQLiteConnectionPropertiesTest {
    @TempDir
    Path directory;

    @Test
    public void defaultConnectionDoesNotRequireUi() throws Exception {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(container.getActualConnectionConfiguration()).thenReturn(new DBPConnectionConfiguration());
        Mockito.when(container.getProject().getAbsolutePath()).thenReturn(directory);
        Mockito.when(container.getId()).thenReturn("sqlite-test");
        Mockito.when(container.getDriver().getFullId()).thenReturn("sqlite:sqlite_jdbc");
        Mockito.when(container.getDriver().getDriverClassName()).thenReturn("org.sqlite.JDBC");
        SQLiteDataSource dataSource = Mockito.mock(SQLiteDataSource.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(container).when(dataSource).getContainer();
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(true);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(true);
        SQLiteExtensionPermissionStore permissions = Mockito.mock(SQLiteExtensionPermissionStore.class);
        try (var workbench = Mockito.mockStatic(DBWorkbench.class);
             var stores = Mockito.mockStatic(SQLiteExtensionPermissionStore.class)) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            stores.when(SQLiteExtensionPermissionStore::getInstance).thenReturn(permissions);
            Properties properties = new Properties();

            dataSource.validateConnectionParameters(new VoidProgressMonitor(), "jdbc:sqlite::memory:", properties);

            Assertions.assertEquals("false", properties.getProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION));
            workbench.verify(() -> DBWorkbench.getService(SQLiteExtensionConfirmation.class), Mockito.never());
        }
    }

    @Test
    public void disablesExtensionsByDefaultAndPreservesReadOnlyMode() throws Exception {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        SQLiteDataSource dataSource = Mockito.mock(SQLiteDataSource.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(container).when(dataSource).getContainer();
        for (boolean readOnly : new boolean[] {false, true}) {
            Mockito.when(container.isConnectionReadOnly()).thenReturn(readOnly);
            Map<String, String> properties = dataSource.getInternalConnectionProperties(
                new VoidProgressMonitor(), Mockito.mock(DBPDriver.class), null, "test", new DBPConnectionConfiguration());
            Assertions.assertFalse(properties.containsKey(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION));
            Assertions.assertEquals(readOnly ? "1" : null, properties.get("open_mode"));
        }
    }
}
