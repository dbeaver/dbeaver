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
package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.*;

/**
 * Composite objects cache.
 * Each composite object consists from several rows.
 * Each row object refers to some other DB objects.
 * Each composite object belongs to some parent object (table usually) and it's name is unique within it's parent.
 * Each row object name is unique within main object.
 *
 * Examples: table index, constraint.
 */
public abstract class JDBCCompositeCache<
    OWNER extends DBSObject,
    PARENT extends DBSObject,
    OBJECT extends DBSObject,
    ROW_REF extends DBSObject>
    extends AbstractObjectCache<OWNER, OBJECT>
{
    protected static final Log log = Log.getLog(JDBCCompositeCache.class);
    public static final String DEFAULT_OBJECT_NAME = "#DBOBJ";

    private final JDBCStructCache<OWNER,?,?> parentCache;
    private final Class<PARENT> parentType;
    private final Object parentColumnName;
    private final Object objectColumnName;

    private final Map<PARENT, List<OBJECT>> objectCache = new IdentityHashMap<>();

    protected JDBCCompositeCache(
        JDBCStructCache<OWNER,?,?> parentCache,
        Class<PARENT> parentType,
        Object parentColumnName,
        Object objectColumnName)
    {
        this.parentCache = parentCache;
        this.parentType = parentType;
        this.parentColumnName = parentColumnName;
        this.objectColumnName = objectColumnName;
    }

    @NotNull
    abstract protected JDBCStatement prepareObjectsStatement(JDBCSession session, OWNER owner, PARENT forParent)
        throws SQLException;

    @Nullable
    abstract protected OBJECT fetchObject(JDBCSession session, OWNER owner, PARENT parent, String childName, JDBCResultSet resultSet)
        throws SQLException, DBException;

    @Nullable
    abstract protected ROW_REF[] fetchObjectRow(JDBCSession session, PARENT parent, OBJECT forObject, JDBCResultSet resultSet)
        throws SQLException, DBException;

    protected PARENT getParent(OBJECT object)
    {
        return (PARENT) object.getParentObject();
    }

    abstract protected void cacheChildren(DBRProgressMonitor monitor, OBJECT object, List<ROW_REF> children);

    // Second cache function. Needed for complex entities which refers to each other (foreign keys)
    // First cache must cache all unique constraint, second must cache foreign keys references which refers unique keys
    protected void cacheChildren2(DBRProgressMonitor monitor, OBJECT object, List<ROW_REF> children) {

    }

    @NotNull
    @Override
    public Collection<OBJECT> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner)
        throws DBException
    {
        return getObjects(monitor, owner, null);
    }

    public Collection<OBJECT> getObjects(DBRProgressMonitor monitor, OWNER owner, PARENT forParent)
        throws DBException
    {
        loadObjects(monitor, owner, forParent);
        return getCachedObjects(forParent);
    }

    public <TYPE extends OBJECT> Collection<TYPE > getTypedObjects(DBRProgressMonitor monitor, OWNER owner, PARENT forParent, Class<TYPE> type)
        throws DBException
    {
        List<TYPE> result = new ArrayList<>();
        Collection<OBJECT> objects = getObjects(monitor, owner, forParent);
        if (objects != null) {
            for (OBJECT object : objects) {
                if (type.isInstance(object)) {
                    result.add(type.cast(object));
                }
            }
        }
        return result;
    }

    public Collection<OBJECT> getCachedObjects(PARENT forParent)
    {
        if (forParent == null) {
            return getCachedObjects();
        } else {
            synchronized (objectCache) {
                return objectCache.get(forParent);
            }
        }
    }

    @Override
    public OBJECT getObject(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner, @NotNull String objectName)
        throws DBException
    {
        loadObjects(monitor, owner, null);

        return getCachedObject(objectName);
    }

    public OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, PARENT forParent, String objectName)
        throws DBException
    {
        loadObjects(monitor, owner, forParent);
        if (forParent == null) {
            return getCachedObject(objectName);
        } else {
            synchronized (objectCache) {
                return DBUtils.findObject(objectCache.get(forParent), objectName);
            }
        }
    }

    @Override
    public void cacheObject(@NotNull OBJECT object)
    {
        super.cacheObject(object);
        synchronized (objectCache) {
            PARENT parent = getParent(object);
            List<OBJECT> objects = objectCache.get(parent);
            if (objects == null) {
                objects = new ArrayList<>();
                objectCache.put(parent, objects);
            }
            objects.add(object);
        }
    }

    @Override
    public void removeObject(@NotNull OBJECT object, boolean resetFullCache)
    {
        super.removeObject(object, resetFullCache);
        objectCache.remove(getParent(object));
    }

    public void clearObjectCache(PARENT forParent)
    {
        if (forParent == null) {
            super.clearCache();
        } else {
            objectCache.remove(forParent);
        }
    }

    public void setObjectCache(PARENT forParent, List<OBJECT> objects)
    {
    }

    @Override
    public void clearCache()
    {
        synchronized (objectCache) {
            this.objectCache.clear();
            super.clearCache();
        }
    }

    @Override
    public void setCache(List<OBJECT> objects) {
        super.setCache(objects);
        synchronized (objectCache) {
            objectCache.clear();
            for (OBJECT object : objects) {
                PARENT parent = getParent(object);
                List<OBJECT> parentObjects = objectCache.get(parent);
                if (parentObjects == null) {
                    parentObjects = new ArrayList<>();
                    objectCache.put(parent, parentObjects);
                }
                parentObjects.add(object);
            }
        }
    }

    private class ObjectInfo {
        final OBJECT object;
        final List<ROW_REF> rows = new ArrayList<>();
        public boolean broken;
        public boolean needsCaching;

        public ObjectInfo(OBJECT object)
        {
            this.object = object;
        }
    }

    protected synchronized void loadObjects(DBRProgressMonitor monitor, OWNER owner, PARENT forParent)
        throws DBException
    {
        synchronized (objectCache) {
            if ((forParent == null && isFullyCached()) ||
                (forParent != null && (!forParent.isPersisted() || objectCache.containsKey(forParent))))
            {
                return;
            }
        }

        // Load tables and columns first
        if (forParent == null) {
            parentCache.loadObjects(monitor, owner);
            parentCache.loadChildren(monitor, owner, null);
        }

        Map<PARENT, Map<String, ObjectInfo>> parentObjectMap = new LinkedHashMap<>();

        // Load index columns
        DBPDataSource dataSource = owner.getDataSource();
        assert (dataSource != null);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load composite objects")) {

            JDBCStatement dbStat = prepareObjectsStatement(session, owner, forParent);
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            try {
                dbStat.executeStatement();
                JDBCResultSet dbResult = dbStat.getResultSet();
                if (dbResult != null) try {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        String parentName = parentColumnName instanceof Number ?
                            JDBCUtils.safeGetString(dbResult, ((Number)parentColumnName).intValue()) :
                            JDBCUtils.safeGetString(dbResult, parentColumnName.toString());
                        String objectName = objectColumnName instanceof Number ?
                            JDBCUtils.safeGetString(dbResult, ((Number)objectColumnName).intValue()) :
                            JDBCUtils.safeGetString(dbResult, objectColumnName.toString());

                        if (CommonUtils.isEmpty(objectName)) {
                            // Use default name
                            objectName = getDefaultObjectName(dbResult, parentName);
                        }

                        if (forParent == null && CommonUtils.isEmpty(parentName)) {
                            // No parent - can't evaluate it
                            log.debug("Empty parent name in " + this);
                            continue;
                        }

                        PARENT parent = forParent;
                        if (parent == null) {
                            parent = parentCache.getObject(monitor, owner, parentName, parentType);
                            if (parent == null) {
                                log.debug("Object '" + objectName + "' owner '" + parentName + "' not found");
                                continue;
                            }
                        }
                        synchronized (objectCache) {
                            if (objectCache.containsKey(parent)) {
                                // Already cached
                                continue;
                            }
                        }
                        // Add to map
                        Map<String, ObjectInfo> objectMap = parentObjectMap.get(parent);
                        if (objectMap == null) {
                            objectMap = new TreeMap<>();
                            parentObjectMap.put(parent, objectMap);
                        }

                        ObjectInfo objectInfo = objectMap.get(objectName);
                        if (objectInfo == null) {
                            OBJECT object = fetchObject(session, owner, parent, objectName, dbResult);
                            if (object == null) {
                                // Can't fetch object
                                continue;
                            }
                            objectName = object.getName();
                            objectInfo = new ObjectInfo(object);
                            objectMap.put(objectName, objectInfo);
                        }
                        ROW_REF[] rowRef = fetchObjectRow(session, parent, objectInfo.object, dbResult);
                        if (rowRef == null || rowRef.length == 0) {
                            // At least one of rows is broken.
                            // So entire object is broken, let's just skip it.
                            objectInfo.broken = true;
                            //log.debug("Object '" + objectName + "' metadata corrupted - NULL child returned");
                            continue;
                        }
                        Collections.addAll(objectInfo.rows, rowRef);
                    }
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex, dataSource);
        }

        if (monitor.isCanceled()) {
            return;
        }

        // Fill global cache
        synchronized (this) {
            synchronized (objectCache) {
                if (forParent != null || !parentObjectMap.isEmpty()) {
                    if (forParent == null) {
                        // Cache global object list
                        List<OBJECT> globalCache = new ArrayList<>();
                        for (Map<String, ObjectInfo> objMap : parentObjectMap.values()) {
                            if (objMap != null) {
                                for (ObjectInfo info : objMap.values()) {
                                    if (!info.broken) {
                                        globalCache.add(info.object);
                                    }
                                }
                            }
                        }
                        // Save precached objects in global cache
                        for (List<OBJECT> objects : objectCache.values()) {
                            globalCache.addAll(objects);
                        }
                        // Add precached objects to global cache too
                        super.setCache(globalCache);
                        this.invalidateObjects(monitor, owner, new CacheIterator());
                    }
                }

                // Cache data in individual objects only if we have read something or have certain parent object
                // Otherwise we assume that this function is not supported for mass data reading

                // All objects are read. Now assign them to parents
                for (Map.Entry<PARENT, Map<String, ObjectInfo>> colEntry : parentObjectMap.entrySet()) {
                    if (colEntry.getValue() == null || objectCache.containsKey(colEntry.getKey())) {
                        // Do not overwrite this object's cache
                        continue;
                    }
                    Collection<ObjectInfo> objectInfos = colEntry.getValue().values();
                    ArrayList<OBJECT> objects = new ArrayList<>(objectInfos.size());
                    for (ObjectInfo objectInfo : objectInfos) {
                        objectInfo.needsCaching = true;
                        objects.add(objectInfo.object);
                    }
                    objectCache.put(colEntry.getKey(), objects);
                }
                // Now set empty object list for other parents
                if (forParent == null) {
                    for (PARENT tmpParent : parentCache.getTypedObjects(monitor, owner, parentType)) {
                        if (!parentObjectMap.containsKey(tmpParent) && !objectCache.containsKey(tmpParent)) {
                            objectCache.put(tmpParent, new ArrayList<OBJECT>());
                        }
                    }
                } else if (!parentObjectMap.containsKey(forParent) && !objectCache.containsKey(forParent)) {
                    objectCache.put(forParent, new ArrayList<OBJECT>());
                }
            }
            // Cache children lists (we do it in the end because children caching may operate with other model objects)
            for (Map.Entry<PARENT, Map<String, ObjectInfo>> colEntry : parentObjectMap.entrySet()) {
                for (ObjectInfo objectInfo : colEntry.getValue().values()) {
                    if (objectInfo.needsCaching) {
                        cacheChildren(monitor, objectInfo.object, objectInfo.rows);
                    }
                }
            }
            for (Map.Entry<PARENT, Map<String, ObjectInfo>> colEntry : parentObjectMap.entrySet()) {
                for (ObjectInfo objectInfo : colEntry.getValue().values()) {
                    if (objectInfo.needsCaching) {
                        cacheChildren2(monitor, objectInfo.object, objectInfo.rows);
                    }
                }
            }
        }

    }

    protected String getDefaultObjectName(JDBCResultSet dbResult, String parentName) {
        return parentName == null ? DEFAULT_OBJECT_NAME : parentName.toUpperCase() + "_" + DEFAULT_OBJECT_NAME;
    }

}
