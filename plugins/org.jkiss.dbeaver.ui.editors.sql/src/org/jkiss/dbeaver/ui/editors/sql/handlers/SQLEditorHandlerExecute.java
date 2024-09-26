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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.sql.transformers.SQLQueryTransformerAllRows;
import org.jkiss.dbeaver.model.sql.transformers.SQLQueryTransformerCount;
import org.jkiss.dbeaver.model.sql.transformers.SQLQueryTransformerExpression;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.exec.SQLNativeExecutorDescriptor;
import org.jkiss.dbeaver.ui.actions.exec.SQLNativeExecutorRegistry;
import org.jkiss.dbeaver.ui.actions.exec.SQLScriptExecutor;
import org.jkiss.dbeaver.ui.dialogs.MessageBoxBuilder;
import org.jkiss.dbeaver.ui.dialogs.Reply;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;


public class SQLEditorHandlerExecute extends AbstractHandler {
    private static final Log log = Log.getLog(SQLEditorHandlerExecute.class);

    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException
    {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor == null) {
            log.error("No active SQL editor found");
            return null;
        }
        String actionId = event.getCommand().getId();
        switch (actionId) {
            case SQLEditorCommands.CMD_EXECUTE_STATEMENT:
                editor.processSQL(false, false);
                break;
            case SQLEditorCommands.CMD_EXECUTE_STATEMENT_NEW:
                editor.processSQL(true, false);
                break;
            case SQLEditorCommands.CMD_EXECUTE_SCRIPT:
                editor.processSQL(false, true);
                break;
            case SQLEditorCommands.CMD_EXECUTE_SCRIPT_NATIVE: {
                if (editor.getDataSourceContainer() == null) {
                    break;
                }
                SQLNativeExecutorDescriptor executorDescriptor = SQLNativeExecutorRegistry.getInstance()
                    .getExecutorDescriptor(editor.getDataSourceContainer());
                if (executorDescriptor == null) {
                    throw new ExecutionException("Valid native executor is not found");
                }
                if (editor.isDirty()) {
                    if (editor.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE)) {
                        editor.doSave(new NullProgressMonitor());
                    } else {
                        Reply reply = MessageBoxBuilder.builder()
                            .setMessage(SQLEditorMessages.dialog_save_script_message)
                            .setTitle(SQLEditorMessages.dialog_save_script_title)
                            .setReplies(Reply.YES, Reply.NO, Reply.CANCEL).setPrimaryImage(DBIcon.STATUS_INFO)
                            .showMessageBox();
                        // Cancel the execution
                        if (reply != null) {
                            if (reply.equals(Reply.CANCEL)) {
                                return null;
                            }
                            if (reply.equals(Reply.YES)) {
                                editor.doSave(new NullProgressMonitor());
                            }
                        }
                    }
                }
                try {
                    if (editor.getExecutionContext() instanceof DBCExecutionContextDefaults) {
                        DBCExecutionContextDefaults<?, ?> executionContext
                            = (DBCExecutionContextDefaults<?, ?>) editor.getExecutionContext();
                        SQLScriptExecutor<DBSObject> nativeExecutor
                            = (SQLScriptExecutor<DBSObject>) executorDescriptor.getNativeExecutor();
                        if (nativeExecutor == null) {
                            throw new ExecutionException("Valid native executor is not found");
                        }
                        DBSObject object = executionContext.getDefaultCatalog();
                        if (object == null) {
                            object = editor.getDataSource();
                        }
                        if (editor.getGlobalScriptContext().getSourceFile() != null) {
                            nativeExecutor.execute(object, editor.getGlobalScriptContext().getSourceFile());
                        } else {
                            nativeExecutor.execute(object, null);
                        }
                    } else {
                        throw new DBException("Disconnected from database");
                    }
                } catch (DBException e) {
                    log.error(e);
                }
                break;
            }
            case SQLEditorCommands.CMD_EXECUTE_SCRIPT_FROM_POSITION:
                editor.processSQL(false, true, true);
                break;
            case SQLEditorCommands.CMD_EXECUTE_SCRIPT_NEW:
                editor.processSQL(true, true);
                break;
            case SQLEditorCommands.CMD_EXECUTE_ROW_COUNT:
                editor.processSQL(false, false, new SQLQueryTransformerCount(), null);
                break;
            case SQLEditorCommands.CMD_EXECUTE_EXPRESSION:
                editor.processSQL(false, false, new SQLQueryTransformerExpression(), null);
                break;
            case SQLEditorCommands.CMD_EXECUTE_ALL_ROWS:
                editor.processSQL(false, false, new SQLQueryTransformerAllRows(), null);
                break;
            case SQLEditorCommands.CMD_EXPLAIN_PLAN:
                editor.explainQueryPlan();
                break;
            case SQLEditorCommands.CMD_LOAD_PLAN:
                editor.loadQueryPlan();
                break;
            case SQLEditorCommands.CMD_MULTIPLE_RESULTS_PER_TAB:
                editor.toggleMultipleResultsPerTab();
                break;
            default:
                log.error("Unsupported SQL editor command: " + actionId);
                break;
        }
        editor.refreshActions();

        return null;
    }
}