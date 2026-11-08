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
package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.Document;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDollarQuoteRule;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SQLDollarQuoteRuleTest extends DBeaverUnitTest {

    @Test
    public void consumesUnnamedDollarQuote() {
        Result result = evaluate("$$body$$");
        Assertions.assertEquals(SQLTokenType.T_STRING, result.token().getData());
        Assertions.assertEquals(8, result.offset());
    }

    @Test
    public void consumesNamedDollarQuote() {
        Result result = evaluate("$tag$body$tag$");
        Assertions.assertEquals(SQLTokenType.T_STRING, result.token().getData());
        Assertions.assertEquals(14, result.offset());
    }

    @Test
    public void ignoresPositionalParameterWithoutScanningTheRest() {
        Result result = evaluate("$1 + 2");
        Assertions.assertTrue(result.token().isUndefined());
        Assertions.assertEquals(0, result.offset());
    }

    @Test
    public void ignoresDigitLeadingTag() {
        Result result = evaluate("$1$body$1$");
        Assertions.assertTrue(result.token().isUndefined());
        Assertions.assertEquals(0, result.offset());
    }

    @Test
    public void ignoresUnclosedNamedDollarQuote() {
        Result result = evaluate("$tag$body");
        Assertions.assertTrue(result.token().isUndefined());
        Assertions.assertEquals(0, result.offset());
    }

    private static Result evaluate(String text) {
        TPRuleBasedScanner scanner = new TPRuleBasedScanner();
        scanner.setRange(new Document(text), 0, text.length());
        TPToken token = new SQLDollarQuoteRule(true, true, true, true).evaluate(scanner);
        return new Result(token, scanner.getOffset());
    }

    private record Result(TPToken token, int offset) {
    }
}
