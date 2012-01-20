/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import java.util.*;

/**
 * WQL ResultSet
 */
public class WMIObject extends WMIQualifiedObject {

    private long objectHandle;

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

    public Collection<WMIObjectAttribute> getAttributes(long flags) throws WMIException
    {
        List<WMIObjectAttribute> attributes = new ArrayList<WMIObjectAttribute>();
        readAttributes(flags, attributes);
        return attributes;
    }

    public Collection<WMIObjectMethod> getMethods(long flags) throws WMIException
    {
        List<WMIObjectMethod> methods = new ArrayList<WMIObjectMethod>();
        readMethods(flags, methods);
        return methods;
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

    @Override
    public String toString()
    {
        return "WMIObject:" + objectHandle;
    }

    private native String readObjectText()
        throws WMIException;

    private native Object readAttributeValue(String name)
        throws WMIException;

    private native void writeAttributeValue(String name, Object value)
        throws WMIException;

    private native void readAttributes(long flags, List<WMIObjectAttribute> attributes)
        throws WMIException;

    private native void readMethods(long flags, List<WMIObjectMethod> method)
        throws WMIException;

    native void readQualifiers(boolean isAttribute, String attrName, List<WMIQualifier> qualifiers)
        throws WMIException;

    native void releaseObject();

}
