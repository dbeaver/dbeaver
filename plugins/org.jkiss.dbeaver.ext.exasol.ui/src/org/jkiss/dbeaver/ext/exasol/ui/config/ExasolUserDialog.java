/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.exasol.ui.internal.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.ExasolUserType;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;


public class ExasolUserDialog extends BaseDialog {

    private String name = "";
    private String password = "";
    private String ldapDN = "";
    private String comment = "";
    private String kerberosPrincipal;
    private ExasolUserType selectedType;

    
    public ExasolUserDialog(Shell parentShell, ExasolDataSource datasource)
    {
        super(parentShell,"Create User",null);
    }
    
    

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        final Composite composite = super.createDialogArea(parent);
        
        final Composite group = new Composite(composite, SWT.NONE);
        final Composite groupText = new Composite(composite, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setLayout(new GridLayout(2, true));
        groupText.setLayoutData(new GridData(GridData.FILL_BOTH));
        groupText.setLayout(new GridLayout(2, true));
        final Text nameText = UIUtils.createLabelText(group, ExasolMessages.dialog_create_user_userid, "");

        final Text commentText = UIUtils.createLabelText(group,ExasolMessages.dialog_create_user_comment, "", SWT.BORDER | SWT.MULTI);
        String[] userTypes = new String[] {
        		ExasolMessages.dialog_create_user_kerberos,
        		ExasolMessages.dialog_create_user_ldap,
        		ExasolMessages.dialog_create_user_local
        		};
        final Text passwordText = UIUtils.createLabelText(groupText, ExasolMessages.dialog_create_user_local_password, "", SWT.BORDER | SWT.PASSWORD);
        final Text urlText = UIUtils.createLabelText(groupText,ExasolMessages.dialog_create_user_ldap_dn, "");
        final Text principalText = UIUtils.createLabelText(groupText,ExasolMessages.dialog_create_user_kerberos_principal, "");
		passwordText.setEnabled(false);
		urlText.setEnabled(false);
		principalText.setEnabled(false);
        int cnt = 0;

        for (ExasolUserType type: ExasolUserType.values())
        {
        	
	        UIUtils.createRadioButton(
	        		group,  
	        		userTypes[cnt], 
	        		type,
	        		SelectionListener.widgetSelectedAdapter(selectionEvent -> {
	        			selectedType = (ExasolUserType)selectionEvent.widget.getData();
	        			switch (selectedType) {
						case KERBEROS:
	        				passwordText.setEnabled(false);
	        				urlText.setEnabled(false);
	        				principalText.setEnabled(true);
							break;
						case LDAP:
	        				passwordText.setEnabled(false);
	        				urlText.setEnabled(true);
	        				principalText.setEnabled(false);
							break;
						case LOCAL:
	        				passwordText.setEnabled(true);
	        				urlText.setEnabled(false);
	        				principalText.setEnabled(false);
							break;
						default:
	        				passwordText.setEnabled(false);
	        				urlText.setEnabled(false);
	        				principalText.setEnabled(false);
						break;
						}
	        		}
	        		));
	        cnt++;
        }
        urlText.setEnabled(false);

        
        ModifyListener mod = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText();
                ldapDN  = urlText.getText();
                kerberosPrincipal = principalText.getText();
                password = passwordText.getText();
                comment = commentText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
                //enable/disable OK button
                
                if (
                		name.isEmpty() |
                		(selectedType == ExasolUserType.KERBEROS & kerberosPrincipal.isEmpty()) |
                		(selectedType == ExasolUserType.LDAP & ldapDN.isEmpty()) |
                		(selectedType == ExasolUserType.LOCAL & password.isEmpty()) 
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
        principalText.addModifyListener(mod);
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
    
    public String getKerberosPrincipal()
    {
    	return kerberosPrincipal;
    }
    

    public String getLDAPDN()
    {
        return ldapDN;
    }

    public String getComment()
    {
        return comment;
    }
    
    public ExasolUserType getUserType() {
    	return selectedType;
    }

    
    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }




}
