/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.Map;

public class FireBirdProcedure extends GenericProcedure implements DBSObjectWithScript {

    @Property(hidden = true)
    @Override
    public GenericCatalog getCatalog() {
        return super.getCatalog();
    }

    @Property(hidden = true)
    @Override
    public GenericSchema getSchema() {
        return super.getSchema();
    }

    @Property(hidden = true)
    @Override
    public GenericPackage getPackage() {
        return super.getPackage();
    }

    public FireBirdProcedure(GenericStructContainer container, String procedureName, String specificName, String description, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return super.getObjectDefinitionText(monitor, options);
    }

    @Override
    public void loadProcedureColumns(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load procedure columns")) {
            String sql;
            if (getProcedureType() == DBSProcedureType.FUNCTION && getDataSource().isServerVersionAtLeast(3, 0)) {
                sql = "SELECT\n" +
                        "COALESCE(FUNA.RDB$ARGUMENT_NAME,\n" +
                        "\t'PARAM_' || FUNA.RDB$ARGUMENT_POSITION) AS COLUMN_NAME,\n" +
                        "\tCOALESCE(FUNA.RDB$FIELD_TYPE,\n" +
                        "\tF.RDB$FIELD_TYPE) AS DATA_TYPE,\n" +
                        "\tCOALESCE(FUNA.RDB$FIELD_SUB_TYPE,\n" +
                        "\tF.RDB$FIELD_SUB_TYPE) AS FIELD_SUB_TYPE,\n" +
                        "\tCOALESCE(FUNA.RDB$FIELD_PRECISION,\n" +
                        "\tF.RDB$FIELD_PRECISION) AS \"PRECISION\",\n" +
                        "\tCOALESCE(FUNA.RDB$FIELD_SCALE,\n" +
                        "\tF.RDB$FIELD_SCALE) AS \"SCALE\",\n" +
                        "\tCOALESCE(FUNA.RDB$FIELD_LENGTH,\n" +
                        "\tF.RDB$FIELD_LENGTH) AS \"LENGTH\",\n" +
                        "\tCOALESCE(FUNA.RDB$CHARACTER_LENGTH,\n" +
                        "\tF.RDB$CHARACTER_LENGTH) AS CHAR_LEN,\n" +
                        "\tCOALESCE(FUNA.RDB$DEFAULT_SOURCE,\n" +
                        "\tF.RDB$DEFAULT_SOURCE) AS DEFAULT_VALUE,\n" +
                        "\tCOALESCE(FUNA.RDB$CHARACTER_SET_ID,\n" +
                        "\tF.RDB$CHARACTER_SET_ID) AS CHARACTER_SET_ID,\n" +
                        "\tCASE\n" +
                        "\tWHEN FUN.RDB$RETURN_ARGUMENT = FUNA.RDB$ARGUMENT_POSITION THEN 0\n" +
                        "\tELSE FUNA.RDB$ARGUMENT_POSITION\n" +
                        "\tEND AS ORDINAL_POSITION,\n" +
                        "\tCASE\n" +
                        "\tWHEN COALESCE(FUNA.RDB$NULL_FLAG,\n" +
                        "\tF.RDB$NULL_FLAG) = 1 THEN TRUE\n" +
                        "\tWHEN FUNA.RDB$MECHANISM = 0 THEN TRUE\n" +
                        "\tWHEN FUNA.RDB$MECHANISM = 1 THEN TRUE\n" +
                        "\tELSE FALSE\n" +
                        "\tEND AS NOT_NULL\n" +
                        "FROM\n" +
                        "\tRDB$FUNCTIONS FUN\n" +
                        "INNER JOIN RDB$FUNCTION_ARGUMENTS FUNA ON\n" +
                        "\tFUNA.RDB$FUNCTION_NAME = FUN.RDB$FUNCTION_NAME\n" +
                        "\tAND FUNA.RDB$PACKAGE_NAME IS NOT DISTINCT\n" +
                        "FROM\n" +
                        "\tFUN.RDB$PACKAGE_NAME\n" +
                        "LEFT JOIN RDB$FIELDS F ON\n" +
                        "\tF.RDB$FIELD_NAME = FUNA.RDB$FIELD_SOURCE\n" +
                        "WHERE\n" +
                        "\tFUN.RDB$FUNCTION_NAME=?";
            } else {
                sql = "SELECT\n" +
                        "\tCAST(PP.RDB$PARAMETER_NAME AS varchar(63)) AS COLUMN_NAME,\n" +
                        "\tPP.RDB$PARAMETER_TYPE AS COLUMN_TYPE,\n" +
                        "\tF.RDB$FIELD_TYPE AS DATA_TYPE,\n" +
                        "\tF.RDB$FIELD_SUB_TYPE AS TYPE_NAME,\n" +
                        "\tF.RDB$FIELD_PRECISION AS \"PRECISION\",\n" +
                        "\tF.RDB$FIELD_SCALE AS \"SCALE\",\n" +
                        "\tF.RDB$FIELD_LENGTH AS \"LENGTH\",\n" +
                        "\tPP.RDB$NULL_FLAG AS NOT_NULL,\n" +
                        "\tPP.RDB$DESCRIPTION AS REMARKS,\n" +
                        "\tF.RDB$CHARACTER_LENGTH AS CHAR_LEN,\n" +
                        "\tPP.RDB$PARAMETER_NUMBER + 1 AS ORDINAL_POSITION,\n" +
                        "\tF.RDB$CHARACTER_SET_ID,\n" +
                        "\tF.RDB$DEFAULT_SOURCE AS DEFAULT_VALUE\n" +
                        "FROM\n" +
                        "\tRDB$PROCEDURE_PARAMETERS PP,\n" +
                        "\tRDB$FIELDS F\n" +
                        "WHERE\n" +
                        "\tPP.RDB$FIELD_SOURCE = F.RDB$FIELD_NAME\n" +
                        "\tAND PP.RDB$PROCEDURE_NAME=?\n" +
                        "ORDER BY\n" +
                        "\tPP.RDB$PROCEDURE_NAME,\n" +
                        "\tPP.RDB$PARAMETER_TYPE DESC,\n" +
                        "\tPP.RDB$PARAMETER_NUMBER";
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    boolean isProcedure = getProcedureType() == DBSProcedureType.PROCEDURE;
                    while (dbResult.next()) {
                        String parameterName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME);
                        int dataType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
                        FireBirdFieldType fieldDT = FireBirdFieldType.getById(dataType);

                        String typeName;
                        if (fieldDT != null) {
                            typeName = fieldDT.getName();
                        } else {
                            typeName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TYPE_NAME);
                        }

                        int columnSize;
                        if ((fieldDT == FireBirdFieldType.CHAR || fieldDT == FireBirdFieldType.VARCHAR) && isProcedure) {
                            columnSize = JDBCUtils.safeGetInt(dbResult, "CHAR_LEN");
                        } else {
                            columnSize = JDBCUtils.safeGetInt(dbResult, JDBCConstants.LENGTH);
                        }

                        boolean notNull = JDBCUtils.safeGetBoolean(dbResult, "NOT_NULL");
                        int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
                        int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
                        String remarks = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.REMARKS);
                        int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);

                        String defaultValue = JDBCUtils.safeGetStringTrimmed(dbResult, "DEFAULT_VALUE");

                        DBSProcedureParameterKind parameterType;
                        if (isProcedure) {
                            int paramTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
                            switch (paramTypeNum) {
                                case 0:
                                    parameterType = DBSProcedureParameterKind.IN;
                                    break;
                                case 1:
                                    parameterType = DBSProcedureParameterKind.OUT;
                                    break;
                                default:
                                    parameterType = DBSProcedureParameterKind.UNKNOWN;
                                    break;
                            }
                        } else {
                            if (position == 0) { // Firebird functions don't have parameter type field in system table and they don't have parameter types. But they have RDB$RETURN_ARGUMENT, which sets the value 0 for position field
                                parameterType = DBSProcedureParameterKind.RETURN;
                            } else {
                                parameterType = DBSProcedureParameterKind.IN;
                            }
                        }
                        FireBirdProcedureParameter parameter = new FireBirdProcedureParameter(
                                this,
                                parameterName,
                                typeName,
                                dataType,
                                position,
                                columnSize,
                                scale,
                                precision,
                                notNull,
                                remarks,
                                parameterType,
                                defaultValue);
                        addColumn(parameter);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, getDataSource());
        }
    }

    @Override
    public void setObjectDefinitionText(String source) {
        setSource(source);
    }
}
