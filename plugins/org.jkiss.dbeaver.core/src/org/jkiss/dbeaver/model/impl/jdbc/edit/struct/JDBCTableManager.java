/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandImpl;
import org.jkiss.dbeaver.model.impl.jdbc.edit.DBEObjectCommanderJDBC;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;

import java.util.Map;

/**
 * JDBC table manager
 */
public abstract class JDBCTableManager<OBJECT_TYPE extends JDBCTable, CONTAINER_TYPE extends DBSEntityContainer>  extends DBEObjectCommanderJDBC<OBJECT_TYPE> implements DBEObjectMaker<OBJECT_TYPE> {

    public CreateResult createNewObject(IWorkbenchWindow workbenchWindow, Object parent, OBJECT_TYPE copyFrom)
    {
        OBJECT_TYPE newUser = createNewTable((CONTAINER_TYPE) parent, copyFrom);
        setObject(newUser);

        return CreateResult.OPEN_EDITOR;
    }

    public void deleteObject(Map<String, Object> options)
    {
        addCommand(new CommandDropTable(), null);
    }

    private class CommandCreateTable extends DBECommandImpl<OBJECT_TYPE> {
        protected CommandCreateTable()
        {
            super("Create table");
        }
        public IDatabasePersistAction[] getPersistActions(final OBJECT_TYPE object)
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Create table", "CREATE TABLE " + object.getFullQualifiedName()) {
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
        protected CommandDropTable()
        {
            super("Drop table");
        }

        public IDatabasePersistAction[] getPersistActions(final OBJECT_TYPE object)
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Drop schema", "DROP TABLE " + object.getFullQualifiedName()) {
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

    protected abstract OBJECT_TYPE createNewTable(CONTAINER_TYPE parent, OBJECT_TYPE copyFrom);

}

