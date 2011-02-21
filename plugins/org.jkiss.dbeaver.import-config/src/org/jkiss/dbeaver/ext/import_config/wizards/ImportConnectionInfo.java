/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
    private int port;
    private String database;
    private String user;
    private String password;
    private Map<String, String> properties = new HashMap<String, String>();
    private boolean checked = false;

    public ImportConnectionInfo(ImportDriverInfo driverInfo, String id, String alias, String url, String host, int port, String database, String user, String password)
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

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
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

    public Map<String, String> getProperties()
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
