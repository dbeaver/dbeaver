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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * VariablesHintLabel
 */
public class VariablesHintLabel {

    private final String[][] variables;
    private IVariableResolver resolver;
    private CLabel infoLabel;

    public VariablesHintLabel(Composite parent, String hintLabel, String hintTitle, String[][] vars) {
        this(parent, hintLabel, hintTitle, vars, true);
    }

    public VariablesHintLabel(Composite parent, String hintLabel, String hintTitle, String[][] vars, boolean stretch) {
        this.variables = vars;

        String varsText = GeneralUtils.generateVariablesLegend(vars);

        infoLabel = UIUtils.createInfoLabel(parent, hintLabel);
        Layout layout = parent.getLayout();
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        if (stretch && layout instanceof GridLayout) {
            gd.horizontalSpan = ((GridLayout) layout).numColumns;
        }
        infoLabel.setLayoutData(gd);
        infoLabel.setCursor(infoLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        infoLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if (resolver != null) {
                    VariableListDialog dialog = new VariableListDialog(parent.getShell(), hintTitle);
                    dialog.open();
                } else {
                    EditTextDialog dialog = new EditTextDialog(parent.getShell(), hintTitle, varsText, true);
                    dialog.setMonospaceFont(true);
                    dialog.setAutoSize(true);
                    dialog.open();
                }
            }
        });
        infoLabel.setToolTipText(varsText);

    }

    public CLabel getInfoLabel() {
        return infoLabel;
    }

    public void setResolver(IVariableResolver resolver) {
        this.resolver = resolver;
    }

    class VariableListDialog extends BaseDialog {

        VariableListDialog(Shell parentShell, String title) {
            super(parentShell, title, null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite composite = super.createDialogArea(parent);

            Table table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
            table.setHeaderVisible(true);
            table.setLayoutData(new GridData(GridData.FILL_BOTH));
            UIUtils.createTableColumn(table, SWT.LEFT, "Variable");
            UIUtils.createTableColumn(table, SWT.LEFT, "Description");
            UIUtils.createTableColumn(table, SWT.LEFT, "Value");

            for (String[] var : variables) {
                String varName = var[0];
                boolean isSecure = DBPConnectionConfiguration.VARIABLE_PASSWORD.equals(varName);
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText(0, GeneralUtils.variablePattern(varName));
                item.setText(1, var[1]);
                item.setText(2, isSecure ? "********" : CommonUtils.notEmpty(resolver.get(varName)));
            }
            UIUtils.packColumns(table);
            UIUtils.setControlContextMenu(table, manager -> UIUtils.fillDefaultTableContextMenu(manager, table));

            TableEditor tableEditor = new TableEditor(table);

            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }
    }

}