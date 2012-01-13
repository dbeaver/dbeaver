package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.WMIConstants;
import org.jkiss.wmi.service.WMIObjectProperty;

/**
 * Class property
 */
public class WMIClassProperty extends WMIClassAttribute<WMIObjectProperty> implements DBSTableColumn
{
    protected WMIClassProperty(WMIClass wmiClass, WMIObjectProperty attribute)
    {
        super(wmiClass, attribute);
    }

    public DBSTable getTable()
    {
        return wmiClass;
    }

    public int getOrdinalPosition()
    {
        return 0;
    }

    public String getDefaultValue()
    {
        return CommonUtils.toString(attribute.getValue());
    }

    public boolean isAutoIncrement()
    {
        return false;
    }

    public boolean isNotNull()
    {
        return false;
    }

    public long getMaxLength()
    {
        return 0;
    }

    public String getTypeName()
    {
        switch (attribute.getType()) {
            case WMIConstants.CIM_ILLEGAL: return "Illegal";
            case WMIConstants.CIM_EMPTY: return "Empty";
            case WMIConstants.CIM_SINT8: return "Int 8";
            case WMIConstants.CIM_UINT8: return "UInt 8";
            case WMIConstants.CIM_SINT16: return "Int 16";
            case WMIConstants.CIM_UINT16: return "UInt 16";
            case WMIConstants.CIM_SINT32: return "Int 32";
            case WMIConstants.CIM_UINT32: return "UInt 32";
            case WMIConstants.CIM_SINT64: return "Int 64";
            case WMIConstants.CIM_UINT64: return "UInt 64";
            case WMIConstants.CIM_REAL32: return "Real 32";
            case WMIConstants.CIM_REAL64: return "Real 64";
            case WMIConstants.CIM_BOOLEAN: return "Boolean";
            case WMIConstants.CIM_STRING: return "String";
            case WMIConstants.CIM_DATETIME: return "DateTime";
            case WMIConstants.CIM_REFERENCE: return "Reference";
            case WMIConstants.CIM_CHAR16: return "Char";
            case WMIConstants.CIM_OBJECT: return "Object";
            case WMIConstants.CIM_FLAG_ARRAY: return "Array";
            default: return "Unknown (" + attribute.getType() + ")";
        }
    }

    public int getValueType()
    {
        return attribute.getType();
    }

    public int getScale()
    {
        return 0;
    }

    public int getPrecision()
    {
        return 0;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }
}
