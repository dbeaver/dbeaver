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

import java.util.HashSet;
import java.util.Set;

public class SQLFunctionRule implements TPRule {
    private final TPToken functionToken;
    private final Set<String> functions = new HashSet<>();
    private final StringBuilder buffer = new StringBuilder();

    public SQLFunctionRule(TPToken functionToken) {
        this.functionToken = functionToken;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        int c = scanner.read();
        int length = 1;

        if (c != TPCharacterScanner.EOF && Character.isUnicodeIdentifierStart(c)) {
            buffer.setLength(0);

            while (c != TPCharacterScanner.EOF && Character.isUnicodeIdentifierPart(c)) {
                buffer.append((char) c);
                length += 1;
                c = scanner.read();
            }

            String symbol = buffer.toString().toLowerCase();

            if (functions.contains(symbol)) {
                while (c != TPCharacterScanner.EOF && Character.isWhitespace(c) && c != '\n') {
                    c = scanner.read();
                    length += 1;
                }

                if (c == '(') {
                    scanner.unread();
                    return functionToken;
                }
            }
        }

        while (length > 0) {
            scanner.unread();
            length -= 1;
        }

        return TPTokenAbstract.UNDEFINED;
    }

    public void addFunction(String function) {
        functions.add(function.toLowerCase());
    }
}
