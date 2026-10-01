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
package org.jkiss.dbeaver.model.sql.parser.rules;

import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;

/**
 * SQL variable rule.
 * ${varName} or {{varName}}
 */
public class ScriptVariableRule implements TPRule {

    private final TPToken parameterToken;

    public ScriptVariableRule(TPToken parameterToken) {
        this.parameterToken = parameterToken;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner)
    {
        int c = scanner.read();
        if (c == '$') {
            if (evaluateDollarVariable(scanner)) {
                return parameterToken;
            }
        } else if (c == '{') {
            if (evaluateDoubleCurlyVariable(scanner)) {
                return parameterToken;
            }
        }
        scanner.unread();

        return TPTokenAbstract.UNDEFINED;
    }

    private boolean evaluateDollarVariable(TPCharacterScanner scanner) {
        int c = scanner.read();
        int prefixLength = 0;
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
            if (evaluateVariableName(scanner, 1)) {
                return true;
            }
            scanner.unread();
        } else {
            scanner.unread();
        }
        for (int i = 0; i < prefixLength; i++) {
            scanner.unread();
        }
        return false;
    }

    private boolean evaluateDoubleCurlyVariable(TPCharacterScanner scanner) {
        int c = scanner.read();
        if (c == '{') {
            if (evaluateVariableName(scanner, 2)) {
                return true;
            }
        }
        scanner.unread();
        return false;
    }

    private boolean evaluateVariableName(TPCharacterScanner scanner, int closingBraceCount) {
        int c;
        int varLength = 0;
        for (;;) {
            c = scanner.read();
            if (c == '}' || Character.isWhitespace(c) || c == TPCharacterScanner.EOF) {
                break;
            }
            varLength++;
        }
        if (varLength > 0 && c == '}') {
            if (closingBraceCount == 1) {
                return true;
            }
            c = scanner.read();
            if (c == '}') {
                return true;
            }
            scanner.unread();
        }
        scanner.unread();
        for (int i = 0; i < varLength; i++) {
            scanner.unread();
        }
        return false;
    }

}
