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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.ui.IActionConstants;

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

        List<DataSourceProviderDescriptor> providers = DataSourceProviderRegistry.getInstance().getDataSourceProviders();
        for (DataSourceProviderDescriptor provider : providers) {
            DataSourceViewDescriptor view = provider.getView(IActionConstants.NEW_CONNECTION_POINT);
            availableProvides.add(provider);
            if (view != null) {
                ConnectionPageSettings pageSettings = new ConnectionPageSettings(
                    NewConnectionWizard.this,
                    view);
                settingsPages.put(provider, pageSettings);
                addPage(pageSettings);
            }
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
            ConnectionPageSettings pageSettings = getPageSettings(pageDrivers.getSelectedDriver());
            if (pageSettings == null) {
                return pageDrivers.getSelectedDriver().isEmbedded() ? pageGeneral : pageNetwork;
            } else {
                return pageSettings;
            }
        } else if (page instanceof ConnectionPageSettings) {
            return pageDrivers.getSelectedDriver().isEmbedded() ? pageGeneral : pageNetwork;
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
        ConnectionPageSettings pageSettings = getPageSettings();
        DataSourceDescriptor dataSourceTpl = pageSettings == null ? getActiveDataSource() : pageSettings.getActiveDataSource();
        DataSourceDescriptor dataSourceNew = new DataSourceDescriptor(
            dataSourceRegistry, dataSourceTpl.getId(), driver, dataSourceTpl.getConnectionConfiguration());
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
        ConnectionPageSettings pageSettings = getPageSettings(dataSource.getDriver());
        if (pageSettings != null) {
            pageSettings.saveSettings(dataSource);
        }
        pageGeneral.saveSettings(dataSource);
        pageNetwork.saveConfigurations(dataSource);
    }

    @Override
    public boolean isNew() {
        return true;
    }

}
