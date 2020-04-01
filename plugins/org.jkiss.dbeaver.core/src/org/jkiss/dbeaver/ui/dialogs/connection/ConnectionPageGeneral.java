/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
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
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.navigator.dialogs.EditObjectFilterDialog;
import org.jkiss.dbeaver.ui.preferences.PrefPageConnectionTypes;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * General connection page (common for all connection types)
 */
class ConnectionPageGeneral extends ConnectionWizardPage {

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

    private ConnectionWizard wizard;
    private DataSourceDescriptor dataSourceDescriptor;
    private Text connectionNameText;
    private CSmartCombo<DBPConnectionType> connectionTypeCombo;
    private Combo navigatorSettingsCombo;
    //private Combo connectionFolderCombo;
    private Text descriptionText;

    private boolean connectionNameChanged = false;
    private boolean activated = false;
    private DBPDataSourceFolder dataSourceFolder;
    private List<DBPDataSourceFolder> connectionFolders = new ArrayList<>();

    private Button readOnlyConnection;

    @NotNull
    private DBNBrowseSettings navigatorSettings;
    private List<DBPDataSourcePermission> accessRestrictions;

    private List<FilterInfo> filters = new ArrayList<>();
    private Group filtersGroup;
    private Font boldFont;

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

        this.navigatorSettings = DataSourceNavigatorSettings.PRESET_FULL.getSettings();
    }

    ConnectionPageGeneral(ConnectionWizard wizard, DataSourceDescriptor dataSourceDescriptor)
    {
        this(wizard);
        this.dataSourceDescriptor = dataSourceDescriptor;
        this.navigatorSettings = new DataSourceNavigatorSettings(dataSourceDescriptor.getNavigatorSettings());
        this.accessRestrictions = dataSourceDescriptor.getModifyPermission();

        for (FilterInfo filterInfo : filters) {
            filterInfo.filter = dataSourceDescriptor.getObjectFilter(filterInfo.type, null, true);
        }
    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(boldFont);
        super.dispose();
    }

    @Override
    public void activatePage()
    {
        if (connectionNameText != null) {
            if (dataSourceDescriptor != null && !CommonUtils.isEmpty(dataSourceDescriptor.getName())) {
                connectionNameText.setText(dataSourceDescriptor.getName());
                connectionNameChanged = true;
            } else {
                ConnectionPageSettings settings = wizard.getPageSettings();
                if (CommonUtils.isEmpty(connectionNameText.getText()) || !connectionNameChanged) {
                    String newName = generateConnectionName(settings);
                    if (newName != null) {
                        connectionNameText.setText(newName);
                    }
                    connectionNameChanged = false;
                }
            }
        }
        if (dataSourceDescriptor != null) {
            if (!activated) {
                // Get settings from data source descriptor
                final DBPConnectionConfiguration conConfig = dataSourceDescriptor.getConnectionConfiguration();
                connectionTypeCombo.select(conConfig.getConnectionType());
                updateNavigatorSettingsPreset();

                /*
                dataSourceFolder = dataSourceDescriptor.getFolder();
                if (dataSourceDescriptor.getFolder() == null) {
                    connectionFolderCombo.select(0);
                } else {
                    connectionFolderCombo.select(connectionFolders.indexOf(dataSourceFolder));
                }
*/

                if (dataSourceDescriptor.getDescription() != null) {
                    descriptionText.setText(dataSourceDescriptor.getDescription());
                }

                readOnlyConnection.setSelection(dataSourceDescriptor.isConnectionReadOnly());

                activated = true;
            }
        } else {
            // Default settings
            connectionTypeCombo.select(0);
/*
            if (dataSourceFolder != null) {
                connectionFolderCombo.select(connectionFolders.indexOf(dataSourceFolder));
            } else {
                connectionFolderCombo.select(0);
            }
*/

            readOnlyConnection.setSelection(false);
        }

        long features = getWizard().getSelectedDriver().getDataSourceProvider().getFeatures();

        for (FilterInfo filterInfo : filters) {
            if (DBSCatalog.class.isAssignableFrom(filterInfo.type)) {
                enableFilter(filterInfo, (features & DBPDataSourceProvider.FEATURE_CATALOGS) != 0);
            } else if (DBSSchema.class.isAssignableFrom(filterInfo.type)) {
                enableFilter(filterInfo, (features & DBPDataSourceProvider.FEATURE_SCHEMAS) != 0);
            } else {
                enableFilter(filterInfo, true);
            }
        }
        filtersGroup.layout();
    }

    private void updateNavigatorSettingsPreset() {
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
                filterInfo.link.setFont(boldFont);
            } else {
                filterInfo.link.setFont(getFont());
            }
        } else {
            //filterInfo.link.setText(NLS.bind(CoreMessages.dialog_connection_wizard_final_filter_link_not_supported_text, filterInfo.title));
            filterInfo.link.setToolTipText(NLS.bind(CoreMessages.dialog_connection_wizard_final_filter_link_not_supported_tooltip, filterInfo.title, getWizard().getSelectedDriver().getName()));
        }
    }

    private String generateConnectionName(ConnectionPageSettings settings) {
        String newName;
        if (settings != null) {
            DBPConnectionConfiguration connectionInfo = settings.getActiveDataSource().getConnectionConfiguration();
            newName = dataSourceDescriptor == null ? "" : settings.getActiveDataSource().getName(); //$NON-NLS-1$
            if (CommonUtils.isEmpty(newName)) {
                newName = connectionInfo.getDatabaseName();
                if (CommonUtils.isEmpty(newName)) {
                    newName = connectionInfo.getHostName();
                }
                if (CommonUtils.isEmpty(newName)) {
                    newName = connectionInfo.getServerName();
                }
                if (CommonUtils.isEmpty(newName)) {
                    newName = CoreMessages.dialog_connection_wizard_final_default_new_connection_name;
                }
                StringTokenizer st = new StringTokenizer(newName, "/\\:,?=%$#@!^&*()"); //$NON-NLS-1$
                while (st.hasMoreTokens()) {
                    newName = st.nextToken();
                }
                newName = settings.getDriver().getName() + " - " + newName; //$NON-NLS-1$
                newName = CommonUtils.truncateString(newName, 50);
            }

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
    }

    @Override
    public void createControl(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        Composite group = UIUtils.createComposite(parent, 1);

        {
            Composite miscGroup = UIUtils.createControlGroup(group, CoreMessages.pref_page_ui_general_group_general, 2, GridData.FILL_HORIZONTAL, 0);

            String connectionName = dataSourceDescriptor == null ? "" : dataSourceDescriptor.getName(); //$NON-NLS-1$
            connectionNameText = UIUtils.createLabelText(miscGroup, CoreMessages.dialog_connection_wizard_final_label_connection_name, CommonUtils.toString(connectionName));
            connectionNameText.addModifyListener(e -> {
                connectionNameChanged = true;
                ConnectionPageGeneral.this.getContainer().updateButtons();
            });

            {
                UIUtils.createControlLabel(miscGroup, CoreMessages.dialog_connection_wizard_final_label_connection_type);

                Composite ctGroup = UIUtils.createComposite(miscGroup, 2);
                connectionTypeCombo = new CSmartCombo<>(ctGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY, new ConnectionTypeLabelProvider());
                loadConnectionTypes();
                connectionTypeCombo.select(0);
                connectionTypeCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DBPConnectionType type = connectionTypeCombo.getItem(connectionTypeCombo.getSelectionIndex());
                        getWizard().firePropertyChangeEvent(ConnectionWizard.PROP_CONNECTION_TYPE, getActiveDataSource().getConnectionConfiguration().getConnectionType(), type);
                    }
                });
                final GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.widthHint = UIUtils.getFontHeight(connectionTypeCombo) * 20;
                connectionTypeCombo.setLayoutData(gd);

                UIUtils.createDialogButton(ctGroup, CoreMessages.dialog_connection_wizard_final_label_connection_types_edit, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DBPConnectionType curConType = connectionTypeCombo.getSelectedItem();
                        DataSourceDescriptor dataSource = getActiveDataSource();
                        UIUtils.showPreferencesFor(
                            getControl().getShell(),
                            dataSource.getConnectionConfiguration().getConnectionType(),
                            PrefPageConnectionTypes.PAGE_ID);
                        loadConnectionTypes();
                        if (!connectionTypeCombo.getItems().contains(curConType)) {
                            curConType = connectionTypeCombo.getItems().get(0);
                        }
                        connectionTypeCombo.select(curConType);
                        getWizard().firePropertyChangeEvent(ConnectionWizard.PROP_CONNECTION_TYPE, curConType, curConType);
                    }
                });
            }

            {
                UIUtils.createControlLabel(miscGroup, CoreMessages.dialog_connection_wizard_final_label_navigator_settings);

                Composite ctGroup = UIUtils.createComposite(miscGroup, 2);
                navigatorSettingsCombo = new Combo(ctGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
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
                            ConnectionPageGeneral.this.navigatorSettings = newSettings.getSettings();
                        }
                    }
                });

                UIUtils.createDialogButton(ctGroup, CoreMessages.dialog_connection_wizard_final_label_navigator_settings_customize, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        editNavigatorSettings();
                    }
                });
            }

    /*
            {
                UIUtils.createControlLabel(miscGroup, CoreMessages.dialog_connection_wizard_final_label_connection_folder);

                connectionFolderCombo = new Combo(miscGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.widthHint = UIUtils.getFontHeight(connectionFolderCombo) * 20;
                connectionFolderCombo.setLayoutData(gd);
                loadConnectionFolders();
                connectionFolderCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        dataSourceFolder = connectionFolders.get(connectionFolderCombo.getSelectionIndex());
                    }
                });
            }
    */

            {
                Label descLabel = UIUtils.createControlLabel(miscGroup, CoreMessages.dialog_connection_wizard_description);
                descLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
                descriptionText = new Text(miscGroup, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
                final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.heightHint = descriptionText.getLineHeight() * 3;
                descriptionText.setLayoutData(gd);
            }
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

            UIUtils.createDialogButton(securityGroup, "Edit permissions ...", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    editPermissions();
                }
            });
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
                                filterInfo.link.setFont(boldFont);
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
            Composite linkGroup = UIUtils.createComposite(refsGroup, 1);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
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

    private void editNavigatorSettings() {
        EditConnectionNavigatorSettingsDialog dialog = new EditConnectionNavigatorSettingsDialog(getShell(), this.navigatorSettings);
        if (dialog.open() == IDialogConstants.OK_ID) {
            this.navigatorSettings = dialog.getNavigatorSettings();
            updateNavigatorSettingsPreset();
        }
    }

    private void editPermissions() {
        EditConnectionPermissionsDialog dialog = new EditConnectionPermissionsDialog(getShell(), accessRestrictions);
        if (dialog.open() == IDialogConstants.OK_ID) {
            accessRestrictions = dialog.getAccessRestrictions();
        }
    }

    private void loadConnectionTypes()
    {
        connectionTypeCombo.removeAll();
        for (DBPConnectionType ct : DataSourceProviderRegistry.getInstance().getConnectionTypes()) {
            connectionTypeCombo.addItem(ct);
        }
    }

/*
    private void loadConnectionFolders()
    {
        connectionFolderCombo.removeAll();
        connectionFolderCombo.add(UINavigatorMessages.toolbar_datasource_selector_empty);
        connectionFolders.clear();
        connectionFolders.add(null);
        for (DBPDataSourceFolder folder : DBUtils.makeOrderedObjectList(getWizard().getDataSourceRegistry().getRootFolders())) {
            loadConnectionFolder(0, folder);
        }
    }

    private void loadConnectionFolder(int level, DBPDataSourceFolder folder) {
        String prefix = "";
        for (int i = 0; i < level; i++) {
            prefix += "   ";
        }

        connectionFolders.add(folder);
        connectionFolderCombo.add(prefix + folder.getName());
        for (DBPDataSourceFolder child : DBUtils.makeOrderedObjectList(folder.getChildren())) {
            loadConnectionFolder(level + 1, child);
        }
    }
*/

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
        final DBPConnectionConfiguration confConfig = dataSource.getConnectionConfiguration();

        String name = connectionNameChanged ? connectionNameText.getText() : generateConnectionName(getWizard().getPageSettings());
        dataSource.setName(name);
        //dataSource.setFolder(dataSourceFolder);

        if (connectionTypeCombo.getSelectionIndex() >= 0) {
            confConfig.setConnectionType(connectionTypeCombo.getItem(connectionTypeCombo.getSelectionIndex()));
        }

        DataSourceDescriptor dsDescriptor = (DataSourceDescriptor) dataSource;
        final String description = descriptionText.getText();
        if (description.isEmpty()) {
            dsDescriptor.setDescription(null);
        } else {
            dsDescriptor.setDescription(description);
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
        this.dataSourceFolder = dataSourceFolder;
    }

    private static class ConnectionTypeLabelProvider extends LabelProvider implements IColorProvider {
        @Override
        public String getText(Object element) {
            return ((DBPConnectionType)element).getName();
        }

        @Override
        public Color getForeground(Object element) {
            return null;
        }

        @Override
        public Color getBackground(Object element) {
            return UIUtils.getConnectionTypeColor((DBPConnectionType)element);
        }
    }

}