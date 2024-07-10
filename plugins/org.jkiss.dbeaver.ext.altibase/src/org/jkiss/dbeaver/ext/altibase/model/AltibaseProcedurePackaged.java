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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.Map;

public class AltibaseProcedurePackaged extends AltibaseProcedureBase {

    private String pkgSchema;
    private String pkgName;
    
    public AltibaseProcedurePackaged(
            GenericStructContainer container,
            String pkgSchema,
            String pkgName,
            String procedureName, 
            boolean valid,
            DBSProcedureType procedureType, 
            GenericFunctionResultType functionResultType) {
        super(container, procedureName, true, procedureType, functionResultType);
        this.pkgSchema = pkgSchema;
        this.pkgName = pkgName;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return "-- Unable to get package dependent object source";
    }

    /**
     * Add procedure columns
     */
    public void loadProcedureColumns(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load procedure columns")) {
            JDBCPreparedStatement dbStat = ((AltibaseMetaModel) getDataSource().getMetaModel())
                    .prepareProcedurePackagedColumnLoadStatement(session, pkgSchema, pkgName, this.getName());
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            dbStat.executeStatement();
            JDBCResultSet dbResult = dbStat.getResultSet();
            try {
                while (dbResult.next()) {
                    final boolean isFunction  = (JDBCUtils.safeGetInt(dbResult, "SUB_TYPE") == 1);
                    final String columnName   = JDBCUtils.safeGetString(dbResult, "PARA_NAME");
                    int position              = JDBCUtils.safeGetInt(dbResult, "PARA_ORDER");
                    final int precision       = JDBCUtils.safeGetInt(dbResult, "PRECISION");
                    final int columnSize      = precision;
                    final int scale           = JDBCUtils.safeGetInt(dbResult, "SCALE");
                    final int columnTypeNum   = JDBCUtils.safeGetInt(dbResult, "INOUT_TYPE"); // 0: IN, 1: OUT, 2: IN OUT
                    final int valueType       = JDBCUtils.safeGetInt(dbResult, "DATA_TYPE");;
                    final String typeName     = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
                    final String defaultValue = JDBCUtils.safeGetString(dbResult, "DEFAULT_VAL");
                    final boolean notNull     = (defaultValue == null);
                    final String remarks      = "";

                    DBSProcedureParameterKind parameterType;

                    switch (columnTypeNum) {
                        case AltibaseProcedureParameter.PARAM_IN:
                            parameterType = DBSProcedureParameterKind.IN;
                            break;
                        case AltibaseProcedureParameter.PARAM_INOUT:
                            parameterType = DBSProcedureParameterKind.INOUT;
                            break;
                        case AltibaseProcedureParameter.PARAM_OUT:
                            parameterType = DBSProcedureParameterKind.OUT;
                            break;
                        default:
                            parameterType = DBSProcedureParameterKind.UNKNOWN;
                            break;
                    }

                    // procedure with no argument case
                    if (!isFunction && columnName == null && position == 0) {
                        return; 
                    }

                    // function return type
                    if (isFunction && columnName == null && position == 1) {
                        continue; 
                    }

                    if (isFunction) {
                        --position;
                    }

                    AltibaseProcedureParameter column = new AltibaseProcedureParameter(
                            this,
                            columnName,
                            typeName,
                            valueType,
                            position,
                            columnSize,
                            scale, precision, notNull,
                            remarks,
                            parameterType);

                    this.addColumn(column);
                }
            } finally {
                dbResult.close();
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
    }
}
