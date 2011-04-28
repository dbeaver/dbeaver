/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic table column manager
 */
public class GenericTableColumnManager extends JDBCTableColumnManager<GenericTableColumn, GenericTable> {


    @Override
    protected Collection<IDatabasePersistAction> makePersistActions(ObjectChangeCommand command)
    {
        final GenericTable table = command.getObject().getTable();
        final GenericTableColumn column = command.getObject();
        final Object columnName = command.getProperty(DBConstants.PROP_ID_NAME);
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        boolean newObject = !column.isPersisted();
        if (newObject) {
            actions.add( new AbstractDatabasePersistAction("Create new table column", "ALTER TABLE " + table.getFullQualifiedName() + " ADD "  + columnName) );
        } else {
            actions.add( new AbstractDatabasePersistAction("Alter table column", "ALTER TABLE " + table.getFullQualifiedName() + " ALTER "  + columnName) );
        }
        return actions;
    }

    @Override
    protected GenericTableColumn createNewTableColumn(GenericTable parent, Object copyFrom)
    {
        return new GenericTableColumn(parent);
    }
}
