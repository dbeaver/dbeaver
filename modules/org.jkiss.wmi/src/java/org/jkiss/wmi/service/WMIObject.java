/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import java.util.*;

/**
 * WQL ResultSet
 */
public class WMIObject extends WMIQualifiedObject {

    private long objectHandle;
    private volatile List<WMIObjectProperty> properties;
    private volatile List<WMIObjectMethod> methods;

    public WMIObject() {
    }

    public String getObjectText()
        throws WMIException
    {
        return readObjectText();
    }

    public Object getValue(String name) throws WMIException
    {
        return readPropertyValue(name);
    }

    public void setValue(String name, Object value) throws WMIException
    {
        writePropertyValue(name, value);
    }

    public Collection<WMIObjectProperty> getProperties() throws WMIException
    {
        readProperties();
        return properties;
    }

    public WMIObjectProperty getProperty(String name) throws WMIException
    {
        readProperties();
        for (WMIObjectProperty property : properties) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    private void readProperties()
        throws WMIException
    {
        if (properties != null) {
            return;
        }
        synchronized (this) {
            if (properties != null) {
                return;
            }
            properties = new ArrayList<WMIObjectProperty>(); 
            readProperties(properties);
        }
    }

    public Collection<WMIObjectMethod> getMethod() throws WMIException
    {
        readMethod();
        return methods;
    }

    public WMIObjectMethod getMethod(String name) throws WMIException
    {
        readMethod();
        for (WMIObjectMethod method : methods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    private void readMethod()
        throws WMIException
    {
        if (methods != null) {
            return;
        }
        synchronized (this) {
            if (methods != null) {
                return;
            }
            methods = new ArrayList<WMIObjectMethod>();
            readMethods(methods);
        }
    }

    public void release()
    {
        releaseObject();
    }

    @Override
    protected void finalize() throws Throwable
    {
        releaseObject();
        super.finalize();
    }

    @Override
    protected void readObjectQualifiers(List<WMIQualifier> qualifiers)
        throws WMIException
    {
        readQualifiers(false, null, qualifiers);
    }

    private native String readObjectText()
        throws WMIException;

    private native Object readPropertyValue(String name)
        throws WMIException;

    private native void writePropertyValue(String name, Object value)
        throws WMIException;

    private native void readProperties(List<WMIObjectProperty> properties)
        throws WMIException;

    private native void readMethods(List<WMIObjectMethod> method)
        throws WMIException;

    native void readQualifiers(boolean property, String attrName, List<WMIQualifier> qualifiers)
        throws WMIException;

    native void releaseObject();

}
