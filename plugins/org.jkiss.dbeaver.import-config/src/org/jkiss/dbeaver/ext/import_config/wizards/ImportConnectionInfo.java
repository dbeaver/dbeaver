package org.jkiss.dbeaver.ext.import_config.wizards;

import java.util.HashMap;
import java.util.Map;

/**
 * Import connection info
 */
public class ImportConnectionInfo {

    private ImportDriverInfo driver;
    private String id;
    private String alias;
    private String url;
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;
    private Map<String, String> properties = new HashMap<String, String>();

    public ImportConnectionInfo(ImportDriverInfo driver, String id, String alias, String url, String host, int port, String database, String user, String password)
    {
        this.driver = driver;
        this.id = id;
        this.alias = alias;
        this.url = url;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public ImportDriverInfo getDriver()
    {
        return driver;
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

    public String getDatabase()
    {
        return database;
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

}
