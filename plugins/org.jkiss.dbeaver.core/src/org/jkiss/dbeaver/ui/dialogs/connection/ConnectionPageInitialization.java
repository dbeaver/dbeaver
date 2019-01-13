/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
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
                isolationLevel.add("");

                DataSourceDescriptor originalDataSource = getWizard().getOriginalDataSource();
                if (originalDataSource != null && originalDataSource.isConnected()) {
                    DBPDataSource dataSource = originalDataSource.getDataSource();
                    loadDatabaseSettings(dataSource);
                }
                defaultSchema.setText(CommonUtils.notEmpty(
                    conConfig.getBootstrap().getDefaultObjectName()));
                keepAliveInterval.setSelection(conConfig.getKeepAliveInterval());
                activated = true;
            }
        } else {
            // Default settings
            isolationLevel.setEnabled(false);
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

    private void loadDatabaseSettings(DBRProgressMonitor monitor, DBPDataSource dataSource) throws InvocationTargetException, InterruptedException {
        Collection<DBPTransactionIsolation> txnLevels = CommonUtils.safeCollection(dataSource.getInfo().getSupportedTransactionsIsolation());
        Integer levelCode = dataSourceDescriptor.getDefaultTransactionsIsolation();

        UIUtils.syncExec(() -> {
            autocommit.setSelection(dataSourceDescriptor.isDefaultAutoCommit());
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
            DBSObjectContainer schemaContainer = DBUtils.getChangeableObjectContainer((DBSObjectContainer) dataSource);

            try {
                final List<String> schemaNames = new ArrayList<>();
                Collection<? extends DBSObject> children = schemaContainer.getChildren(monitor);
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
        }

        txnOptionsLoaded = true;
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
    public void saveSettings(DataSourceDescriptor dataSource) {
        if (dataSourceDescriptor != null && !activated) {
            // No changes anyway
            return;
        }
        try {
            dataSource.setDefaultAutoCommit(autocommit.getSelection(), null, true, null);
            if (txnOptionsLoaded) {
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

        final DBPConnectionConfiguration confConfig = dataSource.getConnectionConfiguration();

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
                    if (autocommit != null) {
                        autocommit.setSelection(type.isAutocommit());
                    }
                }
            });
        }
    }

    @Override
    public void testConnection(DBCSession session) {
        try {
            loadDatabaseSettings(session.getProgressMonitor(), session.getDataSource());
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Database info reading", "Error reading database settings", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
