/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBOCommandImpl;
import org.jkiss.dbeaver.model.impl.jdbc.edit.DBOEditorJDBC;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;

import java.util.Map;

/**
 * MySQLCatalogManager
 */
public class MySQLCatalogManager extends DBOEditorJDBC<MySQLCatalog> implements DBOCreator<MySQLCatalog> {

    public boolean createNewObject(IWorkbenchWindow workbenchWindow, DBSObject parent, MySQLCatalog copyFrom)
    {
        String schemaName = EnterNameDialog.chooseName(workbenchWindow.getShell(), "Schema name");
        if (!CommonUtils.isEmpty(schemaName)) {
            MySQLCatalog newCatalog = new MySQLCatalog((MySQLDataSource) parent, null);
            newCatalog.setName(schemaName);
            setObject(newCatalog);
            addCommand(new CommandCreateCatalog(), null);
        }

        return false;
    }

    public void deleteObject(Map<String, Object> options)
    {
        addCommand(new CommandDropCatalog(), null);
    }

    private class CommandCreateCatalog extends DBOCommandImpl<MySQLCatalog> {
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

    private class CommandDropCatalog extends DBOCommandImpl<MySQLCatalog> {
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

