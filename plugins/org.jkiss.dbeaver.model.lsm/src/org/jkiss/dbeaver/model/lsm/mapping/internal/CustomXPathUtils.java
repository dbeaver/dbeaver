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
package org.jkiss.dbeaver.model.lsm.mapping.internal;


import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathNodes;

public class CustomXPathUtils {

    public static void flattenExclusiveImpl(
        @NotNull Node root,
        @NotNull XPathExpression expr,
        boolean justLeaves,
        @NotNull NodesList<Node> result
    ) throws XPathExpressionException {
        Stack<Node> stack = new Stack<>();
        stack.add(root);
        while (stack.size() > 0) {
            Node node = stack.pop();
            
            XPathNodes subnodes = expr.evaluateExpression(node, XPathNodes.class);
            if (justLeaves) {
                if (subnodes.size() > 0) {
                    reverseIterableOf(subnodes).forEach(stack::add);
                } else {
                    result.add(node);                    
                }
            } else {
                result.ensureCapacity(result.size() + subnodes.size());
                subnodes.forEach(result::add);
                reverseIterableOf(subnodes).forEach(stack::add);
            }
        }
    }

    @NotNull
    public static Stream<Node> streamOf(@NotNull final NodeList list) {
        return StreamSupport.stream(iterableOf(list).spliterator(), false);
    }

    @NotNull
    public static Iterable<Node> iterableOf(@NotNull final NodeList list) {
        return () -> new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < list.getLength();
            }

            @Override
            public Node next() {
                return list.item(index++);
            }
        };
    }

    @NotNull
    public static Iterable<Node> reverseIterableOf(@NotNull final XPathNodes list) {
        return () -> new Iterator<>() {
            private int index = list.size();

            @Override
            public boolean hasNext() {
                return index > 0;
            }

            @Override
            public Node next() {
                try {
                    return list.get(--index);
                } catch (XPathException e) {
                    return null;
                }
            }
        };
    }

    @Nullable
    public static String getText(@NotNull Tree node) {
        String result = null;
        if (node instanceof TreeRuleNode) {
            TreeRuleNode ruleNode = ((TreeRuleNode) node);
            Interval textRange = ruleNode.getRealInterval();
            result = ruleNode.getStart().getInputStream().getText(textRange);
        } else if (node instanceof XTreeTextBase) {
            Interval textRange = ((XTreeTextBase) node).getRealInterval();
            result = ((TerminalNode) node).getSymbol().getInputStream().getText(textRange);
        } else if (node instanceof ParseTree) {
            result = ((ParseTree) node).getText(); // consider extracting whitespaces but not comments
        } else {
            Tree first = node;
            Tree last = node;
            while (!(first instanceof TerminalNode) && first.getChildCount() > 0) {
                first = first.getChild(0);
            }
            while (!(last instanceof TerminalNode) && last.getChildCount() > 0) {
                last = last.getChild(last.getChildCount() - 1);
            }
            if (first instanceof TerminalNode && last instanceof TerminalNode) {
                TerminalNode a = (TerminalNode) first;
                TerminalNode b = (TerminalNode) last;
                Interval textRange = Interval.of(a.getSymbol().getStartIndex(), b.getSymbol().getStopIndex());
                result = b.getSymbol().getTokenSource().getInputStream().getText(textRange);
            }
        }
        return result;
    }    
}
