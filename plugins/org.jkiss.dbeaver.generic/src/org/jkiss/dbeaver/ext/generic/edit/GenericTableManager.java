/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;

/**
 * Generic table manager
 */
public class GenericTableManager extends JDBCTableManager<GenericTable, GenericEntityContainer> {

    private static final Class<?>[] CHILD_TYPES = {
        GenericTableColumn.class,
        GenericPrimaryKey.class,
        GenericForeignKey.class,
        GenericIndex.class
    };

    @Override
    protected GenericTable createNewTable(GenericEntityContainer parent, Object copyFrom)
    {
        final GenericTable table = new GenericTable(parent);
        table.setName(JDBCObjectNameCaseTransformer.transformName(parent, "NewTable"));

        return table;
    }

    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }
}
