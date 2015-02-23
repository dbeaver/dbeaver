/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
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

    @NotNull
    @Override
    public WMIClass getParentObject()
    {
        return wmiClass;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return wmiClass.getDataSource();
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return element.getName();
    }

    @Nullable
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
