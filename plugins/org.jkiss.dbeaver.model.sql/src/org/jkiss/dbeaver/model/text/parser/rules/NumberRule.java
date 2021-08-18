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
package org.jkiss.dbeaver.model.text.parser.rules;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;
import org.jkiss.utils.CommonUtils;


/**
 * An implementation of <code>IRule</code> detecting a numerical value
 * with optional decimal part, scientific notation (<code>10e-3</code>)
 * and support for the hexadecimal base (16).
 */
public class NumberRule implements TPRule {

    public static final int RADIX_DECIMAL = 10;
    public static final int RADIX_HEXADECIMAL = 16;

    /**
     * The token to be returned when this rule is successful
     */
    protected TPToken fToken;

    /**
     * Creates a rule which will return the specified
     * token when a numerical sequence is detected.
     *
     * @param token the token to be returned
     */
    public NumberRule(@NotNull TPToken token) {
        fToken = token;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        int ch = scanner.read();
        int chCount = 1;

        if (!CommonUtils.isDigit(ch, RADIX_DECIMAL)) {
            return undefined(scanner, 1);
        }

        boolean seenDecimalSeparator = false;
        boolean seenScientificNotation = false;
        int radix = RADIX_DECIMAL;

        if (ch == '0') {
            int ch1 = scanner.read();
            if (ch1 == 'x' || ch1 == 'X') {
                ch1 = scanner.read();
                if (CommonUtils.isDigit(ch1, RADIX_HEXADECIMAL)) {
                    radix = RADIX_HEXADECIMAL;
                } else {
                    return undefined(scanner, 3);
                }
            } else {
                scanner.unread();
            }
        }

        while (true) {
            if (radix == RADIX_DECIMAL && ch == '.') {
                if (seenDecimalSeparator) {
                    return undefined(scanner, chCount);
                }
                ch = scanner.read();
                chCount++;
                if (ch < '0' || ch > '9') {
                    return undefined(scanner, chCount);
                }
                seenDecimalSeparator = true;
                continue;
            }

            if (radix == RADIX_DECIMAL && (ch == 'e' || ch == 'E')) {
                if (seenScientificNotation) {
                    return undefined(scanner, chCount);
                }
                ch = scanner.read();
                chCount++;
                if (ch == '+' || ch == '-') {
                    ch = scanner.read();
                    chCount++;
                }
                if (ch < '0' || ch > '9') {
                    return undefined(scanner, chCount);
                }
                seenScientificNotation = true;
                continue;
            }

            if (!CommonUtils.isDigit(ch, radix)) {
                scanner.unread();
                return fToken;
            }

            ch = scanner.read();
            chCount++;
        }
    }

    private static TPToken undefined(TPCharacterScanner scanner, int readCount) {
        while (readCount > 0) {
            readCount--;
            scanner.unread();
        }
        return TPTokenAbstract.UNDEFINED;
    }
}
