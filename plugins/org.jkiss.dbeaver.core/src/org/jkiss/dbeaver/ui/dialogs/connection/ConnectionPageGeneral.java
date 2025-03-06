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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.controls.ConnectionFolderSelector;
import org.jkiss.dbeaver.ui.navigator.dialogs.EditObjectFilterDialog;
import org.jkiss.dbeaver.ui.preferences.PrefPageConnectionTypes;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * General connection page (common for all connection types)
 */
public class ConnectionPageGeneral extends ConnectionWizardPage implements NavigatorSettingsStorage {

    static final String PAGE_NAME = ConnectionPageGeneral.class.getSimpleName();

    private static class FilterInfo {
        final Class<?> type;
        final String title;
        Link link;
        DBSObjectFilter filter;

        private FilterInfo(Class<?> type, String title) {
            this.type = type;
            this.title = title;
        }
    }

    private final ConnectionWizard wizard;
    private DataSourceDescriptor dataSourceDescriptor;
    private Text connectionNameText;
    private CSmartCombo<DBPConnectionType> connectionTypeCombo;
    private Combo navigatorSettingsCombo;
    private ConnectionFolderSelector folderSelector;
    private DBPDataSourceFolder curDataSourceFolder;
    private Text descriptionText;
    private Button showVirtualModelCheck;

    private boolean connectionNameChanged = false;
    private boolean activated = false;

    private Button readOnlyConnection;

    private DBNBrowseSettings navigatorSettings;
    private List<DBPDataSourcePermission> accessRestrictions;

    private final List<FilterInfo> filters = new ArrayList<>();
    private Group filtersGroup;

    ConnectionPageGeneral(ConnectionWizard wizard)
    {
        super(PAGE_NAME);
        this.wizard = wizard;
        setTitle(CoreMessages.dialog_connection_edit_wizard_general);
        setDescription(CoreMessages.dialog_connection_wizard_final_description);

        filters.add(new FilterInfo(DBSCatalog.class, CoreMessages.dialog_connection_wizard_final_filter_catalogs));
        filters.add(new FilterInfo(DBSSchema.class, CoreMessages.dialog_connection_wizard_final_filter_schemas_users));
        filters.add(new FilterInfo(DBSTable.class, CoreMessages.dialog_connection_wizard_final_filter_tables));
        filters.add(new FilterInfo(DBSEntityAttribute.class, CoreMessages.dialog_connection_wizard_final_filter_attributes));
    }

    ConnectionPageGeneral(ConnectionWizard wizard, DataSourceDescriptor dataSourceDescriptor)
    {
        this(wizard);
        this.dataSourceDescriptor = dataSourceDescriptor;
        this.accessRestrictions = dataSourceDescriptor.getModifyPermission();

        for (FilterInfo filterInfo : filters) {
            filterInfo.filter = dataSourceDescriptor.getObjectFilter(filterInfo.type, null, true);
        }
    }

    @Override
    public DBNBrowseSettings getNavigatorSettings() {
        return navigatorSettings;
    }

    @Override
    public void setNavigatorSettings(DBNBrowseSettings settings) {
        this.navigatorSettings = settings;

        if (showVirtualModelCheck != null) {
            showVirtualModelCheck.setSelection(!settings.isHideVirtualModel());
        }
    }

    protected boolean wasActivated() {
        return this.activated;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void activatePage()
    {
        if (this.navigatorSettings == null) {
            this.navigatorSettings = new DataSourceNavigatorSettings(getWizard().getSelectedNavigatorSettings());
        }

        if (connectionNameText != null) {
            ConnectionPageSettings settings = wizard.getPageSettings();

            if (dataSourceDescriptor != null && !CommonUtils.isEmpty(dataSourceDescriptor.getName())) {
                connectionNameText.setText(dataSourceDescriptor.getName());
            } else {
                if (CommonUtils.isEmpty(connectionNameText.getText()) || !connectionNameChanged) {
                    String newName = generateConnectionName(settings,
                        DBWorkbench.getPlatform()
                            .getPreferenceStore()
                            .getString(ModelPreferences.DEFAULT_CONNECTION_NAME_PATTERN));
                    if (!newName.isEmpty()) {
                        connectionNameText.setText(newName);
                    }
                    connectionNameChanged = false;
                }
            }
        }
        folderSelector.loadConnectionFolders(getWizard().getSelectedProject());
        if (dataSourceDescriptor != null) {
            {
                // Get settings from data source descriptor
                final DBPConnectionConfiguration conConfig = dataSourceDescriptor.getConnectionConfiguration();
                setConnectionType(connectionTypeCombo, conConfig.getConnectionType());
                updateNavigatorSettingsPreset(navigatorSettingsCombo, dataSourceDescriptor.getNavigatorSettings());

                folderSelector.setFolder(dataSourceDescriptor.getFolder());

                if (dataSourceDescriptor.getDescription() != null) {
                    descriptionText.setText(dataSourceDescriptor.getDescription());
                }

                readOnlyConnection.setSelection(dataSourceDescriptor.isConnectionReadOnly());

                activated = true;
            }
        } else {
            // Default settings
            setConnectionType(connectionTypeCombo, DBPConnectionType.getDefaultConnectionType());
            updateNavigatorSettingsPreset(navigatorSettingsCombo, getNavigatorSettings());
            folderSelector.setFolder(curDataSourceFolder);

            readOnlyConnection.setSelection(false);
        }

        long features = getWizard().getSelectedDriver().getDataSourceProvider().getFeatures();
        boolean isFeatureCatalogOnlyNeedToApply = (features & DBPDataSourceProvider.FEATURE_CATALOGS_ONLY) != 0;

        for (FilterInfo filterInfo : filters) {
            if (DBSCatalog.class.isAssignableFrom(filterInfo.type)) {
                enableFilter(filterInfo,
                    (features & DBPDataSourceProvider.FEATURE_CATALOGS) != 0 || isFeatureCatalogOnlyNeedToApply);
            } else if (DBSSchema.class.isAssignableFrom(filterInfo.type)) {
                enableFilter(filterInfo,
                    (features & DBPDataSourceProvider.FEATURE_SCHEMAS) != 0 && !isFeatureCatalogOnlyNeedToApply);
            } else {
                enableFilter(filterInfo, !isFeatureCatalogOnlyNeedToApply);
            }
        }
        filtersGroup.layout();
    }

    public static void updateNavigatorSettingsPreset(Combo navigatorSettingsCombo, DBNBrowseSettings navigatorSettings) {
        // Find first preset that matches current connection settings
        boolean isPreset = false;
        for (DataSourceNavigatorSettings.Preset nsEntry : DataSourceNavigatorSettings.PRESETS.values()) {
            if (navigatorSettings.equals(nsEntry.getSettings())) {
                navigatorSettingsCombo.setText(nsEntry.getName());
                isPreset = true;
                break;
            }
        }
        if (!isPreset) {
            navigatorSettingsCombo.select(navigatorSettingsCombo.getItemCount() - 1);
        }
    }

    private void enableFilter(FilterInfo filterInfo, boolean enable) {
        filterInfo.link.setEnabled(enable);
        if (enable) {
            filterInfo.link.setText("<a>" + filterInfo.title + "</a>");
            filterInfo.link.setToolTipText(NLS.bind(CoreMessages.dialog_connection_wizard_final_filter_link_tooltip, filterInfo.title));
            if (filterInfo.filter != null && !filterInfo.filter.isNotApplicable()) {
                filterInfo.link.setFont(BaseThemeSettings.instance.baseFontBold);
            } else {
                filterInfo.link.setFont(getFont());
            }
        } else {
            //filterInfo.link.setText(NLS.bind(CoreMessages.dialog_connection_wizard_final_filter_link_not_supported_text, filterInfo.title));
            filterInfo.link.setToolTipText(NLS.bind(CoreMessages.dialog_connection_wizard_final_filter_link_not_supported_tooltip, filterInfo.title, getWizard().getSelectedDriver().getName()));
        }
    }

    private String generateConnectionName(ConnectionPageSettings settings, String usedName) {
        String newName;
        String resultName = usedName;
        if (settings != null) {
            if (resultName.isBlank()) {
                resultName = GeneralUtils.variablePattern(DBPConnectionConfiguration.VAR_HOST_OR_DATABASE);
            }
            DataSourceDescriptor dataSource = settings.getActiveDataSource();
            DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
            final ConnectionNameResolver resolver = new ConnectionNameResolver(dataSource, connectionInfo, dataSourceDescriptor);
            newName = GeneralUtils.replaceVariables(resultName, resolver);
            String baseName = newName;
            for (int i = 2; ; i++) {
                if (settings.getDataSourceRegistry().findDataSourceByName(newName) != null) {
                    newName = baseName + " " + i;
                } else {
                    break;
                }
            }
        } else {
            newName = wizard.getSelectedDriver().getName();
        }
        return newName;
    }

    @NotNull
    private DataSourceDescriptor getActiveDataSource() {
        ConnectionPageSettings pageSettings = getWizard().getPageSettings();
        return pageSettings == null ? wizard.getActiveDataSource() : pageSettings.getActiveDataSource();
    }

    @Override
    public void deactivatePage()
    {
        saveSettings(dataSourceDescriptor);
    }

    @Override
    public void createControl(Composite parent) {
        if (navigatorSettings == null) {
            navigatorSettings = new DataSourceNavigatorSettings(getWizard().getSelectedNavigatorSettings());
        }

        initializeDialogUnits(parent);
        Composite group = UIUtils.createComposite(parent, 1);

        {
            Composite miscGroup = UIUtils.createControlGroup(group, CoreMessages.pref_page_ui_general_group_general, 2, GridData.FILL_HORIZONTAL, 0);

            String connectionName = dataSourceDescriptor == null ? "" : dataSourceDescriptor.getName(); //$NON-NLS-1$
            connectionNameText = UIUtils.createLabelText(miscGroup, CoreMessages.dialog_connection_wizard_final_label_connection_name, CommonUtils.toString(connectionName));
            connectionNameText.addModifyListener(e -> {
                if (dataSourceDescriptor == null || !connectionNameText.getText().equals(connectionName)) {
                    connectionNameChanged = true;
                    getContainer().updateButtons();
                }
            });
            ContentAssistUtils.installContentProposal(
                connectionNameText,
                new SmartTextContentAdapter(),
                new StringContentProposalProvider(Arrays.stream(ConnectionNameResolver.getConnectionVariables()).map(GeneralUtils::variablePattern).toArray(String[]::new))
            );
            UIUtils.setContentProposalToolTip(connectionNameText, "Connection name patterns",
                ConnectionNameResolver.getConnectionVariables());
            descriptionText = UIUtils.createLabelText(miscGroup, CoreMessages.dialog_connection_wizard_description, null);
            {
                connectionTypeCombo = createConnectionTypeCombo(miscGroup);
                connectionTypeCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DBPConnectionType type = connectionTypeCombo.getItem(connectionTypeCombo.getSelectionIndex());
                        getWizard().firePropertyChangeEvent(ConnectionWizard.PROP_CONNECTION_TYPE, getActiveDataSource().getConnectionConfiguration().getConnectionType(), type);
                    }
                });

                Composite ctGroup = connectionTypeCombo.getParent();
                ((GridLayout)ctGroup.getLayout()).numColumns++;
                UIUtils.createDialogButton(ctGroup, CoreMessages.dialog_connection_wizard_final_label_connection_types_edit, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DBPConnectionType curConType = connectionTypeCombo.getSelectedItem();
                        DataSourceDescriptor dataSource = getActiveDataSource();
                        UIUtils.showPreferencesFor(
                            ctGroup.getShell(),
                            dataSource.getConnectionConfiguration().getConnectionType(),
                            PrefPageConnectionTypes.PAGE_ID);
                        loadConnectionTypes(connectionTypeCombo);
                        if (!connectionTypeCombo.getItems().contains(curConType)) {
                            curConType = connectionTypeCombo.getItems().get(0);
                        }
                        setConnectionType(connectionTypeCombo, curConType);
                        getWizard().firePropertyChangeEvent(ConnectionWizard.PROP_CONNECTION_TYPE, curConType, curConType);
                    }
                });
            }

            {
                navigatorSettingsCombo = createNavigatorSettingsCombo(miscGroup, this, dataSourceDescriptor);
            }

            folderSelector = new ConnectionFolderSelector(miscGroup);
        }

        Composite refsGroup = UIUtils.createComposite(group, 3);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        refsGroup.setLayoutData(gd);

        {
            // Security
            Group securityGroup = UIUtils.createControlGroup(
                refsGroup,
                CoreMessages.dialog_connection_wizard_final_group_security,
                1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            readOnlyConnection = UIUtils.createCheckbox(
                securityGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_connection_readonly,
                dataSourceDescriptor != null && dataSourceDescriptor.isConnectionReadOnly());
            readOnlyConnection.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            UIUtils.createDialogButton(
                securityGroup,
                CoreMessages.pref_page_label_edit_permissions,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        editPermissions();
                    }
                }
            );
        }

        {
            // Filters
            filtersGroup = UIUtils.createControlGroup(
                refsGroup,
                CoreMessages.dialog_connection_wizard_final_group_filters,
                1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
            for (final FilterInfo filterInfo : filters) {
                filterInfo.link = UIUtils.createLink(filtersGroup, "<a>" + filterInfo.title + "</a>", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        EditObjectFilterDialog dialog = new EditObjectFilterDialog(
                            getShell(),
                            getWizard().getDataSourceRegistry(),
                            filterInfo.title,
                            filterInfo.filter != null ? filterInfo.filter : new DBSObjectFilter(),
                            true);
                        if (dialog.open() == IDialogConstants.OK_ID) {
                            filterInfo.filter = dialog.getFilter();
                            if (filterInfo.filter != null && !filterInfo.filter.isNotApplicable()) {
                                filterInfo.link.setFont(BaseThemeSettings.instance.baseFontBold);
                            } else {
                                filterInfo.link.setFont(getFont());
                            }
                        }
                    }
                });
                filterInfo.link.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }
        }

        {
            // Filters
            Composite vmGroup = UIUtils.createControlGroup(
                refsGroup,
                "Virtual model",
                1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
            showVirtualModelCheck = UIUtils.createCheckbox(
                vmGroup,
                "Show virtual model editor",
                "Show virtual model pages in table editor",
                !navigatorSettings.isHideVirtualModel(),
                1);
            showVirtualModelCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                final DataSourceNavigatorSettings settings = new DataSourceNavigatorSettings(navigatorSettings);
                settings.setHideVirtualModel(!showVirtualModelCheck.getSelection());
                updateNavigatorSettingsPreset(navigatorSettingsCombo, settings);
                setNavigatorSettings(settings);
            }));
            Button resetVM = UIUtils.createDialogButton(
                vmGroup,
                "Reset configuration",
                null,
                "Delete all colorings, transformers and virtual table constraints for all tables in this data source",
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (UIUtils.confirmAction(
                            getShell(),
                            "Reset virtual model settings",
                            "You are about to reset all virtual model configuration.\n It includes:\n" +
                                "\t- All virtual constraints and foreign keys\n" +
                                "\t- All column transformers\n" +
                                "\t- All table row colorings"
                            )
                        ) {
                            dataSourceDescriptor.getVirtualModel().resetData();
                            DataSourceDescriptor originalDataSource = getWizard().getOriginalDataSource();
                            originalDataSource.getVirtualModel().resetData();
                            originalDataSource.persistConfiguration();
                        }
                    }
                });
            resetVM.setEnabled(dataSourceDescriptor != null && dataSourceDescriptor.getVirtualModel().hasValuableData());
//            UIUtils.createInfoLabel(vmGroup, "Virtual model is a logical database structure on the client side (not in a real database).\n" +
//                "It also contains information about\nrow coloring and columns transformations", GridData.FILL_HORIZONTAL, 1);
        }

        {
            Composite linkGroup = UIUtils.createComposite(refsGroup, 1);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 3;
            linkGroup.setLayoutData(gd);

            Link initConfigLink = new Link(linkGroup, SWT.NONE);
            initConfigLink.setText("<a>" + CoreMessages.dialog_connection_wizard_connection_init_description + "</a>");
            initConfigLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (getWizard().isNew()) {
                        DataSourceDescriptor dataSource = getActiveDataSource();
                        EditWizardPageDialog dialog = new EditWizardPageDialog(
                            getWizard(),
                            new ConnectionPageInitialization(dataSource),
                            dataSource);
                        dialog.open();
                    } else {
                        getWizard().openSettingsPage(ConnectionPageInitialization.PAGE_NAME);
                    }
                }
            });
            initConfigLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            Link shellConfigLink = new Link(linkGroup, SWT.NONE);
            shellConfigLink.setText("<a>" + CoreMessages.dialog_connection_edit_wizard_shell_cmd + "</a>");
            shellConfigLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (getWizard().isNew()) {
                        DataSourceDescriptor dataSource = getActiveDataSource();
                        EditWizardPageDialog dialog = new EditWizardPageDialog(
                            getWizard(),
                            new ConnectionPageShellCommands(dataSource),
                            dataSource);
                        dialog.open();
                    } else {
                        getWizard().openSettingsPage(ConnectionPageShellCommands.PAGE_NAME);
                    }
                }
            });
            shellConfigLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        }

        setControl(group);

        UIUtils.setHelp(group, IHelpContextIds.CTX_CON_WIZARD_FINAL);
    }

    public static Combo createNavigatorSettingsCombo(Composite composite, NavigatorSettingsStorage settingsStorage, DBPDataSourceContainer dataSourceDescriptor) {
        UIUtils.createControlLabel(composite, CoreMessages.dialog_connection_wizard_final_label_navigator_settings);

        Composite ctGroup = UIUtils.createComposite(composite, 2);
        Combo navigatorSettingsCombo = new Combo(ctGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        final GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(navigatorSettingsCombo) * 20;
        navigatorSettingsCombo.setLayoutData(gd);
        for (String ncPresetName : DataSourceNavigatorSettings.PRESETS.keySet()) {
            navigatorSettingsCombo.add(ncPresetName);
        }
        navigatorSettingsCombo.select(0);
        navigatorSettingsCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (navigatorSettingsCombo.getSelectionIndex() == navigatorSettingsCombo.getItemCount() - 1) {
                    // Custom - no changes
                } else {
                    DataSourceNavigatorSettings.Preset newSettings = DataSourceNavigatorSettings.PRESETS.get(navigatorSettingsCombo.getText());
                    if (newSettings == null) {
                        throw new IllegalStateException("Invalid preset name: " + navigatorSettingsCombo.getText());
                    }
                    settingsStorage.setNavigatorSettings(newSettings.getSettings());
                }
            }
        });

        UIUtils.createDialogButton(ctGroup, CoreMessages.dialog_connection_wizard_final_label_navigator_settings_customize, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                settingsStorage.setNavigatorSettings(
                    editNavigatorSettings(navigatorSettingsCombo, settingsStorage.getNavigatorSettings(), dataSourceDescriptor));
            }
        });
        return navigatorSettingsCombo;
    }

    public static CSmartCombo<DBPConnectionType> createConnectionTypeCombo(Composite composite) {
        UIUtils.createControlLabel(composite, CoreMessages.dialog_connection_wizard_final_label_connection_type);

        Composite ctGroup = UIUtils.createComposite(composite, 1);

        CSmartCombo<DBPConnectionType> connectionTypeCombo = new CSmartCombo<>(ctGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY, new ConnectionTypeLabelProvider());
        loadConnectionTypes(connectionTypeCombo);
        setConnectionType(connectionTypeCombo, DBPConnectionType.getDefaultConnectionType());
        connectionTypeCombo.select(DBPConnectionType.getDefaultConnectionType());
        final GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(connectionTypeCombo) * 20;
        connectionTypeCombo.setLayoutData(gd);

        return connectionTypeCombo;
    }

    private static DBNBrowseSettings editNavigatorSettings(
        @NotNull Combo navigatorSettingsCombo,
        @NotNull DBNBrowseSettings navigatorSettings,
        @Nullable DBPDataSourceContainer dataSourceDescriptor) {
        EditConnectionNavigatorSettingsDialog dialog = new EditConnectionNavigatorSettingsDialog(
            navigatorSettingsCombo.getShell(),
            navigatorSettings,
            dataSourceDescriptor);
        if (dialog.open() == IDialogConstants.OK_ID) {
            navigatorSettings = dialog.getNavigatorSettings();
            updateNavigatorSettingsPreset(navigatorSettingsCombo, navigatorSettings);
        }
        return navigatorSettings;
    }

    private void editPermissions() {
        EditConnectionPermissionsDialog dialog = new EditConnectionPermissionsDialog(getShell(), accessRestrictions);
        if (dialog.open() == IDialogConstants.OK_ID) {
            accessRestrictions = dialog.getAccessRestrictions();
        }
    }

    public static void setConnectionType(@NotNull CSmartCombo<DBPConnectionType> combo, @NotNull DBPConnectionType connectionType) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            final DBPConnectionType item = combo.getItem(i);
            if (item.getId().equals(connectionType.getId())) {
                combo.select(i);
                return;
            }
        }
    }

    public static void loadConnectionTypes(CSmartCombo <DBPConnectionType> connectionTypeCombo) {
        connectionTypeCombo.removeAll();
        for (DBPConnectionType ct : DataSourceProviderRegistry.getInstance().getConnectionTypes()) {
            connectionTypeCombo.addItem(ct);
        }
    }

    @Override
    public boolean isPageComplete()
    {
        return true;
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        if (dataSourceDescriptor != null && !activated) {
            // No changes anyway
            return;
        }
        if (dataSource == null) {
            return;
        }
        final DBPConnectionConfiguration confConfig = dataSource.getConnectionConfiguration();
        final String name;

        if (connectionNameChanged) {
            name = generateConnectionName(getWizard().getPageSettings(), connectionNameText.getText());
        } else if (dataSourceDescriptor != null) {
            name = dataSourceDescriptor.getName();
        } else {
            name = generateConnectionName(getWizard().getPageSettings(),
                DBWorkbench.getPlatform()
                    .getPreferenceStore()
                    .getString(ModelPreferences.DEFAULT_CONNECTION_NAME_PATTERN));
        }

        dataSource.setName(name);
        if (folderSelector.isEmpty()) {
            dataSource.setFolder(curDataSourceFolder);
        } else {
            dataSource.setFolder(folderSelector.getFolder());
        }

        if (connectionTypeCombo.getSelectionIndex() >= 0) {
            DBPConnectionType newConnectionType = connectionTypeCombo.getSelectedItem();
            if (!Objects.equals(newConnectionType, confConfig.getConnectionType())) {
                // Changing connection types also changes defaults
                confConfig.setConnectionType(newConnectionType);
            }
        }

        DataSourceDescriptor dsDescriptor = (DataSourceDescriptor) dataSource;
        final String description = descriptionText.getText();
        if (description.isEmpty()) {
            dsDescriptor.setDescription(null);
        } else {
            dsDescriptor.setDescription(description);
        }

        if (this.navigatorSettings == null) {
            this.navigatorSettings = new DataSourceNavigatorSettings(getWizard().getSelectedNavigatorSettings());
        }
        dsDescriptor.setNavigatorSettings(this.navigatorSettings);

        dsDescriptor.setConnectionReadOnly(this.readOnlyConnection.getSelection());
        dsDescriptor.setModifyPermissions(this.accessRestrictions);

        for (FilterInfo filterInfo : filters) {
            if (filterInfo.filter != null) {
                dataSource.setObjectFilter(filterInfo.type, null, filterInfo.filter);
            }
        }
    }

    public void setDataSourceFolder(DBPDataSourceFolder dataSourceFolder) {
        this.curDataSourceFolder = dataSourceFolder;
    }

}