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

import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;

/**
* SQL variable rule.
* ${varName}
*/
public class SQLVariableRule implements TPRule {

    private final TPToken parameterToken;

    public SQLVariableRule(TPToken parameterToken) {
        this.parameterToken = parameterToken;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner)
    {
        int c = scanner.read();
        if (c == '$') {
            int prefixLength = 0;
            c = scanner.read();
            if (SQLQueryParameter.supportsJasperSyntax()) {
                if (c == 'P') {
                    c = scanner.read();
                    prefixLength++;
                    if (c == '!') {
                        c = scanner.read();
                        prefixLength++;
                    }
                }
            }
            if (c == '{') {
                int varLength = 0;
                for (;;) {
                    c = scanner.read();
                    if (c == '}' || Character.isWhitespace(c) || c == TPCharacterScanner.EOF) {
                        break;
                    }
                    varLength++;
                }
                if (varLength > 0 && c == '}') {
                    return parameterToken;
                }
                scanner.unread();

                for (int i = varLength - 1 + prefixLength; i >= 0; i--) {
                    scanner.unread();
                }
            }
            scanner.unread();
        }
        scanner.unread();

        return TPTokenAbstract.UNDEFINED;
    }

}
