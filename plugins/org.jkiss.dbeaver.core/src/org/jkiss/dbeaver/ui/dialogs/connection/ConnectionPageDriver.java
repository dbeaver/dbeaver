/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.controls.DriverTreeControl;

/**
 * Driver selection page
 */
class ConnectionPageDriver extends WizardPage implements ISelectionChangedListener, IDoubleClickListener {
    private NewConnectionWizard wizard;
    private DriverDescriptor selectedDriver;

    ConnectionPageDriver(NewConnectionWizard wizard)
    {
        super("newConnectionDrivers");
        this.wizard = wizard;
        setTitle("Select new connection type");
        setDescription("This wizard creates a new connection.");
    }

    public void createControl(Composite parent)
    {
        DriverTreeControl treeControl = new DriverTreeControl(parent);
        treeControl.initDrivers(this, wizard.getAvailableProvides());
        setControl(treeControl.getControl());
    }

    public DriverDescriptor getSelectedDriver()
    {
        return selectedDriver;
    }

    public boolean canFlipToNextPage()
    {
        return this.selectedDriver != null;
    }

    public boolean isPageComplete()
    {
        return canFlipToNextPage();
    }

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

    public void doubleClick(DoubleClickEvent event)
    {
        if (selectedDriver != null) {
            wizard.getContainer().showPage(wizard.getNextPage(this));
        }
    }

}