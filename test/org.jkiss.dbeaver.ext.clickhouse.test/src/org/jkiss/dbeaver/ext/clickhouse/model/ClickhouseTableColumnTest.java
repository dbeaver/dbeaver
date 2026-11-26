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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Types;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClickhouseTableColumnTest extends DBeaverUnitTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "Nullable(String)",
        "Nullable(Decimal(18, 4))",
        "LowCardinality(Nullable(String))",
        "Decimal(18, 4)",
        "DateTime64(3)",
        "Array(Nullable(String))",
        "String",
        "UInt32"
    })
    public void importedColumnDeclarationPreservesTypeArguments(@NotNull String typeName) throws DBException {
        var dataSource = mock(ClickhouseDataSource.class);
        when(dataSource.getSQLDialect()).thenReturn(new ClickhouseSQLDialect());
        when(dataSource.getMetaModel()).thenReturn(new ClickhouseMetaModel());
        var table = mock(ClickhouseTable.class);
        when(table.getDataSource()).thenReturn(dataSource);
        var column = new ClickhouseTableColumn(
            table, "value_column", "String", Types.VARCHAR, Types.VARCHAR, 0,
            0, 0, null, null, 10, false, null, null, false, false);

        // Data import sets the full type on a newly created target column before generating DDL.
        column.setFullTypeName(typeName);

        var manager = new GenericTableColumnManager();
        var options = Map.<String, Object>of();
        var declaration = manager.getNestedDeclaration(
            monitor, table, manager.makeCreateCommand(column, options), options);
        assertEquals("value_column " + typeName, declaration.toString());
        assertEquals(typeName, column.getFullTypeName());
    }
}
