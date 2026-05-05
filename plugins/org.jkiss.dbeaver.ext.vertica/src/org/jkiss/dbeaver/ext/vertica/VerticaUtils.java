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

package org.jkiss.dbeaver.ext.vertica;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.vertica.model.VerticaDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

/**
 * VerticaUtils
 */
public class VerticaUtils {

    private static final Log log = Log.getLog(VerticaUtils.class);

    public static String getObjectDDL(DBRProgressMonitor monitor, GenericDataSource dataSource, DBSObject sourceObject) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Vertica object definition")) {
            String objectName = sourceObject instanceof DBPQualifiedObject ?
                    ((DBPQualifiedObject) sourceObject).getFullyQualifiedName(DBPEvaluationContext.DML) :
                    sourceObject.getName();

            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT EXPORT_OBJECTS('','" + objectName + "');")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.nextRow()) {
                        sql.append(JDBCUtils.safeGetStringTrimmed(dbResult, 1));
                    }
                    return sql.toString();
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    public static int resolveValueType(@NotNull String typeName)
    {
        int divPos = typeName.indexOf('(');
        if (divPos != -1) {
            typeName = typeName.substring(0, divPos);
        }
        typeName = typeName.trim().toLowerCase(Locale.ENGLISH);
        switch (typeName) {
            case "binary":
            case "varbinary":
            case "long varbinary":
            case "bytea":
            case "raw":
                return Types.BINARY;

            case "boolean":
                return Types.BOOLEAN;

            case "char":
                return Types.CHAR;
            case "varchar":
                return Types.VARCHAR;
            case "long varchar":
                return Types.LONGVARCHAR;

            case "date":
                return Types.DATE;
            case "datetime":
            case "smalldatetime":
                return Types.TIMESTAMP;
            case "time":
            case "time with timezone":
            case "timetz":
                return Types.TIME;
            case "timestamp": case "timestamptz":
            case "timestamp with timezone":
                return Types.TIMESTAMP;
            case "interval":
            case "interval day":
                return Types.TIMESTAMP;

            case "double precision":
            case "float":
            case "float8":
            case "real":
                return Types.DOUBLE;

            case "integer":
            case "int":
            case "bigint":
            case "int8":
            case "smallint":
            case "tinyint":
            case "decimal":
            case "numeric":
            case "number":
            case "money":
                return Types.BIGINT;

            default:
                return Types.OTHER;
        }
    }

/*
    public static String getObjectComment(DBRProgressMonitor monitor, DBPDataSource dataSource, VerticaObjectType objectType, String schema, String object)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load Vertica comment")) {
            return JDBCUtils.queryString(
                session,
                "select comment from v_catalog.comments c\n" +
                    "where c.object_schema = ? and c.object_name = ? AND c.object_type = ?", schema, object, objectType.name());
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }
*/

    public static void readTableAndColumnsDescriptions(@NotNull DBRProgressMonitor monitor, @NotNull GenericDataSource dataSource, @NotNull GenericTableBase table, boolean isView) {
        Boolean childColumnAvailable = dataSource instanceof VerticaDataSource && ((VerticaDataSource) dataSource).isChildCommentColumnAvailable(monitor);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read table description")) {
            try (JDBCPreparedStatement stat = session.prepareStatement("select object_type, \"comment\"" +
                (childColumnAvailable != null && childColumnAvailable ? ", child_object" : "") +
                "  from v_catalog.comments where object_schema =? and object_name =?")) {
                stat.setString(1, table.getSchema().getName());
                stat.setString(2, table.getName());
                try (JDBCResultSet resultSet = stat.executeQuery()) {
                    while (resultSet.next()) {
                        String objectType = JDBCUtils.safeGetString(resultSet, 1);
                        String comment = JDBCUtils.safeGetString(resultSet, 2);
                        if ("TABLE".equals(objectType) || (isView && "VIEW".equals(objectType))) {
                            table.setDescription(comment);
                            if (isView) {
                                // View Column do not have columns comments in Vertica
                                break;
                            }
                        } else if (childColumnAvailable && "COLUMN".equals(objectType)) {
                            String columnName = JDBCUtils.safeGetString(resultSet, 3);
                            if (CommonUtils.isNotEmpty(columnName)) {
                                GenericTableColumn column = table.getAttribute(monitor, columnName);
                                if (column != null) {
                                    column.setDescription(comment);
                                } else {
                                    log.warn("Column '" + columnName + "' not found in table '" + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error reading table description ", e);
        }
    }

}
