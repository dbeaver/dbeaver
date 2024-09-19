package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class AltibaseTablespaceObj4Table extends AltibaseTablespaceObjAbs {

    AltibaseTablespaceObj4Table (AltibaseTablespace parent, JDBCResultSet resultSet) {
        super(parent, resultSet);
    }
    
    @Property(viewable = true, linkPossible = true, order = 3)
    public GenericTable getObject(DBRProgressMonitor monitor) throws DBException {
        return getTargetTable(monitor, schemaName, objName);
    }
}