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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverTreeControl;

import java.util.*;

/**
 * Connection migration wizard
 */
public class MigrateConnectionWizard extends Wizard
{
    private final DataSourceRegistry registry;
    private final Set<DBPDataSourceContainer> selectedConnections = new HashSet<>();
    private PageConnections pageConnections;
    private PageDriver pageDriver;

    public MigrateConnectionWizard(DataSourceRegistry registry, IStructuredSelection selection)
    {
        this.registry = registry;
        if (selection != null && !selection.isEmpty()) {
            for (Iterator<?> iter = selection.iterator(); iter.hasNext(); ) {
                Object item = iter.next();
                if (item instanceof IDataSourceContainerProvider) {
                    selectedConnections.add(((IDataSourceContainerProvider) item).getDataSourceContainer());
                } else if (item instanceof DBPDataSourceContainer) {
                    selectedConnections.add((DBPDataSourceContainer) item);
                } else if (item instanceof DBSObject) {
                    selectedConnections.add(((DBSObject) item).getDataSource().getContainer());
                } else if (item instanceof DBSWrapper) {
                    selectedConnections.add(((DBSWrapper) item).getObject().getDataSource().getContainer());
                }
            }
        }
        setWindowTitle(CoreMessages.dialog_migrate_wizard_window_title);
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages()
    {
        pageConnections = new PageConnections();
        pageDriver = new PageDriver();
        addPage(pageConnections);
        addPage(pageDriver);
    }

    @Override
    public IWizardPage getStartingPage()
    {
        if (!selectedConnections.isEmpty()) {
            return pageDriver;
        } else {
            return pageConnections;
        }
    }

    @Override
    public boolean performFinish()
    {
        final List<DataSourceDescriptor> connections = pageConnections.getSelectedConnections();
        final DriverDescriptor targetDriver = pageDriver.selectedDriver;

        for (DataSourceDescriptor conn : connections) {
            conn.setDriver(targetDriver);
            conn.getRegistry().updateDataSource(conn);
        }

        return true;
    }

    class PageConnections extends WizardPage {

        private Table connectionsViewer;

        protected PageConnections()
        {
            super(CoreMessages.dialog_migrate_wizard_name);
            setTitle(CoreMessages.dialog_migrate_wizard_start_title);
            setDescription(CoreMessages.dialog_migrate_wizard_start_description);
        }

        @Override
        public void createControl(Composite parent)
        {
            connectionsViewer = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK);
            connectionsViewer.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    getContainer().updateButtons();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e)
                {
                    getContainer().showPage(getNextPage());
                }
            });
            if (registry != null) {
                for (DataSourceDescriptor ds : registry.getDataSources()) {
                    TableItem item = new TableItem(connectionsViewer, SWT.NONE);
                    item.setText(ds.getName());
                    item.setData(ds);
                    item.setImage(DBeaverIcons.getImage(ds.getDriver().getIcon()));
                    if (selectedConnections.contains(ds)) {
                        item.setChecked(true);
                    }
                }
			}
            setControl(connectionsViewer);
        }

        @Override
        public boolean isPageComplete()
        {
            for (TableItem item : connectionsViewer.getItems()) {
                if (item.getChecked()) {
                    return true;
                }
            }
            return false;
        }

        public List<DataSourceDescriptor> getSelectedConnections()
        {
            final List<DataSourceDescriptor> result = new ArrayList<>();
            for (TableItem item : connectionsViewer.getItems()) {
                if (item.getChecked()) {
                    result.add((DataSourceDescriptor) item.getData());
                }
            }
            return result;
        }
    }

    class PageDriver extends WizardPage implements ISelectionChangedListener {
        private DriverDescriptor selectedDriver;
        private DriverTreeControl driverTreeControl;

        PageDriver()
        {
            super("migrateConnectionDriver"); //$NON-NLS-1$
            setTitle(CoreMessages.dialog_migrate_wizard_choose_driver_title);
            setDescription(CoreMessages.dialog_migrate_wizard_choose_driver_description);
        }

        @Override
        public void createControl(Composite parent)
        {
            Composite placeholder = UIUtils.createPlaceholder(parent, 1);
            driverTreeControl = new DriverTreeControl(
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
        public boolean isPageComplete()
        {
            return this.selectedDriver != null;
        }

        @Override
        public void selectionChanged(SelectionChangedEvent event)
        {
            this.selectedDriver = null;
            ISelection selection = event.getSelection();
            if (selection instanceof IStructuredSelection) {
                Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
                if (selectedObject instanceof DBPDriver) {
                    selectedDriver = (DriverDescriptor) selectedObject;
                }
            }
            getWizard().getContainer().updateButtons();
        }

    }
}
