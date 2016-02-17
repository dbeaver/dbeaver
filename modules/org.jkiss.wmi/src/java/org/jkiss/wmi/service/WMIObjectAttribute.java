/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.wmi.service;

/**
 * WMI object property
 */
public class WMIObjectAttribute extends WMIObjectElement
{

    private int type;
    private int flavor;
    private Object value;

    public WMIObjectAttribute(WMIObject owner, String name, int type, int flavor, Object value)
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

    public boolean isSystem()
    {
        return getName().startsWith("__");
    }

    @Override
    public String toString()
    {
        return getName() + "=" + value;
    }

    public String getTypeName()
    {
        int type = getType();
        if ((type & WMIConstants.CIM_FLAG_ARRAY) != 0) {
            return getCIMTypeName(type ^ WMIConstants.CIM_FLAG_ARRAY) + "[]";
        } else {
            return getCIMTypeName(type);
        }
    }

    public static String getCIMTypeName(int type)
    {
        switch (type) {
            case WMIConstants.CIM_ILLEGAL: return "Illegal";
            case WMIConstants.CIM_EMPTY: return "Empty";
            case WMIConstants.CIM_SINT8: return "Int8";
            case WMIConstants.CIM_UINT8: return "UInt8";
            case WMIConstants.CIM_SINT16: return "Int16";
            case WMIConstants.CIM_UINT16: return "UInt16";
            case WMIConstants.CIM_SINT32: return "Int32";
            case WMIConstants.CIM_UINT32: return "UInt32";
            case WMIConstants.CIM_SINT64: return "Int64";
            case WMIConstants.CIM_UINT64: return "UInt64";
            case WMIConstants.CIM_REAL32: return "Real32";
            case WMIConstants.CIM_REAL64: return "Real64";
            case WMIConstants.CIM_BOOLEAN: return "Boolean";
            case WMIConstants.CIM_STRING: return "String";
            case WMIConstants.CIM_DATETIME: return "DateTime";
            case WMIConstants.CIM_REFERENCE: return "Reference";
            case WMIConstants.CIM_CHAR16: return "Char";
            case WMIConstants.CIM_OBJECT: return "Object";
            default: return "Unknown (" + type + ")";
        }
    }
}
