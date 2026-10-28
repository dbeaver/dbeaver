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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MySQLConnectionPropertiesTest extends DBeaverUnitTest {

    private static final Map<String, String> UNSAFE_PROPERTIES = Map.of(
        "autoDeserialize", "true",
        "allowLocalInfile", "true",
        "allowLoadLocalInfile", "true",
        "allowUrlInLocalInfile", "true",
        "allowLoadLocalInfileInPath", "/test-path"
    );
    private static final Map<String, String> SAFE_PROPERTIES = Map.of(
        "autoDeserialize", "false",
        "allowLocalInfile", "false",
        "allowLoadLocalInfile", "false",
        "allowUrlInLocalInfile", "false"
    );

    private final Map<String, Object> driverProperties = new HashMap<>();
    private final DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
    private TestDataSource dataSource;
    private DBPPreferenceStore connectionPreferences;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        connectionPreferences = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(container.getDriver()).thenReturn(driver);
        Mockito.when(container.getPreferenceStore()).thenReturn(connectionPreferences);
        Mockito.when(driver.getConnectionProperties()).thenReturn(driverProperties);
        dataSource = new TestDataSource(monitor, container);
    }

    @Test
    public void sanitizationIsEnabledByDefault() {
        Assertions.assertTrue(DBWorkbench.getPlatform().getPreferenceStore()
            .getDefaultBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES));
    }

    @Test
    public void desktopRejectsConnectionPropertiesWithoutChangingSavedSettings() {
        connectionInfo.getProperties().putAll(UNSAFE_PROPERTIES);
        connectionInfo.setProperty("connectTimeout", "1000");
        Map<String, String> originalProperties = new HashMap<>(connectionInfo.getProperties());

        Properties properties = fillProperties(false, true);

        Assertions.assertEquals(originalProperties, properties);
        assertRejectedWithoutMutation(false, true, "jdbc:mysql://localhost/test", properties);
        Assertions.assertEquals(originalProperties, connectionInfo.getProperties());
    }

    @Test
    public void desktopRejectsDriverPropertiesWithoutChangingDriverSettings() {
        driverProperties.putAll(UNSAFE_PROPERTIES);
        driverProperties.put("connectTimeout", "1000");
        Map<String, Object> originalProperties = new HashMap<>(driverProperties);

        Properties properties = fillProperties(false, true);

        Assertions.assertEquals(originalProperties, properties);
        assertRejectedWithoutMutation(false, true, "jdbc:mysql://localhost/test", properties);
        Assertions.assertEquals(originalProperties, driverProperties);
    }

    @Test
    public void connectionPreferenceCannotDisableGlobalValidation() {
        driverProperties.putAll(SAFE_PROPERTIES);
        connectionInfo.getProperties().putAll(UNSAFE_PROPERTIES);
        Mockito.when(connectionPreferences.getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES)).thenReturn(false);

        Properties properties = fillProperties(false, true);

        Assertions.assertEquals(UNSAFE_PROPERTIES, properties);
        assertRejectedWithoutMutation(false, true, "jdbc:mysql://localhost/test", properties);
        Assertions.assertEquals(SAFE_PROPERTIES, driverProperties);
        Assertions.assertEquals(UNSAFE_PROPERTIES, connectionInfo.getProperties());
    }

    @Test
    public void desktopCanDisableValidationGlobally() throws DBCException {
        driverProperties.putAll(SAFE_PROPERTIES);
        connectionInfo.getProperties().putAll(UNSAFE_PROPERTIES);
        Mockito.when(connectionPreferences.getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES)).thenReturn(true);

        Properties properties = fillProperties(false, false);

        Assertions.assertEquals(UNSAFE_PROPERTIES, properties);
        validateProperties(false, false, "jdbc:mysql://localhost/test?autoDeserialize=true", properties);
        Assertions.assertEquals(UNSAFE_PROPERTIES, properties);
        Assertions.assertEquals(SAFE_PROPERTIES, driverProperties);
        Assertions.assertEquals(UNSAFE_PROPERTIES, connectionInfo.getProperties());
    }

    @Test
    public void multiuserAlwaysRejectsProhibitedProperties() {
        connectionInfo.getProperties().putAll(UNSAFE_PROPERTIES);

        Properties properties = fillProperties(true, false);

        Assertions.assertEquals(UNSAFE_PROPERTIES, properties);
        assertRejectedWithoutMutation(true, false, "jdbc:mysql://localhost/test", properties);
        assertRejectedWithoutMutation(true, true, "jdbc:mysql://localhost/test", properties);
        Assertions.assertEquals(UNSAFE_PROPERTIES, connectionInfo.getProperties());
    }

    @Test
    public void absentPropertiesReceiveSafeDefaultsOnlyAfterValidation() throws DBCException {
        connectionInfo.setProperty("connectTimeout", "1000");
        Properties properties = fillProperties(false, true);

        Assertions.assertEquals(Map.of("connectTimeout", "1000"), properties);
        validateProperties(false, true, "jdbc:mysql://localhost/test", properties);

        Map<String, String> expected = new HashMap<>(SAFE_PROPERTIES);
        expected.put("connectTimeout", "1000");
        Assertions.assertEquals(expected, properties);
        Assertions.assertEquals(Map.of("connectTimeout", "1000"), connectionInfo.getProperties());
        Assertions.assertTrue(driverProperties.isEmpty());
    }

    @Test
    public void desktopOptOutDoesNotAddDefaults() throws DBCException {
        Properties properties = fillProperties(false, false);

        Assertions.assertTrue(properties.isEmpty());
        validateProperties(false, false, "jdbc:mysql://localhost/test", properties);

        Assertions.assertTrue(properties.isEmpty());
        Assertions.assertTrue(connectionInfo.getProperties().isEmpty());
    }

    @Test
    public void explicitlyFalseProhibitedPropertiesAreRejected() {
        connectionInfo.getProperties().putAll(SAFE_PROPERTIES);

        Properties properties = fillProperties(false, true);

        Assertions.assertEquals(SAFE_PROPERTIES, properties);
        assertRejectedWithoutMutation(false, true, "jdbc:mysql://localhost/test", properties);
        Assertions.assertEquals(SAFE_PROPERTIES, connectionInfo.getProperties());
    }

    @NotNull
    private Properties fillProperties(boolean multiuser, boolean sanitize) {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(multiuser);
        Mockito.when(platform.getPreferenceStore().getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES))
            .thenReturn(sanitize);
        Properties properties = new Properties();
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            dataSource.fillProperties(connectionInfo, properties);
        }
        return properties;
    }

    @Test
    public void finalValidationRejectsPropertiesAddedAfterPreparation() {
        Properties properties = fillProperties(false, true);
        Assertions.assertTrue(properties.isEmpty());
        properties.setProperty("ALLOWLOCALINFILE", "false");

        assertRejectedWithoutMutation(false, true, "jdbc:mysql://localhost/test", properties);
    }

    @Test
    public void desktopOptOutAlsoDisablesFinalValidation() throws DBCException {
        Properties properties = new Properties();
        properties.putAll(UNSAFE_PROPERTIES);
        properties.setProperty("prefix%41LLOWLOCALINFILEsuffix", "false");
        properties.setProperty("custom", "prefix%75seConfigsSuffix");
        Properties originalProperties = new Properties();
        originalProperties.putAll(properties);

        validateProperties(false, false, "jdbc:custom:prefix%61utoDeserializeSuffix=false", properties);

        Assertions.assertEquals(originalProperties, properties);
    }

    @Test
    public void finalValidationRejectsDecodedPropertyNameSubstrings() {
        connectionInfo.setProperty("prefix%41LLOWLOCALINFILEsuffix", "false");
        Properties properties = fillProperties(false, true);

        Assertions.assertEquals(connectionInfo.getProperties(), properties);
        assertRejectedWithoutMutation(false, true, "jdbc:mysql://localhost/test", properties);
    }

    @Test
    public void finalValidationRejectsDecodedPropertyValueSubstrings() {
        connectionInfo.setProperty("custom", "prefix%75seConfigsSuffix");
        Properties properties = fillProperties(false, true);

        Assertions.assertEquals(connectionInfo.getProperties(), properties);
        assertRejectedWithoutMutation(false, true, "jdbc:mysql://localhost/test", properties);
    }

    @Test
    public void finalValidationRejectsUnsafeUrlsWhenEnabledOrMultiuser() {
        Properties properties = fillProperties(false, true);
        for (String url : new String[] {
            "jdbc:mysql://localhost/test?allowLocalInfile=false",
            "jdbc:mariadb://localhost/test?allowLocalInfile=false",
            "jdbc:custom:prefix%41LLOWLOCALINFILEsuffix=false"
        }) {
            assertRejectedWithoutMutation(false, true, url, properties);
            assertRejectedWithoutMutation(true, false, url, properties);
            assertRejectedWithoutMutation(true, true, url, properties);
        }
    }

    private void validateProperties(
        boolean multiuser,
        boolean sanitize,
        @NotNull String url,
        @NotNull Properties properties
    ) throws DBCException {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(multiuser);
        Mockito.when(platform.getPreferenceStore().getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES))
            .thenReturn(sanitize);
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            dataSource.validateProperties(url, properties);
        }
    }

    private void assertRejectedWithoutMutation(
        boolean multiuser,
        boolean sanitize,
        @NotNull String url,
        @NotNull Properties properties
    ) {
        Properties originalProperties = new Properties();
        originalProperties.putAll(properties);

        Assertions.assertThrows(DBCException.class, () -> validateProperties(multiuser, sanitize, url, properties));
        Assertions.assertEquals(originalProperties, properties);
    }

    private static class TestDataSource extends MySQLDataSource {
        TestDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container) throws DBException {
            super(monitor, container);
        }

        @Override
        protected void initializeRemoteInstance(@NotNull DBRProgressMonitor monitor) {
            // Property preparation must not open a database connection.
        }

        void fillProperties(@NotNull DBPConnectionConfiguration configuration, @NotNull Properties properties) {
            super.fillConnectionProperties(configuration, properties);
        }

        void validateProperties(@NotNull String url, @NotNull Properties properties) throws DBCException {
            super.validateConnectionProperties(url, properties);
        }
    }
}
