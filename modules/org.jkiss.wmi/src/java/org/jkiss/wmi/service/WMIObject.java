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
    private volatile List<WMIObjectAttribute> attributes;
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
        return readAttributeValue(name);
    }

    public void setValue(String name, Object value) throws WMIException
    {
        writeAttributeValue(name, value);
    }

    public Collection<WMIObjectAttribute> getAttributes() throws WMIException
    {
        readAttributes();
        return attributes;
    }

    public WMIObjectAttribute getAttribute(String name) throws WMIException
    {
        readAttributes();
        for (WMIObjectAttribute attribute : attributes) {
            if (attribute.getName().equals(name)) {
                return attribute;
            }
        }
        return null;
    }

    private void readAttributes()
        throws WMIException
    {
        if (attributes != null) {
            return;
        }
        synchronized (this) {
            if (attributes != null) {
                return;
            }
            attributes = new ArrayList<WMIObjectAttribute>();
            readAttributes(attributes);
        }
    }

    public Collection<WMIObjectMethod> getMethods() throws WMIException
    {
        readMethods();
        return methods;
    }

    public WMIObjectMethod getMethod(String name) throws WMIException
    {
        readMethods();
        for (WMIObjectMethod method : methods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    private void readMethods()
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

    private native Object readAttributeValue(String name)
        throws WMIException;

    private native void writeAttributeValue(String name, Object value)
        throws WMIException;

    private native void readAttributes(List<WMIObjectAttribute> attributes)
        throws WMIException;

    private native void readMethods(List<WMIObjectMethod> method)
        throws WMIException;

    native void readQualifiers(boolean isAttribute, String attrName, List<WMIQualifier> qualifiers)
        throws WMIException;

    native void releaseObject();

}
