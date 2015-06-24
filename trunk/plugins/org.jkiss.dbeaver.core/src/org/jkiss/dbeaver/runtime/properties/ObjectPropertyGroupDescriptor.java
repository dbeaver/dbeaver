/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.dbeaver.model.DBPPropertySource;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ObjectPropertyDescriptor
*/
public class ObjectPropertyGroupDescriptor extends ObjectAttributeDescriptor
{
    private PropertyGroup groupInfo;
    private List<ObjectPropertyDescriptor> children = new ArrayList<ObjectPropertyDescriptor>();

    public ObjectPropertyGroupDescriptor(
        DBPPropertySource source,
        ObjectPropertyGroupDescriptor parent,
        Method getter,
        PropertyGroup groupInfo,
        IPropertyFilter filter)
    {
        super(source, parent, getter, groupInfo.id(), groupInfo.order());
        this.groupInfo = groupInfo;
        extractAnnotations(source, this, getGetter().getReturnType(), children, filter);
    }

    @Override
    public String getCategory()
    {
        return groupInfo.category();
    }

    @Override
    public String getDescription()
    {
        return groupInfo.description();
    }

    public Collection<ObjectPropertyDescriptor> getChildren()
    {
        return children;
    }

    public Object getGroupObject(Object object, DBRProgressMonitor progressMonitor)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if (getParent() != null) {
            object = getParent().getGroupObject(object, progressMonitor);
        }
        if (isLazy(object, false)) {
            if (progressMonitor == null) {
                throw new IllegalAccessException("Can't read lazy properties with null progress monitor");
            }
        }
        if (isLazy()) {
            return getGetter().invoke(object, progressMonitor);
        } else {
            return getGetter().invoke(object);
        }
    }
}
