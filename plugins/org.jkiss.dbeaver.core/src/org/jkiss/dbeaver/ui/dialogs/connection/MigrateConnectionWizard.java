/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
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
        setWindowTitle("Migrate connection(s) to another driver");
    }

    /**
     * Adding the page to the wizard.
     */
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
            super("Connections");
            setTitle("Select connections to migrate");
            setDescription("Select connection(s) you wish to migrate to another driver.");
        }

        public void createControl(Composite parent)
        {
            connectionsViewer = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK);
            connectionsViewer.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e)
                {
                    getContainer().updateButtons();
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                    getContainer().showPage(getNextPage());
                }
            });
            for (DataSourceDescriptor ds : registry.getDataSources()) {
                TableItem item = new TableItem(connectionsViewer, SWT.NONE);
                item.setText(ds.getName());
                item.setData(ds);
                item.setImage(ds.getDriver().getIcon());
                if (selectedConnections.contains(ds)) {
                    item.setChecked(true);
                }
            }
            setControl(connectionsViewer);
        }

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
            super("migrateConnectionDriver");
            setTitle("Choose target driver");
            setDescription("Choose target driver for selected connections.");
        }

        public void createControl(Composite parent)
        {
            Composite placeholder = UIUtils.createPlaceholder(parent, 1);
            driverTreeControl = new DriverTreeControl(placeholder);
            driverTreeControl.initDrivers(this, DBeaverCore.getInstance().getDataSourceProviderRegistry().getDataSourceProviders());
            Control control = driverTreeControl.getControl();
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 200;
            control.setLayoutData(gd);
            setControl(placeholder);
        }

        public boolean isPageComplete()
        {
            return this.selectedDriver != null;
        }

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
