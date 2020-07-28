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
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Initialization connection page (common for all connection types)
 */
class ConnectionPageInitialization extends ConnectionWizardPage implements IDataSourceConnectionTester {
    static final String PAGE_NAME = ConnectionPageInitialization.class.getSimpleName();

    private static final Log log = Log.getLog(ConnectionPageInitialization.class);

    private DataSourceDescriptor dataSourceDescriptor;

    private Button autocommit;
    private Combo isolationLevel;
    private Combo defaultCatalog;
    private Combo defaultSchema;
    private Spinner keepAliveInterval;

    private Font boldFont;

    private boolean activated = false;
    private List<DBPTransactionIsolation> supportedLevels = new ArrayList<>();
    private List<String> bootstrapQueries;
    private boolean ignoreBootstrapErrors;

    private boolean txnOptionsLoaded = false;

    private ConnectionPageInitialization() {
        super(PAGE_NAME); //$NON-NLS-1$
        setTitle(CoreMessages.dialog_connection_wizard_connection_init);
        setDescription(CoreMessages.dialog_connection_wizard_connection_init_description);

        bootstrapQueries = new ArrayList<>();
    }

    ConnectionPageInitialization(DataSourceDescriptor dataSourceDescriptor) {
        this();
        this.dataSourceDescriptor = dataSourceDescriptor;

        bootstrapQueries = new ArrayList<>(dataSourceDescriptor.getConnectionConfiguration().getBootstrap().getInitQueries());
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
                isolationLevel.add("");

                DataSourceDescriptor originalDataSource = getWizard().getOriginalDataSource();
                if (originalDataSource != null && originalDataSource.isConnected()) {
                    DBPDataSource dataSource = originalDataSource.getDataSource();
                    loadDatabaseSettings(dataSource);
                }
                defaultCatalog.setText(CommonUtils.notEmpty(conConfig.getBootstrap().getDefaultCatalogName()));
                defaultSchema.setText(CommonUtils.notEmpty(conConfig.getBootstrap().getDefaultSchemaName()));
                keepAliveInterval.setSelection(conConfig.getKeepAliveInterval());
                activated = true;
            }
        } else {
            // Default settings
            isolationLevel.setEnabled(false);
            defaultCatalog.setText("");
            defaultSchema.setText("");
        }
    }

    private void loadDatabaseSettings(DBPDataSource dataSource) {
        try {
            getContainer().run(true, true, monitor -> {
                loadDatabaseSettings(new DefaultProgressMonitor(monitor), dataSource);
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Database info reading", "Error reading information from database", e.getTargetException());
        } catch (InterruptedException e) {
            //
        }
    }

    private void loadDatabaseSettings(DBRProgressMonitor monitor, DBPDataSource dataSource)  {
        DBPDataSourceContainer dataSourceContainer = dataSource.getContainer();
        Collection<DBPTransactionIsolation> txnLevels = CommonUtils.safeCollection(dataSource.getInfo().getSupportedTransactionsIsolation());
        Integer levelCode = dataSourceContainer.getDefaultTransactionsIsolation();

        UIUtils.syncExec(() -> {
            autocommit.setSelection(dataSourceContainer.isDefaultAutoCommit());
            //isolationLevel.setEnabled(!autocommit.getSelection());
            supportedLevels.clear();

            DBPTransactionIsolation defaultLevel = null;
                {
                if (levelCode != null && !CommonUtils.isEmpty(txnLevels)) {
                    for (DBPTransactionIsolation level : txnLevels) {
                        if (level.getCode() == levelCode) {
                            defaultLevel = level;
                            break;
                        }
                    }

                }
            }

            isolationLevel.removeAll();
            supportedLevels.clear();
            for (DBPTransactionIsolation level : txnLevels) {
                if (!level.isEnabled()) {
                    continue;
                }

                isolationLevel.add(level.getTitle());
                supportedLevels.add(level);

                if (level.equals(defaultLevel)) {
                    isolationLevel.select(isolationLevel.getItemCount() - 1);
                }
            }
        });

        if (dataSource instanceof DBSObjectContainer) {
            DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
            DBCExecutionContextDefaults contextDefaults = executionContext.getContextDefaults();
            DBSObjectContainer catalogContainer = DBUtils.getChangeableObjectContainer(contextDefaults, (DBSObjectContainer) dataSource, DBSCatalog.class);
            if (catalogContainer != null) {
                loadSelectableObject(monitor, catalogContainer, defaultCatalog, contextDefaults, true);
            }
            DBSObjectContainer schemaContainer = DBUtils.getChangeableObjectContainer(contextDefaults, (DBSObjectContainer) dataSource, DBSSchema.class);
            loadSelectableObject(monitor, schemaContainer, defaultSchema, contextDefaults, false);
        }

        txnOptionsLoaded = true;
    }

    private void loadSelectableObject(DBRProgressMonitor monitor, DBSObjectContainer objectContainer, Combo objectCombo, DBCExecutionContextDefaults contextDefaults, boolean isCatalogs) {
        if (objectContainer != null) {
            try {
                final List<String> objectNames = new ArrayList<>();
                Collection<? extends DBSObject> children = objectContainer.getChildren(monitor);
                if (children != null) {
                    for (DBSObject child : children) {
                        if (child instanceof DBSObjectContainer) {
                            objectNames.add(child.getName());
                        }
                    }
                }
                if (!objectNames.isEmpty()) {
                    UIUtils.syncExec(() -> {
                        if (!objectCombo.isDisposed()) {
                            String oldText = objectCombo.getText();
                            objectCombo.removeAll();
                            for (String name : objectNames) {
                                objectCombo.add(name);
                            }
                            if (!CommonUtils.isEmpty(oldText)) {
                                objectCombo.setText(oldText);
                            }
                            if (contextDefaults != null) {
                                DBSObject defaultObject = isCatalogs ? contextDefaults.getDefaultCatalog() : contextDefaults.getDefaultSchema();
                                if (defaultObject != null) {
                                    objectCombo.setText(defaultObject.getName());
                                }
                            }
                        }
                    });
                }
            } catch (DBException e) {
                log.warn("Can't read schema list", e);
            }
        }
    }

    @Override
    public void deactivatePage() {
    }

    @Override
    public void createControl(Composite parent) {
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite group = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group txnGroup = UIUtils.createControlGroup(group, CoreMessages.dialog_connection_wizard_final_label_connection, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            autocommit = UIUtils.createLabelCheckbox(
                txnGroup,
                CoreMessages.dialog_connection_wizard_final_checkbox_auto_commit,
                "Sets auto-commit mode for all connections",
                dataSourceDescriptor != null && dataSourceDescriptor.isDefaultAutoCommit());
            autocommit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            autocommit.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    isolationLevel.setEnabled(!autocommit.getSelection());
                }
            });

            isolationLevel = UIUtils.createLabelCombo(txnGroup, CoreMessages.dialog_connection_wizard_final_label_isolation_level,
                CoreMessages.dialog_connection_wizard_final_label_isolation_level_tooltip, SWT.DROP_DOWN | SWT.READ_ONLY);
            defaultCatalog = UIUtils.createLabelCombo(txnGroup, CoreMessages.dialog_connection_wizard_final_label_default_database,
                    CoreMessages.dialog_connection_wizard_final_label_default_database_tooltip, SWT.DROP_DOWN);
            ((GridData)defaultCatalog.getLayoutData()).widthHint = UIUtils.getFontHeight(defaultCatalog) * 20;
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
                            dataSourceDescriptor,
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

        CLabel infoLabel = UIUtils.createInfoLabel(group, CoreMessages.dialog_connection_wizard_connection_init_hint);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_END);
        gd.grabExcessHorizontalSpace = true;
        infoLabel.setLayoutData(gd);
        infoLabel.setToolTipText(CoreMessages.dialog_connection_wizard_connection_init_hint_tip);


        setControl(group);

        UIUtils.setHelp(group, IHelpContextIds.CTX_CON_WIZARD_FINAL);
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        if (dataSourceDescriptor != null && !activated) {
            // No changes anyway
            return;
        }
        dataSource.setDefaultAutoCommit(autocommit.getSelection());
        if (txnOptionsLoaded) {
            if (CommonUtils.isEmpty(isolationLevel.getText())) {
                dataSource.setDefaultTransactionsIsolation(null);
            } else {
                int levelIndex = isolationLevel.getSelectionIndex();
                if (levelIndex >= 0) {
                    dataSource.setDefaultTransactionsIsolation(supportedLevels.get(levelIndex));
                }
            }
        }
        final DBPConnectionConfiguration confConfig = dataSource.getConnectionConfiguration();
        DBPConnectionBootstrap bootstrap = confConfig.getBootstrap();
        bootstrap.setDefaultCatalogName(defaultCatalog.getText());
        bootstrap.setDefaultSchemaName(defaultSchema.getText());

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
                    if (autocommit != null) {
                        autocommit.setSelection(type.isAutocommit());
                    }
                }
            });
        }
    }

    @Override
    public void testConnection(DBCSession session) {
        // We load settings to fill txn isolation levels and schema names (#6794)
        loadDatabaseSettings(session.getProgressMonitor(), session.getDataSource());
    }

}
