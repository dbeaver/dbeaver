/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.*;

import java.util.List;

/**
* Oracle dialect rules
*/
class OracleDialectRules implements TPRuleProvider {

    @Override
    public void extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull List<TPRule> rules, @NotNull RulePosition position) {
        if (position == RulePosition.INITIAL || position == RulePosition.PARTITION) {
            rules.add(new QStringRule());
        }
    }

    private static class QStringRule implements TPPredicateRule {

        private final TPToken stringToken;
        private char quoteStartChar = (char) -1;

        QStringRule() {
            stringToken = new TPTokenDefault(SQLTokenType.T_STRING);
        }

        private TPToken doEvaluate(TPCharacterScanner scanner, boolean resume) {
            int c = resume ? 'q' : scanner.read();
            if (c == 'Q' || c == 'q') {
                c = resume ? '\'' : scanner.read();
                if (c == '\'') {
                    boolean quoteCharRead = false;
                    if (resume && quoteStartChar != -1) {
                        quoteCharRead = true;
                    }
                    if (!quoteCharRead) {
                        quoteStartChar = (char) scanner.read();
                    }

                    if (!Character.isLetterOrDigit(quoteStartChar)) {
                        // Probably a Q-string
                        char quoteEndChar = getQuoteEndChar(quoteStartChar);
                        int charsRead = 0;
                        boolean isQuote = true;
                        for (;;) {
                            c = scanner.read();
                            charsRead++;
                            if (c == quoteEndChar) {
                                c = scanner.read();
                                charsRead++;
                                if (c == '\'') {
                                    break;
                                }
                            } else if (c == TPCharacterScanner.EOF) {
                                isQuote = false;
                                break;
                            }
                        }
                        if (isQuote) {
                            return stringToken;
                        } else {
                            for (int i = 0; i < charsRead; i++) {
                                scanner.unread();
                            }
                        }
                    } else {
                        quoteStartChar = (char) -1;
                        if (quoteCharRead) {
                            scanner.unread();
                        }
                    }
                } else {
                    scanner.unread();
                }
            } else {
                scanner.unread();
            }
            return TPTokenAbstract.UNDEFINED;
        }

        private static char getQuoteEndChar(char startChar) {
            switch (startChar) {
                case '<': return '>';
                case '(': return ')';
                case '[': return ']';
                case '{': return '}';
                default: return startChar;
            }
        }

        @Override
        public TPToken getSuccessToken() {
            return stringToken;
        }

        @Override
        public TPToken evaluate(TPCharacterScanner scanner) {
            return doEvaluate(scanner, false);
        }

        @Override
        public TPToken evaluate(TPCharacterScanner scanner, boolean resume) {
            return doEvaluate(scanner, resume);
        }
    }

}
