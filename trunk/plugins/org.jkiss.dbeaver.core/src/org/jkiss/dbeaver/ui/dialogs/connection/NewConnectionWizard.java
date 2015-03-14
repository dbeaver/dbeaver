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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.ui.IActionConstants;
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
    private ConnectionPageGeneral pageGeneral;
    private ConnectionPageNetwork pageNetwork;


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
        setWindowTitle(CoreMessages.dialog_new_connection_wizard_title);
    }

    List<DataSourceProviderDescriptor> getAvailableProvides()
    {
        return availableProvides;
    }

    ConnectionPageDriver getPageDrivers()
    {
        return pageDrivers;
    }

    ConnectionPageSettings getPageSettings(DriverDescriptor driver)
    {
        return this.settingsPages.get(driver.getProviderDescriptor());
    }

    @Override
    public DriverDescriptor getSelectedDriver()
    {
        return getPageDrivers().getSelectedDriver();
    }

    @Override
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
    @Override
    public void addPages()
    {
        pageDrivers = new ConnectionPageDriver(this);
        addPage(pageDrivers);

        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    List<DataSourceProviderDescriptor> providers = DataSourceProviderRegistry.getInstance().getDataSourceProviders();
                    monitor.beginTask(CoreMessages.dialog_new_connection_wizard_monitor_load_data_sources, providers.size());
                    for (DataSourceProviderDescriptor provider : providers) {
                        monitor.subTask(provider.getName());
                        DataSourceViewDescriptor view = provider.getView(IActionConstants.NEW_CONNECTION_POINT);
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

        pageGeneral = new ConnectionPageGeneral(this);
        pageNetwork = new ConnectionPageNetwork(this);
        addPage(pageGeneral);
        addPage(pageNetwork);
    }

    @Nullable
    @Override
    public IWizardPage getNextPage(IWizardPage page)
    {
        if (page == pageDrivers) {
            return getPageSettings(pageDrivers.getSelectedDriver());
        } else if (page instanceof ConnectionPageSettings) {
            return pageNetwork;
        } else if (page instanceof ConnectionPageNetwork) {
            return pageGeneral;
        } else {
            return null;
        }
    }

    /**
     * This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it
     * using wizard as execution context.
     */
    @Override
    public boolean performFinish()
    {
        DriverDescriptor driver = getSelectedDriver();
        DataSourceDescriptor dataSourceTpl = getPageSettings().getActiveDataSource();
        DataSourceDescriptor dataSourceNew = new DataSourceDescriptor(
            dataSourceRegistry, dataSourceTpl.getId(), driver, dataSourceTpl.getConnectionInfo());
        dataSourceNew.copyFrom(dataSourceTpl);
        saveSettings(dataSourceNew);
        dataSourceRegistry.addDataSource(dataSourceNew);
        return true;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
    }

    @Override
    protected void saveSettings(DataSourceDescriptor dataSource) {
        getPageSettings(dataSource.getDriver()).saveSettings(dataSource);
        pageGeneral.saveSettings(dataSource);
        pageNetwork.saveConfigurations(dataSource);
    }

    @Override
    public boolean isNew() {
        return true;
    }

}
