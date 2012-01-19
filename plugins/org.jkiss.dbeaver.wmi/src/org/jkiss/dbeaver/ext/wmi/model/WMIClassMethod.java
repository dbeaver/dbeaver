package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.wmi.service.WMIObjectMethod;

/**
 * Class property
 */
public class WMIClassMethod extends WMIClassElement<WMIObjectMethod>
{
    protected WMIClassMethod(WMIClass wmiClass, WMIObjectMethod attribute)
    {
        super(wmiClass, attribute);
    }

    public DBSTable getTable()
    {
        return wmiClass;
    }

}
