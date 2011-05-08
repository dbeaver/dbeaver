/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.DBECommandDeleteObject;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;

import java.util.Map;

/**
 * JDBC table column manager
 */
public abstract class JDBCTableColumnManager<OBJECT_TYPE extends JDBCTableColumn, CONTAINER_TYPE extends JDBCTable>
    extends JDBCObjectEditor<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE>
{
    //private final Map<IPropertyDescriptor, TablePropertyHandler> handlerMap = new IdentityHashMap<IPropertyDescriptor, TablePropertyHandler>();

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, DBECommandContext commandContext, CONTAINER_TYPE parent, Object copyFrom)
    {
        OBJECT_TYPE newColumn = createNewTableColumn(parent, copyFrom);

        makeInitialCommands(newColumn, commandContext, new CommandCreateTableColumn(newColumn));

        return newColumn;
    }

    public void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options)
    {
        commandContext.addCommand(new CommandDropTableColumn(object), new DeleteObjectReflector<OBJECT_TYPE>(), true);
    }

    protected abstract OBJECT_TYPE createNewTableColumn(CONTAINER_TYPE parent, Object copyFrom);

    private class CommandCreateTableColumn extends DBECommandAbstract<OBJECT_TYPE> {
        protected CommandCreateTableColumn(OBJECT_TYPE table)
        {
            super(table, "Create table column");
        }
    }

    private class CommandDropTableColumn extends DBECommandDeleteObject<OBJECT_TYPE> {
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

