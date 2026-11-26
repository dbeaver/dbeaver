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
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionRole;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;
import java.util.Properties;

/**
 * Oracle database native auth model.
 */
public class OracleAuthModelDatabaseNative extends AuthModelDatabaseNative<OracleDatabaseNativeCredentials> {

    public static final String ID = "oracle_native";

    @NotNull
    public static String getConnectionRole(@NotNull DBPConnectionConfiguration configuration) {
        String role = configuration.getProviderProperty(OracleConstants.PROP_AUTH_LOGON_AS);
        if (CommonUtils.isEmpty(role)) {
            role = configuration.getAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS);
        }
        if (CommonUtils.isEmpty(role)) {
            role = configuration.getProviderProperty(OracleConstants.PROP_INTERNAL_LOGON);
        }
        return CommonUtils.isEmpty(role) || OracleConnectionRole.NORMAL.getTitle().equalsIgnoreCase(role)
            ? OracleConnectionRole.NORMAL.getTitle() : role.toUpperCase(Locale.ENGLISH);
    }

    public static void setConnectionRole(@NotNull DBPConnectionConfiguration configuration, @NotNull String role) {
        // Keep the role with the connection settings shared by Desktop and Web, rather than with user credentials.
        configuration.setProviderProperty(OracleConstants.PROP_AUTH_LOGON_AS, role);
        configuration.setAuthProperty(OracleConstants.PROP_AUTH_LOGON_AS, null);
        configuration.removeProviderProperty(OracleConstants.PROP_INTERNAL_LOGON);
    }

    @NotNull
    @Override
    public Object initAuthentication(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull OracleDatabaseNativeCredentials credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connProperties
    ) throws DBException {
        String userName = configuration.getUserName();
        if (!CommonUtils.isEmpty(userName) && !userName.contains(" AS ")) {
            String role = getConnectionRole(configuration);
            if (!OracleConnectionRole.NORMAL.getTitle().equalsIgnoreCase(role)) {
                userName += " AS " + role;
            }
        }

        boolean setOsUser = CommonUtils.getBoolean(
            configuration.getProviderProperty(OracleConstants.PROP_SET_OS_USER),
            false
        );
        if (setOsUser) {
            String formattedUsername = userName.toUpperCase().split(" AS ")[0];
            connProperties.setProperty(OracleConstants.CONN_PROP_SESSION_OS_USER, formattedUsername);
        }

        credentials.setUserName(userName);
        return super.initAuthentication(monitor, dataSource, credentials, configuration, connProperties);
    }

    @Override
    public void endAuthentication(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connProperties) {
        super.endAuthentication(dataSource, configuration, connProperties);
    }

    @NotNull
    @Override
    public OracleDatabaseNativeCredentials createCredentials() {
        return new OracleDatabaseNativeCredentials();
    }
}
