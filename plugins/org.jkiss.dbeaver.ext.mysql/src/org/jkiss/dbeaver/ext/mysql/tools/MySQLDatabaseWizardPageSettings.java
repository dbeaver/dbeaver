/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.tools;

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
import org.jkiss.dbeaver.registry.encode.EncryptionException;
import org.jkiss.dbeaver.registry.encode.SecuredPasswordEncrypter;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.BaseAuthDialog;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizardPage;


public abstract class MySQLDatabaseWizardPageSettings<WIZARD extends AbstractToolWizard> extends AbstractToolWizardPage<WIZARD>
{

    public MySQLDatabaseWizardPageSettings(WIZARD wizard, String title)
    {
        super(wizard, title);
    }

    public void createSecurityGroup(Composite parent)
    {
        try {
            final SecuredPasswordEncrypter encrypter = new SecuredPasswordEncrypter();
            final DBPConnectionConfiguration connectionInfo = wizard.getConnectionInfo();
            final String authProperty = DBConstants.INTERNAL_PROP_PREFIX + "-auth-" + wizard.getDatabaseObject().getName() + "@";
            String authUser = null;
            String authPassword = null;
            {
                Object authValue = connectionInfo.getProperty(authProperty);
                if (authValue != null) {
                    String authCredentials = encrypter.decrypt(authValue.toString());
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
                ") for database '" + wizard.getDatabaseObject().getName() + "'.\nExternal tools like 'mysqldump' may require different set of permissions.");
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            infoLabel.setLayoutData(gd);
            Button authButton = new Button(securityGroup, SWT.PUSH);
            authButton.setText("Authentication");
            authButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    BaseAuthDialog authDialog = new BaseAuthDialog(getShell(), "Authentication");
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
                public void widgetSelected(SelectionEvent e)
                {
                    connectionInfo.getProperties().remove(authProperty);
                    wizard.setToolUserName(connectionInfo.getUserName());
                    wizard.setToolUserPassword(connectionInfo.getUserPassword());
                }
            });
        } catch (EncryptionException e) {
            // Never be here
        }
    }

}
