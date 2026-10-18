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

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;

public class OracleMetadataSnapshotTest {

    private static final OracleMetadataSnapshot.Layout LAYOUT = new OracleMetadataSnapshot.Layout(
        new String[]{"OBJECT_NAME", "STATUS", "LAST_DDL_TIME", "NUM_ROWS", "COMMENTS_LOADED", "DATA_SCALE"},
        new boolean[]{false, false, true, false, false, false});

    @Test
    public void captureReadsTimestampsAsMillisAndOtherColumnsAsStrings() throws SQLException {
        ResultSetMetaData meta = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(meta.getColumnCount()).thenReturn(3);
        Mockito.when(meta.getColumnLabel(1)).thenReturn("OBJECT_NAME");
        Mockito.when(meta.getColumnLabel(2)).thenReturn("CREATED");
        Mockito.when(meta.getColumnLabel(3)).thenReturn("NUM_ROWS");
        Mockito.when(meta.getColumnType(1)).thenReturn(Types.VARCHAR);
        Mockito.when(meta.getColumnType(2)).thenReturn(Types.TIMESTAMP);
        Mockito.when(meta.getColumnType(3)).thenReturn(Types.NUMERIC);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getString(1)).thenReturn("T1");
        Mockito.when(resultSet.getTimestamp(2)).thenReturn(new Timestamp(1_000_000L));
        Mockito.when(resultSet.getString(3)).thenReturn("42");

        OracleMetadataSnapshot.Layout layout = OracleMetadataSnapshot.Layout.of(meta);
        Object[] row = layout.capture(resultSet);

        Assertions.assertArrayEquals(new Object[]{"T1", 1_000_000L, "42"}, row);
        Assertions.assertEquals("T1", layout.getString(row, "object_name"));
    }

    @Test
    public void replayReturnsStoredValuesThroughJdbcGetters() throws SQLException {
        ResultSet resultSet = OracleMetadataSnapshot.replay(LAYOUT, new Object[]{"T1", "VALID", 5_000L, "123", "1", null});

        Assertions.assertEquals("T1", JDBCUtils.safeGetString(resultSet, "OBJECT_NAME"));
        Assertions.assertEquals(new Timestamp(5_000L), JDBCUtils.safeGetTimestamp(resultSet, "LAST_DDL_TIME"));
        Assertions.assertEquals(123L, JDBCUtils.safeGetLong(resultSet, "NUM_ROWS"));
        Assertions.assertTrue(JDBCUtils.safeGetBoolean(resultSet, "COMMENTS_LOADED"));
        Assertions.assertTrue(JDBCUtils.safeGetBoolean(resultSet, "STATUS", "VALID"));
        Assertions.assertEquals(1, resultSet.findColumn("OBJECT_NAME"));
    }

    @Test
    public void replayReportsNullsLikeJdbc() {
        ResultSet resultSet = OracleMetadataSnapshot.replay(LAYOUT, new Object[]{"T1", null, null, null, null, null});

        Assertions.assertNull(JDBCUtils.safeGetInteger(resultSet, "DATA_SCALE"));
        Assertions.assertEquals(0, JDBCUtils.safeGetInt(resultSet, "NUM_ROWS"));
        Assertions.assertNull(JDBCUtils.safeGetTimestamp(resultSet, "LAST_DDL_TIME"));
        Assertions.assertFalse(JDBCUtils.safeGetBoolean(resultSet, "COMMENTS_LOADED"));
    }

    @Test
    public void replayTreatsUnknownColumnsAsMissing() {
        ResultSet resultSet = OracleMetadataSnapshot.replay(LAYOUT, new Object[]{"T1", "VALID", null, null, null, null});

        Assertions.assertThrows(SQLException.class, () -> resultSet.getString("NO_SUCH_COLUMN"));
        Assertions.assertNull(JDBCUtils.safeGetString(resultSet, "NO_SUCH_COLUMN"));
    }

    @Test
    public void replayAllIteratesRowsInOrder() throws SQLException {
        ResultSet resultSet = OracleMetadataSnapshot.replayAll(LAYOUT, List.of(
            new Object[]{"I1", "VALID", null, "1", null, null},
            new Object[]{"I2", "INVALID", null, "2", null, null}));

        Assertions.assertTrue(resultSet.next());
        Assertions.assertEquals("I1", resultSet.getString("OBJECT_NAME"));
        Assertions.assertEquals(1, resultSet.getInt("NUM_ROWS"));
        Assertions.assertTrue(resultSet.next());
        Assertions.assertEquals("I2", resultSet.getString("OBJECT_NAME"));
        Assertions.assertEquals("INVALID", resultSet.getString("STATUS"));
        Assertions.assertFalse(resultSet.next());
    }

    @Test
    public void replayAllOfNoRowsIsEmpty() throws SQLException {
        Assertions.assertFalse(OracleMetadataSnapshot.replayAll(LAYOUT, List.of()).next());
    }

    @Test
    public void unchangedObjectsAreNotReloaded() {
        OracleMetadataRefreshJob.Changes changes = OracleMetadataRefreshJob.findChanges(
            Map.of("T1", stored("T1", "VALID", 10_000, true, 10_000)),
            Map.of(),
            Map.of("T1", dictionary("T1", 5_000, "VALID")),
            0, 2_000);

        Assertions.assertTrue(changes.reloadObjects().isEmpty());
        Assertions.assertTrue(changes.reloadColumns().isEmpty());
        Assertions.assertTrue(changes.dropped().isEmpty());
    }

    @Test
    public void objectChangedAfterReadIsReloaded() {
        OracleMetadataRefreshJob.Changes changes = OracleMetadataRefreshJob.findChanges(
            Map.of("T1", stored("T1", "VALID", 10_000, true, 10_000)),
            Map.of(),
            Map.of("T1", dictionary("T1", 20_000, "VALID")),
            0, 2_000);

        Assertions.assertEquals(List.of("T1"), List.copyOf(changes.reloadObjects()));
    }

    @Test
    public void changeWithinTheMarginBeforeReadIsReloaded() {
        // DDL time has one second precision: a change right before the read may not be in the stored row
        OracleMetadataRefreshJob.Changes changes = OracleMetadataRefreshJob.findChanges(
            Map.of("T1", stored("T1", "VALID", 10_000, false, 0)),
            Map.of(),
            Map.of("T1", dictionary("T1", 9_000, "VALID")),
            0, 2_000);

        Assertions.assertTrue(changes.reloadObjects().contains("T1"));
    }

    @Test
    public void clockOffsetIsApplied() {
        // Server clock is one hour behind the client
        long hour = 3_600_000;
        OracleMetadataRefreshJob.Changes changes = OracleMetadataRefreshJob.findChanges(
            Map.of("T1", stored("T1", "VALID", hour + 10_000, false, 0)),
            Map.of(),
            Map.of("T1", dictionary("T1", 5_000, "VALID")),
            hour, 2_000);

        Assertions.assertTrue(changes.reloadObjects().isEmpty());
    }

    @Test
    public void invalidatedViewIsReloadedThoughDdlTimeDidNotChange() {
        OracleMetadataRefreshJob.Changes changes = OracleMetadataRefreshJob.findChanges(
            Map.of("V1", stored("V1", "VALID", 10_000, true, 10_000)),
            Map.of(),
            Map.of("V1", dictionary("V1", 5_000, "INVALID")),
            0, 2_000);

        Assertions.assertTrue(changes.reloadObjects().contains("V1"));
    }

    @Test
    public void columnsReadBeforeTheLastDdlAreReloaded() {
        OracleMetadataRefreshJob.Changes changes = OracleMetadataRefreshJob.findChanges(
            Map.of("T1", stored("T1", "VALID", 30_000, true, 10_000)),
            Map.of(),
            Map.of("T1", dictionary("T1", 20_000, "VALID")),
            0, 2_000);

        Assertions.assertTrue(changes.reloadObjects().isEmpty());
        Assertions.assertEquals(List.of("T1"), List.copyOf(changes.reloadColumns()));
    }

    @Test
    public void newAndDroppedObjectsAreFound() {
        OracleMetadataRefreshJob.Changes changes = OracleMetadataRefreshJob.findChanges(
            Map.of("OLD", stored("OLD", "VALID", 10_000, false, 0)),
            Map.of(),
            Map.of("NEW", dictionary("NEW", 5_000, "VALID")),
            0, 2_000);

        Assertions.assertEquals(List.of("NEW"), List.copyOf(changes.reloadObjects()));
        Assertions.assertEquals(List.of("OLD"), changes.dropped());
    }

    @Test
    public void unchangedAbsentObjectIsNotReadAgain() {
        // E.g. a nested table: in the dictionary, but never returned by the table query
        OracleMetadataRefreshJob.Changes changes = OracleMetadataRefreshJob.findChanges(
            Map.of(),
            Map.of("NESTED", 10_000L),
            Map.of("NESTED", dictionary("NESTED", 5_000, "VALID")),
            0, 2_000);

        Assertions.assertTrue(changes.reloadObjects().isEmpty());
    }

    private static OracleMetadataSnapshot.StoredObject stored(String name, String status, long readTime, boolean hasColumns, long columnsReadTime) {
        return new OracleMetadataSnapshot.StoredObject(name, status, readTime, hasColumns, columnsReadTime);
    }

    private static OracleMetadataRefreshJob.DictionaryObject dictionary(String name, long ddlTime, String status) {
        return new OracleMetadataRefreshJob.DictionaryObject(name, ddlTime, status, 0);
    }
}
