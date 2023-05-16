/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.parser.tokens.predicates;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;

/**
 * Dialect-specific predicate node producer
 */
class SQLTokenPredicateFactory extends TokenPredicateFactory {
    private static final Log log = Log.getLog(SQLTokenPredicateFactory.class);

    private final TPRule[] allRules;

    private static class StringScanner implements TPCharacterScanner {
        private final String string;
        private int pos = 0;

        public StringScanner(@NotNull String string) {
            this.string = string;
        }

        @Override
        public char[][] getLegalLineDelimiters() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getColumn() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getOffset() {
            return pos;
        }

        @Override
        public int read() {
            return pos >= 0 && pos < string.length() ? string.charAt(pos++) : -1;
        }

        @Override
        public void unread() {
            pos--;
        }

        public void reset() {
            pos = 0;
        }
    }

    public SQLTokenPredicateFactory(@NotNull SQLRuleManager ruleManager) {
        super();
        allRules = ruleManager.getAllRules();
    }

    @Override
    @NotNull
    protected SQLTokenEntry classifyToken(@NotNull String string) {
        StringScanner scanner = new StringScanner(string);
        for (TPRule fRule : allRules) {
            try {
                scanner.reset();
                TPToken token = fRule.evaluate(scanner);
                if (!token.isUndefined()) {
                    SQLTokenType tokenType = token instanceof TPTokenDefault ? (SQLTokenType) ((TPTokenDefault) token).getData() : SQLTokenType.T_OTHER;
                    return new SQLTokenEntry(string, tokenType, false);
                }
            } catch (Throwable e) {
                // some rules raise exceptions in a certain situations when the string does not correspond the rule
                log.debug(e.getMessage());
            }
        }
        return new SQLTokenEntry(string, null, false);
    }
}