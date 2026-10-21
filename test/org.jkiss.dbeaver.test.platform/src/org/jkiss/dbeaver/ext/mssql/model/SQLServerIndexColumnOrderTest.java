/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class SQLServerIndexColumnOrderTest extends DBeaverUnitTest {

    @Test
    public void keyColumnsPrecedeIncludedColumnsInOrdinalOrder() {
        List<SQLServerTableIndexColumn> columns = new ArrayList<>(List.of(
            column(7, 0, true),
            column(3, 2, false),
            column(6, 0, true),
            column(2, 1, false),
            column(8, 0, true)
        ));

        SQLServerSchema.sortIndexColumns(columns);

        Assertions.assertEquals(List.of(2L, 3L, 6L, 7L, 8L),
            columns.stream().map(SQLServerTableIndexColumn::getObjectId).toList());
        Assertions.assertEquals(List.of(false, false, true, true, true),
            columns.stream().map(SQLServerTableIndexColumn::isIncluded).toList());
        // Keep the original SQL Server metadata: INCLUDE columns still have key_ordinal = 0.
        Assertions.assertEquals(List.of(1, 2, 0, 0, 0),
            columns.stream().map(SQLServerTableIndexColumn::getOrdinalPosition).toList());
    }

    @Test
    public void keyOnlyIndexKeepsKeyOrdinalOrder() {
        List<SQLServerTableIndexColumn> columns = new ArrayList<>(List.of(
            column(4, 3, false),
            column(2, 1, false),
            column(3, 2, false)
        ));

        SQLServerSchema.sortIndexColumns(columns);

        Assertions.assertEquals(List.of(2L, 3L, 4L),
            columns.stream().map(SQLServerTableIndexColumn::getObjectId).toList());
    }

    private static SQLServerTableIndexColumn column(long indexColumnId, int ordinal, boolean included) {
        // Sorting needs only the metadata; no SQL Server connection or index object is needed.
        return new SQLServerTableIndexColumn(null, indexColumnId, null, ordinal, true, included);
    }
}
