/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.ui.DBeaverExtensions;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.utils.DBeaverUtils;

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
    private DataSourceRegistry registry;
    private List<DataSourceProviderDescriptor> availableProvides = new ArrayList<DataSourceProviderDescriptor>();
    private ConnectionPageDriver pageDrivers;
    private Map<DataSourceProviderDescriptor, ConnectionPageSettings> settingsPages = new HashMap<DataSourceProviderDescriptor, ConnectionPageSettings>();
    private ConnectionPageFinal pageFinal;

    /**
     * Constructor for SampleNewWizard.
     */
    NewConnectionWizard()
    {
        super();
        setWindowTitle("Create new connection");
        setNeedsProgressMonitor(true);
        this.registry = DataSourceRegistry.getDefault();
    }

    DataSourceRegistry getRegistry()
    {
        return this.registry;
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
            registry.getCore().run(true, true, new DBRRunnableWithProgress()
            {
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
/*
                    monitor.beginTask("Load data sources", 10);
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(500);
                        monitor.worked(1);
                        monitor.subTask("DS " + i);
                    }
                    monitor.done();
*/
                    List<DataSourceProviderDescriptor> providers = registry.getDataSourceProviders();
                    monitor.beginTask("Load data sources", providers.size());
                    for (DataSourceProviderDescriptor provider : registry.getDataSourceProviders()) {
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
            DBeaverUtils.showErrorDialog(getShell(), "Error", "Error loading views", ex);
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
            pageDrivers.getSelectedDriver(),
            getPageSettings().getConnectionInfo());
        pageFinal.saveSettings(dataSource);
        registry.addDataSource(dataSource);
        return true;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
    }

}
