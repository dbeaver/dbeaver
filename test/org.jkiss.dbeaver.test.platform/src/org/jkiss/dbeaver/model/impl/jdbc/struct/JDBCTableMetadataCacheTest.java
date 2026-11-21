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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.model.DBPContextWithAttributes;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSetMetaDataSnapshot;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCFactoryDefault;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class JDBCTableMetadataCacheTest {

    @Test
    void explicitlyRegisteredSourcePreparesOnceForFullSegmentsAndEmptyPage() throws Exception {
        JDBCSession session = session();
        DBCExecutionSource source = source();
        DBPContextWithAttributes attributes = (DBPContextWithAttributes) source;
        attributes.setContextAttribute(DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA, Boolean.TRUE);
        JDBCTable<?, ?> table = table(session.getDataSource());
        List<JDBCStatement> statements = new ArrayList<>();
        stubStatements(session, statements);

        read(table, source, session, 0);
        read(table, source, session, 5);
        read(table, source, session, 10);

        Assertions.assertEquals(3, statements.size());
        ArgumentCaptor<JDBCResultSetMetaDataSnapshot> snapshots = ArgumentCaptor.forClass(JDBCResultSetMetaDataSnapshot.class);
        for (JDBCStatement statement : statements) {
            Mockito.verify(statement).setResultSetMetaDataSnapshot(snapshots.capture());
        }
        Assertions.assertSame(snapshots.getAllValues().get(0), snapshots.getAllValues().get(1));
        Assertions.assertSame(snapshots.getAllValues().get(0), snapshots.getAllValues().get(2));
        Mockito.verify(session, Mockito.times(1)).prepareStatement(Mockito.anyString());
    }

    @Test
    void absentOrRemovedRegistrationLeavesOrdinaryReadsAlone() throws Exception {
        JDBCSession session = session();
        DBCExecutionSource source = source();
        DBPContextWithAttributes attributes = (DBPContextWithAttributes) source;
        JDBCTable<?, ?> table = table(session.getDataSource());
        List<JDBCStatement> statements = new ArrayList<>();
        stubStatements(session, statements);

        read(table, source, session, 0);
        attributes.setContextAttribute(DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA, Boolean.TRUE);
        read(table, source, session, 5);
        attributes.removeContextAttribute(DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA);
        read(table, source, session, 10);

        Mockito.verify(statements.get(0), Mockito.never()).setResultSetMetaDataSnapshot(Mockito.any());
        Mockito.verify(statements.get(1)).setResultSetMetaDataSnapshot(Mockito.any());
        Mockito.verify(statements.get(2), Mockito.never()).setResultSetMetaDataSnapshot(Mockito.any());
        Mockito.verify(session, Mockito.times(1)).prepareStatement(Mockito.anyString());
    }

    @Test
    void anotherTableWithTheSameQueryMustPrepareItsOwnDescription() throws Exception {
        JDBCSession session = session();
        DBCExecutionSource source = source();
        ((DBPContextWithAttributes) source).setContextAttribute(
            DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA, Boolean.TRUE);
        JDBCTable<?, ?> first = table(session.getDataSource());
        JDBCTable<?, ?> second = table(session.getDataSource());
        List<JDBCStatement> statements = new ArrayList<>();
        stubStatements(session, statements);

        read(first, source, session, 0);
        read(second, source, session, 5);

        Mockito.verify(session, Mockito.times(2)).prepareStatement(Mockito.anyString());
    }

    @Test
    void changedSourceQueryRequiresASeparateDescription() throws Exception {
        JDBCSession session = session();
        DBCExecutionSource source = source();
        ((DBPContextWithAttributes) source).setContextAttribute(
            DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA, Boolean.TRUE);
        JDBCTable<?, ?> table = table(session.getDataSource());
        List<JDBCStatement> statements = new ArrayList<>();
        stubStatements(session, statements);

        read(table, source, session, 0);
        Mockito.doReturn("other_books").when(table).getFullyQualifiedName(DBPEvaluationContext.DML);
        read(table, source, session, 5);

        Mockito.verify(session, Mockito.times(2)).prepareStatement(Mockito.anyString());
    }

    @Test
    void executionSourceAttributesAreLazyScopedAndCopied() {
        AbstractExecutionSource source = source();
        Assertions.assertTrue(source.getContextAttributes().isEmpty());
        source.setContextAttribute(DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA, null);
        Map<String, ?> previous = source.getContextAttributes();
        Assertions.assertTrue(previous.containsKey(DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA));

        source.setContextAttribute(DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA, Boolean.TRUE);
        Assertions.assertNull(previous.get(DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA));
        Assertions.assertEquals(Boolean.TRUE, source.getContextAttribute(DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA));

        source.removeContextAttribute(DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA);
        Assertions.assertTrue(source.getContextAttributes().isEmpty());
    }

    @Test
    void sqlLimitedSegmentUsesNoFetchingPortalOrJdbcRowLimit() throws Exception {
        JDBCSession session = session();
        DBCExecutionSource source = source();
        ((DBPContextWithAttributes) source).setContextAttribute(
            DBSDataContainer.CONTEXT_READ_ATTEMPT_LIMIT_TRANSFORMER, Boolean.TRUE);
        ((DBPContextWithAttributes) source).setContextAttribute(
            DBSDataContainer.CONTEXT_READ_PREPARED_COLUMN_METADATA, Boolean.TRUE);
        JDBCTable<?, ?> table = table(session.getDataSource());
        JDBCDataSource dataSource = session.getDataSource();
        Mockito.when(dataSource.createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT))
            .thenReturn(new QueryTransformerLimit(false, true));
        List<JDBCStatement> statements = new ArrayList<>();
        stubStatements(session, statements);

        read(table, source, session, 0);
        read(table, source, session, 5);
        read(table, source, session, 10);

        Assertions.assertEquals(3, statements.size());
        Assertions.assertTrue(Objects.requireNonNull(statements.get(0).getQueryString()).contains("LIMIT 5"));
        Assertions.assertTrue(Objects.requireNonNull(statements.get(1).getQueryString()).contains("LIMIT 5 OFFSET 5"));
        Assertions.assertTrue(Objects.requireNonNull(statements.get(2).getQueryString()).contains("LIMIT 5 OFFSET 10"));
        for (JDBCStatement statement : statements) {
            Mockito.verify(statement).setStatementSource(source);
            Mockito.verify(statement).setResultsFetchSize(0);
            Mockito.verify(statement, Mockito.never()).setLimit(Mockito.anyLong(), Mockito.anyLong());
            Mockito.verify(statement).setResultSetMetaDataSnapshot(Mockito.any());
        }
        Mockito.verify(session, Mockito.times(1)).prepareStatement(Mockito.anyString());
    }

    @Test
    void missingTransformerFallsBackToJdbcLimitAndFetchSize() throws Exception {
        JDBCSession session = session();
        DBCExecutionSource source = source();
        ((DBPContextWithAttributes) source).setContextAttribute(
            DBSDataContainer.CONTEXT_READ_ATTEMPT_LIMIT_TRANSFORMER, Boolean.TRUE);
        JDBCTable<?, ?> table = table(session.getDataSource());
        List<JDBCStatement> statements = new ArrayList<>();
        stubStatements(session, statements);

        read(table, source, session, 5);
        Assertions.assertEquals("SELECT * FROM books", statements.getFirst().getQueryString());
        Mockito.verify(statements.getFirst()).setLimit(5, 5);
        Mockito.verify(statements.getFirst()).setResultsFetchSize(10);
        Mockito.verify(statements.getFirst(), Mockito.never()).setResultsFetchSize(0);
    }

    @Test
    void sqlPaginationMarkerDoesNotAffectSingleQueryReads() throws Exception {
        JDBCSession session = session();
        DBCExecutionSource source = source();
        ((DBPContextWithAttributes) source).setContextAttribute(
            DBSDataContainer.CONTEXT_READ_ATTEMPT_LIMIT_TRANSFORMER, Boolean.TRUE);
        JDBCTable<?, ?> table = table(session.getDataSource());
        List<JDBCStatement> statements = new ArrayList<>();
        stubStatements(session, statements);

        read(table, source, session, -1);
        Assertions.assertEquals("SELECT * FROM books", statements.getFirst().getQueryString());
        Mockito.verify(statements.getFirst()).setResultsFetchSize(10);
        Mockito.verify(statements.getFirst(), Mockito.never()).setResultsFetchSize(0);
    }

    private static void read(
        @NotNull JDBCTable<?, ?> table,
        @NotNull DBCExecutionSource source,
        @NotNull JDBCSession session,
        long offset
    ) throws Exception {
        table.readData(source, session, Mockito.mock(DBDDataReceiver.class), null, offset, 5, DBSDataContainer.FLAG_NONE, 10);
    }

    @NotNull
    private static JDBCTable<?, ?> table(@NotNull JDBCDataSource dataSource) {
        JDBCTable<?, ?> table = Mockito.mock(JDBCTable.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(dataSource).when(table).getDataSource();
        Mockito.doReturn("books").when(table).getFullyQualifiedName(DBPEvaluationContext.DML);
        return table;
    }

    @NotNull
    private static JDBCSession session() throws Exception {
        JDBCSession session = Mockito.mock(JDBCSession.class, Mockito.RETURNS_DEEP_STUBS);
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.getProgressMonitor()).thenReturn(Mockito.mock(DBRProgressMonitor.class));
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(dataSource.getJdbcFactory()).thenReturn(new JDBCFactoryDefault());
        ResultSetMetaData metadata = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(metadata.getColumnCount()).thenReturn(1);
        Mockito.when(metadata.getColumnLabel(1)).thenReturn("title");
        Mockito.when(metadata.getColumnName(1)).thenReturn("title");
        Mockito.when(metadata.getColumnType(1)).thenReturn(Types.VARCHAR);
        Mockito.when(metadata.getColumnTypeName(1)).thenReturn("varchar");
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenAnswer(invocation -> {
            JDBCPreparedStatement prepared = Mockito.mock(JDBCPreparedStatement.class);
            Mockito.when(prepared.getMetaData()).thenReturn(metadata);
            return prepared;
        });
        return session;
    }

    @NotNull
    private static AbstractExecutionSource source() {
        return new AbstractExecutionSource(
            Mockito.mock(DBSDataContainer.class), Mockito.mock(DBCExecutionContext.class), new Object());
    }

    private static void stubStatements(
        @NotNull JDBCSession session,
        @NotNull List<JDBCStatement> statements
    ) throws Exception {
        Mockito.when(session.prepareStatement(
            Mockito.eq(DBCStatementType.SCRIPT), Mockito.anyString(), Mockito.anyBoolean(),
            Mockito.eq(false), Mockito.eq(false)
        )).thenAnswer(invocation -> {
            JDBCStatement statement = Mockito.mock(JDBCStatement.class);
            Mockito.when(statement.getQueryString()).thenReturn(invocation.getArgument(1));
            Mockito.when(statement.getSession()).thenReturn(session);
            statements.add(statement);
            return statement;
        });
    }
}
