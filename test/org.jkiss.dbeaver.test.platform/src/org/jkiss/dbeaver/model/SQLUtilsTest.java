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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.ext.oracle.model.OracleSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class SQLUtilsTest extends DBeaverUnitTest {

    private static final OracleSQLDialect ORACLE_SQL_DIALECT = new OracleSQLDialect();

    @Test
    public void makeRegexFromLikeTest() {
        Assert.assertEquals("^ABC$", SQLUtils.makeRegexFromLike("ABC"));
        Assert.assertEquals("^A.*C$", SQLUtils.makeRegexFromLike("A%C"));
        Assert.assertEquals("^ABC", SQLUtils.makeRegexFromLike("ABC%"));
        Assert.assertEquals("ABC$", SQLUtils.makeRegexFromLike("%ABC"));
        Assert.assertEquals("ABC", SQLUtils.makeRegexFromLike("%ABC%"));
        Assert.assertEquals("^A.C$", SQLUtils.makeRegexFromLike("A_C"));
        Assert.assertEquals("A.C", SQLUtils.makeRegexFromLike("%A_C%"));
    }

    @Test
    public void fixLineFeedsTest() {
        Assert.assertEquals(
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
        Assert.assertEquals(source, SQLUtils.makeGlobFromSqlLikePattern(source));
    }

    @Test
    public void makeGlobFromSqlLikePattern_whenWithSpecialSymbols_thenSuccess() {
        Assert.assertEquals("key?*\\?*\\", SQLUtils.makeGlobFromSqlLikePattern("key_%?*\\"));
    }

    @Test
    public void extractProcedureParameterTypes_whenNullOrEmpty_thenParens() {
        Assert.assertEquals("()", SQLUtils.extractProcedureParameterTypes(null));
        Assert.assertEquals("()", SQLUtils.extractProcedureParameterTypes(""));
        Assert.assertEquals("()", SQLUtils.extractProcedureParameterTypes("   "));
        Assert.assertEquals("()", SQLUtils.extractProcedureParameterTypes("()"));
        Assert.assertEquals("()", SQLUtils.extractProcedureParameterTypes("(   )"));
    }

    @Test
    public void extractProcedureParameterTypes_whenNamesPresent_thenRemoved() {
        Assert.assertEquals(
            "(NUMBER(38,0), VARCHAR)",
            SQLUtils.extractProcedureParameterTypes("(a NUMBER(38,0), b VARCHAR)")
        );
        Assert.assertEquals(
            "(ARRAY, OBJECT)",
            SQLUtils.extractProcedureParameterTypes("(x ARRAY, y OBJECT)")
        );
    }

    @Test
    public void extractTypesOnly_whenNestedProcedureParameterTypes_thenKeepInnerParens() {
        Assert.assertEquals(
            "(DECIMAL(10,2), ARRAY(VARCHAR))",
            SQLUtils.extractProcedureParameterTypes("(price DECIMAL(10,2), tags ARRAY(VARCHAR))")
        );
        Assert.assertEquals(
            "(ARRAY(OBJECT), VARIANT)",
            SQLUtils.extractProcedureParameterTypes("(p ARRAY(OBJECT), v VARIANT)")
        );
    }

    @Test
    public void extractProcedureParameterTypes_whenNoOuterParens_thenSingleType() {
        Assert.assertEquals(
            "(NUMBER)",
            SQLUtils.extractProcedureParameterTypes("id NUMBER")
        );
        Assert.assertEquals(
            "(ARRAY(VARCHAR))",
            SQLUtils.extractProcedureParameterTypes("arr ARRAY(VARCHAR)")
        );
    }

    @Test
    public void extractProcedureParameterTypes_whenWhitespaceAndCase_thenCanonicalUpper() {
        Assert.assertEquals(
            "(NUMBER, VARCHAR)",
            SQLUtils.extractProcedureParameterTypes("(  a   number  ,   b   varchar  )")
        );
        Assert.assertEquals(
            "(ARRAY, OBJECT, VARIANT)",
            SQLUtils.extractProcedureParameterTypes("( arr  array , obj   object , v   variant )")
        );
    }

    @Test
    public void extractProcedureParameterTypes_whenComplexSignature_thenCorrectSplit() {
        Assert.assertEquals(
            "(DECIMAL(38,0), ARRAY(DECIMAL(10,2)), OBJECT)",
            SQLUtils.extractProcedureParameterTypes("(a DECIMAL(38,0), b ARRAY(DECIMAL(10,2)), c OBJECT)")
        );
    }

    @Test
    public void addMultiStatementDDL_whenNullOrEmpty_thenNoChange() {
        StringBuilder sb = new StringBuilder();
        SQLUtils.addMultiStatementDDL(ORACLE_SQL_DIALECT, sb, null);
        Assert.assertEquals("", sb.toString());

        SQLUtils.addMultiStatementDDL(ORACLE_SQL_DIALECT, sb, "   ");
        Assert.assertEquals("", sb.toString());
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

        Assert.assertEquals(
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
        Assert.assertEquals(
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
        Assert.assertEquals(
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
        Assert.assertEquals(
            "CREATE TABLE A(id INT);\n" +
                "CREATE INDEX I ON A(id);\n\n",
            sb.toString()
        );
    }
}
