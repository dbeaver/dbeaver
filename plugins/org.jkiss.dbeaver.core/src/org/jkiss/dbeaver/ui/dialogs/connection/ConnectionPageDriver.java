/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverTreeControl;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverTreeViewer;

/**
 * Driver selection page
 */
class ConnectionPageDriver extends ActiveWizardPage implements ISelectionChangedListener, IDoubleClickListener {
    private NewConnectionWizard wizard;
    private DriverDescriptor selectedDriver;

    ConnectionPageDriver(NewConnectionWizard wizard)
    {
        super("newConnectionDrivers");
        this.wizard = wizard;
        setTitle(CoreMessages.dialog_new_connection_wizard_start_title);
        setDescription(CoreMessages.dialog_new_connection_wizard_start_description);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        DriverTreeControl driverTreeControl = new DriverTreeControl(placeholder, this, wizard.getAvailableProvides(), true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 200;
        driverTreeControl.setLayoutData(gd);
        setControl(placeholder);

        UIUtils.setHelp(placeholder, IHelpContextIds.CTX_CON_WIZARD_DRIVER);
    }

    public DriverDescriptor getSelectedDriver()
    {
        return selectedDriver;
    }

    @Override
    public boolean canFlipToNextPage()
    {
        return this.selectedDriver != null;
    }

    @Override
    public boolean isPageComplete()
    {
        return canFlipToNextPage();
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event)
    {
        this.selectedDriver = null;
        this.setMessage("");
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
            if (selectedObject instanceof DBPDriver) {
                selectedDriver = (DriverDescriptor) selectedObject;
                this.setMessage(selectedDriver.getDescription());
            } else if (selectedObject instanceof DataSourceProviderDescriptor) {
                this.setMessage(((DataSourceProviderDescriptor) selectedObject).getDescription());
            } else if (selectedObject instanceof DriverTreeViewer.DriverCategory) {
                this.setMessage(((DriverTreeViewer.DriverCategory) selectedObject).getName() + " drivers");
            }
        }
        getWizard().getContainer().updateButtons();
        getWizard().getContainer().updateMessage();
    }

    @Override
    public void doubleClick(DoubleClickEvent event)
    {
        if (selectedDriver != null) {
            wizard.getContainer().showPage(wizard.getNextPage(this));
        }
    }

    @Override
    public void activatePage()
    {
    }

    @Override
    public void deactivatePage()
    {

    }
}