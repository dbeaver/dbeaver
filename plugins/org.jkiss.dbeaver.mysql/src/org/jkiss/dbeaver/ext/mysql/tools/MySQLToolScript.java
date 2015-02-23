/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
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
        java.util.List<String> cmd = new ArrayList<String>();
        toolWizard.fillProcessParameters(cmd);

        if (toolWizard.isVerbose()) {
            cmd.add("-v");
        }
        DBPConnectionInfo connectionInfo = toolWizard.getConnectionInfo();
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
