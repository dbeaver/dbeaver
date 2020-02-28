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
package org.jkiss.dbeaver.ext.import_config.wizards;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.ImportConfigMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;


public abstract class ConfigImportWizardPage extends WizardPage {

    private Table connectionTable;
    private ImportData importData;

    protected ConfigImportWizardPage(String pageName)
    {
        super(pageName);
    }

    public ImportData getImportData()
    {
        return importData;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite placeholder = new Composite(parent, SWT.NONE);
        placeholder.setLayout(new GridLayout(1, true));

        UIUtils.createControlLabel(placeholder, ImportConfigMessages.config_import_wizard_page_caption_connections);

        connectionTable = new Table(placeholder, SWT.BORDER | SWT.CHECK | SWT.MULTI);
        connectionTable.setHeaderVisible(true);
        connectionTable.setLinesVisible(true);
        connectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        UIUtils.createTableColumn(connectionTable, SWT.LEFT, ImportConfigMessages.config_import_wizard_page_th_name);
        UIUtils.createTableColumn(connectionTable, SWT.LEFT, ImportConfigMessages.config_import_wizard_page_th_driver);
        UIUtils.createTableColumn(connectionTable, SWT.LEFT, ImportConfigMessages.config_import_wizard_page_th_url);

        {
            Composite buttonsPanel = UIUtils.createComposite(placeholder, 2);
            UIUtils.createDialogButton(buttonsPanel, "Select All", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    for (TableItem item : connectionTable.getItems()) {
                        ((ImportConnectionInfo) item.getData()).setChecked(true);
                        item.setChecked(true);
                    }
                    getContainer().updateButtons();
                }
            });
            UIUtils.createDialogButton(buttonsPanel, "Select None", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    for (TableItem item : connectionTable.getItems()) {
                        item.setChecked(false);
                        ((ImportConnectionInfo) item.getData()).setChecked(false);
                    }
                    getContainer().updateButtons();
                }
            });
        }

        UIUtils.packColumns(connectionTable);

        connectionTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem item = (TableItem) e.item;
                ((ImportConnectionInfo) item.getData()).setChecked(item.getChecked());
                getContainer().updateButtons();
            }
        });

        setControl(placeholder);
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible) {
            connectionTable.removeAll();
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
                    for (ImportConnectionInfo connectionInfo : importData.getConnections()) {
                        TableItem item = new TableItem(connectionTable, SWT.NONE);
                        item.setImage(0, DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
                        item.setText(0, connectionInfo.getAlias());
                        item.setText(1, connectionInfo.getDriverInfo().getName());
                        String url = connectionInfo.getUrl();
                        if (CommonUtils.isEmpty(url)) {
                            url = connectionInfo.getHost();
                        }
                        item.setText(2, url);
                        item.setData(connectionInfo);
                    }
                }
            }
            UIUtils.packColumns(connectionTable);
        }
        super.setVisible(visible);
    }

    protected abstract void loadConnections(ImportData importData) throws DBException;

    @Override
    public boolean isPageComplete()
    {
        for (TableItem item : connectionTable.getItems()) {
            if (item.getChecked()) {
                return true;
            }
        }
        return false;
    }

}
