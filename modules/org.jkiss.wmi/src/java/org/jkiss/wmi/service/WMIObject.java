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
package org.jkiss.wmi.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
