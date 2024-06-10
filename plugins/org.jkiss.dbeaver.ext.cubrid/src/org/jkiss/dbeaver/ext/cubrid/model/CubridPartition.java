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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;

public class CubridPartition extends CubridTable implements DBSTablePartition {

    private final CubridTable parentTable;
    private final String expression;
    private final Integer[] expressionValues;
    private final String comment;
    public CubridPartition(
        @NotNull CubridTable table,
        @NotNull String name,
        @NotNull String type,
        @NotNull JDBCResultSet dbResult
    ) {
        super(table.getContainer(), name, type,dbResult);
        this.parentTable = table;
        expression = JDBCUtils.safeGetString(dbResult, "partition_expr");
        expressionValues = (Integer[]) JDBCUtils.safeGetObject(dbResult, "partition_values");
        comment = JDBCUtils.safeGetString(dbResult, "comment");
    }

    @Override 
    public DBSTable getParentTable() {
        return this.parentTable;
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
    @Property(viewable = true, order = 2)
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
    public Integer[] getExpressionValues() {
        return expressionValues;
    }
    
    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription() {
        return comment;
        
    }
    
    @NotNull
    @Override
    @Property(hidden = true)
    public CubridCollation getCollation() {
        return super.getCollation();
    }
    
    @NotNull
    @Override
    @Property(hidden = true)
    public boolean isReuseOID() {
        return super.isReuseOID();
    }
    
    @Nullable
    @Override
    @Property(hidden = true)
    public Integer getAutoIncrement() {
        return super.getAutoIncrement();
    }
    
    @NotNull
    @Override
    @Property(hidden = true) 
    public CubridCharset getCharset() {
        return super.getCharset();
    }
    
}
