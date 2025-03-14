/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MySQLDialectTest extends DBeaverUnitTest {

    private final MySQLDialect dialect = new MySQLDialect();

    @Test
    public void quoteStringTest() {
        assertEquals("`a ' b`", dialect.getQuotedIdentifier("a ' b", false, true));
        assertEquals("`a `` b`", dialect.getQuotedIdentifier("a ` b", false, true));
        assertEquals("`a b`", dialect.getQuotedIdentifier("a b", false, true));
    }

    @Test
    public void escapeString_whenSimpleStringPasses_thenSameStringReturn(){
        // GIVEN
        String expected = "There is simple string without any quotes and slashes";
        // WHEN
        String actual = dialect.escapeString(expected);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    public void escapeString_whenStringWithQuotes_thenSameStringReturn() {
        // GIVEN
        String given = "[\"{\"subjectId\":3,\"levelId\":2,\"isOur\":true}\"]";
        // WHEN
        String actual = dialect.escapeString(given);
        // THEN
        assertEquals("[\"{\"subjectId\":3,\"levelId\":2,\"isOur\":true}\"]", actual);
    }

    @Test
    public void escapeString_whenStringWithinSlash_thenStringEscapedSlashReturn() {
        // GIVEN
        String given = "[\"{\\\"subjectId\\\":3,\\\"levelId\\\":2,\\\"isOur\\\":true}\"]";
        // WHEN
        String actual = dialect.escapeString(given);
        // THEN
        assertEquals("[\"{\\\\\"subjectId\\\\\":3,\\\\\"levelId\\\\\":2,\\\\\"isOur\\\\\":true}\"]", actual);
    }
}
