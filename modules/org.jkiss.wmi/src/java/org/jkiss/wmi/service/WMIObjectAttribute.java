/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import java.util.List;

/**
 * Object attribute (property or method)
 */
public abstract class WMIObjectAttribute extends WMIQualifiedObject {

    private WMIObject owner;
    private String name;

    protected WMIObjectAttribute(WMIObject owner, String name)
    {
        this.owner = owner;
        this.name = name;
    }

    public WMIObject getOwner()
    {
        return owner;
    }

    public String getName()
    {
        return name;
    }

    @Override
    protected void readObjectQualifiers(List<WMIQualifier> qualifiers) throws WMIException
    {
        getOwner().readQualifiers(this instanceof WMIObjectProperty, getName(), qualifiers);
    }

}
