/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.config.migration.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public abstract class ConfigImportWizard extends Wizard implements IImportWizard {
    private static final Log log = Log.getLog(ConfigImportWizard.class);
    
    private ConfigImportWizardPage mainPage;
    private Map<String, DriverDescriptor> driverClassMap = new HashMap<>();

    public ConfigImportWizard() {
		super();
	}

	@Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(ImportConfigMessages.config_import_wizard_header_import_configuration);
		setNeedsProgressMonitor(true);
		mainPage = createMainPage(); //NON-NLS-1
	}

    protected abstract ConfigImportWizardPage createMainPage();

    @Override
    public void addPages() {
        super.addPages(); 
        addPage(mainPage);        
    }

    @Override
    public boolean performFinish() {
        mainPage.deactivatePage();
        final ImportData importData = mainPage.getImportData();
        try {
            for (ImportConnectionInfo connectionInfo : importData.getConnections()) {
                if (connectionInfo.isChecked() && !findOrCreateDriver(connectionInfo)) {
                    return false;
                }
            }
            // Flush drivers configuration
            DataSourceProviderRegistry.getInstance().saveDrivers();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(ImportConfigMessages.config_import_wizard_import_driver, null, e);
            return false;
        }

        for (ImportConnectionInfo connectionInfo : importData.getConnections()) {
            if (connectionInfo.isChecked()) {
                importConnection(importData, connectionInfo);
            }
        }

        return true;
    }

    private boolean findOrCreateDriver(ImportConnectionInfo connectionInfo) throws DBException
    {
        final ImportDriverInfo driverInfo = connectionInfo.getDriverInfo();
        if (CommonUtils.isEmpty(driverInfo.getDriverClass())) {
            throw new DBException("Cannot create driver '" + driverInfo.getName() + "' - no driver class specified");
        }
        String sampleURL = driverInfo.getSampleURL();
        if (CommonUtils.isEmpty(sampleURL)) {
            throw new DBException("Cannot create driver '" + driverInfo.getName() + "' - no connection URL pattern specified");
        }
        {
            DriverDescriptor driver = driverClassMap.get(driverInfo.getDriverClass());
            if (driver != null) {
                connectionInfo.setDriver(driver);
                return true;
            }
        }
        final DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        List<DriverDescriptor> matchedDrivers = new ArrayList<>();
        for (DataSourceProviderDescriptor dataSourceProvider : registry.getDataSourceProviders()) {
            for (DriverDescriptor driver : dataSourceProvider.getEnabledDrivers()) {
                final String driverClassName = driver.getDriverClassName();
                if (driverClassName != null && driverClassName.equals(driverInfo.getDriverClass())) {
                    matchedDrivers.add(driver);
                }
            }
        }

        DriverDescriptor driver;
        if (matchedDrivers.isEmpty()) {
            // Create new driver
            final DataSourceProviderDescriptor genericProvider = registry.getDataSourceProvider("generic");
            if (genericProvider == null) {
                throw new DBException("Generic datasource provider not found");
            }

            driver = genericProvider.createDriver();
            driver.setName(driverInfo.getName());
            driver.setDriverClassName(driverInfo.getDriverClass());
            driver.setSampleURL(driverInfo.getSampleURL());
            driver.setConnectionProperties(driverInfo.getProperties());
            driver.setDescription(driverInfo.getDescription());
            driver.setDriverDefaultPort(driverInfo.getDefaultPort());
            driver.setDriverDefaultDatabase(driverInfo.getDefaultDatabase());
            driver.setDriverDefaultServer(driverInfo.getDefaultServer());
            driver.setDriverDefaultUser(driverInfo.getDefaultUser());
            for (String path : driverInfo.getLibraries()) {
                driver.addDriverLibrary(path, DBPDriverLibrary.FileType.jar);
            }
            driver.setModified(true);
            genericProvider.addDriver(driver);
            connectionInfo.setDriver(driver);
        } else {
            // Use the only found driver
            driver = matchedDrivers.stream()
                    .filter(driverDescriptor -> driverDescriptor.getName().equalsIgnoreCase(driverInfo.getName()))
                    .findFirst()
                    .orElse(matchedDrivers.get(0));
            connectionInfo.setDriver(driver);
        }

        if (driver != null) {
            //fixme driverClassName is not uniq
            driverClassMap.put(driver.getDriverClassName(), driver);
            return true;
        }
        return false;
    }

    private void importConnection(ImportData importData, ImportConnectionInfo connectionInfo) {
        try {
            adaptConnectionUrl(connectionInfo);
        } catch (DBException e) {
            UIUtils.showMessageBox(getShell(), ImportConfigMessages.config_import_wizard_extract_url_parameters, e.getMessage(), SWT.ICON_WARNING);
        }
        final DBPDataSourceRegistry dataSourceRegistry = NavigatorUtils.getSelectedProject().getDataSourceRegistry();

        String name = connectionInfo.getAlias();
        for (int i = 0; ; i++) {
            if (dataSourceRegistry.findDataSourceByName(name) == null) {
                break;
            }
            name = connectionInfo.getAlias() + " " + (i + 1);
        }

        DBPConnectionConfiguration config = new DBPConnectionConfiguration();
        config.setProperties(connectionInfo.getProperties());
        config.setProviderProperties(connectionInfo.getProviderProperties());
        config.setUrl(connectionInfo.getUrl());
        config.setUserName(connectionInfo.getUser());
        config.setUserPassword(connectionInfo.getPassword());
        config.setHostName(connectionInfo.getHost());
        config.setHostPort(connectionInfo.getPort());
        config.setDatabaseName(connectionInfo.getDatabase());
        config.setAuthModelId(connectionInfo.getAuthModelId());
        config.setAuthProperties(connectionInfo.getAuthProperties());
        //It allows to specify whether connection url should be used directly or not after connection creation.
        if (CommonUtils.isEmpty(connectionInfo.getHost())) {
            config.setConfigurationType(DBPDriverConfigurationType.URL);
        } else {
            config.setConfigurationType(DBPDriverConfigurationType.MANUAL);
        }
        if (!connectionInfo.getNetworkHandlers().isEmpty()) {
            config.setHandlers(connectionInfo.getNetworkHandlers());
        }

        DataSourceDescriptor dataSource = dataSourceRegistry.createDataSource(
            DataSourceDescriptor.generateNewId(connectionInfo.getDriver()),
            connectionInfo.getDriver(),
            config
        );
        dataSource.setName(name);
        dataSource.setSavePassword(!CommonUtils.isEmpty(config.getUserPassword()));
        dataSource.setFolder(importData.getDataSourceFolder());
        try {
            dataSourceRegistry.addDataSource(dataSource);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(ImportConfigMessages.config_import_wizard_header_import_configuration, null, e);
        }
    }

    protected void adaptConnectionUrl(ImportConnectionInfo connectionInfo) throws DBException
    {
        
        //connectionInfo.getDriver()
        String url = connectionInfo.getUrl();
        if (url == null) {
            if (connectionInfo.getDriver() == null) {
                throw new DBCException("Can't detect target driver for '" + connectionInfo.getAlias() + "'");
            }
            if (connectionInfo.getHost() == null) {
                throw new DBCException("No URL and no host name - can't import connection '" + connectionInfo.getAlias() + "'");
            }
            // No URL - generate from props
            DBPConnectionConfiguration conConfig = new DBPConnectionConfiguration();
            conConfig.setHostName(connectionInfo.getHost());
            conConfig.setHostPort(connectionInfo.getPort());
            conConfig.setDatabaseName(connectionInfo.getDatabase());
            url = connectionInfo.getDriver().getConnectionURL(conConfig);
            connectionInfo.setUrl(url);
            return;
        }
        
        try {
            parseUrlAsDriverSampleUrl(connectionInfo);
            return;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        
        /*
         * Here parsing was not successful.
         * URL is not null and not agree with sampleURL from drive. 
         * Still we proceed to import cause can be any other valid url format for the driver.
         */
        log.info("Import url as is it for url:" + url);
        
    }

    /**
     * Try to parse url by driver sample url. 
     * NOTE sampleURL is not the only possible way to define a valid url.
     *
     * @throws DBException in case url does not reflect the sample one from driver.
     */
    private void parseUrlAsDriverSampleUrl(ImportConnectionInfo connectionInfo) throws DBException {
        String url = connectionInfo.getUrl();
        
        String sampleURL = connectionInfo.getDriverInfo().getSampleURL();
        if (connectionInfo.getDriver() != null) {
            sampleURL = connectionInfo.getDriver().getSampleURL();
        }
        Matcher matcher = DatabaseURL.getPattern(sampleURL).matcher(url);
        if (matcher.matches()) {
            connectionInfo.setHost(matcher.group("host"));
            connectionInfo.setPort(matcher.group("port"));
            connectionInfo.setDatabase(matcher.group("database"));
        }
    }
}
