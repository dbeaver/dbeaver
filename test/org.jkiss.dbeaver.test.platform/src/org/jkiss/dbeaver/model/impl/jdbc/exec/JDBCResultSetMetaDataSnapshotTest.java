/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.jdbc.PostgreJdbcFactory;
import org.jkiss.dbeaver.ext.postgresql.model.jdbc.PostgreResultSetMetaDataImpl;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCFactory;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSetMetaDataSnapshot;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import javax.sql.rowset.RowSetMetaDataImpl;

class JDBCResultSetMetaDataSnapshotTest {

    @Test
    void resultSetUsesPreparedColumnsWithoutReadingDriverFieldMetadata() throws Exception {
        ResultSetMetaData described = describedColumn();
        JDBCResultSetMetaDataSnapshot snapshot = capture(described);
        ResultSetMetaData actual = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(actual.getColumnCount()).thenReturn(1);
        Mockito.when(actual.getColumnLabel(1)).thenReturn("title");

        try (JDBCResultSet resultSet = makeResultSet(snapshot, actual)) {
            Assertions.assertEquals("title", resultSet.getMeta().getAttributes().getFirst().getName());
            Assertions.assertEquals("varchar", resultSet.getMeta().getAttributes().getFirst().getTypeName());
        }
        Mockito.verify(actual, Mockito.never()).getTableName(Mockito.anyInt());
        Mockito.verify(actual, Mockito.never()).getColumnTypeName(Mockito.anyInt());
        Mockito.verify(actual, Mockito.never()).isNullable(Mockito.anyInt());
    }

    @Test
    void mismatchedResultColumnsFailBeforeExport() throws Exception {
        JDBCResultSetMetaDataSnapshot snapshot = capture(describedColumn());
        ResultSetMetaData actual = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(actual.getColumnCount()).thenReturn(1);
        Mockito.when(actual.getColumnLabel(1)).thenReturn("different");

        try (JDBCResultSet resultSet = makeResultSet(snapshot, actual)) {
            Assertions.assertThrows(DBCException.class, resultSet::getMeta);
        }
        Mockito.verify(actual, Mockito.never()).getTableName(Mockito.anyInt());
    }

    @Test
    void optionalMetadataFailuresDoNotAbortExport() throws Exception {
        ResultSetMetaData described = describedColumn();
        Mockito.when(described.getTableName(1)).thenThrow(new SQLException("Table name unavailable"));
        Mockito.when(described.isWritable(1)).thenThrow(new SQLException("Writability unavailable"));
        Mockito.when(described.isNullable(1)).thenThrow(new SQLException("Nullability unavailable"));
        Mockito.when(described.getPrecision(1)).thenThrow(new SQLException("Precision unavailable"));

        JDBCResultSetMetaDataSnapshot snapshot = capture(described);
        ResultSetMetaData actual = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(actual.getColumnCount()).thenReturn(1);
        Mockito.when(actual.getColumnLabel(1)).thenReturn("title");

        try (JDBCResultSet resultSet = makeResultSet(snapshot, actual)) {
            Assertions.assertEquals("title", resultSet.getMeta().getAttributes().getFirst().getName());
            Assertions.assertEquals(Integer.valueOf(0), resultSet.getMeta().getAttributes().getFirst().getPrecision());
            Assertions.assertFalse(resultSet.getMeta().getAttributes().getFirst().isRequired());
        }
    }

    @Test
    void postgreFactoryPreservesBaseNamesInPreparedMetadata() throws Exception {
        BaseResultSetMetaData described = new BaseResultSetMetaData();
        described.setColumnCount(1);
        described.setColumnLabel(1, "title");
        described.setColumnName(1, "title");
        described.setColumnType(1, Types.VARCHAR);
        described.setColumnTypeName(1, "varchar");
        JDBCSession session = Mockito.mock(JDBCSession.class, Mockito.RETURNS_DEEP_STUBS);
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.isDriverVersionAtLeast(9, 0)).thenReturn(true);
        Mockito.when(session.getExecutionContext().getOwnerInstance().getName()).thenReturn("catalog");
        JDBCFactory factory = new PostgreJdbcFactory();
        JDBCResultSetMetaDataSnapshot snapshot = prepare(session, factory, described);

        ResultSetMetaData actual = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(actual.getColumnCount()).thenReturn(1);
        Mockito.when(actual.getColumnLabel(1)).thenReturn("title");
        try (JDBCResultSet resultSet = makeResultSet(snapshot, actual, factory, true)) {
            Assertions.assertInstanceOf(PostgreResultSetMetaDataImpl.class, resultSet.getMeta());
            JDBCColumnMetaData column = (JDBCColumnMetaData) resultSet.getMeta().getAttributes().getFirst();
            Assertions.assertEquals("base_table", column.getEntityName());
            Assertions.assertEquals("base_schema", column.getSchemaName());
            Assertions.assertEquals("catalog", column.getCatalogName());
        }
        Mockito.verify(actual, Mockito.never()).getTableName(Mockito.anyInt());
    }

    public static class BaseResultSetMetaData extends RowSetMetaDataImpl {
        @NotNull
        public String getBaseSchemaName(int column) {
            return "base_schema";
        }

        @NotNull
        public String getBaseTableName(int column) {
            return "base_table";
        }
    }

    @NotNull
    private static JDBCResultSetMetaDataSnapshot capture(@NotNull ResultSetMetaData described) throws Exception {
        JDBCSession session = Mockito.mock(JDBCSession.class, Mockito.RETURNS_DEEP_STUBS);
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        return prepare(session, new JDBCFactoryDefault(), described);
    }

    @NotNull
    private static JDBCResultSetMetaDataSnapshot prepare(
        @NotNull JDBCSession session,
        @NotNull JDBCFactory factory,
        @NotNull ResultSetMetaData described
    ) throws Exception {
        Mockito.when(session.getDataSource().getJdbcFactory()).thenReturn(factory);
        JDBCPreparedStatement metadataStatement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.prepareStatement("SELECT title")).thenReturn(metadataStatement);
        Mockito.when(metadataStatement.getMetaData()).thenReturn(described);
        JDBCResultSetMetaDataSnapshot snapshot = JDBCResultSetMetaDataSnapshot.prepare(session, "SELECT title");
        Mockito.verify(metadataStatement).close();
        return snapshot;
    }

    @NotNull
    private static ResultSetMetaData describedColumn() throws Exception {
        ResultSetMetaData metadata = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(metadata.getColumnCount()).thenReturn(1);
        Mockito.when(metadata.getColumnLabel(1)).thenReturn("title");
        Mockito.when(metadata.getColumnName(1)).thenReturn("title");
        Mockito.when(metadata.getTableName(1)).thenReturn("");
        Mockito.when(metadata.getSchemaName(1)).thenReturn("");
        Mockito.when(metadata.getCatalogName(1)).thenReturn("");
        Mockito.when(metadata.getColumnType(1)).thenReturn(Types.VARCHAR);
        Mockito.when(metadata.getColumnTypeName(1)).thenReturn("varchar");
        return metadata;
    }

    @NotNull
    private static JDBCResultSet makeResultSet(
        @NotNull JDBCResultSetMetaDataSnapshot snapshot,
        @NotNull ResultSetMetaData actual
    ) throws Exception {
        return makeResultSet(snapshot, actual, new JDBCFactoryDefault(), false);
    }

    @NotNull
    private static JDBCResultSet makeResultSet(
        @NotNull JDBCResultSetMetaDataSnapshot snapshot,
        @NotNull ResultSetMetaData actual,
        @NotNull JDBCFactory factory,
        boolean postgreDriver
    ) throws Exception {
        JDBCSession session = Mockito.mock(JDBCSession.class, Mockito.RETURNS_DEEP_STUBS);
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getJdbcFactory()).thenReturn(factory);
        if (postgreDriver) {
            Mockito.when(dataSource.isDriverVersionAtLeast(9, 0)).thenReturn(true);
            Mockito.when(session.getExecutionContext().getOwnerInstance().getName()).thenReturn("catalog");
        }

        ResultSet jdbcResultSet = Mockito.mock(ResultSet.class);
        Mockito.when(jdbcResultSet.getMetaData()).thenReturn(actual);
        Statement jdbcStatement = Mockito.mock(Statement.class);
        Mockito.when(jdbcStatement.getResultSet()).thenReturn(jdbcResultSet);
        JDBCStatementImpl<Statement> statement = new JDBCStatementImpl<>(session, () -> jdbcStatement, "SELECT title", true);
        statement.setResultSetMetaDataSnapshot(snapshot);
        return statement.getResultSet();
    }
}
