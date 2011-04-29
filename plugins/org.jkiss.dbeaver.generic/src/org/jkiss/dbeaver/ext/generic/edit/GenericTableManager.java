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
    protected IDatabasePersistAction[] makePersistActions(ObjectChangeCommand command)
    {
        GenericTable table = command.getObject();
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        actions.add( new AbstractDatabasePersistAction("Alter table", "ALTER TABLE " + table.getName()) );
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    @Override
    protected IDatabasePersistAction[] makePersistActions(CommandCreateStruct command)
    {
        final ObjectChangeCommand tableProps = command.getObjectCommands().get(command.getObject());
        if (tableProps == null) {
            log.warn("Object change command not found");
            return null;
        }
        final Object tableName = tableProps.getProperty(DBConstants.PROP_ID_NAME);

        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        actions.add( new AbstractDatabasePersistAction("Create new table", "CREATE TABLE " + tableName) );
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    public boolean isChildType(Class<?> childType)
    {
        return
            childType == GenericTableColumn.class ||
            childType == GenericPrimaryKey.class ||
            childType == GenericForeignKey.class ||
            childType == GenericIndex.class;
    }
}
