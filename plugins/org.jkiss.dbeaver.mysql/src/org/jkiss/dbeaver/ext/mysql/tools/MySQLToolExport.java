/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPTool;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;

/**
 * Database export
 */
public class MySQLToolExport implements DBPTool
{
    public void execute(IWorkbenchWindow window, DBPObject object) throws DBException
    {
        if (object instanceof MySQLCatalog) {
            ActiveWizardDialog dialog = new ActiveWizardDialog(
                window,
                new MySQLDatabaseExportWizard((MySQLCatalog) object));
            dialog.open();
        }
    }
}
