/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class SQLQueryDropDetectionTest extends DBeaverUnitTest {


    @Test
    public void noDropStatementShouldReturnNoneTypeDrop() {
        // given
        var query = new SQLQuery(null, "SELECT * FROM table WHERE id = ?");
        // then
        assertTrue(query.getDropType().isEmpty());
    }

    @Test
    public void dropTableStatementShouldReturnTableDropType() {
        // given
        var query = new SQLQuery(null, "DROP table users");
        // then
        assertDropType(query, "table");
    }

    @Test
    public void dropSchemaStatementShouldReturnDropType() {
        // given
        var query = new SQLQuery(null, "DROP schema users");
        // then
        assertDropType(query, "schema");
    }

    @Test
    private void assertDropType(@NotNull SQLQuery query, @NotNull String expectedType) {
        assertTrue("Drop type must be present in query: " + query, query.getDropType().isPresent());
        assertEquals(expectedType, query.getDropType().get());
    }


}
