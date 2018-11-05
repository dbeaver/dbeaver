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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
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
    private Combo connectionFolderCombo;
    private Text descriptionText;

    private boolean connectionNameChanged = false;
    private boolean activated = false;
    private DBPDataSourceFolder dataSourceFolder;
    private List<DBPDataSourceFolder> connectionFolders = new ArrayList<>();

    private Button showSystemObjects;
    private Button showUtilityObjects;
    private Button readOnlyConnection;

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

    }

    ConnectionPageGeneral(ConnectionWizard wizard, DataSourceDescriptor dataSourceDescriptor)
    {
        this(wizard);
        this.dataSourceDescriptor = dataSourceDescriptor;

        for (FilterInfo filterInfo : filters) {
            filterInfo.filter = dataSourceDescriptor.getObjectFilter(filterInfo.type, null, false);
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
                dataSourceFolder = dataSourceDescriptor.getFolder();
                if (dataSourceDescriptor.getFolder() == null) {
                    connectionFolderCombo.select(0);
                } else {
                    connectionFolderCombo.select(connectionFolders.indexOf(dataSourceFolder));
                }
                if (dataSourceDescriptor.getDescription() != null) {
                    descriptionText.setText(dataSourceDescriptor.getDescription());
                }

                showSystemObjects.setSelection(dataSourceDescriptor.isShowSystemObjects());
                showUtilityObjects.setSelection(dataSourceDescriptor.isShowUtilityObjects());
                readOnlyConnection.setSelection(dataSourceDescriptor.isConnectionReadOnly());

                activated = true;
            }
        } else {
            // Default settings
            connectionTypeCombo.select(0);
            if (dataSourceFolder != null) {
                connectionFolderCombo.select(connectionFolders.indexOf(dataSourceFolder));
            } else {
                connectionFolderCombo.select(0);
            }

            showSystemObjects.setSelection(true);
            showUtilityObjects.setSelection(false);
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
            filterInfo.link.setText(NLS.bind(CoreMessages.dialog_connection_wizard_final_filter_link_not_supported_text, filterInfo.title));
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
                if (!CommonUtils.isEmpty(settings.getDriver().getCategory())) {
                    newName = settings.getDriver().getCategory() + " - " + newName; //$NON-NLS-1$
                } else {
                    newName = settings.getDriver().getName() + " - " + newName; //$NON-NLS-1$
                }
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

        Composite group = UIUtils.createPlaceholder(parent, 1, 5);

        String connectionName = dataSourceDescriptor == null ? "" : dataSourceDescriptor.getName(); //$NON-NLS-1$
        connectionNameText = UIUtils.createLabelText(group, CoreMessages.dialog_connection_wizard_final_label_connection_name, CommonUtils.toString(connectionName));
        connectionNameText.addModifyListener(e -> {
            connectionNameChanged = true;
            ConnectionPageGeneral.this.getContainer().updateButtons();
        });

        {
            UIUtils.createControlLabel(group, CoreMessages.dialog_connection_wizard_final_label_connection_type);

            Composite ctGroup = UIUtils.createPlaceholder(group, 2, 5);
            connectionTypeCombo = new CSmartCombo<>(ctGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY, new ConnectionTypeLabelProvider());
            loadConnectionTypes();
            connectionTypeCombo.select(0);
            connectionTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    DBPConnectionType type = connectionTypeCombo.getItem(connectionTypeCombo.getSelectionIndex());
                    getWizard().firePropertyChangeEvent(ConnectionWizard.PROP_CONNECTION_TYPE, getActiveDataSource().getConnectionConfiguration().getConnectionType(), type);
                }
            });

            Button pickerButton = new Button(ctGroup, SWT.PUSH);
            pickerButton.setText(CoreMessages.dialog_connection_wizard_final_label_edit);
            pickerButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DataSourceDescriptor dataSource = getActiveDataSource();
                    UIUtils.showPreferencesFor(
                        getControl().getShell(),
                        dataSource.getConnectionConfiguration().getConnectionType(),
                        PrefPageConnectionTypes.PAGE_ID);
                    loadConnectionTypes();
                    DBPConnectionType connectionType = dataSource.getConnectionConfiguration().getConnectionType();
                    connectionTypeCombo.select(connectionType);
                    getWizard().firePropertyChangeEvent(ConnectionWizard.PROP_CONNECTION_TYPE, connectionType, connectionType);
                }
            });
        }

        {
            UIUtils.createControlLabel(group, CoreMessages.dialog_connection_wizard_final_label_connection_folder);

            connectionFolderCombo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(connectionFolderCombo) * 30;
            connectionFolderCombo.setLayoutData(gd);
            loadConnectionFolders();
            connectionFolderCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    dataSourceFolder = connectionFolders.get(connectionFolderCombo.getSelectionIndex());
                }
            });
        }

        {
            Label descLabel = UIUtils.createControlLabel(group, CoreMessages.dialog_connection_wizard_description);
            descriptionText = new Text(group, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
            final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            gd.heightHint = descriptionText.getLineHeight() * 3;
            descriptionText.setLayoutData(gd);
        }

        Composite refsGroup = UIUtils.createPlaceholder(group, 2, 5);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        refsGroup.setLayoutData(gd);

        {
            Group miscGroup = UIUtils.createControlGroup(
                refsGroup,
                CoreMessages.dialog_connection_wizard_final_group_misc,
                1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            miscGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            showSystemObjects = UIUtils.createCheckbox(
                miscGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_show_system_objects,
                dataSourceDescriptor == null || dataSourceDescriptor.isShowSystemObjects());
            showSystemObjects.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            showUtilityObjects = UIUtils.createCheckbox(
                miscGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_show_util_objects,
                dataSourceDescriptor == null || dataSourceDescriptor.isShowUtilityObjects());
            showUtilityObjects.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            readOnlyConnection = UIUtils.createCheckbox(
                miscGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_connection_readonly,
                dataSourceDescriptor != null && dataSourceDescriptor.isConnectionReadOnly());
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            readOnlyConnection.setLayoutData(gd);
        }

        {
            // Filters
            filtersGroup = UIUtils.createControlGroup(
                refsGroup,
                CoreMessages.dialog_connection_wizard_final_group_filters,
                2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
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
            }
        }
        {
            Composite linkGroup = UIUtils.createPlaceholder(refsGroup, 1, 5);
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

    private void loadConnectionTypes()
    {
        connectionTypeCombo.removeAll();
        for (DBPConnectionType ct : DataSourceProviderRegistry.getInstance().getConnectionTypes()) {
            connectionTypeCombo.addItem(ct);
        }
    }

    private void loadConnectionFolders()
    {
        connectionFolderCombo.removeAll();
        connectionFolderCombo.add(CoreMessages.toolbar_datasource_selector_empty);
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

    @Override
    public boolean isPageComplete()
    {
        return true;
    }

    @Override
    public void saveSettings(DataSourceDescriptor dataSource) {
        if (dataSourceDescriptor != null && !activated) {
            // No changes anyway
            return;
        }
        final DBPConnectionConfiguration confConfig = dataSource.getConnectionConfiguration();

        String name = connectionNameChanged ? connectionNameText.getText() : generateConnectionName(getWizard().getPageSettings());
        dataSource.setName(name);
        dataSource.setFolder(dataSourceFolder);

        if (connectionTypeCombo.getSelectionIndex() >= 0) {
            confConfig.setConnectionType(connectionTypeCombo.getItem(connectionTypeCombo.getSelectionIndex()));
        }

        final String description = descriptionText.getText();
        if (description.isEmpty()) {
            dataSource.setDescription(null);
        } else {
            dataSource.setDescription(description);
        }

        dataSource.setShowSystemObjects(showSystemObjects.getSelection());
        dataSource.setShowUtilityObjects(showUtilityObjects.getSelection());
        dataSource.setConnectionReadOnly(readOnlyConnection.getSelection());

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