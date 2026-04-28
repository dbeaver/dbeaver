/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;

import java.util.Map;

public class CubridPartition extends CubridTable implements DBSTablePartition {

    private CubridTable parentTable;
    private String partitionName;
    private String expression;
    private Object[] expressionValues;
    private String comment;
    private boolean isNumeric;

    public CubridPartition(
            @NotNull DBRProgressMonitor monitor,
            @NotNull CubridTable table,
            @NotNull String className,
            @NotNull String type,
            @NotNull JDBCResultSet dbResult) {
        super(table.getContainer(), className, type, dbResult);
        this.parentTable = table;
        this.partitionName = JDBCUtils.safeGetString(dbResult, "partition_name");
        this.expression = JDBCUtils.safeGetString(dbResult, "partition_expr").replace("[", "").replace("]", "");
        this.comment = JDBCUtils.safeGetString(dbResult, "comment");
        this.expressionValues = getExpressionValues(dbResult, type);
        this.isNumeric = isNumericColumn(monitor);
    }

    public String getPartitionName() {
        return partitionName;
    }

    @Override 
    public CubridTable getParentTable() {
        return parentTable;
    }

    @Override 
    public boolean isSubPartition(){
        return false;
    }

    @Override 
    public DBSTablePartition getPartitionParent(){
        return null;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @Override
    @Property(viewable = true, order = 2)
    public GenericSchema getSchema() {
        return super.getSchema();
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 3)
    public String getTableType() {
        return super.getTableType();
    }

    @NotNull
    @Property(viewable = true, order = 5)
    public String getExpression() {
        return expression;
    }

    @NotNull
    @Property(viewable = true, order = 6)
    public String getExpressionValues() {
        StringBuilder valueBuilder = new StringBuilder();
        if (expressionValues != null) {
            for (int i = 0; i < expressionValues.length; i++) {
                if (i > 0) {
                    valueBuilder.append(", ");
                }
                Object v = expressionValues[i];
                if (isNumeric) {
                    valueBuilder.append(v);
                } else if (v != null) {
                    String strVal = String.valueOf(v);
                    if ("MAXVALUE".equalsIgnoreCase(strVal)) {
                        valueBuilder.append(strVal);
                    } else {
                        valueBuilder.append(SQLUtils.quoteString(getDataSource(), strVal));
                    }
                }
            }
        }
        return valueBuilder.toString();
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return comment;
    }

    //Hidden Properties
    @Override
    @Property(hidden = true)
    public boolean isPartitioned() {
        return super.isPartitioned();
    }

    @Override
    @Property(hidden = true)
    public CubridCollation getCollation() {
        return super.getCollation();
    }

    @Override
    @Property(hidden = true)
    public boolean isReuseOID() {
        return super.isReuseOID();
    }

    @Override
    @Property(hidden = true)
    public Integer getAutoIncrement() {
        return super.getAutoIncrement();
    }

    @Override
    @Property(hidden = true) 
    public CubridCharset getCharset() {
        return super.getCharset();
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        return getDataSource().getMetaModel().getTableDDL(monitor, getParentTable(), options);
    }

    private boolean isNumericColumn(@NotNull DBRProgressMonitor monitor) {
        try {
            CubridTableColumn column = (CubridTableColumn) getParentTable().getAttribute(monitor, getExpression());
            return column != null && column.getDataKind() == DBPDataKind.NUMERIC;
        } catch (DBException e) {
            return false;
        }
    }

    private Object[] getExpressionValues(JDBCResultSet dbResult, String type) {
        Object valuesObj = JDBCUtils.safeGetObject(dbResult, "partition_values");
        if (valuesObj instanceof Object[]) {
            Object[] valuesArray = (Object[]) valuesObj;
            if ("RANGE".equals(type) && valuesArray.length > 1) {
                return new Object[] { valuesArray[1] == null ? "MAXVALUE" : valuesArray[1] };
            } else {
                return valuesArray;
            }
        }
        return null;
    }
}
