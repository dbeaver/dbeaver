/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDatabase;
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
            if (object instanceof MySQLDatabase) {
                ToolWizardDialog dialog = new ToolWizardDialog(
                    window,
                    new MySQLScriptExecuteWizard((MySQLDatabase) object, false));
                dialog.open();
            }
        }
    }

    public static <BASE_OBJECT extends DBSObject, PROCESS_ARG> List<String> getMySQLToolCommandLine(
        AbstractToolWizard<BASE_OBJECT, PROCESS_ARG> toolWizard, PROCESS_ARG arg) throws IOException
    {
        java.util.List<String> cmd = new ArrayList<>();
        toolWizard.fillProcessParameters(cmd, arg);

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
        // Password is passed in env variable (#1004)
//        if (!CommonUtils.isEmpty(toolWizard.getToolUserPassword())) {
//            cmd.add("--password=" + toolWizard.getToolUserPassword());
//        }

        return cmd;
    }
}
