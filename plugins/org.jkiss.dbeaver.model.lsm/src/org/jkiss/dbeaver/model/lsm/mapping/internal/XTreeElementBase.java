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

import org.w3c.dom.*;

import java.util.Stack;
import java.util.function.Predicate;

public interface XTreeElementBase extends XTreeNodeBase, Element {
    
    @Override
    default String getTagName() {
        return this.getLocalName();
    }

    @Override
    default String getAttribute(String name) {
        return "";
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
        NodesList<XTreeNodeBase> result = new NodesList<>(5);
        Stack<XTreeNodeBase> stack = new Stack<>();
        stack.addAll(getSubnodes().getCollection());
        
        while (!stack.isEmpty()) {
            XTreeNodeBase nodeInfo = stack.pop();
        
            if (nodeInfo != null) {
                stack.push(nodeInfo);
                stack.push(null);
                TreeRuleNode.SubnodesList subnodes = nodeInfo.getSubnodes();
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
        return "";
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
            return EmptyNodesList.INSTANCE;
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
