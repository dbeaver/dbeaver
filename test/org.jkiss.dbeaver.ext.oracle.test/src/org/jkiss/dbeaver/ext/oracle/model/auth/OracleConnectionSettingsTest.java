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
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Properties;

class OracleConnectionSettingsTest extends DBeaverUnitTest {

    @Test
    void readsOldDesktopSettingsWithoutResaving() throws DBException {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS, "sysoper");
        Map<String, String> properties = new OracleDataSourceProvider().getConnectionProviderProperties(configuration);
        Assertions.assertEquals("SYSOPER", properties.get(OracleConstants.PROP_AUTH_LOGON_AS));
        Assertions.assertEquals(OracleConnectionType.SERVICE.name(), properties.get(OracleConstants.PROP_SID_SERVICE));
        Assertions.assertTrue(configuration.getProviderProperties().isEmpty());
        Assertions.assertEquals("sysoper", configuration.getAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS));
        Assertions.assertEquals("sys AS SYSOPER", getAuthenticatedUserName(configuration));

        OracleAuthModelDatabaseNative.setConnectionRole(configuration, OracleConnectionRole.SYSDBA.getTitle());

        Assertions.assertEquals("SYSDBA", configuration.getProviderProperty(OracleConstants.PROP_AUTH_LOGON_AS));
        Assertions.assertNull(configuration.getAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS));
        Assertions.assertEquals("sys AS SYSDBA", getAuthenticatedUserName(configuration));
    }

    @Test
    void normalRoleOverridesAndClearsOldRoles() throws DBException {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setProviderProperty(OracleConstants.PROP_INTERNAL_LOGON, "sysoper");
        Assertions.assertEquals("SYSOPER", OracleAuthModelDatabaseNative.getConnectionRole(configuration));
        configuration.setAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS, "sysdba");
        configuration.setProviderProperty(OracleConstants.PROP_AUTH_LOGON_AS, OracleConnectionRole.NORMAL.getTitle());

        Assertions.assertEquals("sys", getAuthenticatedUserName(configuration));
        OracleAuthModelDatabaseNative.setConnectionRole(configuration, OracleConnectionRole.NORMAL.getTitle());
        Assertions.assertNull(configuration.getAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS));
        Assertions.assertNull(configuration.getProviderProperty(OracleConstants.PROP_INTERNAL_LOGON));
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
