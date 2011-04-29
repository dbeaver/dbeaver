/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic table column manager
 */
public class GenericTableColumnManager extends JDBCTableColumnManager<GenericTableColumn, GenericTable> {


    @Override
    protected IDatabasePersistAction[] makePersistActions(ObjectChangeCommand<GenericTableColumn> command)
    {
        final GenericTable table = command.getObject().getTable();
        final GenericTableColumn column = command.getObject();
        final Object columnName = CommonUtils.toString(command.getProperty(DBConstants.PROP_ID_NAME));
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        boolean newObject = !column.isPersisted();
        if (newObject) {
            actions.add( new AbstractDatabasePersistAction("Create new table column", "ALTER TABLE " + table.getFullQualifiedName() + " ADD "  + columnName) );
        } else {
            actions.add( new AbstractDatabasePersistAction("Alter table column", "ALTER TABLE " + table.getFullQualifiedName() + " ALTER "  + columnName) );
        }
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    @Override
    protected GenericTableColumn createNewTableColumn(GenericTable parent, Object copyFrom)
    {
        final GenericTableColumn column = new GenericTableColumn(parent);
        column.setName("NewColumn");
        column.setTypeName("VARCHAR");
        column.setMaxLength(100);
        column.setOrdinalPosition(parent.getColumnsCache() == null ? 0 : parent.getColumnsCache().size());
        return column;
    }

    public static String getInlineColumnDeclaration(ObjectChangeCommand command)
    {
        GenericTableColumn column = (GenericTableColumn) command.getObject();
        // Create column
        final String columnName = CommonUtils.toString(command.getProperty(DBConstants.PROP_ID_NAME));
        final Object typeName = CommonUtils.toString(command.getProperty(DBConstants.PROP_ID_TYPE_NAME));
        return columnName + " " + typeName;
    }
}
