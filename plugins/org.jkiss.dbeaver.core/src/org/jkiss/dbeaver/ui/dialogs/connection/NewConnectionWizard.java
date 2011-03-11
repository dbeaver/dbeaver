/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.DBeaverExtensions;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a sample new wizard.
 */

public class NewConnectionWizard extends ConnectionWizard
{
    private List<DataSourceProviderDescriptor> availableProvides = new ArrayList<DataSourceProviderDescriptor>();
    private ConnectionPageDriver pageDrivers;
    private Map<DataSourceProviderDescriptor, ConnectionPageSettings> settingsPages = new HashMap<DataSourceProviderDescriptor, ConnectionPageSettings>();
    private ConnectionPageFinal pageFinal;

    public NewConnectionWizard()
    {
        super(DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry());
    }

    /**
     * Constructor for SampleNewWizard.
     */
    public NewConnectionWizard(DataSourceRegistry registry)
    {
        super(registry);
        setWindowTitle("Create new connection");
    }

    List<DataSourceProviderDescriptor> getAvailableProvides()
    {
        return availableProvides;
    }

    ConnectionPageDriver getPageDrivers()
    {
        return pageDrivers;
    }

    ConnectionPageFinal getPageFinal()
    {
        return pageFinal;
    }

    ConnectionPageSettings getPageSettings(DriverDescriptor driver)
    {
        return this.settingsPages.get(driver.getProviderDescriptor());
    }

    public DriverDescriptor getSelectedDriver()
    {
        return getPageDrivers().getSelectedDriver();
    }

    public ConnectionPageSettings getPageSettings()
    {
        if (pageDrivers.getSelectedDriver() == null) {
            return null;
        }
        return this.settingsPages.get(pageDrivers.getSelectedDriver().getProviderDescriptor());
    }

    /**
     * Adding the page to the wizard.
     */
    public void addPages()
    {
        pageDrivers = new ConnectionPageDriver(this);
        addPage(pageDrivers);

        try {
            DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    List<DataSourceProviderDescriptor> providers = DBeaverCore.getInstance().getDataSourceProviderRegistry().getDataSourceProviders();
                    monitor.beginTask("Load data sources", providers.size());
                    for (DataSourceProviderDescriptor provider : providers) {
                        monitor.subTask(provider.getName());
                        DataSourceViewDescriptor view = provider.getView(DBeaverExtensions.NEW_CONNECTION_POINT);
                        if (view == null) {
                            continue;
                        }
                        availableProvides.add(provider);
                        ConnectionPageSettings pageSettings = new ConnectionPageSettings(
                            NewConnectionWizard.this,
                            view);
                        settingsPages.put(provider, pageSettings);
                        addPage(pageSettings);
                        monitor.worked(1);
                    }
                    monitor.done();
                }
            });
        }
        catch (Exception ex) {
            UIUtils.showErrorDialog(getShell(), "Error", "Error loading views", ex);
        }

        pageFinal = new ConnectionPageFinal(this);
        addPage(pageFinal);
    }

    public IWizardPage getNextPage(IWizardPage page)
    {
        if (page == pageDrivers) {
            return getPageSettings(pageDrivers.getSelectedDriver());
        } else if (page instanceof ConnectionPageSettings) {
            return pageFinal;
        } else {
            return null;
        }
    }

    /**
     * This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it
     * using wizard as execution context.
     */
    public boolean performFinish()
    {
        super.performFinish();
        DataSourceDescriptor dataSource = new DataSourceDescriptor(
            dataSourceRegistry,
            DataSourceDescriptor.generateNewId(pageDrivers.getSelectedDriver()),
            pageDrivers.getSelectedDriver(),
            getPageSettings().getConnectionInfo());
        pageFinal.saveSettings(dataSource);
        dataSourceRegistry.addDataSource(dataSource);
        return true;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
    }

}
