/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.ui.internal;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.runtime.sql.SQLRuleProvider;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

/**
* Oracle dialect rules
*/
class OracleDialectRules implements SQLRuleProvider {

    @Override
    public void extendRules(@NotNull List<IRule> rules, @NotNull RulePosition position) {
        if (position == RulePosition.INITIAL || position == RulePosition.PARTITION) {
            rules.add(new QStringRule(position == RulePosition.PARTITION));
        }
    }

    private static class QStringRule implements IPredicateRule {

        private final IToken stringToken;
        private char quoteStartChar = (char) -1;

        public QStringRule(boolean isPartitionRule) {
            if (isPartitionRule) {
                stringToken = new Token(SQLParserPartitions.CONTENT_TYPE_SQL_STRING);
            } else {
                stringToken = new Token(
                    new TextAttribute(UIUtils.getGlobalColor(SQLConstants.CONFIG_COLOR_STRING), null, SWT.NORMAL));
            }
        }

        private IToken doEvaluate(ICharacterScanner scanner, boolean resume) {
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
                            } else if (c == ICharacterScanner.EOF) {
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
            return Token.UNDEFINED;
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
        public IToken getSuccessToken() {
            return stringToken;
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner) {
            return doEvaluate(scanner, false);
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner, boolean resume) {
            return doEvaluate(scanner, resume);
        }
    }

}
