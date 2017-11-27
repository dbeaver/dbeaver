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

package org.jkiss.dbeaver.ext.vertica;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

/**
 * VerticaUtils
 */
public class VerticaUtils {

    private static final Log log = Log.getLog(VerticaUtils.class);

    public static String getObjectDDL(DBRProgressMonitor monitor, GenericDataSource dataSource, DBSObject sourceObject) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read Vertica object definition")) {
            String objectName = sourceObject instanceof DBPQualifiedObject ?
                    ((DBPQualifiedObject) sourceObject).getFullyQualifiedName(DBPEvaluationContext.DML) :
                    sourceObject.getName();

            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT EXPORT_OBJECTS('','" + objectName + "');")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.nextRow()) {
                        sql.append(dbResult.getString(1));
                    }
                    return sql.toString();
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @NotNull
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
}
