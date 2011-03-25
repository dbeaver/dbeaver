/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * ObjectPropertyDescriptor
*/
public class ObjectPropertyGroupDescriptor extends ObjectAttributeDescriptor
{
    private PropertyGroup groupInfo;
    private List<ObjectPropertyDescriptor> children = new ArrayList<ObjectPropertyDescriptor>();

    public ObjectPropertyGroupDescriptor(ObjectPropertyGroupDescriptor parent, Method getter, PropertyGroup groupInfo)
    {
        super(parent, getter, groupInfo.id(), groupInfo.order());
        this.groupInfo = groupInfo;
        extractAnnotations(this, getGetter().getReturnType(), children);
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
