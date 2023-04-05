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
package org.jkiss.dbeaver.antlr.model;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.*;
import org.w3c.dom.*;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import javax.xml.xpath.XPathEvaluationResult.XPathResultType;


public class SyntaxModel {
    private enum FieldTypeKind {
        String(true), 
        Byte(true),
        Short(true), 
        Int(true),
        Long(true),
        Bool(true),
        Float(true),
        Double(true),
        Enum(true),
        Object(false),
        Array(false),
        List(false);
        
        public final boolean isTerm;
        
        FieldTypeKind(boolean isTerm) {
            this.isTerm = isTerm;
        }
    }
    
    private class LiteralTypeInfo {
        public final String ruleName;
        public final Class<?> type;
        public final XPathExpression stringExpr;
        public final Map<Object, XPathExpression> exprByValue;
        public final Map<String, Object> valuesByName;
        public final boolean isCaseSensitive;
        
        public LiteralTypeInfo(String ruleName, Class<?> type, XPathExpression stringExpr, Map<Object, XPathExpression> exprByValue, Map<String, Object> valuesByName, boolean isCaseSensitive) {
            this.ruleName = ruleName;
            this.type = type;
            this.stringExpr = stringExpr;
            this.exprByValue = exprByValue;
            this.valuesByName = valuesByName;
            this.isCaseSensitive = isCaseSensitive;
        }        
    }
    
    
    private class NodeFieldSubnodeInfo {
        public final XPathExpression scopeExpr;
        public final Class<? extends AbstractSyntaxNode> subnodeType;
        public final SyntaxSubnodeLookupMode lookupMode;
        
        public NodeFieldSubnodeInfo(XPathExpression scopeExpr, Class<? extends AbstractSyntaxNode> subnodeType, SyntaxSubnodeLookupMode lookupMode) {
            this.scopeExpr = scopeExpr;
            this.subnodeType = subnodeType;
            this.lookupMode = lookupMode;
        }
    }
    
    private class NodeFieldInfo {
        private final FieldTypeKind kind;
        private final Field info;
        private final List<XPathExpression> termExprs;
        private final List<NodeFieldSubnodeInfo> subnodesInfo;
        
        public NodeFieldInfo(FieldTypeKind kind, Field info, List<XPathExpression> termExprs, List<NodeFieldSubnodeInfo> subnodesInfo) {
            this.kind = kind;
            this.info = info;
            this.termExprs = termExprs;
            this.subnodesInfo = subnodesInfo;
        }
        
        public FieldTypeKind getKind() {
            return this.kind;
        }

        public String getName() {
            return this.info.getName();
        }

        public void bindValue(NodeInfo nodeInfo, List<Node> subnodes) {
            try {
                this.bindValueImpl(nodeInfo, new XPathEvaluationResult<XPathNodes>() {
                    @Override
                    public XPathResultType type() {
                        return XPathResultType.NODESET;
                    }

                    @Override
                    public XPathNodes value() {
                        return new XPathNodes() {
                            @Override
                            public int size() {
                                return subnodes.size();
                            }

                            @Override
                            public Iterator<Node> iterator() {
                                return subnodes.iterator();
                            }

                            @Override
                            public Node get(int index) throws XPathException {
                                return subnodes.get(index);
                            }
                        };
                    }
                });
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException("Failed to bind " + this.info.getName(), ex);
            }            
        }

        public void bindValue(NodeInfo nodeInfo, NodeInfo subnode) {
            try {
                this.bindValueImpl(nodeInfo, new XPathEvaluationResult<Node>() {
                    @Override
                    public XPathResultType type() {
                        return XPathResultType.NODE;
                    }

                    @Override
                    public Node value() {
                        return subnode;
                    }
                });
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException("Failed to bind " + this.info.getName(), ex);
            }            
        }
        
        public void bindValue(NodeInfo nodeInfo, XPathEvaluationResult<?> xvalue) {
            try {
                this.bindValueImpl(nodeInfo, xvalue);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException("Failed to bind " + this.info.getName(), ex);
            }            
        }

        public Object getValue(AbstractSyntaxNode model) {
            try {
                return this.info.get(model);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException("Failed to bind " + this.info.getName(), ex);
            }
        }
        
        private String getScalarString(XPathEvaluationResult<?> xvalue) {
            try {
                switch (xvalue.type()) {
                    case NODE:
                        @SuppressWarnings("unchecked") 
                        NodeInfo nodeInfo = (NodeInfo) xvalue.value();
                        return nodeInfo.getNodeValue();
                    case NODESET:
                        XPathNodes nodes = (XPathNodes) xvalue.value();
                        int count = nodes.size();
                        if (count == 0) {
                            return null;
                        } else if (count == 1) {
                            return nodes.get(0).getNodeValue().toString();
                        } else {
                            // TODO capture ambiguous binding error
                            return nodes.get(0).getNodeValue().toString();
                        }
                    default:
                        if (xvalue.value() != null) {
                            return xvalue.value().toString();
                        } else {
                            return null;
                        }
                }
            } catch (XPathException ex) {
                ex.printStackTrace(); // TODO capture error
                return null;
            }
        }
        
        private void bindValueImpl(NodeInfo nodeInfo, XPathEvaluationResult<?> xvalue) throws IllegalArgumentException, IllegalAccessException {
            Object value;
            switch (this.kind) {
                case Object:
                case Array:
                case List:
    //                if (subnode.model != null) {
    //                    value = subnode.model;
    //                    break;
    //                }
                    switch (xvalue.type()) {
                        case NODE:
                        case NODESET:
                            value = xvalue.value();
                            break;
                        default:
                            throw new RuntimeException("Not supported");
                    }
                    break;
                case String: value = getScalarString(xvalue); break;
                case Byte: value = Byte.parseByte(getScalarString(xvalue)); break;
                case Short: value = Short.parseShort(getScalarString(xvalue)); break;
                case Int: value = Integer.parseInt(getScalarString(xvalue)); break;
                case Long: value = Long.parseLong(getScalarString(xvalue)); break;
                case Bool: value = Boolean.parseBoolean(getScalarString(xvalue)); break;
                case Float: value = Float.parseFloat(getScalarString(xvalue)); break;
                case Double: value = Double.parseDouble(getScalarString(xvalue)); break;
                case Enum:
                    switch (xvalue.type()) {
                        case NODE: 
                            value = mapLiteralValue((NodeInfo) xvalue.value(), this.info.getType());
                            break;
                        case NODESET:
                            XPathNodes nodes = (XPathNodes) xvalue.value();
                            if (nodes.size() == 1) {
                                try {
                                    value = mapLiteralValue((NodeInfo) nodes.get(0), this.info.getType());
                                } catch (XPathException e) {
                                    throw new RuntimeException(e); // TODO collect errors
                                }
                            } else {                                
                                value = null;
                            }
                            break;
                        default:
                            throw new RuntimeException("Not supported");
                    }
                    break;
                default: throw new RuntimeException("Unexpected syntax model field kind " + this.kind);
            }
            
            switch (this.kind) {
                case Object: {
                    if (value instanceof NodeInfo) {
                        @SuppressWarnings("unchecked")
                        NodeInfo subnodeInfo = (NodeInfo) value;
                        if (subnodeInfo.model != null) {
                            this.info.set(nodeInfo.model, subnodeInfo.model);
                        }
                    } else {
                        this.info.set(nodeInfo.model, value);
                    }
                    break;
                }
                case Array: { // TODO
    //                int index;
    //                Object newArr, oldArr = this.info.get(nodeInfo.model);
    //                Class<?> itemType = this.info.getType().getComponentType();
    //                if (value != null) {
    //                    index = Array.getLength(oldArr);
    //                    newArr = Array.newInstance(itemType, index + 1);
    //                    System.arraycopy(oldArr, 0, newArr, 0, index);
    //                } else {
    //                    index = 0;
    //                    newArr = Array.newInstance(itemType, 1);
    //                }
    //                Array.set(newArr, index, value);
    //                this.info.set(nodeInfo.model, newArr);
    //                break;
                    throw new UnsupportedOperationException("TODO");
                }
                case List: {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) this.info.get(nodeInfo.model);
                    if (list == null) {
                        this.info.set(nodeInfo.model, list = new ArrayList<>());
                    } else {
                        list.clear();
                    }
                    if (value instanceof XPathNodes) {
                        XPathNodes nodes = (XPathNodes) value;
                        if (list instanceof ArrayList<?>) {
                            ((ArrayList<?>) list).ensureCapacity(nodes.size());
                        }
                        for (var xnode: nodes) {
                            if (xnode instanceof NodeInfo) {
                                @SuppressWarnings("unchecked")
                                NodeInfo subnodeInfo = (NodeInfo) xnode;
                                if (subnodeInfo.model != null) {
                                    list.add(subnodeInfo.model);
                                }
                            }
                        }
                    } else if (value instanceof NodeInfo) {
                        @SuppressWarnings("unchecked")
                        NodeInfo subnodeInfo = (NodeInfo) value;
                        if (subnodeInfo.model != null) {
                            list.add(subnodeInfo.model);
                        }
                    } else {
                        list.add(value);
                    }
                    break;
                }
                default:
                    this.info.set(nodeInfo.model, value);
                    break;
            }
        }
    }
    
    private class NodeTypeInfo {
        private final String ruleName;
        private final Class<? extends AbstractSyntaxNode> type;
        private final Constructor<? extends AbstractSyntaxNode> ctor;
        private final Map<String, NodeFieldInfo> fields;

        public NodeTypeInfo(String ruleName, Class<? extends AbstractSyntaxNode> type, Constructor<? extends AbstractSyntaxNode> ctor, Map<String, NodeFieldInfo> fields) {
            this.ruleName = ruleName;
            this.type = type;
            this.ctor = ctor;
            this.fields = Collections.unmodifiableMap(fields);
        }
        
        public Collection<NodeFieldInfo> getFields() {
            return this.fields.values();
        }

        public Class<? extends AbstractSyntaxNode> getType() {
            return this.type;
        }
        
        public AbstractSyntaxNode instantiateAndFill(NodeInfo nodeInfo) {
            try {
                if (nodeInfo.model == null) {
                    nodeInfo.model = this.ctor.newInstance();
                }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new RuntimeException("Failed to instantiate " + this.type.getName(), ex);
            }
            
            Set<NodeInfo> subnodes = new HashSet<NodeInfo>(5);
            for (var field : this.fields.values()) {
                subnodes.clear();
                for (var expr : field.termExprs) {
                    try {
                        XPathEvaluationResult<?> value = expr.evaluateExpression(nodeInfo);
                        field.bindValue(nodeInfo, value);
                    } catch (XPathExpressionException e) {
                        // TODO collect error info
                        e.printStackTrace();
                    }
                }
                if (field.kind == FieldTypeKind.Array || field.kind == FieldTypeKind.List) {
                    for (var subnodeInfo : field.subnodesInfo) {
                        boolean tryDescedants = subnodeInfo.lookupMode == SyntaxSubnodeLookupMode.DEPTH_FIRST;
                        try {
                            if (subnodeInfo.scopeExpr != null) {
                                XPathEvaluationResult<?> scopeOrSubnode = subnodeInfo.scopeExpr.evaluateExpression(nodeInfo);
                                if (scopeOrSubnode.type() == XPathResultType.NODESET && scopeOrSubnode.value() instanceof XPathNodes) {
                                    for (var scopeSubnode : (XPathNodes) scopeOrSubnode.value()) {
                                        if (scopeSubnode instanceof NodeInfo) {
                                            mapSubtrees((NodeInfo) scopeSubnode, subnodeInfo.subnodeType, true, tryDescedants, subnodes);
                                        }
                                    }
                                } else if (scopeOrSubnode.type() == XPathResultType.NODE && scopeOrSubnode.value() instanceof NodeInfo) {
                                    mapSubtrees((NodeInfo) scopeOrSubnode.value(), subnodeInfo.subnodeType, true, tryDescedants, subnodes);
                                }
                            } else {
                                if (tryDescedants) {
                                    mapSubtrees(nodeInfo, subnodeInfo.subnodeType, false, true, subnodes);
                                } else {
                                    for (var candidateSubnode : nodeInfo.getSubnodes()) {
                                        mapSubtrees(candidateSubnode, subnodeInfo.subnodeType, true, false, subnodes);
                                    }
                                }
                            }
                        } catch (XPathExpressionException e) {
                            // TODO collect error info
                            e.printStackTrace();
                        }
                    }
                    List<Node> orderedSubnodes = subnodes.stream()
                        .sorted(Comparator.comparingInt(a -> a.model.getStartPosition()))
                        .collect(Collectors.toList());
                    field.bindValue(nodeInfo, orderedSubnodes);
                } else {
                    for (var subnodeInfo : field.subnodesInfo) {
                        boolean tryDescedants = subnodeInfo.lookupMode == SyntaxSubnodeLookupMode.DEPTH_FIRST;
                        try {
                            NodeInfo subnode = null;
                            if (subnodeInfo.scopeExpr != null) {
                                XPathEvaluationResult<?> scopeOrSubnode = subnodeInfo.scopeExpr.evaluateExpression(nodeInfo);
                                if (scopeOrSubnode.type() == XPathResultType.NODESET && scopeOrSubnode.value() instanceof XPathNodes) {
                                    for (var scopeSubnode : (XPathNodes) scopeOrSubnode.value()) {
                                        if (scopeSubnode instanceof NodeInfo) {
                                            subnode = mapSubtree((NodeInfo) scopeSubnode, subnodeInfo.subnodeType, true, tryDescedants);
                                            if (subnode != null) {
                                                break;
                                            }
                                        }
                                    }
                                } else if (scopeOrSubnode.type() == XPathResultType.NODE && scopeOrSubnode.value() instanceof NodeInfo) {
                                    subnode = mapSubtree((NodeInfo) scopeOrSubnode.value(), subnodeInfo.subnodeType, true, tryDescedants);
                                }
                            } else {
                                if (tryDescedants) {
                                    subnode = mapSubtree(nodeInfo, subnodeInfo.subnodeType, false, true);
                                } else {
                                    for (var candidateSubnode : nodeInfo.getSubnodes()) {
                                        mapSubtrees(candidateSubnode, subnodeInfo.subnodeType, true, false, subnodes);
                                    }
                                }
                            }
                            if (subnode != null) {
                                field.bindValue(nodeInfo, subnode);
                                break;
                            }
                        } catch (XPathExpressionException e) {
                            // TODO collect error info
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            if (nodeInfo.node instanceof SyntaxTree) {
                SyntaxTree snode = (SyntaxTree) nodeInfo.node;
                nodeInfo.model.setStartPosition(snode.getSourceInterval().a);
                nodeInfo.model.setEndPosition(snode.getSourceInterval().b);
            }
            return nodeInfo.model;
        }        
    }
    
    private static class NodesList<T extends Node> extends ArrayList<T> implements NodeList {
        public NodesList() {
            super();
        }
        
        public NodesList(int capacity) {
            super(capacity);
        }

        @Override
        public T item(int index) {
            return index < 0 || index >= this.size() ? null : this.get(index);
        }

        @Override
        public int getLength() {
            return this.size();
        }

        public T getFirst() {
            return this.size() > 0 ? this.get(0) : null;
        }

        public T getLast() {
            return this.size() > 0 ? this.get(this.size() - 1) : null;
        }
    }
    
    private static class SubnodesList extends NodesList<NodeInfo> {
        
        public SubnodesList() {
            super();
        }
        
        public SubnodesList(int capacity) {
            super(capacity);
        }
    }
    
    private class NodeInfo implements Element {
        private final NodeInfo parent;
        private final Tree node;
        private final int index;
        private SubnodesList subnodes;
        private AbstractSyntaxNode model;
        private Map<String, Object> userData;
            
        public NodeInfo(NodeInfo parent, Tree node, int index) {
            this.parent = parent;
            this.node = node;
            this.index = index;
            this.subnodes = null;
            this.model = null;
            this.userData = null;
        }
        
        public SubnodesList getSubnodes() {
            if (this.subnodes == null) {
                int count = node.getChildCount();
                var subnodes = new SubnodesList(count);
                for (int i = 0; i < count; i++) {
                    subnodes.add(new NodeInfo(this, node.getChild(i), i));
                }
                this.subnodes = subnodes;
            }
            return this.subnodes;
        }

        @Override
        public String getNodeName() {
            if (node.getChildCount() > 0) {
                return Trees.getNodeText(node, parser);
            } else {
                return "#text";
            }
        }

        @Override
        public String getNodeValue() throws DOMException {
            return getText(node);
        }

        @Override
        public void setNodeValue(String nodeValue) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public short getNodeType() {
            if (node.getChildCount() > 0) {
                return Node.ELEMENT_NODE;
            } else {
                return Node.TEXT_NODE;
            }
        }

        @Override
        public Node getParentNode() {
            return parent;
        }
        
        @Override
        public NodeList getChildNodes() {
            return this.getSubnodes();
        }

        @Override
        public Node getFirstChild() {
            return this.getSubnodes().getFirst();
        }

        @Override
        public Node getLastChild() {
            return this.getSubnodes().getLast();
        }

        @Override
        public Node getPreviousSibling() {
            return this.parent == null ? null : this.parent.getSubnodes().item(this.index - 1);
        }

        @Override
        public Node getNextSibling() {
            return this.parent == null ? null : this.parent.getSubnodes().item(this.index + 1);
        }

        @Override
        public NamedNodeMap getAttributes() {
            return EmptyAttrsMap.INSTANCE;
        }

        @Override
        public Document getOwnerDocument() {
            return null;
        }

        @Override
        public Node insertBefore(Node newChild, Node refChild) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Node removeChild(Node oldChild) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Node appendChild(Node newChild) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasChildNodes() {
            return this.getSubnodes().size() > 0;
        }

        @Override
        public Node cloneNode(boolean deep) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void normalize() {
            // do nothing
        }

        @Override
        public boolean isSupported(String feature, String version) {
            return false;
        }

        @Override
        public String getNamespaceURI() {
            return null;
        }

        @Override
        public String getPrefix() {
            return null;
        }

        @Override
        public void setPrefix(String prefix) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getLocalName() {
            return this.getNodeName();
        }

        @Override
        public boolean hasAttributes() {
            return false;
        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public short compareDocumentPosition(Node other) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getTextContent() throws DOMException {
            return this.getNodeValue();
        }

        @Override
        public void setTextContent(String textContent) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSameNode(Node other) {
            return this.equals(other);
        }

        @Override
        public String lookupPrefix(String namespaceURI) {
            return null;
        }

        @Override
        public boolean isDefaultNamespace(String namespaceURI) {
            return false;
        }

        @Override
        public String lookupNamespaceURI(String prefix) {
            return null;
        }

        @Override
        public boolean isEqualNode(Node arg) {
            return this.equals(arg);
        }

        @Override
        public Object getFeature(String feature, String version) {
            return null;
        }

        @Override
        public Object setUserData(String key, Object data, UserDataHandler handler) {
            if (this.userData == null) {
                this.userData = new HashMap<>();
            }
            return this.userData.replace(key, data);
        }

        @Override
        public Object getUserData(String key) {
            return this.userData == null ? null : this.userData.get(key);
        }

        @Override
        public String getTagName() {
            return this.getLocalName();
        }

        @Override
        public String getAttribute(String name) {
            return null;
        }

        @Override
        public void setAttribute(String name, String value) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(String name) throws DOMException {
            throw new UnsupportedOperationException();    
        }

        @Override
        public Attr getAttributeNode(String name) {
            return null;
        }

        @Override
        public Attr setAttributeNode(Attr newAttr) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
            throw new UnsupportedOperationException();
        }

        public NodeInfo findFirstDescedantByName(String ruleName) {
            var stack = new Stack<NodeInfo>();
            this.getSubnodes().forEach(n -> stack.push((NodeInfo)n));
            
            while (!stack.isEmpty()) {
                NodeInfo nodeInfo = stack.pop();
            
                if (nodeInfo.getLocalName().equals(ruleName)) {
                    return nodeInfo;
                } else {
                    SubnodesList subnodes = nodeInfo.getSubnodes();
                    for (int i = subnodes.size() - 1; i >= 0; i--) {
                        stack.push((NodeInfo) subnodes.get(i));
                    }
                }
            }
            
            return null;
        }
        
        public SubnodesList findDescedantLayerByName(String ruleName) {
            var result = new SubnodesList(5);
            var stack = new Stack<NodeInfo>();
            this.getSubnodes().forEach(n -> stack.push((NodeInfo)n));
            
            while (!stack.isEmpty()) {
                NodeInfo nodeInfo = stack.pop();
            
                if (nodeInfo.getLocalName().equals(ruleName)) {
                    result.add(nodeInfo);
                } else {
                    SubnodesList subnodes = nodeInfo.getSubnodes();
                    for (int i = subnodes.size() - 1; i >= 0; i--) {
                        stack.push((NodeInfo) subnodes.get(i));
                    }
                }
            }
            
            return result;
        }
        
        @Override
        public NodeList getElementsByTagName(String name) {
            Predicate<Node> condition = "*".equals(name) ? n -> true : n -> n.getLocalName().equals(name);
            var result = new SubnodesList(5);
            var stack = new Stack<NodeInfo>();
            this.getSubnodes().forEach(n -> stack.push((NodeInfo)n));
            
            while (!stack.isEmpty()) {
                NodeInfo nodeInfo = stack.pop();
            
                if (nodeInfo != null) {
                    stack.push(nodeInfo);
                    stack.push(null);
                    SubnodesList subnodes = nodeInfo.getSubnodes();
                    for (int i = subnodes.size() - 1; i >= 0; i--) {
                        stack.push((NodeInfo) subnodes.get(i));
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
        public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
            return null;
        }

        @Override
        public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
            return null;
        }

        @Override
        public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
            if (namespaceURI == null || namespaceURI.length() == 0) {
                return this.getElementsByTagName(localName);
            } else {
                return null;
            }
        }

        @Override
        public boolean hasAttribute(String name) {
            return false;
        }

        @Override
        public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
            return false;
        }

        @Override
        public TypeInfo getSchemaTypeInfo() {
            return null;
        }

        @Override
        public void setIdAttribute(String name, boolean isId) throws DOMException {
            throw new UnsupportedOperationException();            
        }

        @Override
        public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
            throw new UnsupportedOperationException();
        }
    }
    
    private final Parser parser;
    private final Map<String, NodeTypeInfo> nodeTypeByRuleName;
    private final Map<String, LiteralTypeInfo> literalTypeByRuleName;
    
    private final XPath xpath;
    
    public SyntaxModel(Parser parser) {
        this.parser = parser;
        this.nodeTypeByRuleName = new HashMap<>();
        this.literalTypeByRuleName = new HashMap<>();

        XPathFactory xf = XPathFactory.newInstance();
        this.xpath = xf.newXPath();
        xpath.setXPathFunctionResolver(new CustomXPathFunctionResolver());
    }
    
    private Object mapLiteralValue(NodeInfo nodeInfo, Class<?> type) {
        SyntaxLiteral ruleAnnotation = type.getAnnotation(SyntaxLiteral.class);
        LiteralTypeInfo typeInfo = literalTypeByRuleName.get(ruleAnnotation.name());
        
        if (typeInfo != null) {
            try {
                String str = typeInfo.stringExpr == null ? nodeInfo.getTextContent() : typeInfo.stringExpr.evaluateExpression(nodeInfo, String.class);
                if (str != null && str.length() > 0) {
                    // System.out.println(str + " | " + nodeInfo.getNodeValue());
                    Object value = typeInfo.valuesByName.get(typeInfo.isCaseSensitive ? str : str.toUpperCase());
                    return value;
                }
            } catch (XPathExpressionException e) {
                e.printStackTrace(); // TODO collect errors
            }
            
            for (var literalCase : typeInfo.exprByValue.entrySet()) {
                try {
                    Object value = literalCase.getValue().evaluateExpression(nodeInfo, Boolean.class);
                    if (Boolean.TRUE.equals(value)) {
                        return literalCase.getKey();
                    }
                } catch (XPathExpressionException e) {
                    e.printStackTrace(); // TODO collect errors
                }
            }
        }

        return null;
    }
    
    private void mapSubtrees(NodeInfo nodeInfo, Class<? extends AbstractSyntaxNode> type,  boolean tryExact, boolean tryDescedants, Set<NodeInfo> subnodes) {
        SyntaxNode ruleAnnotation = type.getAnnotation(SyntaxNode.class);
        NodeTypeInfo typeInfo = nodeTypeByRuleName.get(ruleAnnotation.name());
        
        if (typeInfo != null) {
            if (tryExact && nodeInfo.getLocalName().equals(ruleAnnotation.name())) {
                if (subnodes.add(nodeInfo)) {
                    typeInfo.instantiateAndFill(nodeInfo);
                }
                return;
            }
            
            if (tryDescedants) {
                SubnodesList childNodes = nodeInfo.findDescedantLayerByName(typeInfo.ruleName);
                for (NodeInfo childNode : childNodes) {
                    if (subnodes.add(childNode)) {
                        typeInfo.instantiateAndFill(childNode);
                    }
                }
            }
        }
    }
    
    private NodeInfo mapSubtree(NodeInfo nodeInfo, Class<? extends AbstractSyntaxNode> type,  boolean tryExact, boolean tryDescedants) {
        SyntaxNode ruleAnnotation = type.getAnnotation(SyntaxNode.class);
        NodeTypeInfo typeInfo = nodeTypeByRuleName.get(ruleAnnotation.name());
        
        if (typeInfo != null) {
            if (tryExact && nodeInfo.getLocalName().equals(ruleAnnotation.name())) {
                typeInfo.instantiateAndFill(nodeInfo);
                return nodeInfo;
            }
            
            if (tryDescedants) {
                NodeInfo childNode = nodeInfo.findFirstDescedantByName(typeInfo.ruleName);
                if (childNode != null) {
                    typeInfo.instantiateAndFill(childNode);
                    return childNode;
                }
            }
        }
        return null;
    }
    
    public static class RecognitionResult<T> {
        // TODO introduce mapping errors collection
        public final T model;
        
        public RecognitionResult(T model) {
            this.model = model;
        }   
        
        public boolean isOk() {
            return model != null;
        }
    }
    
    public String toXml(Tree root) throws XMLStreamException, FactoryConfigurationError, TransformerException {
        NodeInfo rootInfo = new NodeInfo(null, root, 0);
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        StringWriter buffer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(rootInfo), new StreamResult(buffer));
        return buffer.toString();
    }
    
    public <T extends AbstractSyntaxNode> RecognitionResult<T> map(Tree root, Class<T> type) {
        NodeInfo nodeInfo = new NodeInfo(null, root, 0);
        NodeInfo modelNodeInfo = this.mapSubtree(nodeInfo, type, true, true);
        if (modelNodeInfo != null) {
            @SuppressWarnings("unchecked")
            T result = (T) modelNodeInfo.model;
            return new RecognitionResult<T>(result);
        } else {
            return new RecognitionResult<T>(null);
        }
    }

    private void appendIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("\t");
        }
    }
    
    public String stringify(AbstractSyntaxNode model) {
        StringBuilder sb = new StringBuilder();
        stringifyImpl(model, sb, 0);
        return sb.toString();
    }
    
    private void stringifyImpl(AbstractSyntaxNode model, StringBuilder sb, int indent) {
        this.appendIndent(sb, indent);
        sb.append("{");
        indent++;
        NodeTypeInfo typeInfo = this.nodeTypeByRuleName.get(model.getName());
        int n = 0;
        {
            sb.append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_position\": ").append(model.getStartPosition());
            sb.append(",").append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_type\": \"").append(model.getClass().getName()).append("\"");;
            sb.append(",").append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_ruleName\": \"").append(model.getName()).append("\"");
            if (typeInfo == null) {
                sb.append(",").append("\n");
                this.appendIndent(sb, indent);
                sb.append("\"_error\": \"").append("syntax model type info not found").append("\"");
            }
            n += 2;
        }
        if (typeInfo != null) {
            for (NodeFieldInfo field : typeInfo.getFields()) {
                Object value = field.getValue(model);
                if (n > 0) {
                    sb.append(",");
                }
                sb.append("\n");
                this.appendIndent(sb, indent);
                sb.append("\"").append(field.getName()).append("\"").append(": ");
                if (value == null) {
                    sb.append("null");
                } else {
                    switch (field.getKind()) {
                        case Enum:
                        case String:
                            sb.append('"').append(value.toString().replace("\"", "\\\"")).append('"');
                            break;
                        case Byte:
                        case Short:
                        case Int:
                        case Long:
                        case Bool:
                        case Float:
                        case Double:
                            sb.append(value);
                            break;
                        case Object:
                            sb.append("\n");
                            this.stringifyImpl((AbstractSyntaxNode) value, sb, indent);
                            break;
                        case Array:
                        case List: {
                            indent++;
                            sb.append("[");
                            int m = 0;
                            for (Object item : (Iterable<?>) value) {
                                if (m > 0) {
                                    sb.append(",");
                                }
                                sb.append("\n");
                                this.stringifyImpl((AbstractSyntaxNode) item, sb, indent); // TODO consider trivial item
                                m++;
                            }
                            indent--;
                            sb.append("\n");
                            this.appendIndent(sb, indent);
                            sb.append("]");
                            break;
                        }
                        default: throw new RuntimeException("Unexpected syntax model field kind " + field.getKind());
                    }
                }
                n++;
            }
        }
        
        indent--;
        sb.append("\n");
        this.appendIndent(sb, indent);
        sb.append("}");
    }
    
    private static <T> int maxValue(T[] arr, ToIntFunction<T> accessor, int def) {
        int result;
        if (arr == null || arr.length == 0) {
            result = def;
        } else {
            result = accessor.applyAsInt(arr[0]);
            for (int i = 1; i < arr.length; i++) {
                result = Math.max(result, accessor.applyAsInt(arr[i]));
            }
        }
        return result;
    }
    
    @FunctionalInterface
    private interface ThrowableFunction<T, R> {
        R apply(T obj) throws Throwable;
    }
    
    private <T, R, E extends Throwable> Function<T, R> captureExceptionInfo(Class<E> exceptionType, ThrowableFunction<T, R> mapper) {
        return o -> {
            try { return mapper.apply(o); }
            catch (Throwable ex) {
                if (exceptionType.isInstance(ex)) {
                    // TODO collect errors
                    ex.printStackTrace();
                    return null;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
    
    private void introduceEnum(Class<?> type) throws XPathExpressionException { // TODO collect errors
        SyntaxLiteral literalAnnotation = type.getAnnotation(SyntaxLiteral.class);
        if (literalAnnotation == null) {
            throw new RuntimeException("Type " + type.getName() + " is not marked as syntax ruleName!"); // TODO collect errors
        } else if (!type.isEnum()) {
            throw new RuntimeException("Type " + type.getName() + " is not a enum while marked as syntax literal!"); // TODO collect errors
        }
        
        var existing = literalTypeByRuleName.get(literalAnnotation.name());
        if (existing == null) {
            // var values = type.getEnumConstants();
            var enumEntries = Stream.of(type.getFields()).filter(f -> f.isEnumConstant()).map(captureExceptionInfo(IllegalAccessException.class, f -> new Object() {
               public final Field field = f;
               public final Object value = f.get(null);
               public final String name = f.getName();
               public final String upperCasedName = f.getName().toUpperCase();
               public final SyntaxLiteralCase literalCaseAnnotation = f.getAnnotation(SyntaxLiteralCase.class);
            })).collect(Collectors.toList());
            
            var countOfDefaultCasedNames = enumEntries.stream().map(e -> e.name).collect(Collectors.toSet()).size(); 
            var countOfUpperCasedNames = enumEntries.stream().map(e -> e.upperCasedName).collect(Collectors.toSet()).size();
            var isCaseSensitive = countOfDefaultCasedNames != countOfUpperCasedNames;
            var valuesByName = isCaseSensitive ? enumEntries.stream().collect(Collectors.toMap(e -> e.name, e -> e.value))
                                               : enumEntries.stream().collect(Collectors.toMap(e -> e.upperCasedName, e -> e.value));
            
            var exprByValue = enumEntries.stream()
                    .filter(e -> e.literalCaseAnnotation != null && e.literalCaseAnnotation.xcondition().length() > 0)
                    .collect(Collectors.toMap(
                            e -> e.value, 
                            captureExceptionInfo(XPathExpressionException.class, e -> xpath.compile(e.literalCaseAnnotation.xcondition())),
                            (x, y) -> { throw new IllegalStateException("Duplicated enum values"); /* should never happen*/ },
                            LinkedHashMap::new
                    ));
            var stringExpr = literalAnnotation.xstring().length() > 0 ? xpath.compile(literalAnnotation.xstring()) : null; // TODO collect errors
            literalTypeByRuleName.put(literalAnnotation.name(), new LiteralTypeInfo(literalAnnotation.name(), type, stringExpr, exprByValue, valuesByName, isCaseSensitive));
        } else if (!existing.type.equals(type)) {
            throw new RuntimeException("Ambiguous syntax literal: both " + type.getName() + " and " + existing.type.getName() + "  are marked with the same name " + literalAnnotation.name()); // TODO collect errors
        } else {
            // already registered, so nothing 
        }
    }

    public <T extends AbstractSyntaxNode> void introduce(Class<T> modelType) {        
        List<String> errors = new LinkedList<>();
        
        Set<Class<? extends AbstractSyntaxNode>> processedTypes = new HashSet<>();
        
        Queue<Class<? extends AbstractSyntaxNode>> queue = new LinkedList<>();
        queue.add(modelType);
        while (!queue.isEmpty()) {
            Class<? extends AbstractSyntaxNode> type = queue.remove();
            if (!processedTypes.add(type)) {
                continue;
            }
            SyntaxNode ruleAnnotation = type.getAnnotation(SyntaxNode.class);
            if (ruleAnnotation == null) {
                errors.add("Type " + type.getName() + " is not marked as syntax ruleName!");
                continue;
            }
            
            var fields = Stream.of(type.getFields()).map(f -> new Object() {
                public final Field info = f;
                public final SyntaxSubnode[] subnodeSpecs = f.getAnnotationsByType(SyntaxSubnode.class);
                public final SyntaxTerm[] termSpecs = f.getAnnotationsByType(SyntaxTerm.class);
            }).filter(
                f -> !Modifier.isStatic(f.info.getModifiers())
            ).collect(Collectors.toList());
            Map<String, NodeFieldInfo> modelFields = new HashMap<>(fields.size());

            for (var field : fields) {
                FieldTypeKind kind = resolveModelFieldKind(field.info.getType());
                if (field.subnodeSpecs.length > 0 && kind.isTerm) {
                    throw new RuntimeException("Field of terminal value kind cannot be bound with complex subnode type");
                }
                
                List<XPathExpression> termExprs = new ArrayList<>(field.termSpecs.length);
                List<NodeFieldSubnodeInfo> subnodeExprs = new ArrayList<>(field.subnodeSpecs.length);
                for (var termSpec : field.termSpecs) {
                    try {
                        termExprs.add(xpath.compile(termSpec.xpath()));
                        if (field.info.getType().isEnum()) {
                            introduceEnum(field.info.getType());
                        }
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException(e);
                    }
                }
                for (var subnodeSpec : field.subnodeSpecs) {
                    Class fieldType = subnodeSpec.type() == null  || subnodeSpec.type().equals(AbstractSyntaxNode.class) 
                        ? field.info.getType() : subnodeSpec.type();
                    try {
                        XPathExpression scopeExpr = subnodeSpec.xpath() != null && subnodeSpec.xpath().length() > 0 ? xpath.compile(subnodeSpec.xpath()) : null;  
                        subnodeExprs.add(new NodeFieldSubnodeInfo(scopeExpr, fieldType, subnodeSpec.lookup()));
                        queue.add(fieldType);
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                modelFields.put(field.info.getName(), new NodeFieldInfo(kind, field.info, termExprs, subnodeExprs));
            }
            
            Constructor<? extends AbstractSyntaxNode> ctor;
            try {
                ctor = type.getConstructor();
            } catch (Throwable ex) {
                errors.add("Failed to resolve default contructor for syntax model type " + type.getName());
                continue;
            }

            String ruleName = ruleAnnotation.name() != null && ruleAnnotation.name().length() > 0 ? ruleAnnotation.name() : type.getName();
            nodeTypeByRuleName.put(ruleName, new NodeTypeInfo(ruleName, type, ctor, modelFields));
        }
    }
    
    private static final Map<Class<?>, FieldTypeKind> builtinTypeKinds = Map.ofEntries(
        java.util.Map.entry(String.class, FieldTypeKind.String), 
        java.util.Map.entry(Byte.class, FieldTypeKind.Byte),
        java.util.Map.entry(Short.class, FieldTypeKind.Short), 
        java.util.Map.entry(Integer.class, FieldTypeKind.Int), 
        java.util.Map.entry(Long.class, FieldTypeKind.Long),
        java.util.Map.entry(Boolean.class, FieldTypeKind.Bool), 
        java.util.Map.entry(Float.class, FieldTypeKind.Float),
        java.util.Map.entry(Double.class, FieldTypeKind.Double), 

        java.util.Map.entry(byte.class, FieldTypeKind.Byte),
        java.util.Map.entry(short.class, FieldTypeKind.Short), 
        java.util.Map.entry(int.class, FieldTypeKind.Int),
        java.util.Map.entry(long.class, FieldTypeKind.Long), 
        java.util.Map.entry(boolean.class, FieldTypeKind.Bool), 
        java.util.Map.entry(float.class, FieldTypeKind.Float),
        java.util.Map.entry(double.class, FieldTypeKind.Double) 
    );

    private static FieldTypeKind resolveModelFieldKind(Class<?> fieldType) {
        FieldTypeKind kind = builtinTypeKinds.get(fieldType);
        if (kind == null) {
            if (fieldType.isEnum()) {
                kind = FieldTypeKind.Enum;
            } else if (fieldType.isArray()) {
                kind = FieldTypeKind.Array;
            } else if (fieldType.isAssignableFrom(ArrayList.class)) {
                kind = FieldTypeKind.List; 
            } else {
                kind = FieldTypeKind.Object;
            }
        }
        return kind;
    }
    
    private static String getText(Tree node) {
        String result;
        if (node instanceof ParseTree) {
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
            Interval textRange = Interval.of(a.getSourceInterval().a, b.getSourceInterval().b);
            result = b.getSymbol().getTokenSource().getInputStream().getText(textRange);
        }
        return result;
    }

    @FunctionalInterface
    private interface MyXPathFunction {
        Object evaluate(List<?> args) throws XPathExpressionException;
    }

    private static java.util.Map.Entry<String, XPathFunction> xfunction(String name, MyXPathFunction impl) {
        return java.util.Map.entry(name, new XPathFunction() {
            @Override
            public Object evaluate(List<?> args) throws XPathFunctionException {
                try {
                    return impl.evaluate(args);
                } catch (XPathExpressionException ex) {
                    throw new XPathFunctionException(ex);
                }
            }
        });
    }
    
    private static void flattenExclusiveImpl(
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
    
    private class CustomXPathFunctionResolver implements XPathFunctionResolver {

        private final Map<String, XPathFunction> functionByName = Map.ofEntries(
            xfunction("echo", args -> {
                for (Object o : args) {
                    if (o instanceof NodeList) {
                        NodeList nodeList = (NodeList)o;
                        if (nodeList.getLength() == 0) {
                            System.out.println("[]");
                        } else {
                            System.out.println(
                                streamOf(nodeList).map(n -> "  " + n.getLocalName() + ": \"" + n.getNodeValue() + "\"")
                                    .collect(Collectors.joining(",\n", "[\n", "\n]"))
                            );
                        }
                    } else if (o instanceof Node) {
                        Node node = (Node) o;
                        System.out.println(node.getLocalName() + ": \"" + node.getNodeValue() + "\"");
                    } else {
                        System.out.println(o);
                    }
                }
                return args.size() > 0 ? args.get(0) : null;
            }),
            xfunction("rootOf", args -> {
                if (args.size() > 0 && args.get(0) instanceof NodeList) {
                    NodeList nodeList = (NodeList) args.get(0);
                    if (nodeList.getLength() > 0) {
                        Node node = nodeList.item(0);
                        while (node.getParentNode() != null) {
                            node = node.getParentNode();
                        }
                        return node;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }),
            xfunction("flatten", args -> {
                final String signatureDescr = "flatten(roots:NodeList, stepExpr:String, justLeaves:bool = false, incudeRoot:bool = ture)";
                if (args.size() < 2) {
                    throw new XPathFunctionException("At least two arguments required for " + signatureDescr);
                } else if (args.size() > 4) {
                    throw new XPathFunctionException("No more than four arguments required for " + signatureDescr);
                } else {
                    NodeList roots = (NodeList) args.get(0);
                    XPathExpression stepExpr = prepareExpr(args.get(1).toString());
                    boolean justLeaves = args.size() > 2 ? (Boolean) args.get(2) : false;
                    boolean includeRoot = args.size() > 3 ? (Boolean) args.get(3) : true;
                    
                    NodesList<Node> result = new NodesList<>(); 
                    if (includeRoot && !justLeaves) {
                        result.ensureCapacity(roots.getLength());
                    }

                    for (Node root : iterableOf(roots)) {
                        if (includeRoot && !justLeaves) {
                            result.add(root);
                        } else {
                            flattenExclusiveImpl(root, stepExpr, justLeaves, result);
                        }
                    }
                    
                    return result;
                }
            }),
            xfunction("joinStrings", args -> {
                final String signatureDescr = "joinStrings(separator:String, nodes...:NodeList)";
                if (args.size() < 2) {
                    throw new XPathFunctionException("At least two arguments required for " + signatureDescr);
                } else {
                    StringBuilder sb = new StringBuilder();
                    String separator = args.get(0).toString();
                    int count = 0;
                    for (int i = 1; i < args.size(); i++) {
                        for (Node node : iterableOf((NodeList) args.get(i))) {
                            if (count > 0) {
                                sb.append(separator);
                            }
                            sb.append(node.getTextContent());
                            count++;
                        }
                    }
                    return sb.toString();
                }
            })
        );
        
        private final Map<String, XPathExpression> exprs = new HashMap<>();
        
        public CustomXPathFunctionResolver() {
        }
        
        private XPathExpression prepareExpr(String exprStr) throws XPathExpressionException {
            XPathExpression expr = exprs.get(exprStr);
            if (expr == null) {
                expr = xpath.compile(exprStr); 
                exprs.put(exprStr, expr);
            }
            return expr;
        }
        
        @Override
        public XPathFunction resolveFunction(QName functionName, int arity) {
            return functionByName.get(functionName.getLocalPart());
        }
        
    }
    
    private static Stream<Node> streamOf(final NodeList list) {
        return StreamSupport.stream(iterableOf(list).spliterator(), false);
    }
    
    private static Iterable<Node> iterableOf(final NodeList list) {
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
    
    private static Iterable<Node> reverseIterableOf(final XPathNodes list) {
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
    
    private static class EmptyAttrsMap implements NamedNodeMap {
        
        public static EmptyAttrsMap INSTANCE = new EmptyAttrsMap();
        
        private EmptyAttrsMap() {
        }
        
        @Override
        public Node setNamedItemNS(Node arg) throws DOMException {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Node setNamedItem(Node arg) throws DOMException {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Node removeNamedItem(String name) throws DOMException {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Node item(int index) {
            return null;
        }
        
        @Override
        public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
            return null;
        }
        
        @Override
        public Node getNamedItem(String name) {
            return null;
        }
        
        @Override
        public int getLength() {
            return 0;
        }
    }

}
