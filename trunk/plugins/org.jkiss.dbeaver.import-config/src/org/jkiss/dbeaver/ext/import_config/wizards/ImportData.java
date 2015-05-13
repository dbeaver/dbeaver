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
