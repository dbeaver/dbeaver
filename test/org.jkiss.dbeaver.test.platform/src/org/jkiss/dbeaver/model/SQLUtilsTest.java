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
import org.jkiss.junit.OSGIUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class SQLUtilsTest extends OSGIUnitTest {
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
}
