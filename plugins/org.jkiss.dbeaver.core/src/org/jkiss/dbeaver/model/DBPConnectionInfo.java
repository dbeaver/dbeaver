/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import java.util.HashMap;
import java.util.Map;

/**
 * DBPConnectionInfo
 */
public class DBPConnectionInfo implements DBPObject
{
    //private DBPDriver driver;
    private String hostName;
    private int hostPort;
    private String serverName;
    private String databaseName;
    private String userName;
    private String userPassword;
    private String url;
    private Map<String, String> properties = new HashMap<String, String>();

    public DBPConnectionInfo()
    {
    }

    public DBPConnectionInfo(DBPConnectionInfo info)
    {
        this.hostName = info.hostName;
        this.hostPort = info.hostPort;
        this.serverName = info.serverName;
        this.databaseName = info.databaseName;
        this.userName = info.userName;
        this.userPassword = info.userPassword;
        this.url = info.url;
        this.properties = new HashMap<String, String>(info.properties);
    }

/*
	public DBPConnectionInfo(DBPDriver driver)
	{
		this.driver = driver;
	}

	public DBPDriver getDriver()
	{
		return driver;
	}
*/

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    public int getHostPort()
    {
        return hostPort;
    }

    public void setHostPort(int hostPort)
    {
        this.hostPort = hostPort;
    }

    public String getServerName()
    {
        return serverName;
    }

    public void setServerName(String serverName)
    {
        this.serverName = serverName;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserPassword()
    {
        return userPassword;
    }

    public void setUserPassword(String userPassword)
    {
        this.userPassword = userPassword;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, String> properties)
    {
        this.properties = new HashMap<String, String>();
        this.properties.putAll(properties);
    }

}
