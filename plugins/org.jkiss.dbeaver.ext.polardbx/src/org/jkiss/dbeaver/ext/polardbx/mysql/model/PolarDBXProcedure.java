/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PolarDBXProcedure - extends MySQLProcedure to support PolarDB-X specific stored procedure and function handling logic.
 *
 * Core feature: overrides the getFullyQualifiedName() method to ensure the correct fully qualified name format is generated.
 */
public class PolarDBXProcedure extends MySQLProcedure {

    private static final Log log = Log.getLog(PolarDBXProcedure.class);

    public PolarDBXProcedure(MySQLCatalog catalog) {
        super(catalog);
    }

    public PolarDBXProcedure(MySQLCatalog catalog, ResultSet dbResult) {
        super(catalog, dbResult);
    }

    /**
     * Override the fully qualified name generation method.
     *
     * Problem: MySQL's implementation returns `catalog.name`, which in PolarDBX is incorrectly parsed as `mysql.mysql.name`.
     * Solution: use the catalog name and stored procedure name directly to generate the correct format `catalog.name`.
     */
    @Override
    @NotNull
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getQuotedIdentifier(getDataSource(), getContainer().getName()) + "." +
               DBUtils.getQuotedIdentifier(getDataSource(), getName());
    }

    @Override
    @NotNull
    public String getName() {
        String originalName = super.getName();

        // For function types, if the name contains a database name prefix, strip the prefix and show only the function name.
        if (getProcedureType() == DBSProcedureType.FUNCTION && originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf(".") + 1);
        }

        return originalName;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getDeclaration(DBRProgressMonitor monitor) throws DBException {
        // Get the clientBody field value from the parent class.
        String currentClientBody = super.getDeclaration();

        if (currentClientBody == null || currentClientBody.isEmpty()) {
            if (!isPersisted()) {
                // Create a default stored procedure/function template.
                String template = "CREATE " + getProcedureType().name() + " "
                    + getFullyQualifiedName(DBPEvaluationContext.DDL) + "()"
                    + GeneralUtils.getDefaultLineSeparator() +
                    (getProcedureType() == DBSProcedureType.FUNCTION ? "RETURNS INT" + GeneralUtils.getDefaultLineSeparator() : "") +
                    "BEGIN" + GeneralUtils.getDefaultLineSeparator() +
                    "END";
                super.setDeclaration(template);
                return template;
            } else {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure declaration")) {
                    // For function types, use the mysql database; for stored procedures, use the original logic.
                    String schemaName = getProcedureType() == DBSProcedureType.FUNCTION ? "mysql" : getContainer().getName();

                    // Get the plain function name, stripping any possible database name prefix.
                    String procedureName = getName();
                    if (getProcedureType() == DBSProcedureType.FUNCTION && procedureName.contains(".")) {
                        // If it is a function and the name contains a dot, take the last part as the function name.
                        procedureName = procedureName.substring(procedureName.lastIndexOf(".") + 1);
                    }

                    // For SHOW CREATE FUNCTION, do not use a database name prefix; for SHOW CREATE PROCEDURE, use the full path.
                    String showCreateTarget;
                    if (getProcedureType() == DBSProcedureType.FUNCTION) {
                        showCreateTarget = "`" + procedureName + "`";  // functions have no database name prefix
                    } else {
                        showCreateTarget = "`" + schemaName + "`.`" + procedureName + "`";  // stored procedures have a database name prefix
                    }

                    try (JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SHOW CREATE " + getProcedureType().name() + " " + showCreateTarget)) {
                        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                            if (dbResult.next()) {
                                String declaration = JDBCUtils.safeGetString(dbResult,
                                    getProcedureType() == DBSProcedureType.PROCEDURE
                                        ? "Create Procedure" : "Create Function");
                                if (declaration == null) {
                                    declaration = "";
                                }
                                super.setDeclaration(declaration);
                                return declaration;
                            } else {
                                super.setDeclaration("");
                                return "";
                            }
                        }
                    }
                } catch (SQLException e) {
                    String errorMsg = e.getMessage();
                    super.setDeclaration(errorMsg);
                    throw new DBDatabaseException(e, getDataSource());
                }
            }
        }
        return currentClientBody;
    }
}
