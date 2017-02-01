/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
        List<WMIObjectAttribute> attributes = new ArrayList<>();
        readAttributes(flags, attributes);
        return attributes;
    }

    public Collection<WMIObjectMethod> getMethods(long flags) throws WMIException
    {
        List<WMIObjectMethod> methods = new ArrayList<>();
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
