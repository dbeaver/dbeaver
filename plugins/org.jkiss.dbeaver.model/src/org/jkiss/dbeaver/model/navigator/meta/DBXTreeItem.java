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
package org.jkiss.dbeaver.model.navigator.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTermProvider;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * DBXTreeItem
 */
public class DBXTreeItem extends DBXTreeNode
{
    private String label;
    private String itemLabel;
    private String path;
    private String propertyName;
    private boolean optional;

    private Map<Class<?>, Method> propertyGettersCache = new IdentityHashMap<>();

    private static final Method NULL_GETTER;

    static {
        Method dummyMethod;
        try {
            dummyMethod = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException e) {
            dummyMethod = null;
        }
        NULL_GETTER = dummyMethod;
    }

    public DBXTreeItem(
        AbstractDescriptor source,
        DBXTreeNode parent,
        IConfigurationElement config,
        String path,
        String propertyName,
        boolean optional,
        boolean navigable,
        boolean inline,
        boolean virtual,
        boolean standalone,
        String visibleIf,
        String recursiveLink)
    {
        super(source, parent, config, navigable, inline, virtual, standalone, visibleIf, recursiveLink);
        this.label = parent == null ? ModelMessages.model_navigator_Connection : config.getAttribute("label");
        this.itemLabel = parent == null ? ModelMessages.model_navigator_Connection : config.getAttribute("itemLabel");
        if (itemLabel == null) {
            itemLabel = label;
        }
        this.path = path;
        this.propertyName = propertyName;
        this.optional = optional;
    }

    public DBXTreeItem(AbstractDescriptor source, DBXTreeNode parent, DBXTreeItem item) {
        super(source, parent, item);
        this.label = item.label;
        this.itemLabel = item.itemLabel;
        this.path = item.path;
        this.propertyName = item.propertyName;
        this.optional = item.optional;
    }

    public String getPath()
    {
        return path;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public boolean isOptional()
    {
        return optional;
    }

    @Override
    public String getChildrenType(@Nullable DBPDataSource dataSource, String locale)
    {
        final String term = getNodeTerm(dataSource, label, true);
        if (term != null) {
            return term;
        }
        return label;
    }

    @Override
    public String getNodeType(@Nullable DBPDataSource dataSource, @Nullable String locale)
    {
        final String term = getNodeTerm(dataSource, itemLabel, false);
        if (term != null) {
            return term;
        }
        return itemLabel;
    }

    private String getNodeTerm(@Nullable DBPDataSource dataSource, String termId, boolean multiple)
    {
        if (termId.startsWith("#") && dataSource instanceof DBPTermProvider) {
            final String term = ((DBPTermProvider) dataSource).getObjectTypeTerm(getPath(), termId.substring(1), multiple);
            if (term != null) {
                return term;
            }
        }
        return null;
    }

    public DBXTreeItem findChildItemByPath(String path) {
        for (DBXTreeNode node : getChildren()) {
            DBXTreeItem subItem = findChildItemByPath(node, path);
            if (subItem != null) {
                return subItem;
            }
        }
        return null;
    }

    private DBXTreeItem findChildItemByPath(DBXTreeNode node, String path) {
        if (node instanceof DBXTreeItem && CommonUtils.equalObjects(((DBXTreeItem) node).getPath(), path)) {
            return (DBXTreeItem) node;
        }
        if (node instanceof DBXTreeFolder) {
            for (DBXTreeNode subFolder : node.getChildren()) {
                DBXTreeItem subItem = findChildItemByPath(subFolder, path);
                if (subItem != null) {
                    return subItem;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Item " + label;
    }

    public synchronized Method getPropertyReadMethod(Class<?> objectClass) {
        Method getter = propertyGettersCache.get(objectClass);
        if (getter == null) {
            getter = findPropertyReadMethod(objectClass, propertyName);
            if (getter == null) {
                getter = NULL_GETTER;
            }
            propertyGettersCache.put(objectClass, getter);
        }
        return getter == NULL_GETTER ? null : getter;
    }

    private static Method findPropertyReadMethod(Class<?> clazz, String propertyName) {
        String methodName = BeanUtils.propertyNameToMethodName(propertyName);
        return findPropertyGetter(clazz, "get" + methodName, "is" + methodName);
    }

    private static Method findPropertyGetter(Class<?> clazz, String getName, String isName) {
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if ((!Modifier.isPublic(method.getModifiers())) ||
                (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) ||
                (method.getReturnType().equals(void.class)))
            {
                // skip
            } else if (method.getName().equals(getName) || (method.getName().equals(isName) && method.getReturnType().equals(boolean.class))) {
                // If it matches the get name, it's the right method
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0 || (parameterTypes.length == 1 && parameterTypes[0] == DBRProgressMonitor.class)) {
                    return method;
                }
            }
        }
        return clazz == Object.class ? null : findPropertyGetter(clazz.getSuperclass(), getName, isName);
    }

}
