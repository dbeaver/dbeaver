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
package org.jkiss.dbeaver.ext.gbase8s.model.meta;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class GBase8sTableFilterTest {
    private final GBase8sMetaModel metaModel = new GBase8sMetaModel();
    private GenericStructContainer owner;
    private DBPDataSourceContainer container;
    private JDBCSession session;
    private JDBCPreparedStatement statement;

    @BeforeEach
    public void setUp() throws Exception {
        owner = Mockito.mock(GenericStructContainer.class);
        final GenericDataSource dataSource = Mockito.mock(GenericDataSource.class);
        container = Mockito.mock(DBPDataSourceContainer.class);
        session = Mockito.mock(JDBCSession.class);
        statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getAllObjectsPattern()).thenReturn("%");
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new GenericSQLDialect());
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(container.getConnectionConfiguration()).thenReturn(new DBPConnectionConfiguration());
        Mockito.when(container.getDriver()).thenReturn(Mockito.mock(DBPDriver.class));
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);
    }

    @Test
    public void tableMaskLeavesViewsUnrestricted() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("TABLE*");
        Mockito.when(container.getEffectiveObjectFilter(GenericTable.class, owner, false)).thenReturn(tables);

        String sql = prepare(null);

        Assertions.assertTrue(sql.contains("(t.tabtype = 'T' AND t.tabname LIKE ?) OR (t.tabtype = 'V' AND t.tabname LIKE ?)"));
        verifyBindings("TABLE%", "%", "T", "V");
    }

    @Test
    public void tableMaskKeepsViewsVisibleWhenDefaultPatternIsNull() throws Exception {
        Mockito.when(owner.getDataSource().getAllObjectsPattern()).thenReturn(null);
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("TABLE*");
        Mockito.when(container.getEffectiveObjectFilter(GenericTable.class, owner, false)).thenReturn(tables);

        prepare(null);

        verifyBindings("TABLE%", "%", "T", "V");
    }

    @Test
    public void viewMaskLeavesTablesUnrestricted() throws Exception {
        DBSObjectFilter views = new DBSObjectFilter();
        views.addInclude("VIEW?");
        Mockito.when(container.getEffectiveObjectFilter(GenericView.class, owner, false)).thenReturn(views);

        prepare(null);

        verifyBindings("%", "VIEW_", "T", "V");
    }

    @Test
    public void eachMaskAppliesOnlyToItsObjectType() throws Exception {
        DBSObjectFilter tables = new DBSObjectFilter();
        tables.addInclude("TABLE*");
        DBSObjectFilter views = new DBSObjectFilter();
        views.addInclude("VIEW*");
        Mockito.when(container.getEffectiveObjectFilter(GenericTable.class, owner, false)).thenReturn(tables);
        Mockito.when(container.getEffectiveObjectFilter(GenericView.class, owner, false)).thenReturn(views);

        prepare(null);

        verifyBindings("TABLE%", "VIEW%", "T", "V");
    }

    @Test
    public void unfilteredAndExactLookupsKeepTheirOriginalBindings() throws Exception {
        String unfilteredSql = prepare(null);
        Assertions.assertTrue(unfilteredSql.contains("WHERE t.tabname LIKE ?"));
        Assertions.assertFalse(unfilteredSql.contains("WHERE ((t.tabtype = 'T'"));
        verifyBindings("%", "T", "V");

        Mockito.clearInvocations(session, statement, container);
        String exactSql = prepare("REFERENCED_TABLE");
        Assertions.assertTrue(exactSql.contains("WHERE t.tabname LIKE ?"));
        verifyBindings("REFERENCED_TABLE", "T", "V");
        Mockito.verify(container, Mockito.never()).getEffectiveObjectFilter(
            Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void synonymLookupDoesNotUseTableOrViewFilters() throws Exception {
        metaModel.prepareSynonymsLoadStatement(session, owner);
        verifyBindings("%", "S");
        Mockito.verify(container, Mockito.never()).getEffectiveObjectFilter(
            Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @NotNull
    private String prepare(@Nullable String objectName) throws Exception {
        metaModel.prepareTableLoadStatement(session, owner, null, objectName);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());
        return sql.getValue();
    }

    private void verifyBindings(@NotNull String... masks) throws Exception {
        for (int i = 0; i < masks.length; i++) {
            Mockito.verify(statement).setString(i + 1, masks[i]);
        }
        Mockito.verify(statement, Mockito.times(masks.length)).setString(Mockito.anyInt(), Mockito.anyString());
    }
}
