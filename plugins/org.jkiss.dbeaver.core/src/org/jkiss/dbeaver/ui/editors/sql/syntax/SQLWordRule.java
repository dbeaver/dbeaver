/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.rules.*;
import org.jkiss.dbeaver.ui.editors.sql.syntax.rules.SQLDelimiterRule;


/**
 * Smart word detector
 */
public class SQLWordRule implements IRule {

    private SQLDelimiterRule delimRule;
    private IToken defaultToken;
    private Map<String, IToken> fWords = new HashMap<>();
    private StringBuilder fBuffer = new StringBuilder();
    private char[][] delimiters;

    public SQLWordRule(SQLDelimiterRule delimRule, IToken defaultToken) {
        this.delimRule = delimRule;
        this.defaultToken = defaultToken;
    }

    public void addWord(String word, IToken token) {
        fWords.put(word.toLowerCase(), token);
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        if (c != ICharacterScanner.EOF && Character.isUnicodeIdentifierStart(c)) {
            fBuffer.setLength(0);
            delimiters = delimRule.getDelimiters();
            do {
                fBuffer.append((char) c);
                c = scanner.read();
            } while (c != ICharacterScanner.EOF && isWordPart((char) c, scanner));
            scanner.unread();

            String buffer = fBuffer.toString().toLowerCase();
            IToken token = fWords.get(buffer);

            if (token != null)
                return token;

            if (defaultToken.isUndefined())
                unreadBuffer(scanner);

            return defaultToken;
        }

        scanner.unread();
        return Token.UNDEFINED;
    }

    private boolean isWordPart(char c, ICharacterScanner scanner) {
        if (!Character.isUnicodeIdentifierPart(c) && c != '$') {
            return false;
        }
        // Check for delimiter
        for (char[] wordDelimiter : delimiters) {
            if (c == wordDelimiter[0]) {
                if (wordDelimiter.length == 1) {
                    return false;
                }
                int charsRead = 0;
                boolean matches = true;
                for (int i = 1; i < wordDelimiter.length; i++) {
                    int c2 = scanner.read();
                    charsRead++;
                    if (c2 == ICharacterScanner.EOF) {
                        break;
                    }
                    if (c2 != wordDelimiter[i]) {
                        matches = false;
                        break;
                    }
                }
                for (int i = 0; i < charsRead; i++) {
                    scanner.unread();
                }
                if (matches) {
                    return false;
                }
            }
        }

        return true;
    }

    private void unreadBuffer(ICharacterScanner scanner) {
        for (int i = fBuffer.length() - 1; i >= 0; i--) {
            scanner.unread();
        }
    }

}
