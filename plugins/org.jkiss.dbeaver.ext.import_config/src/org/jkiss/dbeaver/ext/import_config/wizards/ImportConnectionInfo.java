/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private Map<String, String> properties = new HashMap<>();
    private Map<String, String> providerProperties = new HashMap<>();
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

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void setProperty(String name, String value)
    {
        properties.put(name, value);
    }

    public Map<String, String> getProviderProperties() {
        return providerProperties;
    }

    public void setProviderProperty(String name, String value)
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
