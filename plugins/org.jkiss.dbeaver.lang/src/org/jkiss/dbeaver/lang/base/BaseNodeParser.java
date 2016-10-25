/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.lang.base;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.lang.*;
import org.jkiss.dbeaver.lang.parser.BaseSourceScanner;
import org.jkiss.dbeaver.lang.parser.LiteralRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Source code node
 */
public class BaseNodeParser implements SCMSourceParser {

    public static final IToken NUMBER_TOKEN = new Token(SCMToken.NUMBER);
    public static final IToken STRING_TOKEN = new Token(SCMToken.STRING);
    public static final IToken COMMENT_TOKEN = new Token(SCMToken.COMMENT);

    @Override
    public SCMSourceScanner createScanner(Document document) {
        List<IRule> rules = new ArrayList<>();
        addRules(rules);
        return new BaseSourceScanner(document, rules);
    }

    protected void addRules(List<IRule> rules) {
        rules.add(new WhitespaceRule(new IWhitespaceDetector() {
                @Override
                public boolean isWhitespace(char c) {
                    return Character.isWhitespace(c);
                }
            }));
        rules.add(new MultiLineRule("\"", "\"", STRING_TOKEN, (char)0));
        rules.add(new MultiLineRule("'", "'", STRING_TOKEN, (char)0));

        rules.add(new MultiLineRule("/*", "*/", COMMENT_TOKEN, (char) 0, true));
        rules.add(new EndOfLineRule("--", COMMENT_TOKEN));

        rules.add(new NumberRule(NUMBER_TOKEN));
        rules.add(new LiteralRule());
    }

    @NotNull
    @Override
    public SCMNode parseNode(@NotNull SCMCompositeNode container, @NotNull IToken token, @NotNull SCMSourceScanner scanner) {
        if (token.isWhitespace()) {
            return new SCMEWhitespace(container, scanner);
        }
        Object data = token.getData();
        if (data instanceof SCMToken) {
            switch ((SCMToken) data) {
                case NUMBER:
                    return new SCMENumber(container, scanner);
                case STRING:
                    return new SCMEString(container, scanner);
                case WHITESPACE:
                    return new SCMENumber(container, scanner);
                case LITERAL:
                    return new SCMELiteral(container, scanner);
            }
        }
        return new SCMEUnknown(container, scanner);
    }

    protected void pushError(String message, @NotNull SCMSourceScanner scanner) {

    }
}
