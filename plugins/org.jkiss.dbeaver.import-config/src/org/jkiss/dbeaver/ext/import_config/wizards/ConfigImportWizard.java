/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.SelectObjectDialog;

import java.util.ArrayList;
import java.util.List;

public abstract class ConfigImportWizard extends Wizard implements IImportWizard {
	
	private ConfigImportWizardPage mainPage;

	public ConfigImportWizard() {
		super();
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Configuration"); //NON-NLS-1
		setNeedsProgressMonitor(true);
		mainPage = createMainPage(); //NON-NLS-1
	}

    protected abstract ConfigImportWizardPage createMainPage();

    public void addPages() {
        super.addPages(); 
        addPage(mainPage);        
    }

    public boolean performFinish() {
        final ImportData importData = mainPage.getImportData();
        try {
            for (ImportConnectionInfo connectionInfo : importData.getConnections()) {
                if (!findOrCreateDriver(connectionInfo)) {
                    return false;
                }
            }
        } catch (DBException e) {
            UIUtils.showErrorDialog(getShell(), "Import driver", null, e);
            return false;
        }

        try {
            for (ImportConnectionInfo connectionInfo : importData.getConnections()) {
                importConnection(connectionInfo);
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
        final DataSourceProviderRegistry registry = DBeaverCore.getInstance().getDataSourceProviderRegistry();
        List<DriverDescriptor> matchedDrivers = new ArrayList<DriverDescriptor>();
        for (DataSourceProviderDescriptor dataSourceProvider : registry.getDataSourceProviders()) {
            for (DriverDescriptor driver : dataSourceProvider.getDrivers()) {
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
            genericProvider.addDriver(driver);
            connectionInfo.setDriver(driver);
        } else if (matchedDrivers.size() == 1) {
            // Use the only found driver
            connectionInfo.setDriver(matchedDrivers.get(0));
        } else {
            // Let user to choose correct driver
            final DriverDescriptor driver = SelectObjectDialog.selectObject(getShell(), "Choose driver for connection '" + connectionInfo.getAlias() + "'", matchedDrivers);
            if (driver == null) {
                return false;
            }
            connectionInfo.setDriver(driver);
        }
        return true;
    }

    private void importConnection(ImportConnectionInfo connectionInfo) throws DBException
    {
        final DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry();

        String name = connectionInfo.getAlias();
        for (int i = 0; ; i++) {
            if (dataSourceRegistry.findDataSourceByName(name) == null) {
                break;
            }
            name = connectionInfo.getAlias() + " " + (i + 1);
        }

        DBPConnectionInfo config = new DBPConnectionInfo();
        config.setProperties(connectionInfo.getProperties());
        config.setUrl(connectionInfo.getUrl());
        config.setUserName(connectionInfo.getUser());
        config.setHostName(connectionInfo.getHost());
        config.setHostPort(connectionInfo.getPort());
        config.setDatabaseName(connectionInfo.getDatabase());
        DataSourceDescriptor dataSource = new DataSourceDescriptor(
            dataSourceRegistry,
            DataSourceDescriptor.generateNewId(connectionInfo.getDriver()),
            connectionInfo.getDriver(),
            config);
        dataSource.setName(name);
        dataSource.setSavePassword(false);
        dataSourceRegistry.addDataSource(dataSource);
    }

}
