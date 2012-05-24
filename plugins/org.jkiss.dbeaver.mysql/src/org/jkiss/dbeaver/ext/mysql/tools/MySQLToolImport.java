/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPTool;
import org.jkiss.dbeaver.ui.dialogs.tools.ToolWizardDialog;

/**
 * Database import
 */
public class MySQLToolImport implements DBPTool
{
    @Override
    public void execute(IWorkbenchWindow window, DBPObject object) throws DBException
    {
        if (object instanceof MySQLCatalog) {
            ToolWizardDialog dialog = new ToolWizardDialog(
                window,
                new MySQLScriptExecuteWizard((MySQLCatalog) object, true));
            dialog.open();
        }
    }
}
