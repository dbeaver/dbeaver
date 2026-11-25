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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class OracleBulkColumnCacheTest extends DBeaverUnitTest {
    private DBPDataSourceContainer container;
    private OracleSchema schema;
    private JDBCSession session;
    private JDBCPreparedStatement statement;

    @BeforeEach
    public void setUp() throws Exception {
        container = Mockito.mock(DBPDataSourceContainer.class);
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(container.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(preferences.getBoolean(ModelPreferences.META_USE_SERVER_SIDE_FILTERS)).thenReturn(true);
        Mockito.when(container.getConnectionConfiguration()).thenReturn(new DBPConnectionConfiguration());
        OracleDataSource dataSource = Mockito.mock(OracleDataSource.class);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new OracleSQLDialect());
        Mockito.when(dataSource.isViewAvailable(Mockito.any(), Mockito.nullable(String.class), Mockito.anyString())).thenReturn(true);
        schema = new OracleSchema(dataSource, 1, "TEST_SCHEMA");
        session = Mockito.mock(JDBCSession.class);
        statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.getProgressMonitor()).thenReturn(monitor);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);
    }

    @Test
    public void filteredBulkColumnsUseBoundedNameBatches() throws Exception {
        OracleSchema.TableCache cache = filteredCache();
        List<OracleTableBase> tables = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            tables.add(new OracleTable(schema, "TABLE_" + i));
        }
        // Names resolved outside the filter live in the same cache and must get their columns too.
        tables.add(new OracleTable(schema, "REFERENCED_TABLE"));
        cache.setCache(tables);

        List<List<OracleTableBase>> batches = cache.getBulkChildrenBatches(schema);
        Assertions.assertEquals(2, batches.size());
        Assertions.assertEquals(500, batches.get(0).size());
        Assertions.assertEquals(2, batches.get(1).size());

        container.getConnectionConfiguration().setProviderProperty(OracleConstants.PROP_METADATA_READ_COLUMN_COMMENTS, "true");
        cache.prepareChildrenStatement(session, schema, null, batches.get(1));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getValue().contains("WHERE c.OWNER=? AND c.TABLE_NAME IN (?,?)"));
        Assertions.assertTrue(sql.getValue().contains("LEFT OUTER JOIN ALL_COL_COMMENTS cc"));
        Mockito.verify(statement).setString(1, "TEST_SCHEMA");
        Mockito.verify(statement).setString(2, "TABLE_500");
        Mockito.verify(statement).setString(3, "REFERENCED_TABLE");

        Mockito.clearInvocations(session, statement);
        cache.prepareChildrenStatement(session, schema, tables.get(0));
        Mockito.verify(session).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getValue().contains("WHERE c.OWNER=? AND c.TABLE_NAME=?"));
        Assertions.assertFalse(sql.getValue().contains(" IN ("));
    }

    @Test
    public void broadFilterFallsBackToOwnerWideColumnLoad() throws Exception {
        OracleSchema.TableCache cache = filteredCache();
        List<OracleTableBase> tables = new ArrayList<>();
        for (int i = 0; i <= 10000; i++) {
            tables.add(new OracleTable(schema, "TABLE_" + i));
        }
        cache.setCache(tables);

        List<List<OracleTableBase>> batches = cache.getBulkChildrenBatches(schema);
        Assertions.assertEquals(1, batches.size());
        cache.prepareChildrenStatement(session, schema, null, batches.getFirst());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getValue().contains("WHERE c.OWNER=?"));
        Assertions.assertFalse(sql.getValue().contains("c.TABLE_NAME IN ("));
    }

    @Test
    public void unfilteredBulkColumnsRetainOwnerWideQuery() throws Exception {
        OracleSchema.TableCache cache = schema.getTableCache();
        cache.setCache(List.of(new OracleTable(schema, "TABLE_1")));

        List<List<OracleTableBase>> batches = cache.getBulkChildrenBatches(schema);
        Assertions.assertEquals(1, batches.size());
        cache.prepareChildrenStatement(session, schema, null, batches.getFirst());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getValue().contains("WHERE c.OWNER=?"));
        Assertions.assertFalse(sql.getValue().contains("c.TABLE_NAME IN ("));
    }

    @Test
    public void emptyFilteredCacheSkipsColumnQuery() throws Exception {
        OracleSchema.TableCache cache = filteredCache();
        cache.setCache(List.of());
        Mockito.clearInvocations(session, statement);

        cache.loadChildren(monitor, schema, null);
        cache.loadChildren(monitor, schema, null);
        Mockito.verifyNoInteractions(session, statement);
    }

    @Test
    public void canceledParentLoadDoesNotPreventBulkColumnRetry() throws Exception {
        OracleSchema.TableCache cache = schema.new TableCache() {
            @Override
            @NotNull
            protected JDBCSession openChildrenSession(@NotNull DBRProgressMonitor monitor, @NotNull OracleSchema owner) {
                return session;
            }
        };
        filteredCache(cache);
        // The parent load is canceled after the initial loadChildren check, leaving the list uncached.
        Mockito.when(monitor.isCanceled()).thenReturn(false, true);
        cache.loadChildren(monitor, schema, null);
        Assertions.assertFalse(cache.isFullyCached());
        Mockito.verifyNoInteractions(session, statement);

        // Simulate the parent list becoming available on retry; bulk columns must still load.
        Mockito.when(monitor.isCanceled()).thenReturn(false);
        cache.setCache(List.of(new OracleTable(schema, "TABLE_1")));
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(statement.getResultSet()).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(false);
        cache.loadChildren(monitor, schema, null);
        cache.loadChildren(monitor, schema, null);
        Mockito.verify(session).prepareStatement(Mockito.anyString());
    }

    @Test
    public void unfinishedParentLoadWithoutCancellationDoesNotMarkBulkLoadComplete() throws Exception {
        OracleSchema.TableCache cache = schema.new TableCache() {
            @Override
            @NotNull
            protected JDBCSession openChildrenSession(@NotNull DBRProgressMonitor monitor, @NotNull OracleSchema owner) {
                return session;
            }
        };
        filteredCache(cache);
        // Unit-test mode leaves the parent cache unloaded without canceling the monitor.
        cache.loadChildren(monitor, schema, null);
        Assertions.assertFalse(cache.isFullyCached());
        Mockito.verifyNoInteractions(session, statement);

        cache.setCache(List.of(new OracleTable(schema, "TABLE_1")));
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(statement.getResultSet()).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(false);
        cache.loadChildren(monitor, schema, null);
        Mockito.verify(session).prepareStatement(Mockito.anyString());
    }

    @Test
    public void cancellationBeforeSecondBatchDoesNotMarkBulkLoadComplete() throws Exception {
        OracleSchema.TableCache cache = schema.new TableCache() {
            @Override
            @NotNull
            protected JDBCSession openChildrenSession(@NotNull DBRProgressMonitor monitor, @NotNull OracleSchema owner) {
                return session;
            }
        };
        filteredCache(cache);
        List<OracleTableBase> tables = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            tables.add(new OracleTable(schema, "TABLE_" + i));
        }
        cache.setCache(tables);
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(statement.getResultSet()).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(false);
        Mockito.when(monitor.isCanceled()).thenReturn(false, false, true);

        cache.loadChildren(monitor, schema, null);
        Mockito.verify(session).prepareStatement(Mockito.anyString());

        Mockito.when(monitor.isCanceled()).thenReturn(false);
        cache.loadChildren(monitor, schema, null);
        Mockito.verify(session, Mockito.times(3)).prepareStatement(Mockito.anyString());
        cache.loadChildren(monitor, schema, null);
        Mockito.verify(session, Mockito.times(3)).prepareStatement(Mockito.anyString());
    }

    @NotNull
    private OracleSchema.TableCache filteredCache() throws Exception {
        OracleSchema.TableCache cache = schema.getTableCache();
        filteredCache(cache);
        return cache;
    }

    private void filteredCache(@NotNull OracleSchema.TableCache cache) throws Exception {
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.addInclude("TABLE_*");
        Mockito.when(container.getObjectFilter(OracleTable.class, schema, false)).thenReturn(filter);
        cache.prepareLookupStatement(session, schema, null, null);
        Mockito.clearInvocations(session, statement);
    }
}
