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
package org.jkiss.dbeaver.model.sql.parser.tokens.predicates;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Iterator;

/**
 * Implements token predicate tree formatter responsible for collecting its string representation
 * (mainly for a debugging visualization purposes)
 */
class TokenPredicateFormatter implements TokenPredicateNodeVisitor<StringBuilder, StringBuilder>{

    private static final TokenPredicateFormatter INSTANCE = new TokenPredicateFormatter();

    @NotNull
    public static String format(@Nullable TokenPredicateNode node) {
        return node == null ? "<NULL>" : node.apply(INSTANCE, new StringBuilder()).toString();
    }

    private TokenPredicateFormatter() { }

    private StringBuilder visit(TokenPredicateNode parent, TokenPredicateNode node, StringBuilder sb) {
        boolean needsWrapping = (
                parent instanceof GroupTokenPredicatesNode &&
                node instanceof GroupTokenPredicatesNode &&
                parent.getClass() != node.getClass()
        ) || (
                parent instanceof UnaryTokenPredicateNode &&
                node instanceof GroupTokenPredicatesNode
        );

        if (needsWrapping) {
            sb.append("(");
        }

        node.apply(this, sb);

        if (needsWrapping) {
            sb.append(")");
        }

        return sb;
    }

    private StringBuilder visitUnary(UnaryTokenPredicateNode unary, StringBuilder sb) {
        return this.visit(unary, unary.child, sb);
    }

    private StringBuilder visitGroup(GroupTokenPredicatesNode group, StringBuilder sb, String separator) {
        Iterator<TokenPredicateNode> it = group.childs.iterator();
        if (it.hasNext()) {
            this.visit(group, it.next(), sb);
            while (it.hasNext()) {
                sb.append(separator);
                this.visit(group, it.next(), sb);
            }
        }
        return sb;
    }

    @Override
    public StringBuilder visitSequence(SequenceTokenPredicateNode sequence, StringBuilder sb) {
        return this.visitGroup(sequence, sb, " ");
    }

    @Override
    public StringBuilder visitAlternative(AlternativeTokenPredicateNode alternative, StringBuilder sb) {
        return this.visitGroup(alternative, sb, "|");
    }

    @Override
    public StringBuilder visitOptional(OptionalTokenPredicateNode optional, StringBuilder sb) {
        return this.visitUnary(optional, sb).append("?");
    }

    @Override
    public StringBuilder visitTokenEntry(SQLTokenEntry token, StringBuilder sb) {
        return token.format(sb);
    }
}
