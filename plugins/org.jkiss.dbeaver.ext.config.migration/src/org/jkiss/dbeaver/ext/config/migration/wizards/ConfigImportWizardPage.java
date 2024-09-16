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
package org.jkiss.dbeaver.ext.config.migration.wizards;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConnectionFolderSelector;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.dialogs.ObjectListDialog;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;


public abstract class ConfigImportWizardPage extends ActiveWizardPage<ConfigImportWizard> {

    private Table connectionTable;
    private ImportData importData;

    private ConnectionFolderSelector folderSelector;

    protected ConfigImportWizardPage(String pageName) {
        super(pageName);
    }
 
    @Override
    public void createControl(Composite parent) {
        Composite placeholder = new Composite(parent, SWT.NONE);
        placeholder.setLayout(new GridLayout(1, true));

        UIUtils.createControlLabel(placeholder, ImportConfigMessages.config_import_wizard_page_caption_connections);

        connectionTable = new Table(placeholder, SWT.BORDER | SWT.CHECK | SWT.MULTI);
        getConnectionTable().setHeaderVisible(true);
        getConnectionTable().setLinesVisible(true);
        getConnectionTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        UIUtils.createTableColumn(getConnectionTable(), SWT.LEFT, ImportConfigMessages.config_import_wizard_page_th_name);
        UIUtils.createTableColumn(getConnectionTable(), SWT.LEFT, ImportConfigMessages.config_import_wizard_page_th_driver);
        UIUtils.createTableColumn(getConnectionTable(), SWT.LEFT, ImportConfigMessages.config_import_wizard_page_th_url);

        {
            Composite buttonsPanel = UIUtils.createComposite(placeholder, 5);
            UIUtils.createDialogButton(buttonsPanel, ImportConfigMessages.config_import_wizard_btn_select_all, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    for (TableItem item : getConnectionTable().getItems()) {
                        ((ImportConnectionInfo) item.getData()).setChecked(true);
                        item.setChecked(true);
                    }
                    getContainer().updateButtons();
                }
            });
            UIUtils.createDialogButton(buttonsPanel,  ImportConfigMessages.config_import_wizard_btn_deselect_all, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    for (TableItem item : getConnectionTable().getItems()) {
                        item.setChecked(false);
                        ((ImportConnectionInfo) item.getData()).setChecked(false);
                    }
                    getContainer().updateButtons();
                }
            });
            UIUtils.createDialogButton(buttonsPanel,  ImportConfigMessages.config_import_wizard_btn_set_driver, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    TableItem[] selection = getConnectionTable().getSelection();
                    if (selection != null && selection.length > 0) {
                        for (TableItem item : selection) {
                            if (item.getData() instanceof ImportConnectionInfo connectionInfo) {
                                setConnectionInfoForItem(setDriverForConnection(connectionInfo), item);
                            }
                        }
                        isPageComplete();
                    }
                }
            });

            folderSelector = new ConnectionFolderSelector(buttonsPanel);
            folderSelector.loadConnectionFolders(NavigatorUtils.getSelectedProject());
        }

        UIUtils.packColumns(getConnectionTable());

        getConnectionTable().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem item = (TableItem) e.item;
                if (item == null) {
                    return;
                }
                if (item.getData() instanceof ImportConnectionInfo connectionInfo) {
                    connectionInfo.setChecked(item.getChecked());
                }
                getContainer().updateButtons();
            }
        });

        setControl(placeholder);
    }

    protected ImportConnectionInfo setDriverForConnection(ImportConnectionInfo connectionInfo) {
        final DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        List<DriverDescriptor> matchedDrivers = new ArrayList<>();
        for (DataSourceProviderDescriptor dataSourceProvider : registry.getDataSourceProviders()) {
            for (DriverDescriptor driver : dataSourceProvider.getEnabledDrivers()) {
                matchedDrivers.add(driver);
            }
        }
        matchedDrivers = matchedDrivers.stream().sorted(Comparator.comparing(DriverDescriptor::getName)).collect(Collectors.toList());
        DriverDescriptor driver = ObjectListDialog.selectObject(
            getShell(), NLS.bind(ImportConfigMessages.config_import_wizard_choose_driver_for_connections, connectionInfo.getAlias()), "ImportDriverSelector", matchedDrivers);
        if (driver != null) {
            connectionInfo.setDriver(driver);
            connectionInfo.setDriverInfo(new ImportDriverInfo(
                connectionInfo.getAlias(),
                driver.getName(),
                driver.getSampleURL(),
                driver.getDriverClassName()));
        }
        return connectionInfo;
    }

    @Override
    public void activatePage() {
        getConnectionTable().removeAll();
        importData = new ImportData();
        boolean loaded = false;
        try {
            loadConnections(importData);
            loaded = true;
        } catch (DBException e) {
            setMessage(e.getMessage(), IMessageProvider.ERROR);
        }
        getContainer().updateButtons();
        if (loaded) {
            if (CommonUtils.isEmpty(importData.getConnections())) {
                setMessage(ImportConfigMessages.config_import_wizard_page_label_connection_list, IMessageProvider.WARNING);
            } else {
                setMessage(null);
                for (ImportConnectionInfo connectionInfo : importData.getConnections()) {
                    TableItem item = new TableItem(getConnectionTable(), SWT.NONE);
                    setConnectionInfoForItem(connectionInfo, item);
                }
            }
        }
        UIUtils.packColumns(getConnectionTable());
    }

    private void setConnectionInfoForItem(ImportConnectionInfo connectionInfo,
        TableItem item) {
        if (connectionInfo.getDriverInfo() != null) {
            item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
            item.setText(0, connectionInfo.getAlias());
            item.setText(1, connectionInfo.getDriverInfo().getName());
            String url = connectionInfo.getUrl();
            if (CommonUtils.isEmpty(url)) {
                url = connectionInfo.getHost();
            }
            if (CommonUtils.isEmpty(url)) {
                url = "jdbc:???";
            }
            item.setText(2, url);
            item.setData(connectionInfo);
        } else {
            item.setImage(0, DBeaverIcons.getImage(DBIcon.DATABASE_DEFAULT));
            item.setText(0, connectionInfo.getAlias());
            item.setText(1, ImportConfigMessages.config_import_wizard_page_driver_unknown);
            item.setText(2, ImportConfigMessages.config_import_wizard_page_driver_unknown); 
            item.setData(connectionInfo);
        }
    }

    @Override
    public void deactivatePage() {
        getImportData().setDataSourceFolder(folderSelector.getFolder());
        super.deactivatePage();
    }

    protected abstract void loadConnections(ImportData importData) throws DBException;

    @Override
    public boolean isPageComplete() {
        if (getConnectionTable() == null) {
            return false;
        }
        for (TableItem item : getConnectionTable().getItems()) {
            if (item.getChecked()) {
                return true;
            }
        }
        return false;
    }

    public Table getConnectionTable() {
        return connectionTable;
    }

    public ImportData getImportData() {
        return importData;
    }

}
