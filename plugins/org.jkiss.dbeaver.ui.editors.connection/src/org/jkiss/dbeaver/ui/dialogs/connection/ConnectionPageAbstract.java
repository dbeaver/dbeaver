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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSecurity;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * ConnectionPageAbstract
 */

public abstract class ConnectionPageAbstract extends DialogPage implements IDataSourceConnectionEditor
{
    protected IDataSourceConnectionEditorSite site;
    // Driver name
    protected Text driverText;
    protected Button savePasswordCheck;
    protected ToolBar userManagementToolbar;
    private VariablesHintLabel variablesHintLabel;

    public IDataSourceConnectionEditorSite getSite() {
        return site;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void setSite(IDataSourceConnectionEditorSite site)
    {
        this.site = site;
    }

    protected boolean isCustomURL()
    {
        return false;
    }

    @Override
    public void loadSettings() {
        DBPDriver driver = site.getDriver();
        if (driver != null && driverText != null) {
            driverText.setText(CommonUtils.toString(driver.getFullName()));
        }

        DataSourceDescriptor dataSource = (DataSourceDescriptor) getSite().getActiveDataSource();

        if (savePasswordCheck != null) {
            if (dataSource != null) {
                savePasswordCheck.setSelection(dataSource.isSavePassword());
            } else {
                savePasswordCheck.setSelection(true);
            }
        }

        if (variablesHintLabel != null) {
            if (dataSource != null) {
                variablesHintLabel.setResolver(new DataSourceVariableResolver(dataSource, dataSource.getConnectionConfiguration()));
            } else {
                variablesHintLabel.setResolver(null);
            }
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        saveConnectionURL(dataSource.getConnectionConfiguration());
        if (savePasswordCheck != null) {
            DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSource;
            descriptor.setSavePassword(savePasswordCheck.getSelection());

            if (!descriptor.isSavePassword()) {
                descriptor.resetPassword();
            }
        }
    }

    protected void saveConnectionURL(DBPConnectionConfiguration connectionInfo)
    {
        if (!isCustomURL()) {
            connectionInfo.setUrl(
                site.getDriver().getDataSourceProvider().getConnectionURL(
                    site.getDriver(),
                    connectionInfo));
        }
    }

    protected void createDriverPanel(Composite parent) {

        Composite panel = UIUtils.createComposite(parent, 4);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END);
        gd.horizontalSpan = ((GridLayout) parent.getLayout()).numColumns;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        panel.setLayoutData(gd);

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.CONNECT_USE_ENV_VARS)) {
            variablesHintLabel = new VariablesHintLabel(panel,
                UIConnectionMessages.dialog_connection_edit_connection_settings_variables_hint_label,
                UIConnectionMessages.dialog_connection_edit_connection_settings_variables_hint_label,
                DataSourceDescriptor.CONNECT_VARIABLES,
                false);
            ((GridData)variablesHintLabel.getInfoLabel().getLayoutData()).horizontalSpan = site.isNew() ? 3 : 4;
        } else {
            UIUtils.createEmptyLabel(panel, 3, 1);
        }

        if (site.isNew()) {
            Button advSettingsButton = UIUtils.createDialogButton(panel, UIConnectionMessages.dialog_connection_edit_wizard_conn_conf_general_link, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    site.openSettingsPage("ConnectionPageGeneral");
                }
            });
            advSettingsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        }

        Label divLabel = new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 4;
        divLabel.setLayoutData(gd);

        Label driverLabel = new Label(panel, SWT.NONE);
        driverLabel.setText(UIConnectionMessages.dialog_connection_driver);
        driverLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        driverText = new Text(panel, SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        //gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        //gd.widthHint = 200;
        driverText.setLayoutData(gd);

        Button driverButton = UIUtils.createDialogButton(panel, UIConnectionMessages.dialog_connection_edit_driver_button, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (site.openDriverEditor()) {
                    updateDriverInfo(site.getDriver());
                }
            }
        });
        driverButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    }

    protected void updateDriverInfo(DBPDriver driver) {

    }

    protected void createPasswordControls(Composite parent, Text passwordText) {
        createPasswordControls(parent, passwordText, 1);
    }

    protected void createPasswordControls(Composite parent, Text passwordText, int hSpan) {
        // We don't support password preview in standard project secure storage (as we need password encryption)
        UIServiceSecurity serviceSecurity = DBWorkbench.getService(UIServiceSecurity.class);
        boolean supportsPasswordView = serviceSecurity != null;

        Composite panel = UIUtils.createComposite(parent, supportsPasswordView ? 2 : 1);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        if (hSpan > 1) {
            gd.horizontalSpan = hSpan;
        }
        panel.setLayoutData(gd);

        DataSourceDescriptor dataSource = (DataSourceDescriptor)getSite().getActiveDataSource();
        savePasswordCheck = UIUtils.createCheckbox(panel,
            UIConnectionMessages.dialog_connection_wizard_final_checkbox_save_password_locally,
            dataSource == null || dataSource.isSavePassword());
        savePasswordCheck.setToolTipText(UIConnectionMessages.dialog_connection_wizard_final_checkbox_save_password_locally);
        //savePasswordCheck.setLayoutData(gd);

        if (supportsPasswordView) {
            userManagementToolbar = new ToolBar(panel, SWT.HORIZONTAL);
            ToolItem showPasswordLabel = new ToolItem(userManagementToolbar, SWT.NONE);
            showPasswordLabel.setToolTipText("Show password on screen");
            showPasswordLabel.setImage(DBeaverIcons.getImage(UIIcon.SHOW_ALL_DETAILS));
            showPasswordLabel.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showPasswordText(serviceSecurity, passwordText);
                }
            });
        }

    }

    private void showPasswordText(UIServiceSecurity serviceSecurity, Text passwordText) {
        if (passwordText.getEchoChar() == '\0') {
            passwordText.setEchoChar('*');
            return;
        }
        if (serviceSecurity.validatePassword(site.getProject().getSecureStorage(), "Enter project password", "Enter project master password to unlock connection password view")) {
            passwordText.setEchoChar('\0');
        }
    }

}
