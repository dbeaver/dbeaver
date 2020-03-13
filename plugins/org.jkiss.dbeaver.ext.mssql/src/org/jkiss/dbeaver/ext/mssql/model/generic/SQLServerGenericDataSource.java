/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model.generic;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDialect;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class SQLServerGenericDataSource extends GenericDataSource {

    public SQLServerGenericDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        this(monitor, container,
            new SQLServerMetaModel(
                SQLServerUtils.isDriverSqlServer(container.getDriver())
            ));
    }

    SQLServerGenericDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, SQLServerMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new SQLServerDialect());
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Map<String, String> connectionsProps = new HashMap<>();
        if (!getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            // App name
            connectionsProps.put(
                SQLServerUtils.isDriverJtds(driver) ? SQLServerConstants.APPNAME_CLIENT_PROPERTY : SQLServerConstants.APPLICATION_NAME_CLIENT_PROPERTY,
                CommonUtils.truncateString(DBUtils.getClientApplicationName(getContainer(), context, purpose), 64));
        }
        return connectionsProps;
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        switch (featureId) {
            case DBConstants.FEATURE_LIMIT_AFFECTS_DML:
                return true;
            case DBConstants.FEATURE_MAX_STRING_LENGTH:
                return 8000;
        }
        return super.getDataSourceFeature(featureId);
    }

    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        if (valueType == Types.VARCHAR) {
            // Workaround for jTDS driver (#3555)
            switch (typeName) {
                case SQLServerConstants.TYPE_DATETIME:
                case SQLServerConstants.TYPE_DATETIME2:
                case SQLServerConstants.TYPE_SMALLDATETIME:
                    return DBPDataKind.DATETIME;
                case SQLServerConstants.TYPE_DATETIMEOFFSET:
                    return DBPDataKind.STRING;
            }
        }
        return super.resolveDataKind(typeName, valueType);
    }

    //////////////////////////////////////////////////////////
    // Databases

    //////////////////////////////////////////////////////////
    // Windows authentication

    @Override
    protected String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserName(connectionInfo);
        }
    }

    @Override
    protected String getConnectionUserPassword(@NotNull DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserPassword(connectionInfo);
        }
    }

    @Override
    protected boolean isPopulateClientAppName() {
        return false;
    }

}
