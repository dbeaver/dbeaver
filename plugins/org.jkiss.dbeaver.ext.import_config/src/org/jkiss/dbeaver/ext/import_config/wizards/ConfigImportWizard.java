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
package org.jkiss.dbeaver.ext.import_config.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.ImportConfigMessages;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCURL;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectObjectDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ConfigImportWizard extends Wizard implements IImportWizard {
	
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
            DBWorkbench.getPlatformUI().showError("Import driver", null, e);
            return false;
        }

        for (ImportConnectionInfo connectionInfo : importData.getConnections()) {
            if (connectionInfo.isChecked()) {
                importConnection(connectionInfo);
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
            for (String path : driverInfo.getLibraries()) {
                driver.addDriverLibrary(path, DBPDriverLibrary.FileType.jar);
            }
            driver.setModified(true);
            genericProvider.addDriver(driver);
            connectionInfo.setDriver(driver);
        } else if (matchedDrivers.size() == 1) {
            // Use the only found driver
            driver = matchedDrivers.get(0);
            connectionInfo.setDriver(driver);
        } else {
            // Let user to choose correct driver
            driver = SelectObjectDialog.selectObject(
                getShell(), "Choose driver for connection '" + connectionInfo.getAlias() + "'", "ImportDriverSelector", matchedDrivers);
            if (driver == null) {
                return false;
            }
            connectionInfo.setDriver(driver);
        }

        if (driver != null) {
            driverClassMap.put(driver.getDriverClassName(), driver);
            return true;
        }
        return false;
    }

    private void importConnection(ImportConnectionInfo connectionInfo) {
        try {
            adaptConnectionUrl(connectionInfo);
        } catch (DBException e) {
            UIUtils.showMessageBox(getShell(), "Extract URL parameters", e.getMessage(), SWT.ICON_WARNING);
        }
        final DBPDataSourceRegistry dataSourceRegistry =
            DBWorkbench.getPlatform().getWorkspace().getActiveProject().getDataSourceRegistry();

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
        DataSourceDescriptor dataSource = new DataSourceDescriptor(
            dataSourceRegistry,
            DataSourceDescriptor.generateNewId(connectionInfo.getDriver()),
            connectionInfo.getDriver(),
            config);
        dataSource.setName(name);
        dataSource.setSavePassword(!CommonUtils.isEmpty(config.getUserPassword()));
        dataSourceRegistry.addDataSource(dataSource);
    }

    protected void adaptConnectionUrl(ImportConnectionInfo connectionInfo) throws DBException
    {
        String sampleURL = connectionInfo.getDriverInfo().getSampleURL();
        if (connectionInfo.getDriver() != null) {
            sampleURL = connectionInfo.getDriver().getSampleURL();
        }
        //connectionInfo.getDriver()
        String url = connectionInfo.getUrl();
        if (url != null) {
            // Parse url
            final JDBCURL.MetaURL metaURL = JDBCURL.parseSampleURL(sampleURL);
            int sourceOffset = 0;
            List<String> urlComponents = metaURL.getUrlComponents();
            for (int i = 0, urlComponentsSize = urlComponents.size(); i < urlComponentsSize; i++) {
                String component = urlComponents.get(i);
                if (component.length() > 2 && component.charAt(0) == '{' && component.charAt(component.length() - 1) == '}' &&
                    metaURL.getAvailableProperties().contains(component.substring(1, component.length() - 1))) {
                    // Property
                    int partEnd;
                    if (i < urlComponentsSize - 1) {
                        // Find next component
                        final String nextComponent = urlComponents.get(i + 1);
                        partEnd = url.indexOf(nextComponent, sourceOffset);
                        if (partEnd == -1) {
                            if (nextComponent.equals(":")) {
                                // Try to find another divider - dbvis sometimes contains bad sample URLs (e.g. for Oracle)
                                partEnd = url.indexOf("/", sourceOffset);
                            }
                            if (partEnd == -1) {
                                if (connectionInfo.getHost() == null) {
                                    throw new DBException("Can't parse URL '" + url + "' with pattern '" + sampleURL + "'. String '" + nextComponent + "' not found after '" + component);
                                } else {
                                    // We have connection properties anyway
                                    url = null;
                                    break;
                                }
                            }
                        }
                    } else {
                        partEnd = url.length();
                    }

                    String propertyValue = url.substring(sourceOffset, partEnd);
                    switch (component) {
                        case "{host}":
                            connectionInfo.setHost(propertyValue);
                            break;
                        case "{port}":
                            connectionInfo.setPort(propertyValue);
                            break;
                        case "{database}":
                            connectionInfo.setDatabase(propertyValue);
                            break;
                        default:
                            if (connectionInfo.getHost() == null) {
                                throw new DBException("Unsupported property " + component);
                            }
                    }
                    sourceOffset = partEnd;
                } else {
                    // Static string
                    sourceOffset += component.length();
                }
            }
        }
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
            url = connectionInfo.getDriver().getDataSourceProvider().getConnectionURL(connectionInfo.getDriver(), conConfig);
            connectionInfo.setUrl(url);
        }
    }

}
