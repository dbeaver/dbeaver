/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.lang;

import org.eclipse.jface.text.rules.IToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Source code node
 */
public class SCMRoot extends SCMGroupNode {

    private final SCMSourceParser parser;
    private final SCMSourceScanner scanner;

    public SCMRoot(SCMSourceParser parser, SCMSourceScanner scanner) {
        super(null);
        this.parser = parser;
        this.scanner = scanner;
    }

    @NotNull
    @Override
    public SCMSourceText getSource() {
        return scanner.getSource();
    }

    @Nullable
    @Override
    public IToken parseComposite(@NotNull SCMSourceScanner scanner) {

        for (; ; ) {
            IToken token = scanner.nextToken();
            if (token.isEOF()) {
                break;
            }

            parseToken(this, token);
        }

        return null;
    }

    private void parseToken(SCMCompositeNode container, IToken token) {
        SCMNode node = parser.parseNode(container, token, scanner);
        container.addChild(node);
        if (node instanceof SCMCompositeNode) {
            SCMCompositeNode composite = (SCMCompositeNode) node;
            token = composite.parseComposite(scanner);
            if (token != null) {
                parseToken(container, token);
            }
        }
    }

}
