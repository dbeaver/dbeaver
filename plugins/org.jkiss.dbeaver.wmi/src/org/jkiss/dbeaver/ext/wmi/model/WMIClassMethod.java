/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.struct.DBSEntityMethod;
import org.jkiss.wmi.service.WMIObjectMethod;

/**
 * Class property
 */
public class WMIClassMethod extends WMIClassElement<WMIObjectMethod> implements DBSEntityMethod
{
    protected WMIClassMethod(WMIClass wmiClass, WMIObjectMethod attribute)
    {
        super(wmiClass, attribute);
    }

}
