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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * GenericProcedure
 */
public class VerticaUDF extends AbstractProcedure<GenericDataSource, GenericStructContainer> implements GenericScriptObject, DBPUniqueObject, DBPRefreshableObject
{
    private static final Pattern PATTERN_COL_NAME_NUMERIC = Pattern.compile("\\$?([0-9]+)");

    private String type;
    private String returnType;
    private String argumentType;
    private String definition;
    private String volatility;
    private boolean isStrict;
    private boolean isFenced;

    //private List<GenericProcedureParameter> columns;

    public VerticaUDF(
        VerticaSchema container,
        JDBCResultSet dbResult)
    {
        super(container, true, JDBCUtils.safeGetString(dbResult, "function_name"), null);
        this.type = JDBCUtils.safeGetString(dbResult, "procedure_type");
        this.returnType = JDBCUtils.safeGetString(dbResult, "function_return_type");
        this.argumentType = JDBCUtils.safeGetString(dbResult, "function_argument_type");
        this.definition = JDBCUtils.safeGetString(dbResult, "function_definition");
        this.volatility = JDBCUtils.safeGetString(dbResult, "volatility");
        this.isStrict = JDBCUtils.safeGetBoolean(dbResult, "is_strict");
        this.isFenced = JDBCUtils.safeGetBoolean(dbResult, "is_fenced");
        setDescription(JDBCUtils.safeGetString(dbResult, "comment"));
    }

    @Property(viewable = true, order = 3, labelProvider = GenericCatalog.CatalogNameTermProvider.class)
    public GenericCatalog getCatalog()
    {
        return getContainer().getCatalog();
    }

    @Override
    public DBSProcedureType getProcedureType()
    {
        return DBSProcedureType.FUNCTION;
    }

    @Nullable
    @Override
    public Collection<GenericProcedureParameter> getParameters(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
/*
        if (columns == null) {
            loadProcedureColumns(monitor);
        }
        return columns;
*/
    }

/*
    private void loadProcedureColumns(DBRProgressMonitor monitor) throws DBException
    {
        Collection<? extends VerticaUDF> procedures = getContainer().getProcedures(monitor, getName());
        if (procedures == null || !procedures.contains(this)) {
            throw new DBException("Internal error - cannot read columns for procedure '" + getName() + "' because its not found in container");
        }
        Iterator<? extends VerticaUDF> procIter = procedures.iterator();
        VerticaUDF procedure = null;

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
*/

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @NotNull
    @Override
    public String getUniqueName()
    {
        return getName();
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return definition;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return this;
    }

}
