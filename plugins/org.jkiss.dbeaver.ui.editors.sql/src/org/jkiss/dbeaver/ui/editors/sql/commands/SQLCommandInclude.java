/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.*;
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
import org.jkiss.dbeaver.ui.editors.IncludedScriptFileEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Control command handler
 */
public class SQLCommandInclude implements SQLControlCommandHandler {

    private static final Log log = Log.getLog(SQLCommandInclude.class);


    @NotNull
    @Override
    public SQLControlResult handleCommand(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SQLControlCommand command,
        @NotNull SQLScriptContext scriptContext
    ) throws DBException {
        String fileName = command.getParameter();
        Path includedFile = identifyIncludedScriptFile(fileName, scriptContext);
        verifyNoRecursiveInclusionIsPresent(scriptContext, includedFile, fileName);

        return getSqlControlResult(
            scriptContext,
            includedFile
        );
    }

    @NotNull
    private Path identifyIncludedScriptFile(
        @NotNull String fileName,
        @NotNull SQLScriptContext scriptContext
    ) throws DBException {
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
        return incFile;
    }

    private void verifyNoRecursiveInclusionIsPresent(
        @NotNull SQLScriptContext scriptContext,
        @NotNull Path includedFile,
        @NotNull String fileName
    ) throws DBException {
        for (SQLScriptContext sc = scriptContext; sc != null; sc = sc.getParentContext()) {
            if (sc.getSourceFile() != null && sc.getSourceFile().equals(includedFile)) {
                throw new DBException("File '" + fileName + "' recursive inclusion");
            }
        }
    }

    @NotNull
    private SQLControlResult getSqlControlResult(
        @NotNull SQLScriptContext scriptContext,
        @NotNull Path includedScriptFile
    ) throws DBException {
        IFile workspaceIncludedScriptFile = getWorkspaceIncludedScriptFile(includedScriptFile);
        try {
            CompletableFuture<SQLControlResult> result = getSqlControlResultCompletableFuture(
                scriptContext,
                includedScriptFile,
                workspaceIncludedScriptFile
            );
            return result.get();
        } catch (InterruptedException e) {
            return SQLControlResult.failure();
        } catch (ExecutionException e) {
            throw new DBException("Exception while included script execution", e.getCause());
        }
    }

    @NotNull
    private IFile getWorkspaceIncludedScriptFile(@NotNull Path pathToFile) throws DBException {
        IFile foundFile = ResourcesPlugin.getWorkspace().getRoot()
            .getFileForLocation(org.eclipse.core.runtime.Path.fromOSString(pathToFile.toString()));

        if (foundFile == null) {
            throw new DBException("Cannot find workspace file for included script:" + pathToFile);
        }
        return foundFile;
    }

    @NotNull
    private CompletableFuture<SQLControlResult> getSqlControlResultCompletableFuture(
        @NotNull SQLScriptContext scriptContext,
        @NotNull Path includedScriptFile,
        @NotNull IFile workspaceIncludedScriptFile
    ) {
        CompletableFuture<SQLControlResult> result = new CompletableFuture<>();
        UIUtils.syncExec(() -> {
            try {
                IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
                closeDuplicatedEditors(includedScriptFile);
                SQLEditor sqlEditor = getSqlEditor(scriptContext, includedScriptFile, workspaceIncludedScriptFile, workbenchWindow);
                IncludeScriptListener scriptListener = new IncludeScriptListener(
                    workbenchWindow,
                    sqlEditor,
                    result
                );
                boolean execResult = sqlEditor.processSQL(false, true, null, scriptListener);
                if (!execResult) {
                    result.complete(SQLControlResult.failure());
                }
            } catch (Throwable e) {
                log.error(e);
                result.complete(SQLControlResult.failure());
            }
        });
        return result;
    }

    @NotNull
    private SQLEditor getSqlEditor(
        @NotNull SQLScriptContext scriptContext,
        @NotNull Path includedScriptFile,
        @NotNull IFile workspaceIncludedScriptFile,
        @NotNull IWorkbenchWindow workbenchWindow
    ) {
        IncludedScriptFileEditorInput input = new IncludedScriptFileEditorInput(workspaceIncludedScriptFile, includedScriptFile);
        SQLEditor sqlEditor = SQLEditorHandlerOpenEditor.openNewSQLConsole(
            workbenchWindow,
            new SQLNavigatorContext(scriptContext, true),
            input
        );
        sqlEditor.reloadSyntaxRules();
        return sqlEditor;
    }

    private void closeDuplicatedEditors(@NotNull Path includedScriptFile) throws PartInitException {
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference editorReference : page.getEditorReferences()) {
                    if (isEditorForSameIncludedScript(editorReference, includedScriptFile)) {
                            UIUtils.syncExec(
                                () -> page.closeEditor(editorReference.getEditor(false), false));

                    }
                }
            }
        }
    }

    private boolean isEditorForSameIncludedScript(
        @NotNull IEditorReference editorReference,
        @NotNull Path includedScriptFile
    ) throws PartInitException {
        return editorReference.getEditorInput() instanceof IncludedScriptFileEditorInput includeInput
            && includeInput.getIncludedScriptFile().toAbsolutePath().toString().equals(includedScriptFile.toAbsolutePath().toString());
    }

    private static class IncludeScriptListener implements SQLQueryListener {
        private final IWorkbenchWindow workbenchWindow;
        private final SQLEditor editor;
        private final CompletableFuture<SQLControlResult> result;

        IncludeScriptListener(
            @NotNull IWorkbenchWindow workbenchWindow,
            @NotNull SQLEditor editor,
            @NotNull CompletableFuture<SQLControlResult> result
        ) {
            this.workbenchWindow = workbenchWindow;
            this.editor = editor;
            this.result = result;
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
            if (isShouldCloseIncludedScript(hasErrors)) {
                UIUtils.syncExec(() -> workbenchWindow.getActivePage().closeEditor(editor, false));
            }
        }

        @Override
        public void onEndSqlJob(DBCSession session, SqlJobResult result) {
            this.result.complete(result == SqlJobResult.SUCCESS ? SQLControlResult.success() : SQLControlResult.failure());
        }

        private boolean isShouldCloseIncludedScript(boolean hasErrors) {
            return !hasErrors && editor.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.CLOSE_INCLUDED_SCRIPT_AFTER_EXECUTION);
        }
    }

}
