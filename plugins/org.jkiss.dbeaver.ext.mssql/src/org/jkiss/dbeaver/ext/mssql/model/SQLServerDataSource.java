/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;

public class SQLServerDataSource extends GenericDataSource {

    public SQLServerDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        this(monitor, container,
            new SQLServerMetaModel(
                SQLServerUtils.isDriverSqlServer(container.getDriver())
            ));
    }

    SQLServerDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, SQLServerMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new SQLServerDialect());
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

    @Override
    public DBPDataKind resolveDataKind(String typeName, int valueType) {
        if (valueType == Types.VARCHAR) {
            // Workaround for jTDS driver (#3555)
            switch (typeName) {
                case SQLServerConstants.TYPE_DATETIME:
                case SQLServerConstants.TYPE_DATETIME2:
                case SQLServerConstants.TYPE_SMALLDATETIME:
                case SQLServerConstants.TYPE_DATETIMEOFFSET:
                    return DBPDataKind.DATETIME;
            }
        }
        return super.resolveDataKind(typeName, valueType);
    }

    //////////////////////////////////////////////////////////
    // Databases

    protected boolean isShowAllSchemas() {
        return CommonUtils.toBoolean(getContainer().getConnectionConfiguration().getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS));
    }

    //////////////////////////////////////////////////////////
    // Windows authentication

    @Override
    protected String getConnectionUserName(DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserName(connectionInfo);
        }
    }

    @Override
    protected String getConnectionUserPassword(DBPConnectionConfiguration connectionInfo) {
        if (SQLServerUtils.isWindowsAuth(connectionInfo)) {
            return "";
        } else {
            return super.getConnectionUserPassword(connectionInfo);
        }
    }

}
