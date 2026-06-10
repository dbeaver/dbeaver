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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SnowflakeSQLDialectTest extends DBeaverUnitTest {
    @Test
    public void quoteStatusStringTest() {
        SQLDialect dialect = new SnowflakeSQLDialect();

        Assertions.assertFalse(dialect.mustBeQuoted("_unquotedIdentifier", false));
        Assertions.assertFalse(dialect.mustBeQuoted("unquotedIdentifier", false));
        Assertions.assertFalse(dialect.mustBeQuoted("unquoted_identifier", false));
        Assertions.assertFalse(dialect.mustBeQuoted("unquoted$identifier", false));
        Assertions.assertFalse(dialect.mustBeQuoted("Unquoted_Identifier", false));
        Assertions.assertFalse(dialect.mustBeQuoted("unqu0ted1dentifier", false));

        Assertions.assertTrue(dialect.mustBeQuoted("Бразилски_џијуџицу", false));
        Assertions.assertTrue(dialect.mustBeQuoted("", false));
        Assertions.assertFalse(dialect.mustBeQuoted("noquotesneededforsure", true));
    }
}
