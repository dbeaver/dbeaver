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

import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.sql.SQLControlCommand;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.eval.ScriptVariablesResolver;
import org.jkiss.dbeaver.runtime.sql.SQLControlCommandHandler;
import org.jkiss.dbeaver.runtime.sql.SQLQueryListener;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.OpenHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.net.URI;

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
        fileName = GeneralUtils.replaceVariables(fileName, new ScriptVariablesResolver(scriptContext)).trim();
        fileName = DBUtils.getUnQuotedIdentifier(scriptContext.getExecutionContext().getDataSource(), fileName);

        File curFile = scriptContext.getSourceFile();
        File incFile = curFile == null ? new File(fileName) : new File(curFile.getParent(), fileName);
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
        final File finalIncFile = incFile;
        final boolean statusFlag[] = new boolean[1];
        DBeaverUI.syncExec(new Runnable() {
            @Override
            public void run() {
                final IWorkbenchWindow workbenchWindow = DBeaverUI.getActiveWorkbenchWindow();
                final IncludeEditorInput input = new IncludeEditorInput(finalIncFile, fileContents);
                SQLEditor sqlEditor = OpenHandler.openSQLConsole(
                        workbenchWindow,
                        scriptContext.getExecutionContext().getDataSource().getContainer(),
                        input);
                final IncludeScriptListener scriptListener = new IncludeScriptListener(workbenchWindow, sqlEditor, statusFlag);
                sqlEditor.processSQL(false, true, null, scriptListener);
            }
        });

        // Wait until script finished
        while (!statusFlag[0]) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }

        return true;
    }

    private static class IncludeScriptListener implements SQLQueryListener {
        private final IWorkbenchWindow workbenchWindow;
        private final SQLEditor editor;
        private final boolean[] statusFlag;
        IncludeScriptListener(IWorkbenchWindow workbenchWindow, SQLEditor editor, boolean[] statusFlag) {
            this.workbenchWindow = workbenchWindow;
            this.editor = editor;
            this.statusFlag = statusFlag;
        }

        @Override
        public void onStartScript() {

        }

        @Override
        public void onStartQuery(DBCSession session, SQLQuery query) {

        }

        @Override
        public void onEndQuery(DBCSession session, SQLQueryResult result) {

        }

        @Override
        public void onEndScript(DBCStatistics statistics, boolean hasErrors) {
            DBeaverUI.syncExec(new Runnable() {
                @Override
                public void run() {
                    workbenchWindow.getActivePage().closeEditor(editor, false);
                }
            });
            statusFlag[0] = true;
        }
    }

    private static class IncludeEditorInput extends StringEditorInput implements IURIEditorInput {

        private final File incFile;

        IncludeEditorInput(File incFile, CharSequence value) {
            super(incFile.getName(), value, true, GeneralUtils.DEFAULT_ENCODING);
            this.incFile = incFile;
        }

        @Override
        public URI getURI() {
            return incFile.toURI();
        }
    }
}
