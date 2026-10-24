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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.AbstractObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Various objects cache.
 * Simple cache which may read objects from database and keep them.
 */
public abstract class JDBCObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject> extends AbstractObjectCache<OWNER, OBJECT>
{
    private static final int DEFAULT_MAX_CACHE_SIZE = 1000000;

    private static final Log log = Log.getLog(JDBCObjectCache.class);

    /**
     * Snapshot set of the objects of the cache (one group per object).
     */
    protected static final String SNAPSHOT_OBJECTS = "objects";
    // Restored objects are refreshed after the first user actions: the refresh shares the metadata connection
    private static final long SNAPSHOT_REFRESH_DELAY_MS = 5000;

    // Maximum number of objects in cache
    private int maximumCacheSize = DEFAULT_MAX_CACHE_SIZE;

    private final Object snapshotSync = new Object();
    private JDBCMetadataSnapshot metadataSnapshot;
    private volatile boolean snapshotRestoreAllowed = true;
    // Objects were restored from the snapshot and not refreshed yet
    private volatile boolean objectsRefreshPending;
    private boolean snapshotRefreshScheduled;

    protected JDBCObjectCache() {
    }

    public void setMaximumCacheSize(int maximumCacheSize) {
        this.maximumCacheSize = maximumCacheSize;
    }

    @NotNull
    abstract protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OWNER owner)
        throws SQLException;

    @Nullable
    abstract protected OBJECT fetchObject(@NotNull JDBCSession session, @NotNull OWNER owner, @NotNull JDBCResultSet resultSet)
        throws SQLException, DBException;

    @NotNull
    @Override
    public List<OBJECT> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner)
        throws DBException
    {
        if (!isFullyCached() && !monitor.isForceCacheUsage()) {
            loadObjects(monitor, owner);
        }
        return getCachedObjects();
    }

    @Override
    public OBJECT getObject(@NotNull DBRProgressMonitor monitor, @NotNull OWNER owner, @NotNull String name)
        throws DBException
    {
        if (!isFullyCached() && !monitor.isForceCacheUsage()) {
            this.loadObjects(monitor, owner);
        }
        return getCachedObject(name);
    }

    protected synchronized void loadObjects(DBRProgressMonitor monitor, OWNER owner)
        throws DBException
    {
        if (isFullyCached() || monitor.isForceCacheUsage() || monitor.isCanceled()) {
            return;
        }
        if (DBWorkbench.getPlatform().isUnitTestMode()) {
            log.debug("[TEST] Skip cache read in test mode");
            return;
        }

        DBPDataSource dataSource = owner.getDataSource();
        if (dataSource == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }
        JDBCMetadataSnapshot snapshot = getMetadataSnapshot(owner);
        if (snapshot != null && snapshotRestoreAllowed) {
            // Only the first load of the session: after an explicit refresh objects are read from the database
            snapshotRestoreAllowed = false;
            List<OBJECT> restored = restoreObjects(monitor, owner, snapshot);
            if (restored != null) {
                finishLoading(monitor, owner, restored);
                objectsRefreshPending = true;
                scheduleSnapshotRefresh(owner, snapshot);
                return;
            }
        }

        List<OBJECT> tmpObjectList = new ArrayList<>();
        if (owner.isPersisted()) {
            // Load cache from database only for persisted objects
            try {
                JDBCMetadataSnapshot.Pass pass = snapshot == null ? null : snapshot.beginPass(SNAPSHOT_OBJECTS, true);
                if (!readObjects(monitor, owner, tmpObjectList, pass)) {
                    return;
                }
                if (pass != null) {
                    pass.commit();
                }
            } catch (Exception e) {
                if (!handleCacheReadError(e)) {
                    throw e;
                }
            }
        }
        finishLoading(monitor, owner, tmpObjectList);
    }

    /**
     * Reads objects from the database, without changing the cache.
     *
     * @param pass records the rows read in the metadata snapshot, if any
     * @return false if the read was canceled
     */
    private boolean readObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @NotNull List<OBJECT> objects,
        @Nullable JDBCMetadataSnapshot.Pass pass
    ) throws DBException {
        DBPDataSource dataSource = owner.getDataSource();
        String task = "Load objects from " + DBUtils.getObjectTypeName(owner) + " '" + owner.getName() + "'";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, owner, task)) {
            beforeCacheLoading(session, owner);
            try (JDBCStatement dbStat = prepareObjectsStatement(session, owner)) {
                monitor.subTask("Load " + getCacheName());
                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                dbStat.executeStatement();
                try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                    if (dbResult != null) {
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                return false;
                            }

                            // Rows which can be read only once are copied first, and the object is built from the copy
                            Object[] row = pass != null && isMetadataRowReadOnce() ? pass.capture(dbResult) : null;
                            OBJECT object = row != null ?
                                pass.fetchCopy(session, row, rs -> fetchObject(session, owner, rs)) :
                                fetchObject(session, owner, dbResult);
                            if (object == null || !isValidObject(monitor, owner, object)) {
                                continue;
                            }
                            objects.add(object);
                            if (row != null) {
                                pass.add(object.getName(), row);
                            } else if (pass != null && !isMetadataRowReadOnce()) {
                                pass.record(object.getName(), dbResult);
                            }

                            // Do not log every object load. This overheats UI in case of long lists
                            //monitor.subTask(object.getName());
                            if (objects.size() == maximumCacheSize) {
                                log.warn("Maximum cache size exceeded (" + maximumCacheSize + ") in " + this);
                                break;
                            }
                        }
                    }
                }
            } finally {
                afterCacheLoading(session, owner);
            }
        } catch (SQLException ex) {
            throw new DBDatabaseException(ex, dataSource);
        } catch (DBException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DBException("Internal driver error", ex);
        }
        return true;
    }

    private void finishLoading(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @NotNull List<OBJECT> objects
    ) throws DBException {
        addCustomObjects(monitor, owner, objects);

        Comparator<OBJECT> comparator = getListOrderComparator();
        if (comparator != null && !CommonUtils.isEmpty(objects)) {
            objects.sort(comparator);
        }

        detectCaseSensitivity(owner);
        mergeCache(objects);
        this.invalidateObjects(monitor, owner, new CacheIterator());
    }

    @Override
    public void clearCache() {
        super.clearCache();
        // Explicit refresh: objects are read from the database (and stored in the snapshot again)
        snapshotRestoreAllowed = false;
    }

    /**
     * Whether rows of this cache may be kept in a metadata snapshot between sessions (if the snapshot is enabled
     * for the connection). Only for caches whose objects are built from the rows of the cache query alone:
     * restored objects are built by {@link #fetchObject} from stored rows.
     */
    protected boolean supportsMetadataSnapshot() {
        return false;
    }

    /**
     * Whether a row of the cache queries can be read only once (e.g. Oracle LONG columns). Then rows stored
     * in the snapshot are copied before the object is built, and the object is built from the copy.
     */
    protected boolean isMetadataRowReadOnce() {
        return false;
    }

    /**
     * Reads names of the objects which changed since they were stored in the snapshot, for drivers which can tell
     * it cheaply (e.g. by DDL time). Includes new and dropped objects.
     *
     * @return changed objects, or null if the driver cannot tell: then all restored metadata is read again
     */
    @Nullable
    protected Set<String> readChangedObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @NotNull JDBCMetadataSnapshot snapshot
    ) throws DBException {
        return null;
    }

    /**
     * Driver settings which change the rows read by this cache (e.g. options used in its queries).
     * A snapshot written with other settings is not restored.
     */
    @NotNull
    protected String[] getMetadataSnapshotSettings(@NotNull OWNER owner) {
        return new String[0];
    }

    /**
     * Local copy of the rows read by this cache, or null if metadata snapshots are not used.
     */
    @Nullable
    protected JDBCMetadataSnapshot getMetadataSnapshot(@NotNull OWNER owner) {
        if (!supportsMetadataSnapshot() || !owner.isPersisted()) {
            return null;
        }
        DBPDataSource dataSource = owner.getDataSource();
        if (dataSource == null) {
            return null;
        }
        DBPDataSourceContainer container = dataSource.getContainer();
        if (!container.getPreferenceStore().getBoolean(ModelPreferences.META_SNAPSHOT_ENABLED)) {
            return null;
        }
        synchronized (snapshotSync) {
            if (metadataSnapshot == null) {
                String ownerId = getClass().getName() + "/" + DBUtils.getObjectFullId(owner);
                metadataSnapshot = new JDBCMetadataSnapshot(
                    JDBCMetadataSnapshot.getSnapshotFile(container, ownerId),
                    JDBCMetadataSnapshot.makeSignature(container, getMetadataSnapshotSettings(owner)));
            }
            return metadataSnapshot;
        }
    }

    @Nullable
    private List<OBJECT> restoreObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @NotNull JDBCMetadataSnapshot snapshot
    ) {
        if (!snapshot.isComplete(SNAPSHOT_OBJECTS)) {
            return null;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, owner, "Restore metadata of '" + owner.getName() + "'")) {
            beforeCacheLoading(session, owner);
            try {
                List<OBJECT> objects = snapshot.restore(session, SNAPSHOT_OBJECTS, null, rs -> fetchObject(session, owner, rs));
                if (objects == null) {
                    return null;
                }
                List<OBJECT> result = new ArrayList<>(objects.size());
                for (OBJECT object : objects) {
                    if (isValidObject(monitor, owner, object)) {
                        result.add(object);
                    }
                }
                log.debug("Restored " + result.size() + " objects of " + getCacheName() + " from metadata snapshot");
                return result;
            } finally {
                afterCacheLoading(session, owner);
            }
        } catch (Exception e) {
            // E.g. the cache reads a column type which the snapshot cannot replay
            log.debug("Metadata snapshot of " + getCacheName() + " not restored, reading from database: " + e.getMessage());
            snapshot.clear(SNAPSHOT_OBJECTS);
            return null;
        }
    }

    /**
     * Schedules a background refresh of the metadata restored from the snapshot, unless it is already scheduled.
     */
    protected void scheduleSnapshotRefresh(@NotNull OWNER owner, @NotNull JDBCMetadataSnapshot snapshot) {
        synchronized (snapshotSync) {
            if (snapshotRefreshScheduled) {
                return;
            }
            snapshotRefreshScheduled = true;
        }
        new SnapshotRefreshJob(owner, snapshot).schedule(SNAPSHOT_REFRESH_DELAY_MS);
    }

    /**
     * Whether some metadata was restored from the snapshot and not refreshed yet.
     */
    protected boolean isSnapshotRefreshPending() {
        return objectsRefreshPending;
    }

    /**
     * Updates metadata restored from the snapshot which changed since the snapshot was taken.
     */
    private void refreshRestoredMetadata(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @NotNull JDBCMetadataSnapshot snapshot
    ) throws DBException {
        Set<String> changed = readChangedObjects(monitor, owner, snapshot);
        if (changed != null) {
            log.debug(changed.size() + " objects of " + getCacheName() + " changed since the metadata snapshot");
        }
        refreshRestoredObjects(monitor, owner, snapshot, changed);
        refreshRestoredChildren(monitor, owner, snapshot, changed);
    }

    /**
     * Updates children restored from the snapshot (for caches with children).
     *
     * @param changed objects changed since the snapshot was taken, or null if unknown
     */
    protected void refreshRestoredChildren(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @NotNull JDBCMetadataSnapshot snapshot,
        @Nullable Set<String> changed
    ) throws DBException {
        // No children
    }

    /**
     * Reads objects from the database and updates restored objects which changed since the snapshot was taken.
     * The database is read without the cache lock, so the cache stays usable meanwhile.
     *
     * @param changedObjects objects changed since the snapshot was taken, or null if unknown
     */
    private void refreshRestoredObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OWNER owner,
        @NotNull JDBCMetadataSnapshot snapshot,
        @Nullable Set<String> changedObjects
    ) throws DBException {
        if (!objectsRefreshPending) {
            return;
        }
        objectsRefreshPending = false;
        if (changedObjects != null && changedObjects.isEmpty()) {
            // Nothing changed: restored objects are up to date
            return;
        }
        if (!isFullyCached()) {
            // Cleared meanwhile: the next load reads the database
            return;
        }
        JDBCMetadataSnapshot.Pass pass = snapshot.beginPass(SNAPSHOT_OBJECTS, true);
        List<OBJECT> objects = new ArrayList<>();
        if (!readObjects(monitor, owner, objects, pass)) {
            return;
        }
        Set<String> changed = pass.getChangedGroups();
        List<OBJECT> added = new ArrayList<>();
        List<OBJECT> updated = new ArrayList<>();
        List<OBJECT> removed = new ArrayList<>();
        synchronized (this) {
            if (!isFullyCached()) {
                return;
            }
            for (OBJECT object : objects) {
                if (!changed.contains(object.getName())) {
                    continue;
                }
                OBJECT oldObject = getCachedObject(object.getName());
                if (oldObject != null && oldObject.getClass() == object.getClass()) {
                    // Cached instance may be referenced by navigator nodes and editors
                    deepCopyCachedObject(object, oldObject);
                    updated.add(oldObject);
                } else {
                    if (oldObject != null) {
                        removeObject(oldObject, false);
                        removed.add(oldObject);
                    }
                    cacheObject(object);
                    added.add(object);
                }
            }
            for (String name : changed) {
                OBJECT oldObject = pass.contains(name) ? null : getCachedObject(name);
                if (oldObject != null) {
                    removeObject(oldObject, false);
                    removed.add(oldObject);
                }
            }
            pass.commit();
        }
        removed.forEach(DBUtils::fireObjectRemove);
        added.forEach(object -> DBUtils.fireObjectAdd(object, Collections.emptyMap()));
        updated.forEach(DBUtils::fireObjectUpdate);
        if (!changed.isEmpty()) {
            log.debug("Metadata of " + getCacheName() + " refreshed: " + changed.size() + " objects changed");
        }
    }

    public void beforeCacheLoading(@NotNull JDBCSession session, @NotNull OWNER owner) throws DBException {
        // Do nothing
    }

    public void afterCacheLoading(@NotNull JDBCSession session, OWNER owner) throws DBException {
        // Do nothing
    }

    @NotNull
    protected String getCacheName() {
        return getClass().getSimpleName();
    }

    // Can be implemented to provide custom cache error handler
    protected boolean handleCacheReadError(@NotNull Exception error) {
        return false;
    }

    private class SnapshotRefreshJob extends AbstractJob {
        private final OWNER owner;
        private final JDBCMetadataSnapshot snapshot;

        SnapshotRefreshJob(@NotNull OWNER owner, @NotNull JDBCMetadataSnapshot snapshot) {
            super("Refresh metadata of '" + owner.getName() + "'");
            this.owner = owner;
            this.snapshot = snapshot;
            setSystem(true);
            setUser(false);
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            try {
                refreshRestoredMetadata(monitor, owner, snapshot);
            } catch (Exception e) {
                // The snapshot stays as it is: the next session reads it again
                log.debug("Error refreshing metadata of '" + owner.getName() + "': " + e.getMessage());
            } finally {
                synchronized (snapshotSync) {
                    snapshotRefreshScheduled = false;
                }
            }
            if (isSnapshotRefreshPending() && !monitor.isCanceled()) {
                // Restored meanwhile
                scheduleSnapshotRefresh(owner, snapshot);
            }
            return Status.OK_STATUS;
        }
    }
}
