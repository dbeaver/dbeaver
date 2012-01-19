/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import java.util.List;

/**
 * Object element (property or method)
 */
public abstract class WMIObjectElement extends WMIQualifiedObject {

    private WMIObject owner;
    private String name;

    protected WMIObjectElement(WMIObject owner, String name)
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
        getOwner().readQualifiers(this instanceof WMIObjectAttribute, getName(), qualifiers);
    }

}
