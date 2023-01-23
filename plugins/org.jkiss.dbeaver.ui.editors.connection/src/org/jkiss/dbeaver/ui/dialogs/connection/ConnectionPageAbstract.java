/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSecurity;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.dialogs.AcceptLicenseDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

/**
 * ConnectionPageAbstract
 */
public abstract class ConnectionPageAbstract extends DialogPage implements IDataSourceConnectionEditor {

    protected static final String GROUP_CONNECTION_MODE = "connectionMode"; //$NON-NLS-1$
    @NotNull
    protected final Map<String, List<Control>> propGroupMap = new HashMap<>();

    protected IDataSourceConnectionEditorSite site;
    // Driver name
    protected Text driverText;
    protected Text passwordText;
    protected Button savePasswordCheck;
    protected ToolBar userManagementToolbar;
    private VariablesHintLabel variablesHintLabel;
    @Nullable
    protected Button typeManualRadio;
    @Nullable
    protected Button typeURLRadio;

    private ImageDescriptor curImageDescriptor;
    private Button licenseButton;

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
        if (licenseButton != null) {
            UIUtils.setControlVisible(licenseButton, driver != null && !CommonUtils.isEmpty(driver.getLicense()));
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
                variablesHintLabel.setResolver(new DataSourceVariableResolver(dataSource,
                    dataSource.getConnectionConfiguration()));
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
                site.getDriver().getConnectionURL(connectionInfo));
        }
    }

    protected void createDriverPanel(Composite parent) {

        Composite panel = UIUtils.createComposite(parent, 5);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END);
        gd.horizontalSpan = ((GridLayout) parent.getLayout()).numColumns;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        panel.setLayoutData(gd);

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.CONNECT_USE_ENV_VARS)) {
            variablesHintLabel = new VariablesHintLabel(panel,
                UIConnectionMessages.dialog_connection_edit_connection_settings_variables_hint_label,
                UIConnectionMessages.dialog_connection_edit_connection_settings_variables_hint_label,
                DBPConnectionConfiguration.INTERNAL_CONNECT_VARIABLES,
                false);
            ((GridData)variablesHintLabel.getInfoLabel().getLayoutData()).horizontalSpan = site.isNew() ? 4 : 5;
        } else {
            UIUtils.createEmptyLabel(panel, 5, 1);
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
        gd.horizontalSpan = 5;
        divLabel.setLayoutData(gd);

        {
            Composite driverInfoComp = UIUtils.createComposite(panel, 5);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 5;
            driverInfoComp.setLayoutData(gd);

            Label driverLabel = new Label(driverInfoComp, SWT.NONE);
            driverLabel.setText(UIConnectionMessages.dialog_connection_driver);
            driverLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            driverText = new Text(driverInfoComp, SWT.READ_ONLY);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            //gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 2;
            //gd.widthHint = 200;
            driverText.setLayoutData(gd);

            if (DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_DRIVER_MANAGER)) {
                Button driverButton = UIUtils.createDialogButton(driverInfoComp, UIConnectionMessages.dialog_connection_edit_driver_button, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (site.openDriverEditor()) {
                            updateDriverInfo(site.getDriver());
                        }
                    }
                });
                driverButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            } else {
                UIUtils.createEmptyLabel(driverInfoComp, 1, 1);
            }

            {
                licenseButton = UIUtils.createDialogButton(driverInfoComp, UIConnectionMessages.dialog_edit_driver_text_driver_license, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String driverLicense = site.getDriver().getLicense();
                        if (CommonUtils.isEmpty(driverLicense)) {
                            driverLicense = "N/A";
                        }
                        AcceptLicenseDialog licenseDialog = new AcceptLicenseDialog(getShell(), site.getDriver().getFullName(), driverLicense);
                        licenseDialog.setViewMode(true);
                        licenseDialog.open();
                    }
                });
                licenseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            }
        }
    }

    protected void updateDriverInfo(DBPDriver driver) {

    }

    @Override
    public void setImageDescriptor(ImageDescriptor desc) {
        if (curImageDescriptor != desc) {
            super.setImageDescriptor(desc);
            curImageDescriptor = desc;
        }
    }

    protected Text createPasswordText(Composite parent, String label) {
        if (label != null) {
            UIUtils.createControlLabel(parent, label);
        }
        Composite ph = UIUtils.createPlaceholder(parent, 1);
        ph.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        passwordText = new Text(ph, SWT.BORDER | SWT.PASSWORD);
        passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return passwordText;
    }

    protected void createPasswordControls(Composite parent) {
        createPasswordControls(parent, 1);
    }

    protected void createPasswordControls(Composite parent, int hSpan) {
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
                    showPasswordText(serviceSecurity);
                }
            });
        }

    }

    private void showPasswordText(UIServiceSecurity serviceSecurity) {
        Composite passContainer = passwordText.getParent();
        boolean passHidden = (passwordText.getStyle() & SWT.PASSWORD) == SWT.PASSWORD;
        if (passHidden) {
            if (!serviceSecurity.validatePassword(
                site.getProject(),
                "Enter project password",
                "Enter project master password to unlock connection password view",
                true))
            {
                return;
            }
        }

        Object layoutData = passwordText.getLayoutData();
        String curValue = passwordText.getText();
        passwordText.dispose();

        if (passHidden) {
            passwordText = new Text(passContainer, SWT.BORDER);
        } else {
            passwordText = new Text(passContainer, SWT.PASSWORD | SWT.BORDER);
        }
        passwordText.setLayoutData(layoutData);
        passwordText.setText(curValue);
        passContainer.layout(true, true);

//        if (passwordText.getEchoChar() == '\0') {
//            passwordText.setEchoChar('*');
//            return;
//        }
//        if (serviceSecurity.validatePassword(site.getProject().getSecureStorage(), "Enter project password", "Enter project master password to unlock connection password view")) {
//            passwordText.setEchoChar('\0');
//        }
    }


    protected Image createImage(String imageFilePath) {
        ImageDescriptor imageDescriptor = ResourceLocator.imageDescriptorFromBundle(getClass(), imageFilePath).orElse(null);
        return imageDescriptor == null ? null : imageDescriptor.createImage();
    }

    protected void createConnectionModeSwitcher(Composite parent, SelectionAdapter typeSwitcher) {
        Label cnnTypeLabel = UIUtils.createControlLabel(parent, UIConnectionMessages.dialog_connection_mode_label);
        cnnTypeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        Composite modeGroup = UIUtils.createComposite(parent, 3);
        typeManualRadio = UIUtils.createRadioButton(modeGroup, UIConnectionMessages.dialog_connection_host_label, false, typeSwitcher);
        typeURLRadio = UIUtils.createRadioButton(modeGroup, UIConnectionMessages.dialog_connection_url_label, true, typeSwitcher);
        modeGroup.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
        addControlToGroup(GROUP_CONNECTION_MODE, cnnTypeLabel);
        addControlToGroup(GROUP_CONNECTION_MODE, modeGroup);
    }

    protected void setupConnectionModeSelection(@NotNull Text urlText, boolean useUrl, @NotNull Collection<String> nonUrlPropGroups) {
        if (typeURLRadio != null) typeURLRadio.setSelection(useUrl);
        if (typeManualRadio != null) typeManualRadio.setSelection(!useUrl);
        urlText.setEditable(useUrl);
        urlText.setEnabled(useUrl);

        boolean nonUrl = !useUrl;
        for (String groupName : nonUrlPropGroups) {
            List<Control> controls = propGroupMap.get(groupName);
            if (controls != null) {
                for (Control control : controls) {
                    control.setEnabled(nonUrl);
                    if (control instanceof Text) {
                        ((Text) control).setEditable(nonUrl);
                    }
                }
            }
        }
    }

    protected void addControlToGroup(@NotNull String group, @NotNull Control control) {
        propGroupMap
            .computeIfAbsent(group, k -> new ArrayList<>())
            .add(control);
    }

}
