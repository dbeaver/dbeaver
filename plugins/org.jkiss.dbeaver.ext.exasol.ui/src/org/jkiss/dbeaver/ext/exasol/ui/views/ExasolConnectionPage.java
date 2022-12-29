/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolUIConstants;
import org.jkiss.dbeaver.ext.exasol.ui.internal.ExasolMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class ExasolConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {

    private Label backupHostLabel;
    private Text hostText;
    private Text backupHostText;
    private Text portText;
    private Button useBackupHostList;
    private boolean showBackupHosts = false;
    private Button encryptCommunication;
    private Button useLegacyEncryption;

    private Image logoImage;

    public ExasolConnectionPage() {
        logoImage = createImage("icons/exasol.png"); //$NON-NLS-1$
    }

    @Override
    public void dispose() {
        super.dispose();
        UIUtils.dispose(logoImage);
    }

    @Override
    public Image getImage() {
        return logoImage;
    }

    @Override
    public void createControl(Composite composite) {
        Composite control = new Composite(composite, SWT.NONE);
        control.setLayout(new GridLayout(1, false));
        control.setLayoutData(new GridData(GridData.FILL_BOTH));
        ModifyListener textListener = e -> evaluateURL();
        {
            Composite addrGroup = UIUtils.createControlGroup(control, ExasolMessages.label_database, 2, 0, 0);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            addrGroup.setLayoutData(gd);

            hostText = UIUtils.createLabelText(addrGroup, ExasolMessages.label_host_list, null, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);

            backupHostLabel = UIUtils.createControlLabel(addrGroup, ExasolMessages.label_backup_host_list);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            backupHostLabel.setLayoutData(gd);
            backupHostLabel.setEnabled(showBackupHosts);

            Composite bhPlaceholder = UIUtils.createComposite(addrGroup, 2);
            bhPlaceholder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            backupHostText = new Text(bhPlaceholder, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            backupHostText.setLayoutData(gd);
            backupHostText.addModifyListener(textListener);

            useBackupHostList = UIUtils.createCheckbox(bhPlaceholder, ExasolMessages.label_use_backup_host_list, null, showBackupHosts, 1);
            useBackupHostList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    backupHostLabel.setEnabled(useBackupHostList.getSelection());
                    backupHostText.setEnabled(useBackupHostList.getSelection());

                    //reset text if disabled
                    if (!useBackupHostList.getSelection())
                        backupHostText.setText(""); //$NON-NLS-1$
                }
            });

            portText = UIUtils.createLabelText(addrGroup, ExasolMessages.dialog_connection_port, null, SWT.BORDER);
            gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(portText) * 7;
            portText.setLayoutData(gd);
            portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            portText.addModifyListener(textListener);

            encryptCommunication = UIUtils.createCheckbox(addrGroup, ExasolMessages.label_encrypt, null, false, 2);

            useLegacyEncryption = UIUtils.createCheckbox(
                addrGroup,
                ExasolMessages.connection_page_checkbox_legacy_encrypt,
                ExasolMessages.connection_page_checkbox_legacy_encrypt_tip,
                false,
                2);
        }

        createAuthPanel(control, 1);
        createDriverPanel(control);
        setControl(control);
    }

    @Override
    public boolean isComplete() {
        return super.isComplete() &&
            hostText != null && portText != null &&
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
                hostText.setText(""); //$NON-NLS-1$
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(connectionInfo.getHostPort());
            } else if (site.getDriver().getDefaultPort() != null) {
                portText.setText(site.getDriver().getDefaultPort());
            } else {
                portText.setText("8563"); //$NON-NLS-1$
            }
        }

        String backupHostText = connectionInfo.getProviderProperty(ExasolUIConstants.DRV_BACKUP_HOST_LIST);

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

        String encryptComm = connectionInfo.getProviderProperty(ExasolUIConstants.DRV_ENCRYPT);

        if ("1".equals(encryptComm)) {
            this.encryptCommunication.setEnabled(true);
        }

        useLegacyEncryption.setSelection(CommonUtils.toBoolean(
            connectionInfo.getProviderProperty(ExasolConstants.DRV_USE_LEGACY_ENCRYPTION)));
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

        connectionInfo.setProviderProperty(ExasolUIConstants.DRV_BACKUP_HOST_LIST, backupHostText.getText());
        if (this.encryptCommunication.getSelection())
            connectionInfo.setProviderProperty(ExasolUIConstants.DRV_ENCRYPT, "1"); //$NON-NLS-1$

        connectionInfo.setProviderProperty(ExasolConstants.DRV_USE_LEGACY_ENCRYPTION,
            String.valueOf(useLegacyEncryption.getSelection()));

        super.saveSettings(dataSource);
    }

    private void evaluateURL() {
        site.updateButtons();
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[]{
            new DriverPropertiesDialogPage(this)
        };
    }


}
