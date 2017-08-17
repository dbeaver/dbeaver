/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
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
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;


public class ExasolUserDialog extends BaseDialog {

    private String name = "";
    private String password = "";
    private String ldapDN = "";
    private String comment = "";

    
    public ExasolUserDialog(Shell parentShell, ExasolDataSource datasource)
    {
        super(parentShell,"Create Connection",null);
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
        final Text nameText = UIUtils.createLabelText(group, "User Name", "");

        final Text commentText = UIUtils.createLabelText(group,"Comment", "");
        
        Text passwordText = UIUtils.createLabelText(group, "Password", "", SWT.PASSWORD);
        Button ldapDNCheck = UIUtils.createCheckbox(group, "LDAP User", "Create LDAP User", false, 2);
        final Text urlText = UIUtils.createLabelText(group,"LDAP DN", "");
        urlText.setEnabled(false);

        
        ModifyListener mod = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText();
                ldapDN  = urlText.getText();
                password = passwordText.getText();
                comment = commentText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
                //enable/disable OK button   
                if (
                        (
                            ldapDNCheck.getSelection() & CommonUtils.isEmpty(ldapDN) 
                        ) 
                        | name.isEmpty() | (!ldapDNCheck.getSelection() & CommonUtils.isEmpty(password) )
                    )
                {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                } else {
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                }
            }
        };
        
        nameText.addModifyListener(mod);
        passwordText.addModifyListener(mod);
        urlText.addModifyListener(mod);
        ldapDNCheck.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e)
            {
            	if (ldapDNCheck.getSelection()) {
                	passwordText.setText("");
            	} else {
                	urlText.setText("");
            	}
            	passwordText.setEnabled(!ldapDNCheck.getSelection());
            	urlText.setEnabled(ldapDNCheck.getSelection());
            }
            
        });
        
        return composite;
    }
    
    public String getName()
    {
        return name;
    }


    public String getPassword()
    {
        return password;
    }
    

    public String getLDAPDN()
    {
        return ldapDN;
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
