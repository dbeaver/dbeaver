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

import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class AbstractSQLDialectTest extends DBeaverUnitTest {

    private static final SQLDialect dialect = BasicSQLDialect.INSTANCE;

    @Test
    public void getQuotedStringPlainValueTest() {
        Assert.assertEquals("'hello'", dialect.getQuotedString("hello"));
    }

    @Test
    public void getQuotedStringEmbeddedQuotesTest() {
        Assert.assertEquals("'it''s'", dialect.getQuotedString("it's"));
    }

    @Test
    public void getQuotedStringMultipleQuotedFragmentsTest() {
        Assert.assertEquals(
            "'''03'',''04'',''05'',''10'',''11'''",
            dialect.getQuotedString("'03','04','05','10','11'")
        );
    }

    @Test
    public void getQuotedStringValueLooksLikeQuotedLiteralTest() {
        Assert.assertEquals(
            "'''kkk''''pp'''",
            dialect.getQuotedString("'kkk''pp'")
        );
    }

    @Test
    public void getQuotedStringConsecutiveQuotesTest() {
        Assert.assertEquals("'kkk''''''pp'", dialect.getQuotedString("kkk'''pp"));
        Assert.assertEquals("'kkk''''pp'", dialect.getQuotedString("kkk''pp"));
    }

    @Test
    public void getQuotedStringNestedQuotedLiteralLookingValueTest() {
        Assert.assertEquals(
            "'''kkk''''''''pp'''",
            dialect.getQuotedString("'kkk''''pp'")
        );
    }
}
