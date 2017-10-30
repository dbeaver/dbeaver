/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Abstract object attribute
 */
public abstract class ObjectAttributeDescriptor {

    static final Log log = Log.getLog(ObjectAttributeDescriptor.class);

    public static final Comparator<ObjectAttributeDescriptor> ATTRIBUTE_DESCRIPTOR_COMPARATOR = new Comparator<ObjectAttributeDescriptor>() {
        @Override
        public int compare(ObjectAttributeDescriptor o1, ObjectAttributeDescriptor o2) {
            return o1.getOrderNumber() - o2.getOrderNumber();
        }
    };

    private final DBPPropertySource source;
    private ObjectPropertyGroupDescriptor parent;
    private int orderNumber;
    private String id;
    private Method getter;
    private boolean isLazy;
    private IPropertyCacheValidator cacheValidator;
    private Class<?> declaringClass;

    public ObjectAttributeDescriptor(
        DBPPropertySource source,
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
        if (this.getter.getParameterTypes().length > 0 && getter.getParameterTypes()[0] == DBRProgressMonitor.class) {
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

    public DBPPropertySource getSource()
    {
        return source;
    }

    public int getOrderNumber()
    {
        return orderNumber;
    }

    @NotNull
    public String getId()
    {
        return id;
    }

    public Method getGetter()
    {
        return getter;
    }

    public boolean isNameProperty() {
        return id.equals(DBConstants.PROP_ID_NAME);
    }

    public boolean isRemote()
    {
        return isLazy || parent != null && parent.isRemote();
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
        DBPPropertySource source,
        Class<?> theClass,
        IPropertyFilter filter)
    {
        List<ObjectPropertyDescriptor> annoProps = new ArrayList<ObjectPropertyDescriptor>();
        extractAnnotations(source, null, theClass, annoProps, filter);
        return annoProps;
    }

    public static List<ObjectPropertyDescriptor> extractAnnotations(
        DBPPropertySource source,
        Collection<Class<?>> classList,
        IPropertyFilter filter)
    {
        List<ObjectPropertyDescriptor> annoProps = new ArrayList<>();
        for (Class<?> objectClass : classList) {
            annoProps.addAll(ObjectAttributeDescriptor.extractAnnotations(source, objectClass, filter));
        }
        Collections.sort(annoProps, ATTRIBUTE_DESCRIPTOR_COMPARATOR);
        return annoProps;
    }

    static void extractAnnotations(DBPPropertySource source, ObjectPropertyGroupDescriptor parent, Class<?> theClass, List<ObjectPropertyDescriptor> annoProps, IPropertyFilter filter)
    {
        Method[] methods = theClass.getMethods();
        Map<String, Method> passedNames = new HashMap<>();
        for (Method method : methods) {
            String methodFullName = method.getDeclaringClass().getName() + "." + method.getName();
            final Method prevMethod = passedNames.get(methodFullName);
            if (prevMethod != null) {
                // The same method but probably with another return type
                final Class<?> prevReturnType = prevMethod.getReturnType();
                final Class<?> newReturnType = method.getReturnType();
                if (newReturnType == null || prevReturnType == null || newReturnType == prevReturnType || !prevReturnType.isAssignableFrom(newReturnType)) {
                    continue;
                }
                // Let it another chance. New return types seems to be subclass of previous
            }
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
                if (prevMethod != null) {
                    // Remove previous anno
                    for (Iterator<ObjectPropertyDescriptor> iter = annoProps.iterator(); iter.hasNext(); ) {
                        if (iter.next().getId().equals(desc.getId())) {
                            iter.remove();
                        }
                    }
                }
                annoProps.add(desc);
                passedNames.put(methodFullName, method);
            }
        }
        Collections.sort(annoProps, ATTRIBUTE_DESCRIPTOR_COMPARATOR);
    }

}
