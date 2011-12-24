/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import java.util.List;

/**
 * WMI object property
 */
public class WMIObjectProperty extends WMIObjectAttribute {

    public static int TYPE_INT = 1;

    private int type;
    private int flavor;
    private Object value;

    public WMIObjectProperty(WMIObject owner, String name, int type, int flavor, Object value)
    {
        super(owner, name);
        this.type = type;
        this.flavor = flavor;
        this.value = value;
    }

    public int getType()
    {
        return type;
    }

    public int getFlavor()
    {
        return flavor;
    }

    public Object getValue()
    {
        return value;
    }

}
