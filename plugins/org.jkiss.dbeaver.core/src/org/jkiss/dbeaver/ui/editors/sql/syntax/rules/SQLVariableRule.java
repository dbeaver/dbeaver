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
package org.jkiss.dbeaver.ui.editors.sql.syntax.rules;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

import java.util.regex.Pattern;

/**
* SQL variable rule.
* ${varName}
*/
public class SQLVariableRule implements IRule {

    public static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{[^\\s]+\\}");

    private final IToken parameterToken;

    public SQLVariableRule(IToken parameterToken) {
        this.parameterToken = parameterToken;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner)
    {
        int c = scanner.read();
        if (c == '$') {
            c = scanner.read();
            if (c == '{') {
                int varLength = 0;
                for (;;) {
                    c = scanner.read();
                    if (c == '}' || Character.isWhitespace(c) || c == ICharacterScanner.EOF) {
                        break;
                    }
                    varLength++;
                }
                if (varLength > 0 && c == '}') {
                    return parameterToken;
                }
                scanner.unread();

                for (int i = varLength - 1; i >= 0; i--) {
                    scanner.unread();
                }
            }
            scanner.unread();
        }
        scanner.unread();

        return Token.UNDEFINED;
    }
}
