/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

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

    @Override
    public WMIClass getParentObject()
    {
        return wmiClass;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return wmiClass.getDataSource();
    }

    @Override
    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return element.getName();
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
