/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
import org.jkiss.dbeaver.ui.DBIcon;
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

        UIUtils.createControlLabel(placeholder, "Connections");

        connectionTable = new Table(placeholder, SWT.BORDER | SWT.CHECK | SWT.MULTI);
        connectionTable.setHeaderVisible(true);
        connectionTable.setLinesVisible(true);
        connectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        UIUtils.createTableColumn(connectionTable, SWT.LEFT, "Name");
        UIUtils.createTableColumn(connectionTable, SWT.LEFT, "Driver");
        UIUtils.createTableColumn(connectionTable, SWT.LEFT, "URL");

        UIUtils.packColumns(connectionTable);

        connectionTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem item = (TableItem) e.item;
                ImportConnectionInfo connectionInfo = (ImportConnectionInfo) item.getData();
                connectionInfo.setChecked(item.getChecked());
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
                    setMessage("Connection list is empty", IMessageProvider.WARNING);
                } else {
                    for (ImportConnectionInfo connectionInfo : importData.getConnections()) {
                        TableItem item = new TableItem(connectionTable, SWT.NONE);
                        item.setImage(0, DBIcon.TREE_DATABASE.getImage());
                        item.setText(0, connectionInfo.getAlias());
                        item.setText(1, connectionInfo.getDriverInfo().getName());
                        item.setText(2, connectionInfo.getUrl());
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
