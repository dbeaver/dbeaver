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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public class MySQLConnectionPipelineTest extends DBeaverUnitTest {
    private final DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
    private final DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
    private final DBPDriver driver = Mockito.mock(DBPDriver.class, Mockito.RETURNS_DEEP_STUBS);
    private final Driver jdbcDriver = Mockito.mock(Driver.class);
    private final Connection connection = Mockito.mock(Connection.class);
    @SuppressWarnings("unchecked")
    private final DBAAuthModel<DBAAuthCredentials> authModel = Mockito.mock(DBAAuthModel.class);
    private TestDataSource dataSource;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.when(container.getDriver()).thenReturn(driver);
        Mockito.when(container.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        Mockito.when(container.getActualConnectionConfiguration()).thenReturn(configuration);
        var descriptor = driver.getDataSourceProvider().detectConnectionAuthModel(driver, configuration);
        Mockito.when(descriptor.isApplicableTo(driver)).thenReturn(true);
        Mockito.doReturn(authModel).when(descriptor).getInstance();
        Mockito.when(driver.isInstantiable()).thenReturn(true);
        Mockito.when(driver.getDriverClassName()).thenReturn("com.example.CustomDriver");
        Mockito.when(driver.getConnectionURL(configuration)).thenReturn("jdbc:trino://localhost:8080/test");
        Mockito.when(driver.getDriverLoader(container).getDriverInstance(monitor)).thenReturn(jdbcDriver);
        Mockito.when(driver.getDriverLoader(container).getClassLoader()).thenReturn(getClass().getClassLoader());
        Mockito.when(jdbcDriver.acceptsURL(Mockito.anyString())).thenReturn(true);
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenReturn(connection);
        // Mock only the unrelated abstract metadata methods; exercise the real JDBC pipeline and Eclipse adapter.
        dataSource = Mockito.mock(TestDataSource.class, Mockito.withSettings()
            .useConstructor(container).defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @ParameterizedTest
    @CsvSource({"true,false,true", "false,true,true", "true,true,true", "false,false,false"})
    public void enforcesPolicyAfterAuthentication(boolean sanitize, boolean multiuser, boolean rejected) throws Exception {
        Mockito.when(authModel.initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.isNull(),
            Mockito.same(configuration), Mockito.any(Properties.class)
        )).thenAnswer(invocation -> {
            ((Properties) invocation.getArgument(4)).setProperty("ALLOWLOCALINFILE", "false");
            return null;
        });

        if (rejected) {
            Assertions.assertThrows(DBCException.class, () -> open(sanitize, multiuser));
            Mockito.verify(driver.getDriverLoader(container), Mockito.never()).getDriverInstance(monitor);
            Mockito.verifyNoInteractions(jdbcDriver);
        } else {
            Assertions.assertSame(connection, open(sanitize, multiuser));
        }
        Mockito.verify(authModel).endAuthentication(Mockito.same(container), Mockito.same(configuration), Mockito.any(Properties.class));
        Assertions.assertTrue(configuration.getProperties().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:custom:template;ALLOWLOCALINFILE=false", "jdbc:mysql://localhost/test?%61utoDeserialize=true"})
    public void rejectsFinalDriverTemplateBeforeDriverCreation(@NotNull String url) throws Exception {
        Mockito.when(driver.getConnectionURL(configuration)).thenReturn(url);

        Assertions.assertThrows(DBCException.class, () -> open(true, false));

        Mockito.verify(driver.getDriverLoader(container), Mockito.never()).getDriverInstance(monitor);
        Mockito.verifyNoInteractions(jdbcDriver);
        Mockito.verify(authModel).endAuthentication(Mockito.same(container), Mockito.same(configuration), Mockito.any(Properties.class));
    }

    @Test
    public void strictDriverAcceptsLiteralPercentPasswordWithoutMysqlDefaults() throws Exception {
        Map<String, String> expected = Map.of("user", "test-user", "password", "example%password");
        configuration.getProperties().putAll(expected);
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenAnswer(invocation -> {
            Properties properties = invocation.getArgument(1);
            for (String name : properties.stringPropertyNames()) {
                if (!expected.containsKey(name)) {
                    throw new SQLException("Unrecognized connection property '" + name + "'");
                }
            }
            Assertions.assertEquals(expected, properties);
            return connection;
        });

        Assertions.assertSame(connection, open(true, false));
        Assertions.assertSame(connection, open(false, true));
        Assertions.assertEquals(expected, configuration.getProperties());
    }

    @NotNull
    private Connection open(boolean sanitize, boolean multiuser) throws DBCException {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getPreferenceStore().getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES)).thenReturn(sanitize);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(multiuser);
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            return dataSource.open(monitor, configuration);
        }
    }

    public abstract static class TestDataSource extends JDBCDataSource {
        protected TestDataSource(@NotNull DBPDataSourceContainer container) {
            super(container, Mockito.mock(SQLDialect.class));
        }

        @NotNull
        Connection open(@NotNull DBRProgressMonitor monitor, @NotNull DBPConnectionConfiguration configuration) throws DBCException {
            return super.openConnection(monitor, null, configuration, "Test connection policy");
        }
    }
}
