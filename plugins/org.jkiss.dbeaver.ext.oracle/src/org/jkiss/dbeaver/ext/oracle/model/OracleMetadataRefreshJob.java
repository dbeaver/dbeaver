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
package org.jkiss.dbeaver.ext.oracle.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Brings objects of one kind restored from {@link OracleMetadataSnapshot} up to date in background.
 *
 * <p>Reads name, type, {@code LAST_DDL_TIME} and status of the objects from the dictionary, compares them with the
 * snapshot and reloads only what changed. Everything is read without holding cache locks; the cache is updated
 * in short steps at the end, so navigator and SQL completion keep working with the snapshot meanwhile.</p>
 *
 * <p>{@code LAST_DDL_TIME} of a table changes on DDL of the table itself (columns, constraints, comments, grants,
 * triggers), but not when a dependent view becomes invalid: that is caught by comparing the status.</p>
 */
class OracleMetadataRefreshJob extends AbstractJob {

    private static final Log log = Log.getLog(OracleMetadataRefreshJob.class);

    // DDL time has a precision of one second; server/client clock offset is measured with this error at most
    private static final long DDL_TIME_MARGIN_MS = 2000;
    // The refresh shares the metadata connection: it starts after the first user actions on the restored schema
    static final long START_DELAY_MS = 5000;

    /**
     * Cache operations used by the refresh, for one object kind.
     */
    interface Target<T extends DBSObject> {
        /**
         * Reads the object from the database (recording it in the snapshot), or null if the query does not return it.
         */
        @Nullable
        T read(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException;

        @Nullable
        T getCached(@NotNull String name);

        void add(@NotNull T object);

        void remove(@NotNull T object);

        /**
         * Copies a reloaded object into the cached instance, which may be referenced by navigator nodes and editors.
         */
        void copy(@NotNull T source, @NotNull T target);
    }

    record DictionaryObject(@NotNull String name, long ddlTime, @Nullable String status, int statusPriority) {
    }

    /**
     * Objects to read again (with or without their columns) and objects gone from the database.
     */
    record Changes(@NotNull Set<String> reloadObjects, @NotNull Set<String> reloadColumns, @NotNull List<String> dropped) {
    }

    private final OracleSchema schema;
    private final OracleMetadataSnapshot snapshot;
    private final OracleMetadataSnapshot.Kind kind;
    private final Target<? extends DBSObject> target;

    <T extends DBSObject> OracleMetadataRefreshJob(
        @NotNull OracleMetadataSnapshot snapshot,
        @NotNull OracleMetadataSnapshot.Kind kind,
        @NotNull Target<T> target
    ) {
        super("Refresh " + kind.id + " of " + snapshot.getSchema().getName());
        this.schema = snapshot.getSchema();
        this.snapshot = snapshot;
        this.kind = kind;
        this.target = target;
        setSystem(true);
        setUser(false);
    }

    @Override
    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
        try {
            refresh(monitor, target);
        } catch (Exception e) {
            // Snapshot stays as it is; next session tries again
            log.debug("Error refreshing " + kind.id + " of " + schema.getName() + ": " + e.getMessage());
        }
        return Status.OK_STATUS;
    }

    private <T extends DBSObject> void refresh(@NotNull DBRProgressMonitor monitor, @NotNull Target<T> target) throws DBException {
        long start = System.currentTimeMillis();
        Map<String, OracleMetadataSnapshot.StoredObject> stored = snapshot.getStoredObjects(kind);
        Map<String, Long> absent = snapshot.getAbsentObjects(kind);

        long[] clock = new long[2];
        Map<String, DictionaryObject> dictionary = readDictionary(monitor, clock, kind.objectTypes);
        if (monitor.isCanceled()) {
            return;
        }
        Changes changes = findChanges(stored, absent, dictionary, clock[0], DDL_TIME_MARGIN_MS + clock[1]);
        Set<String> reloadObjects = changes.reloadObjects();
        Set<String> reloadColumns = changes.reloadColumns();
        List<String> dropped = changes.dropped();

        for (String name : dropped) {
            snapshot.removeObject(kind, name);
            T object = target.getCached(name);
            if (object != null) {
                target.remove(object);
                DBUtils.fireObjectRemove(object);
            }
        }

        for (String name : reloadObjects) {
            if (monitor.isCanceled()) {
                return;
            }
            long readTime = System.currentTimeMillis();
            T newObject = target.read(monitor, name);
            T oldObject = target.getCached(name);
            if (newObject == null) {
                if (oldObject != null) {
                    snapshot.removeObject(kind, name);
                    target.remove(oldObject);
                    DBUtils.fireObjectRemove(oldObject);
                } else {
                    snapshot.markAbsent(kind, name, readTime);
                }
                continue;
            }
            if (oldObject != null && oldObject.getClass() == newObject.getClass()) {
                target.copy(newObject, oldObject);
                DBUtils.fireObjectUpdate(oldObject);
            } else {
                if (oldObject != null) {
                    target.remove(oldObject);
                    DBUtils.fireObjectRemove(oldObject);
                }
                target.add(newObject);
                DBUtils.fireObjectAdd(newObject, Collections.emptyMap());
            }
            OracleMetadataSnapshot.StoredObject storedObject = stored.get(name);
            if ((storedObject != null && storedObject.hasColumns()) || (storedObject == null && snapshot.isColumnsComplete())) {
                reloadColumns.add(name);
            }
        }

        if (kind == OracleMetadataSnapshot.Kind.TABLES) {
            OracleSchema.TableCache cache = schema.getTableCache();
            for (String name : reloadColumns) {
                if (monitor.isCanceled()) {
                    return;
                }
                OracleTableBase table = cache.getCachedObject(name);
                if (table != null) {
                    cache.putColumns(table, cache.readColumns(monitor, schema, table));
                    DBUtils.fireObjectUpdate(table);
                }
            }
        }

        if (kind == OracleMetadataSnapshot.Kind.TABLES && !monitor.isCanceled()) {
            refreshIndexes(monitor, !reloadObjects.isEmpty() || !dropped.isEmpty());
        }

        log.debug("Refreshed " + kind.id + " of " + schema.getName() + " in " + (System.currentTimeMillis() - start) + "ms: " +
            dictionary.size() + " checked, " + reloadObjects.size() + " reloaded, " +
            (kind == OracleMetadataSnapshot.Kind.TABLES ? reloadColumns.size() + " column lists reloaded, " : "") +
            dropped.size() + " dropped");
    }

    /**
     * Stored indexes are reloaded (all at once: one query) when an index was created, changed or dropped since they were
     * read, or a table changed. Creating an index does not change the table's DDL time, so indexes are checked apart.
     */
    private void refreshIndexes(@NotNull DBRProgressMonitor monitor, boolean tablesChanged) throws DBException {
        if (!snapshot.isIndexesComplete()) {
            return;
        }
        long[] clock = new long[2];
        Map<String, DictionaryObject> indexes = readDictionary(monitor, clock, "INDEX");
        long readTime = snapshot.getIndexesReadTime() - DDL_TIME_MARGIN_MS - clock[1];
        boolean stale = tablesChanged;
        for (DictionaryObject index : indexes.values()) {
            if (index.ddlTime() + clock[0] >= readTime) {
                stale = true;
                break;
            }
        }
        if (!stale && !indexes.keySet().containsAll(snapshot.getStoredIndexNames())) {
            stale = true;
        }
        if (!stale || monitor.isCanceled()) {
            return;
        }
        snapshot.invalidateIndexes();
        OracleSchema.IndexCache indexCache = schema.getIndexCache();
        indexCache.clearCache();
        indexCache.getAllObjects(monitor, schema);
        log.debug("Reloaded indexes of " + schema.getName());
    }

    /**
     * Compares the dictionary with the snapshot.
     *
     * @param offset client time minus server time, ms
     * @param margin tolerance of DDL time comparisons, ms
     */
    @NotNull
    static Changes findChanges(
        @NotNull Map<String, OracleMetadataSnapshot.StoredObject> stored,
        @NotNull Map<String, Long> absent,
        @NotNull Map<String, DictionaryObject> dictionary,
        long offset,
        long margin
    ) {
        Set<String> reloadObjects = new LinkedHashSet<>();
        Set<String> reloadColumns = new LinkedHashSet<>();
        for (DictionaryObject object : dictionary.values()) {
            long ddlTime = object.ddlTime() + offset;
            OracleMetadataSnapshot.StoredObject storedObject = stored.get(object.name());
            if (storedObject == null) {
                // New object, unless the object query skipped it before and it did not change since
                Long absentTime = absent.get(object.name());
                if (absentTime == null || ddlTime >= absentTime - margin) {
                    reloadObjects.add(object.name());
                }
            } else if (ddlTime >= storedObject.readTime() - margin ||
                (storedObject.status() != null && object.status() != null && !storedObject.status().equals(object.status()))) {
                // Changed by DDL, or invalidated/recompiled (status changes without DDL time)
                reloadObjects.add(object.name());
            } else if (storedObject.hasColumns() && ddlTime >= storedObject.columnsReadTime() - margin) {
                // Columns were stored before the last DDL
                reloadColumns.add(object.name());
            }
        }
        List<String> dropped = new ArrayList<>();
        for (String name : stored.keySet()) {
            if (!dictionary.containsKey(name)) {
                dropped.add(name);
            }
        }
        return new Changes(reloadObjects, reloadColumns, dropped);
    }

    /**
     * Reads DDL time and status of objects of the given types (by priority of their status).
     *
     * @param clock receives the client/server clock offset (client minus server) and the measurement error, in ms
     */
    @NotNull
    private Map<String, DictionaryObject> readDictionary(
        @NotNull DBRProgressMonitor monitor,
        long[] clock,
        @NotNull String... objectTypes
    ) throws DBException {
        Map<String, DictionaryObject> result = new HashMap<>();
        List<String> types = Arrays.asList(objectTypes);
        OracleDataSource dataSource = schema.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, schema, "Read changes of " + kind.id + " of " + schema.getName())) {
            String sql = "SELECT " + OracleUtils.getSysCatalogHint(dataSource) +
                " OBJECT_NAME, OBJECT_TYPE, LAST_DDL_TIME, STATUS, SYSDATE AS SERVER_TIME FROM " +
                OracleUtils.getAdminAllViewPrefix(monitor, dataSource, "OBJECTS") +
                " WHERE OWNER=? AND OBJECT_TYPE IN (" + String.join(",", Collections.nCopies(types.size(), "?")) + ")";
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                dbStat.setString(1, schema.getName());
                for (int i = 0; i < types.size(); i++) {
                    dbStat.setString(i + 2, types.get(i));
                }
                dbStat.setFetchSize(OracleConstants.METADATA_FETCH_SIZE);
                long requestTime = System.currentTimeMillis();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    boolean first = true;
                    while (dbResult.next()) {
                        if (first) {
                            first = false;
                            long responseTime = System.currentTimeMillis();
                            Timestamp serverTime = dbResult.getTimestamp("SERVER_TIME");
                            if (serverTime != null) {
                                clock[0] = (requestTime + responseTime) / 2 - serverTime.getTime();
                                clock[1] = responseTime - requestTime;
                            }
                        }
                        String name = dbResult.getString("OBJECT_NAME");
                        if (name == null) {
                            continue;
                        }
                        Timestamp ddlTime = dbResult.getTimestamp("LAST_DDL_TIME");
                        long ddl = ddlTime == null ? 0 : ddlTime.getTime();
                        String status = dbResult.getString("STATUS");
                        int priority = types.indexOf(dbResult.getString("OBJECT_TYPE"));
                        // One object may have several dictionary objects (e.g. package spec and body):
                        // the latest change wins, the status comes from the object type with the highest priority
                        DictionaryObject prev = result.get(name);
                        if (prev != null) {
                            if (prev.statusPriority() < priority) {
                                status = prev.status();
                                priority = prev.statusPriority();
                            }
                            ddl = Math.max(ddl, prev.ddlTime());
                        }
                        result.put(name, new DictionaryObject(name, ddl, status, priority));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Error reading metadata changes", e);
        }
        return result;
    }
}
