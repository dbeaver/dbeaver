/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandImpl;
import org.jkiss.dbeaver.model.impl.jdbc.edit.DBEObjectCommanderJDBC;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;

import java.util.Map;

/**
 * MySQLCatalogManager
 */
public class MySQLCatalogManager extends DBEObjectCommanderJDBC<MySQLCatalog> implements DBEObjectMaker<MySQLCatalog>, DBEObjectRenamer<MySQLCatalog> {

    public CreateResult createNewObject(IWorkbenchWindow workbenchWindow, Object parent, MySQLCatalog copyFrom)
    {
        String schemaName = EnterNameDialog.chooseName(workbenchWindow.getShell(), "Schema name");
        if (CommonUtils.isEmpty(schemaName)) {
            return CreateResult.CANCEL;
        }
        MySQLCatalog newCatalog = new MySQLCatalog((MySQLDataSource) parent, null);
        newCatalog.setName(schemaName);
        setObject(newCatalog);
        addCommand(new CommandCreateCatalog(), null);

        return CreateResult.SAVE;
    }

    public void deleteObject(Map<String, Object> options)
    {
        addCommand(new CommandDropCatalog(), null);
    }

    public void renameObject(DBRProgressMonitor monitor, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in MySQL. You should use export/import functions for that.");
        //super.addCommand(new CommandRenameCatalog(newName), null);
        //saveChanges(monitor);
    }

    private class CommandCreateCatalog extends DBECommandImpl<MySQLCatalog> {
        protected CommandCreateCatalog()
        {
            super("Create schema");
        }
        public IDatabasePersistAction[] getPersistActions(final MySQLCatalog object)
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Create schema", "CREATE SCHEMA " + object.getName()) {
                    @Override
                    public void handleExecute(Throwable error)
                    {
                        if (error == null) {
                            object.setPersisted(true);
                        }
                    }
                }};
        }
    }

    private class CommandDropCatalog extends DBECommandImpl<MySQLCatalog> {
        protected CommandDropCatalog()
        {
            super("Drop schema");
        }
        public IDatabasePersistAction[] getPersistActions(final MySQLCatalog object)
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Drop schema", "DROP SCHEMA " + object.getName()) {
                    @Override
                    public void handleExecute(Throwable error)
                    {
                        if (error == null) {
                            object.setPersisted(false);
                        }
                    }
                }};
        }
    }

    private class CommandRenameCatalog extends DBECommandImpl<MySQLCatalog> {
        private String newName;

        protected CommandRenameCatalog(String newName)
        {
            super("Rename catalog");
            this.newName = newName;
        }
        public IDatabasePersistAction[] getPersistActions(final MySQLCatalog object)
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Rename catalog", "RENAME SCHEMA " + object.getName() + " TO " + newName)
            };
        }

        @Override
        public void updateModel(MySQLCatalog object)
        {
            object.setName(newName);
            object.getDataSource().getContainer().fireEvent(
                new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object));
        }
    }

}

