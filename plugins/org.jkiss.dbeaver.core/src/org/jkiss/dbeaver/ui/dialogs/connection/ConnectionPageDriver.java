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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DriverTreeControl;
import org.jkiss.dbeaver.ui.controls.DriverTreeViewer;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

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