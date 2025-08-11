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

package org.jkiss.dbeaver.ext.iotdb.model.meta;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.Map;

public class IoTDBTableMetaModel extends GenericMetaModel {

    private static final Log log = Log.getLog(IoTDBTableMetaModel.class);

    private static final String[] allIotdbTableSQLKeywords = {
        "ALTER",
        "AND",
        "AS",
        "BETWEEN",
        "BY",
        "CASE",
        "CAST",
        "CONSTRAINT",
        "CREATE",
        "CROSS",
        "CUBE",
        "CURRENT_CATALOG",
        "CURRENT_DATE",
        "CURRENT_ROLE",
        "CURRENT_SCHEMA",
        "CURRENT_TIME",
        "CURRENT_TIMESTAMP",
        "CURRENT_USER",
        "DEALLOCATE",
        "DELETE",
        "DESCRIBE",
        "DISTINCT",
        "DROP",
        "ELSE",
        "END",
        "ESCAPE",
        "EXCEPT",
        "EXISTS",
        "EXTRACT",
        "FALSE",
        "FOR",
        "FROM",
        "FULL",
        "GROUP",
        "GROUPING",
        "HAVING",
        "IN",
        "INNER",
        "INSERT",
        "INTERSECT",
        "INTO",
        "IS",
        "JOIN",
        "JSON_ARRAY",
        "JSON_EXISTS",
        "JSON_OBJECT",
        "JSON_QUERY",
        "JSON_TABLE",
        "JSON_VALUE",
        "LEFT",
        "LIKE",
        "LISTAGG",
        "LOCALTIME",
        "LOCALTIMESTAMP",
        "NATURAL",
        "NORMALIZE",
        "NOT",
        "NULL",
        "ON",
        "OR",
        "ORDER",
        "OUTER",
        "PREPARE",
        "RECURSIVE",
        "RIGHT",
        "ROLLUP",
        "SELECT",
        "SKIP",
        "TABLE",
        "THEN",
        "TRIM",
        "TRUE",
        "UESCAPE",
        "UNION",
        "UNNEST",
        "USING",
        "VALUES",
        "WHEN",
        "WHERE",
        "WITH",
        "FILL"
    };

    private String getInsertTableName(String tb) {
        String insertTableName = tb;
        for (String keyword : allIotdbTableSQLKeywords) {
            if (tb.equalsIgnoreCase(keyword)) {
                insertTableName = "\"" + tb + "\"";
                break;
            }
        }
        return insertTableName;
    }

    private String getTTL(DBRProgressMonitor monitor,
                          GenericTableBase sourceObject,
                          String databaseName) {
        String ttl = "";

        try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) sourceObject, "Get IoTDB table details")) {
            String sql = String.format("select * from information_schema.tables where database like '%s'", databaseName);
            JDBCStatement stmt = session.createStatement();
            JDBCResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                ttl = rs.getString("ttl(ms)");
            }
        } catch (Exception e) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) sourceObject, "Get IoTDB table details")) {
                String sql = String.format("show tables details from %s", databaseName);
                JDBCStatement stmt = session.createStatement();
                JDBCResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    ttl = rs.getString("TTL(ms)");
                }
            } catch (Exception e1) {
                log.error("Error executing sql", e1);
            }
        }

        if (ttl.equals("INF")) {
            ttl = "'INF'";
        }

        return ttl;
    }

    private String getTableDDLInfoWithISPrivilege(JDBCSession session,
                                                  GenericTableBase sourceObject,
                                                  String databaseName,
                                                  String insertTableName,
                                                  String ttl) throws SQLException {
        StringBuilder toAppend = new StringBuilder(200);
        String sql = String.format(
                "select * from information_schema.columns where database like '%s' and table_name like '%s'", databaseName, insertTableName);
        JDBCStatement stmt = session.createStatement();
        JDBCResultSet rs = stmt.executeQuery(sql);
        toAppend.append("CREATE TABLE ").append(insertTableName).append(" (\n");
        while (rs.next()) {
            toAppend.append("\t").append(rs.getString("column_name")).append(" ");
            toAppend.append(rs.getString("datatype")).append(" ");
            toAppend.append(rs.getString("category"));
            String columnComment = rs.getString("comment");
            if (columnComment != null && !columnComment.isEmpty()) {
                toAppend.append(" COMMENT '").append(columnComment).append("'");
            }
            toAppend.append(",\n");
        }
        toAppend.setLength(toAppend.length() - 2);
        String tableComment = ((DBSEntity) sourceObject).getDescription();
        if (tableComment != null && !tableComment.isEmpty()) {
            toAppend.append("\n) COMMENT '").append(tableComment).append("' ");
            toAppend.append("WITH (TTL=").append(ttl).append(");");
        } else {
            toAppend.append("\n) WITH (TTL=").append(ttl).append(");");
        }

        return toAppend.toString();
    }

    private String getTableDDLInfoWithoutISPrivilege(JDBCSession session,
                                                     GenericTableBase sourceObject,
                                                     String databaseName,
                                                     String insertTableName,
                                                     String ttl) throws SQLException {
        StringBuilder toAppend = new StringBuilder(200);
        String sql = String.format("desc %s.%s details", databaseName, insertTableName);
        JDBCStatement stmt = session.createStatement();
        JDBCResultSet rs = stmt.executeQuery(sql);
        toAppend.append("CREATE TABLE ").append(insertTableName).append(" (\n");
        while (rs.next()) {
            toAppend.append("\t").append(rs.getString("ColumnName")).append(" ");
            toAppend.append(rs.getString("DataType")).append(" ");
            toAppend.append(rs.getString("Category"));
            String columnComment = rs.getString("Comment");
            if (columnComment != null && !columnComment.isEmpty()) {
                toAppend.append(" COMMENT '").append(columnComment).append("'");
            }
            toAppend.append(",\n");
        }
        toAppend.setLength(toAppend.length() - 2);
        String tableComment = ((DBSEntity) sourceObject).getDescription();
        if (tableComment != null && !tableComment.isEmpty()) {
            toAppend.append("\n) COMMENT '").append(tableComment).append("' ");
            toAppend.append("WITH (TTL=").append(ttl).append(");");
        } else {
            toAppend.append("\n) WITH (TTL=").append(ttl).append(");");
        }

        return toAppend.toString();
    }

    /**
     * Get DDL for IoTDB table.
     *
     * @param monitor to create session or to read metadata
     * @param sourceObject source object with required name and parents info
     * @param options for generated DDL
     *
     * @return "test" for temporary
     */
    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor,
                              @NotNull GenericTableBase sourceObject,
                              @NotNull Map<String, Object> options) {

        String databaseName = ((DBSEntity) sourceObject).getParentObject().getName();
        String tableName = ((DBSEntity) sourceObject).getName();
        String insertTableName = getInsertTableName(tableName);

        StringBuilder ddl = new StringBuilder(200);
        ddl.append("DROP TABLE IF EXISTS ").append(insertTableName).append(";\n\n");
        String ttl = getTTL(monitor, sourceObject, databaseName);
        String toAppend = "";

        try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) sourceObject, "Get IoTDB table column details")) {
            toAppend = getTableDDLInfoWithISPrivilege(session, sourceObject, databaseName, insertTableName, ttl);
        } catch (Exception e) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) sourceObject, "Get IoTDB table column details")) {
                toAppend = getTableDDLInfoWithoutISPrivilege(session, sourceObject, databaseName, insertTableName, ttl);
            } catch (Exception e1) {
                log.error("Error executing sql", e1);
            }
        }

        ddl.append(toAppend);
        return ddl.toString();
    }

    /**
     * Check if object names should be trimmed.
     *
     * @return true to trim extra spaces around columns, tables, objects names
     */
    @Override
    public boolean isTrimObjectNames() {
        return true;
    }
}
