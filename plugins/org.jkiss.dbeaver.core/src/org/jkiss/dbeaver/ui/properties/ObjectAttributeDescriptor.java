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

import org.jkiss.dbeaver.core.Log;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Abstract object attribute
 */
public abstract class ObjectAttributeDescriptor {

    static final Log log = Log.getLog(ObjectAttributeDescriptor.class);

    private final IPropertySource source;
    private ObjectPropertyGroupDescriptor parent;
    private int orderNumber;
    private String id;
    private Method getter;
    private boolean isLazy;
    private IPropertyCacheValidator cacheValidator;
    private Class<?> declaringClass;

    public ObjectAttributeDescriptor(
        IPropertySource source,
        ObjectPropertyGroupDescriptor parent,
        Method getter,
        String id,
        int orderNumber)
    {
        this.source = source;
        this.parent = parent;
        this.getter = getter;
        this.orderNumber = orderNumber;
        this.id = id;
        if (CommonUtils.isEmpty(this.id)) {
            this.id = BeanUtils.getPropertyNameFromGetter(getter.getName());
        }

        declaringClass = parent == null ? getter.getDeclaringClass() : parent.getDeclaringClass();
        if (this.getter.getParameterTypes().length == 1 && getter.getParameterTypes()[0] == DBRProgressMonitor.class) {
            this.isLazy = true;
        }

        if (isLazy) {
            final LazyProperty lazyInfo = getter.getAnnotation(LazyProperty.class);
            if (lazyInfo != null) {
                try {
                    cacheValidator = lazyInfo.cacheValidator().newInstance();
                } catch (Exception e) {
                    log.warn("Can't instantiate lazy cache validator '" + lazyInfo.cacheValidator().getName() + "'", e);
                }
            }
        }
    }

    public Class<?> getDeclaringClass()
    {
        return declaringClass;
    }

    public IPropertySource getSource()
    {
        return source;
    }

    public int getOrderNumber()
    {
        return orderNumber;
    }

    public String getId()
    {
        return id;
    }

    public Method getGetter()
    {
        return getter;
    }

    public boolean isLazy()
    {
        return isLazy;
    }

    public boolean isLazy(Object object, boolean checkParent)
    {
        if (isLazy && cacheValidator != null) {
            if (parent != null) {
                if (parent.isLazy(object, true)) {
                    return true;
                }
                try {
                    // Parent isn't lazy so use null progress monitor
                    object = parent.getGroupObject(object, null);
                } catch (Exception e) {
                    log.debug(e);
                    return true;
                }
            }
            return !cacheValidator.isPropertyCached(object, id);
        }
        return isLazy || (checkParent && parent != null && parent.isLazy(object, checkParent));
    }

    public IPropertyCacheValidator getCacheValidator()
    {
        return cacheValidator;
    }

    public ObjectPropertyGroupDescriptor getParent()
    {
        return parent;
    }

    public abstract String getCategory();

    public abstract String getDescription();

    public static List<ObjectPropertyDescriptor> extractAnnotations(
        IPropertySource source,
        Class<?> theClass,
        IFilter filter)
    {
        List<ObjectPropertyDescriptor> annoProps = new ArrayList<ObjectPropertyDescriptor>();
        extractAnnotations(source, null, theClass, annoProps, filter);
        return annoProps;
    }

    static void extractAnnotations(IPropertySource source, ObjectPropertyGroupDescriptor parent, Class<?> theClass, List<ObjectPropertyDescriptor> annoProps, IFilter filter)
    {
        Method[] methods = theClass.getMethods();
        for (Method method : methods) {
            final PropertyGroup propGroupInfo = method.getAnnotation(PropertyGroup.class);
            if (propGroupInfo != null && method.getReturnType() != null) {
                // Property group
                ObjectPropertyGroupDescriptor groupDescriptor = new ObjectPropertyGroupDescriptor(source, parent, method, propGroupInfo, filter);
                annoProps.addAll(groupDescriptor.getChildren());
            } else {
                final Property propInfo = method.getAnnotation(Property.class);
                if (propInfo == null || !BeanUtils.isGetterName(method.getName()) || method.getReturnType() == null) {
                    continue;
                }
                // Single property
                ObjectPropertyDescriptor desc = new ObjectPropertyDescriptor(source, parent, propInfo, method);
                if (filter != null && !filter.select(desc)) {
                    continue;
                }
                annoProps.add(desc);
            }
        }
        Collections.sort(annoProps, new Comparator<ObjectAttributeDescriptor>() {
            @Override
            public int compare(ObjectAttributeDescriptor o1, ObjectAttributeDescriptor o2)
            {
                return o1.getOrderNumber() - o2.getOrderNumber();
            }
        });
    }

}
