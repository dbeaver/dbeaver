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
package org.jkiss.dbeaver.ext.manticore.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManticoreQueryTransformerLimitTest extends DBeaverUnitTest {
    @Test
    void registersOnlyFinitePagination() {
        ManticoreDataSource dataSource = mock(ManticoreDataSource.class, CALLS_REAL_METHODS);
        assertInstanceOf(ManticoreQueryTransformerLimit.class,
            dataSource.createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT));
        assertNull(dataSource.createQueryTransformer(DBCQueryTransformType.FETCH_ALL_TABLE));
    }

    @ParameterizedTest
    @CsvSource({"0,200", "200,200", "800,200"})
    void addsPageLimit(long offset, long length) throws Exception {
        DBCQueryTransformer transformer = transformer(offset, length);
        assertEquals("SELECT * FROM test\nLIMIT " + offset + ", " + length,
            transformer.transformQueryString(new SQLQuery(null, "SELECT * FROM test")));
        DBCStatement statement = mock(DBCStatement.class);
        transformer.transformStatement(statement, 0);
        verifyNoInteractions(statement);
    }

    @ParameterizedTest
    @CsvSource({"900,200,1100", "1000,200,1200", "0,2000,2000", "10000,200,10200"})
    void expandsWindowToIncludeTheRequestedPage(long offset, long length, long window) throws Exception {
        DBCQueryTransformer transformer = transformer(offset, length);
        assertEquals("SELECT id FROM test ORDER BY id\nLIMIT " + offset + ", " + length + "\nOPTION max_matches=" + window,
            transformer.transformQueryString(new SQLQuery(null, "SELECT id FROM test ORDER BY id")));
        DBCStatement statement = mock(DBCStatement.class);
        transformer.transformStatement(statement, 0);
        verifyNoInteractions(statement);
    }

    @Test
    void preservesExplicitLimit() throws Exception {
        String sql = "SELECT * FROM test LIMIT 10";
        DBCQueryTransformer transformer = transformer(1000, 200);
        assertEquals(sql, transformer.transformQueryString(new SQLQuery(null, sql)));
        DBCStatement statement = mock(DBCStatement.class);
        transformer.transformStatement(statement, 0);
        verify(statement).setLimit(1000, 200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT * FROM (SELECT * FROM test LIMIT 10) t",
        "SELECT ' LIMIT 10 ' FROM test",
        "SHOW TABLES",
        "DELETE FROM test"
    })
    void doesNotAddAnOptionWhenTheSharedTransformerDeclines(@NotNull String sql) throws Exception {
        assertEquals(sql, transformer(1000, 200).transformQueryString(new SQLQuery(null, sql)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT * FROM (SELECT * FROM test) ORDER BY id",
        "SELECT category, count(*) FROM test GROUP BY category",
        "SELECT a.id FROM test a JOIN other b ON a.id=b.id",
        "WITH t AS (SELECT * FROM test) SELECT * FROM t"
    })
    void leavesComplexQueryWindowSizingUnchanged(@NotNull String sql) throws Exception {
        assertEquals(sql + "\nLIMIT 1000, 200", transformer(1000, 200).transformQueryString(new SQLQuery(null, sql)));
    }

    @Test
    void retainsTheSharedDelimiterHandling() throws Exception {
        assertEquals("SELECT * FROM test\nLIMIT 1000, 200\nOPTION max_matches=1200",
            transformer(1000, 200).transformQueryString(new SQLQuery(null, "SELECT * FROM test;")));
    }

    @Test
    void recomputesTheWindowWhenParametersChange() throws Exception {
        DBCQueryTransformer transformer = transformer(1000, 200);
        SQLQuery query = new SQLQuery(null, "SELECT * FROM test");
        assertEquals("SELECT * FROM test\nLIMIT 1000, 200\nOPTION max_matches=1200", transformer.transformQueryString(query));
        transformer.setParameters(0, 200);
        assertEquals("SELECT * FROM test\nLIMIT 0, 200", transformer.transformQueryString(query));
    }

    @Test
    void doesNotOverflowTheServerWindow() throws Exception {
        DBCQueryTransformer transformer = transformer(Integer.MAX_VALUE, Long.MAX_VALUE);
        assertFalse(transformer.transformQueryString(new SQLQuery(null, "SELECT * FROM test")).contains("OPTION"));
    }

    @Test
    void sharedStatementCreationUsesTheExpandedWindow() throws Exception {
        DBCSession session = session();
        DBCStatement statement = mock(DBCStatement.class);
        String sql = "SELECT * FROM test\nLIMIT 1000, 200\nOPTION max_matches=1200";
        when(session.prepareStatement(DBCStatementType.QUERY, sql, false, false, false)).thenReturn(statement);

        assertSame(statement, DBUtils.makeStatement(null, session, DBCStatementType.QUERY,
            new SQLQuery(session.getDataSource(), "SELECT * FROM test"), 1000, 200));

        verify(session).prepareStatement(DBCStatementType.QUERY, sql, false, false, false);
        verify(statement, never()).setLimit(anyLong(), anyLong());
    }

    @Test
    void queriesWithExistingManticoreOptionsBypassTheSharedTransformationPath() throws Exception {
        DBCSession session = session();
        DBCStatement statement = mock(DBCStatement.class);
        String sql = "SELECT * FROM test OPTION max_matches=2000";
        when(session.prepareStatement(DBCStatementType.QUERY, sql, false, false, false)).thenReturn(statement);

        assertSame(statement, DBUtils.makeStatement(null, session, DBCStatementType.QUERY,
            new SQLQuery(session.getDataSource(), sql), 0, 200));

        verify(session).prepareStatement(DBCStatementType.QUERY, sql, false, false, false);
        verify(statement).setLimit(0, 200);
    }

    @NotNull
    private static DBCSession session() {
        SimplePreferenceStore preferences = new SimplePreferenceStore() {
            @Override
            public void save() {
            }
        };
        preferences.setValue(ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL, true);
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);
        when(container.getPreferenceStore()).thenReturn(preferences);
        ManticoreDataSource dataSource = mock(ManticoreDataSource.class);
        when(dataSource.getContainer()).thenReturn(container);
        when(dataSource.getSQLDialect()).thenReturn(new ManticoreSQLDialect());
        doCallRealMethod().when(dataSource).createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
        DBCSession session = mock(DBCSession.class);
        when(session.getDataSource()).thenReturn(dataSource);
        return session;
    }

    @NotNull
    private static DBCQueryTransformer transformer(long offset, long length) {
        ManticoreDataSource dataSource = mock(ManticoreDataSource.class, CALLS_REAL_METHODS);
        DBCQueryTransformer transformer = dataSource.createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
        assertNotNull(transformer);
        transformer.setParameters(offset, length);
        return transformer;
    }
}
