package org.jkiss.dbeaver.antlr.model.internal;

import java.util.Iterator;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathNodes;

import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
        if (node instanceof ParseTree) {
            result = ((ParseTree) node).getText(); // consider extracting whitespaces but not comments
        } else {
            // TODO there are things like firstTerm and lastTerm in the ANTLR tree class impl already, consider them
            Tree first = node, last = node;
            while (!(first instanceof TerminalNode) && first.getChildCount() > 0) {
                first = first.getChild(0);
            }
            while (!(last instanceof TerminalNode) && last.getChildCount() > 0) {
                last = last.getChild(last.getChildCount() - 1);
            }
            TerminalNode a = (TerminalNode) first, b = (TerminalNode) last;
            Interval textRange = Interval.of(a.getSourceInterval().a, b.getSourceInterval().b);
            result = b.getSymbol().getTokenSource().getInputStream().getText(textRange);
        }
        return result;
    }    
}
