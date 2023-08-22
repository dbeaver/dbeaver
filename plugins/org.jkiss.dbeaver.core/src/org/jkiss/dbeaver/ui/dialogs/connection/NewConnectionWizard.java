/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreFeatures;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a sample new wizard.
 */

public class NewConnectionWizard extends ConnectionWizard
{
    private final DBPDriver initialDriver;
    private final DBPConnectionConfiguration initialConfiguration;
    private IStructuredSelection selection;
    private final List<DBPDataSourceProviderDescriptor> availableProvides = new ArrayList<>();
    private ConnectionPageDriver pageDrivers;
    private final Map<DBPDataSourceProviderDescriptor, ConnectionPageSettings> settingsPages = new HashMap<>();
    private ConnectionPageGeneral pageGeneral;

    public NewConnectionWizard() {
        this(null, null);
    }

    public NewConnectionWizard(@Nullable DBPDriver initialDriver, @Nullable DBPConnectionConfiguration initialConfiguration) {
        setWindowTitle(CoreMessages.dialog_new_connection_wizard_title);
        this.initialDriver = initialDriver;
        this.initialConfiguration = initialConfiguration;

        addPropertyChangeListener(event -> {
            if (ConnectionPageAbstract.PROP_DRIVER_SUBSTITUTION.equals(event.getProperty())) {
                ((Dialog) getContainer()).close();

                UIUtils.asyncExec(() -> NewConnectionDialog.openNewConnectionDialog(
                    UIUtils.getActiveWorkbenchWindow(),
                    getActiveDataSource().getDriver(),
                    getActiveDataSource().getConnectionConfiguration(),
                    wizard -> wizard.setDriverSubstitution((DBPDriverSubstitutionDescriptor) event.getNewValue())
                ));
            }
        });
    }

    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
        DBPProject project = initialDriver == null ? pageDrivers.getConnectionProject() : DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        return project == null ? null : project.getDataSourceRegistry();
    }

    List<DBPDataSourceProviderDescriptor> getAvailableProvides()
    {
        return availableProvides;
    }

    ConnectionPageDriver getPageDrivers()
    {
        return pageDrivers;
    }

    ConnectionPageSettings getPageSettings(DBPDriver driver)
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
        /*if (initialDriver == null) */{
            // We need drivers page always as it contains some settings
            pageDrivers = new ConnectionPageDriver(this);
            if (initialDriver != null) {
                pageDrivers.setSelectedDriver(initialDriver);
            }
            addPage(pageDrivers);
        }

        for (DBPDataSourceProviderDescriptor provider : DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders()) {
            availableProvides.add(provider);
            DataSourceViewDescriptor view = DataSourceViewRegistry.getInstance().findView(provider, IActionConstants.NEW_CONNECTION_POINT);
            if (view != null) {
                ConnectionPageSettings pageSettings = new ConnectionPageSettings(this, view, null, getDriverSubstitution());
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
            final DBPDriver driver = getSelectedDriver();
            if (driver.isDeprecated()) {
                final ConnectionPageDeprecation nextPage = new ConnectionPageDeprecation(driver);
                nextPage.setWizard(this);
                return nextPage;
            }
            ConnectionPageSettings pageSettings = getPageSettings(driver);
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
    public boolean performFinish() {
        DriverDescriptor driver = (DriverDescriptor) getSelectedDriver();
        if (driver.isDeprecated()) {
            return true;
        }
        ConnectionPageSettings pageSettings = getPageSettings();
        DataSourceDescriptor dataSourceTpl = pageSettings == null ? getActiveDataSource() : pageSettings.getActiveDataSource();
        DBPDataSourceRegistry dataSourceRegistry = getDataSourceRegistry();

        DataSourceDescriptor dataSourceNew = new DataSourceDescriptor(
            dataSourceRegistry, dataSourceTpl.getId(), driver, dataSourceTpl.getConnectionConfiguration());
        dataSourceNew.copyFrom(dataSourceTpl);
        saveSettings(dataSourceNew);
        try {
            dataSourceRegistry.addDataSource(dataSourceNew);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Create failed", "Error adding new connections", e);
            return false;
        }
        CoreFeatures.CONNECTION_CREATE.use(Map.of("driver", dataSourceNew.getDriver().getPreconfiguredId()));
        return true;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
        this.selection = selection;
    }

    @Override
    protected void saveSettings(DataSourceDescriptor dataSource) {
        final DBPDriver driver = dataSource.getDriver();
        if (driver.isDeprecated()) {
            return;
        }
        ConnectionPageSettings pageSettings = getPageSettings(driver);
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

    @NotNull
    @Override
    protected DBPConnectionConfiguration getDefaultConnectionConfiguration() {
        if (initialConfiguration != null) {
            return initialConfiguration;
        }
        return super.getDefaultConnectionConfiguration();
    }
}
