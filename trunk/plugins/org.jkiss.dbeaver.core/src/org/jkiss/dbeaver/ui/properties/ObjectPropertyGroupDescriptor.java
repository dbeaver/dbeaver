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
package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.ui.views.properties.IPropertySource;
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
        IPropertySource source,
        ObjectPropertyGroupDescriptor parent,
        Method getter,
        PropertyGroup groupInfo,
        IFilter filter)
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
