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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericProcedure
 */
public class GenericProcedure extends AbstractProcedure<GenericDataSource, GenericStructContainer> implements GenericScriptObject, DBPUniqueObject, DBPRefreshableObject
{
    private static final Pattern PATTERN_COL_NAME_NUMERIC = Pattern.compile("\\$?([0-9]+)");

    private String specificName;
    private DBSProcedureType procedureType;
    private List<GenericProcedureParameter> columns;
    private String source;
    private GenericFunctionResultType functionResultType;

    public GenericProcedure(
        GenericStructContainer container,
        String procedureName,
        String specificName,
        String description,
        DBSProcedureType procedureType,
        GenericFunctionResultType functionResultType)
    {
        super(container, true, procedureName, description);
        this.procedureType = procedureType;
        this.functionResultType = functionResultType;
        this.specificName = specificName;
    }

    @Property(viewable = true, order = 3)
    public GenericCatalog getCatalog()
    {
        return getContainer().getCatalog();
    }

    @Property(viewable = true, order = 4)
    public GenericSchema getSchema()
    {
        return getContainer().getSchema();
    }

    @Property(viewable = true, order = 5)
    public GenericPackage getPackage()
    {
        return getContainer() instanceof GenericPackage ? (GenericPackage) getContainer() : null;
    }

    @Override
    @Property(viewable = true, order = 6)
    public DBSProcedureType getProcedureType()
    {
        return procedureType;
    }

    @Property(viewable = true, order = 7)
    public GenericFunctionResultType getFunctionResultType() {
        return functionResultType;
    }

    @Override
    public Collection<GenericProcedureParameter> getParameters(DBRProgressMonitor monitor)
        throws DBException
    {
        if (columns == null) {
            loadProcedureColumns(monitor);
        }
        return columns;
    }

    private void loadProcedureColumns(DBRProgressMonitor monitor) throws DBException
    {
        Collection<? extends GenericProcedure> procedures = getContainer().getProcedures(monitor, getName());
        if (procedures == null || !procedures.contains(this)) {
            throw new DBException("Internal error - cannot read columns for procedure '" + getName() + "' because its not found in container");
        }
        Iterator<? extends GenericProcedure> procIter = procedures.iterator();
        GenericProcedure procedure = null;

        final GenericMetaObject pcObject = getDataSource().getMetaObject(GenericConstants.OBJECT_PROCEDURE_COLUMN);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load procedure columns")) {
            final JDBCResultSet dbResult;
            if (functionResultType == null) {
                dbResult = session.getMetaData().getProcedureColumns(
                    getCatalog() == null ?
                        this.getPackage() == null || !this.getPackage().isNameFromCatalog() ?
                            null :
                            this.getPackage().getName() :
                        getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    getName(),
                    getDataSource().getAllObjectsPattern()
                );
            } else {
                dbResult = session.getMetaData().getFunctionColumns(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    getName(),
                    getDataSource().getAllObjectsPattern()
                );
            }
            try {
                int previousPosition = -1;
                while (dbResult.next()) {
                    String columnName = GenericUtils.safeGetString(pcObject, dbResult, JDBCConstants.COLUMN_NAME);
                    int columnTypeNum = GenericUtils.safeGetInt(pcObject, dbResult, JDBCConstants.COLUMN_TYPE);
                    int valueType = GenericUtils.safeGetInt(pcObject, dbResult, JDBCConstants.DATA_TYPE);
                    String typeName = GenericUtils.safeGetString(pcObject, dbResult, JDBCConstants.TYPE_NAME);
                    int columnSize = GenericUtils.safeGetInt(pcObject, dbResult, JDBCConstants.LENGTH);
                    boolean notNull = GenericUtils.safeGetInt(pcObject, dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
                    int scale = GenericUtils.safeGetInt(pcObject, dbResult, JDBCConstants.SCALE);
                    int precision = GenericUtils.safeGetInt(pcObject, dbResult, JDBCConstants.PRECISION);
                    //int radix = GenericUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
                    String remarks = GenericUtils.safeGetString(pcObject, dbResult, JDBCConstants.REMARKS);
                    int position = GenericUtils.safeGetInt(pcObject, dbResult, JDBCConstants.ORDINAL_POSITION);
                    DBSProcedureParameterKind parameterType;
                    if (functionResultType == null) {
                        switch (columnTypeNum) {
                            case DatabaseMetaData.procedureColumnIn:
                                parameterType = DBSProcedureParameterKind.IN;
                                break;
                            case DatabaseMetaData.procedureColumnInOut:
                                parameterType = DBSProcedureParameterKind.INOUT;
                                break;
                            case DatabaseMetaData.procedureColumnOut:
                                parameterType = DBSProcedureParameterKind.OUT;
                                break;
                            case DatabaseMetaData.procedureColumnReturn:
                                parameterType = DBSProcedureParameterKind.RETURN;
                                break;
                            case DatabaseMetaData.procedureColumnResult:
                                parameterType = DBSProcedureParameterKind.RESULTSET;
                                break;
                            default:
                                parameterType = DBSProcedureParameterKind.UNKNOWN;
                                break;
                        }
                    } else {
                        switch (columnTypeNum) {
                            case DatabaseMetaData.functionColumnIn:
                                parameterType = DBSProcedureParameterKind.IN;
                                break;
                            case DatabaseMetaData.functionColumnInOut:
                                parameterType = DBSProcedureParameterKind.INOUT;
                                break;
                            case DatabaseMetaData.functionColumnOut:
                                parameterType = DBSProcedureParameterKind.OUT;
                                break;
                            case DatabaseMetaData.functionReturn:
                                parameterType = DBSProcedureParameterKind.RETURN;
                                break;
                            case DatabaseMetaData.functionColumnResult:
                                parameterType = DBSProcedureParameterKind.RESULTSET;
                                break;
                            default:
                                parameterType = DBSProcedureParameterKind.UNKNOWN;
                                break;
                        }
                    }
                    if (CommonUtils.isEmpty(columnName) && parameterType == DBSProcedureParameterKind.RETURN) {
                        columnName = "RETURN";
                    }
                    if (position == 0) {
                        // Some drivers do not return ordinal position (PostgreSQL) but
                        // position is contained in column name
                        Matcher numberMatcher = PATTERN_COL_NAME_NUMERIC.matcher(columnName);
                        if (numberMatcher.matches()) {
                            position = Integer.parseInt(numberMatcher.group(1));
                        }
                    }

                    if (procedure == null || (previousPosition >= 0 && position <= previousPosition && procIter.hasNext())) {
                        procedure = procIter.next();
                    }
                    GenericProcedureParameter column = new GenericProcedureParameter(
                        procedure,
                        columnName,
                        typeName,
                        valueType,
                        position,
                        columnSize,
                        scale, precision, notNull,
                        remarks,
                        parameterType);

                    procedure.addColumn(column);

                    previousPosition = position;
                }
            } finally {
                dbResult.close();
            }
        } catch (SQLException e) {
            throw new DBException(e, getDataSource());
        }

    }

    private void addColumn(GenericProcedureParameter column)
    {
        if (this.columns == null) {
            this.columns = new ArrayList<>();
        }
        this.columns.add(column);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getCatalog(),
            getSchema(),
            this);
    }

    @NotNull
    @Override
    public String getUniqueName()
    {
        return CommonUtils.isEmpty(specificName) ? getName() : specificName;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (source == null) {
            source = getDataSource().getMetaModel().getProcedureDDL(monitor, this);
        }
        return source;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        source = null;
        return this;
    }
}
