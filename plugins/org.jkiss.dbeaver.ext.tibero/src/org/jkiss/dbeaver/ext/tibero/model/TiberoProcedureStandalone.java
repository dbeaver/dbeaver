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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureArgument;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureStandalone;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TiberoProcedure
 */
public class TiberoProcedureStandalone extends OracleProcedureStandalone {

    public TiberoProcedureStandalone(OracleSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    @Override
    @Association
    public Collection<OracleProcedureArgument> getParameters(@NotNull DBRProgressMonitor monitor) throws DBException {
        return loadParameters(monitor);
    }

    private Collection<OracleProcedureArgument> loadParameters(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleProcedureArgument> parameters = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getSchema(), "Load Tibero procedure parameters")) {
            loadParameters(session, monitor, parameters, true);
            if (parameters.isEmpty()) {
                loadParameters(session, monitor, parameters, false);
            }
        } catch (SQLException e) {
            throw new DBException("Error reading Tibero procedure parameters", e);
        }
        return parameters;
    }

    private void loadParameters(
        @NotNull JDBCSession session,
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<OracleProcedureArgument> parameters,
        boolean strict
    ) throws SQLException {
        try (JDBCPreparedStatement dbStat = prepareParametersStatement(session, strict)) {
            try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                while (resultSet.next()) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    parameters.add(new OracleProcedureArgument(monitor, this, resultSet));
                }
            }
        }
    }

    private JDBCPreparedStatement prepareParametersStatement(
        @NotNull JDBCSession session,
        boolean strict
    ) throws SQLException {
        String whereClause = strict
            ? "OWNER = ? AND OBJECT_NAME = ? AND (PACKAGE_NAME IS NULL OR PACKAGE_NAME='') "
            : "OWNER = ? AND OBJECT_NAME = ? AND DATA_LEVEL=0 ";
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT A.*\n" +
            "     , A.POSITION AS SEQUENCE \n" +
            "FROM " + OracleUtils.getSysSchemaPrefix(getDataSource()) + "ALL_ARGUMENTS A \n" +
            "WHERE " + whereClause + "\n" +
            "ORDER BY POSITION, DATA_LEVEL");
        int paramNum = 1;
        dbStat.setString(paramNum++, getSchema().getName());
        dbStat.setString(paramNum++, getName());
        return dbStat;
    }
}
