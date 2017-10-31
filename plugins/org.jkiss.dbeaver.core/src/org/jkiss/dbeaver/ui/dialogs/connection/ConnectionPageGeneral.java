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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.preferences.PrefPageConnectionTypes;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * General connection page (common for all connection types)
 */
class ConnectionPageGeneral extends ActiveWizardPage<ConnectionWizard> {
    private static final Log log = Log.getLog(ConnectionPageGeneral.class);

    private ConnectionWizard wizard;
    private DataSourceDescriptor dataSourceDescriptor;
    private Text connectionNameText;
    private CSmartCombo<DBPConnectionType> connectionTypeCombo;
    private CSmartCombo<DBPDataSourceFolder> connectionFolderCombo;
    private Button savePasswordCheck;
    private Button autocommit;
    private Combo isolationLevel;
    private Combo defaultSchema;
    private Spinner keepAliveInterval;

    private Button showSystemObjects;
    private Button showUtilityObjects;
    private Button readOnlyConnection;
    private Button eventsButton;
    private Font boldFont;

    private boolean connectionNameChanged = false;
    private List<FilterInfo> filters = new ArrayList<>();
    private Group filtersGroup;
    private boolean activated = false;
    private List<DBPTransactionIsolation> supportedLevels = new ArrayList<>();
    private List<String> bootstrapQueries;
    private boolean ignoreBootstrapErrors;
    private Text descriptionText;
    private DBPDataSourceFolder dataSourceFolder;

    private static class FilterInfo {
        final Class<?> type;
        final String title;
        Link link;
        DBSObjectFilter filter;

        private FilterInfo(Class<?> type, String title)
        {
            this.type = type;
            this.title = title;
        }
    }
    
    ConnectionPageGeneral(ConnectionWizard wizard)
    {
        super("newConnectionFinal"); //$NON-NLS-1$
        this.wizard = wizard;
        setTitle(wizard.isNew() ? CoreMessages.dialog_connection_wizard_final_header : CoreMessages.dialog_connection_edit_wizard_general);
        setDescription(CoreMessages.dialog_connection_wizard_final_description);

        filters.add(new FilterInfo(DBSCatalog.class, CoreMessages.dialog_connection_wizard_final_filter_catalogs));
        filters.add(new FilterInfo(DBSSchema.class, CoreMessages.dialog_connection_wizard_final_filter_schemas_users));
        filters.add(new FilterInfo(DBSTable.class, CoreMessages.dialog_connection_wizard_final_filter_tables));

        bootstrapQueries = new ArrayList<>();
    }

    ConnectionPageGeneral(ConnectionWizard wizard, DataSourceDescriptor dataSourceDescriptor)
    {
        this(wizard);
        this.dataSourceDescriptor = dataSourceDescriptor;

        for (FilterInfo filterInfo : filters) {
            filterInfo.filter = dataSourceDescriptor.getObjectFilter(filterInfo.type, null, false);
        }
        bootstrapQueries = dataSourceDescriptor.getConnectionConfiguration().getBootstrap().getInitQueries();
        ignoreBootstrapErrors = dataSourceDescriptor.getConnectionConfiguration().getBootstrap().isIgnoreErrors();
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
            ConnectionPageSettings settings = wizard.getPageSettings();
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
                        newName = connectionInfo.getUrl();
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
            } else {
                newName = wizard.getSelectedDriver().getName();
            }
            if (CommonUtils.isEmpty(connectionNameText.getText()) || !connectionNameChanged) {
                if (newName != null) {
                    connectionNameText.setText(newName);
                }
                connectionNameChanged = false;
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
                    connectionFolderCombo.select(dataSourceFolder);
                }
                savePasswordCheck.setSelection(dataSourceDescriptor.isSavePassword());
                autocommit.setSelection(dataSourceDescriptor.isDefaultAutoCommit());
                showSystemObjects.setSelection(dataSourceDescriptor.isShowSystemObjects());
                showUtilityObjects.setSelection(dataSourceDescriptor.isShowUtilityObjects());
                readOnlyConnection.setSelection(dataSourceDescriptor.isConnectionReadOnly());
                isolationLevel.add("");

                DataSourceDescriptor originalDataSource = getWizard().getOriginalDataSource();
                if (originalDataSource != null && originalDataSource.isConnected()) {
                    DBPDataSource dataSource = originalDataSource.getDataSource();
                    isolationLevel.setEnabled(!autocommit.getSelection());
                    supportedLevels.clear();
                    DBPTransactionIsolation defaultLevel = dataSourceDescriptor.getActiveTransactionsIsolation();
                    for (DBPTransactionIsolation level : CommonUtils.safeCollection(dataSource.getInfo().getSupportedTransactionsIsolation())) {
                        if (!level.isEnabled()) continue;
                        isolationLevel.add(level.getTitle());
                        supportedLevels.add(level);
                        if (level.equals(defaultLevel)) {
                            isolationLevel.select(isolationLevel.getItemCount() - 1);
                        }
                    }
                    if (dataSource instanceof DBSObjectContainer) {
                        new SchemaReadJob((DBSObjectContainer)dataSource).schedule();
                    }
                } else {
                    isolationLevel.setEnabled(false);
                }
                defaultSchema.setText(CommonUtils.notEmpty(
                    conConfig.getBootstrap().getDefaultObjectName()));
                keepAliveInterval.setSelection(conConfig.getKeepAliveInterval());
                if (dataSourceDescriptor.getDescription() != null) {
                    descriptionText.setText(dataSourceDescriptor.getDescription());
                }
                activated = true;
            }
        } else {
            if (eventsButton != null) {
                eventsButton.setFont(getFont());
                DataSourceDescriptor dataSource = getActiveDataSource();
                for (DBPConnectionEventType eventType : dataSource.getConnectionConfiguration().getDeclaredEvents()) {
                    if (dataSource.getConnectionConfiguration().getEvent(eventType).isEnabled()) {
                        eventsButton.setFont(boldFont);
                        break;
                    }
                }
            }
            // Default settings
            savePasswordCheck.setSelection(true);
            connectionTypeCombo.select(0);
            autocommit.setSelection((connectionTypeCombo.getItem(0)).isAutocommit());
            if (dataSourceFolder != null) {
                connectionFolderCombo.select(dataSourceFolder);
            } else {
                connectionFolderCombo.select(0);
            }
            showSystemObjects.setSelection(true);
            showUtilityObjects.setSelection(false);
            readOnlyConnection.setSelection(false);
            isolationLevel.setEnabled(false);
            defaultSchema.setText("");
        }
        if (savePasswordCheck != null) {
            //savePasswordCheck.setEnabled();
        }
        long features = wizard.getSelectedDriver().getDataSourceProvider().getFeatures();

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

    @NotNull
    private DataSourceDescriptor getActiveDataSource() {
        ConnectionPageSettings pageSettings = getWizard().getPageSettings();
        return pageSettings == null ? wizard.getActiveDataSource() : pageSettings.getActiveDataSource();
    }

    private void enableFilter(FilterInfo filterInfo, boolean enable)
    {
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
            filterInfo.link.setToolTipText(NLS.bind(CoreMessages.dialog_connection_wizard_final_filter_link_not_supported_tooltip, filterInfo.title, wizard.getSelectedDriver().getName()));
        }
    }

    @Override
    public void deactivatePage()
    {
    }

    @Override
    public void createControl(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        group.setLayout(gl);

        String connectionName = dataSourceDescriptor == null ? "" : dataSourceDescriptor.getName(); //$NON-NLS-1$
        connectionNameText = UIUtils.createLabelText(group, CoreMessages.dialog_connection_wizard_final_label_connection_name, CommonUtils.toString(connectionName));
        connectionNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                connectionNameChanged = true;
                ConnectionPageGeneral.this.getContainer().updateButtons();
            }
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
                    autocommit.setSelection(type.isAutocommit());
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
                    autocommit.setSelection(connectionType.isAutocommit());
                }
            });
        }

        {
            UIUtils.createControlLabel(group, CoreMessages.dialog_connection_wizard_final_label_connection_folder);

            connectionFolderCombo = new CSmartCombo<>(group, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY, new ConnectionFolderLabelProvider());
            //connectionFolderCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            loadConnectionFolders();
            connectionFolderCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    dataSourceFolder = connectionFolderCombo.getItem(connectionFolderCombo.getSelectionIndex());
                }
            });
        }

        {
            Composite optionsGroup = new Composite(group, SWT.NONE);
            gl = new GridLayout(2, true);
            gl.verticalSpacing = 0;
            gl.horizontalSpacing = 5;
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            optionsGroup.setLayout(gl);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            optionsGroup.setLayoutData(gd);
            Composite leftSide = UIUtils.createPlaceholder(optionsGroup, 1, 5);
            leftSide.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
            Composite rightSide = UIUtils.createPlaceholder(optionsGroup, 1, 5);
            rightSide.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
            {
                Group securityGroup = UIUtils.createControlGroup(leftSide, CoreMessages.dialog_connection_wizard_final_group_security, 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);

                savePasswordCheck = UIUtils.createCheckbox(securityGroup, CoreMessages.dialog_connection_wizard_final_checkbox_save_password_locally, dataSourceDescriptor == null || dataSourceDescriptor.isSavePassword());
                savePasswordCheck.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            }

            {
                Group txnGroup = UIUtils.createControlGroup(rightSide, CoreMessages.dialog_connection_wizard_final_label_connection, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
                autocommit = UIUtils.createLabelCheckbox(
                    txnGroup,
                    CoreMessages.dialog_connection_wizard_final_checkbox_auto_commit,
                    "Sets auto-commit mode for all connections",
                    dataSourceDescriptor != null && dataSourceDescriptor.isDefaultAutoCommit());
                autocommit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                autocommit.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        if (dataSourceDescriptor != null && dataSourceDescriptor.isConnected()) {
                            isolationLevel.setEnabled(!autocommit.getSelection());
                        }
                    }
                });

                isolationLevel = UIUtils.createLabelCombo(txnGroup, CoreMessages.dialog_connection_wizard_final_label_isolation_level, 
                		CoreMessages.dialog_connection_wizard_final_label_isolation_level_tooltip, SWT.DROP_DOWN | SWT.READ_ONLY);
                defaultSchema = UIUtils.createLabelCombo(txnGroup, CoreMessages.dialog_connection_wizard_final_label_default_schema, 
                		CoreMessages.dialog_connection_wizard_final_label_default_schema_tooltip, SWT.DROP_DOWN);
                keepAliveInterval = UIUtils.createLabelSpinner(txnGroup, CoreMessages.dialog_connection_wizard_final_label_keepalive,
                		CoreMessages.dialog_connection_wizard_final_label_keepalive_tooltip, 0, 0, Short.MAX_VALUE);

                {
                    String bootstrapTooltip = CoreMessages.dialog_connection_wizard_final_label_bootstrap_tooltip;
                    UIUtils.createControlLabel(txnGroup, CoreMessages.dialog_connection_wizard_final_label_bootstrap_query).setToolTipText(bootstrapTooltip);
                    final Button queriesConfigButton = UIUtils.createPushButton(txnGroup, CoreMessages.dialog_connection_wizard_configure, DBeaverIcons.getImage(UIIcon.SQL_SCRIPT));
                    queriesConfigButton.setToolTipText(bootstrapTooltip);
                    if (dataSourceDescriptor != null && !CommonUtils.isEmpty(dataSourceDescriptor.getConnectionConfiguration().getBootstrap().getInitQueries())) {
                        queriesConfigButton.setFont(boldFont);
                    }
                    queriesConfigButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            EditBootstrapQueriesDialog dialog = new EditBootstrapQueriesDialog(
                                getShell(),
                                bootstrapQueries,
                                ignoreBootstrapErrors);
                            if (dialog.open() == IDialogConstants.OK_ID) {
                                bootstrapQueries = dialog.getQueries();
                                ignoreBootstrapErrors = dialog.isIgnoreErrors();
                            }
                        }
                    });
                }

                if (getWizard().isNew()) {
                    UIUtils.createControlLabel(txnGroup, CoreMessages.dialog_connection_wizard_final_label_shell_command);
                    eventsButton = new Button(txnGroup, SWT.PUSH);
                    eventsButton.setText(CoreMessages.dialog_connection_wizard_configure);
                    eventsButton.setImage(DBeaverIcons.getImage(UIIcon.EVENT));
                    eventsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                    eventsButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e)
                        {
                            configureEvents();
                        }
                    });
                }
            }

            {
                Group miscGroup = UIUtils.createControlGroup(
                    leftSide,
                    CoreMessages.dialog_connection_wizard_final_group_misc,
                    1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);

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
                //gd.horizontalSpan = 2;
                readOnlyConnection.setLayoutData(gd);
            }
            {
                // Filters
                filtersGroup = UIUtils.createControlGroup(
                    leftSide,
                    CoreMessages.dialog_connection_wizard_final_group_filters,
                    1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);

                for (int i = 0; i < filters.size(); i++) {
                    final FilterInfo filterInfo = filters.get(i);
                    filterInfo.link = UIUtils.createLink(filtersGroup, "<a>" + filterInfo.title + "</a>", new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            EditObjectFilterDialog dialog = new EditObjectFilterDialog(
                                getShell(),
                                dataSourceDescriptor.getRegistry(),
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
        }

        {
            final Group descGroup = UIUtils.createControlGroup(group, CoreMessages.dialog_connection_wizard_description, 1, GridData.FILL_HORIZONTAL, 0);
            ((GridData) descGroup.getLayoutData()).horizontalSpan = 2;
            descriptionText = new Text(descGroup, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = descriptionText.getLineHeight() * 3;
            descriptionText.setLayoutData(gd);
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
        connectionFolderCombo.addItem(null);
        for (DBPDataSourceFolder folder : getWizard().getDataSourceRegistry().getRootFolders()) {
            loadConnectionFolder(0, folder);
        }
    }

    private void loadConnectionFolder(int level, DBPDataSourceFolder folder) {
        connectionFolderCombo.addItem(folder);
        for (DBPDataSourceFolder child : folder.getChildren()) {
            loadConnectionFolder(level + 1, child);
        }
    }

    @Override
    public boolean isPageComplete()
    {
        return connectionNameText != null &&
            !CommonUtils.isEmpty(connectionNameText.getText());
    }

    void saveSettings(DataSourceDescriptor dataSource)
    {
        if (dataSourceDescriptor != null && !activated) {
            // No changes anyway
            return;
        }
        dataSource.setName(connectionNameText.getText());
        dataSource.setSavePassword(savePasswordCheck.getSelection());
        try {
            dataSource.setDefaultAutoCommit(autocommit.getSelection(), null, true, null);
            if (dataSource.isConnected()) {
                int levelIndex = isolationLevel.getSelectionIndex();
                if (levelIndex <= 0) {
                    dataSource.setDefaultTransactionsIsolation(null);
                } else {
                    dataSource.setDefaultTransactionsIsolation(supportedLevels.get(levelIndex - 1));
                }
            }
        } catch (DBException e) {
            log.error(e);
        }
        dataSource.setDefaultActiveObject(defaultSchema.getText());
        dataSource.setShowSystemObjects(showSystemObjects.getSelection());
        dataSource.setShowUtilityObjects(showUtilityObjects.getSelection());
        dataSource.setConnectionReadOnly(readOnlyConnection.getSelection());
        if (!dataSource.isSavePassword()) {
            dataSource.resetPassword();
        }
        dataSource.setFolder(dataSourceFolder);

        final DBPConnectionConfiguration confConfig = dataSource.getConnectionConfiguration();

        if (connectionTypeCombo.getSelectionIndex() >= 0) {
            confConfig.setConnectionType(connectionTypeCombo.getItem(connectionTypeCombo.getSelectionIndex()));
        }
        for (FilterInfo filterInfo : filters) {
            if (filterInfo.filter != null) {
                dataSource.setObjectFilter(filterInfo.type, null, filterInfo.filter);
            }
        }
        DBPConnectionBootstrap bootstrap = confConfig.getBootstrap();
        bootstrap.setIgnoreErrors(ignoreBootstrapErrors);
        bootstrap.setInitQueries(bootstrapQueries);

        confConfig.setKeepAliveInterval(keepAliveInterval.getSelection());

        final String description = descriptionText.getText();
        if (description.isEmpty()) {
            dataSource.setDescription(null);
        } else {
            dataSource.setDescription(description);
        }
    }

    private void configureEvents()
    {
        DataSourceDescriptor dataSource = getActiveDataSource();
        EditShellCommandsDialog dialog = new EditShellCommandsDialog(
            getShell(),
            dataSource);
        if (dialog.open() == IDialogConstants.OK_ID) {
            eventsButton.setFont(getFont());
            for (DBPConnectionEventType eventType : dataSource.getConnectionConfiguration().getDeclaredEvents()) {
                if (dataSource.getConnectionConfiguration().getEvent(eventType).isEnabled()) {
                    eventsButton.setFont(boldFont);
                    break;
                }
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

    private static class ConnectionFolderLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element) {
            return DBeaverIcons.getImage(DBIcon.TREE_DATABASE_CATEGORY);
        }

        @Override
        public String getText(Object element) {
            if (element == null) {
                return CoreMessages.toolbar_datasource_selector_empty;
            }
            String prefix = "";
            for (DBPDataSourceFolder folder = ((DBPDataSourceFolder) element).getParent(); folder != null; folder = folder.getParent()) {
                prefix += "   ";
            }
            return prefix + ((DBPDataSourceFolder)element).getName();
        }
    }

    private class SchemaReadJob extends AbstractJob {
        private DBSObjectContainer objectContainer;
        public SchemaReadJob(DBSObjectContainer objectContainer) {
            super("Schema reader");
            this.objectContainer = objectContainer;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                final List<String> schemaNames = new ArrayList<>();
                Collection<? extends DBSObject> children = objectContainer.getChildren(monitor);
                if (children != null) {
                    for (DBSObject child : children) {
                        schemaNames.add(child.getName());
                    }
                }
                if (!schemaNames.isEmpty()) {
                    DBeaverUI.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!defaultSchema.isDisposed()) {
                                String oldText = defaultSchema.getText();
                                defaultSchema.removeAll();
                                for (String name : schemaNames) {
                                    defaultSchema.add(name);
                                }
                                if (!CommonUtils.isEmpty(oldText)) {
                                    defaultSchema.setText(oldText);
                                }
                            }
                        }
                    });
                }
            } catch (DBException e) {
                log.warn("Can't read schema list", e);
            }
            return Status.OK_STATUS;
        }
    }
}