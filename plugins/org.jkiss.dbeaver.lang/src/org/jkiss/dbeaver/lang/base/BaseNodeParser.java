/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
