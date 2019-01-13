/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.BeanUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Various objects cache
 */
public abstract class AbstractObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject>
    implements DBSObjectCache<OWNER, OBJECT>
{
    private static final Log log = Log.getLog(AbstractObjectCache.class);

    private List<OBJECT> objectList;
    private Map<String, OBJECT> objectMap;
    protected volatile boolean fullCache = false;
    protected volatile boolean caseSensitive = true;
    protected Comparator<OBJECT> listOrderComparator;

    protected AbstractObjectCache() {
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }

    public Comparator<OBJECT> getListOrderComparator()
    {
        return listOrderComparator;
    }

    public void setListOrderComparator(Comparator<OBJECT> listOrderComparator)
    {
        this.listOrderComparator = listOrderComparator;
    }

    @NotNull
    @Override
    public List<OBJECT> getCachedObjects()
    {
        synchronized (this) {
            return objectList == null ? Collections.<OBJECT>emptyList() : objectList;
        }
    }

    public <SUB_TYPE> Collection<SUB_TYPE> getTypedObjects(DBRProgressMonitor monitor, OWNER owner, Class<SUB_TYPE> type)
        throws DBException
    {
        List<SUB_TYPE> result = new ArrayList<>();
        for (OBJECT object : getAllObjects(monitor, owner)) {
            if (type.isInstance(object)) {
                result.add(type.cast(object));
            }
        }
        return result;
    }

    @Nullable
    @Override
    public OBJECT getCachedObject(@NotNull String name)
    {
        synchronized (this) {
            return objectList == null || name == null ? null : getObjectMap().get(caseSensitive ? name : name.toUpperCase());
        }
    }

    @Override
    public void cacheObject(@NotNull OBJECT object)
    {
        synchronized (this) {
            if (this.objectList == null) {
                this.objectList = new ArrayList<>();
            }
            detectCaseSensitivity(object);
            this.objectList.add(object);
            if (this.objectMap != null) {
                String name = getObjectName(object);
                checkDuplicateName(name, object);
                this.objectMap.put(name, object);
            }
        }
    }

    @Override
    public void removeObject(@NotNull OBJECT object, boolean resetFullCache)
    {
        synchronized (this) {
            if (this.objectList != null) {
                detectCaseSensitivity(object);
                this.objectList.remove(object);
                if (this.objectMap != null) {
                    this.objectMap.remove(getObjectName(object));
                }
            }
            if (resetFullCache) {
                fullCache = false;
            }
        }
    }

    @Nullable
    public <SUB_TYPE> SUB_TYPE getObject(DBRProgressMonitor monitor, OWNER owner, String name, Class<SUB_TYPE> type)
        throws DBException
    {
        final OBJECT object = getObject(monitor, owner, name);
        return type.isInstance(object) ? type.cast(object) : null;
    }

    public boolean isFullyCached()
    {
        return this.fullCache;
    }

    @Override
    public void clearCache()
    {
        synchronized (this) {
            this.objectList = null;
            this.objectMap = null;
            this.fullCache = false;
        }
    }

    public void setCache(List<OBJECT> objects)
    {
        synchronized (this) {
            this.objectList = objects;
            this.objectMap = null;
            this.fullCache = true;
        }
    }

    /**
     * Merges new cache with existing.
     * If objects with the same name were already cached - leave them in cache
     * (because they might be referenced somewhere).
     */
    protected void mergeCache(List<OBJECT> objects)
    {
        synchronized (this) {
            if (this.objectList != null) {
                // Merge lists
                objects = new ArrayList<>(objects);
                for (int i = 0; i < objects.size(); i++) {
                    OBJECT newObject = objects.get(i);
                    String newObjectName = getObjectName(newObject);
                    for (int k = 0; k < objectList.size(); k++) {
                        OBJECT oldObject = objectList.get(k);
                        String oldObjectName = getObjectName(oldObject);
                        if (newObjectName.equals(oldObjectName)) {
                            objects.set(i, oldObject);
                            break;
                        }
                    }
                }
            }
        }
        setCache(objects);
    }

    private synchronized Map<String, OBJECT> getObjectMap()
    {
        if (this.objectMap == null) {
            this.objectMap = new HashMap<>();
            for (OBJECT object : objectList) {
                String name = getObjectName(object);
                checkDuplicateName(name, object);
                this.objectMap.put(name, object);
            }
        }
        return this.objectMap;
    }

    private void checkDuplicateName(String name, OBJECT object) {
        if (this.objectMap.containsKey(name)) {
            log.debug("Duplicate object name '" + name + "' in cache " + this.getClass().getSimpleName() + ". Last value: " + DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL));
        }
    }

    protected void detectCaseSensitivity(DBSObject object) {
        if (this.caseSensitive) {
            DBPDataSource dataSource = object.getDataSource();
            if (dataSource instanceof SQLDataSource &&
                ((SQLDataSource) dataSource).getSQLDialect().storesUnquotedCase() == DBPIdentifierCase.MIXED)
            {
                this.caseSensitive = false;
            }
        }
    }

    protected void invalidateObjects(DBRProgressMonitor monitor, OWNER owner, Iterator<OBJECT> objectIter)
    {

    }

    public void clearChildrenOf(DBSObject parent) {
        synchronized (this) {
            if (objectList == null) {
                return;
            }
            for (int i = 0; i < objectList.size(); ) {
                OBJECT object = objectList.get(i);
                if (object.getParentObject() == parent) {
                    this.objectList.remove(object);
                    if (this.objectMap != null) {
                        this.objectMap.remove(getObjectName(object));
                    }
                    fullCache = false;
                } else {
                    i++;
                }
            }
        }
    }

    @NotNull
    protected String getObjectName(@NotNull OBJECT object) {
        String name;
        if (object instanceof DBPUniqueObject) {
            name = ((DBPUniqueObject) object).getUniqueName();
        } else {
            name = object.getName();
        }
        if (name == null) {
            return null;
        }
        if (!caseSensitive) {
            return name.toUpperCase();
        }
        return name;
    }

    /**
     * Performs a deep copy of srcObject into dstObject.
     * Copies all fields (recursively) and clears all nested caches
     */
    protected void deepCopyCachedObject(@NotNull Object srcObject, @NotNull Object dstObject) {
        if (srcObject.getClass() != dstObject.getClass()) {
            log.error("Can't make object copy: src class " + srcObject.getClass().getName() + "' != dest class '" + dstObject.getClass().getName() + "'");
            return;
        }
        try {
            for (Class<?> theClass = srcObject.getClass(); theClass != Object.class; theClass = theClass.getSuperclass()) {
                final Field[] fields = theClass.getDeclaredFields();
                for (Field field : fields) {
                    final int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers)) {
                        continue;
                    }
                    field.setAccessible(true);
                    final Object srcValue = field.get(srcObject);
                    final Object dstValue = field.get(dstObject);
                    if (DBSObjectCache.class.isAssignableFrom(field.getType())) {
                        if (dstValue != null) {
                            ((DBSObjectCache) dstValue).clearCache();
                        }
                    } else {
                        if (isPropertyGroupField(field)) {
                            // This is a group of properties. Copy recursively
                            // Just in case check that values not null and have the same type
                            if (dstValue != null && srcValue != null && dstValue.getClass() == srcValue.getClass()) {
                                deepCopyCachedObject(srcValue, dstValue);
                            }
                        } else if (Modifier.isFinal(modifiers)) {
                            // Ignore final fields
                        } else {
                            // Just copy value
                            field.set(dstObject, srcValue);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.error("Error copying object state", e);
        }
    }

    protected class CacheIterator implements Iterator<OBJECT> {
        private Iterator<OBJECT> listIterator = objectList.iterator();
        private OBJECT curObject;
        public CacheIterator()
        {
        }

        @Override
        public boolean hasNext()
        {
            return listIterator.hasNext();
        }

        @Override
        public OBJECT next()
        {
            return (curObject = listIterator.next());
        }

        @Override
        public void remove()
        {
            listIterator.remove();
            if (objectMap != null) {
                objectMap.remove(getObjectName(curObject));
            }
        }
    }

    public static boolean isPropertyGroupField(Field field) {
        String getterName = "get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        for (Method getter : field.getDeclaringClass().getMethods()) {
            if (getter.getName().equals(getterName) &&
                isPropertyGetter(getter) &&
                getter.getAnnotation(PropertyGroup.class) != null)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isPropertyGetter(Method method) {
        if (BeanUtils.isGetterName(method.getName())) {
            return method.getParameterTypes().length == 0 ||
                (method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == DBRProgressMonitor.class);
        }
        return false;
    }

}
