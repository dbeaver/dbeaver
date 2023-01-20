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
package org.jkiss.dbeaver.parser.common;

import org.jkiss.dbeaver.parser.common.grammar.GrammarRule;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Parsing context
 */
class ParserStack {
    private final ParserStack prev;

    private final int exprId;
    private final int exprPosition;
    private final GrammarRule rule;

    private ParserStack(ParserStack prev, int exprId, int exprPosition, GrammarRule rule) {
        this.prev = prev;
        this.exprId = exprId;
        this.exprPosition = exprPosition;
        this.rule = rule;
    }

    public int getExprId() {
        return exprId;
    }

    public int getExprPosition() {
        return exprPosition;
    }

    public GrammarRule getRule() {
        return rule;
    }

    public static ParserStack initial() {
        return new ParserStack(null, -1, 0, null);
    }

    public ParserStack push(int exprId, int exprPosition, GrammarRule rule) {
        return new ParserStack(this, exprId, exprPosition, rule);
    }

    public ParserStack pop() {
        return this.prev;
    }

    public String collectDescription() {
        ArrayList<String> items = new ArrayList<>();
        String prevName = "";
        for (ParserStack item = this; item != null; item = item.prev) {
            if (item.getRule() != null) {
                String name = item.getRule().getName();
                if (!name.equals(prevName)) {
                    prevName = name;
                    items.add(name);
                }
            }
        }
        Collections.reverse(items);
        return String.join("/", items);
    }
}

