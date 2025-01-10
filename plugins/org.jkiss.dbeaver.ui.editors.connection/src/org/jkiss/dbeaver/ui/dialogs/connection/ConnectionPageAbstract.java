/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
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
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSecurity;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.dialogs.AcceptLicenseDialog;
import org.jkiss.dbeaver.ui.dialogs.IConnectionWizard;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

/**
 * ConnectionPageAbstract
 */
public abstract class ConnectionPageAbstract extends DialogPage implements IDataSourceConnectionEditor {
    public static final String PROP_DRIVER_SUBSTITUTION = "driver-substitution";

    protected static final String GROUP_CONNECTION_MODE = "connectionMode"; //$NON-NLS-1$
    protected static final String GROUP_CONNECTION = "connection"; //$NON-NLS-1$
    protected static final String GROUP_URL = "url"; //$NON-NLS-1$
    protected static final List<String> GROUP_CONNECTION_ARR = List.of(GROUP_CONNECTION);
    protected static final List<String> GROUP_URL_ARR = List.of(GROUP_URL);
    @NotNull
    protected final Map<String, Set<Control>> propGroupMap = new HashMap<>();

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
    private Combo driverSubstitutionCombo;

    private ImageDescriptor curImageDescriptor;
    private Button licenseButton;
    @Nullable
    private Control databaseDocumentationInfoLabel;

    public IDataSourceConnectionEditorSite getSite() {
        return site;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void setSite(@NotNull IDataSourceConnectionEditorSite site)
    {
        this.site = site;
    }

    protected boolean isCustomURL() {
        return typeURLRadio != null && typeURLRadio.getSelection();
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

        if (driver != null && databaseDocumentationInfoLabel != null) {
            databaseDocumentationInfoLabel.setVisible(
                CommonUtils.isNotEmpty(driver.getDatabaseDocumentationSuffixURL()));
        }

        if (driverSubstitutionCombo != null) {
            final DBPDriverSubstitutionDescriptor driverSubstitution = dataSource.getDriverSubstitution();
            if (driverSubstitution != null) {
                final DBPDriverSubstitutionDescriptor[] substitutions
                    = DataSourceProviderRegistry.getInstance().getAllDriverSubstitutions();
                driverSubstitutionCombo.select(ArrayUtils.indexOf(substitutions, driverSubstitution) + 1);
            } else {
                driverSubstitutionCombo.select(0);
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

        if (driverSubstitutionCombo != null) {
            final int substitutionIndex = driverSubstitutionCombo.getSelectionIndex();
            if (substitutionIndex > 0) {
                final DBPDriverSubstitutionDescriptor[] substitutions
                    = DataSourceProviderRegistry.getInstance().getAllDriverSubstitutions();
                dataSource.setDriverSubstitution(substitutions[substitutionIndex - 1]);
            } else {
                dataSource.setDriverSubstitution(null);
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
            ((GridData) variablesHintLabel.getInfoLabel().getLayoutData()).horizontalSpan = 2;
        } else {
            UIUtils.createFormPlaceholder(panel, 2, 1);
        }

        formDatabaseDocumentationInfoLabel(panel);

        if (site.isNew()) {
            Button advSettingsButton = UIUtils.createDialogButton(panel,
                UIConnectionMessages.dialog_connection_edit_wizard_conn_conf_general_link,
                SelectionListener.widgetSelectedAdapter(e -> site.openSettingsPage("ConnectionPageGeneral")));
            advSettingsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        } else {
            UIUtils.createEmptyLabel(panel, 1, 1);
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
                Button driverButton = UIUtils.createDialogButton(driverInfoComp, UIConnectionMessages.dialog_connection_edit_driver_button,
                    SelectionListener.widgetSelectedAdapter(e -> {
                        if (site.openDriverEditor()) {
                            updateDriverInfo(site.getDriver());
                        }
                }));
                driverButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            } else {
                UIUtils.createEmptyLabel(driverInfoComp, 1, 1);
            }

            {
                licenseButton = UIUtils.createDialogButton(driverInfoComp, UIConnectionMessages.dialog_edit_driver_text_driver_license,
                    SelectionListener.widgetSelectedAdapter(e -> {
                        String driverLicense = site.getDriver().getLicense();
                        if (CommonUtils.isEmpty(driverLicense)) {
                            driverLicense = "N/A";
                        }
                        AcceptLicenseDialog licenseDialog = new AcceptLicenseDialog(getShell(), site.getDriver().getFullName(), driverLicense);
                        licenseDialog.setViewMode(true);
                        licenseDialog.open();
                    }));
                licenseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            }
        }
    }

    private void formDatabaseDocumentationInfoLabel(Composite panel) {
        databaseDocumentationInfoLabel = UIUtils.createInfoLabel(
            panel,
            UIConnectionMessages.dialog_connection_database_documentation,
            () -> {
                String databaseDocumentationSuffixURL = site.getDriver().getDatabaseDocumentationSuffixURL();
                ShellUtils.launchProgram(HelpUtils.getHelpExternalReference(databaseDocumentationSuffixURL));
            });
        databaseDocumentationInfoLabel.setToolTipText(
            UIConnectionMessages.dialog_connection_database_documentation);
        databaseDocumentationInfoLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        databaseDocumentationInfoLabel.setVisible(CommonUtils.isNotEmpty(
            site.getDriver().getDatabaseDocumentationSuffixURL()));
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
            UIConnectionMessages.dialog_connection_wizard_final_checkbox_save_password,
            dataSource == null || dataSource.isSavePassword());
        savePasswordCheck.setToolTipText(UIConnectionMessages.dialog_connection_wizard_final_checkbox_save_password);
        //savePasswordCheck.setLayoutData(gd);

        if (supportsPasswordView) {
            userManagementToolbar = new ToolBar(panel, SWT.HORIZONTAL);
            ToolItem showPasswordLabel = new ToolItem(userManagementToolbar, SWT.NONE);
            showPasswordLabel.setToolTipText("Show password on screen");
            showPasswordLabel.setImage(DBeaverIcons.getImage(UIIcon.SHOW_ALL_DETAILS));
            showPasswordLabel.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> showPasswordText(serviceSecurity)));
        }

    }

    private void showPasswordText(UIServiceSecurity serviceSecurity) {
        boolean passHidden = (passwordText.getStyle() & SWT.PASSWORD) == SWT.PASSWORD;
        if (passHidden) {
            if (!serviceSecurity.validatePassword(
                site.getProject(),
                "Enter project password",
                "Enter project password to unlock connection password view",
                true))
            {
                return;
            }
        }

        passwordText = UIUtils.recreateTextControl(
            passwordText,
            passHidden ? SWT.BORDER : SWT.BORDER | SWT.PASSWORD
        );
    }


    protected Image createImage(String imageFilePath) {
        ImageDescriptor imageDescriptor = ResourceLocator.imageDescriptorFromBundle(getClass(), imageFilePath).orElse(null);
        return imageDescriptor == null ? null : imageDescriptor.createImage();
    }

    protected void createConnectionModeSwitcher(Composite parent, SelectionListener typeSwitcher) {
        Label cnnTypeLabel = UIUtils.createControlLabel(parent, UIConnectionMessages.dialog_connection_mode_label);
        cnnTypeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        Composite modeGroup = UIUtils.createComposite(parent, 3);
        typeManualRadio = UIUtils.createRadioButton(modeGroup, UIConnectionMessages.dialog_connection_host_label, false, typeSwitcher);
        typeURLRadio = UIUtils.createRadioButton(modeGroup, UIConnectionMessages.dialog_connection_url_label, true, typeSwitcher);
        modeGroup.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
        if (supportsDriverSubstitution()) {
            createDriverSubstitutionControls(modeGroup);
        }
        addControlToGroup(GROUP_CONNECTION_MODE, cnnTypeLabel);
        addControlToGroup(GROUP_CONNECTION_MODE, modeGroup);
    }

    protected void createDriverSubstitutionControls(@NotNull Composite parent) {
        final DBPDriverSubstitutionDescriptor[] driverSubstitutions = DataSourceProviderRegistry.getInstance().getAllDriverSubstitutions();

        if (driverSubstitutions.length > 0) {
            final Composite substitutionGroup = UIUtils.createComposite(parent, 2);
            substitutionGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.BEGINNING).create());

            driverSubstitutionCombo = UIUtils.createLabelCombo(
                substitutionGroup,
                "Driver type",
                NLS.bind(
                    "Replaces the current driver ({0}) with the selected one.\nProvides all functionality of the original driver but driven by the substituting driver.",
                    site.getActiveDataSource().getDriver().getFullName()
                ),
                SWT.DROP_DOWN | SWT.READ_ONLY
            );
            driverSubstitutionCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                final int index = driverSubstitutionCombo.getSelectionIndex();
                final DBPDriverSubstitutionDescriptor driverSubstitution = index > 0 ? driverSubstitutions[index - 1] : null;
                final IConnectionWizard wizard = (IConnectionWizard) site.getWizard();
                if (wizard != null) {
                    wizard.firePropertyChangeEvent(PROP_DRIVER_SUBSTITUTION, wizard.getDriverSubstitution(), driverSubstitution);
                }
            }));
            driverSubstitutionCombo.add("JDBC");

            for (DBPDriverSubstitutionDescriptor descriptor : driverSubstitutions) {
                driverSubstitutionCombo.add(descriptor.getName());
            }
        }
    }

    protected boolean isHideNonApplicableControls() {
        return false;
    }

    protected void setupConnectionModeSelection(
        @NotNull Text urlText,
        boolean useUrl,
        @NotNull Collection<String> nonUrlPropGroups
    ) {
        addControlToGroup(GROUP_URL, urlText);
        setupConnectionModeSelection(useUrl, Collections.singleton(GROUP_URL), nonUrlPropGroups);
    }

    protected void setupConnectionModeSelection(
        boolean useUrl,
        @NotNull Collection<String> urlPropGroups,
        @NotNull Collection<String> nonUrlPropGroups
    ) {
        if (typeURLRadio != null) typeURLRadio.setSelection(useUrl);
        if (typeManualRadio != null) typeManualRadio.setSelection(!useUrl);

        updateConnectionModeControlsVisibility(urlPropGroups, useUrl);
        updateConnectionModeControlsVisibility(nonUrlPropGroups, !useUrl);
        if (isHideNonApplicableControls()) {
            Control shellControl = getControl();
            if (shellControl instanceof Composite shc) {
                shc.layout(true, true);
            }
        }
    }

    private void updateConnectionModeControlsVisibility(@NotNull Collection<String> nonUrlPropGroups, boolean enable) {
        for (String groupName : nonUrlPropGroups) {
            Set<Control> controls = propGroupMap.get(groupName);
            if (controls != null) {
                for (Control control : controls) {
                    control.setEnabled(enable);
                    if (control instanceof Text text) {
                        text.setEditable(enable);
                    }
                    if (isHideNonApplicableControls()) {
                        UIUtils.setControlVisible(control, enable);
                    }
                }
            }
        }
    }

    protected void addControlToGroup(@NotNull String group, @NotNull Control ... list) {
        Set<Control> controls = propGroupMap
            .computeIfAbsent(group, k -> new HashSet<>());
        Collections.addAll(controls, list);
    }

    protected void updateUrlFromSettings(Text urlText) {
        DBPDataSourceContainer dataSourceContainer = site.getActiveDataSource();
        urlText.setText(dataSourceContainer.getDriver().getConnectionURL(site.getActiveDataSource().getConnectionConfiguration()));
    }

    protected boolean supportsDriverSubstitution() {
        return true;
    }

}
