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
package org.jkiss.dbeaver.ext.postgresql.sql;

import org.eclipse.jface.text.Document;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PostgreEscapeStringRuleTest extends DBeaverUnitTest {

    @Test
    public void recognizesEscapeStringAtLineStart() {
        assertToken("E'value' ", 0, SQLTokenType.T_STRING, 8);
        assertToken("e'value' ", 0, SQLTokenType.T_STRING, 8);
    }

    @Test
    public void recognizesEscapeStringAfterWhitespace() {
        assertToken("SELECT E'value' ", 7, SQLTokenType.T_STRING, 15);
    }

    @Test
    public void ignoresEscapeStringAfterIdentifier() {
        assertUndefined("SELECT xE'value'", 8, 8);
    }

    @Test
    public void ignoresUnclosedEscapeString() {
        assertUndefined("SELECT E'value", 7, 7);
    }

    private static void assertToken(
        @NotNull String text,
        int offset,
        @NotNull SQLTokenType expectedType,
        int expectedEndOffset
    ) {
        TPRuleBasedScanner scanner = createScanner(text, offset);
        TPToken token = new PostgreEscapeStringRule().evaluate(scanner);
        Assertions.assertFalse(token.isUndefined());
        Assertions.assertEquals(expectedType, token.getData());
        Assertions.assertEquals(expectedEndOffset, scanner.getOffset());
    }

    private static void assertUndefined(@NotNull String text, int offset, int expectedEndOffset) {
        TPRuleBasedScanner scanner = createScanner(text, offset);
        Assertions.assertTrue(new PostgreEscapeStringRule().evaluate(scanner).isUndefined());
        Assertions.assertEquals(expectedEndOffset, scanner.getOffset());
    }

    @NotNull
    private static TPRuleBasedScanner createScanner(@NotNull String text, int offset) {
        TPRuleBasedScanner scanner = new TPRuleBasedScanner();
        scanner.setRange(new Document(text), offset, text.length() - offset);
        return scanner;
    }
}
