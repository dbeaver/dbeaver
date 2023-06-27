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
package org.jkiss.dbeaver.ext.altibase.sql;

import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPPredicateRule;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;

/**
* Altibase dialect rules
*/
class AltibaseDialectRules implements TPRuleProvider {

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
                    boolean quoteCharNeedsToBeUnread = false;
                    if (resume && quoteStartChar != -1) {
                        quoteCharRead = true;
                    }
                    if (!quoteCharRead) {
                        quoteStartChar = (char) scanner.read();
                        quoteCharNeedsToBeUnread = true;
                    }

                    if (!Character.isLetterOrDigit(quoteStartChar)) {
                        // Probably a Q-string
                        char quoteEndChar = getQuoteEndChar(quoteStartChar);

                        if (tryReadQString(scanner, quoteEndChar)) {
                            return stringToken;
                        }
                        if (quoteCharNeedsToBeUnread) {
                            scanner.unread();
                        }
                    } else {
                        quoteStartChar = (char) -1;
                        if (quoteCharRead || quoteCharNeedsToBeUnread) {
                            scanner.unread();
                        }
                    }
                }
                if (!resume) {
                    scanner.unread();
                }
            }
            if (!resume) {
                scanner.unread();
            }
            return TPTokenAbstract.UNDEFINED;
        }

        private boolean tryReadQString(TPCharacterScanner scanner, char quoteEndChar) {
            int charsRead = 0;
            int prevChar = -1, currChar = -1;
            boolean isEndOfLiteral, isEndOfText;
            do {
                prevChar = currChar;
                currChar = scanner.read();
                charsRead++;
                isEndOfLiteral = prevChar == quoteEndChar && currChar == '\'';
                isEndOfText = currChar == TPCharacterScanner.EOF;
            } while (!isEndOfLiteral && !isEndOfText);

            if (isEndOfText) {
                for (int i = 0; i < charsRead; i++) {
                    scanner.unread();
                }
            }
            return isEndOfLiteral;
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
