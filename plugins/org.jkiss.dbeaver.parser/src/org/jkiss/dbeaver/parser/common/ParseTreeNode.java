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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.parser.common.grammar.GrammarRule;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Parsing tree
 */
public class ParseTreeNode {
    private final GrammarRule rule;
    private final String tag;
    private final int position;
    private final ParseTreeNode parent;
    private final List<ParseTreeNode> children;

    private int endPosition;
    
    public ParseTreeNode(GrammarRule rule, String tag, int position, int endPosition, ParseTreeNode parent, List<ParseTreeNode> children) {
        this.rule = rule;
        this.tag = tag;
        this.position = position;
        this.endPosition = endPosition;
        this.parent = parent;
        this.children = children;
    }
       
    public GrammarRule getRule() {
        return rule;
    }
    
    public String getTag() {
        return tag;
    }
    
    public int getPosition() {
        return position;
    }

    public int getEndPosition() {
        return endPosition;
    }
    
    void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public ParseTreeNode getParent() {
        return parent;
    }

    public List<ParseTreeNode> getChildren() {
        return children;
    }
    
    public String collectString() {
        return this.collectString(null);
    }

    public String collectString(String text) {
        StringBuilder sb = new StringBuilder();
        this.collectStringImpl(sb, text, "");
        return sb.toString();
    }

    @NotNull
    public Stream<ParseTreeNode> stream() {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(new Itr(this), Spliterator.DISTINCT | Spliterator.IMMUTABLE),
            false
        );
    }

    private void collectStringImpl(StringBuilder sb, String text, String indent) {
        sb.append(indent);
        if (this.rule == null && this.children.size() == 0) {
            if (text != null && this.position >= 0 && this.endPosition >= this.position && this.endPosition <= text.length()) {
                sb.append("'").append(text.substring(this.position, this.endPosition)).append("'");
            } else {
                sb.append("<TERM> ");
            }
        } else {
            sb.append(this.rule == null ? "<NULL>" : this.rule.getName());
        }

        if (text != null || (this.rule == null && this.children.size() == 0)) {
            sb.append(" (").append(this.position).append("-").append(this.endPosition).append(")");
        }
        sb.append("\n");
        
        for (ParseTreeNode child: this.children) {
            child.collectStringImpl(sb, text, indent + "  ");
        }
    }

    public String getContent(String text) {
        return text.substring(this.getPosition(), this.getEndPosition());
    }

    private static class Itr implements Iterator<ParseTreeNode> {
        private final Queue<ParseTreeNode> queue;

        public Itr(@NotNull ParseTreeNode root) {
            this.queue = new ArrayDeque<>();
            this.queue.add(root);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public ParseTreeNode next() {
            final ParseTreeNode node = queue.remove();
            queue.addAll(node.children);
            return node;
        }
    }
}
