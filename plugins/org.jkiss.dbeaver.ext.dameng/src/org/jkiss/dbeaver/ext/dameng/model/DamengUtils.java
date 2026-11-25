/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dameng.DamengConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPObjectWithLongId;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.cache.AbstractObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * @author Shengkai Bai
 */
public class DamengUtils {

    public static String getDDL(DBRProgressMonitor monitor, DBSObject object, DamengConstants.ObjectType objectType, String schema) throws DBException {

        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Load source code for " + objectType + " '" + object.getName() + "'")) {
            return getDDL(session, objectType, object.getName(), schema);
        } catch (SQLException e) {
            throw new DBException("Load source code for " + objectType + " '" + object.getName() + "' failed", e);
        }
    }

    @Nullable
    static String getTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase table,
        @NotNull String schema,
        boolean skipIndexes
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Read Dameng table DDL")) {
            String tableDDL = getDDL(session, DamengConstants.ObjectType.TABLE, table.getName(), schema);
            if (CommonUtils.isEmpty(tableDDL)) {
                return null;
            }
            StringBuilder ddl = new StringBuilder(tableDDL.length());
            DBStructUtils.addDDLLine(ddl, tableDDL);
            appendTableComments(session, table, schema, ddl);
            if (!skipIndexes) {
                appendTableIndexes(session, table.getName(), schema, ddl);
            }
            return ddl.toString().stripTrailing();
        } catch (SQLException e) {
            throw new DBException("Load source code for table '" + table.getName() + "' failed", e);
        }
    }

    @Nullable
    private static String getDDL(JDBCSession session, DamengConstants.ObjectType objectType, String name, String schema)
        throws SQLException {
        try (JDBCPreparedStatement statement = session.prepareStatement("SELECT DBMS_METADATA.GET_DDL(?,?,?)")) {
            statement.setString(1, objectType.name());
            statement.setString(2, name);
            statement.setString(3, schema);
            try (JDBCResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static void appendTableComments(JDBCSession session, GenericTableBase table, String schema, StringBuilder ddl)
        throws SQLException {
        String query = """
            SELECT NULL AS COLUMN_NAME, COMMENT$ FROM SYS.SYSTABLECOMMENTS
            WHERE SCHNAME = ? AND TVNAME = ? AND TABLE_TYPE = 'TABLE' AND COMMENT$ IS NOT NULL
            UNION ALL
            SELECT COLNAME, COMMENT$ FROM SYS.SYSCOLUMNCOMMENTS
            WHERE SCHNAME = ? AND TVNAME = ? AND TABLE_TYPE = 'TABLE' AND COMMENT$ IS NOT NULL
            ORDER BY COLUMN_NAME NULLS FIRST
            """;
        var dataSource = table.getDataSource();
        String tableName = DBUtils.getQuotedIdentifier(dataSource, schema, true, true) + "."
            + DBUtils.getQuotedIdentifier(dataSource, table.getName(), true, true);
        try (JDBCPreparedStatement statement = session.prepareStatement(query)) {
            statement.setString(1, schema);
            statement.setString(2, table.getName());
            statement.setString(3, schema);
            statement.setString(4, table.getName());
            try (JDBCResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String columnName = result.getString(1);
                    String comment = SQLUtils.quoteString(table, result.getString(2));
                    String statementPrefix = columnName == null
                        ? "COMMENT ON TABLE " + tableName
                        : "COMMENT ON COLUMN " + tableName + "."
                            + DBUtils.getQuotedIdentifier(dataSource, columnName, true, true);
                    DBStructUtils.addDDLLine(ddl, statementPrefix + " IS " + comment);
                }
            }
        }
    }

    private static void appendTableIndexes(JDBCSession session, String table, String schema, StringBuilder ddl)
        throws SQLException {
        // bit 0 marks system-generated indexes, including those backing primary and unique keys.
        String query = """
            SELECT DBMS_METADATA.GET_DDL('INDEX', idx.NAME, sch.NAME)
            FROM SYS.SYSOBJECTS tab
            JOIN SYS.SYSOBJECTS sch ON sch.ID = tab.SCHID
            JOIN SYS.SYSOBJECTS idx ON idx.PID = tab.ID AND idx.SUBTYPE$ = 'INDEX'
            JOIN SYS.SYSINDEXES info ON info.ID = idx.ID
            WHERE sch.NAME = ? AND tab.NAME = ? AND tab.SUBTYPE$ = 'UTAB'
                AND info.FLAG & 0x01 = 0
            ORDER BY idx.NAME
            """;
        try (JDBCPreparedStatement statement = session.prepareStatement(query)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (JDBCResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String indexDDL = result.getString(1);
                    if (indexDDL != null) {
                        DBStructUtils.addDDLLine(ddl, indexDDL);
                    }
                }
            }
        }
    }

    @Nullable
    public static <OWNER extends DBSObject, OBJECT extends DBPObjectWithLongId & DBSObject> OBJECT getObjectById(
            @NotNull DBRProgressMonitor monitor,
            @NotNull AbstractObjectCache<OWNER, OBJECT> cache,
            @NotNull OWNER owner,
            long objectId)
            throws DBException {
        for (OBJECT object : cache.getAllObjects(monitor, owner)) {
            if (object.getObjectId() == objectId) {
                return object;
            }
        }
        return null;
    }

}
