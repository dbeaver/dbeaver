package org.jkiss.dbeaver.ui.editors.sql.syntax.extended;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.dbeaver.model.text.parser.TPTokenType;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleScanner;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLTokenAdapter;

public class SQLPassiveSyntaxRule implements IPredicateRule {
    
    static class LazyToken extends TPTokenDefault {
        public LazyToken(TPTokenType type) {
            super(type);
        }
    }

    private final SQLBackgroundParsingJob backgroundParsingJob;
    private final SQLTokenAdapter token;
    private final SQLTokenType tokenType;
    
    public SQLPassiveSyntaxRule(SQLBackgroundParsingJob backgroundParsingJob, SQLRuleScanner sqlRuleScanner, SQLTokenType tokenType) {
        this.backgroundParsingJob = backgroundParsingJob;
        this.token = new SQLTokenAdapter(new LazyToken(tokenType), sqlRuleScanner);
        this.tokenType = tokenType;
    }

    @Override
    public IToken getSuccessToken() {
        return token;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        if (scanner instanceof TPCharacterScanner s) {
            int offset = s.getOffset();
            TokenEntry entry = this.backgroundParsingJob.getCurrentContext().findToken(offset);
            if (entry != null && entry.tokenType.equals(this.tokenType) && s.getOffset() < entry.end()) {
                StringBuilder sb = new StringBuilder();
                while (s.getOffset() < entry.end()) {
                    sb.append((char)s.read());
                }
                // System.out.println("found @" + offset + " " + entry + " = " + sb.toString());
                return this.token;
            } else {
                return Token.UNDEFINED;
            }
        } else {
            return Token.UNDEFINED;
        }
    }
    
    @Override
    public IToken evaluate(ICharacterScanner scanner, boolean resume) {
        return this.evaluate(scanner, false);
    }
}
