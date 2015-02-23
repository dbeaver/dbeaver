/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class EditTextDialog extends BaseDialog {

    private String text;
    private Text textControl;
    private int textWidth = 300;
    private int textHeight = 200;
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
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

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

        return parent;
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
