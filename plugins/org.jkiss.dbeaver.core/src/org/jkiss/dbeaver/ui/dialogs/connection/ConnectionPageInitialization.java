/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.preferences.PrefPageConnectionClient;
import org.jkiss.dbeaver.ui.preferences.PrefPageConnectionTypes;
import org.jkiss.dbeaver.ui.preferences.WizardPrefPage;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Initialization connection page
 */
class ConnectionPageInitialization extends ConnectionWizardPage implements IDialogPageProvider, IDataSourceConnectionTester {
    static final String PAGE_NAME = ConnectionPageInitialization.class.getSimpleName();

    private static final Log log = Log.getLog(ConnectionPageInitialization.class);

    private static final String PAGE_DOCS_LINK = "Configure-Connection-Initialization-Settings";

    private DataSourceDescriptor dataSourceDescriptor;

    private Combo autocommit;
    private Combo isolationLevel;
    private Combo defaultCatalog;
    private Combo defaultSchema;
    private Spinner keepAliveInterval;
    private Button closeIdleConnectionsCheck;
    private Spinner closeIdleConnectionsPeriod;

    private boolean activated = false;
    private final List<DBPTransactionIsolation> supportedLevels = new ArrayList<>();
    private List<String> bootstrapQueries;
    private boolean ignoreBootstrapErrors;

    private boolean txnOptionsLoaded = false;
    private ConnectionPageShellCommands shellCommandPage;
    private WizardPrefPage clientAppPage;

    private ConnectionPageInitialization() {
        super(PAGE_NAME); //$NON-NLS-1$
        setTitle(CoreMessages.dialog_connection_wizard_connection_init);
        setDescription(CoreMessages.dialog_connection_wizard_connection_init_description);
    }

    ConnectionPageInitialization(@NotNull DataSourceDescriptor dataSourceDescriptor) {
        this();
        this.dataSourceDescriptor = dataSourceDescriptor;

        bootstrapQueries = new ArrayList<>(dataSourceDescriptor.getConnectionConfiguration().getBootstrap().getInitQueries());
        ignoreBootstrapErrors = dataSourceDescriptor.getConnectionConfiguration().getBootstrap().isIgnoreErrors();
        shellCommandPage = new ConnectionPageShellCommands(dataSourceDescriptor);
        if (!dataSourceDescriptor.getDriver().isEmbedded()) {
            clientAppPage = new WizardPrefPage(
                new PrefPageConnectionClient(),
                CoreMessages.dialog_connection_edit_wizard_connections,
                CoreMessages.dialog_connection_edit_wizard_connections_description
            );
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    private static int getAutocommitSelIndex(DBPConnectionConfiguration configuration) {
        Boolean defaultAutoCommit = configuration.getBootstrap().getDefaultAutoCommit();
        return defaultAutoCommit == null ? 0 : defaultAutoCommit ? 1 : 2;
    }

    private static Boolean getAutocommitValueFromIndex(int index) {
        return switch (index) {
            case 1 -> true;
            case 2 -> false;
            default -> null;
        };
    }

    @Override
    public void activatePage() {
        if (dataSourceDescriptor != null) {
            if (!activated) {
                final DBPConnectionConfiguration conConfig = dataSourceDescriptor.getConnectionConfiguration();
                // Get settings from data source descriptor
                autocommit.select(getAutocommitSelIndex(dataSourceDescriptor.getConnectionConfiguration()));
                isolationLevel.add("");

                DataSourceDescriptor originalDataSource = getWizard().getOriginalDataSource();
                if (originalDataSource != null && originalDataSource.isConnected()) {
                    DBPDataSource dataSource = originalDataSource.getDataSource();
                    loadDatabaseSettings(dataSource);
                }
                defaultCatalog.setText(CommonUtils.notEmpty(conConfig.getBootstrap().getDefaultCatalogName()));
                defaultSchema.setText(CommonUtils.notEmpty(conConfig.getBootstrap().getDefaultSchemaName()));
                keepAliveInterval.setSelection(conConfig.getKeepAliveInterval());
                closeIdleConnectionsCheck.setSelection(conConfig.isCloseIdleConnection());
                closeIdleConnectionsPeriod.setSelection(
                    conConfig.getCloseIdleInterval() > 0 ?
                        conConfig.getCloseIdleInterval() :
                        conConfig.getConnectionType().getCloseIdleConnectionPeriod());
                closeIdleConnectionsPeriod.setEnabled(closeIdleConnectionsCheck.getSelection());
                activated = true;
            }
        } else {
            // Default settings
            isolationLevel.setEnabled(false);
            defaultCatalog.setText("");
            defaultSchema.setText("");
        }
    }

    @Nullable
    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        List<IDialogPage> pages = new ArrayList<>();
        pages.add(shellCommandPage);
        if (clientAppPage != null) {
            pages.add(clientAppPage);
        }
        return pages.toArray(new IDialogPage[0]);
    }

    private void loadDatabaseSettings(DBPDataSource dataSource) {
        try {
            getWizard().getRunnableContext().run(true, true, monitor ->
                loadDatabaseSettings(monitor, dataSource));
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
            autocommit.select(getAutocommitSelIndex(dataSourceDescriptor.getConnectionConfiguration()));
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
            if (executionContext != null) {
                DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
                DBSObjectContainer catalogContainer = DBUtils.getChangeableObjectContainer(
                    contextDefaults,
                    (DBSObjectContainer) dataSource,
                    DBSCatalog.class
                );
                if (catalogContainer != null) {
                    loadSelectableObject(monitor, catalogContainer, defaultCatalog, contextDefaults, true);
                }
                DBSObjectContainer schemaContainer = DBUtils.getChangeableObjectContainer(
                    contextDefaults,
                    (DBSObjectContainer) dataSource,
                    DBSSchema.class
                );
                loadSelectableObject(monitor, schemaContainer, defaultSchema, contextDefaults, false);
            }
        }

        txnOptionsLoaded = true;
    }

    private void loadSelectableObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBSObjectContainer objectContainer,
        @NotNull Combo objectCombo,
        @Nullable DBCExecutionContextDefaults<?, ?> contextDefaults,
        boolean isCatalogs
    ) {
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
    public void createControl(Composite parent) {
        Composite group = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Composite txnGroup = UIUtils.createControlGroup(
                group,
                CoreMessages.dialog_connection_edit_wizard_transactions,
                2,
                GridData.HORIZONTAL_ALIGN_BEGINNING,
                -1
            );

            autocommit = UIUtils.createLabelCombo(
                txnGroup,
                CoreMessages.action_menu_transactionMonitor_autocommitMode,
                "Sets auto-commit mode for this connection.\nIf set to default then connection type configuration will be used.",
                SWT.DROP_DOWN | SWT.READ_ONLY);
            autocommit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            autocommit.add("Default");
            autocommit.add("Auto commit");
            autocommit.add("Manual commit");

            isolationLevel = UIUtils.createLabelCombo(txnGroup, CoreMessages.dialog_connection_wizard_final_label_isolation_level,
                CoreMessages.dialog_connection_wizard_final_label_isolation_level_tooltip, SWT.DROP_DOWN | SWT.READ_ONLY);
        }
        UIUtils.createPreferenceLink(
            group,
            CoreMessages.action_menu_transaction_pref_page_link_extended,
            PrefPageConnectionTypes.PAGE_ID,
            null, null
        );

        {
            Composite conGroup = UIUtils.createControlGroup(
                group,
                CoreMessages.dialog_connection_wizard_final_label_connection,
                2,
                GridData.HORIZONTAL_ALIGN_BEGINNING,
                -1
            );

            defaultCatalog = UIUtils.createLabelCombo(conGroup, CoreMessages.dialog_connection_wizard_final_label_default_database,
                    CoreMessages.dialog_connection_wizard_final_label_default_database_tooltip, SWT.DROP_DOWN);
            ((GridData)defaultCatalog.getLayoutData()).widthHint = UIUtils.getFontHeight(defaultCatalog) * 20;
            defaultSchema = UIUtils.createLabelCombo(conGroup, CoreMessages.dialog_connection_wizard_final_label_default_schema,
                CoreMessages.dialog_connection_wizard_final_label_default_schema_tooltip, SWT.DROP_DOWN);
            ((GridData)defaultSchema.getLayoutData()).widthHint = UIUtils.getFontHeight(defaultSchema) * 20;
            keepAliveInterval = UIUtils.createLabelSpinner(conGroup, CoreMessages.dialog_connection_wizard_final_label_keepalive,
                CoreMessages.dialog_connection_wizard_final_label_keepalive_tooltip, 0, 0, Short.MAX_VALUE);

            Composite idleConComp = UIUtils.createComposite(conGroup, 2);
            idleConComp.setLayoutData(GridDataFactory.create(GridData.FILL_HORIZONTAL).span(2, 1).create());
            closeIdleConnectionsCheck = UIUtils.createCheckbox(idleConComp,
                CoreMessages.dialog_connection_wizard_final_label_close_idle_connections,
                CoreMessages.dialog_connection_wizard_final_label_close_idle_connections_tooltip, true, 1);
            closeIdleConnectionsCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent ->
                closeIdleConnectionsPeriod.setEnabled(closeIdleConnectionsCheck.getSelection())));
            closeIdleConnectionsPeriod = UIUtils.createSpinner(idleConComp,
                CoreMessages.dialog_connection_wizard_final_label_close_idle_connections_tooltip, 0, 0, Short.MAX_VALUE);

            {
                String bootstrapTooltip = CoreMessages.dialog_connection_wizard_final_label_bootstrap_tooltip;
                UIUtils.createControlLabel(conGroup, CoreMessages.dialog_connection_wizard_final_label_bootstrap_query).setToolTipText(bootstrapTooltip);
                final Button queriesConfigButton = UIUtils.createPushButton(conGroup, CoreMessages.dialog_connection_wizard_configure, DBeaverIcons.getImage(DBIcon.TREE_SCRIPT));
                queriesConfigButton.setToolTipText(bootstrapTooltip);
                if (dataSourceDescriptor != null && !CommonUtils.isEmpty(dataSourceDescriptor.getConnectionConfiguration().getBootstrap().getInitQueries())) {
                    queriesConfigButton.setFont(BaseThemeSettings.instance.baseFontBold);
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

        Control infoLabel = UIUtils.createInfoLabel(group, CoreMessages.dialog_connection_wizard_connection_init_hint);
        infoLabel.setToolTipText(CoreMessages.dialog_connection_wizard_connection_init_hint_tip);

        Link urlHelpLabel = UIUtils.createLink(
            group,
            CoreMessages.dialog_connection_wizard_connection_init_docs_hint,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShellUtils.launchProgram(HelpUtils.getHelpExternalReference(PAGE_DOCS_LINK));
                }
            }
        );
        urlHelpLabel.setLayoutData(new GridData(GridData.FILL, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 2, 1));

        setControl(group);

        UIUtils.setHelp(group, IHelpContextIds.CTX_CON_WIZARD_FINAL);
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        if (activated) {
            dataSource.getConnectionConfiguration().getBootstrap().setDefaultAutoCommit(
                getAutocommitValueFromIndex(autocommit.getSelectionIndex()));
            if (txnOptionsLoaded) {
                DBPTransactionIsolation level = null;
                if (!CommonUtils.isEmpty(isolationLevel.getText())) {
                    int levelIndex = isolationLevel.getSelectionIndex();
                    if (levelIndex >= 0) {
                        level = supportedLevels.get(levelIndex);
                    }
                }
                dataSource.getConnectionConfiguration().getBootstrap().setDefaultTransactionIsolation(
                    level == null ? null : level.getCode());

            }
            final DBPConnectionConfiguration confConfig = dataSource.getConnectionConfiguration();
            DBPConnectionBootstrap bootstrap = confConfig.getBootstrap();
            bootstrap.setDefaultCatalogName(defaultCatalog.getText());
            bootstrap.setDefaultSchemaName(defaultSchema.getText());

            bootstrap.setIgnoreErrors(ignoreBootstrapErrors);
            bootstrap.setInitQueries(bootstrapQueries);

            confConfig.setKeepAliveInterval(keepAliveInterval.getSelection());
            confConfig.setCloseIdleConnection(closeIdleConnectionsCheck.getSelection());
            if (confConfig.isCloseIdleConnection() && closeIdleConnectionsPeriod.getSelection() != confConfig.getConnectionType()
                .getCloseIdleConnectionPeriod()) {
                // Save only if it is enabled and not equals to default
                confConfig.setCloseIdleInterval(closeIdleConnectionsPeriod.getSelection());
            } else {
                confConfig.setCloseIdleInterval(0);
            }
        }

        shellCommandPage.saveSettings(dataSource);
        if (clientAppPage != null) {
            clientAppPage.performFinish();
        }
    }

    @Override
    public void setWizard(IWizard newWizard) {
        super.setWizard(newWizard);
        if (newWizard instanceof ConnectionWizard connectionWizard && !connectionWizard.isNew()) {
            // Listen for connection type change
            connectionWizard.addPropertyChangeListener(event -> {
                if (ConnectionWizard.PROP_CONNECTION_TYPE.equals(event.getProperty())) {
                    DBPConnectionType type = (DBPConnectionType) event.getNewValue();
                    if (closeIdleConnectionsCheck != null) {
                        closeIdleConnectionsCheck.setSelection(type.isAutoCloseConnections());
                    }
                    if (closeIdleConnectionsPeriod != null) {
                        closeIdleConnectionsPeriod.setSelection(type.getCloseIdleConnectionPeriod());
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
