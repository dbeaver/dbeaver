/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
