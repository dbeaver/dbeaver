/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.tools;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.runtime.encode.EncryptionException;
import org.jkiss.dbeaver.runtime.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.BaseAuthDialog;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizardPage;

@SuppressWarnings("rawtypes")
public class ExasolWizardPageSettings<WIZARD extends AbstractToolWizard> extends AbstractToolWizardPage<WIZARD> {


    public ExasolWizardPageSettings(WIZARD wizard, String title) {
        super(wizard, title);
    }

    public void createSecurityGroup(Composite parent) {
        try {
            final SecuredPasswordEncrypter encrypter = new SecuredPasswordEncrypter();
            final DBPConnectionConfiguration connectionInfo = wizard.getConnectionInfo();
            final String authProperty = DBConstants.INTERNAL_PROP_PREFIX + "-auth-" + wizard.getObjectsName() + "@";
            String authUser = null;
            String authPassword = null;
            {
                String authValue = connectionInfo.getProperty(authProperty);
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
                parent, "Security", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
            Label infoLabel = new Label(securityGroup, SWT.NONE);
            infoLabel.setText("Override user credentials (" + wizard.getConnectionInfo().getUserName() +
                ") for objects '" + wizard.getObjectsName() + "'.\nExternal tools like 'mysqldump' may require different set of permissions.");
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            infoLabel.setLayoutData(gd);
            Button authButton = new Button(securityGroup, SWT.PUSH);
            authButton.setText("Authentication");
            authButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    BaseAuthDialog authDialog = new BaseAuthDialog(getShell(), "Authentication", false, true);
                    authDialog.setUserName(wizard.getToolUserName());
                    authDialog.setUserPassword(wizard.getToolUserPassword());
                    authDialog.setSavePassword(savePassword);
                    if (authDialog.open() == IDialogConstants.OK_ID) {
                        wizard.setToolUserName(authDialog.getUserName());
                        wizard.setToolUserPassword(authDialog.getUserPassword());
                        if (authDialog.isSavePassword()) {
                            try {
                                connectionInfo.setProperty(
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
            resetButton.setText("Reset to default");
            resetButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    connectionInfo.getProperties().remove(authProperty);
                    wizard.setToolUserName(connectionInfo.getUserName());
                    wizard.setToolUserPassword(connectionInfo.getUserPassword());
                }
            });
        } catch (EncryptionException e) {
            // Never be here
        }
    }

    @Override
    public void createControl(Composite arg0) {
        // TODO Auto-generated method stub

    }

}


