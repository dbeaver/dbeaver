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

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
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

    public static final String PAGE_NAME = ConnectionPageGeneral.class.getSimpleName();

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

    ConnectionPageGeneral(ConnectionWizard wizard)
    {
        super(PAGE_NAME);
        this.wizard = wizard;
        setTitle(CoreMessages.dialog_connection_edit_wizard_general);
        setDescription(CoreMessages.dialog_connection_wizard_final_description);
    }

    ConnectionPageGeneral(ConnectionWizard wizard, DataSourceDescriptor dataSourceDescriptor)
    {
        this(wizard);
        this.dataSourceDescriptor = dataSourceDescriptor;
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void activatePage()
    {
        if (connectionNameText != null) {
            ConnectionPageSettings settings = wizard.getPageSettings();
            String newName = generateConnectionName(settings);
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
                    connectionFolderCombo.select(connectionFolders.indexOf(dataSourceFolder));
                }
                if (dataSourceDescriptor.getDescription() != null) {
                    descriptionText.setText(dataSourceDescriptor.getDescription());
                }
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
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        group.setLayout(gl);

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
            //connectionFolderCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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
            ((GridData) descLabel.getLayoutData()).horizontalSpan = 2;
            descriptionText = new Text(group, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
            final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            gd.heightHint = descriptionText.getLineHeight() * 3;
            descriptionText.setLayoutData(gd);
        }

        {
            Group linkGroup = UIUtils.createControlGroup(group, CoreMessages.dialog_connection_wizard_final_group_other, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            ((GridData)linkGroup.getLayoutData()).horizontalSpan = 2;

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

        String name = connectionNameText.getText();
        if (name.isEmpty()) {
            name = generateConnectionName(getWizard().getPageSettings());
        }
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