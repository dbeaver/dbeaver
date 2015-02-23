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

import org.jkiss.dbeaver.registry.DriverDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Import connection info
 */
public class ImportConnectionInfo {

    private DriverDescriptor driver;
    private ImportDriverInfo driverInfo;
    private String id;
    private String alias;
    private String url;
    private String host;
    private String port;
    private String database;
    private String user;
    private String password;
    private Map<Object, Object> properties = new HashMap<Object, Object>();
    private boolean checked = false;

    public ImportConnectionInfo(ImportDriverInfo driverInfo, String id, String alias, String url, String host, String port, String database, String user, String password)
    {
        this.driverInfo = driverInfo;
        this.id = id;
        this.alias = alias;
        this.url = url;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public DriverDescriptor getDriver()
    {
        return driver;
    }

    public void setDriver(DriverDescriptor driver)
    {
        this.driver = driver;
    }

    public ImportDriverInfo getDriverInfo()
    {
        return driverInfo;
    }

    public String getId()
    {
        return id;
    }

    public String getAlias()
    {
        return alias;
    }

    public String getUrl()
    {
        return url;
    }

    public String getHost()
    {
        return host;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = port;
    }

    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
    }

    public Map<Object, Object> getProperties()
    {
        return properties;
    }

    public void setProperty(String name, String value)
    {
        properties.put(name, value);
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public boolean isChecked()
    {
        return checked;
    }

    public void setChecked(boolean checked)
    {
        this.checked = checked;
    }
}
