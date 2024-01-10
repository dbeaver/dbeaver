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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class SnowflakeSQLDialectTest {
    @Test
    public void quoteStatusStringTest() {
        SQLDialect dialect = new SnowflakeSQLDialect();

        assertFalse(dialect.mustBeQuoted("_unquotedIdentifier", false));
        assertFalse(dialect.mustBeQuoted("unquotedIdentifier", false));
        assertFalse(dialect.mustBeQuoted("unquoted_identifier", false));
        assertFalse(dialect.mustBeQuoted("unquoted$identifier", false));
        assertFalse(dialect.mustBeQuoted("Unquoted_Identifier", false));
        assertFalse(dialect.mustBeQuoted("unqu0ted1dentifier", false));

        assertTrue(dialect.mustBeQuoted("Бразилски_џијуџицу", false));
        assertTrue(dialect.mustBeQuoted("", false));
        assertFalse(dialect.mustBeQuoted("noquotesneededforsure", true));
    }
}
