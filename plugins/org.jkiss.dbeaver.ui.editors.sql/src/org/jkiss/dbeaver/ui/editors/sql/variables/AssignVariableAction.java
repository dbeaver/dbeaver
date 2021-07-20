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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class AssignVariableAction extends Action {
    static protected final Log log = Log.getLog(AssignVariableAction.class);

    private final SQLEditor editor;
    private final String queryText;
    private boolean isQuery = false;
    private boolean isEditable = true;
    private boolean checkDuplicates = true;
    private SQLEditorBase valueEditor;
    private String varValue;

    public AssignVariableAction(SQLEditor editor, String varValue) {
        super(SQLEditorMessages.action_result_tabs_assign_variable);
        this.editor = editor;
        this.queryText = varValue;
    }

    public void setQuery(boolean query) {
        isQuery = query;
    }

    public void setEditable(boolean editable) {
        isEditable = editable;
    }

    public void setCheckDuplicates(boolean checkDuplicates) {
        this.checkDuplicates = checkDuplicates;
    }

    @Override
    public void run() {
        EnterNameDialog dialog = new EnterNameDialog(
            editor.getEditorControlWrapper().getShell(),
            isQuery ? SQLEditorMessages.action_result_tabs_assign_variable_sql : SQLEditorMessages.action_result_tabs_assign_variable,
            "")
        {

            @Override
            protected IDialogSettings getDialogBoundsSettings() {
                return UIUtils.getDialogSettings("DBeaver.SQLEditor.AssignVariableDialog"); //$NON-NLS-1$
            }

            @Override
            protected Composite createDialogArea(Composite parent) {
                final Composite area = super.createDialogArea(parent);

                valueEditor = new SQLEditorBase() {
                    @Nullable
                    @Override
                    public DBCExecutionContext getExecutionContext() {
                        return editor.getExecutionContext();
                    }
                };
                try {
                    valueEditor.init(new SubEditorSite(editor.getSite()),
                        new StringEditorInput("Variable value", queryText, !isEditable, GeneralUtils.getDefaultFileEncoding()));
                } catch (PartInitException e) {
                    log.error(e);
                }
                Composite editorPH = UIUtils.createComposite(area, 1);
                editorPH.setLayoutData(new GridData(GridData.FILL_BOTH));
                UIUtils.createControlLabel(editorPH, isQuery ? "Query" : "Value");
                valueEditor.createPartControl(editorPH);
                valueEditor.getEditorControlWrapper().setLayoutData(new GridData(GridData.FILL_BOTH));
                valueEditor.setWordWrap(true);
                valueEditor.reloadSyntaxRules();

                UIUtils.asyncExec(() -> propNameText.setFocus());

                return area;
            }

            @Override
            protected void okPressed() {
                if (checkDuplicates) {
                    String varName = propNameText.getText();
                    if (editor.getGlobalScriptContext().hasVariable(varName)) {
                        UIUtils.showMessageBox(getShell(), "Duplicate variable", "Variable '" + varName + "' already declared", SWT.ICON_ERROR);
                        return;
                    }
                }
                varValue = valueEditor.getEditorControl().getText();
                super.okPressed();
            }
        };
        if (dialog.open() == IDialogConstants.OK_ID) {
            editor.getGlobalScriptContext().setVariable(
                dialog.getResult(),
                varValue);
        }
    }
}
