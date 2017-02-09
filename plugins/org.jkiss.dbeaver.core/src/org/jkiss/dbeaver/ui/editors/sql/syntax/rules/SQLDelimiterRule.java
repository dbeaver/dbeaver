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
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
* DelimiterRule
*/
public class SQLDelimiterRule implements IRule {
    private final IToken token;
    private char[][] delimiters, origDelimiters;
    private char[] buffer, origBuffer;
    public SQLDelimiterRule(String[] delimiters, IToken token) {
        this.token = token;
        this.origDelimiters = this.delimiters = new char[delimiters.length][];
        int index = 0, maxLength = 0;
        for (String delim : delimiters) {
            this.delimiters[index] = delim.toCharArray();
            for (int i = 0; i < this.delimiters[index].length; i++) {
                this.delimiters[index][i] = Character.toUpperCase(this.delimiters[index][i]);
            }
            maxLength = Math.max(maxLength, this.delimiters[index].length);
            index++;
        }
        this.origBuffer = this.buffer = new char[maxLength];
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        for (int i = 0; ; i++) {
            int c = scanner.read();
            boolean matches = false;
            if (c != ICharacterScanner.EOF) {
                c = Character.toUpperCase(c);
                for (int k = 0; k < delimiters.length; k++) {
                    if (i < delimiters[k].length && delimiters[k][i] == c) {
                        buffer[i] = (char)c;
                        if (i == delimiters[k].length - 1 && equalsBegin(delimiters[k])) {
                            // Matched. Check next character
                            if (Character.isLetterOrDigit(c)) {
                                int cn = scanner.read();
                                scanner.unread();
                                if (Character.isLetterOrDigit(cn)) {
                                    matches = false;
                                    continue;
                                }
                            }
                            return token;
                        }
                        matches = true;
                        break;
                    }
                }
            }
            if (!matches) {
                for (int k = 0; k <= i; k++) {
                    scanner.unread();
                }
                return Token.UNDEFINED;
            }
        }
    }

    private boolean equalsBegin(char[] delimiter) {
        for (int i = 0; i < delimiter.length; i++) {
            if (buffer[i] != delimiter[i]) {
                return false;
            }
        }
        return true;
    }

    public void changeDelimiter(String newDelimiter) {
        if (CommonUtils.isEmpty(newDelimiter)) {
            this.delimiters = this.origDelimiters;
            this.buffer = this.origBuffer;
        } else {
            this.delimiters = new char[1][];
            this.delimiters[0] = newDelimiter.toUpperCase(Locale.ENGLISH).toCharArray();
            this.buffer = new char[newDelimiter.length()];
        }
    }
}
