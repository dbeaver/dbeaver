/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;

import java.util.Map;

/**
 * JDBC table manager
 */
public abstract class JDBCTableManager<OBJECT_TYPE extends JDBCTable, CONTAINER_TYPE extends DBSEntityContainer>
    extends JDBCStructEditor<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE>
{

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, DBECommandContext commander, Object parent, Object copyFrom)
    {
        OBJECT_TYPE newTable = createNewTable((CONTAINER_TYPE) parent, copyFrom);
        // Add dummy command (it does nothing)
        commander.addCommand(new CommandCreateStruct(newTable), null);

        return newTable;
    }

    public void deleteObject(DBECommandContext commander, OBJECT_TYPE object, Map<String, Object> options)
    {
        commander.addCommand(new CommandDropTable(object), null);
    }

    protected abstract OBJECT_TYPE createNewTable(CONTAINER_TYPE parent, Object copyFrom);

    private class CommandDropTable extends DBECommandAbstract<OBJECT_TYPE> {
        protected CommandDropTable(OBJECT_TYPE table)
        {
            super(table, "Drop table");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Drop table", "DROP TABLE " + getObject().getFullQualifiedName()) {
                    @Override
                    public void handleExecute(Throwable error)
                    {
                        if (error == null) {
                            //object.setPersisted(false);
                        }
                    }
                }};
        }
    }

}

