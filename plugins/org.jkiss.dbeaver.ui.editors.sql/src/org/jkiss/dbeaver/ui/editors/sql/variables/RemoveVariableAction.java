/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

public class RemoveVariableAction extends Action {
    static protected final Log log = Log.getLog(RemoveVariableAction.class);

    private final SQLEditor editor;
    private final String varName;

    public RemoveVariableAction(SQLEditor editor, String varName) {
        super(SQLEditorMessages.action_result_tabs_assign_variable);
        this.editor = editor;
        this.varName = varName;
    }

    @Override
    public void run() {
        if (UIUtils.confirmAction(
            editor.getSite().getShell(),
            "Delete variable '" + varName + "'?",
            "Delete variable '" + varName + "'?"))
        {
            editor.getGlobalScriptContext().removeVariable(varName);
        }
    }
}
