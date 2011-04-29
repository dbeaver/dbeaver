/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableManager;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generic table manager
 */
public class GenericTableManager extends JDBCTableManager<GenericTable, GenericEntityContainer> {

    @Override
    protected GenericTable createNewTable(GenericEntityContainer parent, Object copyFrom)
    {
        return new GenericTable(parent, "New Table", null, null, false);
    }

    @Override
    protected IDatabasePersistAction[] makePersistActions(ObjectChangeCommand<GenericTable> command)
    {
        GenericTable table = command.getObject();
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        actions.add( new AbstractDatabasePersistAction("Alter table", "ALTER TABLE " + table.getName()) );
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    @Override
    protected IDatabasePersistAction[] makePersistActions(CommandCreateStruct command)
    {
        final GenericTable table = command.getObject();
        final ObjectChangeCommand tableProps = command.getObjectCommands().get(table);
        if (tableProps == null) {
            log.warn("Object change command not found");
            return null;
        }
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();

        final Object tableName = CommonUtils.toString(tableProps.getProperty(DBConstants.PROP_ID_NAME));

        String lineSeparator = ContentUtils.getDefaultLineSeparator();
        StringBuilder createQuery = new StringBuilder(100);
        createQuery.append("CREATE TABLE ").append(tableName).append(" (").append(lineSeparator);
        List<ObjectChangeCommand> columnCommands = new ArrayList<ObjectChangeCommand>();
        for (Map.Entry<DBPObject,ObjectChangeCommand> nestedEntry : command.getObjectCommands().entrySet()) {
            final DBPObject nestedObject = nestedEntry.getKey();
            if (nestedObject == table) {
                continue;
            }
            final ObjectChangeCommand nestedCommand = nestedEntry.getValue();
            if (nestedObject instanceof GenericTableColumn) {
                columnCommands.add(nestedCommand);
            } else {
                // This command should be executed separately
                final IDatabasePersistAction[] nestedActions = nestedCommand.getPersistActions();
                if (nestedActions != null) {
                    Collections.addAll(actions, nestedActions);
                }
            }
        }

        if (!CommonUtils.isEmpty(columnCommands)) {
            for (int i = 0; i < columnCommands.size(); i++) {
                ObjectChangeCommand columnCommand = columnCommands.get(i);
                createQuery.append("\t").append(
                    GenericTableColumnManager.getInlineColumnDeclaration(columnCommand));
                if (i < columnCommands.size() - 1) {
                    createQuery.append(",");
                }
                createQuery.append(lineSeparator);
            }
        }
        createQuery.append(")");

        actions.add( 0, new AbstractDatabasePersistAction("Create new table", createQuery.toString()) );

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
