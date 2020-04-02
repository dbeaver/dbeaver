/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
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
    private DBPDriver initialDriver;
    private IStructuredSelection selection;
    private List<DataSourceProviderDescriptor> availableProvides = new ArrayList<>();
    private ConnectionPageDriver pageDrivers;
    private Map<DataSourceProviderDescriptor, ConnectionPageSettings> settingsPages = new HashMap<>();
    private ConnectionPageGeneral pageGeneral;


    public NewConnectionWizard() {
        this(null);
    }

    public NewConnectionWizard(DBPDriver initialDriver) {
        setWindowTitle(CoreMessages.dialog_new_connection_wizard_title);
        this.initialDriver = initialDriver;
    }

    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
        DBPProject project = initialDriver == null ? pageDrivers.getConnectionProject() : DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        return project == null ? null : project.getDataSourceRegistry();
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
    public DBPDriver getSelectedDriver()
    {
        return initialDriver != null ? initialDriver : getPageDrivers().getSelectedDriver();
    }

    @Override
    DBPProject getSelectedProject() {
        return pageDrivers.getConnectionProject();
    }

    @Override
    DBNBrowseSettings getSelectedNavigatorSettings() {
        return pageDrivers.getNavigatorSettings();
    }

    @Override
    public ConnectionPageSettings getPageSettings()
    {
        DriverDescriptor selectedDriver = (DriverDescriptor) getSelectedDriver();
        if (selectedDriver == null) {
            return null;
        }
        return this.settingsPages.get(selectedDriver.getProviderDescriptor());
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages()
    {
        if (initialDriver == null) {
            pageDrivers = new ConnectionPageDriver(this);
            addPage(pageDrivers);
        }

        for (DataSourceProviderDescriptor provider : DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders()) {
            availableProvides.add(provider);
            DataSourceViewDescriptor view = DataSourceViewRegistry.getInstance().findView(provider, IActionConstants.NEW_CONNECTION_POINT);
            if (view != null) {
                ConnectionPageSettings pageSettings = new ConnectionPageSettings(
                    NewConnectionWizard.this,
                    view);
                settingsPages.put(provider, pageSettings);
                addPage(pageSettings);
            }
        }

        pageGeneral = new ConnectionPageGeneral(this);
        //pageNetwork = new ConnectionPageNetwork(this);
        addPage(pageGeneral);
        //addPage(pageNetwork);

        // Initial settings
        if (selection != null && !selection.isEmpty()) {
            final Object element = selection.getFirstElement();
            if (element instanceof DBNLocalFolder) {
                pageGeneral.setDataSourceFolder(((DBNLocalFolder) element).getFolder());
            }
        }
    }

    @Override
    public IWizardPage getStartingPage() {
        if (initialDriver == null) {
            return super.getStartingPage();
        } else {
            return getPageSettings((DriverDescriptor) getSelectedDriver());
        }
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        if (initialDriver != null && page instanceof ConnectionPageSettings) {
            return null;
        }
        return super.getPreviousPage(page);
    }

    @Nullable
    @Override
    public IWizardPage getNextPage(IWizardPage page)
    {
        if (page == pageDrivers) {
            ConnectionPageSettings pageSettings = getPageSettings((DriverDescriptor) getSelectedDriver());
            if (pageSettings == null) {
                return pageGeneral;
            } else {
                return pageSettings;
            }
        } else if (page instanceof ConnectionPageSettings) {
            return null;//pageDrivers.getSelectedDriver().isEmbedded() ? pageGeneral : pageNetwork;
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
        DriverDescriptor driver = (DriverDescriptor) getSelectedDriver();
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
        //pageNetwork.saveSettings(dataSource);
    }

    @Override
    public boolean isNew() {
        return true;
    }

}
