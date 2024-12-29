/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.commands;

import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDEEncoding;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.eval.ScriptVariablesResolver;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Control command handler
 */
public class SQLCommandInclude implements SQLControlCommandHandler {

    private static final Log log = Log.getLog(SQLCommandInclude.class);

    public static String getResourceEncoding() {
        String resourceEncoding = IDEEncoding.getResourceEncoding();
        return CommonUtils.isEmpty(resourceEncoding) ? GeneralUtils.getDefaultFileEncoding() : resourceEncoding;
    }

    @NotNull
    @Override
    public SQLControlResult handleCommand(@NotNull DBRProgressMonitor monitor, @NotNull SQLControlCommand command, @NotNull final SQLScriptContext scriptContext) throws DBException {
        String fileName = command.getParameter();
        if (CommonUtils.isEmpty(fileName)) {
            throw new DBException("Empty input file");
        }
        fileName = GeneralUtils.replaceVariables(fileName, new ScriptVariablesResolver(scriptContext), true).trim();
        fileName = DBUtils.getUnQuotedIdentifier(scriptContext.getExecutionContext().getDataSource(), fileName);

        Path curFile = scriptContext.getSourceFile();
        Path incFile = curFile == null ? Path.of(fileName) : curFile.getParent().resolve(fileName);
        if (!Files.exists(incFile)) {
            incFile = Path.of(fileName);
        }
        if (!Files.exists(incFile)) {
            throw new DBException("File '" + fileName + "' not found");
        }

        // Check for nested inclusion
        for (SQLScriptContext sc = scriptContext; sc != null ;sc = sc.getParentContext()) {
            if (sc.getSourceFile() != null && sc.getSourceFile().equals(incFile)) {
                throw new DBException("File '" + fileName + "' recursive inclusion");
            }
        }

        final String fileContents;
        try (InputStream is = Files.newInputStream(incFile)) {
            Reader reader = new InputStreamReader(is, getResourceEncoding());
            fileContents = IOUtils.readToString(reader);
        } catch (IOException e) {
            throw new DBException("IO error reading file '" + fileName + "'", e);
        }
        final Path finalIncFile = incFile;
        final boolean[] statusFlag = new boolean[1];
        UIUtils.syncExec(() -> {
            try {
                final IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
                for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
                    for (IWorkbenchPage page : window.getPages()) {
                        for (IEditorReference editorReference : page.getEditorReferences()) {
                            if (editorReference.getEditorInput() instanceof IncludeEditorInput includeInput) {
                                if (includeInput.incFile.toAbsolutePath().toString().equals(finalIncFile.toAbsolutePath().toString())) {
                                    UIUtils.syncExec(
                                        () -> page.closeEditor(editorReference.getEditor(false), false));
                                }
                            }
                        }
                    }
                }
                final IncludeEditorInput input = new IncludeEditorInput(finalIncFile, fileContents);
                SQLEditor sqlEditor = SQLEditorHandlerOpenEditor.openSQLConsole(
                        workbenchWindow,
                        new SQLNavigatorContext(scriptContext),
                        input);
                final IncludeScriptListener scriptListener = new IncludeScriptListener(
                    workbenchWindow,
                    sqlEditor,
                    statusFlag);
                boolean execResult = sqlEditor.processSQL(false, true, null, scriptListener);
                if (!execResult) {
                    statusFlag[0] = true;
                }
            } catch (Throwable e) {
                log.error(e);
                statusFlag[0] = true;
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

        return SQLControlResult.success();
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
        public void onEndQuery(DBCSession session, SQLQueryResult result, DBCStatistics statistics) {

        }

        @Override
        public void onEndScript(DBCStatistics statistics, boolean hasErrors) {
            if (editor.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.CLOSE_INCLUDED_SCRIPT_AFTER_EXECUTION)) {
                UIUtils.syncExec(() -> workbenchWindow.getActivePage().closeEditor(editor, false));
            }
            statusFlag[0] = true;
        }

        @Override
        public void onEndSqlJob(DBCSession session, SqlJobResult result) {

        }
    }

    private static class IncludeEditorInput extends StringEditorInput implements IURIEditorInput {

        private final Path incFile;

        IncludeEditorInput(Path incFile, CharSequence value) {
            super(incFile.getFileName().toString(), value, true, GeneralUtils.DEFAULT_ENCODING);
            this.incFile = incFile;
        }

        @Override
        public URI getURI() {
            return incFile.toUri();
        }
    }
}
