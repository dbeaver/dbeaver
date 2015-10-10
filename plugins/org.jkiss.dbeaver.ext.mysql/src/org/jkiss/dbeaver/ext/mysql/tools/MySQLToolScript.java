/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.dbeaver.ui.dialogs.tools.ToolWizardDialog;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Database import
 */
public class MySQLToolScript implements IExternalTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        for (DBSObject object : objects) {
            if (object instanceof MySQLCatalog) {
                ToolWizardDialog dialog = new ToolWizardDialog(
                    window,
                    new MySQLScriptExecuteWizard((MySQLCatalog) object, false));
                dialog.open();
            }
        }
    }

    public static List<String> getMySQLToolCommandLine(AbstractToolWizard toolWizard) throws IOException
    {
        java.util.List<String> cmd = new ArrayList<>();
        toolWizard.fillProcessParameters(cmd);

        if (toolWizard.isVerbose()) {
            cmd.add("-v");
        }
        DBPConnectionConfiguration connectionInfo = toolWizard.getConnectionInfo();
        cmd.add("--host=" + connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            cmd.add("--port=" + connectionInfo.getHostPort());
        }
        cmd.add("-u");
        cmd.add(toolWizard.getToolUserName());
        if (!CommonUtils.isEmpty(toolWizard.getToolUserPassword())) {
            cmd.add("--password=" + toolWizard.getToolUserPassword());
        }

        cmd.add(toolWizard.getDatabaseObject().getName());
        return cmd;
    }
}
