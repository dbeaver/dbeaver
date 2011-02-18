package org.jkiss.dbeaver.ext.import_config.wizards;

import java.util.ArrayList;
import java.util.List;

/**
 * Import data
 */
public class ImportData {

    private List<ImportDriverInfo> drivers = new ArrayList<ImportDriverInfo>();
    private List<ImportConnectionInfo> connections = new ArrayList<ImportConnectionInfo>();

    public List<ImportDriverInfo> getDrivers()
    {
        return drivers;
    }

    public ImportDriverInfo getDriver(String name)
    {
        for (ImportDriverInfo driver : drivers) {
            if (name.equals(driver.getName())) {
                return driver;
            }
        }
        return null;
    }

    public void addDriver(ImportDriverInfo driverInfo)
    {
        drivers.add(driverInfo);
    }

    public List<ImportConnectionInfo> getConnections()
    {
        return connections;
    }

    public void addConnection(ImportConnectionInfo connectionInfo)
    {
        connections.add(connectionInfo);
    }

}
