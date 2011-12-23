/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * WQL ResultSet
 */
public class WMIObject {

    private Map<String, Object> properties = new TreeMap<String, Object>();

    public WMIObject() {
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Object getProperty(String name)
    {
        return properties.get(name);
    }

    public void addProperty(String name, Object value)
    {
        properties.put(name, value);
    }

}
