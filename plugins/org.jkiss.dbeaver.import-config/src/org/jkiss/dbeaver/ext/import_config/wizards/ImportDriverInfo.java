/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
    private Map<String, String> properties = new HashMap<String, String>();
    private int defaultPort;

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

    public int getDefaultPort()
    {
        return defaultPort;
    }

    public void setDefaultPort(int defaultPort)
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

    public Map<String,String> getProperties()
    {
        return properties;
    }

    public void setProperty(String name, String value)
    {
        properties.put(name, value);
    }

    @Override
    public String toString()
    {
        return name + " - " + driverClass + " - " + sampleURL;
    }
}
