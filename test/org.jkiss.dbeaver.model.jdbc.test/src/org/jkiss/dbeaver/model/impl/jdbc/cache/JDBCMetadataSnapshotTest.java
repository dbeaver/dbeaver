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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCFactoryDefault;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JDBCMetadataSnapshotTest extends DBeaverUnitTest {

    private static final String TABLES = "tables";
    private static final String COLUMNS = "columns";
    private static final String SIGNATURE = "test";
    private static final Timestamp CREATED = Timestamp.valueOf("2026-01-02 03:04:05");

    @TempDir
    Path tempDir;

    private Path file;
    private JDBCSession session;

    @BeforeEach
    public void setUp() {
        file = tempDir.resolve("snapshot.json.gz");
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class);
        Mockito.when(dataSource.getJdbcFactory()).thenReturn(new JDBCFactoryDefault());
        session = Mockito.mock(JDBCSession.class);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.getExecutionContext()).thenReturn(Mockito.mock(JDBCExecutionContext.class));
    }

    @Test
    public void restoresRowsWrittenToDisk() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        recordTables(snapshot, true, new Object[][]{{"T1", 10, CREATED}, {"T2", null, null}});
        snapshot.flush();

        JDBCMetadataSnapshot restored = new JDBCMetadataSnapshot(file, SIGNATURE);
        Assertions.assertTrue(restored.isComplete(TABLES));
        List<String> rows = restored.restore(session, TABLES, null, rs -> {
            String name = rs.getString("TABLE_NAME");
            int size = rs.getInt("ROW_COUNT");
            boolean sizeNull = rs.wasNull();
            Timestamp created = rs.getTimestamp("CREATED");
            return name + ":" + size + ":" + sizeNull + ":" + created;
        });
        Assertions.assertEquals(List.of("T1:10:false:" + CREATED, "T2:0:true:null"), rows);
    }

    @Test
    public void ignoresSnapshotOfOtherSignature() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        recordTables(snapshot, true, new Object[][]{{"T1", 1, CREATED}});
        snapshot.flush();

        JDBCMetadataSnapshot other = new JDBCMetadataSnapshot(file, "other");
        Assertions.assertFalse(other.isComplete(TABLES));
        Assertions.assertNull(other.restore(session, TABLES, null, rs -> rs.getString(1)));
    }

    @Test
    public void ignoresBrokenFile() throws Exception {
        Files.writeString(file, "not a snapshot", StandardCharsets.UTF_8);
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        Assertions.assertFalse(snapshot.isComplete(TABLES));

        recordTables(snapshot, true, new Object[][]{{"T1", 1, CREATED}});
        snapshot.flush();
        Assertions.assertTrue(new JDBCMetadataSnapshot(file, SIGNATURE).isComplete(TABLES));
    }

    @Test
    public void keepsSnapshotWhenPassIsNotCommitted() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        recordTables(snapshot, true, new Object[][]{{"T1", 1, CREATED}});

        // Canceled load: rows are recorded, but the pass is not committed
        JDBCMetadataSnapshot.Pass pass = snapshot.beginPass(TABLES, true);
        ResultSet resultSet = tableResultSet(new Object[][]{{"T2", 2, CREATED}});
        while (resultSet.next()) {
            pass.record(resultSet.getString(1), resultSet);
        }
        Assertions.assertEquals(List.of("T1"), restoreNames(snapshot, null));
    }

    @Test
    public void skipsPassWhenRowCannotBeRead() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        recordTables(snapshot, true, new Object[][]{{"T1", 1, CREATED}});

        JDBCMetadataSnapshot.Pass pass = snapshot.beginPass(TABLES, true);
        ResultSet resultSet = tableResultSet(new Object[][]{{"T2", 2, CREATED}});
        resultSet.next();
        Mockito.when(resultSet.getString(2)).thenThrow(new SQLException("Unsupported type"));
        pass.record("T2", resultSet);
        pass.commit();
        Assertions.assertEquals(List.of("T1"), restoreNames(snapshot, null));
    }

    @Test
    public void fullListReplacesGroups() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        recordTables(snapshot, true, new Object[][]{{"T1", 1, CREATED}, {"T2", 2, CREATED}});
        recordTables(snapshot, true, new Object[][]{{"T2", 2, CREATED}, {"T3", 3, CREATED}});
        Assertions.assertEquals(List.of("T2", "T3"), restoreNames(snapshot, null));
    }

    @Test
    public void partialPassUpdatesGroups() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        recordTables(snapshot, true, new Object[][]{{"T1", 1, CREATED}, {"T2", 2, CREATED}});
        final long before = snapshot.getReadTimes(TABLES).get("T2");
        Thread.sleep(5);
        recordTables(snapshot, false, new Object[][]{{"T2", 20, CREATED}});

        Assertions.assertTrue(snapshot.isComplete(TABLES));
        Assertions.assertEquals(List.of("T2:20"), snapshot.restore(session, TABLES, "T2",
            rs -> rs.getString("TABLE_NAME") + ":" + rs.getLong("ROW_COUNT")));
        Map<String, Long> readTimes = snapshot.getReadTimes(TABLES);
        Assertions.assertTrue(readTimes.get("T2") > before);
        Assertions.assertEquals(List.of("T1", "T2"), List.copyOf(readTimes.keySet()));
    }

    @Test
    public void restoresGroupsOfChildren() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        JDBCMetadataSnapshot.Pass pass = snapshot.beginPass(COLUMNS, false);
        ResultSet resultSet = tableResultSet(new Object[][]{{"T1", 1, CREATED}, {"T1", 2, CREATED}});
        while (resultSet.next()) {
            pass.record(resultSet.getString(1), resultSet);
        }
        pass.recordEmpty("T2");
        pass.commit();

        Assertions.assertFalse(snapshot.isComplete(COLUMNS));
        Assertions.assertNull(snapshot.restore(session, COLUMNS, null, rs -> rs.getString(1)));
        Assertions.assertEquals(List.of(1, 2), snapshot.restore(session, COLUMNS, "T1", rs -> rs.getInt(2)));
        Assertions.assertEquals(List.of(), snapshot.restore(session, COLUMNS, "T2", rs -> rs.getInt(2)));
        Assertions.assertNull(snapshot.restore(session, COLUMNS, "T3", rs -> rs.getInt(2)));

        snapshot.removeGroup(COLUMNS, "T1");
        Assertions.assertFalse(snapshot.hasGroup(COLUMNS, "T1"));
        Assertions.assertTrue(snapshot.hasGroup(COLUMNS, "T2"));
    }

    @Test
    public void detectsChangedGroups() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        recordTables(snapshot, true, new Object[][]{{"T1", 1, CREATED}, {"T2", 2, CREATED}, {"T3", 3, CREATED}});

        // T1 unchanged, T2 changed, T3 dropped, T4 created
        JDBCMetadataSnapshot.Pass pass = snapshot.beginPass(TABLES, true);
        ResultSet resultSet = tableResultSet(new Object[][]{{"T1", 1, CREATED}, {"T2", 20, CREATED}, {"T4", 4, CREATED}});
        while (resultSet.next()) {
            pass.record(resultSet.getString(1), resultSet);
        }
        Assertions.assertEquals(Set.of("T2", "T3", "T4"), pass.getChangedGroups());
        Assertions.assertTrue(pass.contains("T4"));
        Assertions.assertFalse(pass.contains("T3"));

        // Partial pass: groups which were not read are not reported
        JDBCMetadataSnapshot.Pass partial = snapshot.beginPass(TABLES, false);
        resultSet = tableResultSet(new Object[][]{{"T1", 1, CREATED}});
        while (resultSet.next()) {
            partial.record(resultSet.getString(1), resultSet);
        }
        partial.recordEmpty("T2");
        Assertions.assertEquals(Set.of("T2"), partial.getChangedGroups());
    }

    @Test
    public void restoresAllGroups() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        JDBCMetadataSnapshot.Pass pass = snapshot.beginPass(COLUMNS, true);
        ResultSet resultSet = tableResultSet(new Object[][]{{"T1", 1, CREATED}, {"T2", 2, CREATED}, {"T1", 3, CREATED}});
        while (resultSet.next()) {
            pass.record(resultSet.getString(1), resultSet);
        }
        pass.recordEmpty("T3");
        pass.commit();

        Map<String, List<String>> restored = snapshot.restoreGroups(session, COLUMNS,
            (group, rs) -> group + ":" + rs.getInt("ROW_COUNT"));
        Assertions.assertEquals(Map.of("T1", List.of("T1:1", "T1:3"), "T2", List.of("T2:2"), "T3", List.of()), restored);
        Assertions.assertNull(snapshot.restoreGroups(session, TABLES, (group, rs) -> group));
    }

    @Test
    public void clearForgetsSet() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        recordTables(snapshot, true, new Object[][]{{"T1", 1, CREATED}});
        snapshot.clear(TABLES);
        snapshot.flush();
        Assertions.assertFalse(new JDBCMetadataSnapshot(file, SIGNATURE).isComplete(TABLES));
    }

    @Test
    public void reportsUnsupportedGetters() throws Exception {
        JDBCMetadataSnapshot snapshot = new JDBCMetadataSnapshot(file, SIGNATURE);
        recordTables(snapshot, true, new Object[][]{{"T1", 1, CREATED}});
        Assertions.assertThrows(SQLFeatureNotSupportedException.class,
            () -> snapshot.restore(session, TABLES, null, rs -> rs.getBytes(1)));
        Assertions.assertThrows(SQLException.class,
            () -> snapshot.restore(session, TABLES, null, rs -> rs.getString("MISSING")));
    }

    @NotNull
    private List<String> restoreNames(@NotNull JDBCMetadataSnapshot snapshot, @Nullable String group) throws Exception {
        return snapshot.restore(session, TABLES, group, rs -> rs.getString("TABLE_NAME"));
    }

    private static void recordTables(
        @NotNull JDBCMetadataSnapshot snapshot,
        boolean fullList,
        @NotNull Object[][] rows
    ) throws SQLException {
        JDBCMetadataSnapshot.Pass pass = snapshot.beginPass(TABLES, fullList);
        ResultSet resultSet = tableResultSet(rows);
        while (resultSet.next()) {
            pass.record(resultSet.getString(1), resultSet);
        }
        pass.commit();
    }

    /**
     * Result set with columns TABLE_NAME (varchar), ROW_COUNT (integer) and CREATED (timestamp).
     */
    @NotNull
    private static ResultSet tableResultSet(@NotNull Object[][] rows) throws SQLException {
        ResultSetMetaData meta = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(meta.getColumnCount()).thenReturn(3);
        Mockito.when(meta.getColumnLabel(1)).thenReturn("TABLE_NAME");
        Mockito.when(meta.getColumnLabel(2)).thenReturn("ROW_COUNT");
        Mockito.when(meta.getColumnLabel(3)).thenReturn("CREATED");
        Mockito.when(meta.getColumnType(1)).thenReturn(Types.VARCHAR);
        Mockito.when(meta.getColumnType(2)).thenReturn(Types.INTEGER);
        Mockito.when(meta.getColumnType(3)).thenReturn(Types.TIMESTAMP);

        ResultSet resultSet = Mockito.mock(ResultSet.class);
        int[] position = {-1};
        Mockito.when(resultSet.getMetaData()).thenReturn(meta);
        Mockito.when(resultSet.next()).thenAnswer(inv -> ++position[0] < rows.length);
        Mockito.when(resultSet.getString(Mockito.anyInt())).thenAnswer(inv -> {
            Object value = rows[position[0]][(int) inv.getArgument(0) - 1];
            return value == null ? null : value.toString();
        });
        Mockito.when(resultSet.getTimestamp(Mockito.anyInt())).thenAnswer(
            inv -> (Timestamp) rows[position[0]][(int) inv.getArgument(0) - 1]);
        return resultSet;
    }
}
