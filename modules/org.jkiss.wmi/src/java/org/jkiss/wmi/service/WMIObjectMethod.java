/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import java.util.List;

/**
 * WMI object property
 */
public class WMIObjectMethod extends WMIObjectAttribute {

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

}
