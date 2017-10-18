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

package org.jkiss.dbeaver.ext.import_config.wizards.custom;

import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverTreeControl;


public class ConfigImportWizardPageCustomDriver extends WizardPage implements ISelectionChangedListener,IDoubleClickListener {

    private DriverDescriptor selectedDriver;

    protected ConfigImportWizardPageCustomDriver()
    {
        super("Driver");
        setTitle("Driver selection");
        setDescription("Select the driver to use for imported connections");
    }

    @Override
    public void createControl(Composite parent) {
        Composite placeholder = new Composite(parent, SWT.NONE);
        placeholder.setLayout(new GridLayout(1, true));

        DriverTreeControl driverTreeControl = new DriverTreeControl(
            placeholder,
            this,
            DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders(),
            true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 200;
        driverTreeControl.setLayoutData(gd);

        setControl(placeholder);
    }

    @Override
    public boolean isPageComplete() {
        return selectedDriver != null;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        this.selectedDriver = null;
        this.setMessage("");
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
            if (selectedObject instanceof DBPDriver) {
                selectedDriver = (DriverDescriptor) selectedObject;
                this.setMessage(selectedDriver.getDescription());
            }
        }
        getWizard().getContainer().updateButtons();
        getWizard().getContainer().updateMessage();
    }

    @Override
    public void doubleClick(DoubleClickEvent event) {
        if (selectedDriver != null) {
            getWizard().getContainer().showPage(getWizard().getNextPage(this));
        }
    }

    public DBPDriver getSelectedDriver() {
        return selectedDriver;
    }
}
