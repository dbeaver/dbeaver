/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.dbeaver.registry.driver.DriverDescriptor;

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
    private Map<Object, Object> properties = new HashMap<>();
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

    public void setUrl(String url) {
        this.url = url;
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

    public void setUser(String user) {
        this.user = user;
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
