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

    private static final String SNAPSHOT_CHILDREN = "children";

    private final Object objectNameColumn;
    private volatile boolean childrenCached = false;
    private volatile boolean childrenRestoreAllowed = true;
    // Objects whose children came from the metadata snapshot: they are refreshed in background
    private final Set<String> restoredChildren = new HashSet<>();
    private final Map<OBJECT, SimpleObjectCache<OBJECT, CHILD>> childrenCache = new IdentityHashMap<>();

    @NotNull
    protected abstract JDBCStatement prepareChildrenStatement(
        @NotNull JDBCSession session,
        @NotNull OWNER owner,
        @Nullable OBJECT forObject
    ) throws SQLException;

    @Nullable
    protected abstract CHILD fetchChild(
        @NotNull JDBCSession session,
        @NotNull OWNER owner,
        @NotNull OBJECT parent,
        @NotNull JDBCResultSet dbResult
    ) throws SQLException, DBException;

    protected JDBCStructCache(@NotNull Object objectNameColumn) {
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

        JDBCMetadataSnapshot snapshot = getMetadataSnapshot(owner);
        if (snapshot != null && childrenRestoreAllowed && restoreChildren(monitor, owner, forObject, snapshot)) {
            scheduleSnapshotRefresh(owner, snapshot);
            return;
        }
        JDBCMetadataSnapshot.Pass pass = snapshot == null ? null : snapshot.beginPass(SNAPSHOT_CHILDREN, forObject == null);
        Map<OBJECT, List<CHILD>> objectMap = readChildren(monitor, owner, forObject, pass, true);
        if (objectMap == null) {
            return;
        }

        // All children are read. Now assign them to parents
        for (Map.Entry<OBJECT, List<CHILD>> colEntry : objectMap.entrySet()) {
            if (!isChildrenCached(colEntry.getKey())) {
                // isChildrenCached may return true if the same cache was read in other thread
                // just skip
                cacheChildren(colEntry.getKey(), colEntry.getValue());
            }
        }
        if (forObject == null) {
            if (objectMap.isEmpty()) {
                // Nothing was read. May be it means empty list of children
                // but possibly this feature is not supported [JDBC: SQLite]
                return;
            } else {
                // Now set empty column list for other tables
                for (OBJECT tmpObject : getAllObjects(monitor, owner)) {
                    if (!isChildrenCached(tmpObject) && !objectMap.containsKey(tmpObject)) {
                        cacheChildren(tmpObject, new ArrayList<>());
                    }
                }
                this.childrenCached = true;
            }
        } else if (!objectMap.containsKey(forObject)) {
            cacheChildren(forObject, new ArrayList<>());
        }
        if (pass != null) {
            pass.commit();
        }
    }

    /**
     * Reads children from the database, without changing the cache.
     *
     * @param pass records the rows read in the metadata snapshot, if any
     * @param skipCached skip children of objects which are already cached
     * @return children by parent, or null if the read was canceled
     */
    @Nullable
    private Map<OBJECT, List<CHILD>> readChildren(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @Nullable OBJECT forObject,
        @Nullable JDBCMetadataSnapshot.Pass pass,
        boolean skipCached
    ) throws DBException {
        DBPDataSource dataSource = owner.getDataSource();
        if (dataSource == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }
        Map<OBJECT, List<CHILD>> objectMap = new HashMap<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, owner, "Load child objects")) {
            // Load columns
            try (JDBCStatement dbStat = prepareChildrenStatement(session, owner, forObject)) {
                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                dbStat.executeStatement();
                JDBCResultSet dbResult = dbStat.getResultSet();
                if (dbResult != null) {
                    try {
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                return null;
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
                            // Recorded even if already cached: the snapshot keeps all rows of the read.
                            // Rows which can be read only once are copied, and the child is built from the copy.
                            Object[] row = null;
                            if (pass != null) {
                                if (isMetadataRowReadOnce()) {
                                    row = pass.capture(dbResult);
                                    if (row != null) {
                                        pass.add(object.getName(), row);
                                    }
                                } else {
                                    pass.record(object.getName(), dbResult);
                                }
                            }
                            if (skipCached && isChildrenCached(object)) {
                                // Already read
                                continue;
                            }
                            final OBJECT parent = object;
                            CHILD child = row != null ?
                                pass.fetchCopy(session, row, rs -> fetchChild(session, owner, parent, rs)) :
                                fetchChild(session, owner, object, dbResult);
                            if (child == null) {
                                continue;
                            }

                            // Add to map
                            List<CHILD> children = objectMap.computeIfAbsent(object, k -> new ArrayList<>());
                            children.add(child);
                        }

                        if (monitor.isCanceled()) {
                            return null;
                        }
                    } finally {
                        dbResult.close();
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DBDatabaseException(ex, dataSource);
        }
        if (pass != null) {
            // Objects without children
            if (forObject != null) {
                pass.recordEmpty(forObject.getName());
            } else {
                for (OBJECT object : getCachedObjects()) {
                    pass.recordEmpty(object.getName());
                }
            }
        }
        return objectMap;
    }

    /**
     * Restores children of one object (or of all objects) from the snapshot.
     *
     * @return false if they are not stored or cannot be restored
     */
    private boolean restoreChildren(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @Nullable OBJECT forObject,
        @NotNull JDBCMetadataSnapshot snapshot
    ) {
        if (forObject != null ? !snapshot.hasGroup(SNAPSHOT_CHILDREN, forObject.getName()) : !snapshot.isComplete(SNAPSHOT_CHILDREN)) {
            return false;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, owner, "Restore metadata of '" + owner.getName() + "'")) {
            if (forObject != null) {
                List<CHILD> children = snapshot.restore(session, SNAPSHOT_CHILDREN, forObject.getName(),
                    rs -> fetchChild(session, owner, forObject, rs));
                if (children == null) {
                    return false;
                }
                cacheChildren(forObject, children);
                restoredChildren.add(forObject.getName());
                return true;
            }
            List<OBJECT> objects = getCachedObjects();
            for (OBJECT object : objects) {
                if (!isChildrenCached(object) && !snapshot.hasGroup(SNAPSHOT_CHILDREN, object.getName())) {
                    // Object created after the snapshot was taken: read all children from the database
                    return false;
                }
            }
            Map<String, List<CHILD>> restored = snapshot.restoreGroups(session, SNAPSHOT_CHILDREN, (group, rs) -> {
                OBJECT parent = getCachedObject(group);
                return parent == null ? null : fetchChild(session, owner, parent, rs);
            });
            if (restored == null) {
                return false;
            }
            for (OBJECT object : objects) {
                List<CHILD> children = restored.get(object.getName());
                if (!isChildrenCached(object) && children != null) {
                    cacheChildren(object, children);
                    restoredChildren.add(object.getName());
                }
            }
            this.childrenCached = true;
            log.debug("Restored children of " + restored.size() + " objects of " + getCacheName() + " from metadata snapshot");
            return true;
        } catch (Exception e) {
            log.debug("Metadata snapshot of " + getCacheName() + " children not restored, reading from database: " + e.getMessage());
            snapshot.clear(SNAPSHOT_CHILDREN);
            return false;
        }
    }

    @Override
    protected boolean isSnapshotRefreshPending() {
        synchronized (this) {
            return super.isSnapshotRefreshPending() || !restoredChildren.isEmpty();
        }
    }

    @Override
    protected void refreshRestoredChildren(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @NotNull JDBCMetadataSnapshot snapshot,
        @Nullable Set<String> changed
    ) throws DBException {
        Set<String> restored;
        synchronized (this) {
            restored = new LinkedHashSet<>(restoredChildren);
            restoredChildren.clear();
        }
        if (changed != null) {
            // Children of unchanged objects are up to date
            restored.retainAll(changed);
        }
        if (restored.isEmpty() || monitor.isCanceled()) {
            return;
        }
        // Children of all objects are read in one query, if all of them are stored and the changed objects
        // are not known. Otherwise, object by object.
        boolean allObjects = changed == null && snapshot.isComplete(SNAPSHOT_CHILDREN);
        JDBCMetadataSnapshot.Pass pass = snapshot.beginPass(SNAPSHOT_CHILDREN, allObjects);
        Map<String, List<CHILD>> children = new HashMap<>();
        if (allObjects) {
            Map<OBJECT, List<CHILD>> objectMap = readChildren(monitor, owner, null, pass, false);
            if (objectMap == null) {
                return;
            }
            objectMap.forEach((object, list) -> children.put(object.getName(), list));
        } else {
            for (String name : restored) {
                OBJECT object = getCachedObject(name);
                if (object == null) {
                    continue;
                }
                Map<OBJECT, List<CHILD>> objectMap = readChildren(monitor, owner, object, pass, false);
                if (objectMap == null) {
                    return;
                }
                children.put(name, objectMap.getOrDefault(object, new ArrayList<>()));
            }
        }
        List<CHILD> added = new ArrayList<>();
        List<CHILD> updated = new ArrayList<>();
        List<CHILD> removed = new ArrayList<>();
        int changedObjects = 0;
        synchronized (this) {
            for (String name : pass.getChangedGroups()) {
                OBJECT object = restored.contains(name) ? getCachedObject(name) : null;
                if (object != null && isChildrenCached(object)) {
                    mergeChildren(object, children.getOrDefault(name, new ArrayList<>()), added, updated, removed);
                    changedObjects++;
                }
            }
            pass.commit();
        }
        removed.forEach(DBUtils::fireObjectRemove);
        added.forEach(child -> DBUtils.fireObjectAdd(child, Collections.emptyMap()));
        updated.forEach(DBUtils::fireObjectUpdate);
        if (changedObjects > 0) {
            log.debug("Children of " + changedObjects + " objects of " + getCacheName() + " refreshed");
        }
    }

    /**
     * Replaces cached children of an object with children read again. Children with the same name keep their
     * cached instance (which may be referenced by navigator nodes and editors), with the new content copied into it.
     */
    private void mergeChildren(
        @NotNull OBJECT object,
        @NotNull List<CHILD> newChildren,
        @NotNull List<CHILD> added,
        @NotNull List<CHILD> updated,
        @NotNull List<CHILD> removed
    ) {
        Map<String, CHILD> oldChildren = new LinkedHashMap<>();
        for (CHILD child : getChildrenCache(object).getCachedObjects()) {
            oldChildren.put(child.getName(), child);
        }
        List<CHILD> merged = new ArrayList<>(newChildren.size());
        for (CHILD child : newChildren) {
            CHILD oldChild = oldChildren.remove(child.getName());
            if (oldChild != null && oldChild.getClass() == child.getClass()) {
                deepCopyCachedObject(child, oldChild);
                merged.add(oldChild);
                updated.add(oldChild);
            } else {
                if (oldChild != null) {
                    removed.add(oldChild);
                }
                merged.add(child);
                added.add(child);
            }
        }
        removed.addAll(oldChildren.values());
        cacheChildren(object, merged);
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

    public void clearChildrenCache(@Nullable OBJECT forParent) {
        // Explicit refresh: children are read from the database
        childrenRestoreAllowed = false;
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