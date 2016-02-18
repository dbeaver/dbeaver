/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.preferences.PrefPageConnectionTypes;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * General connection page (common for all connection types)
 */
class ConnectionPageGeneral extends ActiveWizardPage<ConnectionWizard> {
    static final Log log = Log.getLog(ConnectionPageGeneral.class);

    private ConnectionWizard wizard;
    private DataSourceDescriptor dataSourceDescriptor;
    private Text connectionNameText;
    private CImageCombo connectionTypeCombo;
    private Button savePasswordCheck;
    private Button autocommit;
    private Combo isolationLevel;
    private Combo defaultSchema;
    private Button showSystemObjects;
    private Button readOnlyConnection;
    private Button eventsButton;
    private Font boldFont;

    private boolean connectionNameChanged = false;
    private java.util.List<FilterInfo> filters = new ArrayList<>();
    private Group filtersGroup;
    private boolean activated = false;
    private java.util.List<DBPTransactionIsolation> supportedLevels = new ArrayList<>();
    private java.util.List<String> bootstrapQueries;
    private boolean ignoreBootstrapErrors;

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
        setTitle(wizard.isNew() ? CoreMessages.dialog_connection_wizard_final_header : "General");
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
                connectionNameText.setText(newName);
                connectionNameChanged = false;
            }
        }
        if (dataSourceDescriptor != null) {
            if (!activated) {
                // Get settings from data source descriptor
                connectionTypeCombo.select(dataSourceDescriptor.getConnectionConfiguration().getConnectionType());
                savePasswordCheck.setSelection(dataSourceDescriptor.isSavePassword());
                autocommit.setSelection(dataSourceDescriptor.isDefaultAutoCommit());
                showSystemObjects.setSelection(dataSourceDescriptor.isShowSystemObjects());
                readOnlyConnection.setSelection(dataSourceDescriptor.isConnectionReadOnly());
                isolationLevel.add("");
                if (dataSourceDescriptor.isConnected()) {
                    DBPDataSource dataSource = dataSourceDescriptor.getDataSource();
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
                    dataSourceDescriptor.getConnectionConfiguration().getBootstrap().getDefaultObjectName()));
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
            autocommit.setSelection(((DBPConnectionType)connectionTypeCombo.getData(0)).isAutocommit());
            showSystemObjects.setSelection(true);
            readOnlyConnection.setSelection(false);
            isolationLevel.setEnabled(false);
            defaultSchema.setText("");
        }
        if (savePasswordCheck != null) {
            //savePasswordCheck.setEnabled();
        }
        long features = 0;
        try {
            features = wizard.getSelectedDriver().getDataSourceProvider().getFeatures();
        } catch (DBException e) {
            log.error("Can't obtain data source provider instance", e); //$NON-NLS-1$
        }

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
            if (filterInfo.filter != null && !filterInfo.filter.isEmpty()) {
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
            UIUtils.createControlLabel(group, "Connection type");

            Composite ctGroup = UIUtils.createPlaceholder(group, 2, 5);
            connectionTypeCombo = new CImageCombo(ctGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            loadConnectionTypes();
            connectionTypeCombo.select(0);
            connectionTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    DBPConnectionType type = (DBPConnectionType)connectionTypeCombo.getItem(connectionTypeCombo.getSelectionIndex()).getData();
                    autocommit.setSelection(type.isAutocommit());
                }
            });

            Button pickerButton = new Button(ctGroup, SWT.PUSH);
            pickerButton.setText("Edit");
            pickerButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
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
/*
            UIUtils.createControlLabel(colorGroup, "Custom color");
            new CImageCombo(colorGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            Button pickerButton = new Button(colorGroup, SWT.PUSH);
            pickerButton.setText("...");
*/
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
                Group txnGroup = UIUtils.createControlGroup(rightSide, "Connection", 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
                autocommit = UIUtils.createCheckbox(
                    txnGroup,
                    CoreMessages.dialog_connection_wizard_final_checkbox_auto_commit,
                    dataSourceDescriptor != null && dataSourceDescriptor.isDefaultAutoCommit());
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.horizontalSpan = 2;
                autocommit.setLayoutData(gd);
                autocommit.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        if (dataSourceDescriptor != null && dataSourceDescriptor.isConnected()) {
                            isolationLevel.setEnabled(!autocommit.getSelection());
                        }
                    }
                });

                isolationLevel = UIUtils.createLabelCombo(txnGroup, "Isolation level", SWT.DROP_DOWN | SWT.READ_ONLY);
                isolationLevel.setToolTipText(
                    "Default transaction isolation level.");
                defaultSchema = UIUtils.createLabelCombo(txnGroup, "Default schema", SWT.DROP_DOWN);
                defaultSchema.setToolTipText(
                    "Name of schema or catalog which will be set as default.");

                UIUtils.createControlLabel(txnGroup, "Bootstrap queries");
                Button queriesConfigButton = UIUtils.createPushButton(txnGroup, "Configure ...", DBeaverIcons.getImage(UIIcon.SQL_SCRIPT));
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
                    rightSide,
                    CoreMessages.dialog_connection_wizard_final_group_filters,
                    1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);

                for (int i = 0; i < filters.size(); i++) {
                    final FilterInfo filterInfo = filters.get(i);
                    filterInfo.link = UIUtils.createLink(filtersGroup, "<a>" + filterInfo.title + "</a>", new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            EditObjectFilterDialog dialog = new EditObjectFilterDialog(
                                getShell(),
                                filterInfo.title,
                                filterInfo.filter != null ? filterInfo.filter : new DBSObjectFilter(),
                                true);
                            if (dialog.open() == IDialogConstants.OK_ID) {
                                filterInfo.filter = dialog.getFilter();
                                if (filterInfo.filter != null && !filterInfo.filter.isEmpty()) {
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
            //Composite buttonsGroup = UIUtils.createPlaceholder(group, 3);
            Composite buttonsGroup = new Composite(group, SWT.NONE);
            gl = new GridLayout(1, false);
            gl.verticalSpacing = 0;
            gl.horizontalSpacing = 10;
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            buttonsGroup.setLayout(gl);

            //buttonsGroup.setLayout(new GridLayout(2, true));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            buttonsGroup.setLayoutData(gd);

            if (getWizard().isNew()) {
                eventsButton = new Button(buttonsGroup, SWT.PUSH);
                eventsButton.setText("Shell Commands");
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

        setControl(group);

        UIUtils.setHelp(group, IHelpContextIds.CTX_CON_WIZARD_FINAL);
    }

    private void loadConnectionTypes()
    {
        connectionTypeCombo.removeAll();
        for (DBPConnectionType ct : DataSourceProviderRegistry.getInstance().getConnectionTypes()) {
            connectionTypeCombo.add(null, ct.getName(), UIUtils.getConnectionTypeColor(ct), ct);
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
        dataSource.setConnectionReadOnly(readOnlyConnection.getSelection());
        if (!dataSource.isSavePassword()) {
            dataSource.resetPassword();
        }
        if (connectionTypeCombo.getSelectionIndex() >= 0) {
            dataSource.getConnectionConfiguration().setConnectionType(
                (DBPConnectionType) connectionTypeCombo.getData(connectionTypeCombo.getSelectionIndex()));
        }
        for (FilterInfo filterInfo : filters) {
            if (filterInfo.filter != null) {
                dataSource.setObjectFilter(filterInfo.type, null, filterInfo.filter);
            }
        }
        DBPConnectionBootstrap bootstrap = dataSource.getConnectionConfiguration().getBootstrap();
        bootstrap.setIgnoreErrors(ignoreBootstrapErrors);
        bootstrap.setInitQueries(bootstrapQueries);
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

    private class SchemaReadJob extends AbstractJob {
        private DBSObjectContainer objectContainer;
        public SchemaReadJob(DBSObjectContainer objectContainer) {
            super("Schema reader");
            this.objectContainer = objectContainer;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                final java.util.List<String> schemaNames = new ArrayList<>();
                Collection<? extends DBSObject> children = objectContainer.getChildren(monitor);
                if (children != null) {
                    for (DBSObject child : children) {
                        schemaNames.add(child.getName());
                    }
                }
                if (!schemaNames.isEmpty()) {
                    UIUtils.runInUI(null, new Runnable() {
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