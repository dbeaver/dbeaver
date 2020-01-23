/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.sqlite;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteObjectType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * SQLiteUtils
 */
public class SQLiteUtils {

    private static final Log log = Log.getLog(SQLiteUtils.class);


    public static String readMasterDefinition(DBRProgressMonitor monitor, DBSObject sourceObject, SQLiteObjectType objectType, String sourceObjectName, GenericTableBase table) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Load SQLite description")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT sql FROM sqlite_master WHERE type=? AND tbl_name=?" + (sourceObjectName != null ? " AND name=?" : "") + "\n" +
                    "UNION ALL\n" +
                    "SELECT sql FROM sqlite_temp_master WHERE type=? AND tbl_name=?" + (sourceObjectName != null ? " AND name=?" : "") + "\n"))
            {
                int paramIndex = 1;
                dbStat.setString(paramIndex++, objectType.name());
                dbStat.setString(paramIndex++, table.getName());
                if (sourceObjectName != null) {
                    dbStat.setString(paramIndex++, sourceObjectName);
                }
                dbStat.setString(paramIndex++, objectType.name());
                dbStat.setString(paramIndex++, table.getName());
                if (sourceObjectName != null) {
                    dbStat.setString(paramIndex++, sourceObjectName);
                }
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (resultSet.next()) {
                        String ddl = resultSet.getString(1);
                        if (ddl != null) {
                            sql.append(ddl);
                            sql.append(";\n");
                        }
                    }
                    return sql.toString();
                }
            }
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }
}
