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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class SQLUtilsTest extends DBeaverUnitTest {
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
            + "ORDER BY PersonID ;"));
    }

    @Test
    public void makeGlobFromSqlLikePattern_whenWithNoSpecialSymbols_thenSuccess(){

        String source = "key1234";
        Assert.assertEquals(source, SQLUtils.makeGlobFromSqlLikePattern(source));
    }

    @Test
    public void makeGlobFromSqlLikePattern_whenWithSpecialSymbols_thenSuccess(){

        Assert.assertEquals("key?*\\?*\\", SQLUtils.makeGlobFromSqlLikePattern("key_%?*\\"));
    }

    @Test
    public void testSkipWhitespaces() {
        String sql = "   SELECT * FROM dual";
        int nextIndex = SQLUtils.skipWhitespaces(sql, 0, sql.length());
        Assert.assertEquals(3, nextIndex);

        sql = "\n\t  INSERT INTO t VALUES(1)";
        nextIndex = SQLUtils.skipWhitespaces(sql, 0, sql.length());
        Assert.assertEquals(4, nextIndex);

        sql = "DELETE";
        nextIndex = SQLUtils.skipWhitespaces(sql, 0, sql.length());
        Assert.assertEquals(0, nextIndex);

        sql = "  ";
        nextIndex = SQLUtils.skipWhitespaces(sql, 0, sql.length());
        Assert.assertEquals(sql.length(), nextIndex);
    }

    @Test
    public void testSkipLeadingComments() {
        String[] sl = {"--", "#"};
        String mlStart = "/*";
        String mlEnd = "*/";

        Assert.assertEquals(0, SQLUtils.skipLeadingComments("SELECT 1", mlStart, mlEnd, sl));

        Assert.assertEquals(6, SQLUtils.skipLeadingComments("  \n\t  SELECT 1", mlStart, mlEnd, sl));

        String sql = "-- comment\nSELECT *";
        Assert.assertEquals(sql.indexOf('S'), SQLUtils.skipLeadingComments(sql, mlStart, mlEnd, sl));

        sql = "# hello\r\nSELECT *";
        Assert.assertEquals(sql.indexOf('S'), SQLUtils.skipLeadingComments(sql, mlStart, mlEnd, sl));

        sql = "/* block */SELECT *";
        Assert.assertEquals(sql.indexOf('S'), SQLUtils.skipLeadingComments(sql, mlStart, mlEnd, sl));

        sql = "  /* a */\n-- b\n\t/* c */   SELECT *";
        Assert.assertEquals(sql.indexOf('S'), SQLUtils.skipLeadingComments(sql, mlStart, mlEnd, sl));

        sql = "/* open only\nSELECT *";
        Assert.assertEquals(sql.length(), SQLUtils.skipLeadingComments(sql, mlStart, mlEnd, sl));

        sql = "  -- just comment\n  ";
        Assert.assertEquals(sql.length(), SQLUtils.skipLeadingComments(sql, mlStart, mlEnd, sl));

        Assert.assertEquals(0, SQLUtils.skipLeadingComments("'-- not comment' SELECT 1", mlStart, mlEnd, sl));

        Assert.assertEquals(0, SQLUtils.skipLeadingComments("/* not treated */ SELECT", null, null, sl));

        Assert.assertEquals(0, SQLUtils.skipLeadingComments("-- not treated\nSELECT", mlStart, mlEnd, new String[0]));
    }

    @Test
    public void testReplaceCreateToCreateOrReplace() {

        String[] sl = {"--", "#"};
        String mlStart = "/*";
        String mlEnd = "*/";

        String ddl = "CREATE OR REPLACE FUNCTION f() RETURN INT";
        Assert.assertEquals(ddl, SQLUtils.replaceCreateToCreateOrReplace(ddl, mlStart, mlEnd, sl));

        ddl = "CREATE VIEW v AS SELECT 1";
        Assert.assertEquals(
            "CREATE OR REPLACE VIEW v AS SELECT 1",
            SQLUtils.replaceCreateToCreateOrReplace(ddl, mlStart, mlEnd, sl)
        );

        ddl = "-- comment\nCREATE PROCEDURE p() BEGIN END";
        Assert.assertEquals(
            "-- comment\nCREATE OR REPLACE PROCEDURE p() BEGIN END",
            SQLUtils.replaceCreateToCreateOrReplace(ddl, mlStart, mlEnd, sl)
        );

    }

}
