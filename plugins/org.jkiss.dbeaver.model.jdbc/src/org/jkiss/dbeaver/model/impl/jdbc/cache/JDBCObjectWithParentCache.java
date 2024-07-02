/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.AbstractObjectCache;
import org.jkiss.dbeaver.model.struct.cache.DBSCompositeCache;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;

/**
 *  This is basically cropped copy of JDBCCompositeCache, but without object children. Created specially for triggers.
 */

public abstract class JDBCObjectWithParentCache<OWNER extends DBSObject, PARENT extends DBSObject, OBJECT extends DBSObject> extends AbstractObjectCache<OWNER, OBJECT>
    implements DBSCompositeCache<PARENT, OBJECT> {
    protected static final Log log = Log.getLog(JDBCObjectWithParentCache.class);

    private final JDBCStructCache<OWNER, ?, ?> parentCache;
    private final Class<PARENT> parentType;
    private final Object parentColumnName;
    private final Object objectColumnName;

    private final Map<PARENT, List<OBJECT>> objectCache = new IdentityHashMap<>();

    protected JDBCObjectWithParentCache(
        JDBCStructCache<OWNER, ?, ?> parentCache,
        Class<PARENT> parentType,
        Object parentColumnName,
        Object objectColumnName) {
        this.parentCache = parentCache;
        this.parentType = parentType;
        this.parentColumnName = parentColumnName;
        this.objectColumnName = objectColumnName;
    }

    @NotNull
    abstract protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OWNER owner, @Nullable PARENT forParent)
        throws SQLException;

    @Nullable
    abstract protected OBJECT fetchObject(@NotNull JDBCSession session, @NotNull OWNER owner, @NotNull PARENT parent, String childName, @NotNull JDBCResultSet resultSet)
        throws SQLException, DBException;

    protected PARENT getParent(OBJECT object) {
        return (PARENT) object.getParentObject();
    }

    @NotNull
    @Override
    public List<OBJECT> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner)
        throws DBException {
        return getObjects(monitor, owner, null);
    }

    public List<OBJECT> getObjects(@NotNull DBRProgressMonitor monitor, OWNER owner, PARENT forParent)
        throws DBException {
        if (!monitor.isCanceled() && !monitor.isForceCacheUsage()) {
            loadObjects(monitor, owner, forParent);
        }
        return getCachedObjects(forParent);
    }

    @Override
    public List<OBJECT> getCachedObjects(@Nullable PARENT forParent) {
        if (forParent == null) {
            return getCachedObjects();
        } else {
            synchronized (objectCache) {
                return objectCache.get(forParent);
            }
        }
    }

    @Override
    public OBJECT getObject(@NotNull DBRProgressMonitor monitor, @NotNull OWNER owner, @NotNull String objectName)
        throws DBException {
        loadObjects(monitor, owner, null);

        return getCachedObject(objectName);
    }

    public OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, PARENT forParent, String objectName)
        throws DBException {
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
    public void cacheObject(@NotNull OBJECT object) {
        super.cacheObject(object);
        synchronized (objectCache) {
            PARENT parent = getParent(object);
            List<OBJECT> objects = objectCache.computeIfAbsent(parent, k -> new ArrayList<>());
            objects.add(object);
        }
    }

    @Override
    public void removeObject(@NotNull OBJECT object, boolean resetFullCache) {
        super.removeObject(object, resetFullCache);
        synchronized (objectCache) {
            PARENT parent = getParent(object);
            if (resetFullCache) {
                objectCache.remove(parent);
            } else {
                List<OBJECT> subCache = objectCache.get(parent);
                if (subCache != null) {
                    subCache.remove(object);
                }
            }
        }
    }

    @Override
    public void clearObjectCache(@NotNull PARENT forParent) {
        if (forParent == null) {
            super.clearCache();
            objectCache.clear();
        } else {
            List<OBJECT> removedObjects = objectCache.remove(forParent);
            if (removedObjects != null) {
                for (OBJECT obj : removedObjects) {
                    super.removeObject(obj, false);
                }
            }
        }
    }

    @Override
    public void clearCache() {
        synchronized (objectCache) {
            this.objectCache.clear();
        }
        super.clearCache();
    }

    @Override
    public void setCache(@NotNull List<OBJECT> objects) {
        super.setCache(objects);
        synchronized (objectCache) {
            objectCache.clear();
            for (OBJECT object : objects) {
                PARENT parent = getParent(object);
                List<OBJECT> parentObjects = objectCache.computeIfAbsent(parent, k -> new ArrayList<>());
                parentObjects.add(object);
            }
        }
    }

    protected void loadObjects(@NotNull DBRProgressMonitor monitor, OWNER owner, @Nullable PARENT forParent)
        throws DBException {
        synchronized (objectCache) {
            if (monitor.isForceCacheUsage() ||
                (forParent == null && isFullyCached()) ||
                (forParent != null && (!forParent.isPersisted() || objectCache.containsKey(forParent)))) {
                return;
            }
        }

        // Load parents first
        if (forParent == null) {
            parentCache.loadObjects(monitor, owner);
        }

        Map<PARENT, Map<String, OBJECT>> parentObjectMap = new LinkedHashMap<>();

        monitor.beginTask("Load parent and object cache", 1);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, owner, "Load parent and object objects")) {

            JDBCStatement dbStat = prepareObjectsStatement(session, owner, forParent);
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            try {
                dbStat.executeStatement();
                JDBCResultSet dbResult = dbStat.getResultSet();
                if (dbResult != null) try {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            return;
                        }
                        String parentName = forParent != null ?
                            forParent.getName() :
                            (parentColumnName instanceof Number ?
                                JDBCUtils.safeGetString(dbResult, ((Number) parentColumnName).intValue()) :
                                JDBCUtils.safeGetStringTrimmed(dbResult, parentColumnName.toString()));
                        String objectName = objectColumnName instanceof Number ?
                            JDBCUtils.safeGetString(dbResult, ((Number) objectColumnName).intValue()) :
                            JDBCUtils.safeGetStringTrimmed(dbResult, objectColumnName.toString());

                        if (forParent == null && CommonUtils.isEmpty(parentName)) {
                            // No parent - can't evaluate it
                            log.debug("Empty parent name in " + this);
                            continue;
                        }

                        PARENT parent = forParent;
                        if (parent == null) {
                            parent = parentCache.getObject(monitor, owner, parentName, parentType);
                            if (parent == null) {
                                log.debug("Parent object '" + parentName + "' not found");
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
                        Map<String, OBJECT> objectMap = parentObjectMap.computeIfAbsent(parent, k -> new TreeMap<>());

                        OBJECT objectInfo = objectMap.get(objectName);
                        if (objectInfo == null) {
                            OBJECT object = fetchObject(session, owner, parent, objectName, dbResult);
                            if (object == null || !isValidObject(monitor, owner, object)) {
                                // Can't fetch object
                                continue;
                            }
                            objectName = object.getName();
                            objectInfo = object;
                            objectMap.put(objectName, objectInfo);
                        }
                    }
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        } catch (SQLException ex) {
            if (ex instanceof SQLFeatureNotSupportedException) {
                log.debug("Error reading cache " + getClass().getSimpleName() + ", feature not supported: " + ex.getMessage());
            } else {
                DBPDataSource dataSource = owner.getDataSource();
                throw new DBDatabaseException(ex, dataSource);
            }
        } finally {
            monitor.done();
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
                        for (Map<String, OBJECT> objMap : parentObjectMap.values()) {
                            if (objMap != null) {
                                globalCache.addAll(objMap.values());
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
                for (Map.Entry<PARENT, Map<String, OBJECT>> colEntry : parentObjectMap.entrySet()) {
                    if (colEntry.getValue() == null || objectCache.containsKey(colEntry.getKey())) {
                        // Do not overwrite this object's cache
                        continue;
                    }
                    Collection<OBJECT> objectInfos = colEntry.getValue().values();
                    ArrayList<OBJECT> objects = new ArrayList<>(objectInfos.size());
                    objects.addAll(objectInfos);
                    objectCache.put(colEntry.getKey(), objects);
                }
                // Now set empty object list for other parents
                if (forParent == null) {
                    for (PARENT tmpParent : parentCache.getTypedObjects(monitor, owner, parentType)) {
                        if (!parentObjectMap.containsKey(tmpParent) && !objectCache.containsKey(tmpParent)) {
                            objectCache.put(tmpParent, new ArrayList<>());
                        }
                    }
                } else if (!parentObjectMap.containsKey(forParent) && !objectCache.containsKey(forParent)) {
                    objectCache.put(forParent, new ArrayList<>());
                }
            }
        }

    }
}
