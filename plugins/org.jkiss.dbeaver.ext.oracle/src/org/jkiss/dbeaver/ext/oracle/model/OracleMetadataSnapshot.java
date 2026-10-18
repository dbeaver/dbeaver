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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Local copy of the metadata which SQL completion and the navigator need first (tables, views, materialized views,
 * their columns and packages) of one schema, kept on disk between sessions.
 *
 * <p>The snapshot stores the rows returned by the metadata queries, not the objects. Restored rows go through
 * the same {@code fetchObject}/{@code fetchChild} code as rows read from the database, so a restored object
 * is built exactly like a loaded one. Every stored row keeps the (client) time it was read at: the incremental
 * refresh compares it with {@code LAST_DDL_TIME} to find objects changed since then.</p>
 */
public class OracleMetadataSnapshot {

    private static final Log log = Log.getLog(OracleMetadataSnapshot.class);

    /**
     * Object kinds kept in the snapshot, one per object cache.
     */
    enum Kind {
        // A materialized view also has a TABLE object of the same name: its own status comes first
        TABLES("tables", "MATERIALIZED VIEW", "VIEW", "TABLE"),
        PACKAGES("packages", "PACKAGE", "PACKAGE BODY");

        final String id;
        // Dictionary object types which change objects of this kind, by priority of their status
        final String[] objectTypes;

        Kind(@NotNull String id, @NotNull String... objectTypes) {
            this.id = id;
            this.objectTypes = objectTypes;
        }

        @Nullable
        static Kind byId(@NotNull String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private static final int FORMAT_VERSION = 4;
    private static final String SNAPSHOT_FOLDER = "oracle-metadata-snapshots";
    private static final long SAVE_DELAY_MS = 3000;

    /**
     * Reads rows of a replayed result set into objects.
     */
    @FunctionalInterface
    public interface RowFetcher<T> {
        @Nullable
        T fetch(@NotNull JDBCResultSet resultSet) throws SQLException, DBException;
    }

    /**
     * Column labels of a stored row set. Timestamp columns are stored as epoch milliseconds, all others as strings.
     */
    static final class Layout {
        final String[] labels;
        final boolean[] timestamps;
        private final Map<String, Integer> index = new HashMap<>();

        Layout(@NotNull String[] labels, @NotNull boolean[] timestamps) {
            this.labels = labels;
            this.timestamps = timestamps;
            for (int i = 0; i < labels.length; i++) {
                index.putIfAbsent(labels[i].toUpperCase(Locale.ENGLISH), i);
            }
        }

        @NotNull
        static Layout of(@NotNull ResultSetMetaData meta) throws SQLException {
            int count = meta.getColumnCount();
            String[] labels = new String[count];
            boolean[] timestamps = new boolean[count];
            for (int i = 0; i < count; i++) {
                labels[i] = meta.getColumnLabel(i + 1);
                int type = meta.getColumnType(i + 1);
                timestamps[i] = type == Types.DATE || type == Types.TIMESTAMP || type == Types.TIMESTAMP_WITH_TIMEZONE;
            }
            return new Layout(labels, timestamps);
        }

        int indexOf(@NotNull String label) {
            Integer i = index.get(label.toUpperCase(Locale.ENGLISH));
            return i == null ? -1 : i;
        }

        boolean sameAs(@Nullable Layout other) {
            return other != null && Arrays.equals(labels, other.labels) && Arrays.equals(timestamps, other.timestamps);
        }

        @NotNull
        Object[] capture(@NotNull ResultSet resultSet) throws SQLException {
            // Columns are read in their natural order: LONG columns (e.g. DATA_DEFAULT) can be read only once
            Object[] row = new Object[labels.length];
            for (int i = 0; i < labels.length; i++) {
                if (timestamps[i]) {
                    Timestamp ts = resultSet.getTimestamp(i + 1);
                    row[i] = ts == null ? null : ts.getTime();
                } else {
                    row[i] = resultSet.getString(i + 1);
                }
            }
            return row;
        }

        @Nullable
        String getString(@NotNull Object[] row, @NotNull String label) {
            int i = indexOf(label);
            return i < 0 || row[i] == null ? null : row[i].toString();
        }
    }

    static final class Entry {
        Object[] row;
        long readTime;
        // Tables only
        @Nullable
        List<Object[]> columns;
        long columnsReadTime;
    }

    private enum RestoreState {
        // Nothing loaded yet: objects may come from the snapshot
        INITIAL,
        // Objects came from the snapshot (and are being refreshed in background)
        RESTORED,
        // Objects are read from the database (first session, or after a refresh of the schema)
        LIVE
    }

    /**
     * Stored objects of one kind.
     */
    private static final class ObjectSet {
        Layout layout;
        // The full list was read at least once: the set may be restored even if it is empty
        boolean complete;
        final Map<String, Entry> entries = new HashMap<>();
        // Objects reported by the dictionary which the object query does not return (e.g. nested tables)
        final Map<String, Long> absent = new HashMap<>();
        RestoreState restoreState = RestoreState.INITIAL;
        boolean refreshScheduled;
    }

    /**
     * Rows read by one load in the current thread. Committed to the snapshot only when the load completes.
     */
    private static final class Pass {
        private final Kind kind;
        private final boolean fullList;
        private final long startTime = System.currentTimeMillis();
        private final Map<String, Object[]> objectRows = new LinkedHashMap<>();
        private final Map<String, List<Object[]>> columnRows = new LinkedHashMap<>();
        // Index rows (one per index column) by table, for index passes only
        private Map<String, List<Object[]>> indexRows;
        private ResultSet lastResultSet;
        private Layout lastLayout;

        private Pass(@Nullable Kind kind, boolean fullList) {
            this.kind = kind;
            this.fullList = fullList;
        }

        @NotNull
        private Layout layoutOf(@NotNull ResultSet resultSet) throws SQLException {
            if (!Objects.equals(resultSet, lastResultSet)) {
                lastResultSet = resultSet;
                lastLayout = Layout.of(resultSet.getMetaData());
            }
            return lastLayout;
        }
    }

    /**
     * State of a stored object, for comparison with the dictionary.
     */
    record StoredObject(@NotNull String name, @Nullable String status, long readTime, boolean hasColumns, long columnsReadTime) {
    }

    private final OracleSchema schema;
    private final Path file;
    private final String signature;

    private final Map<Kind, ObjectSet> sets = new EnumMap<>(Kind.class);
    private Layout columnLayout;
    private boolean columnsComplete;
    private Layout indexLayout;
    private final Map<String, List<Object[]>> indexRows = new HashMap<>();
    private boolean indexesComplete;
    private long indexesReadTime;
    // Table sizes (navigator statistics), null if never read
    private Map<String, Long> tableSizes;

    private boolean fileRead;
    private final ThreadLocal<Pass> currentPass = new ThreadLocal<>();
    private SaveJob saveJob;

    OracleMetadataSnapshot(@NotNull OracleSchema schema) {
        this.schema = schema;
        for (Kind kind : Kind.values()) {
            sets.put(kind, new ObjectSet());
        }
        this.file = getSnapshotFile(schema.getDataSource().getContainer(), schema.getName());
        this.signature = makeSignature(schema.getDataSource(), schema.getName());
    }

    /**
     * Snapshot file of a schema. Kept in the workspace metadata, never in the project folder (which may be shared).
     * The escaped name is lossy (e.g. quoted names "A/B" and "A_B", or case on case-insensitive file systems),
     * so the hash of the exact name keeps files of different schemas apart.
     */
    @NotNull
    static Path getSnapshotFile(@NotNull DBPDataSourceContainer container, @NotNull String schemaName) {
        return DBWorkbench.getPlatform().getWorkspace().getMetadataFolder()
            .resolve(SNAPSHOT_FOLDER)
            .resolve(CommonUtils.escapeFileName(container.getProject().getName()))
            .resolve(CommonUtils.escapeFileName(container.getId()))
            .resolve(CommonUtils.escapeFileName(schemaName) + "-" + Integer.toHexString(schemaName.hashCode()) + ".json.gz");
    }

    /**
     * Settings which change the content of metadata rows. A snapshot written with other settings is ignored.
     */
    @NotNull
    private static String makeSignature(@NotNull OracleDataSource dataSource, @NotNull String schemaName) {
        DBPConnectionConfiguration cfg = dataSource.getContainer().getConnectionConfiguration();
        return String.join("|",
            String.valueOf(FORMAT_VERSION),
            schemaName,
            CommonUtils.notEmpty(cfg.getUrl()),
            CommonUtils.notEmpty(cfg.getUserName()),
            String.valueOf(dataSource.isServerVersionAtLeast(12, 1)),
            CommonUtils.notEmpty(cfg.getProviderProperty(OracleConstants.PROP_ALWAYS_USE_DBA_VIEWS)),
            CommonUtils.notEmpty(cfg.getProviderProperty(OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY)),
            CommonUtils.notEmpty(cfg.getProviderProperty(OracleConstants.PROP_METADATA_READ_COLUMN_COMMENTS)),
            String.valueOf(dataSource.isUseRuleHint()));
    }

    @NotNull
    OracleSchema getSchema() {
        return schema;
    }

    ///////////////////////////////////////////////////////////////////////
    // Recording of rows read from the database

    /**
     * Starts recording rows of an object list load in the current thread.
     *
     * @param fullList the load reads all objects of the kind: stored objects missing in it are removed
     */
    void beginObjectsPass(@NotNull Kind kind, boolean fullList) {
        readFile();
        currentPass.set(new Pass(kind, fullList));
    }

    /**
     * Starts recording column rows in the current thread.
     */
    void beginColumnsPass() {
        readFile();
        currentPass.set(new Pass(null, false));
    }

    /**
     * Finishes the current pass. Rows are kept only if the load completed.
     *
     * @param completed load was not canceled and did not fail
     * @param columnsOf table whose columns were read in this pass (stored even if it has no columns)
     */
    void endPass(boolean completed, @Nullable String columnsOf) {
        Pass pass = currentPass.get();
        currentPass.remove();
        if (pass == null || !completed) {
            return;
        }
        synchronized (this) {
            readFile();
            if (pass.kind != null) {
                ObjectSet set = sets.get(pass.kind);
                if (pass.fullList) {
                    set.entries.keySet().retainAll(pass.objectRows.keySet());
                    set.absent.clear();
                    set.complete = true;
                }
                for (Map.Entry<String, Object[]> e : pass.objectRows.entrySet()) {
                    Entry entry = set.entries.computeIfAbsent(e.getKey(), k -> new Entry());
                    entry.row = e.getValue();
                    entry.readTime = pass.startTime;
                    set.absent.remove(e.getKey());
                }
            }
            Map<String, Entry> tables = sets.get(Kind.TABLES).entries;
            Set<String> tablesWithColumns = new HashSet<>(pass.columnRows.keySet());
            if (columnsOf != null) {
                tablesWithColumns.add(columnsOf);
            }
            for (String tableName : tablesWithColumns) {
                Entry entry = tables.get(tableName);
                if (entry == null) {
                    continue;
                }
                List<Object[]> rows = pass.columnRows.get(tableName);
                if (rows != null) {
                    entry.columns = rows;
                    entry.columnsReadTime = pass.startTime;
                } else if (entry.columns == null) {
                    // No rows may also mean the columns were already cached and nothing was read:
                    // an empty list never replaces stored rows
                    entry.columns = new ArrayList<>();
                    entry.columnsReadTime = pass.startTime;
                }
            }
        }
        scheduleSave();
    }

    /**
     * Records an object row read from the database and builds the object from the recorded copy.
     */
    @Nullable
    <T> T recordObject(
        @NotNull Kind kind,
        @NotNull JDBCSession session,
        @NotNull JDBCResultSet resultSet,
        @NotNull RowFetcher<T> fetcher
    ) throws SQLException, DBException {
        Pass pass = currentPass.get();
        if (pass != null && pass.kind != kind) {
            pass = null;
        }
        Layout layout = pass != null ? pass.layoutOf(resultSet) : Layout.of(resultSet.getMetaData());
        Object[] row = layout.capture(resultSet);
        synchronized (this) {
            readFile();
            ObjectSet set = sets.get(kind);
            if (!layout.sameAs(set.layout)) {
                // Query shape changed (e.g. other server version): rows of the old shape are useless
                set.entries.clear();
                set.layout = layout;
            }
        }
        T object = replayRow(session, layout, row, fetcher);
        String name = layout.getString(row, OracleConstants.COLUMN_OBJECT_NAME);
        if (object != null && name != null) {
            if (pass != null) {
                pass.objectRows.put(name, row);
            } else {
                synchronized (this) {
                    ObjectSet set = sets.get(kind);
                    Entry entry = set.entries.computeIfAbsent(name, k -> new Entry());
                    entry.row = row;
                    entry.readTime = System.currentTimeMillis();
                    set.absent.remove(name);
                }
                scheduleSave();
            }
        }
        return object;
    }

    /**
     * Records a column row read from the database and builds the column from the recorded copy.
     */
    @Nullable
    <T> T recordColumn(
        @NotNull JDBCSession session,
        @NotNull String tableName,
        @NotNull JDBCResultSet resultSet,
        @NotNull RowFetcher<T> fetcher
    ) throws SQLException, DBException {
        Pass pass = currentPass.get();
        Layout layout = pass != null ? pass.layoutOf(resultSet) : Layout.of(resultSet.getMetaData());
        Object[] row = layout.capture(resultSet);
        synchronized (this) {
            readFile();
            if (!layout.sameAs(columnLayout)) {
                for (Entry entry : sets.get(Kind.TABLES).entries.values()) {
                    entry.columns = null;
                }
                columnsComplete = false;
                columnLayout = layout;
            }
        }
        T column = replayRow(session, layout, row, fetcher);
        if (column != null && pass != null) {
            pass.columnRows.computeIfAbsent(tableName, k -> new ArrayList<>()).add(row);
        }
        return column;
    }

    ///////////////////////////////////////////////////////////////////////
    // Restore

    /**
     * Returns true once per session if the snapshot can fill an empty cache of this kind.
     * After a refresh of the schema caches (F5) objects are read from the database again.
     */
    synchronized boolean canRestore(@NotNull Kind kind) {
        ObjectSet set = sets.get(kind);
        if (set.restoreState != RestoreState.INITIAL) {
            return false;
        }
        readFile();
        return set.complete;
    }

    synchronized boolean canRestoreColumns() {
        return sets.get(Kind.TABLES).restoreState == RestoreState.RESTORED;
    }

    synchronized void setRestored(@NotNull Kind kind, boolean restored) {
        sets.get(kind).restoreState = restored ? RestoreState.RESTORED : RestoreState.LIVE;
    }

    /**
     * Marks the background refresh of this kind as scheduled. Returns false if it was scheduled already.
     */
    synchronized boolean markRefreshScheduled(@NotNull Kind kind) {
        ObjectSet set = sets.get(kind);
        if (set.refreshScheduled) {
            return false;
        }
        set.refreshScheduled = true;
        return true;
    }

    synchronized void markColumnsComplete() {
        readFile();
        columnsComplete = true;
        scheduleSave();
    }

    synchronized boolean isColumnsComplete() {
        return columnsComplete;
    }

    /**
     * Starts recording index rows in the current thread.
     *
     * @param fullList the load reads indexes of all tables
     */
    void beginIndexesPass(boolean fullList) {
        readFile();
        Pass pass = new Pass(null, fullList);
        pass.indexRows = new LinkedHashMap<>();
        currentPass.set(pass);
    }

    /**
     * Finishes the current index pass. Rows are kept only if the load completed.
     */
    void endIndexesPass(boolean completed, @Nullable String forTable) {
        Pass pass = currentPass.get();
        currentPass.remove();
        if (pass == null || pass.indexRows == null || !completed) {
            return;
        }
        if (pass.lastResultSet == null) {
            // Nothing was read (e.g. the indexes were cached already): keep what is stored
            return;
        }
        synchronized (this) {
            readFile();
            if (pass.fullList) {
                indexRows.clear();
                indexRows.putAll(pass.indexRows);
                indexesComplete = true;
                indexesReadTime = pass.startTime;
            } else if (forTable != null) {
                indexRows.put(forTable, pass.indexRows.getOrDefault(forTable, new ArrayList<>()));
            }
        }
        scheduleSave();
    }

    /**
     * Records an index row read from the database and builds the index columns from the recorded copy.
     */
    @Nullable
    <T> T recordIndexRow(
        @NotNull JDBCSession session,
        @NotNull String tableName,
        @NotNull JDBCResultSet resultSet,
        @NotNull RowFetcher<T> fetcher
    ) throws SQLException, DBException {
        Pass pass = currentPass.get();
        if (pass != null && pass.indexRows == null) {
            pass = null;
        }
        Layout layout = pass != null ? pass.layoutOf(resultSet) : Layout.of(resultSet.getMetaData());
        Object[] row = layout.capture(resultSet);
        synchronized (this) {
            readFile();
            if (!layout.sameAs(indexLayout)) {
                indexRows.clear();
                indexesComplete = false;
                indexLayout = layout;
            }
        }
        if (pass != null) {
            pass.indexRows.computeIfAbsent(tableName, k -> new ArrayList<>()).add(row);
        }
        return replayRow(session, layout, row, fetcher);
    }

    /**
     * Stored index rows (all tables if forTable is null), ordered as the index query returns them,
     * or null if they cannot be restored now.
     */
    @Nullable
    synchronized ResultSet replayIndexRows(@Nullable String forTable) {
        if (sets.get(Kind.TABLES).restoreState != RestoreState.RESTORED || indexLayout == null) {
            return null;
        }
        List<Object[]> rows = new ArrayList<>();
        if (forTable == null) {
            if (!indexesComplete) {
                return null;
            }
            List<String> tables = new ArrayList<>(indexRows.keySet());
            Collections.sort(tables);
            for (String table : tables) {
                rows.addAll(indexRows.get(table));
            }
        } else {
            List<Object[]> tableRows = indexRows.get(forTable);
            if (tableRows == null) {
                if (!indexesComplete) {
                    return null;
                }
                // Complete index list without this table: it has no indexes
            } else {
                rows.addAll(tableRows);
            }
        }
        return replayAll(indexLayout, rows);
    }

    /**
     * Result set iterating stored rows with {@code next()}, like a query result.
     */
    @NotNull
    static ResultSet replayAll(@NotNull Layout layout, @NotNull List<Object[]> rows) {
        RowReplay replay = new RowReplay(layout);
        replay.rows = rows;
        return replay.newResultSet();
    }

    /**
     * Like {@link #replayAll}, for rows with only some of the columns: missing columns read as NULL.
     */
    @NotNull
    static ResultSet replayPartial(@NotNull Layout layout, @NotNull List<Object[]> rows) {
        RowReplay replay = new RowReplay(layout);
        replay.rows = rows;
        replay.lenient = true;
        return replay.newResultSet();
    }

    /**
     * Stored table sizes, or null if there are none or tables were not restored from the snapshot.
     */
    @Nullable
    synchronized Map<String, Long> getTableSizes() {
        if (sets.get(Kind.TABLES).restoreState != RestoreState.RESTORED || tableSizes == null) {
            return null;
        }
        return new HashMap<>(tableSizes);
    }

    synchronized void setTableSizes(@NotNull Map<String, Long> sizes) {
        readFile();
        tableSizes = new HashMap<>(sizes);
        if (sets.get(Kind.TABLES).complete) {
            // Sizes alone (schema never opened) are not worth a snapshot file
            scheduleSave();
        }
    }

    synchronized boolean isIndexesComplete() {
        return indexesComplete;
    }

    synchronized long getIndexesReadTime() {
        return indexesReadTime;
    }

    /**
     * Names of stored indexes.
     */
    @NotNull
    synchronized Set<String> getStoredIndexNames() {
        Set<String> names = new HashSet<>();
        if (indexLayout != null) {
            for (List<Object[]> rows : indexRows.values()) {
                for (Object[] row : rows) {
                    String name = indexLayout.getString(row, "INDEX_NAME");
                    if (name != null) {
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    /**
     * Stored indexes are outdated: the next index load reads them from the database.
     */
    synchronized void invalidateIndexes() {
        indexesComplete = false;
        indexRows.clear();
        scheduleSave();
    }

    /**
     * Builds objects from all stored rows of this kind.
     */
    @NotNull
    <T> List<T> restoreObjects(
        @NotNull Kind kind,
        @NotNull JDBCSession session,
        @NotNull RowFetcher<T> fetcher
    ) throws SQLException, DBException {
        Layout layout;
        List<Object[]> rows = new ArrayList<>();
        synchronized (this) {
            ObjectSet set = sets.get(kind);
            layout = set.layout;
            for (Entry entry : set.entries.values()) {
                if (entry.row != null) {
                    rows.add(entry.row);
                }
            }
        }
        if (layout == null || rows.isEmpty()) {
            // Schema without objects of this kind
            return new ArrayList<>();
        }
        return replayRows(session, layout, rows, fetcher);
    }

    /**
     * Builds columns of a table from stored rows. Returns null if columns of this table are not stored.
     */
    @Nullable
    <T> List<T> restoreColumns(
        @NotNull JDBCSession session,
        @NotNull String tableName,
        @NotNull RowFetcher<T> fetcher
    ) throws SQLException, DBException {
        Layout layout;
        List<Object[]> rows;
        synchronized (this) {
            Entry entry = sets.get(Kind.TABLES).entries.get(tableName);
            if (columnLayout == null || entry == null || entry.columns == null) {
                return null;
            }
            layout = columnLayout;
            rows = entry.columns;
        }
        return replayRows(session, layout, rows, fetcher);
    }

    @NotNull
    private static <T> List<T> replayRows(
        @NotNull JDBCSession session,
        @NotNull Layout layout,
        @NotNull List<Object[]> rows,
        @NotNull RowFetcher<T> fetcher
    ) throws SQLException, DBException {
        List<T> result = new ArrayList<>(rows.size());
        RowReplay replay = new RowReplay(layout);
        try (JDBCResultSet resultSet = JDBCResultSetImpl.makeResultSet(session, null, replay.newResultSet(), true)) {
            for (Object[] row : rows) {
                replay.row = row;
                T object = fetcher.fetch(resultSet);
                if (object != null) {
                    result.add(object);
                }
            }
        }
        return result;
    }

    @Nullable
    private static <T> T replayRow(
        @NotNull JDBCSession session,
        @NotNull Layout layout,
        @NotNull Object[] row,
        @NotNull RowFetcher<T> fetcher
    ) throws SQLException, DBException {
        List<T> result = replayRows(session, layout, Collections.singletonList(row), fetcher);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Result set positioned on one stored row (as restored objects see it).
     */
    @NotNull
    static ResultSet replay(@NotNull Layout layout, @NotNull Object[] row) {
        RowReplay replay = new RowReplay(layout);
        replay.row = row;
        return replay.newResultSet();
    }

    /**
     * Whether the result set replays snapshot rows (and must not be recorded again).
     */
    static boolean isReplay(@NotNull JDBCResultSet resultSet) {
        ResultSet original = resultSet.getOriginal();
        return original != null && Proxy.isProxyClass(original.getClass())
            && Proxy.getInvocationHandler(original) instanceof RowReplay;
    }

    ///////////////////////////////////////////////////////////////////////
    // Incremental refresh support

    @NotNull
    synchronized Map<String, StoredObject> getStoredObjects(@NotNull Kind kind) {
        readFile();
        ObjectSet set = sets.get(kind);
        Map<String, StoredObject> result = new HashMap<>(set.entries.size());
        for (Map.Entry<String, Entry> e : set.entries.entrySet()) {
            Entry entry = e.getValue();
            String status = entry.row == null || set.layout == null ? null : set.layout.getString(entry.row, OracleConstants.COLUMN_STATUS);
            result.put(e.getKey(), new StoredObject(e.getKey(), status, entry.readTime, entry.columns != null, entry.columnsReadTime));
        }
        return result;
    }

    @NotNull
    synchronized Map<String, Long> getAbsentObjects(@NotNull Kind kind) {
        return new HashMap<>(sets.get(kind).absent);
    }

    synchronized void markAbsent(@NotNull Kind kind, @NotNull String name, long readTime) {
        readFile();
        sets.get(kind).absent.put(name, readTime);
        scheduleSave();
    }

    synchronized void removeObject(@NotNull Kind kind, @NotNull String name) {
        readFile();
        ObjectSet set = sets.get(kind);
        set.entries.remove(name);
        set.absent.remove(name);
        scheduleSave();
    }

    ///////////////////////////////////////////////////////////////////////
    // Persistence

    /**
     * Reads the snapshot file once. Every change goes through this first: the in-memory state always contains
     * what is on disk, so a save never replaces stored objects with a partial set.
     */
    private synchronized void readFile() {
        if (fileRead) {
            return;
        }
        fileRead = true;
        if (!Files.exists(file)) {
            return;
        }
        long start = System.currentTimeMillis();
        try (JsonReader reader = new JsonReader(new InputStreamReader(
            new GZIPInputStream(new BufferedInputStream(Files.newInputStream(file), 1 << 16)), StandardCharsets.UTF_8))) {
            readSnapshot(reader);
            log.debug("Metadata snapshot of " + schema.getName() + " read in " + (System.currentTimeMillis() - start) +
                "ms: " + sets.get(Kind.TABLES).entries.size() + " tables, " + sets.get(Kind.PACKAGES).entries.size() + " packages");
        } catch (Exception e) {
            log.warn("Error reading metadata snapshot " + file + ", ignoring it", e);
            clearContent();
        }
    }

    private void clearContent() {
        for (ObjectSet set : sets.values()) {
            set.entries.clear();
            set.absent.clear();
            set.layout = null;
            set.complete = false;
        }
        columnLayout = null;
        columnsComplete = false;
        indexLayout = null;
        indexRows.clear();
        indexesComplete = false;
        tableSizes = null;
    }

    private void readSnapshot(@NotNull JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "version" -> {
                    if (reader.nextInt() != FORMAT_VERSION) {
                        clearContent();
                        return;
                    }
                }
                case "signature" -> {
                    if (!signature.equals(reader.nextString())) {
                        log.debug("Metadata snapshot of " + schema.getName() + " was made with other settings, ignoring it");
                        clearContent();
                        return;
                    }
                }
                case "sets" -> {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        Kind kind = Kind.byId(reader.nextName());
                        if (kind == null) {
                            reader.skipValue();
                        } else {
                            readObjectSet(reader, sets.get(kind));
                        }
                    }
                    reader.endObject();
                }
                case "columns" -> readColumns(reader);
                case "indexes" -> readIndexes(reader);
                case "sizes" -> {
                    tableSizes = new HashMap<>();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        tableSizes.put(reader.nextName(), reader.nextLong());
                    }
                    reader.endObject();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static void readObjectSet(@NotNull JsonReader reader, @NotNull ObjectSet set) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "layout" -> set.layout = readLayout(reader);
                case "complete" -> set.complete = reader.nextBoolean();
                case "rows" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        Entry entry = new Entry();
                        while (reader.hasNext()) {
                            switch (reader.nextName()) {
                                case "read" -> entry.readTime = reader.nextLong();
                                case "row" -> entry.row = readRow(reader, set.layout);
                                default -> reader.skipValue();
                            }
                        }
                        reader.endObject();
                        String name = entry.row == null ? null : set.layout.getString(entry.row, OracleConstants.COLUMN_OBJECT_NAME);
                        if (name != null) {
                            set.entries.put(name, entry);
                        }
                    }
                    reader.endArray();
                }
                case "absent" -> {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        set.absent.put(reader.nextName(), reader.nextLong());
                    }
                    reader.endObject();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
    }

    private void readColumns(@NotNull JsonReader reader) throws IOException {
        Map<String, Entry> tables = sets.get(Kind.TABLES).entries;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "layout" -> columnLayout = readLayout(reader);
                case "complete" -> columnsComplete = reader.nextBoolean();
                case "tables" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        String name = null;
                        long readTime = 0;
                        List<Object[]> rows = new ArrayList<>();
                        while (reader.hasNext()) {
                            switch (reader.nextName()) {
                                case "name" -> name = reader.nextString();
                                case "read" -> readTime = reader.nextLong();
                                case "rows" -> {
                                    reader.beginArray();
                                    while (reader.hasNext()) {
                                        rows.add(readRow(reader, columnLayout));
                                    }
                                    reader.endArray();
                                }
                                default -> reader.skipValue();
                            }
                        }
                        reader.endObject();
                        Entry entry = name == null ? null : tables.get(name);
                        if (entry != null) {
                            entry.columns = rows;
                            entry.columnsReadTime = readTime;
                        }
                    }
                    reader.endArray();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
    }

    private void readIndexes(@NotNull JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "layout" -> indexLayout = readLayout(reader);
                case "complete" -> indexesComplete = reader.nextBoolean();
                case "read" -> indexesReadTime = reader.nextLong();
                case "tables" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        String name = null;
                        List<Object[]> rows = new ArrayList<>();
                        while (reader.hasNext()) {
                            switch (reader.nextName()) {
                                case "name" -> name = reader.nextString();
                                case "rows" -> {
                                    reader.beginArray();
                                    while (reader.hasNext()) {
                                        rows.add(readRow(reader, indexLayout));
                                    }
                                    reader.endArray();
                                }
                                default -> reader.skipValue();
                            }
                        }
                        reader.endObject();
                        if (name != null) {
                            indexRows.put(name, rows);
                        }
                    }
                    reader.endArray();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
    }

    @NotNull
    private static Layout readLayout(@NotNull JsonReader reader) throws IOException {
        List<String> labels = new ArrayList<>();
        String types = "";
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "labels" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        labels.add(reader.nextString());
                    }
                    reader.endArray();
                }
                case "types" -> types = reader.nextString();
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (types.length() != labels.size()) {
            throw new IOException("Broken row layout");
        }
        boolean[] timestamps = new boolean[labels.size()];
        for (int i = 0; i < timestamps.length; i++) {
            timestamps[i] = types.charAt(i) == 'T';
        }
        return new Layout(labels.toArray(new String[0]), timestamps);
    }

    @NotNull
    private static Object[] readRow(@NotNull JsonReader reader, @Nullable Layout layout) throws IOException {
        if (layout == null) {
            throw new IOException("Row without layout");
        }
        Object[] row = new Object[layout.labels.length];
        reader.beginArray();
        for (int i = 0; reader.hasNext(); i++) {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
            } else if (i < row.length) {
                row[i] = layout.timestamps[i] ? (Object) reader.nextLong() : reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endArray();
        return row;
    }

    private void writeFile() throws IOException {
        Files.createDirectories(file.getParent());
        Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(
            new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpFile), 1 << 16)), StandardCharsets.UTF_8))) {
            synchronized (this) {
                readFile();
                writeSnapshot(writer);
            }
        }
        // Readers never see a partially written snapshot
        try {
            Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Some file systems (e.g. network workspaces) cannot move atomically
            Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void writeSnapshot(@NotNull JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("version").value(FORMAT_VERSION);
        writer.name("signature").value(signature);
        writer.name("sets").beginObject();
        for (Map.Entry<Kind, ObjectSet> e : sets.entrySet()) {
            ObjectSet set = e.getValue();
            if (set.layout == null && !set.complete) {
                continue;
            }
            writer.name(e.getKey().id).beginObject();
            writer.name("complete").value(set.complete);
            if (set.layout != null) {
                writer.name("layout");
                writeLayout(writer, set.layout);
            }
            writer.name("rows").beginArray();
            for (Entry entry : set.entries.values()) {
                if (entry.row != null) {
                    writer.beginObject();
                    writer.name("read").value(entry.readTime);
                    writer.name("row");
                    writeRow(writer, entry.row);
                    writer.endObject();
                }
            }
            writer.endArray();
            writer.name("absent").beginObject();
            for (Map.Entry<String, Long> absent : set.absent.entrySet()) {
                writer.name(absent.getKey()).value(absent.getValue());
            }
            writer.endObject();
            writer.endObject();
        }
        writer.endObject();
        if (columnLayout != null) {
            writer.name("columns").beginObject();
            writer.name("layout");
            writeLayout(writer, columnLayout);
            writer.name("complete").value(columnsComplete);
            writer.name("tables").beginArray();
            for (Map.Entry<String, Entry> e : sets.get(Kind.TABLES).entries.entrySet()) {
                List<Object[]> columns = e.getValue().columns;
                if (columns != null) {
                    writer.beginObject();
                    writer.name("name").value(e.getKey());
                    writer.name("read").value(e.getValue().columnsReadTime);
                    writer.name("rows").beginArray();
                    for (Object[] row : columns) {
                        writeRow(writer, row);
                    }
                    writer.endArray();
                    writer.endObject();
                }
            }
            writer.endArray();
            writer.endObject();
        }
        if (tableSizes != null) {
            writer.name("sizes").beginObject();
            for (Map.Entry<String, Long> e : tableSizes.entrySet()) {
                writer.name(e.getKey()).value(e.getValue());
            }
            writer.endObject();
        }
        if (indexLayout != null) {
            writer.name("indexes").beginObject();
            writer.name("layout");
            writeLayout(writer, indexLayout);
            writer.name("complete").value(indexesComplete);
            writer.name("read").value(indexesReadTime);
            writer.name("tables").beginArray();
            for (Map.Entry<String, List<Object[]>> e : indexRows.entrySet()) {
                writer.beginObject();
                writer.name("name").value(e.getKey());
                writer.name("rows").beginArray();
                for (Object[] row : e.getValue()) {
                    writeRow(writer, row);
                }
                writer.endArray();
                writer.endObject();
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }

    private static void writeLayout(@NotNull JsonWriter writer, @NotNull Layout layout) throws IOException {
        writer.beginObject();
        writer.name("labels").beginArray();
        for (String label : layout.labels) {
            writer.value(label);
        }
        writer.endArray();
        StringBuilder types = new StringBuilder(layout.timestamps.length);
        for (boolean ts : layout.timestamps) {
            types.append(ts ? 'T' : 'S');
        }
        writer.name("types").value(types.toString());
        writer.endObject();
    }

    private static void writeRow(@NotNull JsonWriter writer, @NotNull Object[] row) throws IOException {
        writer.beginArray();
        for (Object value : row) {
            if (value == null) {
                writer.nullValue();
            } else if (value instanceof Long l) {
                writer.value(l);
            } else {
                writer.value(value.toString());
            }
        }
        writer.endArray();
    }

    private synchronized void scheduleSave() {
        if (saveJob == null) {
            saveJob = new SaveJob();
        }
        // Rescheduling postpones the save: a burst of changes is written once
        saveJob.schedule(SAVE_DELAY_MS);
    }

    /**
     * Writes pending changes immediately (e.g. when the connection is closed).
     */
    void flush() {
        SaveJob job;
        synchronized (this) {
            job = saveJob;
        }
        if (job != null && job.cancel()) {
            job.save();
        }
    }

    private class SaveJob extends AbstractJob {
        SaveJob() {
            super("Save metadata snapshot of " + schema.getName());
            setSystem(true);
            setUser(false);
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            save();
            return Status.OK_STATUS;
        }

        void save() {
            long start = System.currentTimeMillis();
            try {
                writeFile();
                log.debug("Metadata snapshot of " + schema.getName() + " saved in " + (System.currentTimeMillis() - start) + "ms");
            } catch (IOException e) {
                log.warn("Error saving metadata snapshot " + file, e);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////
    // Replay result set

    /**
     * Read-only result set positioned on one stored row. Implements the getters used by metadata constructors
     * (via {@code JDBCUtils.safeGet*}); everything else is reported as not supported.
     */
    private static final class RowReplay implements InvocationHandler {
        private final Layout layout;
        private Object[] row;
        // Rows iterated by next(); null for a result set positioned on one row
        private List<Object[]> rows;
        private int position = -1;
        // Columns missing in the layout read as NULL instead of failing
        private boolean lenient;
        private boolean wasNull;

        RowReplay(@NotNull Layout layout) {
            this.layout = layout;
        }

        @NotNull
        ResultSet newResultSet() {
            return (ResultSet) Proxy.newProxyInstance(
                OracleMetadataSnapshot.class.getClassLoader(), new Class<?>[]{ResultSet.class}, this);
        }

        @Nullable
        @Override
        public Object invoke(@NotNull Object proxy, @NotNull Method method, @Nullable Object[] args) throws Throwable {
            String name = method.getName();
            switch (name) {
                case "next" -> {
                    if (rows == null || position + 1 >= rows.size()) {
                        return false;
                    }
                    row = rows.get(++position);
                    return true;
                }
                case "isClosed" -> {
                    return false;
                }
                case "close" -> {
                    return null;
                }
                case "wasNull" -> {
                    return wasNull;
                }
                case "findColumn" -> {
                    return columnIndex(args[0]) + 1;
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "equals" -> {
                    // Same replay, without calling equals() on the proxy itself
                    return args[0] != null && Proxy.isProxyClass(args[0].getClass())
                        && Proxy.getInvocationHandler(args[0]).equals(this);
                }
                case "toString" -> {
                    return "Metadata snapshot row";
                }
                default -> {
                    if (name.startsWith("get") && args != null && args.length == 1) {
                        if (lenient && args[0] instanceof String label && layout.indexOf(label) < 0) {
                            wasNull = true;
                            return defaultValue(method.getReturnType());
                        }
                        return getValue(name, columnIndex(args[0]), method.getReturnType());
                    }
                    throw new SQLFeatureNotSupportedException(name + " is not supported by metadata snapshot");
                }
            }
        }

        private int columnIndex(@NotNull Object column) throws SQLException {
            int index = column instanceof Number n ? n.intValue() - 1 : layout.indexOf(column.toString());
            if (index < 0 || index >= layout.labels.length) {
                throw new SQLException("Column '" + column + "' not found");
            }
            return index;
        }

        @Nullable
        private Object getValue(@NotNull String getter, int index, @NotNull Class<?> type) throws SQLException {
            Object value = row[index];
            wasNull = value == null;
            switch (getter) {
                case "getString", "getNString" -> {
                    return value == null ? null : layout.timestamps[index] ? new Timestamp((Long) value).toString() : value.toString();
                }
                case "getTimestamp" -> {
                    return value instanceof Long l ? new Timestamp(l) : null;
                }
                case "getDate" -> {
                    return value instanceof Long l ? new Date(l) : null;
                }
                case "getObject" -> {
                    return value instanceof Long l && layout.timestamps[index] ? new Timestamp(l) : value;
                }
                case "getBoolean" -> {
                    if (value == null) {
                        return false;
                    }
                    String str = value.toString().trim();
                    return isNumber(str) ? new BigDecimal(str).signum() != 0 : Boolean.parseBoolean(str);
                }
                case "getBigDecimal" -> {
                    return value == null ? null : toNumber(value);
                }
                case "getInt", "getLong", "getShort", "getByte", "getDouble", "getFloat" -> {
                    BigDecimal number = value == null ? BigDecimal.ZERO : toNumber(value);
                    if (type == int.class) {
                        return number.intValue();
                    }
                    if (type == long.class) {
                        return number.longValue();
                    }
                    if (type == short.class) {
                        return number.shortValue();
                    }
                    if (type == byte.class) {
                        return number.byteValue();
                    }
                    if (type == double.class) {
                        return number.doubleValue();
                    }
                    return number.floatValue();
                }
                default -> throw new SQLFeatureNotSupportedException(getter + " is not supported by metadata snapshot");
            }
        }

        @Nullable
        private static Object defaultValue(@NotNull Class<?> type) {
            if (type == boolean.class) {
                return false;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == double.class) {
                return 0d;
            }
            if (type == float.class) {
                return 0f;
            }
            return null;
        }

        private static boolean isNumber(@NotNull String str) {
            try {
                new BigDecimal(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        @NotNull
        private static BigDecimal toNumber(@NotNull Object value) throws SQLException {
            if (value instanceof Long l) {
                return BigDecimal.valueOf(l);
            }
            try {
                return new BigDecimal(value.toString().trim());
            } catch (NumberFormatException e) {
                throw new SQLException("Value '" + value + "' is not a number");
            }
        }
    }
}
