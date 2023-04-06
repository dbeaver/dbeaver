package org.jkiss.dbeaver.antlr.model.internal;

import java.util.Stack;
import java.util.function.Predicate;

import org.jkiss.dbeaver.antlr.model.internal.TreeRuleNode.SubnodesList;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

public interface CustomXPathModelElementBase extends CustomXPathModelNodeBase, Element {
    
    @Override
    default String getTagName() {
        return this.getLocalName();
    }

    @Override
    default String getAttribute(String name) {
        return null;
    }

    @Override
    default void setAttribute(String name, String value) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default void removeAttribute(String name) throws DOMException {
        throw new UnsupportedOperationException();    
    }

    @Override
    default Attr getAttributeNode(String name) {
        return null;
    }

    @Override
    default Attr setAttributeNode(Attr newAttr) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default NodeList getElementsByTagName(String name) {
        Predicate<Node> condition = "*".equals(name) ? n -> true : n -> n.getLocalName().equals(name);
        var result = new NodesList<CustomXPathModelNodeBase>(5);
        var stack = new Stack<CustomXPathModelNodeBase>();
        stack.addAll(getSubnodes().getCollection());
        
        while (!stack.isEmpty()) {
            CustomXPathModelNodeBase nodeInfo = stack.pop();
        
            if (nodeInfo != null) {
                stack.push(nodeInfo);
                stack.push(null);
                SubnodesList subnodes = nodeInfo.getSubnodes();
                for (int i = subnodes.getLength() - 1; i >= 0; i--) {
                    stack.push(subnodes.item(i));
                }
            } else {
                nodeInfo = stack.pop();
                if (condition.test(nodeInfo)) {
                    result.add(nodeInfo);
                }
            }
        }
        
        return result;
    }

    @Override
    default String getAttributeNS(String namespaceURI, String localName) throws DOMException {
        return null;
    }

    @Override
    default void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
        return null;
    }

    @Override
    default Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
        if (namespaceURI == null || namespaceURI.length() == 0) {
            return this.getElementsByTagName(localName);
        } else {
            return null;
        }
    }

    @Override
    default boolean hasAttribute(String name) {
        return false;
    }

    @Override
    default boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
        return false;
    }

    @Override
    default TypeInfo getSchemaTypeInfo() {
        return null;
    }

    @Override
    default void setIdAttribute(String name, boolean isId) throws DOMException {
        throw new UnsupportedOperationException();            
    }

    @Override
    default void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
        throw new UnsupportedOperationException();
    }
}
