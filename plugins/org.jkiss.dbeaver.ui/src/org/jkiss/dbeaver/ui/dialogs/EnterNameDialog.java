/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
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

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return null;//UIUtils.getDialogSettings("DBeaver.EnterNameDialog"); //$NON-NLS-1$
    }

    public String getResult()
    {
        return result;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        getShell().setText(propertyName);

        Composite propGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        propGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        propGroup.setLayoutData(gd);

        propNameText = UIUtils.createLabelText(propGroup, propertyName, null);
        if (propertyValue != null) {
            propNameText.setText(propertyValue);
            propNameText.selectAll();
        }

        return propGroup;
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
