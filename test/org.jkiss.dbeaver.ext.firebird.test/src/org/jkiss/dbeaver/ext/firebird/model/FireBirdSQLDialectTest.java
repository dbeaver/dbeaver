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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class FireBirdSQLDialectTest extends DBeaverUnitTest {

    private FireBirdSQLDialect dialect;

    @Before
    public void setUp() {
        dialect = new FireBirdSQLDialect();
    }

    // ---- Block headers and bounds ----

    @Test
    public void testBlockHeaderStringsContainExecuteBlock() {
        String[] headers = dialect.getBlockHeaderStrings();
        assertNotNull("Block headers should not be null", headers);
        List<String> headerList = Arrays.asList(headers);
        assertTrue("Block headers must contain EXECUTE BLOCK", headerList.contains("EXECUTE BLOCK"));
    }

    @Test
    public void testBlockBoundStringsContainBeginEnd() {
        String[][] bounds = dialect.getBlockBoundStrings();
        assertNotNull("Block bounds should not be null", bounds);
        assertEquals("Should have exactly one BEGIN/END pair", 1, bounds.length);
        assertEquals("BEGIN", bounds[0][0]);
        assertEquals("END", bounds[0][1]);
    }

    // ---- Boolean behavior methods ----

    @Test
    public void testSupportsAliasInSelect() {
        assertTrue("Firebird supports alias in SELECT", dialect.supportsAliasInSelect());
    }

    @Test
    public void testSupportsAliasInHaving() {
        assertFalse("Firebird does not support alias in HAVING", dialect.supportsAliasInHaving());
    }

    @Test
    public void testSupportsInsertAllDefaultValues() {
        assertTrue("Firebird supports INSERT with all default values",
            dialect.supportsInsertAllDefaultValuesStatement());
    }

    // ---- Identifier handling ----

    @Test
    public void testValidIdentifierPartAcceptsDollarSign() {
        assertTrue("Dollar sign must be valid in identifiers", dialect.validIdentifierPart('$', false));
    }

    @Test
    public void testValidIdentifierPartAcceptsUnderscore() {
        assertTrue("Underscore must be valid in identifiers", dialect.validIdentifierPart('_', false));
    }

    @Test
    public void testValidIdentifierPartRejectsSpace() {
        assertFalse("Space must not be valid in unquoted identifiers", dialect.validIdentifierPart(' ', false));
    }

    // ---- Keyword array structure (via reflection) ----

    @Test
    public void testFirebirdKeywordsArrayIsSorted() throws Exception {
        String[] keywords = getStaticStringArray("FIREBIRD_KEYWORDS");
        for (int i = 1; i < keywords.length; i++) {
            assertTrue("Keywords array must be sorted alphabetically: '" + keywords[i - 1] + "' before '" + keywords[i] + "'",
                keywords[i - 1].compareTo(keywords[i]) < 0);
        }
    }

    // ---- Helper methods ----

    private String[] getStaticStringArray(String fieldName) throws Exception {
        Field field = FireBirdSQLDialect.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }
}
