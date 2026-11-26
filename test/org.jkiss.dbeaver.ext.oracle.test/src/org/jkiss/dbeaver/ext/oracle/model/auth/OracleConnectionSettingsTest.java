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
package org.jkiss.dbeaver.ext.oracle.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleDataSourceProvider;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionRole;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;

class OracleConnectionSettingsTest extends DBeaverUnitTest {

    @ParameterizedTest
    @EnumSource(OracleConnectionRole.class)
    void desktopRoleIsAvailableAsWebProviderProperty(@NotNull OracleConnectionRole role) {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        OracleAuthModelDatabaseNative.setConnectionRole(configuration, role.getTitle());

        Assertions.assertEquals(role.getTitle(), configuration.getProviderProperty(OracleConstants.PROP_AUTH_LOGON_AS));
        Assertions.assertNull(configuration.getAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS));
    }

    @ParameterizedTest
    @EnumSource(OracleConnectionRole.class)
    void webRoleIsUsedByDesktopAndAuthentication(@NotNull OracleConnectionRole role) throws DBException {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setProviderProperties(Map.of(OracleConstants.PROP_AUTH_LOGON_AS, role.getTitle()));

        Assertions.assertEquals(role.getTitle(), OracleAuthModelDatabaseNative.getConnectionRole(configuration));
        String expectedUserName = role == OracleConnectionRole.NORMAL ? "sys" : "sys AS " + role.getTitle();
        Assertions.assertEquals(expectedUserName, getAuthenticatedUserName(configuration));
    }

    @Test
    void webNormalRoleOverridesOldDesktopRole() throws DBException {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS, "sysdba");
        configuration.setProviderProperty(OracleConstants.PROP_AUTH_LOGON_AS, OracleConnectionRole.NORMAL.getTitle());

        Assertions.assertEquals("sys", getAuthenticatedUserName(configuration));
    }

    @Test
    void readsPreviouslySavedDesktopRole() throws DBException {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS, "sysoper");

        Assertions.assertEquals("sysoper", OracleAuthModelDatabaseNative.getConnectionRole(configuration));
        Assertions.assertEquals("sys AS sysoper", getAuthenticatedUserName(configuration));
    }

    @Test
    void readsLegacyRole() throws DBException {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setProviderProperty(OracleConstants.PROP_INTERNAL_LOGON, "sysdba");

        Assertions.assertEquals("sys AS sysdba", getAuthenticatedUserName(configuration));
    }

    @Test
    void desktopNormalRoleClearsOldRolesAndPreservesOtherSettings() throws DBException {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS, "sysdba");
        configuration.setAuthProperty("other", "value");
        configuration.setProviderProperty(OracleConstants.PROP_INTERNAL_LOGON, "sysoper");

        OracleAuthModelDatabaseNative.setConnectionRole(configuration, OracleConnectionRole.NORMAL.getTitle());

        Assertions.assertNull(configuration.getAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS));
        Assertions.assertNull(configuration.getProviderProperty(OracleConstants.PROP_INTERNAL_LOGON));
        Assertions.assertEquals("value", configuration.getAuthProperty("other"));
        Assertions.assertEquals("sys", getAuthenticatedUserName(configuration));
    }

    @Test
    void missingRoleUsesNormalAuthentication() throws DBException {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();

        Assertions.assertEquals(OracleConnectionRole.NORMAL.getTitle(), OracleAuthModelDatabaseNative.getConnectionRole(configuration));
        Assertions.assertEquals("sys", getAuthenticatedUserName(configuration));
    }

    @Test
    void defaultServiceTypeMatchesConnectionUrl() throws Exception {
        String defaultServiceType = null;
        try (InputStream stream = OracleDataSourceProvider.class.getResourceAsStream("/plugin.xml")) {
            Assertions.assertNotNull(stream);
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            var propertyNodes = document.getElementsByTagName("property");
            for (int i = 0; i < propertyNodes.getLength(); i++) {
                var property = (Element) propertyNodes.item(i);
                if (OracleConstants.PROP_SID_SERVICE.equals(property.getAttribute("id"))) {
                    defaultServiceType = property.getAttribute("defaultValue");
                    break;
                }
            }
        }
        Assertions.assertEquals(OracleConnectionType.SERVICE.name(), defaultServiceType);
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setHostName("localhost");
        configuration.setHostPort("1521");
        configuration.setDatabaseName("ORCL");
        OracleDataSourceProvider provider = new OracleDataSourceProvider();
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        String defaultUrl = provider.getConnectionURL(driver, configuration);
        configuration.setProviderProperty(OracleConstants.PROP_SID_SERVICE, defaultServiceType);

        Assertions.assertEquals("jdbc:oracle:thin:@//localhost:1521/ORCL", defaultUrl);
        Assertions.assertEquals(defaultUrl, provider.getConnectionURL(driver, configuration));
        configuration.setProviderProperty(OracleConstants.PROP_SID_SERVICE, OracleConnectionType.SID.name());
        Assertions.assertEquals("jdbc:oracle:thin:@localhost:1521:ORCL", provider.getConnectionURL(driver, configuration));
    }

    @NotNull
    private static String getAuthenticatedUserName(@NotNull DBPConnectionConfiguration configuration) throws DBException {
        configuration.setUserName("sys");
        OracleDatabaseNativeCredentials credentials = new OracleDatabaseNativeCredentials();
        credentials.setUserName(configuration.getUserName());
        credentials.setUserPassword("password");
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getContainer()).thenReturn(Mockito.mock(DBPDataSourceContainer.class));
        Properties properties = new Properties();

        new OracleAuthModelDatabaseNative().initAuthentication(
            new VoidProgressMonitor(), dataSource, credentials, configuration, properties
        );

        return properties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_USER);
    }
}
