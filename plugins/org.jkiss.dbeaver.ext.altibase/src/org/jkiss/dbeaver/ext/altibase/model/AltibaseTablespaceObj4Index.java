package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class AltibaseTablespaceObj4Index extends AltibaseTablespaceObjAbs {

    private String tableSchema;
    private String tableName;

    private AltibaseTableIndex targetIndex;

    AltibaseTablespaceObj4Index (AltibaseTablespace parent, JDBCResultSet resultSet) {
        super(parent, resultSet);

        tableSchema = JDBCUtils.safeGetString(resultSet, "table_schema");
        tableName = JDBCUtils.safeGetString(resultSet, "table_name");
    }

    @Property(viewable = true, linkPossible = true, order = 3)
    public AltibaseTableIndex getObject(DBRProgressMonitor monitor) throws DBException {
        return getTargetIndex(monitor);
    }

    @Property(viewable = true, linkPossible = true, order = 20)
    public AltibaseSchema getTableSchema() {
        return (AltibaseSchema) getDataSource().getSchema(tableSchema);
    } 

    @Property(viewable = true, linkPossible = true, order = 21)
    public GenericTable getTable(DBRProgressMonitor monitor) throws DBException {
        return getTargetTable(monitor, tableSchema, tableName);
    }

    private AltibaseTableIndex getTargetIndex(DBRProgressMonitor monitor) throws DBException {
        if (targetIndex == null) {
            AltibaseSchema schema = (AltibaseSchema) getDataSource().getSchema(schemaName);
            if (schema != null) {
                targetIndex = (AltibaseTableIndex) schema.getIndex(monitor, objName);
            }
        }
        return targetIndex;
    }
}