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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class FireBirdSQLDialectTest extends DBeaverUnitTest {

    private FireBirdSQLDialect dialect;

    @BeforeEach
    public void setUp() {
        dialect = new FireBirdSQLDialect();
    }

    // ---- Block headers and bounds ----

    @Test
    public void testBlockHeaderStringsContainExecuteBlock() {
        String[] headers = dialect.getBlockHeaderStrings();
        Assertions.assertNotNull(headers, "Block headers should not be null");
        List<String> headerList = Arrays.asList(headers);
        Assertions.assertTrue(headerList.contains("EXECUTE BLOCK"), "Block headers must contain EXECUTE BLOCK");
    }

    @Test
    public void testBlockBoundStringsContainBeginEnd() {
        String[][] bounds = dialect.getBlockBoundStrings();
        Assertions.assertNotNull(bounds, "Block bounds should not be null");
        Assertions.assertEquals(1, bounds.length, "Should have exactly one BEGIN/END pair");
        Assertions.assertEquals("BEGIN", bounds[0][0]);
        Assertions.assertEquals("END", bounds[0][1]);
    }

    // ---- Boolean behavior methods ----

    @Test
    public void testSupportsAliasInSelect() {
        Assertions.assertTrue(dialect.supportsAliasInSelect(), "Firebird supports alias in SELECT");
    }

    @Test
    public void testSupportsAliasInHaving() {
        Assertions.assertFalse(dialect.supportsAliasInHaving(), "Firebird does not support alias in HAVING");
    }

    @Test
    public void testSupportsInsertAllDefaultValues() {
        Assertions.assertTrue(dialect.supportsInsertAllDefaultValuesStatement(),
            "Firebird supports INSERT with all default values");
    }

    // ---- Identifier handling ----

    @Test
    public void testValidIdentifierPartAcceptsDollarSign() {
        Assertions.assertTrue(dialect.validIdentifierPart('$', false), "Dollar sign must be valid in identifiers");
    }

    @Test
    public void testValidIdentifierPartAcceptsUnderscore() {
        Assertions.assertTrue(dialect.validIdentifierPart('_', false), "Underscore must be valid in identifiers");
    }

    @Test
    public void testValidIdentifierPartRejectsSpace() {
        Assertions.assertFalse(dialect.validIdentifierPart(' ', false), "Space must not be valid in unquoted identifiers");
    }

    // ---- Keyword array structure (via reflection) ----

    @Test
    public void testFirebirdKeywordsArrayIsSorted() throws Exception {
        String[] keywords = getStaticStringArray("FIREBIRD_KEYWORDS");
        for (int i = 1; i < keywords.length; i++) {
            Assertions.assertTrue(keywords[i - 1].compareTo(keywords[i]) < 0,
                "Keywords array must be sorted alphabetically: '" + keywords[i - 1] + "' before '" + keywords[i] + "'");
        }
    }

    // ---- Helper methods ----

    private String[] getStaticStringArray(String fieldName) throws Exception {
        Field field = FireBirdSQLDialect.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }
}
