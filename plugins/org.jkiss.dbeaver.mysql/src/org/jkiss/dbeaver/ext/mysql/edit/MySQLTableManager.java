/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;

/**
 * MySQL table manager
 */
public class MySQLTableManager extends JDBCTableManager<MySQLTable, MySQLCatalog> {

    private static final Class<?>[] CHILD_TYPES = {
        MySQLTableColumn.class,
        MySQLConstraint.class,
        MySQLForeignKey.class,
        MySQLIndex.class
    };

    @Override
    protected MySQLTable createNewTable(MySQLCatalog parent, Object copyFrom)
    {
        final MySQLTable table = new MySQLTable(parent);
        table.setName(JDBCObjectNameCaseTransformer.transformName(parent, "NewTable"));

        return table;
    }

    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }
}
