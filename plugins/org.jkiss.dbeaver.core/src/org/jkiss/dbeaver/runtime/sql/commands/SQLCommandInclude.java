/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.sql.commands;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.sql.SQLControlCommand;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.runtime.sql.SQLControlCommandHandler;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.OpenHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;

/**
 * Control command handler
 */
public class SQLCommandInclude implements SQLControlCommandHandler {

    @Override
    public boolean handleCommand(SQLControlCommand command, final SQLScriptContext scriptContext) throws DBException {
        String fileName = command.getParameter();
        if (CommonUtils.isEmpty(fileName)) {
            throw new DBException("Empty input file");
        }
        fileName = GeneralUtils.replaceVariables(fileName, new GeneralUtils.MapResolver(scriptContext.getVariables())).trim();
        if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
            fileName = fileName.substring(1, fileName.length() - 1);
        }

        //scriptContext.getOutputWriter().println(parameter);
        File curFile = EditorUtils.getLocalFileFromInput(scriptContext.getEditorInput());
        File incFile = new File(curFile.getParent(), fileName);
        if (!incFile.exists()) {
            incFile = new File(fileName);
        }
        if (!incFile.exists()) {
            throw new DBException("File '" + fileName + "' not found");
        }

        final String fileContents;
        try (InputStream is = new FileInputStream(incFile)) {
            Reader reader = new InputStreamReader(is, DBeaverCore.getGlobalPreferenceStore().getString(DBeaverPreferences.DEFAULT_RESOURCE_ENCODING));
            fileContents = IOUtils.readToString(reader);
        } catch (IOException e) {
            throw new DBException("IO error reading file '" + fileName + "'", e);
        }
        final String inputFileName = fileName;
        DBeaverUI.syncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchWindow workbenchWindow = DBeaverUI.getActiveWorkbenchWindow();
                SQLEditor sqlEditor = OpenHandler.openSQLConsole(
                        workbenchWindow,
                        scriptContext.getExecutionContext().getDataSource().getContainer(),
                        inputFileName,
                        fileContents);
                sqlEditor.processSQL(false, true);

                workbenchWindow.getActivePage().closeEditor(sqlEditor, false);
            }
        });

        return true;
    }

}
