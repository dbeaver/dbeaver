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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverTreeControl;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverTreeViewer;

import java.util.List;

/**
 * Driver selection page
 * step1
 */
class ConnectionPageDriver extends ActiveWizardPage implements ISelectionChangedListener, IDoubleClickListener {
    private NewConnectionWizard wizard;
    private DriverDescriptor selectedDriver;
    private IProject connectionProject;

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

        Group projectGroup = UIUtils.createControlGroup(placeholder, CoreMessages.dialog_connection_driver_project, 1, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        final Combo projectCombo = new Combo(projectGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        projectCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final List<IProject> projects = DBeaverCore.getInstance().getLiveProjects();
        if (!projects.isEmpty()) {

            final IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
            for (IProject project : projects) {
                projectCombo.add(project.getName());
            }

            if (activeProject == null) {
                projectCombo.select(0);
                connectionProject = projects.get(0);
            } else {
                connectionProject = activeProject;
                projectCombo.setText(activeProject.getName());
            }
            projectCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    connectionProject = projects.get(projectCombo.getSelectionIndex());
                }
            });

            if (projects.size() < 2) {
                projectCombo.setEnabled(false);
            }
        } else {
            setErrorMessage("You need to create a project first");
        }

        UIUtils.setHelp(placeholder, IHelpContextIds.CTX_CON_WIZARD_DRIVER);
    }

    public DriverDescriptor getSelectedDriver()
    {
        return selectedDriver;
    }

    public IProject getConnectionProject() {
        return connectionProject;
    }

    @Override
    public boolean canFlipToNextPage()
    {
        return this.connectionProject != null && this.selectedDriver != null;
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