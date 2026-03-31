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

    // ---- DDL Keywords ----

    @Test
    public void testDDLKeywordsContainRecreate() {
        List<String> ddlKeywords = Arrays.asList(dialect.getDDLKeywords());
        assertTrue("DDL keywords must contain RECREATE", ddlKeywords.contains("RECREATE"));
    }

    @Test
    public void testDDLKeywordsContainComment() {
        List<String> ddlKeywords = Arrays.asList(dialect.getDDLKeywords());
        assertTrue("DDL keywords must contain COMMENT", ddlKeywords.contains("COMMENT"));
    }

    @Test
    public void testDDLKeywordsContainStandardEntries() {
        List<String> ddlKeywords = Arrays.asList(dialect.getDDLKeywords());
        assertTrue("DDL keywords must contain CREATE", ddlKeywords.contains("CREATE"));
        assertTrue("DDL keywords must contain ALTER", ddlKeywords.contains("ALTER"));
        assertTrue("DDL keywords must contain DROP", ddlKeywords.contains("DROP"));
        assertTrue("DDL keywords must contain EXECUTE", ddlKeywords.contains("EXECUTE"));
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

    // ---- Keyword array completeness (via reflection) ----

    @Test
    public void testFirebirdKeywordsArrayContainsExpectedEntries() throws Exception {
        String[] keywords = getStaticStringArray("FIREBIRD_KEYWORDS");
        List<String> keywordList = Arrays.asList(keywords);

        // Firebird 3.0+ keywords
        assertTrue("Must contain BOOLEAN", keywordList.contains("BOOLEAN"));
        assertTrue("Must contain RETURNING", keywordList.contains("RETURNING"));
        assertTrue("Must contain MERGE", keywordList.contains("MERGE"));
        assertTrue("Must contain MATCHED", keywordList.contains("MATCHED"));
        assertTrue("Must contain GENERATOR", keywordList.contains("GENERATOR"));
        assertTrue("Must contain RECREATE", keywordList.contains("RECREATE"));
        assertTrue("Must contain PACKAGE", keywordList.contains("PACKAGE"));
        assertTrue("Must contain DOMAIN", keywordList.contains("DOMAIN"));
        assertTrue("Must contain EXCEPTION", keywordList.contains("EXCEPTION"));

        // Firebird 4.0+ keywords
        assertTrue("Must contain DECFLOAT", keywordList.contains("DECFLOAT"));
        assertTrue("Must contain INT128", keywordList.contains("INT128"));
        assertTrue("Must contain LATERAL", keywordList.contains("LATERAL"));
        assertTrue("Must contain BINARY", keywordList.contains("BINARY"));
        assertTrue("Must contain VARBINARY", keywordList.contains("VARBINARY"));
        assertTrue("Must contain LOCALTIME", keywordList.contains("LOCALTIME"));
        assertTrue("Must contain LOCALTIMESTAMP", keywordList.contains("LOCALTIMESTAMP"));
        assertTrue("Must contain OVERRIDING", keywordList.contains("OVERRIDING"));
        assertTrue("Must contain DEFINER", keywordList.contains("DEFINER"));
        assertTrue("Must contain INVOKER", keywordList.contains("INVOKER"));
        assertTrue("Must contain SECURITY", keywordList.contains("SECURITY"));

        // Window function keywords
        assertTrue("Must contain WINDOW", keywordList.contains("WINDOW"));
        assertTrue("Must contain OVER", keywordList.contains("OVER"));
        assertTrue("Must contain PARTITION", keywordList.contains("PARTITION"));
        assertTrue("Must contain ROWS", keywordList.contains("ROWS"));
        assertTrue("Must contain UNBOUNDED", keywordList.contains("UNBOUNDED"));
        assertTrue("Must contain PRECEDING", keywordList.contains("PRECEDING"));
        assertTrue("Must contain FOLLOWING", keywordList.contains("FOLLOWING"));
        assertTrue("Must contain RANGE", keywordList.contains("RANGE"));

        // Firebird 5.0 keywords
        assertTrue("Must contain SKIP", keywordList.contains("SKIP"));
        assertTrue("Must contain LOCKED", keywordList.contains("LOCKED"));
        assertTrue("Must contain OPTIMIZE", keywordList.contains("OPTIMIZE"));

        // Statistical functions as keywords
        assertTrue("Must contain STDDEV_POP", keywordList.contains("STDDEV_POP"));
        assertTrue("Must contain VAR_SAMP", keywordList.contains("VAR_SAMP"));
        assertTrue("Must contain CORR", keywordList.contains("CORR"));

        // PSQL keywords
        assertTrue("Must contain INSERTING", keywordList.contains("INSERTING"));
        assertTrue("Must contain UPDATING", keywordList.contains("UPDATING"));
        assertTrue("Must contain DELETING", keywordList.contains("DELETING"));
    }

    @Test
    public void testFirebirdKeywordsArrayIsSorted() throws Exception {
        String[] keywords = getStaticStringArray("FIREBIRD_KEYWORDS");
        for (int i = 1; i < keywords.length; i++) {
            assertTrue("Keywords array must be sorted alphabetically: '" + keywords[i - 1] + "' before '" + keywords[i] + "'",
                keywords[i - 1].compareTo(keywords[i]) < 0);
        }
    }

    // ---- Function array completeness (via reflection) ----

    @Test
    public void testFirebirdFunctionsArrayContainsExpectedEntries() throws Exception {
        String[] functions = getStaticStringArray("FIREBIRD_FUNCTIONS");
        List<String> functionList = Arrays.asList(functions);

        // Math functions
        assertTrue("Must contain ABS", functionList.contains("ABS"));
        assertTrue("Must contain SQRT", functionList.contains("SQRT"));
        assertTrue("Must contain POWER", functionList.contains("POWER"));
        assertTrue("Must contain LOG", functionList.contains("LOG"));
        assertTrue("Must contain MOD", functionList.contains("MOD"));
        assertTrue("Must contain FLOOR", functionList.contains("FLOOR"));
        assertTrue("Must contain CEIL", functionList.contains("CEIL"));

        // String functions
        assertTrue("Must contain LPAD", functionList.contains("LPAD"));
        assertTrue("Must contain REPLACE", functionList.contains("REPLACE"));
        assertTrue("Must contain LEFT", functionList.contains("LEFT"));
        assertTrue("Must contain RIGHT", functionList.contains("RIGHT"));
        assertTrue("Must contain CHAR_LENGTH", functionList.contains("CHAR_LENGTH"));
        assertTrue("Must contain POSITION", functionList.contains("POSITION"));
        assertTrue("Must contain SUBSTRING", functionList.contains("SUBSTRING"));
        assertTrue("Must contain TRIM", functionList.contains("TRIM"));
        assertTrue("Must contain HASH", functionList.contains("HASH"));

        // Date/time functions
        assertTrue("Must contain DATEADD", functionList.contains("DATEADD"));
        assertTrue("Must contain DATEDIFF", functionList.contains("DATEDIFF"));
        assertTrue("Must contain FIRST_DAY", functionList.contains("FIRST_DAY"));
        assertTrue("Must contain LAST_DAY", functionList.contains("LAST_DAY"));

        // UUID functions
        assertTrue("Must contain GEN_UUID", functionList.contains("GEN_UUID"));
        assertTrue("Must contain UUID_TO_CHAR", functionList.contains("UUID_TO_CHAR"));
        assertTrue("Must contain CHAR_TO_UUID", functionList.contains("CHAR_TO_UUID"));

        // Context functions
        assertTrue("Must contain RDB$GET_CONTEXT", functionList.contains("RDB$GET_CONTEXT"));
        assertTrue("Must contain RDB$SET_CONTEXT", functionList.contains("RDB$SET_CONTEXT"));

        // Bitwise functions
        assertTrue("Must contain BIN_AND", functionList.contains("BIN_AND"));
        assertTrue("Must contain BIN_OR", functionList.contains("BIN_OR"));
        assertTrue("Must contain BIN_XOR", functionList.contains("BIN_XOR"));

        // Aggregate functions
        assertTrue("Must contain LIST", functionList.contains("LIST"));

        // Window functions (FB 3.0+)
        assertTrue("Must contain ROW_NUMBER", functionList.contains("ROW_NUMBER"));
        assertTrue("Must contain RANK", functionList.contains("RANK"));
        assertTrue("Must contain DENSE_RANK", functionList.contains("DENSE_RANK"));
        assertTrue("Must contain LAG", functionList.contains("LAG"));
        assertTrue("Must contain LEAD", functionList.contains("LEAD"));
        assertTrue("Must contain FIRST_VALUE", functionList.contains("FIRST_VALUE"));
        assertTrue("Must contain LAST_VALUE", functionList.contains("LAST_VALUE"));

        // Window functions (FB 4.0+)
        assertTrue("Must contain CUME_DIST", functionList.contains("CUME_DIST"));
        assertTrue("Must contain NTILE", functionList.contains("NTILE"));
        assertTrue("Must contain PERCENT_RANK", functionList.contains("PERCENT_RANK"));
        assertTrue("Must contain NTH_VALUE", functionList.contains("NTH_VALUE"));

        // Crypto functions (FB 4.0+)
        assertTrue("Must contain CRYPT_HASH", functionList.contains("CRYPT_HASH"));
        assertTrue("Must contain ENCRYPT", functionList.contains("ENCRYPT"));
        assertTrue("Must contain DECRYPT", functionList.contains("DECRYPT"));
        assertTrue("Must contain BASE64_ENCODE", functionList.contains("BASE64_ENCODE"));
        assertTrue("Must contain BASE64_DECODE", functionList.contains("BASE64_DECODE"));
        assertTrue("Must contain HEX_ENCODE", functionList.contains("HEX_ENCODE"));
        assertTrue("Must contain HEX_DECODE", functionList.contains("HEX_DECODE"));

        // FB 5.0 functions
        assertTrue("Must contain UNICODE_CHAR", functionList.contains("UNICODE_CHAR"));
        assertTrue("Must contain UNICODE_VAL", functionList.contains("UNICODE_VAL"));
    }

    @Test
    public void testFirebirdFunctionsCountIsSubstantial() throws Exception {
        String[] functions = getStaticStringArray("FIREBIRD_FUNCTIONS");
        assertTrue("Function array should have at least 80 entries (was " + functions.length + ")",
            functions.length >= 80);
    }

    @Test
    public void testFirebirdKeywordsCountIsSubstantial() throws Exception {
        String[] keywords = getStaticStringArray("FIREBIRD_KEYWORDS");
        assertTrue("Keyword array should have at least 60 entries (was " + keywords.length + ")",
            keywords.length >= 60);
    }

    // ---- Helper methods ----

    private String[] getStaticStringArray(String fieldName) throws Exception {
        Field field = FireBirdSQLDialect.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }
}
