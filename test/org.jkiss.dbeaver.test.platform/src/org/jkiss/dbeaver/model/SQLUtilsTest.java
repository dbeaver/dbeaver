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
    public void extractTypesOnly_whenNullOrEmpty_thenParens() {
        Assert.assertEquals("()", SQLUtils.extractTypesOnly(null));
        Assert.assertEquals("()", SQLUtils.extractTypesOnly(""));
        Assert.assertEquals("()", SQLUtils.extractTypesOnly("   "));
        Assert.assertEquals("()", SQLUtils.extractTypesOnly("()"));
        Assert.assertEquals("()", SQLUtils.extractTypesOnly("(   )"));
    }

    @Test
    public void extractTypesOnly_whenNamesPresent_thenRemoved() {
        Assert.assertEquals("(NUMBER(38,0), VARCHAR)",
            SQLUtils.extractTypesOnly("(a NUMBER(38,0), b VARCHAR)"));
        Assert.assertEquals("(ARRAY, OBJECT)",
            SQLUtils.extractTypesOnly("(x ARRAY, y OBJECT)"));
    }

    @Test
    public void extractTypesOnly_whenNestedTypes_thenKeepInnerParens() {
        Assert.assertEquals("(DECIMAL(10,2), ARRAY(VARCHAR))",
            SQLUtils.extractTypesOnly("(price DECIMAL(10,2), tags ARRAY(VARCHAR))"));
        Assert.assertEquals("(ARRAY(OBJECT), VARIANT)",
            SQLUtils.extractTypesOnly("(p ARRAY(OBJECT), v VARIANT)"));
    }

    @Test
    public void extractTypesOnly_whenNoOuterParens_thenSingleType() {
        Assert.assertEquals("(NUMBER)",
            SQLUtils.extractTypesOnly("id NUMBER"));
        Assert.assertEquals("(ARRAY(VARCHAR))",
            SQLUtils.extractTypesOnly("arr ARRAY(VARCHAR)"));
    }

    @Test
    public void extractTypesOnly_whenWhitespaceAndCase_thenCanonicalUpper() {
        Assert.assertEquals("(NUMBER, VARCHAR)",
            SQLUtils.extractTypesOnly("(  a   number  ,   b   varchar  )"));
        Assert.assertEquals("(ARRAY, OBJECT, VARIANT)",
            SQLUtils.extractTypesOnly("( arr  array , obj   object , v   variant )"));
    }

    @Test
    public void extractTypesOnly_whenComplexSignature_thenCorrectSplit() {
        Assert.assertEquals("(DECIMAL(38,0), ARRAY(DECIMAL(10,2)), OBJECT)",
            SQLUtils.extractTypesOnly("(a DECIMAL(38,0), b ARRAY(DECIMAL(10,2)), c OBJECT)"));
    }
}
