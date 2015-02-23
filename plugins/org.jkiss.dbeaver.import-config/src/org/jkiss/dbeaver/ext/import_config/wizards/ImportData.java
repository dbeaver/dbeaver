/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

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

    public ImportDriverInfo getDriverByID(String id)
    {
        for (ImportDriverInfo driver : drivers) {
            if (id.equals(driver.getId())) {
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
