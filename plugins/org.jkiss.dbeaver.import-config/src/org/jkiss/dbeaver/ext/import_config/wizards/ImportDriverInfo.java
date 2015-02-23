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
    private List<String> libraries = new ArrayList<String>();
    private Map<Object, Object> properties = new HashMap<Object, Object>();
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
