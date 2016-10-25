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
