package org.jkiss.dbeaver.ext.wmi.model;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.WMIConstants;
import org.jkiss.wmi.service.WMIObjectProperty;

/**
 * Class property
 */
public class WMIClassProperty extends WMIClassAttribute<WMIObjectProperty> implements DBSTableColumn, IObjectImageProvider
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

    @Property(name = "Type", viewable = true, order = 10)
    public String getTypeName()
    {
        return attribute.getTypeName();
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

    @Property(name = "Default Value", viewable = true, order = 20)
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

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }

    public Image getObjectImage()
    {
        switch (attribute.getType()) {
            case WMIConstants.CIM_SINT8:
            case WMIConstants.CIM_UINT8:
            case WMIConstants.CIM_SINT16:
            case WMIConstants.CIM_UINT16:
            case WMIConstants.CIM_SINT32:
            case WMIConstants.CIM_UINT32:
            case WMIConstants.CIM_SINT64:
            case WMIConstants.CIM_UINT64:
            case WMIConstants.CIM_REAL32:
            case WMIConstants.CIM_REAL64:
                return DBIcon.TYPE_NUMBER.getImage();
            case WMIConstants.CIM_BOOLEAN:
                return DBIcon.TYPE_BOOLEAN.getImage();
            case WMIConstants.CIM_STRING:
            case WMIConstants.CIM_CHAR16:
                return DBIcon.TYPE_STRING.getImage();
            case WMIConstants.CIM_DATETIME:
                return DBIcon.TYPE_DATETIME.getImage();
            default:
                return DBIcon.TYPE_UNKNOWN.getImage();
        }
    }
}
