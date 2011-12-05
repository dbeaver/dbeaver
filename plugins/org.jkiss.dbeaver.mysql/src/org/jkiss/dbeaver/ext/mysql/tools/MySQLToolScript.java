/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPTool;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Database import
 */
public class MySQLToolScript implements DBPTool
{
    public void execute(IWorkbenchWindow window, DBPObject object) throws DBException
    {
        if (object instanceof MySQLCatalog) {
            ActiveWizardDialog dialog = new ActiveWizardDialog(
                window,
                new MySQLDatabaseImportWizard((MySQLCatalog) object, false));
            dialog.open();
        }
    }

    public static List<String> getMySQLToolCommandLine(AbstractToolWizard toolWizard)
    {
        java.util.List<String> cmd = new ArrayList<String>();
        toolWizard.fillProcessParameters(cmd);

        if (toolWizard.isVerbose()) {
            cmd.add("-v");
        }
        cmd.add("-q");
        DBPConnectionInfo connectionInfo = toolWizard.getConnectionInfo();
        cmd.add("--host=" + connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            cmd.add("--port=" + connectionInfo.getHostPort());
        }
        cmd.add("-u");
        cmd.add(connectionInfo.getUserName());
        cmd.add("--password=" + connectionInfo.getUserPassword());

        cmd.add(toolWizard.getDbObject().getName());
        return cmd;
    }
}
