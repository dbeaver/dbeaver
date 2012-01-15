/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

/**
 * Qualifier
 */
public class WMIQualifier {

    private final String name;
    private final Object value;
    private final int flavor;

    public WMIQualifier(String name, int flavor, Object value)
    {
        this.name = name;
        this.flavor = flavor;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public Object getValue()
    {
        return value;
    }

    public int getFlavor()
    {
        return flavor;
    }

    @Override
    public String toString()
    {
        return name + "=" + value;
    }
}
