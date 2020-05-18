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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.lang.SCMCompositeNode;
import org.jkiss.dbeaver.lang.SCMLeafNode;
import org.jkiss.dbeaver.lang.SCMSourceScanner;

/**
 * Leaf node - linked to particular source code segment
 */
public class SCMEString extends SCMLeafNode {

    public SCMEString(@NotNull SCMCompositeNode parent, @NotNull SCMSourceScanner scanner) {
        super(parent, scanner);
        if (endOffset - beginOffset < 2) {
            throw new IllegalArgumentException("String node length must be >= 2 (" + (endOffset - beginOffset) + " specified");
        }
    }

    public String getStringValue() {
        return parent.getSource().getSegment(beginOffset + 1, endOffset - 1);
    }

    public char getOpenQuote() {
        return parent.getSource().getChar(beginOffset);
    }

    public char getCloseQuote() {
        return parent.getSource().getChar(endOffset);
    }

}
