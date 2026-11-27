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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.ext.oracle.model.OracleSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class SQLUtilsTest extends DBeaverUnitTest {

    private static final OracleSQLDialect ORACLE_SQL_DIALECT = new OracleSQLDialect();

    @Test
    public void makeRegexFromLikeTest() {
        Assertions.assertEquals("^ABC$", SQLUtils.makeRegexFromLike("ABC"));
        Assertions.assertEquals("^A.*C$", SQLUtils.makeRegexFromLike("A%C"));
        Assertions.assertEquals("^ABC", SQLUtils.makeRegexFromLike("ABC%"));
        Assertions.assertEquals("ABC$", SQLUtils.makeRegexFromLike("%ABC"));
        Assertions.assertEquals("ABC", SQLUtils.makeRegexFromLike("%ABC%"));
        Assertions.assertEquals("^A.C$", SQLUtils.makeRegexFromLike("A_C"));
        Assertions.assertEquals("A.C", SQLUtils.makeRegexFromLike("%A_C%"));
    }

    @Test
    public void fixLineFeedsTest() {
        Assertions.assertEquals(
            "SELECT LastName -- x\r\n"
                + "FROM Persons drai where PersonID  = 1\r\n"
                + "-- AND ResourceId  = 1\n\r"
                + "ORDER BY PersonID ;",
            SQLUtils.fixLineFeeds("SELECT LastName -- x\r"
                + "FROM Persons drai where PersonID  = 1\r\n"
                + "-- AND ResourceId  = 1\n\r"
                + "ORDER BY PersonID ;")
        );
    }

    @Test
    public void makeGlobFromSqlLikePattern_whenWithNoSpecialSymbols_thenSuccess() {
        String source = "key1234";
        Assertions.assertEquals(source, SQLUtils.makeGlobFromSqlLikePattern(source));
    }

    @Test
    public void makeGlobFromSqlLikePattern_whenWithSpecialSymbols_thenSuccess() {
        Assertions.assertEquals("key?*\\?*\\", SQLUtils.makeGlobFromSqlLikePattern("key_%?*\\"));
    }

    @Test
    public void extractProcedureParameterTypes_whenNullOrEmpty_thenParens() {
        Assertions.assertEquals("()", SQLUtils.extractProcedureParameterTypes(null));
        Assertions.assertEquals("()", SQLUtils.extractProcedureParameterTypes(""));
        Assertions.assertEquals("()", SQLUtils.extractProcedureParameterTypes("   "));
        Assertions.assertEquals("()", SQLUtils.extractProcedureParameterTypes("()"));
        Assertions.assertEquals("()", SQLUtils.extractProcedureParameterTypes("(   )"));
    }

    @Test
    public void extractProcedureParameterTypes_whenNamesPresent_thenRemoved() {
        Assertions.assertEquals(
            "(NUMBER(38,0), VARCHAR)",
            SQLUtils.extractProcedureParameterTypes("(a NUMBER(38,0), b VARCHAR)")
        );
        Assertions.assertEquals(
            "(ARRAY, OBJECT)",
            SQLUtils.extractProcedureParameterTypes("(x ARRAY, y OBJECT)")
        );
    }

    @Test
    public void extractTypesOnly_whenNestedProcedureParameterTypes_thenKeepInnerParens() {
        Assertions.assertEquals(
            "(DECIMAL(10,2), ARRAY(VARCHAR))",
            SQLUtils.extractProcedureParameterTypes("(price DECIMAL(10,2), tags ARRAY(VARCHAR))")
        );
        Assertions.assertEquals(
            "(ARRAY(OBJECT), VARIANT)",
            SQLUtils.extractProcedureParameterTypes("(p ARRAY(OBJECT), v VARIANT)")
        );
    }

    @Test
    public void extractProcedureParameterTypes_whenNoOuterParens_thenSingleType() {
        Assertions.assertEquals(
            "(NUMBER)",
            SQLUtils.extractProcedureParameterTypes("id NUMBER")
        );
        Assertions.assertEquals(
            "(ARRAY(VARCHAR))",
            SQLUtils.extractProcedureParameterTypes("arr ARRAY(VARCHAR)")
        );
    }

    @Test
    public void extractProcedureParameterTypes_whenWhitespaceAndCase_thenCanonicalUpper() {
        Assertions.assertEquals(
            "(NUMBER, VARCHAR)",
            SQLUtils.extractProcedureParameterTypes("(  a   number  ,   b   varchar  )")
        );
        Assertions.assertEquals(
            "(ARRAY, OBJECT, VARIANT)",
            SQLUtils.extractProcedureParameterTypes("( arr  array , obj   object , v   variant )")
        );
    }

    @Test
    public void extractProcedureParameterTypes_whenComplexSignature_thenCorrectSplit() {
        Assertions.assertEquals(
            "(DECIMAL(38,0), ARRAY(DECIMAL(10,2)), OBJECT)",
            SQLUtils.extractProcedureParameterTypes("(a DECIMAL(38,0), b ARRAY(DECIMAL(10,2)), c OBJECT)")
        );
    }

    @Test
    public void addMultiStatementDDL_whenNullOrEmpty_thenNoChange() {
        StringBuilder sb = new StringBuilder();
        SQLUtils.addMultiStatementDDL(ORACLE_SQL_DIALECT, sb, null);
        Assertions.assertEquals("", sb.toString());

        SQLUtils.addMultiStatementDDL(ORACLE_SQL_DIALECT, sb, "   ");
        Assertions.assertEquals("", sb.toString());
    }

    @Test
    public void addMultiStatementDDL_oracleUserAndGrants_examples() {
        String ddl =
            "GRANT CREATE TABLE TO \"TEST_USER_DECL\"\n" +
            "GRANT CREATE SESSION TO \"TEST_USER_DECL\"\n" +
            "GRANT \"ROLE1\" TO \"TEST_USER_DECL\"\n" +
            "GRANT \"ROLE2\" TO \"TEST_USER_DECL\"\n" +
            "GRANT \"ROLE3\" TO \"TEST_USER_DECL\";\n";

        StringBuilder sb = new StringBuilder();
        SQLUtils.addMultiStatementDDL(ORACLE_SQL_DIALECT, sb, ddl);

        Assertions.assertEquals(
            "GRANT CREATE TABLE TO \"TEST_USER_DECL\";\n" +
                "GRANT CREATE SESSION TO \"TEST_USER_DECL\";\n" +
                "GRANT \"ROLE1\" TO \"TEST_USER_DECL\";\n" +
                "GRANT \"ROLE2\" TO \"TEST_USER_DECL\";\n" +
                "GRANT \"ROLE3\" TO \"TEST_USER_DECL\";\n\n",
            sb.toString()
        );
    }

    @Test
    public void addMultiStatementDDL_whenNoDelimiters_thenAppendDelimiterPerLineAndTrailingLF() {
        String ddl = "CREATE TABLE A(id INT)\nCREATE INDEX I ON A(id)";
        StringBuilder sb = new StringBuilder();
        SQLUtils.addMultiStatementDDL(ORACLE_SQL_DIALECT, sb, ddl);
        Assertions.assertEquals(
            "CREATE TABLE A(id INT);\n" +
                "CREATE INDEX I ON A(id);\n\n",
            sb.toString()
        );
    }

    @Test
    public void addMultiStatementDDL_whenAlreadyDelimited_thenDoNotDuplicateDelimiter() {
        String ddl = "CREATE TABLE A(id INT);\nCREATE INDEX I ON A(id);";
        StringBuilder sb = new StringBuilder();
        SQLUtils.addMultiStatementDDL(ORACLE_SQL_DIALECT, sb, ddl);
        Assertions.assertEquals(
            "CREATE TABLE A(id INT);\n" +
                "CREATE INDEX I ON A(id);\n\n",
            sb.toString()
        );
    }

    @Test
    public void addMultiStatementDDL_whenHasEmptyLines_thenSkipThem() {
        String ddl = "\n\n  \nCREATE TABLE A(id INT)\n   \nCREATE INDEX I ON A(id)  \n\n";
        StringBuilder sb = new StringBuilder();
        SQLUtils.addMultiStatementDDL(ORACLE_SQL_DIALECT, sb, ddl);
        Assertions.assertEquals(
            "CREATE TABLE A(id INT);\n" +
                "CREATE INDEX I ON A(id);\n\n",
            sb.toString()
        );
    }

    // --- makeLikePattern / matchesLike backslash regression tests (issue #42872) ---

    @Test
    public void makeLikePatternBackslashBeforeNonWildcardMatchesLiteral() {
        // CORP backslash user must compile and match the same literal string
        String pattern = SQLUtils.makeLikePattern("CORP\\user");
        Assertions.assertDoesNotThrow(() -> Pattern.compile(pattern));
        Assertions.assertTrue(SQLUtils.matchesLike("CORP\\user", "CORP\\user"));
        // Must not match without the backslash
        Assertions.assertFalse(SQLUtils.matchesLike("CORPuser", "CORP\\user"));
    }

    @Test
    public void makeLikePatternBackslashBeforeJMatchesLiteral() {
        // CORP\john must not throw
        String pattern = SQLUtils.makeLikePattern("CORP\\john");
        Assertions.assertDoesNotThrow(() -> Pattern.compile(pattern));
        Assertions.assertTrue(SQLUtils.matchesLike("CORP\\john", "CORP\\john"));
    }

    @Test
    public void makeLikePatternTrailingBackslashMatchesLiteral() {
        // Trailing backslash must not be dropped or produce invalid regex
        String pattern = SQLUtils.makeLikePattern("trail\\");
        Assertions.assertDoesNotThrow(() -> Pattern.compile(pattern));
        Assertions.assertTrue(SQLUtils.matchesLike("trail\\", "trail\\"));
        Assertions.assertFalse(SQLUtils.matchesLike("trail", "trail\\"));
    }

    @Test
    public void makeLikePatternBackslashBDoesNotActAsWordBoundary() {
        // a\b must match the literal string "a\b", not act as regex \b (word boundary)
        String pattern = SQLUtils.makeLikePattern("a\\b");
        Assertions.assertDoesNotThrow(() -> Pattern.compile(pattern));
        Assertions.assertTrue(SQLUtils.matchesLike("a\\b", "a\\b"));
        // Must not match just "a" (would happen if \b was a word boundary)
        Assertions.assertFalse(SQLUtils.matchesLike("a", "a\\b"));
    }

    @Test
    public void makeLikePatternEscapedWildcardsStillWork() {
        // \_ should match literal underscore, not single-char wildcard
        Assertions.assertTrue(SQLUtils.matchesLike("a_b", "a\\_b"));
        Assertions.assertFalse(SQLUtils.matchesLike("aXb", "a\\_b"));

        // \* should match literal asterisk
        Assertions.assertTrue(SQLUtils.matchesLike("a*b", "a\\*b"));
        Assertions.assertFalse(SQLUtils.matchesLike("aXYZb", "a\\*b"));

        // \? should match literal question mark
        Assertions.assertTrue(SQLUtils.matchesLike("a?b", "a\\?b"));
        Assertions.assertFalse(SQLUtils.matchesLike("aXb", "a\\?b"));

        // \. should match literal dot
        Assertions.assertTrue(SQLUtils.matchesLike("a.b", "a\\.b"));
        Assertions.assertFalse(SQLUtils.matchesLike("aXb", "a\\.b"));

        // \% should match literal percent
        Assertions.assertTrue(SQLUtils.matchesLike("a%b", "a\\%b"));
        Assertions.assertFalse(SQLUtils.matchesLike("aXYZb", "a\\%b"));
    }

    @Test
    public void makeLikePatternWildcardsStillWork() {
        // % and * as multi-char wildcards
        Assertions.assertTrue(SQLUtils.matchesLike("abcdef", "%cd%"));
        Assertions.assertTrue(SQLUtils.matchesLike("abcdef", "*cd*"));

        // _ and ? as single-char wildcards
        Assertions.assertTrue(SQLUtils.matchesLike("abc", "a_c"));
        Assertions.assertTrue(SQLUtils.matchesLike("abc", "a?c"));
        Assertions.assertFalse(SQLUtils.matchesLike("abbc", "a_c"));
    }

    @Test
    public void makeLikePatternMultipleBackslashesMatchLiteral() {
        // Double backslash should match literal double backslash
        Assertions.assertTrue(SQLUtils.matchesLike("a\\\\b", "a\\\\b"));
        // Backslash followed by different non-wildcard chars
        Assertions.assertTrue(SQLUtils.matchesLike("x\\y\\z", "x\\y\\z"));
    }

    @Test
    public void makeLikePatternBackslashWithWildcardCombined() {
        // Pattern with both literal backslash and multi-char wildcard %
        Assertions.assertTrue(SQLUtils.matchesLike("CORP\\user_01", "CORP\\user%"));
        Assertions.assertFalse(SQLUtils.matchesLike("CORP\\other", "CORP\\user%"));

        // Single-char wildcard _ still works alongside literal backslash
        Assertions.assertTrue(SQLUtils.matchesLike("CORP\\userX01", "CORP\\user_01"));
        Assertions.assertFalse(SQLUtils.matchesLike("CORP\\userXY01", "CORP\\user_01"));

        // Escaped wildcard \_ matches literal underscore only
        Assertions.assertTrue(SQLUtils.matchesLike("CORP\\user_01", "CORP\\user\\_01"));
        Assertions.assertFalse(SQLUtils.matchesLike("CORP\\userX01", "CORP\\user\\_01"));
    }
}
