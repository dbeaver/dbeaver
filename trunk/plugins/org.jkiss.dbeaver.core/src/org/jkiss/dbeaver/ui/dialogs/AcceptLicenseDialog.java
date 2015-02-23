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
