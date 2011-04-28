/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic table manager
 */
public class GenericTableManager extends JDBCTableManager<GenericTable, GenericEntityContainer> {


    @Override
    protected GenericTable createNewTable(GenericEntityContainer parent, Object copyFrom)
    {
        return new GenericTable(parent, null, null, null, false);
    }

    @Override
    protected Collection<IDatabasePersistAction> makePersistActions(ObjectChangeCommand command)
    {
        GenericTable table = command.getObject();
        final Object tableName = command.getProperty(DBConstants.PROP_ID_NAME);
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        boolean newObject = !table.isPersisted();
        if (newObject) {
            actions.add( new AbstractDatabasePersistAction("Create new table", "CREATE TABLE " + tableName) );
        } else {
            actions.add( new AbstractDatabasePersistAction("Alter table", "ALTER TABLE " + table.getName()) );
        }
        return actions;
    }

    public Class<?>[] getChildTypes()
    {
        return new Class[] {
            GenericTableColumn.class,
            GenericPrimaryKey.class,
            GenericForeignKey.class,
            GenericIndex.class,
        };
    }

}
