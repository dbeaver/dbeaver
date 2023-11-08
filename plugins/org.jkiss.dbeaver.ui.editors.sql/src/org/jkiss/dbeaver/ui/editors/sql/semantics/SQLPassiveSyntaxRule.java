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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.jkiss.code.NotNull;
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
    
    public SQLPassiveSyntaxRule(
        @NotNull SQLBackgroundParsingJob backgroundParsingJob,
        @NotNull SQLRuleScanner sqlRuleScanner,
        @NotNull SQLTokenType tokenType
    ) {
        this.backgroundParsingJob = backgroundParsingJob;
        this.token = new SQLTokenAdapter(new LazyToken(tokenType), sqlRuleScanner);
        this.tokenType = tokenType;
    }

    @Override
    public IToken getSuccessToken() {
        return token;
    }

    @NotNull
    @Override
    public IToken evaluate(@NotNull ICharacterScanner scanner) {
        if (scanner instanceof TPCharacterScanner s) {
            int offset = s.getOffset();
            SQLDocumentSyntaxTokenEntry entry = this.backgroundParsingJob.getCurrentContext().findToken(offset);
            if (entry != null && entry.symbolEntry.getSymbolClass().getTokenType().equals(this.tokenType) && s.getOffset() < entry.end) {
//                StringBuilder sb = new StringBuilder();
//                while (s.getOffset() < entry.end) {
//                    sb.append((char)s.read());
//                }
//                System.out.println("found @" + offset + " " + entry + " = " + sb.toString());
                while (s.getOffset() < entry.end) {
                    s.read();
                }
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
        return this.evaluate(scanner);
    }
}
