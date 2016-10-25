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
