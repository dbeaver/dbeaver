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
import org.jkiss.wmi.service.WMIObjectMethod;
import org.jkiss.wmi.service.WMIObjectProperty;

/**
 * Class property
 */
public class WMIClassMethod extends WMIClassAttribute<WMIObjectMethod>
{
    protected WMIClassMethod(WMIClass wmiClass, WMIObjectMethod attribute)
    {
        super(wmiClass, attribute);
    }

    public DBSTable getTable()
    {
        return wmiClass;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }

}
