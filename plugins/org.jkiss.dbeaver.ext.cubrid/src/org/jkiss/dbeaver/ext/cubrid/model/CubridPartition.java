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
package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.Arrays;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;

public class CubridPartition extends CubridTable implements DBSTablePartition {

    private CubridTable parentTable;
    private String partitionName;
    private String expression;
    private String expressionValues;
    private String comment;

    public CubridPartition(
            @NotNull CubridTable table,
            @NotNull String className,
            @NotNull String type,
            @NotNull JDBCResultSet dbResult) {
        super(table.getContainer(), className, type, dbResult);
        this.parentTable = table;
        this.partitionName = JDBCUtils.safeGetString(dbResult, "partition_name");
        this.expression = JDBCUtils.safeGetString(dbResult, "partition_expr").replace("[", "").replace("]", "");
        this.comment = JDBCUtils.safeGetString(dbResult, "comment");
        if ("RANGE".equals(type)) {
            Object[] partitions = (Object[]) JDBCUtils.safeGetObject(dbResult, "partition_values");
            this.expressionValues = partitions[1] == null ? "MAXVALUE" : partitions[1].toString();
        } else {
            this.expressionValues = Arrays.toString((Object[]) JDBCUtils.safeGetObject(dbResult, "partition_values")).replace("[", "").replace("]", "");
        }
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
        return expressionValues;
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

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return getDataSource().getMetaModel().getTableDDL(monitor, getParentTable(), options);
    }

}
