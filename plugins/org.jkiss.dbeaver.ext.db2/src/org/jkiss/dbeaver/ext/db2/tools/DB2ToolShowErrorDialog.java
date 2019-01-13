/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.tools;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Dialog to display the SQL message associated with an SQL Error Code
 * 
 * @author Denis Forveille
 * 
 */
class DB2ToolShowErrorDialog extends Dialog {

    private final DB2DataSource db2DataSource;

    private Text textSqlErrorCode;
    private Text resultMessage;

    public DB2ToolShowErrorDialog(IWorkbenchWindow window, DB2DataSource db2DataSource)
    {
        super(window.getShell());
        this.db2DataSource = db2DataSource;
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(DB2Messages.dialog_tools_msg_title);
    }

    @Override
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton)
    {
        // Disable all default buttons
        return null;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite area = (Composite) super.createDialogArea(parent);

        // -----------------------------------
        // Line 1: Label + input code + button
        // -----------------------------------
        Composite container1 = UIUtils.createPlaceholder(area, 3, 5);

        // SQL Error Code
        UIUtils.createLabel(container1, DB2Messages.dialog_tools_msg_code);

        textSqlErrorCode = new Text(container1, SWT.BORDER);

        // Button
        Button button = new Button(container1, SWT.PUSH);
        button.setText("Retrieve Message");
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Integer sqlIntegerCode = 0;
                try {
                    sqlIntegerCode = Integer.valueOf(textSqlErrorCode.getText());
                } catch (NumberFormatException nfe) {
                    DBWorkbench.getPlatformUI().showError(DB2Messages.dialog_tools_mes_error_code_title,
                        DB2Messages.dialog_tools_mes_error_code);
                    return;
                }

                try {
                    String msg = DB2Utils.getMessageFromCode(db2DataSource, sqlIntegerCode);
                    resultMessage.setText(msg);
                } catch (Exception e1) {
                    // Most propably, there is no message for this code. tell this to the user..
                    resultMessage.setText(e1.getMessage());
                }
            }
        });

        getShell().setDefaultButton(button);

        // -----------------------------------
        // Line 2: Label for Message
        // -----------------------------------

        UIUtils.createControlLabel(container1, DB2Messages.dialog_tools_mes_message);

        // -----------------------------------
        // Line 3: Message
        // -----------------------------------

        // Message
        resultMessage = new Text(container1, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        resultMessage.setLayoutData(new GridData(GridData.FILL_BOTH));
        resultMessage.setEditable(false);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.widthHint = 600;
        gd.heightHint = 80;
        resultMessage.setLayoutData(gd);

        return area;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }
}
