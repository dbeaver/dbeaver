/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.WMIConstants;
import org.jkiss.wmi.service.WMIObjectAttribute;

/**
 * Class property
 */
public class WMIClassAttribute extends WMIClassElement<WMIObjectAttribute> implements DBSEntityAttribute, IObjectImageProvider
{
    protected WMIClassAttribute(WMIClass wmiClass, WMIObjectAttribute attribute)
    {
        super(wmiClass, attribute);
    }

    @Property(name = "Type", viewable = true, order = 10)
    public String getTypeName()
    {
        return element.getTypeName();
    }

    public int getTypeID()
    {
        return element.getType();
    }

    @Override
    public int getScale()
    {
        return 0;
    }

    @Override
    public int getPrecision()
    {
        return 0;
    }

    @Override
    public long getMaxLength()
    {
        return 0;
    }

    @Override
    public boolean isRequired()
    {
        return false;
    }

    @Override
    public boolean isSequence()
    {
        return false;
    }

    @Property(name = "Default Value", viewable = true, order = 20)
    public String getDefaultValue()
    {
        return CommonUtils.toString(element.getValue());
    }

    public Image getObjectImage()
    {
        return getPropertyImage(element.getType());
    }

    public static Image getPropertyImage(int type)
    {
        switch (type) {
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
