/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
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
        IPropertyFilter filter,
        String locale)
    {
        super(source, parent, getter, groupInfo.id(), groupInfo.order());
        this.groupInfo = groupInfo;
        extractAnnotations(source, this, getGetter().getReturnType(), children, filter, locale);
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
