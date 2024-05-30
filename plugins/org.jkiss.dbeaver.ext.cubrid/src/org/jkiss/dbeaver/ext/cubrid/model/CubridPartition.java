package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;

public class CubridPartition extends CubridTable implements DBSTablePartition{

    private CubridTable parentTable;
    private String expression;
    private Integer[] expression_values;
    private String comment;
    public CubridPartition(
        @NotNull CubridTable table,
        @NotNull String name,
        @NotNull String type,
        @NotNull JDBCResultSet dbResult
    ) {
        super(table.getContainer(), name, type,dbResult);
        this.parentTable = table;
        expression = JDBCUtils.safeGetString(dbResult, "partition_expr");
        expression_values = (Integer[]) JDBCUtils.safeGetObject(dbResult, "partition_values");
        comment = JDBCUtils.safeGetString(dbResult, "comment");
        this.setDescription(comment);
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
        return expression_values;
    }
    
    
    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription() {
        return comment;
        
    }
    
}
