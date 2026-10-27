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
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
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
    private MySQLDataSource dataSource;
    private DBPPreferenceStore connectionPreferences;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        connectionPreferences = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(container.getDriver()).thenReturn(driver);
        Mockito.when(container.getPreferenceStore()).thenReturn(connectionPreferences);
        Mockito.when(driver.getConnectionProperties()).thenReturn(driverProperties);
        dataSource = new MySQLDataSource(monitor, container) {
            @Override
            protected void initializeRemoteInstance(@NotNull DBRProgressMonitor monitor) {
                // Property preparation must not open a database connection.
            }
        };
    }

    @Test
    public void sanitizationIsEnabledByDefault() {
        Assertions.assertTrue(DBWorkbench.getPlatform().getPreferenceStore()
            .getDefaultBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES));
    }

    @Test
    public void desktopSanitizesConnectionPropertiesWithoutChangingSavedSettings() {
        connectionInfo.getProperties().putAll(UNSAFE_PROPERTIES);
        connectionInfo.setProperty("connectTimeout", "1000");
        Map<String, String> originalProperties = new HashMap<>(connectionInfo.getProperties());

        Properties properties = fillProperties(false, true);

        assertSanitized(properties);
        Assertions.assertEquals("1000", properties.getProperty("connectTimeout"));
        Assertions.assertEquals(originalProperties, connectionInfo.getProperties());
    }

    @Test
    public void desktopSanitizesDriverPropertiesWithoutChangingDriverSettings() {
        driverProperties.putAll(UNSAFE_PROPERTIES);
        driverProperties.put("connectTimeout", "1000");
        Map<String, Object> originalProperties = new HashMap<>(driverProperties);

        Properties properties = fillProperties(false, true);

        assertSanitized(properties);
        Assertions.assertEquals("1000", properties.getProperty("connectTimeout"));
        Assertions.assertEquals(originalProperties, driverProperties);
    }

    @Test
    public void connectionPropertiesCannotOverrideSanitizedDriverProperties() {
        driverProperties.putAll(SAFE_PROPERTIES);
        connectionInfo.getProperties().putAll(UNSAFE_PROPERTIES);
        Mockito.when(connectionPreferences.getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES)).thenReturn(false);

        assertSanitized(fillProperties(false, true));
    }

    @Test
    public void desktopCanDisableSanitizationGlobally() {
        driverProperties.putAll(SAFE_PROPERTIES);
        connectionInfo.getProperties().putAll(UNSAFE_PROPERTIES);
        Mockito.when(connectionPreferences.getBoolean(ModelPreferences.CONNECTION_SANITIZE_DRIVER_PROPERTIES)).thenReturn(true);

        Assertions.assertEquals(UNSAFE_PROPERTIES, fillProperties(false, false));
    }

    @Test
    public void multiuserAlwaysSanitizesProperties() {
        connectionInfo.getProperties().putAll(UNSAFE_PROPERTIES);

        assertSanitized(fillProperties(true, false));
        assertSanitized(fillProperties(true, true));
    }

    @Test
    public void absentPropertiesReceiveSafeDefaults() {
        Assertions.assertEquals(SAFE_PROPERTIES, fillProperties(false, true));
        Assertions.assertTrue(connectionInfo.getProperties().isEmpty());
    }

    @Test
    public void alreadySafePropertiesArePreserved() {
        connectionInfo.getProperties().putAll(SAFE_PROPERTIES);

        Assertions.assertEquals(SAFE_PROPERTIES, fillProperties(false, true));
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
            dataSource.fillConnectionProperties(connectionInfo, properties);
        }
        return properties;
    }

    private void assertSanitized(@NotNull Properties properties) {
        SAFE_PROPERTIES.forEach((key, value) -> Assertions.assertEquals(value, properties.getProperty(key), key));
        Assertions.assertFalse(properties.containsKey("allowLoadLocalInfileInPath"));
    }
}
