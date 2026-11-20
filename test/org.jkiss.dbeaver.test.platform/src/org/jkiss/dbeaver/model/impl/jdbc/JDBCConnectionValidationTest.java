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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.mysql.model.MySQLConnectionValidator;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConstants;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Map;
import java.util.Properties;

public class JDBCConnectionValidationTest {
    private static final String URL = "jdbc:mysql://localhost/test";
    private static final String PROVIDER_PLUGIN = "test.connection.validator.provider";

    @Test
    public void rejectsBeforeDriverConnectAndFinishesAuthentication() throws Exception {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(container.getDataSource()).thenReturn(null);
        Mockito.when(container.getDriverSubstitution()).thenReturn(null);
        DBPDriver driver = container.getDriver();
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:h2:mem:test");
        DBPAuthModelDescriptor authDescriptor = Mockito.mock(DBPAuthModelDescriptor.class);
        Mockito.when(driver.getDataSourceProvider().detectConnectionAuthModel(driver, configuration)).thenReturn(authDescriptor);
        Mockito.when(authDescriptor.isApplicableTo(driver)).thenReturn(true);
        @SuppressWarnings("unchecked")
        DBAAuthModel<DBAAuthCredentials> authModel = Mockito.mock(DBAAuthModel.class);
        Mockito.when(authDescriptor.getInstance()).thenReturn(authModel);
        TestDataSource dataSource = Mockito.mock(TestDataSource.class, Mockito.withSettings()
            .useConstructor(container, Mockito.mock(SQLDialect.class)).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        Driver jdbcDriver = Mockito.mock(Driver.class);
        Mockito.doReturn(jdbcDriver).when(dataSource).createDriverInstance(monitor, driver);
        Mockito.doAnswer(invocation -> {
            DBPConnectionConfiguration actualConfiguration = invocation.getArgument(3);
            actualConfiguration.setDatabaseName("changed");
            actualConfiguration.setUrl("jdbc:h2:mem:changed");
            Properties properties = invocation.getArgument(4);
            properties.setProperty("INIT", "CALL 2");
            return null;
        }).when(authModel).initAuthentication(Mockito.eq(monitor), Mockito.eq(dataSource), Mockito.any(),
            Mockito.eq(configuration), Mockito.any());
        Mockito.doAnswer(invocation -> {
            Properties properties = invocation.getArgument(3);
            Assertions.assertEquals("CALL 2", properties.getProperty("INIT"));
            properties.setProperty("INIT", "CALL 3");
            return null;
        }).when(dataSource).fixupConnectionProperties(Mockito.eq(monitor), Mockito.eq(configuration),
            Mockito.eq("jdbc:h2:mem:changed"), Mockito.any());
        Mockito.doAnswer(invocation -> {
            Properties properties = invocation.getArgument(1);
            Assertions.assertEquals("CALL 3", properties.getProperty("INIT"));
            properties.setProperty("INIT", "CALL 4");
            return null;
        }).when(dataSource).validateConnectionProperties(Mockito.eq("jdbc:h2:mem:changed"), Mockito.any());
        Mockito.doAnswer(invocation -> {
            Assertions.assertEquals("jdbc:h2:mem:changed", invocation.getArgument(1));
            Properties properties = invocation.getArgument(2);
            Assertions.assertEquals("CALL 4", properties.getProperty("INIT"));
            throw new DBCException("Not approved");
        }).when(dataSource).validateConnectionParameters(Mockito.eq(monitor), Mockito.anyString(), Mockito.any());

        DBCException exception = Assertions.assertThrows(DBCException.class,
            () -> dataSource.openConnection(monitor, null, configuration, "test"));

        Assertions.assertEquals("Not approved", exception.getMessage());
        Mockito.verify(dataSource, Mockito.never()).createDriverInstance(Mockito.any(), Mockito.any());
        Mockito.verify(jdbcDriver, Mockito.never()).connect(Mockito.anyString(), Mockito.any());
        Mockito.verify(authModel).endAuthentication(Mockito.eq(container), Mockito.eq(configuration), Mockito.any());
    }

    @Test
    public void resolvesDeclaredValidatorFromProviderBundleInsteadOfAdapter() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture();
             var platform = Mockito.mockStatic(Platform.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            fixture.declareValidator(MySQLConnectionValidator.class);
            platform.when(() -> Platform.getBundle(PROVIDER_PLUGIN)).thenReturn(fixture.bundle);
            Properties properties = new Properties();
            properties.setProperty("allowLoadLocalInfile", "false");

            fixture.dataSource.validateConnectionProperties(URL, properties);
            properties.setProperty("allowLoadLocalInfile", "true");
            Assertions.assertThrows(DBCException.class, () -> fixture.dataSource.validateConnectionProperties(URL, properties));
            Assertions.assertThrows(DBCException.class, () -> fixture.dataSource.validateConnectionProperties(
                URL + "?autoDeserialize=true", new Properties()));

            platform.verify(() -> Platform.getBundle(PROVIDER_PLUGIN), Mockito.times(3));
            Mockito.verify(fixture.bundle, Mockito.times(3)).loadClass(MySQLConnectionValidator.class.getName());
            fixture.verifyNoFallback();
        }
    }

    @Test
    public void absentParameterUsesAdapterAndPropagatesRejection() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture()) {
            Properties properties = new Properties();
            DBCException rejection = new DBCException("Adapter rejected connection");
            Mockito.doThrow(rejection).when(fixture.fallback).validateConnectionProperties(URL, properties);

            Assertions.assertSame(rejection, Assertions.assertThrows(DBCException.class,
                () -> fixture.dataSource.validateConnectionProperties(URL, properties)));

            Mockito.verify(fixture.fallback).validateConnectionProperties(Mockito.eq(URL), Mockito.same(properties));
            Mockito.verify(fixture.driver, Mockito.never()).getProviderDescriptor();
        }
    }

    @Test
    public void absentParameterAndAdapterAllowConnection() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture();
             var adapters = Mockito.mockStatic(GeneralUtils.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            adapters.when(() -> GeneralUtils.adapt(fixture.dataSource, JDBCConnectionValidator.class)).thenReturn(null);

            Assertions.assertDoesNotThrow(() -> fixture.dataSource.validateConnectionProperties(URL, new Properties()));

            adapters.verify(() -> GeneralUtils.adapt(fixture.dataSource, JDBCConnectionValidator.class));
            Mockito.verify(fixture.driver, Mockito.never()).getProviderDescriptor();
        }
    }

    @Test
    public void missingDeclaredClassFailsClosed() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture();
             var platform = Mockito.mockStatic(Platform.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            String className = "test.MissingValidator";
            Mockito.when(fixture.driver.getDriverParameter(DBPDriverConstants.PARAM_CONNECTION_VALIDATOR)).thenReturn(className);
            Mockito.when(fixture.bundle.loadClass(className)).thenThrow(new ClassNotFoundException(className));
            platform.when(() -> Platform.getBundle(PROVIDER_PLUGIN)).thenReturn(fixture.bundle);

            DBCException exception = fixture.assertFailsClosed();

            Assertions.assertInstanceOf(ClassNotFoundException.class, exception.getCause().getCause());
        }
    }

    @Test
    public void emptyDeclaredClassDoesNotUseAdapterFallback() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture();
             var platform = Mockito.mockStatic(Platform.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            Mockito.when(fixture.driver.getDriverParameter(DBPDriverConstants.PARAM_CONNECTION_VALIDATOR)).thenReturn("");
            Mockito.when(fixture.bundle.loadClass("")).thenThrow(new ClassNotFoundException());
            platform.when(() -> Platform.getBundle(PROVIDER_PLUGIN)).thenReturn(fixture.bundle);

            fixture.assertFailsClosed();

            Mockito.verify(fixture.bundle).loadClass("");
        }
    }

    @Test
    public void wrongDeclaredTypeFailsClosed() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture();
             var platform = Mockito.mockStatic(Platform.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            fixture.declareValidator(String.class);
            platform.when(() -> Platform.getBundle(PROVIDER_PLUGIN)).thenReturn(fixture.bundle);

            Assertions.assertInstanceOf(IllegalStateException.class, fixture.assertFailsClosed().getCause());
        }
    }

    @Test
    public void missingProviderBundleFailsClosed() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture();
             var platform = Mockito.mockStatic(Platform.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            fixture.declareValidator(MySQLConnectionValidator.class);
            platform.when(() -> Platform.getBundle(PROVIDER_PLUGIN)).thenReturn(null);

            fixture.assertFailsClosed();

            platform.verify(() -> Platform.getBundle(PROVIDER_PLUGIN));
            Mockito.verifyNoInteractions(fixture.bundle);
        }
    }

    @Test
    public void nonPublicConstructorFailsClosed() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture();
             var platform = Mockito.mockStatic(Platform.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            fixture.declareValidator(PrivateConstructorValidator.class);
            platform.when(() -> Platform.getBundle(PROVIDER_PLUGIN)).thenReturn(fixture.bundle);

            Assertions.assertInstanceOf(NoSuchMethodException.class, fixture.assertFailsClosed().getCause());
        }
    }

    @Test
    public void throwingConstructorFailsClosed() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture();
             var platform = Mockito.mockStatic(Platform.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            fixture.declareValidator(ThrowingConstructorValidator.class);
            platform.when(() -> Platform.getBundle(PROVIDER_PLUGIN)).thenReturn(fixture.bundle);

            Assertions.assertInstanceOf(IllegalStateException.class, fixture.assertFailsClosed().getCause().getCause());
        }
    }

    @Test
    public void validatorMetadataIsNotIncludedInJdbcProperties() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture();
             var platform = Mockito.mockStatic(Platform.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            fixture.declareValidator(MySQLConnectionValidator.class);
            platform.when(() -> Platform.getBundle(PROVIDER_PLUGIN)).thenReturn(fixture.bundle);
            Mockito.when(fixture.driver.getDriverParameters()).thenReturn(
                Map.of(DBPDriverConstants.PARAM_CONNECTION_VALIDATOR, MySQLConnectionValidator.class.getName()));
            Mockito.when(fixture.driver.getConnectionProperties()).thenReturn(Map.of("connectTimeout", "1000"));
            DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
            configuration.getProperties().put("socketTimeout", "2000");

            Properties properties = fixture.dataSource.getAllConnectionProperties(
                new VoidProgressMonitor(), null, "test", configuration);
            fixture.dataSource.validateConnectionProperties(URL, properties);

            Assertions.assertEquals(Map.of("connectTimeout", "1000", "socketTimeout", "2000"), properties);
            Assertions.assertFalse(properties.containsKey(DBPDriverConstants.PARAM_CONNECTION_VALIDATOR));
        }
    }

    @Test
    public void disabledPolicySkipsValidatorResolution() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture()) {
            Mockito.when(fixture.platform.getPreferenceStore()
                .getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES)).thenReturn(false);
            Mockito.when(fixture.driver.getDriverParameter(DBPDriverConstants.PARAM_CONNECTION_VALIDATOR))
                .thenReturn("test.MissingValidator");

            Assertions.assertDoesNotThrow(() -> fixture.dataSource.validateConnectionProperties(URL, new Properties()));

            Mockito.verify(fixture.driver, Mockito.never()).getDriverParameter(DBPDriverConstants.PARAM_CONNECTION_VALIDATOR);
            fixture.verifyNoFallback();
        }
    }

    @Test
    public void multiuserEnforcesValidationWhenPreferenceIsDisabled() throws Exception {
        try (ValidationFixture fixture = new ValidationFixture()) {
            Mockito.when(fixture.platform.getApplication().isMultiuser()).thenReturn(true);
            Mockito.when(fixture.platform.getPreferenceStore()
                .getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES)).thenReturn(false);
            Properties properties = new Properties();
            DBCException rejection = new DBCException("Mandatory validation rejected connection");
            Mockito.doThrow(rejection).when(fixture.fallback).validateConnectionProperties(URL, properties);

            Assertions.assertSame(rejection, Assertions.assertThrows(DBCException.class,
                () -> fixture.dataSource.validateConnectionProperties(URL, properties)));
        }
    }

    private static class ValidationFixture implements AutoCloseable {
        private final DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class, Mockito.RETURNS_DEEP_STUBS);
        private final DBPDriver driver = container.getDriver();
        private final DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        private final Bundle bundle = Mockito.mock(Bundle.class);
        private final JDBCConnectionValidator fallback = Mockito.mock(JDBCConnectionValidator.class);
        private final TestDataSource dataSource;
        private final MockedStatic<DBWorkbench> workbench;

        private ValidationFixture() {
            Mockito.when(container.getDataSource()).thenReturn(null);
            dataSource = Mockito.mock(TestDataSource.class, Mockito.withSettings()
                .useConstructor(container, Mockito.mock(SQLDialect.class)).defaultAnswer(Mockito.CALLS_REAL_METHODS));
            DBPDataSourceProviderDescriptor provider = Mockito.mock(DBPDataSourceProviderDescriptor.class);
            Mockito.when(driver.getProviderDescriptor()).thenReturn(provider);
            Mockito.when(provider.getPluginId()).thenReturn(PROVIDER_PLUGIN);
            Mockito.when(platform.getPreferenceStore().getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES))
                .thenReturn(true);
            Mockito.doReturn(fallback).when(dataSource).getAdapter(JDBCConnectionValidator.class);
            workbench = Mockito.mockStatic(DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
        }

        private void declareValidator(@NotNull Class<?> validatorClass) throws ClassNotFoundException {
            Mockito.when(driver.getDriverParameter(DBPDriverConstants.PARAM_CONNECTION_VALIDATOR)).thenReturn(validatorClass.getName());
            Mockito.doReturn(validatorClass).when(bundle).loadClass(validatorClass.getName());
        }

        @NotNull
        private DBCException assertFailsClosed() {
            DBCException exception = Assertions.assertThrows(DBCException.class,
                () -> dataSource.validateConnectionProperties(URL, new Properties()));
            verifyNoFallback();
            return exception;
        }

        private void verifyNoFallback() {
            Mockito.verify(dataSource, Mockito.never()).getAdapter(JDBCConnectionValidator.class);
            Mockito.verifyNoInteractions(fallback);
        }

        @Override
        public void close() {
            workbench.close();
        }
    }

    public static class PrivateConstructorValidator implements JDBCConnectionValidator {
        private PrivateConstructorValidator() {
        }

        @Override
        public void validateConnectionProperties(@Nullable String url, @NotNull Properties properties) {
            Assertions.fail("Validator with a private constructor must not be invoked");
        }
    }

    public static class ThrowingConstructorValidator implements JDBCConnectionValidator {
        public ThrowingConstructorValidator() {
            throw new IllegalStateException("Constructor failed");
        }

        @Override
        public void validateConnectionProperties(@Nullable String url, @NotNull Properties properties) {
            Assertions.fail("Validator with a failing constructor must not be invoked");
        }
    }

    public abstract static class TestDataSource extends JDBCDataSource {
        public TestDataSource(@NotNull DBPDataSourceContainer container, @NotNull SQLDialect dialect) {
            super(container, dialect);
        }

        @NotNull
        @Override
        public Properties getAllConnectionProperties(
            @NotNull DBRProgressMonitor monitor,
            @Nullable JDBCExecutionContext context,
            @NotNull String purpose,
            @NotNull DBPConnectionConfiguration connectionInfo
        ) throws DBCException {
            return super.getAllConnectionProperties(monitor, context, purpose, connectionInfo);
        }

        @Nullable
        @Override
        public Driver createDriverInstance(@NotNull DBRProgressMonitor monitor, @NotNull DBPDriver driver) throws DBCException {
            return super.createDriverInstance(monitor, driver);
        }

        @Override
        public void fixupConnectionProperties(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPConnectionConfiguration connectionInfo,
            @NotNull String connectionUrl,
            @NotNull Properties connectionProperties
        ) throws DBException {
            super.fixupConnectionProperties(monitor, connectionInfo, connectionUrl, connectionProperties);
        }

        @Override
        public void validateConnectionProperties(@Nullable String url, @NotNull Properties connectProps) throws DBCException {
            super.validateConnectionProperties(url, connectProps);
        }

        @Override
        public void validateConnectionParameters(
            @NotNull DBRProgressMonitor monitor,
            @Nullable String url,
            @NotNull Properties connectProps
        ) throws DBCException {
            super.validateConnectionParameters(monitor, url, connectProps);
        }

        @NotNull
        @Override
        public Connection openConnection(
            @NotNull DBRProgressMonitor monitor,
            @Nullable JDBCExecutionContext context,
            @NotNull DBPConnectionConfiguration connectionInfo,
            @NotNull String purpose
        ) throws DBCException {
            return super.openConnection(monitor, context, connectionInfo, purpose);
        }
    }
}
