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

    @Override
    public GenericCatalog getCatalog() {
        return super.getCatalog();
    }

    @Override
    public GenericSchema getSchema() {
        return super.getSchema();
    }

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
            boolean isProcedure = getProcedureType() == DBSProcedureType.PROCEDURE;
            if (!isProcedure && getDataSource().isServerVersionAtLeast(3, 0)) {
                sql = "SELECT\n" +
                        "COALESCE(FUNA.RDB$ARGUMENT_NAME, 'PARAM_' || FUNA.RDB$ARGUMENT_POSITION) AS COLUMN_NAME,\n" +
                        "COALESCE(FUNA.RDB$FIELD_TYPE, F.RDB$FIELD_TYPE) AS DATA_TYPE,\n" +
                        "COALESCE(FUNA.RDB$FIELD_SUB_TYPE, F.RDB$FIELD_SUB_TYPE) AS FIELD_SUB_TYPE,\n" +
                        "COALESCE(FUNA.RDB$FIELD_PRECISION, F.RDB$FIELD_PRECISION) AS \"PRECISION\",\n" +
                        "COALESCE(FUNA.RDB$FIELD_SCALE, F.RDB$FIELD_SCALE) AS \"SCALE\",\n" +
                        "COALESCE(FUNA.RDB$FIELD_LENGTH, F.RDB$FIELD_LENGTH) AS \"LENGTH\",\n" +
                        "COALESCE(FUNA.RDB$CHARACTER_LENGTH, F.RDB$CHARACTER_LENGTH) AS CHAR_LEN,\n" +
                        "COALESCE(FUNA.RDB$DEFAULT_SOURCE, F.RDB$DEFAULT_SOURCE) AS DEFAULT_VALUE,\n" +
                        "COALESCE(FUNA.RDB$CHARACTER_SET_ID, F.RDB$CHARACTER_SET_ID) AS CHARACTER_SET_ID,\n" +
                        "CASE\n" +
                        "   WHEN FUN.RDB$RETURN_ARGUMENT = FUNA.RDB$ARGUMENT_POSITION THEN 0\n" +
                        "   ELSE FUNA.RDB$ARGUMENT_POSITION\n" +
                        "END AS ORDINAL_POSITION,\n" +
                        "CASE\n" +
                        "   WHEN COALESCE(FUNA.RDB$NULL_FLAG, F.RDB$NULL_FLAG) = 1 THEN TRUE\n" +
                        "   WHEN FUNA.RDB$MECHANISM = 0 THEN TRUE\n" +
                        "   WHEN FUNA.RDB$MECHANISM = 1 THEN TRUE\n" +
                        "   ELSE FALSE\n" +
                        "END AS NOT_NULL\n" +
                        "FROM\n" +
                        "   RDB$FUNCTIONS FUN\n" +
                        "INNER JOIN RDB$FUNCTION_ARGUMENTS FUNA ON\n" +
                        "   FUNA.RDB$FUNCTION_NAME = FUN.RDB$FUNCTION_NAME\n" +
                        "   AND FUNA.RDB$PACKAGE_NAME IS NOT DISTINCT\n" +
                        "FROM\n" +
                        "   FUN.RDB$PACKAGE_NAME\n" +
                        "LEFT JOIN RDB$FIELDS F ON\n" +
                        "   F.RDB$FIELD_NAME = FUNA.RDB$FIELD_SOURCE\n" +
                        "WHERE\n" +
                        "   FUN.RDB$FUNCTION_NAME=?";
            } else {
                sql = "SELECT\n" +
                        "CAST(PP.RDB$PARAMETER_NAME AS varchar(63)) AS COLUMN_NAME,\n" +
                        "PP.RDB$PARAMETER_TYPE AS COLUMN_TYPE,\n" +
                        "F.RDB$FIELD_TYPE AS DATA_TYPE,\n" +
                        "F.RDB$FIELD_SUB_TYPE AS FIELD_SUB_TYPE,\n" +
                        "F.RDB$FIELD_PRECISION AS \"PRECISION\",\n" +
                        "F.RDB$FIELD_SCALE AS \"SCALE\",\n" +
                        "F.RDB$FIELD_LENGTH AS \"LENGTH\",\n" +
                        "PP.RDB$NULL_FLAG AS NOT_NULL,\n" +
                        "PP.RDB$DESCRIPTION AS REMARKS,\n" +
                        "F.RDB$CHARACTER_LENGTH AS CHAR_LEN,\n" +
                        "PP.RDB$PARAMETER_NUMBER + 1 AS ORDINAL_POSITION,\n" +
                        "F.RDB$CHARACTER_SET_ID,\n" +
                        "F.RDB$DEFAULT_SOURCE AS DEFAULT_VALUE\n" +
                        "FROM\n" +
                        "   RDB$PROCEDURE_PARAMETERS PP,\n" +
                        "   RDB$FIELDS F\n" +
                        "WHERE\n" +
                        "   PP.RDB$FIELD_SOURCE = F.RDB$FIELD_NAME\n" +
                        "   AND PP.RDB$PROCEDURE_NAME=?\n" +
                        "ORDER BY\n" +
                        "   PP.RDB$PARAMETER_NUMBER";
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String parameterName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME);
                        int dataType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
                        int subType = JDBCUtils.safeGetInt(dbResult, "FIELD_SUB_TYPE");
                        FireBirdFieldType fieldDT = FireBirdFieldType.getById(dataType, subType);

                        String typeName = "";
                        if (fieldDT != null) {
                            typeName = fieldDT.getName();
                        }

                        int columnSize;
                        if ((fieldDT == FireBirdFieldType.CHAR || fieldDT == FireBirdFieldType.VARCHAR) && isProcedure) {
                            columnSize = JDBCUtils.safeGetInt(dbResult, "CHAR_LEN");
                        } else {
                            columnSize = JDBCUtils.safeGetInt(dbResult, JDBCConstants.LENGTH);
                        }

                        boolean notNull = JDBCUtils.safeGetBoolean(dbResult, "NOT_NULL");
                        int scale = Math.abs(JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE)); // For some reason, FireBird returns the negative value in the scale field.
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
