/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLParameterToken;

/**
* SQL parameter rule
*/
public class SQLParameterRule implements IRule {
    private final SQLSyntaxManager syntaxManager;
    private final SQLParameterToken parameterToken;
    private final StringBuilder buffer;
    private final char anonymousParameterMark;
    private final String namedParameterPrefix;

    public SQLParameterRule(SQLSyntaxManager syntaxManager, SQLParameterToken parameterToken, String prefix) {
        this.syntaxManager = syntaxManager;
        this.parameterToken = parameterToken;
        this.buffer = new StringBuilder();
        this.anonymousParameterMark = syntaxManager.getAnonymousParameterMark();
        this.namedParameterPrefix = prefix;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner)
    {
        scanner.unread();
        int prevChar = scanner.read();
        if (Character.isJavaIdentifierPart(prevChar) ||
            prevChar == namedParameterPrefix.charAt(0) || prevChar == anonymousParameterMark || prevChar == '\\' || prevChar == '/')
        {
            return Token.UNDEFINED;
        }
        int c = scanner.read();
        if (c != ICharacterScanner.EOF && (c == anonymousParameterMark || c == namedParameterPrefix.charAt(0))) {
            buffer.setLength(0);
            do {
                buffer.append((char) c);
                c = scanner.read();
            } while (c != ICharacterScanner.EOF && Character.isJavaIdentifierPart(c));
            scanner.unread();

            // Check for parameters
            if (syntaxManager.isAnonymousParametersEnabled()) {
                if (buffer.length() == 1 && buffer.charAt(0) == anonymousParameterMark) {
                    return parameterToken;
                }
            }
            if (syntaxManager.isParametersEnabled()) {
                if (buffer.charAt(0) == namedParameterPrefix.charAt(0) && buffer.length() > 1) {
                    boolean validChars = true;
                    for (int i = 1; i < buffer.length(); i++) {
                        if (!Character.isJavaIdentifierPart(buffer.charAt(i))) {
                            validChars = false;
                            break;
                        }
                    }
                    if (validChars) {
                        return parameterToken;
                    }
                }
            }

            for (int i = buffer.length() - 1; i >= 0; i--) {
                scanner.unread();
            }
        } else {
            scanner.unread();
        }
        return Token.UNDEFINED;
    }
}
