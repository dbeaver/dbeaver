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
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Local copy of metadata rows of one cache owner (e.g. a schema), kept on disk between sessions.
 *
 * <p>The snapshot stores the rows returned by the metadata queries, not the objects. Restored rows are replayed
 * as a result set through the same {@code fetchObject} code as rows read from the database, so a restored object
 * is built exactly like a loaded one.</p>
 *
 * <p>Rows are kept in sets (one per kind of metadata, e.g. tables or columns) and grouped by object name:
 * a table list has one row per group, a column list has all columns of one table in a group.
 * Every group keeps the (client) time its rows were read at, for drivers which can tell what changed since then.</p>
 */
public class JDBCMetadataSnapshot {
    private static final Log log = Log.getLog(JDBCMetadataSnapshot.class);

    private static final int FORMAT_VERSION = 1;
    private static final String SNAPSHOT_FOLDER = "metadata-snapshots";
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
     * Reads rows of a replayed result set into objects, for rows of a group.
     */
    @FunctionalInterface
    public interface GroupFetcher<T> {
        @Nullable
        T fetch(@NotNull String group, @NotNull JDBCResultSet resultSet) throws SQLException, DBException;
    }

    /**
     * Column labels of a stored row set. Date and time columns are stored as epoch milliseconds, all others as strings.
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
    }

    private record Group(@NotNull List<Object[]> rows, long readTime) {
    }

    private static final class RowSet {
        Layout layout;
        // The full list was read at least once: the set may be restored even if it has no groups
        boolean complete;
        final Map<String, Group> groups = new LinkedHashMap<>();
    }

    /**
     * Rows read by one load. Stored in the snapshot only by {@link #commit()}, i.e. when the load completed:
     * a canceled or failed load leaves the snapshot as it was.
     */
    public final class Pass {
        private final String setId;
        private final boolean fullList;
        private final long startTime = System.currentTimeMillis();
        private final Map<String, List<Object[]>> groups = new LinkedHashMap<>();
        private Layout layout;
        private ResultSet lastResultSet;
        private boolean failed;

        private Pass(@NotNull String setId, boolean fullList) {
            this.setId = setId;
            this.fullList = fullList;
        }

        /**
         * Stores the current row of the result set in a group. Errors are not thrown: the load goes on,
         * only the pass is not committed.
         */
        public void record(@NotNull String group, @NotNull ResultSet resultSet) {
            if (failed) {
                return;
            }
            try {
                if (!Objects.equals(resultSet, lastResultSet)) {
                    Layout newLayout = Layout.of(resultSet.getMetaData());
                    if (layout != null && !layout.sameAs(newLayout)) {
                        throw new SQLException("Different row layout in one pass");
                    }
                    layout = newLayout;
                    lastResultSet = resultSet;
                }
                groups.computeIfAbsent(group, g -> new ArrayList<>()).add(layout.capture(resultSet));
            } catch (Exception e) {
                log.debug("Metadata row of " + setId + " not stored in snapshot: " + e.getMessage());
                failed = true;
            }
        }

        /**
         * Stores a group without rows (e.g. a table without columns).
         */
        public void recordEmpty(@NotNull String group) {
            groups.computeIfAbsent(group, g -> new ArrayList<>());
        }

        /**
         * Whether rows were read for the group in this pass.
         */
        public boolean contains(@NotNull String group) {
            return groups.containsKey(group);
        }

        /**
         * Groups whose rows differ from the stored ones: new and changed groups and, for a full list, removed groups.
         * Rows are compared as read (strings and timestamps), so any change of a metadata row is detected.
         */
        @NotNull
        public Set<String> getChangedGroups() {
            Set<String> changed = new LinkedHashSet<>();
            synchronized (JDBCMetadataSnapshot.this) {
                readFile();
                RowSet set = sets.get(setId);
                boolean sameLayout = set != null && (layout == null || layout.sameAs(set.layout));
                for (Map.Entry<String, List<Object[]>> e : groups.entrySet()) {
                    Group stored = sameLayout ? set.groups.get(e.getKey()) : null;
                    if (stored == null || !sameRows(stored.rows(), e.getValue())) {
                        changed.add(e.getKey());
                    }
                }
                if (fullList && set != null) {
                    for (String group : set.groups.keySet()) {
                        if (!groups.containsKey(group)) {
                            changed.add(group);
                        }
                    }
                }
            }
            return changed;
        }

        public void commit() {
            if (failed) {
                return;
            }
            synchronized (JDBCMetadataSnapshot.this) {
                readFile();
                RowSet set = sets.computeIfAbsent(setId, id -> new RowSet());
                if (fullList || (layout != null && set.layout != null && !layout.sameAs(set.layout))) {
                    // Groups missing in a full list are gone. Rows of another layout cannot be kept with new ones.
                    set.groups.clear();
                    set.complete = fullList;
                }
                if (layout != null) {
                    set.layout = layout;
                }
                for (Map.Entry<String, List<Object[]>> e : groups.entrySet()) {
                    set.groups.put(e.getKey(), new Group(e.getValue(), startTime));
                }
                scheduleSave();
            }
        }
    }

    private final Path file;
    private final String signature;
    private final Map<String, RowSet> sets = new HashMap<>();
    private boolean fileRead;
    private boolean dirty;
    private SaveJob saveJob;

    public JDBCMetadataSnapshot(@NotNull Path file, @NotNull String signature) {
        this.file = file;
        this.signature = signature;
    }

    /**
     * Snapshot file of a cache owner. Kept in the workspace metadata, never in the project folder (which may be shared).
     * The escaped name is lossy (e.g. "A/B" and "A_B", or case on case-insensitive file systems),
     * so the hash of the exact name keeps files of different owners apart.
     */
    @NotNull
    public static Path getSnapshotFile(@NotNull DBPDataSourceContainer container, @NotNull String ownerId) {
        return DBWorkbench.getPlatform().getWorkspace().getMetadataFolder()
            .resolve(SNAPSHOT_FOLDER)
            .resolve(CommonUtils.escapeFileName(container.getProject().getName()))
            .resolve(CommonUtils.escapeFileName(container.getId()))
            .resolve(CommonUtils.escapeFileName(ownerId) + "-" + Integer.toHexString(ownerId.hashCode()) + ".json.gz");
    }

    /**
     * Connection settings which change the content of metadata rows, plus driver specific ones.
     * A snapshot written with another signature is ignored.
     * Navigator settings are included: caches skip rows of hidden (system or utility) objects.
     */
    @NotNull
    public static String makeSignature(@NotNull DBPDataSourceContainer container, @NotNull String... settings) {
        DBPConnectionConfiguration cfg = container.getConnectionConfiguration();
        DBNBrowseSettings navigatorSettings = container.getNavigatorSettings();
        List<String> parts = new ArrayList<>();
        parts.add(String.valueOf(FORMAT_VERSION));
        parts.add(container.getDriver().getFullId());
        parts.add(CommonUtils.notEmpty(cfg.getUrl()));
        parts.add(CommonUtils.notEmpty(cfg.getUserName()));
        parts.add(String.valueOf(navigatorSettings.isShowSystemObjects()));
        parts.add(String.valueOf(navigatorSettings.isShowUtilityObjects()));
        parts.addAll(Arrays.asList(settings));
        return String.join("|", parts);
    }

    @NotNull
    public Path getFile() {
        return file;
    }

    /**
     * Starts recording rows of one load.
     *
     * @param fullList the load reads all groups of the set: stored groups missing in it are removed
     */
    @NotNull
    public Pass beginPass(@NotNull String setId, boolean fullList) {
        return new Pass(setId, fullList);
    }

    /**
     * Whether all groups of the set were stored, so the set can be restored without reading the database.
     */
    public synchronized boolean isComplete(@NotNull String setId) {
        readFile();
        RowSet set = sets.get(setId);
        return set != null && set.complete;
    }

    public synchronized boolean hasGroup(@NotNull String setId, @NotNull String group) {
        readFile();
        RowSet set = sets.get(setId);
        return set != null && set.groups.containsKey(group);
    }

    /**
     * Read times of the stored groups, by group name.
     */
    @NotNull
    public synchronized Map<String, Long> getReadTimes(@NotNull String setId) {
        readFile();
        RowSet set = sets.get(setId);
        Map<String, Long> result = new LinkedHashMap<>();
        if (set != null) {
            for (Map.Entry<String, Group> e : set.groups.entrySet()) {
                result.put(e.getKey(), e.getValue().readTime());
            }
        }
        return result;
    }

    public synchronized void removeGroup(@NotNull String setId, @NotNull String group) {
        readFile();
        RowSet set = sets.get(setId);
        if (set != null && set.groups.remove(group) != null) {
            scheduleSave();
        }
    }

    /**
     * Forgets the stored rows of a set (e.g. after an explicit refresh of the owner).
     */
    public synchronized void clear(@NotNull String setId) {
        readFile();
        if (sets.remove(setId) != null) {
            scheduleSave();
        }
    }

    /**
     * Builds objects from the stored rows, in the order they were read.
     *
     * @param group group to restore, or null for all groups of the set
     * @return restored objects, or null if nothing is stored
     */
    @Nullable
    public <T> List<T> restore(
        @NotNull JDBCSession session,
        @NotNull String setId,
        @Nullable String group,
        @NotNull RowFetcher<T> fetcher
    ) throws SQLException, DBException {
        Layout layout;
        List<Object[]> rows = new ArrayList<>();
        synchronized (this) {
            readFile();
            RowSet set = sets.get(setId);
            if (set == null || (group == null && !set.complete) || (group != null && !set.groups.containsKey(group))) {
                return null;
            }
            layout = set.layout;
            for (Map.Entry<String, Group> e : set.groups.entrySet()) {
                if (group == null || group.equals(e.getKey())) {
                    rows.addAll(e.getValue().rows());
                }
            }
        }
        List<T> result = new ArrayList<>(rows.size());
        if (rows.isEmpty()) {
            return result;
        }
        RowReplay replay = new RowReplay(layout, rows);
        try (JDBCResultSet resultSet = JDBCResultSetImpl.makeResultSet(session, null, replay.newResultSet(), true)) {
            while (resultSet.next()) {
                T object = fetcher.fetch(resultSet);
                if (object != null) {
                    result.add(object);
                }
            }
        }
        return result;
    }

    /**
     * Builds objects from the stored rows of all groups of a complete set, in the order they were read.
     *
     * @return restored objects by group name, or null if the set is not complete
     */
    @Nullable
    public <T> Map<String, List<T>> restoreGroups(
        @NotNull JDBCSession session,
        @NotNull String setId,
        @NotNull GroupFetcher<T> fetcher
    ) throws SQLException, DBException {
        Layout layout;
        List<Object[]> rows = new ArrayList<>();
        List<String> rowGroups = new ArrayList<>();
        Map<String, List<T>> result = new LinkedHashMap<>();
        synchronized (this) {
            readFile();
            RowSet set = sets.get(setId);
            if (set == null || !set.complete) {
                return null;
            }
            layout = set.layout;
            for (Map.Entry<String, Group> e : set.groups.entrySet()) {
                result.put(e.getKey(), new ArrayList<>());
                for (Object[] row : e.getValue().rows()) {
                    rows.add(row);
                    rowGroups.add(e.getKey());
                }
            }
        }
        if (rows.isEmpty()) {
            return result;
        }
        RowReplay replay = new RowReplay(layout, rows);
        try (JDBCResultSet resultSet = JDBCResultSetImpl.makeResultSet(session, null, replay.newResultSet(), true)) {
            for (int i = 0; resultSet.next(); i++) {
                String group = rowGroups.get(i);
                T object = fetcher.fetch(group, resultSet);
                if (object != null) {
                    result.get(group).add(object);
                }
            }
        }
        return result;
    }

    /**
     * Writes pending changes immediately (e.g. when the connection is closed).
     */
    public void flush() {
        SaveJob job;
        synchronized (this) {
            job = saveJob;
        }
        if (job != null) {
            job.cancel();
        }
        save();
    }

    private static boolean sameRows(@NotNull List<Object[]> rows1, @NotNull List<Object[]> rows2) {
        if (rows1.size() != rows2.size()) {
            return false;
        }
        for (int i = 0; i < rows1.size(); i++) {
            if (!Arrays.equals(rows1.get(i), rows2.get(i))) {
                return false;
            }
        }
        return true;
    }

    ///////////////////////////////////////////////////////////////////////
    // Persistence

    /**
     * Reads the snapshot file once. Every change goes through this first: the in-memory state always contains
     * what is on disk, so a save never replaces stored sets with a partial content.
     */
    private synchronized void readFile() {
        if (fileRead) {
            return;
        }
        fileRead = true;
        if (!Files.exists(file)) {
            return;
        }
        try (JsonReader reader = new JsonReader(new InputStreamReader(
            new GZIPInputStream(new BufferedInputStream(Files.newInputStream(file), 1 << 16)), StandardCharsets.UTF_8))) {
            readSnapshot(reader);
        } catch (Exception e) {
            log.warn("Error reading metadata snapshot " + file + ", ignoring it", e);
            sets.clear();
        }
    }

    private void readSnapshot(@NotNull JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "version" -> {
                    if (reader.nextInt() != FORMAT_VERSION) {
                        return;
                    }
                }
                case "signature" -> {
                    if (!signature.equals(reader.nextString())) {
                        // Written with other connection settings
                        return;
                    }
                }
                case "sets" -> {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String setId = reader.nextName();
                        sets.put(setId, readSet(reader));
                    }
                    reader.endObject();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
    }

    @NotNull
    private static RowSet readSet(@NotNull JsonReader reader) throws IOException {
        RowSet set = new RowSet();
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "complete" -> set.complete = reader.nextBoolean();
                case "layout" -> set.layout = readLayout(reader);
                case "groups" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        readGroup(reader, set);
                    }
                    reader.endArray();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return set;
    }

    private static void readGroup(@NotNull JsonReader reader, @NotNull RowSet set) throws IOException {
        String name = null;
        long readTime = 0;
        List<Object[]> rows = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "name" -> name = reader.nextString();
                case "read" -> readTime = reader.nextLong();
                case "rows" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        rows.add(readRow(reader, set.layout));
                    }
                    reader.endArray();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (name == null) {
            throw new IOException("Group without name");
        }
        set.groups.put(name, new Group(rows, readTime));
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

    private synchronized void scheduleSave() {
        dirty = true;
        if (saveJob == null) {
            saveJob = new SaveJob();
        }
        // Rescheduling postpones the save: a burst of changes is written once
        saveJob.schedule(SAVE_DELAY_MS);
    }

    private void save() {
        try {
            writeFile();
        } catch (IOException e) {
            log.warn("Error saving metadata snapshot " + file, e);
        }
    }

    private void writeFile() throws IOException {
        Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");
        synchronized (this) {
            if (!dirty) {
                return;
            }
            Files.createDirectories(file.getParent());
            try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(
                new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpFile), 1 << 16)), StandardCharsets.UTF_8))) {
                writeSnapshot(writer);
            }
            dirty = false;
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
        for (Map.Entry<String, RowSet> e : sets.entrySet()) {
            RowSet set = e.getValue();
            writer.name(e.getKey()).beginObject();
            writer.name("complete").value(set.complete);
            if (set.layout != null) {
                writer.name("layout");
                writeLayout(writer, set.layout);
            }
            writer.name("groups").beginArray();
            for (Map.Entry<String, Group> g : set.groups.entrySet()) {
                writer.beginObject();
                writer.name("name").value(g.getKey());
                writer.name("read").value(g.getValue().readTime());
                writer.name("rows").beginArray();
                for (Object[] row : g.getValue().rows()) {
                    writeRow(writer, row);
                }
                writer.endArray();
                writer.endObject();
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
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

    private class SaveJob extends AbstractJob {
        SaveJob() {
            super("Save metadata snapshot");
            setSystem(true);
            setUser(false);
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            save();
            return Status.OK_STATUS;
        }
    }

    ///////////////////////////////////////////////////////////////////////
    // Replay result set

    /**
     * Read-only result set over stored rows. Implements the getters used by metadata readers
     * (e.g. via {@code JDBCUtils.safeGet*}); everything else is reported as not supported,
     * which makes the caller read the database instead.
     */
    static final class RowReplay implements InvocationHandler {
        private final Layout layout;
        private final List<Object[]> rows;
        private Object[] row;
        private int position = -1;
        private boolean wasNull;

        RowReplay(@NotNull Layout layout, @NotNull List<Object[]> rows) {
            this.layout = layout;
            this.rows = rows;
        }

        @NotNull
        ResultSet newResultSet() {
            return (ResultSet) Proxy.newProxyInstance(
                JDBCMetadataSnapshot.class.getClassLoader(), new Class<?>[]{ResultSet.class}, this);
        }

        @Nullable
        @Override
        public Object invoke(@NotNull Object proxy, @NotNull Method method, @Nullable Object[] args) throws Throwable {
            String name = method.getName();
            switch (name) {
                case "next" -> {
                    if (position + 1 >= rows.size()) {
                        row = null;
                        return false;
                    }
                    row = rows.get(++position);
                    return true;
                }
                case "isClosed" -> {
                    return false;
                }
                case "close", "clearWarnings", "getWarnings" -> {
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
                    return "Metadata snapshot rows";
                }
                default -> {
                    if (name.startsWith("get") && args != null && args.length == 1) {
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
            if (row == null) {
                throw new SQLException("Result set is not positioned on a row");
            }
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
