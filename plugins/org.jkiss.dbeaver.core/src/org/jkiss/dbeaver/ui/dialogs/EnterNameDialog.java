/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;

public class EnterNameDialog extends Dialog {

    private String propertyName;
    private String propertyValue;
    private Text propNameText;
    private String result;

    public EnterNameDialog(Shell parentShell, String propertyName, String propertyValue)
    {
        super(parentShell);
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    public String getResult()
    {
        return result;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(propertyName);

        Composite propGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        propGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        propGroup.setLayoutData(gd);

        propNameText = UIUtils.createLabelText(propGroup, propertyName, null);
        if (propertyValue != null) {
            propNameText.setText(propertyValue);
            propNameText.selectAll();
        }

        return parent;
    }

    @Override
    protected void okPressed()
    {
        result = propNameText.getText();
        super.okPressed();
    }

    public String chooseName()
    {
        if (open() == IDialogConstants.OK_ID) {
            return result;
        } else {
            return null;
        }
    }

    public static String chooseName(Shell parentShell, String propertyName)
    {
        return chooseName(parentShell, propertyName, null);
    }

    public static String chooseName(Shell parentShell, String propertyName, String propertyValue)
    {
        EnterNameDialog dialog = new EnterNameDialog(parentShell, propertyName, propertyValue);
        return dialog.chooseName();
    }
}
