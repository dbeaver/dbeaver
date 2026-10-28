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
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLoader;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConnectionConfigurer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

public class MySQLConnectionPipelineTest extends DBeaverUnitTest {

    private final DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
    private final DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
    private final DBPDriver driver = Mockito.mock(DBPDriver.class);
    private final DBPDriverLoader driverLoader = Mockito.mock(DBPDriverLoader.class);
    private final Driver jdbcDriver = Mockito.mock(Driver.class);
    private final Connection connection = Mockito.mock(Connection.class);
    private final JDBCConnectionConfigurer configurer = Mockito.mock(JDBCConnectionConfigurer.class);
    @SuppressWarnings("unchecked")
    private final DBAAuthModel<DBAAuthCredentials> authModel = Mockito.mock(DBAAuthModel.class);
    private final DBAAuthCredentials credentials = Mockito.mock(DBAAuthCredentials.class);
    private TestDataSource dataSource;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceProvider<?> provider = Mockito.mock(DBPDataSourceProvider.class);
        DBPAuthModelDescriptor authDescriptor = Mockito.mock(DBPAuthModelDescriptor.class);
        Mockito.when(container.getDriver()).thenReturn(driver);
        Mockito.when(container.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        Mockito.when(container.getActualConnectionConfiguration()).thenReturn(connectionInfo);
        Mockito.when(driver.getConnectionProperties()).thenReturn(Map.of());
        Mockito.doReturn(provider).when(driver).getDataSourceProvider();
        Mockito.when(provider.detectConnectionAuthModel(driver, connectionInfo)).thenReturn(authDescriptor);
        Mockito.when(authDescriptor.isApplicableTo(driver)).thenReturn(true);
        Mockito.doReturn(authModel).when(authDescriptor).getInstance();
        Mockito.when(authModel.loadCredentials(container, connectionInfo)).thenReturn(credentials);
        Mockito.when(driver.isInstantiable()).thenReturn(true);
        Mockito.when(driver.getDriverClassName()).thenReturn("com.mysql.cj.jdbc.Driver");
        Mockito.when(driver.getDriverLoader(container)).thenReturn(driverLoader);
        Mockito.when(driverLoader.getDriverInstance(monitor)).thenReturn(jdbcDriver);
        Mockito.when(driverLoader.getClassLoader()).thenReturn(getClass().getClassLoader());
        Mockito.when(jdbcDriver.acceptsURL(Mockito.anyString())).thenReturn(true);
        Mockito.when(jdbcDriver.getPropertyInfo(Mockito.anyString(), Mockito.any(Properties.class))).thenReturn(new DriverPropertyInfo[0]);
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenReturn(connection);
        connectionInfo.setHostName("localhost");
        connectionInfo.setHostPort("3306");
        connectionInfo.setDatabaseName("test");
        // Avoid the optional driver-version lookup during MySQL's initial property preparation.
        connectionInfo.setProperty(MySQLConstants.PROP_ZERO_DATETIME_BEHAVIOR, "CONVERT_TO_NULL");
        Mockito.when(driver.getConnectionURL(connectionInfo)).thenAnswer(invocation ->
            "jdbc:mysql://" + connectionInfo.getHostName() + ":" + connectionInfo.getHostPort()
                + "/" + connectionInfo.getDatabaseName());
        dataSource = new TestDataSource(monitor, container, configurer);
    }

    @Test
    public void authenticationPropertiesAreSanitizedBeforeDriverConsumption() throws Exception {
        Mockito.when(authModel.initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), Mockito.any(Properties.class)
        )).thenAnswer(invocation -> {
            Properties properties = invocation.getArgument(4);
            assertSanitized(properties);
            properties.putAll(Map.of(
                "autoDeserialize", "true",
                "allowLocalInfile", "true",
                "allowLoadLocalInfile", "true",
                "allowUrlInLocalInfile", "true",
                "allowLoadLocalInfileInPath", "/test-path",
                "ALLOWLOCALINFILE", "true",
                "password", "authentication-password"
            ));
            return null;
        });
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenAnswer(invocation -> {
            // Inspect at consumption time, rather than after authentication cleanup could change the map.
            Properties properties = invocation.getArgument(1);
            assertSanitized(properties);
            Assertions.assertEquals("authentication-password", properties.getProperty("password"));
            return connection;
        });

        Assertions.assertSame(connection, openConnection());

        InOrder order = Mockito.inOrder(configurer, authModel, driverLoader, jdbcDriver);
        order.verify(configurer).beforeConnection(Mockito.same(monitor), Mockito.same(connectionInfo), Mockito.any(Properties.class));
        order.verify(authModel).initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), Mockito.any(Properties.class));
        order.verify(driverLoader).getDriverInstance(monitor);
        order.verify(jdbcDriver).getPropertyInfo(Mockito.eq("jdbc:mysql://localhost:3306/test"), Mockito.any(Properties.class));
        order.verify(jdbcDriver).acceptsURL("jdbc:mysql://localhost:3306/test");
        ArgumentCaptor<Properties> properties = ArgumentCaptor.forClass(Properties.class);
        order.verify(jdbcDriver).connect(Mockito.eq("jdbc:mysql://localhost:3306/test"), properties.capture());
        order.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
        order.verify(configurer).afterConnection(monitor, connectionInfo, properties.getValue(), connection, null);
        Mockito.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
    }

    @Test
    public void authenticationHostChangeIsRejectedBeforeConnection() throws Exception {
        assertAuthenticationUrlRejected(configuration -> configuration.setHostName(
            "address=(host=localhost)(allowLoadLocalInfile=true)"), "allowLoadLocalInfile");
    }

    @Test
    public void authenticationDatabaseChangeIsRejectedBeforeConnection() throws Exception {
        assertAuthenticationUrlRejected(configuration -> configuration.setDatabaseName(
            "test?autoDeserialize=true"), "autoDeserialize");
    }

    @Test
    public void driverCreationFailureStillEndsAuthenticationWithoutAfterConnection() throws Exception {
        DBException failure = new DBException("Driver creation failed");
        Mockito.when(driverLoader.getDriverInstance(monitor)).thenThrow(failure);

        DBCException error = Assertions.assertThrows(DBCException.class, this::openConnection);

        Assertions.assertSame(failure, error.getCause());
        InOrder order = Mockito.inOrder(authModel, driverLoader);
        order.verify(authModel).initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), Mockito.any(Properties.class));
        order.verify(driverLoader).getDriverInstance(monitor);
        order.verify(authModel).endAuthentication(
            Mockito.same(container), Mockito.same(connectionInfo), Mockito.any(Properties.class));
        assertNoConnectionAttempt();
    }

    private void assertAuthenticationUrlRejected(
        @NotNull Consumer<DBPConnectionConfiguration> change,
        @NotNull String prohibitedProperty
    ) throws Exception {
        Mockito.when(jdbcDriver.getPropertyInfo(Mockito.anyString(), Mockito.any(Properties.class)))
            .thenReturn(new DriverPropertyInfo[] {new DriverPropertyInfo(prohibitedProperty, "true")});
        Mockito.when(authModel.initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), Mockito.any(Properties.class)
        )).thenAnswer(invocation -> {
            change.accept(invocation.getArgument(3));
            return null;
        });

        DBCException error = Assertions.assertThrows(DBCException.class, this::openConnection);

        Assertions.assertTrue(error.getMessage().contains(prohibitedProperty), error.getMessage());
        InOrder order = Mockito.inOrder(driver, authModel);
        order.verify(driver).getConnectionURL(connectionInfo);
        ArgumentCaptor<Properties> properties = ArgumentCaptor.forClass(Properties.class);
        order.verify(authModel).initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), properties.capture());
        order.verify(driver).getConnectionURL(connectionInfo);
        order.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
        Mockito.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
        Mockito.verify(driverLoader).getDriverInstance(monitor);
        Mockito.verify(jdbcDriver).getPropertyInfo(Mockito.eq(
            "jdbc:mysql://" + connectionInfo.getHostName() + ":" + connectionInfo.getHostPort()
                + "/" + connectionInfo.getDatabaseName()), Mockito.any(Properties.class));
        assertNoConnectionAttempt();
    }

    private void assertNoConnectionAttempt() throws Exception {
        Mockito.verify(jdbcDriver, Mockito.never()).acceptsURL(Mockito.anyString());
        Mockito.verify(jdbcDriver, Mockito.never()).connect(Mockito.anyString(), Mockito.any(Properties.class));
        Mockito.verify(configurer).beforeConnection(Mockito.same(monitor), Mockito.same(connectionInfo), Mockito.any(Properties.class));
        Mockito.verifyNoMoreInteractions(configurer);
    }

    @NotNull
    private Connection openConnection() throws DBCException {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getPreferenceStore().getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES))
            .thenReturn(true);
        // Keep DBeaverUnitTest bootstrap on the real platform; scope the desktop policy to execution only.
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            return dataSource.open(monitor, connectionInfo);
        }
    }

    private static void assertSanitized(@NotNull Properties properties) {
        for (String name : new String[] {"autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile"}) {
            Assertions.assertEquals("false", properties.getProperty(name), name);
        }
        Assertions.assertFalse(properties.containsKey("allowLoadLocalInfileInPath"));
        Assertions.assertFalse(properties.containsKey("ALLOWLOCALINFILE"));
    }

    private static class TestDataSource extends MySQLDataSource implements JDBCConnectionConfigurer {
        private final JDBCConnectionConfigurer configurer;

        private TestDataSource(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDataSourceContainer container,
            @NotNull JDBCConnectionConfigurer configurer
        ) throws DBException {
            super(monitor, container);
            this.configurer = configurer;
        }

        @Override
        protected void initializeRemoteInstance(@NotNull DBRProgressMonitor monitor) {
            // The tests exercise connection opening explicitly, without remote metadata initialization.
        }

        @NotNull
        private Connection open(@NotNull DBRProgressMonitor monitor, @NotNull DBPConnectionConfiguration configuration) throws DBCException {
            return super.openConnection(monitor, null, configuration, "Test connection pipeline");
        }

        @Override
        public void beforeConnection(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPConnectionConfiguration configuration,
            @NotNull Properties properties
        ) throws DBCException {
            configurer.beforeConnection(monitor, configuration, properties);
        }

        @Override
        public void afterConnection(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPConnectionConfiguration configuration,
            @NotNull Properties properties,
            @Nullable Connection connection,
            @Nullable Throwable error
        ) {
            configurer.afterConnection(monitor, configuration, properties, connection, error);
        }
    }
}
