/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

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

    public String getCategory()
    {
        return groupInfo.category();
    }

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
        if (getGetter().getParameterTypes().length > 0) {
            return getGetter().invoke(object, progressMonitor);
        } else {
            return getGetter().invoke(object);
        }
    }
}
