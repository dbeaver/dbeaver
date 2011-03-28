package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.dbeaver.ext.generic.model.GenericEntityContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;

/**
 * Generic table manager
 */
public class GenericTableManager extends JDBCTableManager<GenericTable, GenericEntityContainer> {


    @Override
    protected GenericTable createNewTable(GenericEntityContainer parent, Object copyFrom)
    {
        return new GenericTable(parent, null, null, null);
    }
}
