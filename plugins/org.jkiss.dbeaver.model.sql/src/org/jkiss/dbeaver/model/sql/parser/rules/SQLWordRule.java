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
package org.jkiss.dbeaver.model.sql.parser.rules;

import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;

import java.util.HashMap;
import java.util.Map;


/**
 * Smart word detector
 */
public class SQLWordRule implements TPRule {

    private SQLDelimiterRule delimRule;
    private TPToken defaultToken;
    private Map<String, TPToken> fWords = new HashMap<>();
    private StringBuilder fBuffer = new StringBuilder();
    private char[][] delimiters;

    public SQLWordRule(SQLDelimiterRule delimRule, TPToken defaultToken) {
        this.delimRule = delimRule;
        this.defaultToken = defaultToken;
    }

    public boolean hasWord(String word) {
        return fWords.containsKey(word.toLowerCase());
    }

    public void addWord(String word, TPToken token) {
        fWords.put(word.toLowerCase(), token);
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        int c = scanner.read();
        if (c != TPCharacterScanner.EOF && Character.isUnicodeIdentifierStart(c)) {
            fBuffer.setLength(0);
            delimiters = delimRule.getDelimiters();
            do {
                fBuffer.append((char) c);
                c = scanner.read();
            } while (c != TPCharacterScanner.EOF && isWordPart((char) c, scanner));
            scanner.unread();

            String buffer = fBuffer.toString().toLowerCase();
            TPToken token = fWords.get(buffer);

            if (token != null)
                return token;

            if (defaultToken.isUndefined())
                unreadBuffer(scanner);

            return defaultToken;
        }

        scanner.unread();
        return TPTokenAbstract.UNDEFINED;
    }

    private boolean isWordPart(char c, TPCharacterScanner scanner) {
        if (!Character.isUnicodeIdentifierPart(c) && c != '$') {
            return false;
        }
        // Check for delimiter
        for (char[] wordDelimiter : delimiters) {
            if (!Character.isLetter(c) && c == wordDelimiter[0]) {
                if (wordDelimiter.length == 1) {
                    return false;
                }
                int charsRead = 0;
                boolean matches = true;
                for (int i = 1; i < wordDelimiter.length; i++) {
                    int c2 = scanner.read();
                    charsRead++;
                    if (c2 == TPCharacterScanner.EOF) {
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

    private void unreadBuffer(TPCharacterScanner scanner) {
        for (int i = fBuffer.length() - 1; i >= 0; i--) {
            scanner.unread();
        }
    }

}
