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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class EditTextDialog extends BaseDialog {

    private String text;
    private Text textControl;
    protected int textWidth = 300;
    protected int textHeight = 200;
    private boolean readonly = false;

    public EditTextDialog(Shell parentShell, String title, String text)
    {
        super(parentShell, title, null);
        this.text = text;
    }

    public void setReadonly(boolean readonly)
    {
        this.readonly = readonly;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);
        createControlsBeforeText(composite);
        textControl = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        if (text != null) {
            textControl.setText(text);
        }
        textControl.setEditable(!readonly);
        GridData gd = new GridData(GridData.FILL_BOTH);
        if (textWidth > 0) {
            gd.widthHint = textWidth;
        }
        if (textHeight > 0) {
            gd.heightHint = textHeight;
        }
        gd.minimumHeight = 100;
        gd.minimumWidth = 100;
        textControl.setLayoutData(gd);

        return composite;
    }

    protected void createControlsBeforeText(Composite composite) {

    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		if (!readonly) {
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		}
    }

    @Override
    protected void okPressed()
    {
        text = textControl.getText();
        super.okPressed();
    }

    public static String editText(Shell parentShell, String title, String text)
    {
        EditTextDialog dialog = new EditTextDialog(parentShell, title, text);
        if (dialog.open() == IDialogConstants.OK_ID) {
            return dialog.text;
        } else {
            return null;
        }
    }

    public static void showText(Shell parentShell, String title, String text)
    {
        EditTextDialog dialog = new EditTextDialog(parentShell, title, text);
        dialog.setReadonly(true);
        dialog.open();
    }

}
