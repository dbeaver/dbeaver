/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.BaseAuthDialog;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizardPage;


public abstract class PostgreWizardPageSettings<WIZARD extends AbstractToolWizard> extends AbstractToolWizardPage<WIZARD>
{

    public PostgreWizardPageSettings(WIZARD wizard, String title)
    {
        super(wizard, title);
    }

    public void createSecurityGroup(Composite parent)
    {
        try {
            final SecuredPasswordEncrypter encrypter = new SecuredPasswordEncrypter();
            final DBPConnectionConfiguration connectionInfo = wizard.getConnectionInfo();
            final String authProperty = DBConstants.INTERNAL_PROP_PREFIX + "-auth-" + wizard.getObjectsName() + "@";
            String authUser = null;
            String authPassword = null;
            {
                String authValue = connectionInfo.getProviderProperty(authProperty);
                if (authValue != null) {
                    String authCredentials = encrypter.decrypt(authValue);
                    int divPos = authCredentials.indexOf(':');
                    if (divPos != -1) {
                        authUser = authCredentials.substring(0, divPos);
                        authPassword = authCredentials.substring(divPos + 1);
                    }
                }
            }

            wizard.setToolUserName(authUser == null ? connectionInfo.getUserName() : authUser);
            wizard.setToolUserPassword(authPassword == null ? connectionInfo.getUserPassword() : authPassword);
            final boolean savePassword = authUser != null;
            Group securityGroup = UIUtils.createControlGroup(
                parent, PostgreMessages.wizard_backup_page_setting_group_security, 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
            Label infoLabel = new Label(securityGroup, SWT.NONE);
            infoLabel.setText(NLS.bind(PostgreMessages.wizard_backup_page_setting_group_security_label_info, wizard.getConnectionInfo().getUserName(),
           		 wizard.getObjectsName()));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            infoLabel.setLayoutData(gd);
            Button authButton = new Button(securityGroup, SWT.PUSH);
            authButton.setText(PostgreMessages.wizard_backup_page_setting_group_security_btn_authentication);
            authButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    BaseAuthDialog authDialog = new BaseAuthDialog(getShell(), PostgreMessages.wizard_backup_page_setting_group_security_btn_authentication, false);
                    authDialog.setUserName(wizard.getToolUserName());
                    authDialog.setUserPassword(wizard.getToolUserPassword());
                    authDialog.setSavePassword(savePassword);
                    if (authDialog.open() == IDialogConstants.OK_ID) {
                        wizard.setToolUserName(authDialog.getUserName());
                        wizard.setToolUserPassword(authDialog.getUserPassword());
                        if (authDialog.isSavePassword()) {
                            try {
                                connectionInfo.setProviderProperty(
                                    authProperty,
                                    encrypter.encrypt(wizard.getToolUserName() + ':' + wizard.getToolUserPassword()));
                            } catch (EncryptionException e1) {
                                // Never be here
                            }
                        }
                    }
                }
            });

            Button resetButton = new Button(securityGroup, SWT.PUSH);
            resetButton.setText(PostgreMessages.wizard_backup_page_setting_group_security_btn_reset_default);
            resetButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    connectionInfo.getProviderProperties().remove(authProperty);
                    wizard.setToolUserName(connectionInfo.getUserName());
                    wizard.setToolUserPassword(connectionInfo.getUserPassword());
                }
            });
        } catch (EncryptionException e) {
            // Never be here
        }
    }

}
