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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManticoreOrderingTest extends DBeaverUnitTest {

    @ParameterizedTest
    @CsvSource({
        "true, false, id ASC",
        "true, true, id DESC",
        "false, false, id",
        "false, true, id DESC"
    })
    void generatesDirectionOnlyWhenRequired(boolean manticore, boolean descending, @NotNull String expectedOrder) throws Exception {
        DBPDataSource dataSource = dataSource(manticore ? new ManticoreSQLDialect() : BasicSQLDialect.INSTANCE);
        DBDDataFilter filter = new DBDDataFilter(List.of(constraint("id", 1, descending)));

        StringBuilder query = new StringBuilder("SELECT * FROM test");
        SQLUtils.appendQueryOrder(dataSource, query, null, filter);
        assertEquals("SELECT * FROM test\nORDER BY " + expectedOrder, query.toString());

        String parsedOrder = manticore ? expectedOrder : expectedOrder.replace("id", "\"id\"");
        assertEquals("SELECT * FROM test ORDER BY " + parsedOrder,
            SQLSemanticProcessor.injectFiltersToQuery(new VoidProgressMonitor(), dataSource, "SELECT * FROM test", filter));

        assertEquals("SELECT * FROM (\nSELECT * FROM test\n) z_q ORDER BY z_q." + expectedOrder,
            dataSource.getSQLDialect().getQueryGenerator().getWrappedFilterQuery(dataSource, "SELECT * FROM test", filter));
    }

    @Test
    void preservesMixedColumnDirections() throws Exception {
        DBPDataSource dataSource = dataSource(new ManticoreSQLDialect());
        DBDDataFilter filter = new DBDDataFilter(List.of(
            constraint("category", 1, false),
            constraint("id", 2, true)
        ));

        StringBuilder order = new StringBuilder();
        SQLUtils.appendOrderString(filter, dataSource, null, false, order);
        assertEquals("category ASC,id DESC", order.toString());
        assertEquals("SELECT * FROM test ORDER BY category ASC, id DESC",
            SQLSemanticProcessor.injectFiltersToQuery(new VoidProgressMonitor(), dataSource, "SELECT * FROM test", filter));
    }

    @Test
    void preservesCustomOrdering() throws Exception {
        DBPDataSource dataSource = dataSource(new ManticoreSQLDialect());
        DBDDataFilter filter = new DBDDataFilter();
        filter.setOrder("id DESC");

        StringBuilder order = new StringBuilder();
        SQLUtils.appendOrderString(filter, dataSource, null, false, order);
        assertEquals("id DESC", order.toString());
        assertEquals("SELECT * FROM test ORDER BY id DESC",
            SQLSemanticProcessor.injectFiltersToQuery(new VoidProgressMonitor(), dataSource, "SELECT * FROM test", filter));
    }

    @NotNull
    private static DBDAttributeConstraint constraint(@NotNull String name, int position, boolean descending) {
        DBDAttributeConstraint constraint = new DBDAttributeConstraint(name, position - 1);
        constraint.setOrderPosition(position);
        constraint.setOrderDescending(descending);
        return constraint;
    }

    @NotNull
    private static DBPDataSource dataSource(@NotNull SQLDialect dialect) {
        DBPDataSource dataSource = mock(DBPDataSource.class);
        when(dataSource.getSQLDialect()).thenReturn(dialect);
        return dataSource;
    }
}
