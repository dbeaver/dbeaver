/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2017 Karl Griesser (fullref@gmail.com)
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

package org.jkiss.dbeaver.ext.exasol.ui;


import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;


public class ExasolUserQueryPassword extends BaseDialog {

    private String password;

    
    public ExasolUserQueryPassword(Shell parentShell) {
        super(parentShell,"Query old Password",null);
    }
    
    @Override
    protected Composite createDialogArea(Composite parent)
    {
        final Composite composite = super.createDialogArea(parent);
        
        final Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new GridLayout(2, true));
        final Text passwordText = UIUtils.createLabelText(group, "Old Password", "", SWT.PASSWORD);
        passwordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                password = passwordText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(!password.isEmpty());
            }
        });

        return composite;


    }
    
    public String getPassword() {
        return password;
    }
    
    
    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
    
}
