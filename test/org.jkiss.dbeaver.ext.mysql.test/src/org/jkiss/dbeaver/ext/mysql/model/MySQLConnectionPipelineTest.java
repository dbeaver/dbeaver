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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConnectionValidator;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
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
        Mockito.when(driver.getConnectionURL(configuration)).thenReturn("jdbc:mysql://localhost/test");
        Mockito.when(driver.getDriverLoader(container).getDriverInstance(monitor)).thenReturn(jdbcDriver);
        Mockito.when(driver.getDriverLoader(container).getClassLoader()).thenReturn(getClass().getClassLoader());
        Mockito.when(jdbcDriver.acceptsURL(Mockito.anyString())).thenReturn(true);
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenReturn(connection);
        dataSource = new TestDataSource(monitor, container);
    }

    @Test
    public void adapterAcceptsOnlyMysqlDataSourcesIncludingSubclasses() {
        JDBCDataSource other = createOtherDataSource();

        Assertions.assertInstanceOf(MySQLConnectionValidator.class, GeneralUtils.adapt(dataSource, JDBCConnectionValidator.class));
        Assertions.assertNull(GeneralUtils.adapt(other, JDBCConnectionValidator.class));
    }

    @ParameterizedTest
    @CsvSource({"true,false,true", "false,true,true", "true,true,true", "false,false,false"})
    public void enforcesPolicyAfterAuthentication(boolean sanitize, boolean multiuser, boolean rejected) throws Exception {
        Mockito.when(authModel.initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.isNull(),
            Mockito.same(configuration), Mockito.any(Properties.class)
        )).thenAnswer(invocation -> {
            ((Properties) invocation.getArgument(4)).setProperty("ALLOWLOCALINFILE", "true");
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
    public void rejectsDriverUrlBeforeJdbcUrlParsing(@NotNull String url) throws Exception {
        Mockito.when(driver.getConnectionURL(configuration)).thenReturn(url);

        Assertions.assertThrows(DBCException.class, () -> open(true, false));

        Mockito.verify(driver.getDriverLoader(container), Mockito.never()).getDriverInstance(monitor);
        Mockito.verifyNoInteractions(jdbcDriver);
        Mockito.verify(authModel).endAuthentication(Mockito.same(container), Mockito.same(configuration), Mockito.any(Properties.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:mysql://localhost/test", "jdbc:custom:template;ALLOWLOCALINFILE=true"})
    public void otherJdbcDataSourcesAreNotRestrictedOrGivenDefaults(@NotNull String url) throws Exception {
        TestJDBCDataSource other = createOtherDataSource();
        Mockito.when(driver.getConnectionURL(configuration)).thenReturn(url);
        Mockito.when(authModel.initAuthentication(
            Mockito.same(monitor), Mockito.same(other), Mockito.isNull(),
            Mockito.same(configuration), Mockito.any(Properties.class)
        )).thenAnswer(invocation -> {
            ((Properties) invocation.getArgument(4)).setProperty("ALLOWLOCALINFILE", "true");
            return null;
        });
        Mockito.when(jdbcDriver.connect(Mockito.eq(url), Mockito.any(Properties.class))).thenAnswer(invocation -> {
            Assertions.assertEquals(Map.of("ALLOWLOCALINFILE", "true"), invocation.getArgument(1));
            return connection;
        });

        Assertions.assertSame(connection, open(other, true, false));
        Assertions.assertSame(connection, open(other, false, true));
        Assertions.assertTrue(configuration.getProperties().isEmpty());
        Mockito.verify(authModel, Mockito.times(2))
            .endAuthentication(Mockito.same(container), Mockito.same(configuration), Mockito.any(Properties.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mysql5", "mysql8", "mysql_ndb", "mariaDB"})
    public void driverDefaultsArePreservedRegardlessOfPolicy(@NotNull String driverId) throws Exception {
        Map<String, Object> defaults = DataSourceProviderRegistry.getInstance()
            .getDataSourceProvider("mysql").getDriver(driverId).getDefaultConnectionProperties();
        for (String name : new String[] {"autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile"}) {
            Assertions.assertEquals("false", defaults.get(name));
        }
        Mockito.when(driver.getConnectionProperties()).thenReturn(defaults);
        // The adapter's data source type, rather than the URL prefix, determines whether the policy applies.
        Mockito.when(driver.getConnectionURL(configuration)).thenReturn("jdbc:custom:test");
        configuration.setProperty("password", "example%password+");
        Properties expected = new Properties();
        defaults.forEach((name, value) -> expected.setProperty(name, value.toString()));
        expected.setProperty("password", "example%password+");
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenAnswer(invocation -> {
            Assertions.assertEquals(expected, invocation.getArgument(1));
            return connection;
        });

        Assertions.assertSame(connection, open(true, false));
        Assertions.assertSame(connection, open(false, true));
        Assertions.assertSame(connection, open(false, false));
        Assertions.assertEquals(Map.of("password", "example%password+"), configuration.getProperties());
    }

    @ParameterizedTest
    @ValueSource(strings = {"autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile"})
    public void rejectsUnsafeConnectionOverridesOfDriverDefaults(@NotNull String name) {
        Mockito.when(driver.getConnectionProperties()).thenReturn(Map.of(name, "false"));
        configuration.setProperty(name, "true");

        Assertions.assertThrows(DBCException.class, () -> open(true, false));
        Assertions.assertThrows(DBCException.class, () -> open(false, true));
        Mockito.verifyNoInteractions(jdbcDriver);
    }

    @NotNull
    private TestJDBCDataSource createOtherDataSource() {
        return Mockito.mock(TestJDBCDataSource.class, Mockito.withSettings()
            .useConstructor(container).defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @NotNull
    private Connection open(boolean sanitize, boolean multiuser) throws DBCException {
        return open(dataSource, sanitize, multiuser);
    }

    @NotNull
    private Connection open(@NotNull TestConnectionSource source, boolean sanitize, boolean multiuser) throws DBCException {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getPreferenceStore().getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES)).thenReturn(sanitize);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(multiuser);
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            return source.open(monitor, configuration);
        }
    }

    private interface TestConnectionSource {
        @NotNull
        Connection open(@NotNull DBRProgressMonitor monitor, @NotNull DBPConnectionConfiguration configuration) throws DBCException;
    }

    public static class TestDataSource extends MySQLDataSource implements TestConnectionSource {
        TestDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container) throws DBException {
            super(monitor, container, Mockito.mock(SQLDialect.class));
        }

        @Override
        protected void initializeRemoteInstance(@NotNull DBRProgressMonitor monitor) {
            // Keep construction offline; the test explicitly opens the connection through the JDBC pipeline.
        }

        @NotNull
        @Override
        protected Map<String, String> getInternalConnectionProperties(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDriver driver,
            @Nullable JDBCExecutionContext context,
            @NotNull String purpose,
            @NotNull DBPConnectionConfiguration connectionInfo
        ) {
            // MySQL's version-dependent defaults can load the driver before the validation under test.
            return Map.of();
        }

        @NotNull
        @Override
        public Connection open(@NotNull DBRProgressMonitor monitor, @NotNull DBPConnectionConfiguration configuration) throws DBCException {
            return super.openConnection(monitor, null, configuration, "Test connection policy");
        }
    }

    public abstract static class TestJDBCDataSource extends JDBCDataSource implements TestConnectionSource {
        protected TestJDBCDataSource(@NotNull DBPDataSourceContainer container) {
            super(container, Mockito.mock(SQLDialect.class));
        }

        @NotNull
        @Override
        public Connection open(@NotNull DBRProgressMonitor monitor, @NotNull DBPConnectionConfiguration configuration) throws DBCException {
            return super.openConnection(monitor, null, configuration, "Test connection policy");
        }
    }
}
