/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.sql.SQLQueryTransformerAllRows;
import org.jkiss.dbeaver.model.impl.sql.SQLQueryTransformerCount;
import org.jkiss.dbeaver.model.impl.sql.SQLQueryTransformerExpression;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.utils.RuntimeUtils;


public class SQLEditorHandlerExecute extends AbstractHandler
{
    private static final Log log = Log.getLog(SQLEditorHandlerExecute.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
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
            default:
                log.error("Unsupported SQL editor command: " + actionId);
                break;
        }
        editor.refreshActions();

        return null;
    }

}