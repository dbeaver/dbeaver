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
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Initialization connection page (common for all connection types)
 */
class ConnectionPageInitialization extends ConnectionWizardPage {
    public static final String PAGE_NAME = ConnectionPageInitialization.class.getSimpleName();

    private static final Log log = Log.getLog(ConnectionPageInitialization.class);

    private DataSourceDescriptor dataSourceDescriptor;

    private Button autocommit;
    private Combo isolationLevel;
    private Combo defaultSchema;
    private Spinner keepAliveInterval;

    private Button showSystemObjects;
    private Button showUtilityObjects;
    private Button readOnlyConnection;
    private Font boldFont;

    private List<FilterInfo> filters = new ArrayList<>();
    private Group filtersGroup;
    private boolean activated = false;
    private List<DBPTransactionIsolation> supportedLevels = new ArrayList<>();
    private List<String> bootstrapQueries;
    private boolean ignoreBootstrapErrors;

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

    ConnectionPageInitialization() {
        super(PAGE_NAME); //$NON-NLS-1$
        setTitle(CoreMessages.dialog_connection_wizard_connection_init);
        setDescription(CoreMessages.dialog_connection_wizard_connection_init_description);

        filters.add(new FilterInfo(DBSCatalog.class, CoreMessages.dialog_connection_wizard_final_filter_catalogs));
        filters.add(new FilterInfo(DBSSchema.class, CoreMessages.dialog_connection_wizard_final_filter_schemas_users));
        filters.add(new FilterInfo(DBSTable.class, CoreMessages.dialog_connection_wizard_final_filter_tables));
        filters.add(new FilterInfo(DBSEntityAttribute.class, CoreMessages.dialog_connection_wizard_final_filter_attributes));

        bootstrapQueries = new ArrayList<>();
    }

    ConnectionPageInitialization(DataSourceDescriptor dataSourceDescriptor) {
        this();
        this.dataSourceDescriptor = dataSourceDescriptor;

        for (FilterInfo filterInfo : filters) {
            filterInfo.filter = dataSourceDescriptor.getObjectFilter(filterInfo.type, null, false);
        }
        bootstrapQueries = dataSourceDescriptor.getConnectionConfiguration().getBootstrap().getInitQueries();
        ignoreBootstrapErrors = dataSourceDescriptor.getConnectionConfiguration().getBootstrap().isIgnoreErrors();
    }

    @Override
    public void dispose() {
        UIUtils.dispose(boldFont);
        super.dispose();
    }

    @Override
    public void activatePage() {
        if (dataSourceDescriptor != null) {
            if (!activated) {
                // Get settings from data source descriptor
                final DBPConnectionConfiguration conConfig = dataSourceDescriptor.getConnectionConfiguration();
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
                        DBSObjectContainer schemaContainer = (DBSObjectContainer) dataSource;
                        if (dataSource instanceof DBSObjectSelector) {
                            DBSObject defaultObject = ((DBSObjectSelector) dataSource).getDefaultObject();
                            if (defaultObject instanceof DBSObjectContainer) {
                                schemaContainer = (DBSObjectContainer) defaultObject;
                            }
                        }
                        new SchemaReadJob(schemaContainer).schedule();
                    }
                } else {
                    isolationLevel.setEnabled(false);
                }
                defaultSchema.setText(CommonUtils.notEmpty(
                    conConfig.getBootstrap().getDefaultObjectName()));
                keepAliveInterval.setSelection(conConfig.getKeepAliveInterval());
                activated = true;
            }
        } else {
            // Default settings
            showSystemObjects.setSelection(true);
            showUtilityObjects.setSelection(false);
            readOnlyConnection.setSelection(false);
            isolationLevel.setEnabled(false);
            defaultSchema.setText("");
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

    @Override
    public void deactivatePage() {
    }

    @Override
    public void createControl(Composite parent) {
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        group.setLayout(gl);

        {
            Composite optionsGroup = new Composite(group, SWT.NONE);
            gl = new GridLayout(1, true);
            gl.verticalSpacing = 0;
            gl.horizontalSpacing = 5;
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            optionsGroup.setLayout(gl);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            optionsGroup.setLayoutData(gd);

            {
                Group txnGroup = UIUtils.createControlGroup(optionsGroup, CoreMessages.dialog_connection_wizard_final_label_connection, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);
                autocommit = UIUtils.createLabelCheckbox(
                    txnGroup,
                    CoreMessages.dialog_connection_wizard_final_checkbox_auto_commit,
                    "Sets auto-commit mode for all connections",
                    dataSourceDescriptor != null && dataSourceDescriptor.isDefaultAutoCommit());
                autocommit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                autocommit.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (dataSourceDescriptor != null && dataSourceDescriptor.isConnected()) {
                            isolationLevel.setEnabled(!autocommit.getSelection());
                        }
                    }
                });

                isolationLevel = UIUtils.createLabelCombo(txnGroup, CoreMessages.dialog_connection_wizard_final_label_isolation_level,
                    CoreMessages.dialog_connection_wizard_final_label_isolation_level_tooltip, SWT.DROP_DOWN | SWT.READ_ONLY);
                defaultSchema = UIUtils.createLabelCombo(txnGroup, CoreMessages.dialog_connection_wizard_final_label_default_schema,
                    CoreMessages.dialog_connection_wizard_final_label_default_schema_tooltip, SWT.DROP_DOWN);
                ((GridData)defaultSchema.getLayoutData()).widthHint = UIUtils.getFontHeight(defaultSchema) * 20;
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
            }

            {
                Group miscGroup = UIUtils.createControlGroup(
                    optionsGroup,
                    CoreMessages.dialog_connection_wizard_final_group_misc,
                    1, GridData.VERTICAL_ALIGN_BEGINNING, 0);

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
                    group,
                    CoreMessages.dialog_connection_wizard_final_group_filters,
                    2, GridData.VERTICAL_ALIGN_BEGINNING, 0);
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
        }

        setControl(group);

        UIUtils.setHelp(group, IHelpContextIds.CTX_CON_WIZARD_FINAL);
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

    @Override
    public void saveSettings(DataSourceDescriptor dataSource) {
        if (dataSourceDescriptor != null && !activated) {
            // No changes anyway
            return;
        }
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

        final DBPConnectionConfiguration confConfig = dataSource.getConnectionConfiguration();

        for (FilterInfo filterInfo : filters) {
            if (filterInfo.filter != null) {
                dataSource.setObjectFilter(filterInfo.type, null, filterInfo.filter);
            }
        }
        DBPConnectionBootstrap bootstrap = confConfig.getBootstrap();
        bootstrap.setIgnoreErrors(ignoreBootstrapErrors);
        bootstrap.setInitQueries(bootstrapQueries);

        confConfig.setKeepAliveInterval(keepAliveInterval.getSelection());
    }

    @Override
    public void setWizard(IWizard newWizard) {
        super.setWizard(newWizard);
        if (newWizard instanceof ConnectionWizard && !((ConnectionWizard) newWizard).isNew()) {
            // Listen for connection type change
            ((ConnectionWizard) newWizard).addPropertyChangeListener(event -> {
                if (ConnectionWizard.PROP_CONNECTION_TYPE.equals(event.getProperty())) {
                    DBPConnectionType type = (DBPConnectionType) event.getNewValue();
                    autocommit.setSelection(type.isAutocommit());
                }
            });
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
                        if (child instanceof DBSObjectContainer) {
                            schemaNames.add(child.getName());
                        }
                    }
                }
                if (!schemaNames.isEmpty()) {
                    UIUtils.syncExec(() -> {
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
                    });
                }
            } catch (DBException e) {
                log.warn("Can't read schema list", e);
            }
            return Status.OK_STATUS;
        }
    }

}
