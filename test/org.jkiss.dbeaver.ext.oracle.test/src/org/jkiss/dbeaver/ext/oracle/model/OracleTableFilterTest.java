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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OracleTableFilterTest extends DBeaverUnitTest {

    private DBPDataSourceContainer container;
    private DBPPreferenceStore preferences;
    private OracleSchema schema;
    private JDBCSession session;
    private JDBCPreparedStatement statement;

    @BeforeEach
    public void setUp() throws Exception {
        container = Mockito.mock(DBPDataSourceContainer.class);
        preferences = Mockito.mock(DBPPreferenceStore.class);
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
    public void filtersEachObjectTypeInNormalAndUnionQueries() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("ABC");
        tables.addInclude("*JOB_1234*");
        tables.addExclude("BAD*");
        tables.addExclude("OLD");
        Mockito.when(container.getObjectFilter(OracleTable.class, schema, false)).thenReturn(tables);
        DBSObjectFilter views = new DBSObjectFilter();
        views.addExclude("HIDDEN*");
        Mockito.when(container.getObjectFilter(OracleView.class, schema, false)).thenReturn(views);
        DBSObjectFilter materializedViews = new DBSObjectFilter();
        materializedViews.addInclude("MV?");
        Mockito.when(container.getObjectFilter(OracleMaterializedView.class, schema, false)).thenReturn(materializedViews);

        for (boolean useUnion : new boolean[] {false, true}) {
            container.getConnectionConfiguration().setProviderProperty(
                OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY, Boolean.toString(useUnion));
            Mockito.clearInvocations(session, statement);
            String sql = prepare(null);
            schema.getTableCache().setFullCache(true);
            Assertions.assertTrue(schema.getTableCache().isFullyCached());
            if (useUnion) {
                Assertions.assertTrue(sql.contains("UPPER(t.TABLE_NAME)=UPPER(?) OR UPPER(t.TABLE_NAME) LIKE UPPER(?)"));
                Assertions.assertTrue(sql.contains("AND NOT (t.TABLE_NAME LIKE ?"));
                Assertions.assertTrue(sql.contains("o.OBJECT_NAME LIKE ?"));
                Assertions.assertTrue(sql.contains("UPPER(o.OBJECT_NAME) LIKE UPPER(?)"));
                Assertions.assertTrue(sql.contains("UNION ALL"));
                verifyBindings("TEST_SCHEMA", "ABC", "%JOB_1234%", "BAD%", "OLD",
                    "TEST_SCHEMA", "HIDDEN%", "TEST_SCHEMA", "MV_");
            } else {
                Assertions.assertTrue(sql.contains(
                    "O.OBJECT_TYPE='TABLE' AND (UPPER(O.OBJECT_NAME)=UPPER(?) OR UPPER(O.OBJECT_NAME) LIKE UPPER(?)"));
                Assertions.assertTrue(sql.contains("AND NOT (O.OBJECT_NAME LIKE ?"));
                Assertions.assertTrue(sql.contains("O.OBJECT_TYPE='VIEW' AND NOT (O.OBJECT_NAME LIKE ?"));
                Assertions.assertTrue(sql.contains("O.OBJECT_TYPE='MATERIALIZED VIEW' AND (UPPER(O.OBJECT_NAME) LIKE UPPER(?)"));
                verifyBindings("TEST_SCHEMA", "ABC", "%JOB_1234%", "BAD%", "OLD", "HIDDEN%", "MV_");
            }
        }
    }

    @Test
    public void disabledFiltersAndClientSidePreferenceLeaveListQueryUnfiltered() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("MATCH*");
        tables.setEnabled(false);
        Mockito.when(container.getObjectFilter(OracleTable.class, schema, false)).thenReturn(tables);
        String sql = prepare(null);
        Assertions.assertFalse(sql.contains("O.OBJECT_NAME LIKE"));
        schema.getTableCache().setFullCache(true);
        Assertions.assertTrue(schema.getTableCache().isFullyCached());

        tables.setEnabled(true);
        Mockito.when(preferences.getBoolean(ModelPreferences.META_USE_SERVER_SIDE_FILTERS)).thenReturn(false);
        Mockito.clearInvocations(session, statement);
        sql = prepare(null);
        Assertions.assertFalse(sql.contains("O.OBJECT_NAME LIKE"));
        verifyBindings("TEST_SCHEMA");
    }

    @Test
    public void globalFilterIsOverriddenBySchemaFilterAndDoesNotRestrictExactLookup() throws Exception {
        DBSObjectFilter global = new DBSObjectFilter();
        global.addInclude("GLOBAL*");
        DBSObjectFilter local = new DBSObjectFilter();
        local.addInclude("LOCAL*");
        Mockito.when(container.getObjectFilter(OracleTable.class, schema, false)).thenReturn(local);
        Mockito.when(container.getObjectFilter(OracleView.class, schema, false)).thenReturn(global);

        String sql = prepare(null);
        Assertions.assertTrue(sql.contains("O.OBJECT_TYPE='TABLE' AND (UPPER(O.OBJECT_NAME) LIKE UPPER(?)"));
        Assertions.assertTrue(sql.contains("O.OBJECT_TYPE='VIEW' AND (UPPER(O.OBJECT_NAME) LIKE UPPER(?)"));
        verifyBindings("TEST_SCHEMA", "LOCAL%", "GLOBAL%");
        schema.getTableCache().setFullCache(true);
        Assertions.assertTrue(schema.getTableCache().isFullyCached());

        Mockito.clearInvocations(session, statement);
        sql = prepare("REFERENCED_TABLE");
        Assertions.assertTrue(sql.contains("O.OBJECT_NAME=?"));
        Assertions.assertFalse(sql.contains("LIKE UPPER(?)"));
        verifyBindings("TEST_SCHEMA", "REFERENCED_TABLE");
        schema.getTableCache().clearCache();
        schema.getTableCache().setFullCache(true);
        Assertions.assertTrue(schema.getTableCache().isFullyCached());
    }

    @Test
    public void unionQueryUsesExactNameForReferenceLookupAndLeavesUnfilteredBranchesOpen() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("FOO*");
        container.getConnectionConfiguration().setProviderProperty(
            OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY, "true");
        Mockito.when(container.getObjectFilter(OracleTable.class, schema, false)).thenReturn(tables);

        String sql = prepare(null);
        Assertions.assertTrue(sql.contains("UPPER(t.TABLE_NAME) LIKE UPPER(?)"));
        Assertions.assertFalse(sql.contains("o.OBJECT_NAME LIKE ?"));
        verifyBindings("TEST_SCHEMA", "FOO%", "TEST_SCHEMA", "TEST_SCHEMA");

        Mockito.clearInvocations(session, statement);
        sql = prepare("REF_TABLE");
        Assertions.assertTrue(sql.contains("WHERE OBJECT_NAME=?"));
        Assertions.assertFalse(sql.contains("LIKE UPPER(?)"));
        verifyBindings("TEST_SCHEMA", "TEST_SCHEMA", "TEST_SCHEMA", "REF_TABLE");
    }

    @Test
    public void emptyAndEscapedMasksKeepIncludeExcludeSemantics() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("");
        tables.addInclude("A\\_B*");
        tables.addExclude("");
        tables.addExclude("OLD?");
        Mockito.when(container.getObjectFilter(OracleTable.class, schema, false)).thenReturn(tables);

        String sql = prepare(null);
        Assertions.assertTrue(sql.contains("O.OBJECT_TYPE='TABLE' AND (UPPER(O.OBJECT_NAME) LIKE UPPER(?) ESCAPE"));
        Assertions.assertTrue(sql.contains("AND NOT (O.OBJECT_NAME LIKE ? ESCAPE"));
        verifyBindings("TEST_SCHEMA", "A\\_B%", "OLD_");

        tables.setInclude(List.of(""));
        Mockito.clearInvocations(session, statement);
        sql = prepare(null);
        Assertions.assertTrue(sql.contains("O.OBJECT_TYPE='TABLE' AND (1=0)"));
        verifyBindings("TEST_SCHEMA", "OLD_");
    }

    @Test
    public void escapedGlobCharactersRemainLiteralInBothQueryModes() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("LITERAL\\*NAME");
        tables.addInclude("LITERAL\\?NAME*");
        tables.addInclude("ESCAPED\\_NAME%");
        tables.addExclude("LITERAL\\?NAME_BAD*");
        Mockito.when(container.getObjectFilter(OracleTable.class, schema, false)).thenReturn(tables);
        tables.setCaseSensitive(true);
        Assertions.assertTrue(tables.matches("LITERAL*NAME"));
        Assertions.assertFalse(tables.matches("LITERAL%NAME"));
        Assertions.assertTrue(tables.matches("LITERAL?NAME_GOOD"));
        Assertions.assertFalse(tables.matches("LITERAL?NAME_BAD1"));

        for (boolean useUnion : new boolean[] {false, true}) {
            container.getConnectionConfiguration().setProviderProperty(
                OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY, Boolean.toString(useUnion));
            Mockito.clearInvocations(session, statement);
            String sql = prepare(null);
            String column = useUnion ? "t.TABLE_NAME" : "O.OBJECT_NAME";
            Assertions.assertTrue(sql.contains(column + " LIKE ? ESCAPE"));
            Assertions.assertTrue(sql.contains(" AND NOT (" + column + " LIKE ? ESCAPE"));
            if (useUnion) {
                verifyBindings("TEST_SCHEMA", "LITERAL*NAME", "LITERAL?NAME%", "ESCAPED\\_NAME%",
                    "LITERAL?NAME_BAD%", "TEST_SCHEMA", "TEST_SCHEMA");
            } else {
                verifyBindings("TEST_SCHEMA", "LITERAL*NAME", "LITERAL?NAME%", "ESCAPED\\_NAME%",
                    "LITERAL?NAME_BAD%");
            }
        }
    }

    @Test
    public void wildcardIncludesFollowCaseSensitivityWhileExcludesRemainConservative() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("foo*");
        tables.addExclude("bar*");
        Mockito.when(container.getObjectFilter(OracleTable.class, schema, false)).thenReturn(tables);

        for (boolean useUnion : new boolean[] {false, true}) {
            container.getConnectionConfiguration().setProviderProperty(
                OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY, Boolean.toString(useUnion));
            String column = useUnion ? "t.TABLE_NAME" : "O.OBJECT_NAME";
            for (boolean caseSensitive : new boolean[] {false, true}) {
                tables.setCaseSensitive(caseSensitive);
                Mockito.clearInvocations(session, statement);
                String sql = prepare(null);
                String include = caseSensitive ? column + " LIKE ? ESCAPE" : "UPPER(" + column + ") LIKE UPPER(?) ESCAPE";
                Assertions.assertTrue(sql.contains("AND (" + include));
                Assertions.assertTrue(sql.contains("AND NOT (" + column + " LIKE ? ESCAPE"));
                Assertions.assertFalse(sql.contains("AND NOT (UPPER(" + column + ") LIKE UPPER(?)"));
                if (useUnion) {
                    verifyBindings("TEST_SCHEMA", "foo%", "bar%", "TEST_SCHEMA", "TEST_SCHEMA");
                } else {
                    verifyBindings("TEST_SCHEMA", "foo%", "bar%");
                }
            }
        }

        tables.setInclude(null);
        tables.setCaseSensitive(false);
        Mockito.clearInvocations(session, statement);
        String sql = prepare(null);
        Assertions.assertTrue(sql.contains("AND NOT (t.TABLE_NAME LIKE ? ESCAPE"));
        Assertions.assertFalse(sql.contains("UPPER(t.TABLE_NAME) LIKE UPPER(?)"));
        verifyBindings("TEST_SCHEMA", "bar%", "TEST_SCHEMA", "TEST_SCHEMA");
    }

    @Test
    public void filteredFullCacheDoesNotResolveMissingTableNames() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("VISIBLE*");
        Mockito.when(container.getObjectFilter(OracleTable.class, schema, false)).thenReturn(tables);
        Mockito.when(container.isConnected()).thenReturn(true);
        AtomicInteger lookups = new AtomicInteger();
        OracleSchema.TableCache cache = schema.new TableCache() {
            @Override
            @NotNull
            protected OracleTableBase reloadObject(
                @NotNull DBRProgressMonitor monitor,
                @NotNull OracleSchema owner,
                @Nullable OracleTableBase object,
                @Nullable String objectName
            ) {
                lookups.incrementAndGet();
                return new OracleTable(owner, objectName);
            }
        };
        cache.prepareLookupStatement(session, schema, null, null);
        OracleTableBase previouslyResolved = cache.getObject(monitor, schema, "REFERENCED_TABLE");
        cache.setFullCache(true);
        Assertions.assertSame(previouslyResolved, cache.getObject(monitor, schema, "REFERENCED_TABLE"));
        Assertions.assertNull(cache.getObject(monitor, schema, "FILTERED_OUT_TABLE"));
        Assertions.assertEquals(1, lookups.get());
    }

    @NotNull
    private String prepare(@Nullable String objectName) throws Exception {
        schema.getTableCache().prepareLookupStatement(session, schema, null, objectName);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());
        return sql.getValue();
    }

    private void verifyBindings(@NotNull String... values) throws Exception {
        for (int i = 0; i < values.length; i++) {
            Mockito.verify(statement).setString(i + 1, values[i]);
        }
        Mockito.verify(statement, Mockito.times(values.length)).setString(Mockito.anyInt(), Mockito.anyString());
    }
}
