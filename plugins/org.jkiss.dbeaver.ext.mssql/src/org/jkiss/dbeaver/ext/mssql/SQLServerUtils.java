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

package org.jkiss.dbeaver.ext.mssql;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDatabase;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * SQLServerUtils
 */
public class SQLServerUtils {

    private static final Log log = Log.getLog(SQLServerUtils.class);


    public static boolean isDriverSqlServer(DBPDriver driver) {
        return driver.getSampleURL().contains(":sqlserver");
    }

    public static boolean isDriverGeneric(DBPDriver driver) {
        return driver.getId().contains("generic");
    }

    public static boolean isDriverAzure(DBPDriver driver) {
        return driver.getId().contains("azure");
    }

    public static boolean isDriverJtds(DBPDriver driver) {
        return driver.getSampleURL().startsWith("jdbc:jtds");
    }

    public static boolean isWindowsAuth(DBPConnectionConfiguration connectionInfo) {
        return CommonUtils.toBoolean(connectionInfo.getProviderProperty(SQLServerConstants.PROP_CONNECTION_WINDOWS_AUTH)) ||
                CommonUtils.toBoolean(connectionInfo.getProperties().get(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY));
    }

    public static boolean isActiveDirectoryAuth(DBPConnectionConfiguration connectionInfo) {
        return SQLServerConstants.AUTH_ACTIVE_DIRECTORY_PASSWORD.equals(
            connectionInfo.getProperty(SQLServerConstants.PROP_CONNECTION_AUTHENTICATION));
    }

    public static void setCurrentDatabase(JDBCSession session, String schema) throws SQLException {
        JDBCUtils.executeSQL(session,
            "use " + DBUtils.getQuotedIdentifier(session.getDataSource(), schema));
    }

    public static String getCurrentDatabase(JDBCSession session) throws SQLException {
        return JDBCUtils.queryString(
            session,
            "select db_name()");
    }

    public static boolean isShowAllSchemas(DBPDataSource dataSource) {
        return CommonUtils.toBoolean(dataSource.getContainer().getConnectionConfiguration().getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS));
    }

    public static String getSystemSchemaFQN(JDBCDataSource dataSource, String catalog, String systemSchema) {
        return catalog != null && dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2005_VERSION_MAJOR ,0) ?
            DBUtils.getQuotedIdentifier(dataSource, catalog) + "." + systemSchema : systemSchema;
    }

    public static String getSystemTableName(SQLServerDatabase database, String tableName) {
        return SQLServerUtils.getSystemSchemaFQN(database.getDataSource(), database.getName(), SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA) + "." + tableName;
    }

    public static DBSForeignKeyModifyRule getForeignKeyModifyRule(int actionNum) {
        switch (actionNum) {
            case 0: return DBSForeignKeyModifyRule.NO_ACTION;
            case 1: return DBSForeignKeyModifyRule.CASCADE;
            case 2: return DBSForeignKeyModifyRule.SET_NULL;
            case 3: return DBSForeignKeyModifyRule.SET_DEFAULT;
            default:
                return DBSForeignKeyModifyRule.NO_ACTION;
        }
    }
}
