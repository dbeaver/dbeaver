/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.DBECommandDeleteObject;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JDBC table manager
 */
public abstract class JDBCTableManager<OBJECT_TYPE extends JDBCTable, CONTAINER_TYPE extends DBSEntityContainer>
    extends JDBCStructEditor<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, DBECommandContext commandContext, CONTAINER_TYPE parent, Object copyFrom)
    {
        OBJECT_TYPE newTable = createNewTable(parent, copyFrom);

        makeInitialCommands(newTable, commandContext, new StructCreateCommand(newTable, "Create table"));

        return newTable;
    }

    public void deleteObject(DBECommandContext commandContext, OBJECT_TYPE object, Map<String, Object> options)
    {
        commandContext.addCommand(new CommandDropTable(object), new DeleteObjectReflector(), true);
    }

    protected abstract OBJECT_TYPE createNewTable(CONTAINER_TYPE parent, Object copyFrom);

    protected IDatabasePersistAction[] makeObjectChangeActions(ObjectChangeCommand<OBJECT_TYPE> command)
    {
        // Base SQL syntax do not support table properties change
        return null;
    }

    private class CommandDropTable extends DBECommandDeleteObject<OBJECT_TYPE> {
        protected CommandDropTable(OBJECT_TYPE table)
        {
            super(table, "Drop table");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Drop table", "DROP TABLE " + getObject().getFullQualifiedName())
            };
        }
    }

}

