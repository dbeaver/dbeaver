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
package org.jkiss.dbeaver.ui.editors.sql.syntax.rules;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLSetDelimiterToken;

/**
* Delimiter redefien rule
*/
public class SQLDelimiterSetRule implements IRule {

    private final String setDelimiterWord;
    private final SQLSetDelimiterToken setDelimiterToken;
    private final SQLDelimiterRule delimiterRule;

    public SQLDelimiterSetRule(String setDelimiterWord, SQLSetDelimiterToken setDelimiterToken, SQLDelimiterRule delimiterRule) {
        this.setDelimiterWord = setDelimiterWord;
        this.setDelimiterToken = setDelimiterToken;
        this.delimiterRule = delimiterRule;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        // Must be in the beginning of line
        {
            scanner.unread();
            int prevChar = scanner.read();
            if (prevChar != ICharacterScanner.EOF && prevChar != '\r' && prevChar != '\n') {
                return Token.UNDEFINED;
            }
        }

        for (int i = 0; i < setDelimiterWord.length(); i++) {
            char c = setDelimiterWord.charAt(i);
            final int nextChar = scanner.read();
            if (Character.toUpperCase(nextChar) != c) {
                // Doesn't match
                for (int k = 0; k <= i; k++) {
                    scanner.unread();
                }
                return Token.UNDEFINED;
            }
        }
        StringBuilder delimBuffer = new StringBuilder();
        int delimLength = 0;

        int next = scanner.read();
        if (next == ICharacterScanner.EOF || next == '\n' || next == '\r') {
            // Empty delimiter
            scanner.unread();
        } else {
            if (!Character.isWhitespace(next)) {
                for (int k = 0; k < setDelimiterWord.length() + 1; k++) {
                    scanner.unread();
                }
                return Token.UNDEFINED;
            }
            // Get everything till the end of line
            for (; ; ) {
                next = scanner.read();
                if (next == ICharacterScanner.EOF || next == '\n' || next == '\r') {
                    break;
                } else if (delimLength == 0 && delimBuffer.length() > 0 && Character.isWhitespace(next)) {
                    delimLength = delimBuffer.length();
                }
                delimBuffer.append((char) next);
            }
            scanner.unread();
        }
        if (scanner instanceof SQLRuleManager && ((SQLRuleManager) scanner).isEvalMode()) {
            final String newDelimiter = delimLength <= 0 ?
                delimBuffer.toString().trim() : delimBuffer.substring(0, delimLength).trim();
            delimiterRule.changeDelimiter(newDelimiter);
        }

        return setDelimiterToken;
    }
}
