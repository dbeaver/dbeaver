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
package org.jkiss.dbeaver.ext.informix.model;

import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class InformixProcedure extends GenericProcedure {

    private final int procid;
    private final String paramTypes;

    public InformixProcedure(
        GenericStructContainer container,
        int procid,
        String procedureName,
        String specificName,
        String paramTypes,
        String description,
        DBSProcedureType procedureType,
        GenericFunctionResultType functionResultType
    ) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
        this.procid = procid;
        this.paramTypes = paramTypes == null ? "" : paramTypes.trim();
    }

    public int getProcid() {
        return procid;
    }

    public String getParamTypes() {
        return paramTypes;
    }

    @Override
    public void loadProcedureColumns(DBRProgressMonitor monitor) throws DBException {
        // Informix JDBC driver collapses overloaded procedures into a single row in
        // getProcedures(), and getProcedureColumns() returns the parameters of every
        // overload concatenated together with the overload signature only present in
        // the REMARKS column. Pick the rows that belong to this overload by matching
        // the signature embedded in REMARKS against paramTypes from sysprocedures.
        String expectedSignature = normalizeSignature(paramTypes);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Informix procedure columns")) {
            try (JDBCResultSet dbResult = session.getMetaData().getProcedureColumns(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : JDBCUtils.escapeWildCards(session, getSchema().getName()),
                JDBCUtils.escapeWildCards(session, getName()),
                getDataSource().getAllObjectsPattern()))
            {
                while (dbResult.next()) {
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
                    String rowSignature = extractSignature(remarks);
                    if (rowSignature == null || !rowSignature.equals(expectedSignature)) {
                        continue;
                    }
                    String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                    int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
                    int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
                    String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
                    int columnSize = JDBCUtils.safeGetInt(dbResult, JDBCConstants.LENGTH);
                    boolean notNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
                    int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
                    int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
                    int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
                    DBSProcedureParameterKind parameterKind = getParameterKindFromJdbc(columnTypeNum);
                    if (CommonUtils.isEmpty(columnName) && parameterKind == DBSProcedureParameterKind.RETURN) {
                        columnName = "RETURN";
                    }
                    GenericProcedureParameter column = new GenericProcedureParameter(
                        this,
                        columnName,
                        typeName,
                        valueType,
                        position,
                        columnSize,
                        scale,
                        precision,
                        notNull,
                        remarks,
                        parameterKind);
                    addColumn(column);
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
    }

    private static DBSProcedureParameterKind getParameterKindFromJdbc(int columnTypeNum) {
        return switch (columnTypeNum) {
            case DatabaseMetaData.procedureColumnIn -> DBSProcedureParameterKind.IN;
            case DatabaseMetaData.procedureColumnInOut -> DBSProcedureParameterKind.INOUT;
            case DatabaseMetaData.procedureColumnOut -> DBSProcedureParameterKind.OUT;
            case DatabaseMetaData.procedureColumnReturn -> DBSProcedureParameterKind.RETURN;
            case DatabaseMetaData.procedureColumnResult -> DBSProcedureParameterKind.RESULTSET;
            default -> DBSProcedureParameterKind.UNKNOWN;
        };
    }

    private static String normalizeSignature(String paramTypes) {
        if (paramTypes == null) {
            return "";
        }
        return paramTypes.replaceAll("\\s+", "").toLowerCase();
    }

    private static String extractSignature(String remarks) {
        if (remarks == null) {
            return null;
        }
        int open = remarks.lastIndexOf('(');
        int close = remarks.lastIndexOf(')');
        if (open < 0 || close < 0 || close <= open) {
            return null;
        }
        return normalizeSignature(remarks.substring(open + 1, close));
    }
}
