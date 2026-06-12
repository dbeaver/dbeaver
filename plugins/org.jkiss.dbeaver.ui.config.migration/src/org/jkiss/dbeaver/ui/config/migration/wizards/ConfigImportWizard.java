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
package org.jkiss.dbeaver.ui.config.migration.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.DataSourceUtils;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ConfigImportWizard extends Wizard implements IImportWizard {
    private static final Log log = Log.getLog(ConfigImportWizard.class);
    
    private ConfigImportWizardPage mainPage;
    private final Map<String, DBPDriver> driverClassMap = new HashMap<>();

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
            DBPDriver driver = driverClassMap.get(driverInfo.getDriverClass());
            if (driver != null) {
                connectionInfo.setDriver(driver);
                return true;
            }
        }
        final DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        List<DBPDriver> matchedDrivers = new ArrayList<>();
        for (DataSourceProviderDescriptor dataSourceProvider : registry.getDataSourceProviders()) {
            for (DBPDriver driver : dataSourceProvider.getEnabledDrivers()) {
                final String driverClassName = driver.getDriverClassName();
                if (driverClassName != null && driverClassName.equals(driverInfo.getDriverClass())) {
                    matchedDrivers.add(driver);
                }
            }
        }

        DBPDriver driver;
        if (matchedDrivers.isEmpty()) {
            // Create new driver
            final DataSourceProviderDescriptor genericProvider = registry.getDataSourceProvider("generic");
            if (genericProvider == null) {
                throw new DBException("Generic datasource provider not found");
            }

            DriverDescriptor newDriver = genericProvider.createDriver();
            newDriver.setName(driverInfo.getName());
            newDriver.setDriverClassName(driverInfo.getDriverClass(), true);
            newDriver.setSampleURL(driverInfo.getSampleURL());
            newDriver.setConnectionProperties(driverInfo.getProperties());
            newDriver.setDescription(driverInfo.getDescription());
            newDriver.setDriverDefaultPort(driverInfo.getDefaultPort());
            newDriver.setDriverDefaultDatabase(driverInfo.getDefaultDatabase());
            newDriver.setDriverDefaultServer(driverInfo.getDefaultServer());
            newDriver.setDriverDefaultUser(driverInfo.getDefaultUser());
            for (String path : driverInfo.getLibraries()) {
                newDriver.addDriverLibrary(path, DBPDriverLibrary.FileType.jar);
            }
            newDriver.setModified(true);
            genericProvider.addDriver(newDriver);
            connectionInfo.setDriver(newDriver);
            driver = newDriver;
        } else {
            // Use the only found driver
            driver = matchedDrivers.stream()
                    .filter(driverDescriptor -> driverDescriptor.getName().equalsIgnoreCase(driverInfo.getName()))
                    .findFirst()
                    .orElse(matchedDrivers.getFirst());
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
        dataSource.setName(DataSourceUtils.generateUniqueDataSourceName(dataSourceRegistry, connectionInfo.getAlias(), 2));
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
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            /*
             * URL is not null and does not agree with sampleURL from driver.
             * Still we proceed to import because it can be any other valid url format for the driver.
             */
            log.info("Import url as is it for url:" + url);
        }

        // Extract URL query parameters as provider properties. Non-standard datasource pages
        // (e.g. Snowflake warehouse/schema/role) keep their parameters in the query string and
        // are lost otherwise because the sample URL pattern does not capture them.
        extractUrlQueryParams(connectionInfo);
    }

    /**
     * Try to parse url by driver sample url.
     * NOTE sampleURL is not the only possible way to define a valid url.
     *
     */
    private void parseUrlAsDriverSampleUrl(ImportConnectionInfo connectionInfo) {
        String url = connectionInfo.getUrl();

        String sampleURL = connectionInfo.getDriverInfo().getSampleURL();
        if (connectionInfo.getDriver() != null) {
            sampleURL = connectionInfo.getDriver().getSampleURL();
        }
        boolean matched = tryMatchSampleUrl(connectionInfo, url, sampleURL);
        parseStandardJdbcUrl(connectionInfo, url);
        if (!matched) {
            log.debug("Sample URL pattern did not match '" + url + "', used generic JDBC URL parsing");
        }
    }

    private boolean tryMatchSampleUrl(@NotNull ImportConnectionInfo connectionInfo, @NotNull String url, @NotNull String sampleURL) {
        Pattern pattern = DatabaseURL.getPattern(sampleURL);
        Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            int queryStart = url.indexOf('?');
            if (queryStart <= 0) {
                return false;
            }
            matcher = pattern.matcher(url.substring(0, queryStart));
            if (!matcher.matches()) {
                return false;
            }
        }
        String host = safeGroup(matcher, "host");
        if (!CommonUtils.isEmpty(host)) {
            connectionInfo.setHost(host);
        }
        String port = safeGroup(matcher, "port");
        if (!CommonUtils.isEmpty(port)) {
            connectionInfo.setPort(port);
        }
        String database = safeGroup(matcher, "database");
        if (!CommonUtils.isEmpty(database)) {
            connectionInfo.setDatabase(database);
        }
        return true;
    }

    /**
     * Fallback parse for any standard-shape JDBC URL (jdbc:scheme://host[:port][/path][?query]).
     * Only fills fields that were not set by the sample URL pattern match.
     */
    private void parseStandardJdbcUrl(@NotNull ImportConnectionInfo connectionInfo, @NotNull String url) {
        String jdbcPrefix = "jdbc:";
        if (!url.startsWith(jdbcPrefix)) {
            return;
        }
        try {
            URI uri = URI.create(url.substring(jdbcPrefix.length()));
            String host = uri.getHost();
            if (!CommonUtils.isEmpty(host) && CommonUtils.isEmpty(connectionInfo.getHost())) {
                connectionInfo.setHost(host);
            }
            int port = uri.getPort();
            if (port > 0 && CommonUtils.isEmpty(connectionInfo.getPort())) {
                connectionInfo.setPort(String.valueOf(port));
            }
            String path = uri.getPath();
            if (!CommonUtils.isEmpty(path) && path.length() > 1 && CommonUtils.isEmpty(connectionInfo.getDatabase())) {
                connectionInfo.setDatabase(path.substring(1));
            }
        } catch (Exception e) {
            log.debug("Could not parse connection URL as standard JDBC URL: " + e.getMessage());
        }
    }

    @Nullable
    private static String safeGroup(@NotNull Matcher matcher, @NotNull String groupName) {
        try {
            return matcher.group(groupName);
        } catch (IllegalArgumentException e) {
            // Named group not present in the sample URL pattern.
            return null;
        }
    }

    /**
     * Extract query string key=value pairs from the connection URL and store them as
     * provider properties on the connection info. This makes non-standard driver
     * parameters (e.g. Snowflake warehouse/schema/role) available when the connection is
     * later used to rebuild the URL or shown in the driver's connection page.
     */
    protected void extractUrlQueryParams(@NotNull ImportConnectionInfo connectionInfo) {
        String url = connectionInfo.getUrl();
        if (CommonUtils.isEmpty(url)) {
            return;
        }
        int queryStart = url.indexOf('?');
        if (queryStart < 0 || queryStart == url.length() - 1) {
            return;
        }
        String query = url.substring(queryStart + 1);
        for (String param : query.split("&")) {
            int eqIdx = param.indexOf('=');
            if (eqIdx <= 0) {
                continue;
            }
            String key = param.substring(0, eqIdx).trim();
            String value = param.substring(eqIdx + 1).trim();
            if (key.isEmpty()) {
                continue;
            }
            if ("db".equalsIgnoreCase(key) || "database".equalsIgnoreCase(key)) {
                if (CommonUtils.isEmpty(connectionInfo.getDatabase())) {
                    connectionInfo.setDatabase(value);
                }
                continue;
            }
            if (!connectionInfo.getProviderProperties().containsKey(key)) {
                connectionInfo.setProviderProperty(key, value);
            }
        }
    }
}
