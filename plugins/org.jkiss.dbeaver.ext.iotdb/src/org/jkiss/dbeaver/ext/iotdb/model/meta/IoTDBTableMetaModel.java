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

    /**
     * @param monitor to create session or to read metadata
     * @param sourceObject source object with required name and parents info
     * @param options for generated DDL
     * @return "test" for temporary
     */
    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor,
                              @NotNull GenericTableBase sourceObject,
                              @NotNull Map<String, Object> options) {

        String databaseName = ((DBSEntity) sourceObject).getParentObject().getName();
        String tableName = ((DBSEntity) sourceObject).getName();
        String insertTableName = tableName;
        for (String keyword : allIotdbTableSQLKeywords) {
            if (tableName.equalsIgnoreCase(keyword)) {
                insertTableName = "\"" + tableName + "\"";
                break;
            }
        }

        StringBuilder ddl = new StringBuilder(200);
        ddl.append("DROP TABLE IF EXISTS ").append(insertTableName).append(";\n\n");
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

        try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) sourceObject, "Get IoTDB table column details")) {
            String sql = String.format("select * from information_schema.columns where database like '%s' and table_name like '%s'", databaseName, insertTableName);
            JDBCStatement stmt = session.createStatement();
            JDBCResultSet rs = stmt.executeQuery(sql);
            ddl.append("CREATE TABLE ").append(insertTableName).append(" (\n");
            while (rs.next()) {
                ddl.append("\t").append(rs.getString("column_name")).append(" ");
                ddl.append(rs.getString("datatype")).append(" ");
                ddl.append(rs.getString("category"));
                String columnComment = rs.getString("comment");
                if (columnComment != null && !columnComment.isEmpty()) {
                    ddl.append(" COMMENT '").append(columnComment).append("'");
                }
                ddl.append(",\n");
            }
            ddl.setLength(ddl.length() - 2);
            String tableComment = ((DBSEntity) sourceObject).getDescription();
            if (tableComment != null && !tableComment.isEmpty()) {
                ddl.append("\n) COMMENT '").append(tableComment).append("' ");
                ddl.append("WITH (TTL=").append(ttl).append(");");
            }
            else {
                ddl.append("\n) WITH (TTL=").append(ttl).append(");");
            }
        } catch (Exception e) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) sourceObject, "Get IoTDB table column details")) {
                String sql = String.format("desc %s.%s details", databaseName, insertTableName);
                JDBCStatement stmt = session.createStatement();
                JDBCResultSet rs = stmt.executeQuery(sql);
                ddl.append("CREATE TABLE ").append(insertTableName).append(" (\n");
                while (rs.next()) {
                    ddl.append("\t").append(rs.getString("ColumnName")).append(" ");
                    ddl.append(rs.getString("DataType")).append(" ");
                    ddl.append(rs.getString("Category"));
                    String columnComment = rs.getString("Comment");
                    if (columnComment != null && !columnComment.isEmpty()) {
                        ddl.append(" COMMENT '").append(columnComment).append("'");
                    }
                    ddl.append(",\n");
                }
                ddl.setLength(ddl.length() - 2);
                String tableComment = ((DBSEntity) sourceObject).getDescription();
                if (tableComment != null && !tableComment.isEmpty()) {
                    ddl.append("\n) COMMENT '").append(tableComment).append("' ");
                    ddl.append("WITH (TTL=").append(ttl).append(");");
                }
                else {
                    ddl.append("\n) WITH (TTL=").append(ttl).append(");");
                }
            } catch (Exception e1) {
                log.error("Error executing sql", e1);
            }
        }

        return ddl.toString();
    }

    /**
     * @return true to trim extra spaces around columns, tables, objects names
     */
    @Override
    public boolean isTrimObjectNames() {
        return true;
    }
}
