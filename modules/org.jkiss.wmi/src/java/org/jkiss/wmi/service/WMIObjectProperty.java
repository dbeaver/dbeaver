/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

import java.util.List;

/**
 * WMI object property
 */
public class WMIObjectProperty extends WMIObjectAttribute {

    public static int CIM_ILLEGAL      = 4095;  // 0xFFF
    public static int CIM_EMPTY        = 0;     // 0x0
    public static int CIM_SINT8        = 16;    // 0x10
    public static int CIM_UINT8        = 17;    // 0x11
    public static int CIM_SINT16       = 2;     // 0x2
    public static int CIM_UINT16       = 18;    // 0x12
    public static int CIM_SINT32       = 3;     // 0x3
    public static int CIM_UINT32       = 19;    // 0x13
    public static int CIM_SINT64       = 20;    // 0x14
    public static int CIM_UINT64       = 21;    // 0x15
    public static int CIM_REAL32       = 4;     // 0x4
    public static int CIM_REAL64       = 5;     // 0x5
    public static int CIM_BOOLEAN      = 11;    // 0xB
    public static int CIM_STRING       = 8;     // 0x8
    public static int CIM_DATETIME     = 101;   // 0x65
    public static int CIM_REFERENCE    = 102;   // 0x66
    public static int CIM_CHAR16       = 103;   // 0x67
    public static int CIM_OBJECT       = 13;    // 0xD
    public static int CIM_FLAG_ARRAY   = 8192;  // 0x2000

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

    @Override
    public String toString()
    {
        return getName() + "=" + value;
    }
}
