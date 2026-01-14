/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.sql;

import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.*;

/**
 * This rule matches string literals with C-Style escapes, as
 * described in <b>4.1.2.2</b> chapter of PostgreSQL documentation.
 *
 * @see <a href="https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-STRINGS-ESCAPE">4.1.2.2. String Constants with C-Style Escapes</a>
 */
public class PostgreEscapeStringRule implements TPPredicateRule {
    private final TPToken stringToken = new TPTokenDefault(SQLTokenType.T_STRING);

    @Override
    public TPToken getSuccessToken() {
        return stringToken;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner, boolean resume) {
        int ch;
        int chRead = 2;

        if (scanner.getColumn() > 0) {
            scanner.unread();
            if (Character.isLetterOrDigit(ch = scanner.read()) || ch == '_') {
                // Previous character is a part of identifier, we
                // don't want to take a bite of it by accident
                return TPTokenAbstract.UNDEFINED;
            }
        }

        if ((ch = scanner.read()) != 'e' && ch != 'E') {
            scanner.unread();
            return TPTokenAbstract.UNDEFINED;
        }

        if (scanner.read() != '\'') {
            scanner.unread();
            scanner.unread();
            return TPTokenAbstract.UNDEFINED;
        }

        do {
            ch = scanner.read();
            chRead++;

            if (ch == '\\') {
                ch = scanner.read();
                chRead++;
            } else if (ch == '\'') {
                ch = scanner.read();
                chRead++;
                if (ch != '\'' && ch != TPCharacterScanner.EOF) {
                    scanner.unread();
                    return stringToken;
                }
            }
        } while (ch != TPCharacterScanner.EOF);

        while (chRead-- > 0) {
            scanner.unread();
        }

        return TPTokenAbstract.UNDEFINED;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        return evaluate(scanner, false);
    }
}
