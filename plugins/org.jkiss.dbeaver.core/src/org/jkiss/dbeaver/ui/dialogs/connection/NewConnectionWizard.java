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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
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
    private IStructuredSelection selection;
    private List<DataSourceProviderDescriptor> availableProvides = new ArrayList<>();
    private ConnectionPageDriver pageDrivers;
    private Map<DataSourceProviderDescriptor, ConnectionPageSettings> settingsPages = new HashMap<>();
    private ConnectionPageGeneral pageGeneral;
    private ConnectionPageNetwork pageNetwork;


    public NewConnectionWizard()
    {
        setWindowTitle(CoreMessages.dialog_new_connection_wizard_title);
    }

    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
        return DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(
            pageDrivers.getConnectionProject());
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

        // Initial settings
        if (selection != null && !selection.isEmpty()) {
            final Object element = selection.getFirstElement();
            if (element instanceof DBNLocalFolder) {
                pageGeneral.setDataSourceFolder(((DBNLocalFolder) element).getFolder());
            }
        }
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
        DBPDataSourceRegistry dataSourceRegistry = getDataSourceRegistry();

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
        this.selection = selection;
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
