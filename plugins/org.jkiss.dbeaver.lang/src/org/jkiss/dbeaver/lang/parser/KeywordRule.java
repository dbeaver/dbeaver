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
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.jkiss.dbeaver.lang.SCMKeyword;
import org.jkiss.dbeaver.lang.SCMKeywordToken;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class KeywordRule extends LiteralRule {

    private final Map<String, SCMKeyword> keywordMap = new HashMap<>();

    private final StringBuilder buffer = new StringBuilder();

    public KeywordRule(SCMKeyword[] keywords) {
        for (SCMKeyword keyword : keywords) {
            this.keywordMap.put(keyword.name(), keyword);
        }
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        if (c != ICharacterScanner.EOF && isWordStart((char) c)) {
            buffer.setLength(0);
            do {
                buffer.append((char)c);
                c = scanner.read();
            }
            while (c != ICharacterScanner.EOF && isWordPart((char) c));
            scanner.unread();

            SCMKeyword keyword = keywordMap.get(buffer.toString().toUpperCase(Locale.ENGLISH));
            if (keyword != null) {
                return new SCMKeywordToken(keyword);
            }
            for (int i = buffer.length() - 1; i > 0; i--) {
                scanner.unread();
            }
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
