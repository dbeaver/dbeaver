/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.snowflake;

import org.junit.Test;

import static org.jkiss.dbeaver.ext.snowflake.SnowflakeUtils.*;
import static org.junit.Assert.*;

public class SnowflakeUtilsTest {
    @Test
    public void isCaseSensitiveIdentifierTest() {
        assertFalse(isCaseSensitiveIdentifier("StartsWithUpperCaseLetter"));
        assertFalse(isCaseSensitiveIdentifier("startsWithLowerCaseLetter"));
        assertFalse(isCaseSensitiveIdentifier("_StartsWithUnderscore"));
        assertFalse(isCaseSensitiveIdentifier("ContainsLetters_Underscores$DollarSigns0123456789andDigits"));

        assertTrue(isCaseSensitiveIdentifier("\"surroundedByDoubleQuotes\""));
        assertTrue(isCaseSensitiveIdentifier(" May_even_start_with_a_blank"));
    }

    @Test
    public void escapeIdentifierTest() {
        String identifier = "StartsWithUpperCaseLetter";
        assertEquals(identifier, escapeIdentifier(identifier));
        identifier = "startsWithLowerCaseLetter";
        assertEquals(identifier, escapeIdentifier(identifier));
        identifier = "_StartsWithUnderscore";
        assertEquals(identifier, escapeIdentifier(identifier));
        identifier = "ContainsLetters_Underscores$DollarSigns0123456789andDigits";
        assertEquals(identifier, escapeIdentifier(identifier));

        assertEquals("\"quote\"\"andunquote\"\"\"", escapeIdentifier("quote\"andunquote\""));
    }
}
