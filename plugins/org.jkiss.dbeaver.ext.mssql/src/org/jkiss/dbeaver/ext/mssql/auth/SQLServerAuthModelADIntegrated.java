/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.mssql.auth;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNativeCredentials;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Properties;

/**
 * SQL Server AD integrated auth model.
 */
public class SQLServerAuthModelADIntegrated extends SQLServerAuthModelAbstract {

    public static final String ID = "sqlserver_ad_integrated";

    @Override
    public boolean isUserNameApplicable() {
        return false;
    }

    @Override
    public boolean isUserPasswordApplicable() {
        return false;
    }

    @Override
    public Object initAuthentication(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull AuthModelDatabaseNativeCredentials credentials, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connProperties) throws DBException {
        connProperties.put(SQLServerConstants.PROP_CONNECTION_AUTHENTICATION, SQLServerConstants.AUTH_ACTIVE_DIRECTORY_INTEGRATED);
        connProperties.remove(DBConstants.DATA_SOURCE_PROPERTY_USER);
        connProperties.remove(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD);
        return credentials;
    }

    @Override
    public void endAuthentication(@NotNull DBPDataSourceContainer dataSource, @NotNull DBPConnectionConfiguration configuration, @NotNull Properties connProperties) {
        super.endAuthentication(dataSource, configuration, connProperties);
    }

}
