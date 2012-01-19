/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

/**
 * WMI object property
 */
public class WMIObjectMethod extends WMIObjectElement
{

    private WMIObject inParameter;
    private WMIObject outParameter;

    public WMIObjectMethod(WMIObject owner, String name, WMIObject inParameter, WMIObject outParameter)
    {
        super(owner, name);
        this.inParameter = inParameter;
        this.outParameter = outParameter;
    }

    public WMIObject getInParameter()
    {
        return inParameter;
    }

    public WMIObject getOutParameter()
    {
        return outParameter;
    }

    @Override
    public String toString()
    {
        return getName() + "()";
    }
}
