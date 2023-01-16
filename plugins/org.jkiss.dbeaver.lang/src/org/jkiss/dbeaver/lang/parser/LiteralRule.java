/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.lang.parser;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.jkiss.dbeaver.lang.SCMToken;

public class LiteralRule implements IRule {

    private static Token WORD_TOKEN = new Token(SCMToken.LITERAL);

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        if (c != ICharacterScanner.EOF && isWordStart((char) c)) {
            do {
                c = scanner.read();
            }
            while (c != ICharacterScanner.EOF && isWordPart((char) c));
            scanner.unread();

            return WORD_TOKEN;
        }

        scanner.unread();
        return Token.UNDEFINED;
    }

    private boolean isWordStart(char c) {
        return Character.isLetter(c);
    }

    private boolean isWordPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }

}
