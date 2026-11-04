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
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PostgreEscapeStringRuleTest extends DBeaverUnitTest {

    @Test
    public void recognizesEscapeStringAtLineStart() {
        Assertions.assertEquals(SQLTokenType.T_STRING, evaluateToken("E'value' ", 0));
        Assertions.assertEquals(SQLTokenType.T_STRING, evaluateToken("e'value' ", 0));
    }

    @Test
    public void recognizesEscapeStringAfterWhitespace() {
        Assertions.assertEquals(SQLTokenType.T_STRING, evaluateToken("SELECT E'value' ", 7));
    }

    @Test
    public void ignoresEscapeStringAfterIdentifier() {
        TPToken token = evaluate("SELECT xE'value'", 8);
        Assertions.assertTrue(token.isUndefined());
    }

    @Test
    public void ignoresUnclosedEscapeString() {
        TPToken token = evaluate("SELECT E'value", 7);
        Assertions.assertTrue(token.isUndefined());
    }

    private static SQLTokenType evaluateToken(String text, int offset) {
        TPToken token = evaluate(text, offset);
        Assertions.assertFalse(token.isUndefined());
        return (SQLTokenType) token.getData();
    }

    private static TPToken evaluate(String text, int offset) {
        TPRuleBasedScanner scanner = new TPRuleBasedScanner();
        scanner.setRange(new Document(text), offset, text.length() - offset);
        return new PostgreEscapeStringRule().evaluate(scanner);
    }
}
