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
package org.jkiss.dbeaver.ext.exasol.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.exasol.Activator;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class ExasolConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage {

    private Label backupHostLabel;

    public ExasolConnectionPage() {
    }

    private Text hostText;
    private Text backupHostText;
    private Text portText;
    private Text usernameText;
    private Text passwordText;
    private ClientHomesSelector homesSelector;
    private Button useBackupHostList;
    private boolean showBackupHosts = false;
    private Button encryptCommunication;

    private static ImageDescriptor EXASOL_LOGO_IMG = Activator.getImageDescriptor("icons/exasol.png");


    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite) {
        setImageDescriptor(EXASOL_LOGO_IMG);

        Composite control = new Composite(composite, SWT.NONE);
        control.setLayout(new GridLayout(1, false));
        control.setLayoutData(new GridData(GridData.FILL_BOTH));
        ModifyListener textListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                evaluateURL();
            }
        };
        {
            Composite addrGroup = UIUtils.createControlGroup(control, "Database", 2, 0, 0);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            addrGroup.setLayoutData(gd);

            Label hostLabel = UIUtils.createControlLabel(addrGroup, "Host List");
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);

            backupHostLabel = UIUtils.createControlLabel(addrGroup, "Backup Host List");
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            backupHostLabel.setLayoutData(gd);
            backupHostLabel.setEnabled(showBackupHosts);

            backupHostText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            backupHostText.setLayoutData(gd);
            backupHostText.addModifyListener(textListener);


            useBackupHostList = UIUtils.createLabelCheckbox(addrGroup, "Use Backup Host List", showBackupHosts);

            useBackupHostList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    backupHostLabel.setEnabled(useBackupHostList.getSelection());
                    backupHostText.setEnabled(useBackupHostList.getSelection());

                    //reset text if disabled
                    if (!useBackupHostList.getSelection())
                        backupHostText.setText("");
                }
            });

            Label portLabel = UIUtils.createControlLabel(addrGroup, ExasolMessages.dialog_connection_port);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            portLabel.setLayoutData(gd);


            portText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            gd.widthHint = 40;
            portText.setLayoutData(gd);
            portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            portText.addModifyListener(textListener);

            encryptCommunication = UIUtils.createLabelCheckbox(addrGroup, "Encrypt Communication", false);


        }

        {
            Composite addrGroup = UIUtils.createControlGroup(control, "Security", 2, 0, 0);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            addrGroup.setLayoutData(gd);
            Label usernameLabel = UIUtils.createControlLabel(addrGroup, ExasolMessages.dialog_connection_user_name);
            usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            usernameText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = 200;
            usernameText.setLayoutData(gd);
            usernameText.addModifyListener(textListener);

            Label passwordLabel = UIUtils.createControlLabel(addrGroup, ExasolMessages.dialog_connection_password);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            Composite passPH = UIUtils.createPlaceholder(addrGroup, 2, 5);
            passwordText = new Text(passPH, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = 200;
            passwordText.setLayoutData(gd);
            passwordText.addModifyListener(textListener);

            createSavePasswordButton(passPH);
        }

        createDriverPanel(control);
        setControl(control);
    }

    @Override
    public boolean isComplete() {
        return hostText != null && portText != null &&
            !CommonUtils.isEmpty(hostText.getText()) &&
            !CommonUtils.isEmpty(portText.getText());
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText("");
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(String.valueOf(connectionInfo.getHostPort()));
            } else if (site.getDriver().getDefaultPort() != null) {
                portText.setText(site.getDriver().getDefaultPort());
            } else {
                portText.setText("8563");
            }
        }
        if (usernameText != null) {
            usernameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }

        String backupHostText = connectionInfo.getProviderProperty(ExasolConstants.DRV_BACKUP_HOST_LIST);

        if (!CommonUtils.isEmpty(backupHostText)) {
            this.backupHostLabel.setEnabled(true);
            this.backupHostText.setText(backupHostText);
            this.backupHostText.setEnabled(true);
            this.useBackupHostList.setSelection(true);
        } else {
            this.backupHostLabel.setEnabled(false);
            this.backupHostText.setEnabled(false);
            this.useBackupHostList.setSelection(false);
        }

        String encryptComm = connectionInfo.getProviderProperty(ExasolConstants.DRV_ENCRYPT);

        if (encryptComm != null) {
            if (encryptComm.equals("1"))
                this.encryptCommunication.setEnabled(true);
        }

    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText().trim());
        }
        if (portText != null) {
            connectionInfo.setHostPort(portText.getText().trim());
        }
        if (usernameText != null) {
            connectionInfo.setUserName(usernameText.getText().trim());
        }
        if (passwordText != null) {
            connectionInfo.setUserPassword(passwordText.getText());
        }
        if (homesSelector != null) {
            connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
        }

        connectionInfo.setProviderProperty(ExasolConstants.DRV_BACKUP_HOST_LIST, backupHostText.getText());
        if (this.encryptCommunication.getSelection())
            connectionInfo.setProviderProperty(ExasolConstants.DRV_ENCRYPT, "1");

        super.saveSettings(dataSource);
    }

    private void evaluateURL() {
        site.updateButtons();
    }

    @Override
    public IDialogPage[] getSubPages() {
        return new IDialogPage[]{
            new DriverPropertiesDialogPage(this)
        };
    }


}
