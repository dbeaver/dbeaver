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
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLVariableToken;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPPredicateRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;

/**
 * Rule that matches {@code @variableName} supported by some dialects.
 */
public class SQLVariableRule implements TPPredicateRule {

    private final SQLDialect dialect;
    private final TPToken token;

    public SQLVariableRule(@NotNull SQLDialect dialect) {
        this.dialect = dialect;
        this.token = new SQLVariableToken();
    }

    @Override
    public TPToken getSuccessToken() {
        return token;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner, boolean resume) {
        scanner.unread();

        int ch = scanner.read();
        int read = 0;

        if (!dialect.validIdentifierPart((char) ch, false)) {
            ch = scanner.read();
            read++;

            if (ch == '@') {
                do {
                    ch = scanner.read();
                    read++;
                } while (dialect.validIdentifierPart((char) ch, false));

                if (read > 2) {
                    scanner.unread();
                    return token;
                }
            }
        }

        while (read-- > 0) {
            scanner.unread();
        }

        return TPTokenAbstract.UNDEFINED;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        return evaluate(scanner, false);
    }
}
