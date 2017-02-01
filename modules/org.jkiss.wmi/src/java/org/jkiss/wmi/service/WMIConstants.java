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

    // Condition flags
    public static final long WBEM_FLAG_ALWAYS	= 0;
	public static final long WBEM_FLAG_ONLY_IF_TRUE	= 0x1;
	public static final long WBEM_FLAG_ONLY_IF_FALSE	= 0x2;
	public static final long WBEM_FLAG_ONLY_IF_IDENTICAL	= 0x3;
	public static final long WBEM_MASK_PRIMARY_CONDITION	= 0x3;
	public static final long WBEM_FLAG_KEYS_ONLY	= 0x4;
	public static final long WBEM_FLAG_REFS_ONLY	= 0x8;
	public static final long WBEM_FLAG_LOCAL_ONLY	= 0x10;
	public static final long WBEM_FLAG_PROPAGATED_ONLY	= 0x20;
	public static final long WBEM_FLAG_SYSTEM_ONLY	= 0x30;
	public static final long WBEM_FLAG_NONSYSTEM_ONLY	= 0x40;
	public static final long WBEM_MASK_CONDITION_ORIGIN	= 0x70;
	public static final long WBEM_FLAG_CLASS_OVERRIDES_ONLY	= 0x100;
	public static final long WBEM_FLAG_CLASS_LOCAL_AND_OVERRIDES	= 0x200;
	public static final long WBEM_MASK_CLASS_CONDITION	= 0x300;

    // Flavor types
    public static final long WBEM_FLAVOR_DONT_PROPAGATE	= 0;
	public static final long WBEM_FLAVOR_FLAG_PROPAGATE_TO_INSTANCE	= 0x1;
	public static final long WBEM_FLAVOR_FLAG_PROPAGATE_TO_DERIVED_CLASS	= 0x2;
	public static final long WBEM_FLAVOR_MASK_PROPAGATION	= 0xf;
	public static final long WBEM_FLAVOR_OVERRIDABLE	= 0;
	public static final long WBEM_FLAVOR_NOT_OVERRIDABLE	= 0x10;
	public static final long WBEM_FLAVOR_MASK_PERMISSIONS	= 0x10;
	public static final long WBEM_FLAVOR_ORIGIN_LOCAL	= 0;
	public static final long WBEM_FLAVOR_ORIGIN_PROPAGATED	= 0x20;
	public static final long WBEM_FLAVOR_ORIGIN_SYSTEM	= 0x40;
	public static final long WBEM_FLAVOR_MASK_ORIGIN	= 0x60;
	public static final long WBEM_FLAVOR_NOT_AMENDED	= 0;
	public static final long WBEM_FLAVOR_AMENDED	= 0x80;
	public static final long WBEM_FLAVOR_MASK_AMENDED	= 0x80;

    // Class properties

    public static final String CLASS_PROP_CLASS_NAME = "__CLASS";
    public static final String CLASS_PROP_SUPER_CLASS = "__SUPERCLASS";
    public static final String CLASS_PROP_PATH = "__PATH";

    // Standard qualifiers

    public static final String Q_Abstract = "Abstract";
    public static final String Q_Aggregate = "Aggregate";
    public static final String Q_Aggregation = "Aggregation";
    public static final String Q_Association = "Association";
    public static final String Q_Alias = "Alias";
    public static final String Q_ArrayType = "ArrayType";
    public static final String Q_BitMap = "BitMap";
    public static final String Q_BitValues = "BitValues";
    public static final String Q_Constructor = "Constructor";
    public static final String Q_CreateBy = "CreateBy";
    public static final String Q_DeleteBy = "DeleteBy";
    public static final String Q_Description = "Description";
    public static final String Q_Destructor = "Destructor";
    public static final String Q_DisplayName = "DisplayName";
    public static final String Q_Gauge = "Gauge";
    public static final String Q_In = "In";
    public static final String Q_InOut = "In, Out";
    public static final String Q_Key = "Key";
    public static final String Q_Lazy = "Lazy";
    public static final String Q_MappingStrings = "MappingStrings";
    public static final String Q_Max = "Max";
    public static final String Q_MaxLen = "MaxLen";
    public static final String Q_MaxValue = "MaxValue";
    public static final String Q_Min = "Min";
    public static final String Q_MinValue = "MinValue";
    public static final String Q_ModelCorrespondence = "ModelCorrespondence";
    public static final String Q_Nonlocal = "Nonlocal";
    public static final String Q_NonlocalType = "NonlocalType";
    public static final String Q_NullValue = "NullValue";
    public static final String Q_Out = "Out";
    public static final String Q_Override = "Override";
    public static final String Q_Propagated = "Propagated";
    public static final String Q_Read = "Read";
    public static final String Q_Required = "Required";
    public static final String Q_Revision = "Revision";
    public static final String Q_Schema = "Schema";
    public static final String Q_Source = "Source";
    public static final String Q_SourceType = "SourceType";
    public static final String Q_SupportsCreate = "SupportsCreate";
    public static final String Q_SupportsDelete = "SupportsDelete";
    public static final String Q_SupportsUpdate = "SupportsUpdate";
    public static final String Q_Terminal = "Terminal";
    public static final String Q_Units = "Units";
    public static final String Q_ValueMap = "ValueMap";
    public static final String Q_Values = "Values";
    public static final String Q_Version = "Version";
    public static final String Q_Weak = "Weak";
    public static final String Q_Write = "Write";
    public static final String Q_WriteAtCreate = "WriteAtCreate";
    public static final String Q_WriteAtUpdate = "WriteAtUpdate";


    public static final String Q_CIMTYPE = "CIMTYPE";
    public static final String Q_CIM_Key = "CIM_Key";

}
