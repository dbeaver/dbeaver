/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;

import java.util.Map;

/**
 * JDBC table column manager
 */
public abstract class JDBCTableColumnManager<OBJECT_TYPE extends JDBCTableColumn, CONTAINER_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE>
{
    //private final Map<IPropertyDescriptor, TablePropertyHandler> handlerMap = new IdentityHashMap<IPropertyDescriptor, TablePropertyHandler>();

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, Object parent, Object copyFrom)
    {
        OBJECT_TYPE newColumn = createNewTableColumn((CONTAINER_TYPE) parent, copyFrom);

        makeInitialCommands(newColumn, context, new CommandCreateTableColumn(newColumn));

        return newColumn;
    }

    public void deleteObject(DBECommandContext commander, OBJECT_TYPE object, Map<String, Object> options)
    {
        commander.addCommand(new CommandDropTableColumn(object), null);
    }

    protected abstract OBJECT_TYPE createNewTableColumn(CONTAINER_TYPE parent, Object copyFrom);

    private class CommandCreateTableColumn extends DBECommandAbstract<OBJECT_TYPE> {
        protected CommandCreateTableColumn(OBJECT_TYPE table)
        {
            super(table, "Create table column");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return null;
        }
    }

    private class CommandDropTableColumn extends DBECommandAbstract<OBJECT_TYPE> {
        protected CommandDropTableColumn(OBJECT_TYPE table)
        {
            super(table, "Drop table column");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return null;
        }
    }


}

