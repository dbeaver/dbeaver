/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandImpl;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;

import java.util.Map;

/**
 * JDBC table manager
 */
public abstract class JDBCTableManager<OBJECT_TYPE extends JDBCTable, CONTAINER_TYPE extends DBSEntityContainer>  extends JDBCObjectManager<OBJECT_TYPE> implements DBEObjectMaker<OBJECT_TYPE> {
    public long getMakerOptions()
    {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, DBEObjectCommander commander, Object parent, Object copyFrom)
    {
        OBJECT_TYPE newTable = createNewTable((CONTAINER_TYPE) parent, copyFrom);

        commander.addCommand(new CommandCreateTable(newTable), null);

        return newTable;
    }

    public void deleteObject(DBEObjectCommander commander, OBJECT_TYPE object, Map<String, Object> options)
    {
        commander.addCommand(new CommandDropTable(object), null);
    }

    private class CommandCreateTable extends DBECommandImpl<OBJECT_TYPE> {
        protected CommandCreateTable(OBJECT_TYPE table)
        {
            super(table, "Create table");
        }
        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Create table", "CREATE TABLE " + getObject().getFullQualifiedName()) {
                    @Override
                    public void handleExecute(Throwable error)
                    {
                        if (error == null) {
                            //object.setPersisted(true);
                        }
                    }
                }};
        }
    }

    private class CommandDropTable extends DBECommandImpl<OBJECT_TYPE> {
        protected CommandDropTable(OBJECT_TYPE table)
        {
            super(table, "Drop table");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Drop schema", "DROP TABLE " + getObject().getFullQualifiedName()) {
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

    protected abstract OBJECT_TYPE createNewTable(CONTAINER_TYPE parent, Object copyFrom);

}

