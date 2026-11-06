/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.cache.DBSStructCache;
import org.jkiss.dbeaver.model.struct.cache.SimpleObjectCache;

import java.sql.SQLException;
import java.util.*;

/**
 * JDBC structured objects cache.
 * Stores objects themselves and their child objects.
 */
public abstract class JDBCStructCache<OWNER extends DBSObject, OBJECT extends DBSObject, CHILD extends DBSObject>
    extends JDBCObjectCache<OWNER, OBJECT>
    implements DBSStructCache<OWNER, OBJECT, CHILD> {
    private static final Log log = Log.getLog(JDBCStructCache.class);

    private final Object objectNameColumn;
    private volatile boolean childrenCached = false;
    private final Map<OBJECT, SimpleObjectCache<OBJECT, CHILD>> childrenCache = new IdentityHashMap<>();

    @NotNull
    protected abstract JDBCStatement prepareChildrenStatement(
        @NotNull JDBCSession session,
        @NotNull OWNER owner,
        @Nullable OBJECT forObject
    ) throws SQLException;

    @NotNull
    protected JDBCStatement prepareChildrenStatement(
        @NotNull JDBCSession session,
        @NotNull OWNER owner,
        @Nullable OBJECT forObject,
        @Nullable List<OBJECT> batch
    ) throws SQLException {
        return prepareChildrenStatement(session, owner, forObject);
    }

    /**
     * Return null for the original, unrestricted bulk query. Subclasses may split a bulk load
     * into batches of cached parents without changing single-parent loading.
     */
    @NotNull
    protected List<List<OBJECT>> getBulkChildrenBatches(@NotNull OWNER owner) {
        return Collections.singletonList(null);
    }

    protected boolean isEmptyBulkChildrenResultComplete() {
        return false;
    }

    @NotNull
    protected JDBCSession openChildrenSession(@NotNull DBRProgressMonitor monitor, @NotNull OWNER owner) throws DBException {
        return DBUtils.openMetaSession(monitor, owner, "Load child objects");
    }

    @Nullable
    protected abstract CHILD fetchChild(
        @NotNull JDBCSession session,
        @NotNull OWNER owner,
        @NotNull OBJECT parent,
        @NotNull JDBCResultSet dbResult
    ) throws SQLException, DBException;

    protected JDBCStructCache(Object objectNameColumn) {
        this.objectNameColumn = objectNameColumn;
    }

    /**
     * Reads children objects from database
     *
     * @param monitor
     *            monitor
     * @param forObject
     *            object for which to read children. If null then reads children for all objects in this container.
     * @throws org.jkiss.dbeaver.DBException
     *             on error
     */
    public synchronized void loadChildren(DBRProgressMonitor monitor, OWNER owner, @Nullable final OBJECT forObject) throws DBException {
        if (monitor.isForceCacheUsage() || monitor.isCanceled() ||
            (forObject == null && this.childrenCached) ||
            (forObject != null && (!forObject.isPersisted() || isChildrenCached(forObject)))) {
            return;
        }
        if (forObject == null) {
            // If we have some child objects read before that - do not clear them.
            // We have to reuse them because there could be some references in cached model
            //clearChildrenCache(null);
            super.loadObjects(monitor, owner);
        }

        List<List<OBJECT>> batches = forObject == null ? getBulkChildrenBatches(owner) : Collections.singletonList(null);
        if (batches.isEmpty()) {
            // An empty list is complete only if loading the parents finished successfully.
            if (!monitor.isCanceled() && isFullyCached()) {
                this.childrenCached = true;
            }
            return;
        }

        DBPDataSource dataSource = owner.getDataSource();
        if (dataSource == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }
        try (JDBCSession session = openChildrenSession(monitor, owner)) {
            Map<OBJECT, List<CHILD>> objectMap = new HashMap<>();

            // Load columns
            for (List<OBJECT> batch : batches) {
                if (monitor.isCanceled()) {
                    return;
                }
                // Collect results from every batch before marking any parents as complete.
                try (JDBCStatement dbStat = prepareChildrenStatement(session, owner, forObject, batch)) {
                    dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                    dbStat.executeStatement();
                    try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                        if (dbResult == null) {
                            return;
                        }
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                return;
                            }
                            OBJECT object = forObject;
                            if (object == null) {
                                String objectName;
                                if (objectNameColumn instanceof Number) {
                                    objectName = JDBCUtils.safeGetString(dbResult, ((Number) objectNameColumn).intValue());
                                } else {
                                    objectName = JDBCUtils.safeGetStringTrimmed(dbResult, objectNameColumn.toString());
                                }
                                if (objectName == null) {
                                    log.debug("NULL object name in " + this);
                                    continue;
                                }

                                object = super.getCachedObject(objectName);
                                if (object == null) {
                                    log.debug("Object '" + objectName + "' not found in struct cache (" + getClass().getSimpleName() + ")");
                                    continue;
                                }
                            }
                            if (isChildrenCached(object)) {
                                // Already read
                                continue;
                            }
                            CHILD child = fetchChild(session, owner, object, dbResult);
                            if (child != null) {
                                // Add to map
                                objectMap.computeIfAbsent(object, k -> new ArrayList<>()).add(child);
                            }
                        }
                    }
                }
            }
            if (monitor.isCanceled()) {
                return;
            }
            // All children are read. Now assign them to parents
            for (Map.Entry<OBJECT, List<CHILD>> colEntry : objectMap.entrySet()) {
                if (!isChildrenCached(colEntry.getKey())) {
                    // isChildrenCached may return true if the same cache was read in other thread
                    cacheChildren(colEntry.getKey(), colEntry.getValue());
                }
            }
            if (forObject == null) {
                if (objectMap.isEmpty() && !isEmptyBulkChildrenResultComplete()) {
                    // Nothing was read. May be it means empty list of children
                    // but possibly this feature is not supported [JDBC: SQLite]
                } else {
                    // Now set empty column list for other tables
                    List<OBJECT> bulkParents = new ArrayList<>();
                    if (batches.getFirst() == null) {
                        bulkParents.addAll(getAllObjects(monitor, owner));
                    } else {
                        for (List<OBJECT> batch : batches) {
                            bulkParents.addAll(batch);
                        }
                    }
                    for (OBJECT tmpObject : bulkParents) {
                        if (!isChildrenCached(tmpObject) && !objectMap.containsKey(tmpObject)) {
                            cacheChildren(tmpObject, new ArrayList<>());
                        }
                    }
                    this.childrenCached = true;
                }
            } else if (!objectMap.containsKey(forObject)) {
                cacheChildren(forObject, new ArrayList<>());
            }
        } catch (SQLException ex) {
            throw new DBDatabaseException(ex, dataSource);
        }
    }

    @Override
    public void removeObject(@NotNull OBJECT object, boolean resetFullCache) {
        super.removeObject(object, resetFullCache);
        clearChildrenCache(object);
    }

    @Override
    public void clearCache() {
        this.clearChildrenCache(null);
        super.clearCache();
    }

    /**
     * Returns cache for child objects. Creates cache i it doesn't exists
     *
     * @param forObject parent object
     * @return cache
     */
    public DBSObjectCache<OBJECT, CHILD> getChildrenCache(@NotNull final OBJECT forObject) {
        synchronized (childrenCache) {
            SimpleObjectCache<OBJECT, CHILD> nestedCache = childrenCache.get(forObject);
            if (nestedCache == null) {
                // Create new empty children cache
                // This may happen only when invoked for newly created object (e.g. when we create new column
                // in a new created table)
                nestedCache = this.createNestedCache();
                nestedCache.setCache(new ArrayList<>());
                childrenCache.put(forObject, nestedCache);
            }
            return nestedCache;
        }
    }

    @Nullable
    public List<CHILD> getChildren(@NotNull DBRProgressMonitor monitor, @NotNull OWNER owner, final OBJECT forObject) throws DBException {
        if (!monitor.isForceCacheUsage()) {
            loadChildren(monitor, owner, forObject);
        }
        synchronized (childrenCache) {
            SimpleObjectCache<OBJECT, CHILD> nestedCache = childrenCache.get(forObject);
            return nestedCache == null ? null : nestedCache.getAllObjects(monitor, null);
        }
    }

    @Nullable
    public CHILD getChild(@Nullable DBRProgressMonitor monitor, @NotNull OWNER owner, final OBJECT forObject, @NotNull String objectName) throws DBException {
        if (monitor != null) {
            loadChildren(monitor, owner, forObject);
        }
        synchronized (childrenCache) {
            SimpleObjectCache<OBJECT, CHILD> nestedCache = childrenCache.get(forObject);
            return nestedCache == null ? null : nestedCache.getObject(monitor, forObject, objectName);
        }
    }

    public void clearChildrenCache(OBJECT forParent) {
        synchronized (childrenCache) {
            if (forParent != null) {
                this.childrenCache.remove(forParent);
            } else {
                this.childrenCache.clear();
            }
            childrenCached = false;
        }
    }

    protected boolean isChildrenCached(OBJECT parent) {
        synchronized (childrenCache) {
            return childrenCache.containsKey(parent);
//            SimpleObjectCache<OBJECT, CHILD> chCache = childrenCache.get(parent);
//            return chCache != null && !CommonUtils.isEmpty(chCache.getCachedObjects());
        }
    }

    protected void cacheChildren(OBJECT parent, List<CHILD> children) {
        synchronized (childrenCache) {
            SimpleObjectCache<OBJECT, CHILD> nestedCache = childrenCache.get(parent);
            if (nestedCache == null) {
                nestedCache = this.createNestedCache();
                childrenCache.put(parent, nestedCache);
            }
            nestedCache.setCache(children);
        }
    }

    @NotNull
    protected SimpleObjectCache<OBJECT, CHILD> createNestedCache() {
        SimpleObjectCache<OBJECT, CHILD> nestedCache = new SimpleObjectCache<>();
        nestedCache.setCaseSensitive(caseSensitive);
        return nestedCache;
    }

}
