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
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DriverTreeControl;

import java.util.*;

/**
 * Connection migration wizard
 */
public class MigrateConnectionWizard extends Wizard
{
    private final DataSourceRegistry registry;
    private final Set<DBSDataSourceContainer> selectedConnections = new HashSet<DBSDataSourceContainer>();
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
                } else if (item instanceof DBSDataSourceContainer) {
                    selectedConnections.add((DBSDataSourceContainer) item);
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
            final List<DataSourceDescriptor> result = new ArrayList<DataSourceDescriptor>();
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
            driverTreeControl = new DriverTreeControl(placeholder, this, DataSourceProviderRegistry.getInstance().getDataSourceProviders(), true);
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
