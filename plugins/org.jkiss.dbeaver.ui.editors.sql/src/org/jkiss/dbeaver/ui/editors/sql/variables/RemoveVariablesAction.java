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
package org.jkiss.dbeaver.ui.editors.sql.variables;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.registry.SQLQueryParameterRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.util.List;

public class RemoveVariablesAction extends Action {
    static protected final Log log = Log.getLog(RemoveVariablesAction.class);

    private final SQLEditor editor;
    private final List<String> varNames;

    public RemoveVariablesAction(SQLEditor editor, List<String> varNames) {
        super(SQLEditorMessages.action_result_tabs_assign_variable);
        this.editor = editor;
        this.varNames = varNames;
    }

    @Override
    public void run() {
        if (UIUtils.confirmAction(
            editor.getSite().getShell(),
            SQLEditorMessages.action_result_tabs_delete_variables,
            SQLEditorMessages.action_result_tabs_delete_variables_question +
                ' ' +
                varNames.toString().replaceAll("^[\\[]|[\\]]$","")
                + "?"))
        {
            for (String varName : varNames) {
                final SQLQueryParameterRegistry instance = SQLQueryParameterRegistry.getInstance();

                if (editor.getGlobalScriptContext().hasDefaultParameterValue(varName) || instance.getParameter(varName) != null){
                    editor.getGlobalScriptContext().removeDefaultParameterValue(varName);
                } else {
                    editor.getGlobalScriptContext().removeVariable(varName);
                }
            }
        }
    }
}
