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

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.SyntaxTree;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.w3c.dom.*;

import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

public interface XTreeNodeBase extends SyntaxTree, Node {

    /*
     * Interval in terms of real source text positions
     */
    Interval getRealInterval();
    
    int getIndex();

    void fixup(Parser parser, int index);
    
    TreeRuleNode.SubnodesList getSubnodes();
    
    default AbstractSyntaxNode getModel() {
        throw new UnsupportedOperationException();
    }
    
    default void setModel(AbstractSyntaxNode model) {
        throw new UnsupportedOperationException();
    }

    Map<String, Object> getUserDataMap(boolean createIfMissing);
    
    default XTreeNodeBase getParentXNode() {
        return (XTreeNodeBase) this.getParent();
    }
    
    
    @Override
    default String getNodeValue() throws DOMException {
        return CustomXPathUtils.getText(this);
    }

    @Override
    default void setNodeValue(String nodeValue) throws DOMException {
        throw new UnsupportedOperationException();
    }
    

    @Override
    default Node getParentNode() {
        return (Node) this.getParent();
    }
    
    @Override
    default NodeList getChildNodes() {
        return this.getSubnodes();
    }

    @Override
    default Node getFirstChild() {
        return this.getSubnodes().getFirst();
    }

    @Override
    default Node getLastChild() {
        return this.getSubnodes().getLast();
    }

    @Override
    default Node getPreviousSibling() {
        return this.getParentXNode() == null ? null : this.getParentXNode().getSubnodes().item(this.getIndex() - 1);
    }

    @Override
    default Node getNextSibling() {
        return this.getParentXNode() == null ? null : this.getParentXNode().getSubnodes().item(this.getIndex() + 1);
    }

    @Override
    default NamedNodeMap getAttributes() {
        return EmptyAttrsMap.INSTANCE;
    }

    @Override
    default Document getOwnerDocument() {
        return null;
    }

    @Override
    default Node insertBefore(Node newChild, Node refChild) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default Node removeChild(Node oldChild) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default Node appendChild(Node newChild) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean hasChildNodes() {
        return this.getSubnodes().getLength() > 0;
    }

    @Override
    default Node cloneNode(boolean deep) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void normalize() {
        // do nothing
    }

    @Override
    default boolean isSupported(String feature, String version) {
        return false;
    }

    @Override
    default String getNamespaceURI() {
        return null;
    }

    @Override
    default String getPrefix() {
        return null;
    }

    @Override
    default void setPrefix(String prefix) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default String getLocalName() {
        return this.getNodeName();
    }

    @Override
    default boolean hasAttributes() {
        return false;
    }

    @Override
    default String getBaseURI() {
        return null;
    }

    @Override
    default short compareDocumentPosition(Node other) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default String getTextContent() throws DOMException {
        return this.getNodeValue();
    }

    @Override
    default void setTextContent(String textContent) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isSameNode(Node other) {
        return this.equals(other);
    }

    @Override
    default String lookupPrefix(String namespaceURI) {
        return null;
    }

    @Override
    default boolean isDefaultNamespace(String namespaceURI) {
        return false;
    }

    @Override
    default String lookupNamespaceURI(String prefix) {
        return null;
    }

    @Override
    default boolean isEqualNode(Node arg) {
        return this.equals(arg);
    }

    @Override
    default Object getFeature(String feature, String version) {
        return null;
    }

    @Override
    default Object setUserData(String key, Object data, UserDataHandler handler) {
        Map<String, Object> userData = this.getUserDataMap(true);
        return userData.replace(key, data);
    }

    @Override
    default Object getUserData(String key) {
        Map<String, Object> userData = this.getUserDataMap(false);
        return userData == null ? null : userData.get(key);
    }

    default XTreeNodeBase findFirstDescedantByName(String ruleName) {
        Stack<XTreeNodeBase> stack = new Stack<>();
        stack.addAll(getSubnodes().getCollection());
        
        while (!stack.isEmpty()) {
            XTreeNodeBase nodeInfo = stack.pop();
        
            if (nodeInfo.getLocalName().equals(ruleName)) {
                return nodeInfo;
            } else {
                TreeRuleNode.SubnodesList subnodes = nodeInfo.getSubnodes();
                for (int i = subnodes.getLength() - 1; i >= 0; i--) {
                    stack.push(subnodes.item(i));
                }
            }
        }
        
        return null;
    }

    default NodesList<XTreeNodeBase> findDescedantLayerByName(String ruleName) {
        NodesList<XTreeNodeBase> result = new NodesList<>(5);
        Stack<XTreeNodeBase> stack = new Stack<>();
        stack.addAll(getSubnodes().getCollection());
        
        while (!stack.isEmpty()) {
            XTreeNodeBase nodeInfo = stack.pop();
        
            if (nodeInfo.getLocalName().equals(ruleName)) {
                result.add(nodeInfo);
            } else {
                TreeRuleNode.SubnodesList subnodes = nodeInfo.getSubnodes();
                for (int i = subnodes.getLength() - 1; i >= 0; i--) {
                    stack.push(subnodes.item(i));
                }
            }
        }
        
        return result;
    }

    default String getFullPathName() {
        LinkedList<String> names = new LinkedList<>();
        for (XTreeNodeBase node = this; node != null; node = node.getParentXNode()) {
            names.addFirst(node.getNodeName());
        }
        return String.join(".", names);
    }   
}
