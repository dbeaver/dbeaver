/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.ui.internal.ExasolMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;


public class ExasolConnectionDialog extends BaseDialog {

    private String name = "";
    private String user = "";
    private String password = "";
    private String url = "";
    private String comment = "";

    
    public ExasolConnectionDialog(Shell parentShell, ExasolDataSource datasource)
    {
        super(parentShell, ExasolMessages.dialog_create_connection_title,null);
    }
    
    

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        final Composite composite = super.createDialogArea(parent);
        
        final Composite group = new Composite(composite, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 250;
        gd.heightHint = 200;
        gd.verticalIndent = 0;
        gd.horizontalIndent = 0;
        group.setLayoutData(gd);
        group.setLayout(new GridLayout(2, true));  
        group.setLayout(new GridLayout(2, false));
        final Text nameText = UIUtils.createLabelText(group, ExasolMessages.dialog_create_connection_connection_name, "");
        final Text urlText = UIUtils.createLabelText(group,ExasolMessages.dialog_create_connection_connection_url, "");
        final Text commentText = UIUtils.createLabelText(group,ExasolMessages.dialog_create_connection_description, "");


        Button saveCred = UIUtils.createCheckbox(group, ExasolMessages.dialog_create_connection_provide_credentials,ExasolMessages.dialog_create_connection_provide_credentials_tip, false, 2);
        Text userText = UIUtils.createLabelText(group, ExasolMessages.dialog_create_connection_user, "");
        userText.setEnabled(false);
        Text passwordText = UIUtils.createLabelText(group, ExasolMessages.dialog_create_connection_password, "", SWT.BORDER | SWT.PASSWORD);
        passwordText.setEnabled(false);

        
        ModifyListener mod = e -> {
            name = nameText.getText().trim();
            user = userText.getText().trim();
            url  = urlText.getText().trim();
            password = passwordText.getText();
            comment = commentText.getText();
            //enable/disable OK button
            if (
                    (
                        saveCred.getSelection() &
                        (
                            CommonUtils.isEmpty(user) |
                            CommonUtils.isEmpty(password)
                        )
                    )
                    | name.isEmpty()
                    | url.isEmpty()
                )
            {
                getButton(IDialogConstants.OK_ID).setEnabled(false);
            } else {
                getButton(IDialogConstants.OK_ID).setEnabled(true);
            }
        };
        
        nameText.addModifyListener(mod);
        userText.addModifyListener(mod);
        urlText.addModifyListener(mod);
        passwordText.addModifyListener(mod);
        commentText.addModifyListener(mod);
        saveCred.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                userText.setEnabled(saveCred.getSelection());
                passwordText.setEnabled(saveCred.getSelection());
            }
            
        });
        
        return composite;
    }
    
    public String getName()
    {
        return name;
    }


    public String getUser()
    {
        return user;
    }


    public String getPassword()
    {
        return password;
    }
    

    public String getUrl()
    {
        return url;
    }

    public String getComment()
    {
        return comment;
    }

    
    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

}
