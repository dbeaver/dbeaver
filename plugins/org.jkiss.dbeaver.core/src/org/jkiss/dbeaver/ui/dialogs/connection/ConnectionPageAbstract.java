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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.ui.UIUtils;
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

        if (savePasswordCheck != null) {
            DataSourceDescriptor dataSource = (DataSourceDescriptor) getSite().getActiveDataSource();
            if (dataSource != null) {
                savePasswordCheck.setSelection(dataSource.isSavePassword());
            } else {
                savePasswordCheck.setSelection(true);
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
        int numColumns = ((GridLayout) parent.getLayout()).numColumns;

        Composite panel = UIUtils.createPlaceholder(parent, 4, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = numColumns;
        panel.setLayoutData(gd);

        {
            Composite placeholder = UIUtils.createPlaceholder(panel, 2);
            gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_END);
            gd.horizontalSpan = 4;
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
            placeholder.setLayoutData(gd);

            if (DBeaverCore.getGlobalPreferenceStore().getBoolean(ModelPreferences.CONNECT_USE_ENV_VARS)) {
                CLabel infoLabel = UIUtils.createInfoLabel(placeholder, CoreMessages.dialog_connection_edit_connection_settings_variables_hint_label);
                gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_END);
                gd.grabExcessHorizontalSpace = true;
                infoLabel.setLayoutData(gd);
                infoLabel.setToolTipText("You can use OS environment variables in connection parameters.\nUse ${variable} patterns.");
            } else {
                Control label = UIUtils.createEmptyLabel(placeholder, 1, 1);
                gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_END);
                gd.grabExcessHorizontalSpace = true;
                label.setLayoutData(gd);
            }

            Composite linksComposite = UIUtils.createPlaceholder(placeholder, 1, 2);
            linksComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            if (!site.getDriver().isEmbedded()) {
                Link netConfigLink = new Link(linksComposite, SWT.NONE);
                netConfigLink.setText("<a>" + CoreMessages.dialog_connection_edit_wizard_conn_conf_network_link + "</a>");
                netConfigLink.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        site.openSettingsPage(ConnectionPageNetwork.PAGE_NAME);
                    }
                });
                netConfigLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            }
            {
                Link netConfigLink = new Link(linksComposite, SWT.NONE);
                netConfigLink.setText("<a>" + CoreMessages.dialog_connection_edit_wizard_conn_conf_general_link + "</a>");
                netConfigLink.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        site.openSettingsPage(ConnectionPageGeneral.PAGE_NAME);
                    }
                });
                netConfigLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            }

        }

        Label divLabel = new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 4;
        divLabel.setLayoutData(gd);

        Label driverLabel = new Label(panel, SWT.NONE);
        driverLabel.setText(CoreMessages.dialog_connection_driver);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        driverLabel.setLayoutData(gd);

        driverText = new Text(panel, SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        //gd.widthHint = 200;
        driverText.setLayoutData(gd);

        Button driverButton = new Button(panel, SWT.PUSH);
        driverButton.setText(CoreMessages.dialog_connection_edit_driver_button);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        driverButton.setLayoutData(gd);
        driverButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (site.openDriverEditor()) {
                    updateDriverInfo(site.getDriver());
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });
    }

    protected void updateDriverInfo(DBPDriver driver) {

    }

    protected void createSavePasswordButton(Composite parent) {
        createSavePasswordButton(parent, 1);
    }

    protected void createSavePasswordButton(Composite parent, int hSpan) {
        DataSourceDescriptor dataSource = (DataSourceDescriptor)getSite().getActiveDataSource();
        savePasswordCheck = UIUtils.createCheckbox(parent,
            CoreMessages.dialog_connection_wizard_final_checkbox_save_password_locally,
            dataSource == null || dataSource.isSavePassword());
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        if (hSpan > 1) {
            gd.horizontalSpan = hSpan;
        }
        savePasswordCheck.setLayoutData(gd);
    }

}
