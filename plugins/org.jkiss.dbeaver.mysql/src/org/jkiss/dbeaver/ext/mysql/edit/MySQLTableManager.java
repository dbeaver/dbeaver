/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

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
        try {
            table.getAdditionalInfo(VoidProgressMonitor.INSTANCE).setEngine(parent.getDataSource().getDefaultEngine());
        } catch (DBCException e) {
            // Never be here
            log.error(e);
        }

        return table;
    }

    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }
}
