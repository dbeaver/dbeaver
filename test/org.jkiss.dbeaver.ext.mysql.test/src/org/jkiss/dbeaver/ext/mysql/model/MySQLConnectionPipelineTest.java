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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
        Mockito.when(driver.getDriverClassName()).thenReturn("com.example.CustomDriver");
        Mockito.when(driver.getDriverLoader(container)).thenReturn(driverLoader);
        Mockito.when(driverLoader.getDriverInstance(monitor)).thenReturn(jdbcDriver);
        Mockito.when(driverLoader.getClassLoader()).thenReturn(getClass().getClassLoader());
        Mockito.when(jdbcDriver.acceptsURL(Mockito.anyString())).thenReturn(true);
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenReturn(connection);
        connectionInfo.setHostName("localhost");
        connectionInfo.setHostPort("3306");
        connectionInfo.setDatabaseName("test");
        Mockito.when(driver.getConnectionURL(connectionInfo)).thenAnswer(invocation ->
            "jdbc:custom://" + connectionInfo.getHostName() + ":" + connectionInfo.getHostPort()
                + "/" + connectionInfo.getDatabaseName());
        dataSource = new TestDataSource(container, configurer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:mysql://localhost/test", "jdbc:mariadb://localhost/test", "jdbc:mysql+srv://localhost/test"})
    public void safePropertiesReceiveDefaultsOnlyAfterAuthentication(@NotNull String url) throws Exception {
        Mockito.when(driver.getConnectionURL(connectionInfo)).thenReturn(url);
        Mockito.doAnswer(invocation -> {
            Properties properties = invocation.getArgument(2);
            assertNoSafeDefaults(properties);
            properties.setProperty("configured", "safe-value");
            return null;
        }).when(configurer).beforeConnection(Mockito.same(monitor), Mockito.same(connectionInfo), Mockito.any(Properties.class));
        Mockito.when(authModel.initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), Mockito.any(Properties.class)
        )).thenAnswer(invocation -> {
            Properties properties = invocation.getArgument(4);
            assertNoSafeDefaults(properties);
            Assertions.assertEquals("safe-value", properties.getProperty("configured"));
            properties.setProperty("password", "authentication-password");
            return null;
        });
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenAnswer(invocation -> {
            // Inspect at consumption time, rather than after authentication cleanup could change the map.
            Properties properties = invocation.getArgument(1);
            assertSafeDefaults(properties);
            Assertions.assertEquals("safe-value", properties.getProperty("configured"));
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
        order.verify(jdbcDriver).acceptsURL(url);
        ArgumentCaptor<Properties> properties = ArgumentCaptor.forClass(Properties.class);
        order.verify(jdbcDriver).connect(Mockito.eq(url), properties.capture());
        order.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
        order.verify(configurer).afterConnection(monitor, connectionInfo, properties.getValue(), connection, null);
        Mockito.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
        Mockito.verifyNoMoreInteractions(jdbcDriver);
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:trino://localhost:8080/test", "jdbc:custom:arbitrary-template"})
    public void strictDriversReceiveOnlyTheirOwnProperties(@NotNull String url) throws Exception {
        Mockito.when(driver.getConnectionURL(connectionInfo)).thenReturn(url);
        connectionInfo.setProperty("user", "test-user");
        connectionInfo.setProperty("password", "example%password");
        Map<String, String> expected = Map.of("user", "test-user", "password", "example%password");
        Mockito.when(jdbcDriver.connect(Mockito.eq(url), Mockito.any(Properties.class))).thenAnswer(invocation -> {
            Properties properties = invocation.getArgument(1);
            // Model drivers such as Trino, which reject rather than ignore unrecognized properties.
            for (String name : properties.stringPropertyNames()) {
                if (!expected.containsKey(name)) {
                    throw new SQLException("Unrecognized connection property '" + name + "'");
                }
            }
            Assertions.assertEquals(expected, properties);
            return connection;
        });

        Assertions.assertSame(connection, openConnection());
        Assertions.assertSame(connection, openConnection(false, true));
        Assertions.assertEquals(expected, connectionInfo.getProperties());
    }

    @ParameterizedTest
    @MethodSource("prohibitedPropertyNames")
    public void authenticationPropertiesAreRejectedBeforeDriverCreation(@NotNull String name) throws Exception {
        Mockito.when(authModel.initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), Mockito.any(Properties.class)
        )).thenAnswer(invocation -> {
            Properties properties = invocation.getArgument(4);
            assertNoSafeDefaults(properties);
            properties.setProperty(name, "false");
            return null;
        });

        assertRejectedBeforeDriverCreation(name);
    }

    @ParameterizedTest
    @MethodSource("prohibitedPropertyNames")
    public void customUrlTemplateRejectsMixedCaseParametersWithoutDriverMetadata(@NotNull String name) throws Exception {
        Mockito.when(driver.getConnectionURL(connectionInfo)).thenReturn(
            "jdbc:custom:arbitrary-template;" + name.toUpperCase(Locale.ROOT) + "=false");

        assertRejectedBeforeDriverCreation(name);
    }

    @ParameterizedTest
    @MethodSource("prohibitedPropertyNames")
    public void connectionPropertyNameSubstringsAreRejected(@NotNull String name) throws Exception {
        connectionInfo.setProperty("prefix-" + name.toUpperCase(Locale.ROOT) + "-suffix", "false");

        assertRejectedBeforeDriverCreation(name);
    }

    @ParameterizedTest
    @MethodSource("prohibitedPropertyNames")
    public void connectionPropertyValueSubstringsAreRejected(@NotNull String name) throws Exception {
        connectionInfo.setProperty("ordinaryProperty", "prefix-" + name.toUpperCase(Locale.ROOT) + "-suffix");

        assertRejectedBeforeDriverCreation(name);
    }

    @Test
    public void driverConnectionDefaultsAreRejected() throws Exception {
        Mockito.when(driver.getConnectionProperties()).thenReturn(Map.of("allowLocalInfile", "false"));

        assertRejectedBeforeDriverCreation("allowLocalInfile");
    }

    @Test
    public void driverConnectionDefaultValuesAreRejected() throws Exception {
        Mockito.when(driver.getConnectionProperties()).thenReturn(Map.of("ordinaryProperty", "prefix-useConfigs-suffix"));

        assertRejectedBeforeDriverCreation("useConfigs");
    }

    @Test
    public void configurerPropertyAdditionIsRejected() throws Exception {
        Mockito.doAnswer(invocation -> {
            Properties properties = invocation.getArgument(2);
            properties.setProperty("ALLOWLOADLOCALINFILE", "false");
            return null;
        }).when(configurer).beforeConnection(Mockito.same(monitor), Mockito.same(connectionInfo), Mockito.any(Properties.class));

        assertRejectedBeforeDriverCreation("allowLoadLocalInfile");
    }

    @Test
    public void authenticationPropertyValueAdditionIsRejected() throws Exception {
        Mockito.when(authModel.initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), Mockito.any(Properties.class)
        )).thenAnswer(invocation -> {
            Properties properties = invocation.getArgument(4);
            properties.setProperty("password", "prefix-CREDENTIALTYPE-suffix");
            return null;
        });

        assertRejectedBeforeDriverCreation("credentialType");
    }

    @Test
    public void driverManagerPathIsRejectedBeforeDriverLoading() throws Exception {
        Mockito.when(driver.isInstantiable()).thenReturn(false);
        connectionInfo.setUrl("jdbc:custom:arbitrary-template;AlLoWlOcAlInFiLe=false");
        try (MockedStatic<DriverManager> driverManager = Mockito.mockStatic(
            DriverManager.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            assertRejectedBeforeDriverCreation("allowLocalInfile");
            driverManager.verifyNoInteractions();
        }
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
        assertNoDriverApiInvocation();
    }

    @Test
    public void driverConnectionFailureStillEndsAuthenticationBeforeAfterConnection() throws Exception {
        SQLException failure = new SQLException("Driver connection failed");
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenThrow(failure);

        DBCException error = Assertions.assertThrows(DBCException.class, this::openConnection);

        Assertions.assertSame(failure, error.getCause());
        InOrder order = Mockito.inOrder(jdbcDriver, authModel, configurer);
        ArgumentCaptor<Properties> properties = ArgumentCaptor.forClass(Properties.class);
        order.verify(jdbcDriver).connect(Mockito.anyString(), properties.capture());
        order.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
        order.verify(configurer).afterConnection(monitor, connectionInfo, properties.getValue(), null, failure);
        Mockito.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
    }

    @Test
    public void multiuserRejectsPropertiesWhenSanitizePreferenceIsDisabled() throws Exception {
        connectionInfo.setProperty("autoDeserialize", "false");

        assertRejectedBeforeDriverCreation("autoDeserialize", false, true);
    }

    @Test
    public void desktopWithoutSanitizePreferenceDoesNotValidate() throws Exception {
        connectionInfo.setProperty("allowLocalInfile", "true");
        Mockito.when(jdbcDriver.connect(Mockito.anyString(), Mockito.any(Properties.class))).thenAnswer(invocation -> {
            Properties properties = invocation.getArgument(1);
            Assertions.assertEquals("true", properties.getProperty("allowLocalInfile"));
            Assertions.assertFalse(properties.containsKey("autoDeserialize"));
            return connection;
        });

        Assertions.assertSame(connection, openConnection(false, false));
    }

    private void assertAuthenticationUrlRejected(
        @NotNull Consumer<DBPConnectionConfiguration> change,
        @NotNull String prohibitedProperty
    ) throws Exception {
        Mockito.when(authModel.initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), Mockito.any(Properties.class)
        )).thenAnswer(invocation -> {
            change.accept(invocation.getArgument(3));
            return null;
        });

        assertRejectedBeforeDriverCreation(prohibitedProperty);
        InOrder order = Mockito.inOrder(driver, authModel);
        order.verify(driver).getConnectionURL(connectionInfo);
        ArgumentCaptor<Properties> properties = ArgumentCaptor.forClass(Properties.class);
        order.verify(authModel).initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), properties.capture());
        order.verify(driver).getConnectionURL(connectionInfo);
        order.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
        Mockito.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
    }

    private void assertRejectedBeforeDriverCreation(@NotNull String prohibitedProperty) throws Exception {
        assertRejectedBeforeDriverCreation(prohibitedProperty, true, false);
    }

    private void assertRejectedBeforeDriverCreation(
        @NotNull String prohibitedProperty,
        boolean sanitize,
        boolean multiuser
    ) throws Exception {
        DBCException error = Assertions.assertThrows(DBCException.class, () -> openConnection(sanitize, multiuser));

        // A longer name can be rejected by its prohibited substring (e.g. allowLoadLocalInfileInPath).
        Assertions.assertTrue(prohibitedPropertyNames()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .filter(name -> prohibitedProperty.toLowerCase(Locale.ROOT).contains(name))
            .anyMatch(name -> error.getMessage().toLowerCase(Locale.ROOT).contains(name)), error.getMessage());
        ArgumentCaptor<Properties> properties = ArgumentCaptor.forClass(Properties.class);
        InOrder order = Mockito.inOrder(configurer, authModel);
        order.verify(configurer).beforeConnection(Mockito.same(monitor), Mockito.same(connectionInfo), Mockito.any(Properties.class));
        order.verify(authModel).initAuthentication(
            Mockito.same(monitor), Mockito.same(dataSource), Mockito.same(credentials),
            Mockito.same(connectionInfo), properties.capture());
        order.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
        Mockito.verify(authModel).endAuthentication(container, connectionInfo, properties.getValue());
        Mockito.verify(driver, Mockito.never()).getDriverClassName();
        Mockito.verify(driver, Mockito.never()).getDriverLoader(container);
        Mockito.verifyNoInteractions(driverLoader);
        assertNoDriverApiInvocation();
    }

    private void assertNoDriverApiInvocation() throws Exception {
        // Includes metadata methods such as getPropertyInfo, as well as acceptsURL and connect.
        Mockito.verifyNoInteractions(jdbcDriver);
        Mockito.verify(configurer).beforeConnection(Mockito.same(monitor), Mockito.same(connectionInfo), Mockito.any(Properties.class));
        Mockito.verifyNoMoreInteractions(configurer);
    }

    @NotNull
    private Connection openConnection() throws DBCException {
        return openConnection(true, false);
    }

    @NotNull
    private Connection openConnection(boolean sanitize, boolean multiuser) throws DBCException {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getPreferenceStore().getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES))
            .thenReturn(sanitize);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(multiuser);
        // Keep DBeaverUnitTest bootstrap on the real platform; scope the desktop policy to execution only.
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            return dataSource.open(monitor, connectionInfo);
        }
    }

    @NotNull
    private static Stream<String> prohibitedPropertyNames() {
        return Stream.of(
            "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile",
            "allowLoadLocalInfileInPath", "propertiesTransform", "useConfigs", "credentialType");
    }

    private static void assertNoSafeDefaults(@NotNull Properties properties) {
        prohibitedPropertyNames().forEach(name -> Assertions.assertFalse(properties.containsKey(name), name));
    }

    private static void assertSafeDefaults(@NotNull Properties properties) {
        for (String name : new String[] {"autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile"}) {
            Assertions.assertEquals("false", properties.getProperty(name), name);
        }
        Assertions.assertFalse(properties.containsKey("allowLoadLocalInfileInPath"));
        Assertions.assertFalse(properties.containsKey("ALLOWLOCALINFILE"));
    }

    private static class TestDataSource extends JDBCDataSource implements JDBCConnectionConfigurer {
        private final JDBCConnectionConfigurer configurer;

        private TestDataSource(
            @NotNull DBPDataSourceContainer container,
            @NotNull JDBCConnectionConfigurer configurer
        ) {
            super(container, Mockito.mock(SQLDialect.class));
            this.configurer = configurer;
        }

        @Override
        public void initialize(@NotNull DBRProgressMonitor monitor) {
            // The tests exercise connection opening explicitly, without remote metadata initialization.
        }

        @NotNull
        @Override
        public Collection<? extends DBSDataType> getLocalDataTypes() {
            return List.of();
        }

        @Nullable
        @Override
        public DBSDataType getLocalDataType(@Nullable String typeName) {
            return null;
        }

        @NotNull
        @Override
        public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) {
            return List.of();
        }

        @Nullable
        @Override
        public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) {
            return null;
        }

        @NotNull
        @Override
        public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
            return DBSObject.class;
        }

        @Override
        public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) {
            // No database structure is needed to exercise the connection pipeline.
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
