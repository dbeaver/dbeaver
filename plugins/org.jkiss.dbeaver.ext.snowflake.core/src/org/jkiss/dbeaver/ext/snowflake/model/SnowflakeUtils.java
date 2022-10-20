/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

public class SnowflakeUtils {

    /**
     * This method opens a meta session and execute query with the function to return
     * DDL of the object with the full object name. Full object name is a parameter of the GET_DDL function
     * (TRUE in the end of the statement)
     *
     * @param monitor progress monitor for the meta session
     * @param object DBSObject with the fully qualified name for the DDL process reading
     * @param objectTypeName name of the object type will be used for the GET_DDL statement
     * @param description can be null, if object does not have description or GET_DDL return DDL with comment inside
     * @return DDL of the object
     * @throws DBException in case of SQL or other Exceptions
     */
    public static String getObjectDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPQualifiedObject object,
        @NotNull String objectTypeName,
        @Nullable String description
    ) throws DBException {
        StringBuilder ddl = new StringBuilder();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) object, "Read Snowflake " + objectTypeName + " DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT GET_DDL('" + objectTypeName + "', '" + object.getFullyQualifiedName(DBPEvaluationContext.DML) + "', TRUE)")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        ddl.append(JDBCUtils.safeGetString(dbResult, 1));
                    }
                }
            }
            if (!CommonUtils.isEmpty(description)) {
                ddl.append("\n\nCOMMENT ON ")
                    .append(objectTypeName)
                    .append(" ")
                    .append(object.getFullyQualifiedName(DBPEvaluationContext.DML))
                    .append(" IS ")
                    .append(SQLUtils.quoteString((DBSObject) object, description))
                    .append(";");
            }
            return ddl.toString();
        } catch (SQLException e) {
            throw new DBException("Can't read " + objectTypeName + " DDL from database", e);
        }
    }
}
