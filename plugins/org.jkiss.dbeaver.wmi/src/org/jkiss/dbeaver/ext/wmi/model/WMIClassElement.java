package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityElement;
import org.jkiss.wmi.service.WMIObjectElement;
import org.jkiss.wmi.service.WMIQualifiedObject;

/**
 * Class attribute
 */
public abstract class WMIClassElement<T extends WMIObjectElement> extends WMIPropertySource implements DBSEntityElement
{
    protected final WMIClass wmiClass;
    protected final T element;

    protected WMIClassElement(WMIClass wmiClass, T element)
    {
        this.wmiClass = wmiClass;
        this.element = element;
    }

    @Override
    protected WMIQualifiedObject getQualifiedObject()
    {
        return element;
    }

    public WMIClass getParentObject()
    {
        return wmiClass;
    }

    public DBPDataSource getDataSource()
    {
        return wmiClass.getDataSource();
    }

    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return element.getName();
    }

    public String getDescription()
    {
        return null;
    }

    public boolean isPersisted()
    {
        return true;
    }

}
