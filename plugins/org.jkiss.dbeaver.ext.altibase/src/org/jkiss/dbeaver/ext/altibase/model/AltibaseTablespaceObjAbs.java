package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public abstract class AltibaseTablespaceObjAbs extends AltibaseObject<AltibaseTablespace> {

    protected String schemaName;
    protected String objName;
    protected String partnName;
    
    protected GenericTable targetTable;

    AltibaseTablespaceObjAbs (AltibaseTablespace parent, JDBCResultSet resultSet) {
        super(parent, DBUtils.getFullyQualifiedName(
                parent.getDataSource(),
                JDBCUtils.safeGetString(resultSet, "USER_NAME"), 
                JDBCUtils.safeGetString(resultSet, "OBJ_NAME"), 
                JDBCUtils.safeGetString(resultSet, "PARTITION_NAME")),
                true);
        
        schemaName = JDBCUtils.safeGetString(resultSet, "USER_NAME");
        objName = JDBCUtils.safeGetString(resultSet, "OBJ_NAME");
        partnName = JDBCUtils.safeGetString(resultSet, "PARTITION_NAME");
    }
    
    @Override
    @Property(viewable = false, order = 1, hidden = true)
    public String getName() {
        return name;
    }
    
    @Property(viewable = true, order = 2)
    public AltibaseSchema getSchema() {
        return (AltibaseSchema) getDataSource().getSchema(schemaName);
    }
    
    @Property(viewable = true, order = 10)
    public String getPartnName() {
        return partnName;
    }
    
    protected GenericTable getTargetTable(DBRProgressMonitor monitor, String tableSchema, String tableName) throws DBException {
        if (targetTable == null) {
            AltibaseSchema schema = (AltibaseSchema) getDataSource().getSchema(tableSchema);
            if (schema != null) {
                targetTable = (GenericTable) schema.getTable(monitor, tableName);
            }
        }
        return targetTable;
    }
}