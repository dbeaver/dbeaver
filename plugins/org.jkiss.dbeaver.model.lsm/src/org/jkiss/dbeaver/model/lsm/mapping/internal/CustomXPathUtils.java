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
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathNodes;
import java.util.Iterator;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CustomXPathUtils {

    public static void flattenExclusiveImpl(
        Node root,
        XPathExpression expr,
        boolean justLeaves,
        NodesList<Node> result
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
    
    public static Stream<Node> streamOf(final NodeList list) {
        return StreamSupport.stream(iterableOf(list).spliterator(), false);
    }
    
    public static Iterable<Node> iterableOf(final NodeList list) {
        return new Iterable<Node>() {
            @Override
            public Iterator<Node> iterator() {
                return new Iterator<>() {
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
        };
    }
    
    public static Iterable<Node> reverseIterableOf(final XPathNodes list) {
        return new Iterable<Node>() {
            @Override
            public Iterator<Node> iterator() {
                return new Iterator<>() {
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
        };
    }
    
    public static String getText(Tree node) {
        String result;
        if (node instanceof TreeRuleNode) {
            TreeRuleNode ruleNode = ((TreeRuleNode) node);
            Interval textRange = ruleNode.getRealInterval();
            result = ruleNode.getStart().getInputStream().getText(textRange);
        } else if (node instanceof XTreeTextBase && node instanceof TerminalNode) {
            Interval textRange = ((XTreeTextBase)node).getRealInterval();
            result = ((TerminalNode)node).getSymbol().getInputStream().getText(textRange);
        } else if (node instanceof ParseTree) {
            result = ((ParseTree) node).getText(); // consider extracting whitespaces but not comments
        } else {
            Tree first = node, last = node;
            while (!(first instanceof TerminalNode) && first.getChildCount() > 0) {
                first = first.getChild(0);
            }
            while (!(last instanceof TerminalNode) && last.getChildCount() > 0) {
                last = last.getChild(last.getChildCount() - 1);
            }
            TerminalNode a = (TerminalNode) first, b = (TerminalNode) last;
            Interval textRange = Interval.of(a.getSymbol().getStartIndex(), b.getSymbol().getStopIndex());
            result = b.getSymbol().getTokenSource().getInputStream().getText(textRange);
        }
        return result;
    }    
}
