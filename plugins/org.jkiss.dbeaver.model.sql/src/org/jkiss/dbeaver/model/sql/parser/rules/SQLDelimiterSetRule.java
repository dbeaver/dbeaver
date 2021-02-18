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
package org.jkiss.dbeaver.model.sql.parser.rules;

import org.jkiss.dbeaver.model.sql.parser.tokens.SQLSetDelimiterToken;
import org.jkiss.dbeaver.model.text.parser.*;

/**
* Delimiter redefine rule
*/
public class SQLDelimiterSetRule implements TPRule {

    private final String setDelimiterWord;
    private final SQLSetDelimiterToken setDelimiterToken;
    private final SQLDelimiterRule delimiterRule;

    public SQLDelimiterSetRule(String setDelimiterWord, SQLSetDelimiterToken setDelimiterToken, SQLDelimiterRule delimiterRule) {
        this.setDelimiterWord = setDelimiterWord;
        this.setDelimiterToken = setDelimiterToken;
        this.delimiterRule = delimiterRule;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        // Must be in the beginning of line
        {
            scanner.unread();
            int prevChar = scanner.read();
            if (prevChar != TPCharacterScanner.EOF && prevChar != '\r' && prevChar != '\n') {
                return TPTokenAbstract.UNDEFINED;
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
                return TPTokenAbstract.UNDEFINED;
            }
        }
        StringBuilder delimBuffer = new StringBuilder();
        int delimLength = 0;

        int next = scanner.read();
        if (next == TPCharacterScanner.EOF || next == '\n' || next == '\r') {
            // Empty delimiter
            scanner.unread();
        } else {
            if (!Character.isWhitespace(next)) {
                for (int k = 0; k < setDelimiterWord.length() + 1; k++) {
                    scanner.unread();
                }
                return TPTokenAbstract.UNDEFINED;
            }
            // Get everything till the end of line
            for (; ; ) {
                next = scanner.read();
                if (next == TPCharacterScanner.EOF || next == '\n' || next == '\r') {
                    break;
                } else if (delimLength == 0 && delimBuffer.length() > 0 && Character.isWhitespace(next)) {
                    delimLength = delimBuffer.length();
                }
                delimBuffer.append((char) next);
            }
            scanner.unread();
        }
        if (scanner instanceof TPEvalScanner && ((TPEvalScanner) scanner).isEvalMode()) {
            final String newDelimiter = delimLength <= 0 ?
                delimBuffer.toString().trim() : delimBuffer.substring(0, delimLength).trim();
            delimiterRule.changeDelimiter(newDelimiter);
        }

        return setDelimiterToken;
    }
}
