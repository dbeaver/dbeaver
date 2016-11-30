/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
        textControl.setText(text);
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
