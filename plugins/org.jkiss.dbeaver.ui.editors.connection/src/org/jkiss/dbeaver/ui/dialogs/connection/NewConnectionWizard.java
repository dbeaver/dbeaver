/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.registry.DataSourceConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.DataSourceConfiguratorRegistry;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ConnectionFeatures;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

import java.util.*;

/**
 * This is a sample new wizard.
 */

public class NewConnectionWizard extends ConnectionWizard
{
    private final DBPDriver initialDriver;
    private final DBPConnectionConfiguration initialConfiguration;
    private IStructuredSelection selection;
    private final List<DBPDataSourceProviderDescriptor> availableProvides = new ArrayList<>();
    private ConnectionPageDataSource pageDataSource;
    private ConnectionPageConnector pageConnector;
    private final Map<DBPDriver, ConnectionPageSettings> settingsPages = new HashMap<>();
    private ConnectionPageGeneral pageGeneral;
    private DataSourceDescriptor dataSourceNew;

    /** A default constructor used by Eclipse's "New" command */
    @SuppressWarnings("unused")
    public NewConnectionWizard() {
        this(null, null);
    }

    public NewConnectionWizard(@Nullable DBPDriver initialDriver, @Nullable DBPConnectionConfiguration initialConfiguration) {
        setWindowTitle(UIConnectionMessages.dialog_new_connection_wizard_title);
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
    @Nullable
    public DBPDataSourceRegistry getDataSourceRegistry() {
        DBPProject project = initialDriver == null ? pageDataSource.getConnectionProject() : DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        return project == null ? null : project.getDataSourceRegistry();
    }

    List<DBPDataSourceProviderDescriptor> getAvailableProvides()
    {
        return availableProvides;
    }

    @NotNull
    ConnectionPageDataSource getPageDataSource()
    {
        return pageDataSource;
    }

    @Nullable
    ConnectionPageSettings getPageSettings(@NotNull DBPDriver driver)
    {
        return this.settingsPages.get(driver);
    }

    @Override
    @Nullable
    public DBPDriver getSelectedDriver()
    {
        if (initialDriver != null) {
            return initialDriver;
        }
        DBPDataSourceType type = getPageDataSource().getSelectedDataSourceType();
        DBPDriver selectedDriver = pageConnector.getDataSourceType() == type ? pageConnector.getSelectedDriver() : null;
        if (selectedDriver != null) {
            return selectedDriver;
        }
        List<? extends DBPDriver> drivers = getAvailableDrivers(type);
        return drivers.isEmpty() ? null : drivers.get(0);
    }

    @Override
    @Nullable
    DBPProject getSelectedProject() {
        return pageDataSource.getConnectionProject();
    }

    @Override
    @NotNull
    DBNBrowseSettings getSelectedNavigatorSettings() {
        return pageDataSource.getNavigatorSettings();
    }

    @Override
    public ConnectionPageSettings getPageSettings()
    {
        DriverDescriptor selectedDriver = (DriverDescriptor) getSelectedDriver();
        if (selectedDriver == null) {
            return null;
        }
        return this.settingsPages.get(selectedDriver);
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages()
    {
        /*if (initialDriver == null) */{
            // We need drivers page always as it contains some settings
            pageDataSource = new ConnectionPageDataSource(this);
            if (initialDriver != null) {
                pageDataSource.setSelectedDataSourceType(initialDriver.getDataSourceType());
            }
            addPage(pageDataSource);
        }
        pageConnector = new ConnectionPageConnector(this);
        addPage(pageConnector);

        Map<DataSourceConfiguratorDescriptor, ConnectionPageSettings> configuratorPages = new HashMap<>();
        for (DBPDataSourceProviderDescriptor provider : DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders()) {
            availableProvides.add(provider);
            for (DBPDriver driver : provider.getEnabledDrivers()) {
                DataSourceConfiguratorDescriptor configurator = DataSourceConfiguratorRegistry.getInstance()
                    .findConnectionConfigurator(driver);
                if (configurator != null) {
                    ConnectionPageSettings pageSettings = configuratorPages.get(configurator);
                    if (pageSettings == null) {
                        pageSettings = new ConnectionPageSettings(this, configurator, getDriverSubstitution());
                        configuratorPages.put(configurator, pageSettings);
                        addPage(pageSettings);
                    }
                    settingsPages.put(driver, pageSettings);
                }
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
        if (page == pageConnector) {
            return pageDataSource;
        }
        if (initialDriver == null &&
            (page instanceof ConnectionPageSettings || page instanceof ConnectionPageDeprecation || page == pageGeneral)
        ) {
            DBPDataSourceType type = pageDataSource.getSelectedDataSourceType();
            return getAvailableDrivers(type).size() > 1 ? pageConnector : pageDataSource;
        }
        return super.getPreviousPage(page);
    }

    @Nullable
    @Override
    public IWizardPage getNextPage(@NotNull IWizardPage page)
    {
        if (page == pageDataSource) {
            DBPDataSourceType type = pageDataSource.getSelectedDataSourceType();
            List<? extends DBPDriver> drivers = getAvailableDrivers(type);
            if (type != null) {
                pageConnector.setDataSourceType(type);
            }
            if (drivers.size() > 1) {
                return pageConnector;
            }
            return drivers.isEmpty() ? null : getNextPageForDriver(drivers.get(0));
        } else if (page == pageConnector) {
            return getNextPageForDriver(Objects.requireNonNull(pageConnector.getSelectedDriver()));
        } else if (page instanceof ConnectionPageSettings) {
            return null;
        } else {
            return null;
        }
    }

    @NotNull
    private IWizardPage getNextPageForDriver(@NotNull DBPDriver driver) {
        if (driver.getDriverStub() != null) {
            final ConnectionPageDeprecation nextPage = new ConnectionPageDeprecation(driver.getDriverStub());
            nextPage.setWizard(this);
            return nextPage;
        }
        ConnectionPageSettings pageSettings = getPageSettings(driver);
        if (pageSettings == null) {
            return pageGeneral;
        } else {
            return pageSettings;
        }
    }

    @NotNull
    private static List<? extends DBPDriver> getAvailableDrivers(@Nullable DBPDataSourceType type) {
        if (type == null) {
            return List.of();
        }
        return type.getEnabledDrivers().stream()
            .filter(driver -> !DBWorkbench.isDistributed() || driver.getDefaultDriverLoader().isDriverInstalled())
            .toList();
    }

    @NotNull
    @Override
    protected PersistResult persistDataSource() {
        DriverDescriptor driver = (DriverDescriptor) getSelectedDriver();
        if (driver.getDriverStub() != null) {
            return PersistResult.UNCHANGED;
        }

        DBPDataSourceRegistry dataSourceRegistry = Objects.requireNonNull(getDataSourceRegistry());

        if (dataSourceNew == null) {
            ConnectionPageSettings pageSettings = getPageSettings();
            DataSourceDescriptor dataSourceTpl = pageSettings == null ? getActiveDataSource() : pageSettings.getActiveDataSource();
            dataSourceNew = dataSourceRegistry.createDataSource(
                dataSourceTpl.getId(),
                driver,
                dataSourceTpl.getConnectionConfiguration()
            );

            dataSourceNew.copyFrom(dataSourceTpl);
            saveSettings(dataSourceNew);

            try {
                dataSourceRegistry.addDataSource(dataSourceNew);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Create failed", "Error adding new connections", e);
                return PersistResult.ERROR;
            }

            ConnectionFeatures.CONNECTION_CREATE.use(Map.of("driver", dataSourceNew.getDriver().getPreconfiguredId()));
        } else {
            saveSettings(dataSourceNew);
            dataSourceNew.persistConfiguration();

            try {
                dataSourceRegistry.checkForErrors();
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Persist failed", "Error persisting connection", e);
                return PersistResult.ERROR;
            }
        }

        return PersistResult.CHANGED;
    }

    /**
     * This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it
     * using wizard as execution context.
     */
    @Override
    public boolean performFinish() {
        return persistDataSource() != PersistResult.ERROR;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
        this.selection = selection;
    }

    @Override
    protected void saveSettings(DataSourceDescriptor dataSource) {
        final DBPDriver driver = dataSource.getDriver();
        if (driver.getDriverStub() != null) {
            return;
        }
        ConnectionPageSettings pageSettings = getPageSettings(driver);
        if (pageSettings != null) {
            pageSettings.saveSettings(dataSource);
        }
        pageGeneral.saveSettings(dataSource);
        //pageNetwork.saveSettings(dataSource);
    }

    @Nullable
    @Override
    public DataSourceDescriptor getOriginalDataSource() {
        return dataSourceNew;
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
