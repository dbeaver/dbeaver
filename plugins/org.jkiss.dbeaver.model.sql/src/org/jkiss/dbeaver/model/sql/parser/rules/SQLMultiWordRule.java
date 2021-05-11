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

import org.eclipse.core.runtime.Assert;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;

/**
 * Rule for matching tokens consisting of several words separated by one or more whitespaces.
 */
public class SQLMultiWordRule implements TPRule {
    private final String[] parts;
    private final TPToken token;

    public SQLMultiWordRule(String[] parts, TPToken token) {
        this.parts = parts;
        this.token = token;
        Assert.isLegal(parts.length > 1, "Multi-word rule should consist of two or more parts");
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        int ch = scanner.read();
        int read = 1;

        outer:
        for (int partIndex = 0; partIndex < parts.length; partIndex++) {
            if (ch == TPCharacterScanner.EOF || !Character.isUnicodeIdentifierStart(ch)) {
                break;
            }

            for (char partCh : parts[partIndex].toCharArray()) {
                if (ch == TPCharacterScanner.EOF || !Character.isUnicodeIdentifierPart(ch) || Character.toUpperCase(partCh) != Character.toUpperCase(ch)) {
                    break outer;
                }

                ch = scanner.read();
                read++;
            }

            if (partIndex == parts.length - 1 && !Character.isUnicodeIdentifierPart(ch)) {
                // Accept rule if last part is preceded by non-identifier character
                scanner.unread();
                return token;
            }

            if (ch == TPCharacterScanner.EOF || !Character.isWhitespace(ch)) {
                // Require at least one whitespace character between parts
                break;
            }

            while (ch != TPCharacterScanner.EOF && Character.isWhitespace(ch)) {
                ch = scanner.read();
                read++;
            }
        }

        while (read > 0) {
            scanner.unread();
            read--;
        }

        return TPTokenAbstract.UNDEFINED;
    }
}
