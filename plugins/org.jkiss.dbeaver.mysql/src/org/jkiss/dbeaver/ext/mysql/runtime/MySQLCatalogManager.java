/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandImpl;
import org.jkiss.dbeaver.model.impl.jdbc.edit.DBEObjectCommanderJDBC;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;

import java.util.Map;

/**
 * MySQLCatalogManager
 */
public class MySQLCatalogManager extends DBEObjectCommanderJDBC<MySQLCatalog> implements DBEObjectMaker<MySQLCatalog> {

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

}

