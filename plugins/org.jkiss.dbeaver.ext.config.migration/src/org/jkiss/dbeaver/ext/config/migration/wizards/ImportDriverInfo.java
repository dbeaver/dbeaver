/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.config.migration.wizards;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import data
 */
public class ImportDriverInfo {

    private String id;
    private String name;
    private String sampleURL;
    private String driverClass;
    private List<String> libraries = new ArrayList<>();
    private Map<String, Object> properties = new HashMap<>();
    private String defaultPort, defaultDatabase, defaultServer, defaultUser;
    private String description;

    public ImportDriverInfo(String id, String name, String sampleURL, String driverClass)
    {
        this.id = id;
        this.name = name;
        this.sampleURL = sampleURL;
        this.driverClass = driverClass;
    }

    public ImportDriverInfo(@Nullable DBPDriver driver) {
        if (driver == null) {
            this.id = "";
            this.name = "";
            this.sampleURL = "";
            this.driverClass = "";
            return;
        }
        this.id = driver.getId();
        this.name = driver.getName();
        this.sampleURL = driver.getSampleURL();
        this.driverClass = driver.getDriverClassName();
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getSampleURL()
    {
        return sampleURL;
    }

    public void setSampleURL(String sampleURL)
    {
        this.sampleURL = sampleURL;
    }

    public String getDriverClass()
    {
        return driverClass;
    }

    public String getDefaultPort()
    {
        return defaultPort;
    }

    public void setDefaultPort(String defaultPort)
    {
        this.defaultPort = defaultPort;
    }

    public String getDefaultDatabase() {
        return defaultDatabase;
    }

    public void setDefaultDatabase(String defaultDatabase) {
        this.defaultDatabase = defaultDatabase;
    }

    public String getDefaultServer() {
        return defaultServer;
    }

    public void setDefaultServer(String defaultServer) {
        this.defaultServer = defaultServer;
    }

    public String getDefaultUser() {
        return defaultUser;
    }

    public void setDefaultUser(String defaultUser) {
        this.defaultUser = defaultUser;
    }

    public List<String> getLibraries()
    {
        return libraries;
    }

    public void addLibrary(String path)
    {
        libraries.add(path);
    }

    public Map<String, Object> getProperties()
    {
        return properties;
    }

    public void setProperty(String name, String value)
    {
        properties.put(name, value);
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public String toString()
    {
        return name + " - " + driverClass + " - " + sampleURL;
    }

}
