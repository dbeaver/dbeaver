/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.parser;

import java.util.List;

import org.jkiss.dbeaver.parser.grammar.GrammarRule;

/**
 * Parsing tree
 */
public class ParseTreeNode {
    private final GrammarRule rule;
    private final int position;
    private final ParseTreeNode parent;
    private final List<ParseTreeNode> childs;
    
    public ParseTreeNode(GrammarRule rule, int position, ParseTreeNode parent, List<ParseTreeNode> childs) {
        this.rule = rule;
        this.position = position;
        this.parent = parent;
        this.childs = childs;
    }
       
    public GrammarRule getRule() {
        return rule;
    }

    public int getPosition() {
        return position;
    }

    public ParseTreeNode getParent() {
        return parent;
    }

    public List<ParseTreeNode> getChilds() {
        return childs;
    }

    public String collectString() {
        StringBuilder sb = new StringBuilder();
        this.collectStringImpl(sb, "");
        return sb.toString();
    }
    
    private void collectStringImpl(StringBuilder sb, String indent) {
        sb.append(indent).append(this.rule == null ? "<NULL>" : this.rule.getName());
        if (this.childs.size() == 0) {
            sb.append("@").append(this.position);
        }
        sb.append("\n");
        for (ParseTreeNode child: this.childs) {
            child.collectStringImpl(sb, indent + "  ");
        }
    }
}
