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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;

public class AcceptLicenseDialog extends Dialog {

    private final String title;
    private final String license;

    public AcceptLicenseDialog(Shell parentShell, String title, String license)
    {
        super(parentShell);
        this.title = title;
        this.license = license;
    }

    @Override
    protected boolean isResizable() {
    	return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(title);

        Composite composite = (Composite) super.createDialogArea(parent);

        UIUtils.createControlLabel(composite, title);
        Text textControl = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        textControl.setText(license);
        textControl.setEditable(false);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 300;
        gd.minimumHeight = 100;
        gd.minimumWidth = 100;
        textControl.setLayoutData(gd);

        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.YES_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.NO_LABEL, false);
    }

    public static boolean acceptLicense(Shell parentShell, String title, String license)
    {
        AcceptLicenseDialog dialog = new AcceptLicenseDialog(parentShell, title, license);
        if (dialog.open() == IDialogConstants.OK_ID) {
            return true;
        } else {
            return false;
        }
    }

}
