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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionConfirmation;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionLoading;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionPermissionStore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ServiceRegistry;
import org.jkiss.junit.StaticFieldOverride;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

@Isolated
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
        TestDataSource dataSource = Mockito.mock(TestDataSource.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(container).when(dataSource).getContainer();
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(true);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(true);
        var constructor = SQLiteExtensionPermissionStore.class.getDeclaredConstructor(Path.class);
        constructor.setAccessible(true);
        SQLiteExtensionPermissionStore permissions = constructor.newInstance(directory.resolve("permissions.json"));
        ServiceRegistry services = Mockito.mock(ServiceRegistry.class);
        try (var workbench = new StaticFieldOverride(DBWorkbench.class, "platform", platform);
             var registry = new StaticFieldOverride(ServiceRegistry.class, "instance", services);
             var stores = new StaticFieldOverride(SQLiteExtensionPermissionStore.class, "instance", permissions)) {
            Properties properties = new Properties();

            dataSource.validateConnectionParameters(new VoidProgressMonitor(), "jdbc:sqlite::memory:", properties);

            Assertions.assertEquals("false", properties.getProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION));
            Mockito.verify(services, Mockito.never()).getService(SQLiteExtensionConfirmation.class);
        }
    }

    @Test
    public void disablesExtensionsByDefaultAndPreservesReadOnlyMode() throws Exception {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        TestDataSource dataSource = Mockito.mock(TestDataSource.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(container).when(dataSource).getContainer();
        for (boolean readOnly : new boolean[] {false, true}) {
            Mockito.when(container.isConnectionReadOnly()).thenReturn(readOnly);
            Map<String, String> properties = dataSource.getInternalConnectionProperties(
                new VoidProgressMonitor(), Mockito.mock(DBPDriver.class), null, "test", new DBPConnectionConfiguration());
            Assertions.assertFalse(properties.containsKey(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION));
            Assertions.assertEquals(readOnly ? "1" : null, properties.get("open_mode"));
        }
    }

    public abstract static class TestDataSource extends SQLiteDataSource {
        public TestDataSource(
            @NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container, @NotNull GenericMetaModel metaModel
        ) throws DBException {
            super(monitor, container, metaModel);
        }

        @Override
        public void validateConnectionParameters(
            @NotNull DBRProgressMonitor monitor, @Nullable String url, @NotNull Properties connectProps
        ) throws DBCException {
            super.validateConnectionParameters(monitor, url, connectProps);
        }

        @NotNull
        @Override
        public Map<String, String> getInternalConnectionProperties(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDriver driver,
            @Nullable JDBCExecutionContext context,
            @NotNull String purpose,
            @NotNull DBPConnectionConfiguration connectionInfo
        ) throws DBCException {
            return super.getInternalConnectionProperties(monitor, driver, context, purpose, connectionInfo);
        }
    }
}
