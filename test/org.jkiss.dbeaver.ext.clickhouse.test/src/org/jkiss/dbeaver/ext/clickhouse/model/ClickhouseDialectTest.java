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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class ClickhouseDialectTest extends DBeaverUnitTest {
    @Test
    public void quoteStatusStringTest() {
        final ClickhouseSQLDialect dialect = new ClickhouseSQLDialect();

        Assert.assertFalse(dialect.mustBeQuoted("test", false));
        Assert.assertFalse(dialect.mustBeQuoted("testNAME", false));
        Assert.assertFalse(dialect.mustBeQuoted("Test", false));
        Assert.assertFalse(dialect.mustBeQuoted("TEST", false));

        Assert.assertTrue(dialect.mustBeQuoted("тест", false));
        Assert.assertTrue(dialect.mustBeQuoted("  test  ", false));
        Assert.assertTrue(dialect.mustBeQuoted("њен", false));
        Assert.assertTrue(dialect.mustBeQuoted("भिडियोहरू", false));
        Assert.assertTrue(dialect.mustBeQuoted("名错误", false));
        Assert.assertTrue(dialect.mustBeQuoted("თახვი", false));

        Assert.assertTrue(dialect.mustBeQuoted("default", false));
        Assert.assertTrue(dialect.mustBeQuoted("DEFAULT", false));
        Assert.assertTrue(dialect.mustBeQuoted("SYSTEM", false));
        Assert.assertTrue(dialect.mustBeQuoted("system", false));
    }
}
