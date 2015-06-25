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
package org.jkiss.dbeaver.ext.import_config.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionConfiguration;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.SelectObjectDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class ConfigImportWizard extends Wizard implements IImportWizard {
	
	private ConfigImportWizardPage mainPage;

	public ConfigImportWizard() {
		super();
	}

	@Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Configuration"); //NON-NLS-1
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
            UIUtils.showErrorDialog(getShell(), "Import driver", null, e);
            return false;
        }

        try {
            for (ImportConnectionInfo connectionInfo : importData.getConnections()) {
                if (connectionInfo.isChecked()) {
                    importConnection(connectionInfo);
                }
            }
        } catch (DBException e) {
            UIUtils.showErrorDialog(getShell(), "Import driver", null, e);
            return false;
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
        final DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        List<DriverDescriptor> matchedDrivers = new ArrayList<DriverDescriptor>();
        for (DataSourceProviderDescriptor dataSourceProvider : registry.getDataSourceProviders()) {
            for (DriverDescriptor driver : dataSourceProvider.getEnabledDrivers()) {
                if (driver.getDriverClassName().equals(driverInfo.getDriverClass())) {
                    matchedDrivers.add(driver);
                }
            }
        }

        if (matchedDrivers.isEmpty()) {
            // Create new driver
            final DataSourceProviderDescriptor genericProvider = registry.getDataSourceProvider("generic");
            if (genericProvider == null) {
                throw new DBException("Generic datasource provider not found");
            }

            DriverDescriptor driver = genericProvider.createDriver();
            driver.setName(driverInfo.getName());
            driver.setDriverClassName(driverInfo.getDriverClass());
            driver.setSampleURL(driverInfo.getSampleURL());
            driver.setConnectionProperties(driverInfo.getProperties());
            driver.setDescription(driverInfo.getDescription());
            driver.setDriverDefaultPort(driverInfo.getDefaultPort());
            for (String path : driverInfo.getLibraries()) {
                driver.addLibrary(path);
            }
            driver.setModified(true);
            genericProvider.addDriver(driver);
            connectionInfo.setDriver(driver);
        } else if (matchedDrivers.size() == 1) {
            // Use the only found driver
            connectionInfo.setDriver(matchedDrivers.get(0));
        } else {
            // Let user to choose correct driver
            final DriverDescriptor driver = SelectObjectDialog.selectObject(
                getShell(), "Choose driver for connection '" + connectionInfo.getAlias() + "'", matchedDrivers);
            if (driver == null) {
                return false;
            }
            connectionInfo.setDriver(driver);
        }
        return true;
    }

    private void importConnection(ImportConnectionInfo connectionInfo) throws DBException
    {
        try {
            adaptConnectionUrl(connectionInfo);
        } catch (DBException e) {
            UIUtils.showMessageBox(getShell(), "Extract URL parameters", e.getMessage(), SWT.ICON_WARNING);
        }
        final DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry();

        String name = connectionInfo.getAlias();
        for (int i = 0; ; i++) {
            if (dataSourceRegistry.findDataSourceByName(name) == null) {
                break;
            }
            name = connectionInfo.getAlias() + " " + (i + 1);
        }

        DBPConnectionConfiguration config = new DBPConnectionConfiguration();
        config.setProperties(connectionInfo.getProperties());
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
        final DriverDescriptor.MetaURL metaURL = DriverDescriptor.parseSampleURL(sampleURL);
        final String url = connectionInfo.getUrl();
        int sourceOffset = 0;
        List<String> urlComponents = metaURL.getUrlComponents();
        for (int i = 0, urlComponentsSize = urlComponents.size(); i < urlComponentsSize; i++) {
            String component = urlComponents.get(i);
            if (component.length() > 2 && component.charAt(0) == '{' && component.charAt(component.length() - 1) == '}' && metaURL.getAvailableProperties().contains(component.substring(1, component.length() - 1))) {
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
                            throw new DBException("Can't parse URL '" + url + "' with pattern '" + sampleURL + "'. String '" + nextComponent + "' not found after '" + component);
                        }
                    }
                } else {
                    partEnd = url.length();
                }

                String propertyValue = url.substring(sourceOffset, partEnd);
                if (component.equals("{host}")) {
                    connectionInfo.setHost(propertyValue);
                } else if (component.equals("{port}")) {
                    connectionInfo.setPort(propertyValue);
                } else if (component.equals("{database}")) {
                    connectionInfo.setDatabase(propertyValue);
                } else {
                    throw new DBException("Unsupported property " + component);
                }
                sourceOffset = partEnd;
            } else {
                // Static string
                sourceOffset += component.length();
            }
        }
    }

}
