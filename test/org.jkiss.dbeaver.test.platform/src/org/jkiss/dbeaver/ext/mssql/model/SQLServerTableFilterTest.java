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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class SQLServerTableFilterTest extends DBeaverUnitTest {

    private SQLServerSchema schema;
    private SQLServerDataSource dataSource;
    private DBPDataSourceContainer container;
    private JDBCSession session;

    @BeforeEach
    public void setUp() throws Exception {
        schema = Mockito.mock(SQLServerSchema.class);
        dataSource = Mockito.mock(SQLServerDataSource.class);
        container = Mockito.mock(DBPDataSourceContainer.class);
        SQLServerDatabase database = Mockito.mock(SQLServerDatabase.class);
        final DBPDriver driver = Mockito.mock(DBPDriver.class);
        session = Mockito.mock(JDBCSession.class);
        Mockito.when(schema.getDataSource()).thenReturn(dataSource);
        Mockito.when(schema.getDatabase()).thenReturn(database);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new SQLServerDialectMssql());
        Mockito.when(database.getDataSource()).thenReturn(dataSource);
        Mockito.when(container.getDriver()).thenReturn(driver);
        Mockito.when(driver.getId()).thenReturn("mssql");
        Mockito.when(driver.getSampleURL()).thenReturn("jdbc:sqlserver://localhost");
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(Mockito.mock(JDBCPreparedStatement.class));
    }

    @Test
    public void tableFilterDoesNotRestrictViewsOrTableTypes() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("TABLE%");
        Mockito.when(container.getEffectiveObjectFilter(SQLServerTable.class, schema, false)).thenReturn(tables);

        String sql = prepare(false);

        Assertions.assertTrue(sql.contains("(o.type IN ('U','S') AND (o.name LIKE N'TABLE%'))"));
        Assertions.assertTrue(sql.contains(" OR (o.type = 'V') OR (o.type = 'TT')"));
        Assertions.assertFalse(sql.contains("o.type = 'V' AND o.name"));
    }

    @Test
    public void viewFilterDoesNotRestrictTables() throws Exception {
        DBSObjectFilter views = new DBSObjectFilter();
        views.addInclude("VIEW%");
        Mockito.when(container.getEffectiveObjectFilter(SQLServerView.class, schema, false)).thenReturn(views);

        String sql = prepare(false);

        Assertions.assertTrue(sql.contains("(o.type IN ('U','S')) OR (o.type = 'V' AND (o.name LIKE N'VIEW%'))"));
        Assertions.assertTrue(sql.contains(" OR (o.type = 'TT')"));
    }

    @Test
    public void tableTypeFilterDoesNotRestrictTablesOrViews() throws Exception {
        DBSObjectFilter tableTypes = new DBSObjectFilter();
        tableTypes.addInclude("TYPE%");
        Mockito.when(container.getEffectiveObjectFilter(SQLServerTableType.class, schema, false)).thenReturn(tableTypes);

        String sql = prepare(false);

        Assertions.assertTrue(sql.contains("(o.type IN ('U','S')) OR (o.type = 'V') OR (o.type = 'TT' AND (o.name LIKE N'TYPE%'))"));
    }

    @Test
    public void tableAndViewFiltersApplyToTheirOwnRows() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("TABLE%");
        DBSObjectFilter views = new DBSObjectFilter();
        views.addInclude("VIEW%");
        Mockito.when(container.getEffectiveObjectFilter(SQLServerTable.class, schema, false)).thenReturn(tables);
        Mockito.when(container.getEffectiveObjectFilter(SQLServerView.class, schema, false)).thenReturn(views);

        String sql = prepare(false);

        Assertions.assertTrue(sql.contains(
            "(o.type IN ('U','S') AND (o.name LIKE N'TABLE%')) OR (o.type = 'V' AND (o.name LIKE N'VIEW%'))"));
        Assertions.assertTrue(sql.contains(" OR (o.type = 'TT')"));
    }

    @Test
    public void disabledFiltersLeaveTheListQueryUnchanged() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("TABLE%");
        tables.setEnabled(false);
        Mockito.when(container.getEffectiveObjectFilter(SQLServerTable.class, schema, false)).thenReturn(tables);

        String sql = prepare(false);

        Assertions.assertTrue(sql.contains("WHERE o.type IN ('U','S','V','TT') AND o.schema_id = 0"));
        Assertions.assertFalse(sql.contains(" AND ((o.type"));
    }

    @Test
    public void externalTablesHaveTheirOwnFilter() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("TABLE%");
        DBSObjectFilter externalTables = new DBSObjectFilter();
        externalTables.addInclude("EXTERNAL%");
        Mockito.when(container.getEffectiveObjectFilter(SQLServerTable.class, schema, false)).thenReturn(tables);
        Mockito.when(container.getEffectiveObjectFilter(SQLServerExternalTable.class, schema, false)).thenReturn(externalTables);

        String sql = prepare(true);

        Assertions.assertTrue(sql.contains(
            "o.type IN ('U','S') AND (t.is_external IS NULL OR t.is_external = 0) AND (o.name LIKE N'TABLE%')"));
        Assertions.assertTrue(sql.contains("o.type IN ('U','S') AND t.is_external = 1 AND (o.name LIKE N'EXTERNAL%')"));
        Assertions.assertTrue(sql.contains(" OR (o.type = 'V') OR (o.type = 'TT') OR "));
    }

    @Test
    public void includesAndExcludesStayWithinTheirObjectType() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("A%");
        tables.addInclude("B%");
        tables.addExclude("BAD%");
        Mockito.when(container.getEffectiveObjectFilter(SQLServerTable.class, schema, false)).thenReturn(tables);

        String sql = prepare(false);

        Assertions.assertTrue(sql.contains("o.type IN ('U','S') AND (o.name LIKE N'A%' OR o.name LIKE N'B%') AND o.name NOT LIKE N'BAD%'"));
        Assertions.assertTrue(sql.contains(" OR (o.type = 'V')"));
    }

    @Test
    public void convertsNavigatorWildcardsForEachObjectType() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("*");
        DBSObjectFilter views = new DBSObjectFilter();
        views.addInclude("V?EW*");
        DBSObjectFilter tableTypes = new DBSObjectFilter();
        tableTypes.addExclude("*");
        DBSObjectFilter externalTables = new DBSObjectFilter();
        externalTables.addExclude("OLD*");
        Mockito.when(container.getEffectiveObjectFilter(SQLServerTable.class, schema, false)).thenReturn(tables);
        Mockito.when(container.getEffectiveObjectFilter(SQLServerView.class, schema, false)).thenReturn(views);
        Mockito.when(container.getEffectiveObjectFilter(SQLServerTableType.class, schema, false)).thenReturn(tableTypes);
        Mockito.when(container.getEffectiveObjectFilter(SQLServerExternalTable.class, schema, false)).thenReturn(externalTables);

        String sql = prepare(true);

        Assertions.assertTrue(sql.contains("(t.is_external IS NULL OR t.is_external = 0) AND (o.name LIKE N'%')"));
        Assertions.assertTrue(sql.contains("o.type = 'V' AND (o.name LIKE N'V_EW%')"));
        Assertions.assertTrue(sql.contains("o.type = 'TT' AND o.name NOT LIKE N'%'"));
        Assertions.assertTrue(sql.contains("t.is_external = 1 AND o.name NOT LIKE N'OLD%'"));
        Assertions.assertFalse(sql.contains("LIKE N'*'"));
    }

    @Test
    public void exactLookupIgnoresFilters() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("TABLE%");
        Mockito.when(container.getEffectiveObjectFilter(SQLServerTable.class, schema, false)).thenReturn(tables);

        SQLServerTableBase object = Mockito.mock(SQLServerTableBase.class);
        Mockito.when(object.getName()).thenReturn("OTHER_TABLE");
        SQLServerSchema.TableCache cache = Mockito.mock(SQLServerSchema.TableCache.class, Mockito.CALLS_REAL_METHODS);
        cache.prepareLookupStatement(session, schema, object, null);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());

        Assertions.assertTrue(sql.getValue().contains("AND o.name = N'OTHER_TABLE'"));
        Assertions.assertFalse(sql.getValue().contains("TABLE%"));
        Mockito.verify(container, Mockito.never()).getEffectiveObjectFilter(
            Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @NotNull
    private String prepare(boolean supportsExternalTables) throws Exception {
        Mockito.when(dataSource.supportsExternalTables(session)).thenReturn(supportsExternalTables);
        SQLServerSchema.TableCache cache = Mockito.mock(SQLServerSchema.TableCache.class, Mockito.CALLS_REAL_METHODS);
        cache.prepareLookupStatement(session, schema, null, null);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());
        return sql.getValue();
    }
}
