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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Smart word detector
 */
public class SQLWordRule implements TPRule {

    private final SQLDelimiterRule delimRule;
    private final TPToken functionToken;
    private final TPToken defaultToken;
    private final Map<String, TPToken> words = new HashMap<>();
    private final Set<String> functions = new HashSet<>();
    private final StringBuilder buffer = new StringBuilder();
    private final SQLDialect dialect;
    private char[][] delimiters;

    public SQLWordRule(SQLDelimiterRule delimRule, TPToken functionToken, TPToken defaultToken, @NotNull SQLDialect dialect) {
        this.delimRule = delimRule;
        this.functionToken = functionToken;
        this.defaultToken = defaultToken;
        this.dialect = dialect;
    }

    public boolean hasWord(String word) {
        return words.containsKey(word.toLowerCase());
    }

    public void addWord(String word, TPToken token) {
        words.put(word.toLowerCase(), token);
    }

    public boolean hasFunction(String function) {
        return functions.contains(function);
    }

    public void addFunction(String function) {
        functions.add(function.toLowerCase());
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        int c = scanner.read();
        if (c != TPCharacterScanner.EOF && dialect.isWordStart(c)) {
            buffer.setLength(0);
            delimiters = delimRule.getDelimiters();
            char prevC;
            do {
                prevC = (char)c;
                buffer.append((char) c);
                c = scanner.read();
            } while (c != TPCharacterScanner.EOF && isWordPart((char) c, prevC, scanner));
            scanner.unread();

            String buffer = this.buffer.toString().toLowerCase();
            TPToken token = words.get(buffer);

            if (functions.contains(buffer)) {
                int length = 0;
                while (c != TPCharacterScanner.EOF && Character.isWhitespace(c)) {
                    c = scanner.read();
                    length += 1;
                }
                while (length > 0) {
                    scanner.unread();
                    length -= 1;
                }
                if (c == '(' || token == null) {
                    return functionToken;
                }
            }

            if (token != null)
                return token;

            if (defaultToken.isUndefined())
                unreadBuffer(scanner);

            return defaultToken;
        }

        scanner.unread();
        return TPTokenAbstract.UNDEFINED;
    }

    private boolean isWordPart(char c, char prevC, TPCharacterScanner scanner) {
        if (!dialect.isWordPart(c) && c != '$') {
            return false;
        }
        if (c == '$' && prevC == '$') {
            // Double dollar. Prev dollar is also wrong char
            scanner.unread();
            buffer.setLength(buffer.length() - 1);
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
        for (int i = buffer.length() - 1; i >= 0; i--) {
            scanner.unread();
        }
    }

}
