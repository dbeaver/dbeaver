/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.UIUtils;


public abstract class ConfigImportWizardPage extends WizardPage {

    private Table connectionTable;

    protected ConfigImportWizardPage(String pageName)
    {
        super(pageName);
    }

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

        UIUtils.packColumns(connectionTable);

        setControl(placeholder);
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible) {
            connectionTable.removeAll();
            ImportData importData = new ImportData();
            boolean loaded = false;
            try {
                loadConnections(importData);
                loaded = true;
            } catch (DBException e) {
                setMessage(e.getMessage(), IMessageProvider.ERROR);
            }
            UIUtils.packColumns(connectionTable);
            getContainer().updateButtons();
            if (loaded) {
                if (CommonUtils.isEmpty(importData.getConnections())) {
                    setMessage("Connection list is empty", IMessageProvider.WARNING);
                }
            }
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
