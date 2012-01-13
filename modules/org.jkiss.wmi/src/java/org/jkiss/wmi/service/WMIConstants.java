/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.wmi.service;

/**
 * WMI constants
 */
public class WMIConstants {
    
    public static final int CIM_ILLEGAL      = 4095;  // 0xFFF
    public static final int CIM_EMPTY        = 0;     // 0x0
    public static final int CIM_SINT8        = 16;    // 0x10
    public static final int CIM_UINT8        = 17;    // 0x11
    public static final int CIM_SINT16       = 2;     // 0x2
    public static final int CIM_UINT16       = 18;    // 0x12
    public static final int CIM_SINT32       = 3;     // 0x3
    public static final int CIM_UINT32       = 19;    // 0x13
    public static final int CIM_SINT64       = 20;    // 0x14
    public static final int CIM_UINT64       = 21;    // 0x15
    public static final int CIM_REAL32       = 4;     // 0x4
    public static final int CIM_REAL64       = 5;     // 0x5
    public static final int CIM_BOOLEAN      = 11;    // 0xB
    public static final int CIM_STRING       = 8;     // 0x8
    public static final int CIM_DATETIME     = 101;   // 0x65
    public static final int CIM_REFERENCE    = 102;   // 0x66
    public static final int CIM_CHAR16       = 103;   // 0x67
    public static final int CIM_OBJECT       = 13;    // 0xD
    public static final int CIM_FLAG_ARRAY   = 8192;  // 0x2000

    public static final long WBEM_FLAG_RETURN_IMMEDIATELY	= 0x10;
	public static final long WBEM_FLAG_RETURN_WBEM_COMPLETE	= 0;
	public static final long WBEM_FLAG_BIDIRECTIONAL	= 0;
	public static final long WBEM_FLAG_FORWARD_ONLY	= 0x20;
	public static final long WBEM_FLAG_NO_ERROR_OBJECT	= 0x40;
	public static final long WBEM_FLAG_RETURN_ERROR_OBJECT	= 0;
	public static final long WBEM_FLAG_SEND_STATUS	= 0x80;
	public static final long WBEM_FLAG_DONT_SEND_STATUS	= 0;
	public static final long WBEM_FLAG_ENSURE_LOCATABLE	= 0x100;
	public static final long WBEM_FLAG_DIRECT_READ	= 0x200;
	public static final long WBEM_FLAG_SEND_ONLY_SELECTED	= 0;
	public static final long WBEM_RETURN_WHEN_COMPLETE	= 0;
	public static final long WBEM_RETURN_IMMEDIATELY	= 0x10;
	public static final long WBEM_MASK_RESERVED_FLAGS	= 0x1f000;
	public static final long WBEM_FLAG_USE_AMENDED_QUALIFIERS	= 0x20000;
	public static final long WBEM_FLAG_STRONG_VALIDATION	= 0x100000;


	public static final long WBEM_FLAG_DEEP	= 0;
	public static final long WBEM_FLAG_SHALLOW	= 1;
	public static final long WBEM_FLAG_PROTOTYPE	= 2;

    public static final String CLASS_PROP_CLASS_NAME = "__CLASS";
    public static final String CLASS_PROP_SUPER_CLASS = "__SUPERCLASS";
    public static final String CLASS_PROP_PATH = "__PATH";
}
