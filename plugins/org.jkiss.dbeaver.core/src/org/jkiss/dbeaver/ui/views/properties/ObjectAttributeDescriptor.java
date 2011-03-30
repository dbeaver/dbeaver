/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.BeanUtils;
import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Abstract object attribute
 */
public abstract class ObjectAttributeDescriptor {

    static final Log log = LogFactory.getLog(ObjectAttributeDescriptor.class);

    private final IPropertySource source;
    private ObjectPropertyGroupDescriptor parent;
    private int orderNumber;
    private String id;
    private Method getter;
    private boolean isLazy;
    private IPropertyCacheValidator cacheValidator;

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
            return !cacheValidator.isPropertyCached(object);
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

    public static List<ObjectPropertyDescriptor> extractAnnotations(IPropertySource source)
    {
        return extractAnnotations(
            source,
            source.getEditableValue().getClass());
    }

    public static List<ObjectPropertyDescriptor> extractAnnotations(IPropertySource source, Class<?> theClass)
    {
        List<ObjectPropertyDescriptor> annoProps = new ArrayList<ObjectPropertyDescriptor>();
        extractAnnotations(source, null, theClass, annoProps);
        return annoProps;
    }

    static void extractAnnotations(IPropertySource source, ObjectPropertyGroupDescriptor parent, Class<?> theClass, List<ObjectPropertyDescriptor> annoProps)
    {
        Method[] methods = theClass.getMethods();
        for (Method method : methods) {
            final PropertyGroup propGroupInfo = method.getAnnotation(PropertyGroup.class);
            if (propGroupInfo != null && method.getReturnType() != null) {
                // Property group
                ObjectPropertyGroupDescriptor groupDescriptor = new ObjectPropertyGroupDescriptor(source, parent, method, propGroupInfo);
                annoProps.addAll(groupDescriptor.getChildren());
            } else {
                final Property propInfo = method.getAnnotation(Property.class);
                if (propInfo == null || !BeanUtils.isGetterName(method.getName()) || method.getReturnType() == null) {
                    continue;
                }
                // Single property
                ObjectPropertyDescriptor desc = new ObjectPropertyDescriptor(source, parent, propInfo, method);
                annoProps.add(desc);
            }
        }
        Collections.sort(annoProps, new Comparator<ObjectAttributeDescriptor>() {
            public int compare(ObjectAttributeDescriptor o1, ObjectAttributeDescriptor o2)
            {
                return o1.getOrderNumber() - o2.getOrderNumber();
            }
        });
    }

}
