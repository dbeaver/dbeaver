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
    private Map<Object, Object> properties = new HashMap<>();
    private String defaultPort;
    private String description;

    public ImportDriverInfo(String id, String name, String sampleURL, String driverClass)
    {
        this.id = id;
        this.name = name;
        this.sampleURL = sampleURL;
        this.driverClass = driverClass;
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

    public List<String> getLibraries()
    {
        return libraries;
    }

    public void addLibrary(String path)
    {
        libraries.add(path);
    }

    public Map<Object,Object> getProperties()
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
