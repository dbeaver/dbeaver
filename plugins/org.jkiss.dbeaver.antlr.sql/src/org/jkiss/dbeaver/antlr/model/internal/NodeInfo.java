//package org.jkiss.dbeaver.antlr.model.internal;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Stack;
//import java.util.function.Predicate;
//
//import javax.xml.xpath.XPathExpression;
//
//import org.antlr.v4.runtime.Parser;
//import org.antlr.v4.runtime.tree.Tree;
//import org.antlr.v4.runtime.tree.Trees;
//import org.jkiss.dbeaver.antlr.model.AbstractSyntaxNode;
//import org.w3c.dom.Attr;
//import org.w3c.dom.DOMException;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NamedNodeMap;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//import org.w3c.dom.TypeInfo;
//import org.w3c.dom.UserDataHandler;
//
//public class NodeInfo implements Element {
//    
//    public static class SubnodesList extends NodesList<NodeInfo> {
//        
//        public SubnodesList() {
//            super();
//        }
//        
//        public SubnodesList(int capacity) {
//            super(capacity);
//        }
//    }
//    
//    public final NodeInfo parent;
//    public final Tree node;
//    public final int index;
//    
//    private final Parser parserCtx;
//    
//    private SubnodesList subnodes;
//    private AbstractSyntaxNode model;
//    private Map<String, Object> userData;
//        
//    public NodeInfo(NodeInfo parent, Tree node, int index, Parser parserCtx) {
//        this.parent = parent;
//        this.node = node;
//        this.index = index;
//        this.subnodes = null;
//        this.model = null;
//        this.userData = null;
//        this.parserCtx = parserCtx;
//    }
//    
//    public void setModel(AbstractSyntaxNode model) {
//        if (this.model != null) {
//            throw new IllegalStateException();
//        } else {
//            this.model = model;
//        }
//    }
//    
//    public AbstractSyntaxNode getModel() {
//        return this.model;
//    }
//    
//    public SubnodesList getSubnodes() {
//        if (this.subnodes == null) {
//            int count = node.getChildCount();
//            var subnodes = new SubnodesList(count);
//            for (int i = 0; i < count; i++) {
//                subnodes.add(new NodeInfo(this, node.getChild(i), i, parserCtx));
//            }
//            this.subnodes = subnodes;
//        }
//        return this.subnodes;
//    }
//
//    @Override
//    public String getNodeName() {
//        if (node.getChildCount() > 0) {
//            return Trees.getNodeText(node, parserCtx);
//        } else {
//            return "#text";
//        }
//    }
//
//    @Override
//    public String getNodeValue() throws DOMException {
//        return CustomXPathUtils.getText(node);
//    }
//
//    @Override
//    public void setNodeValue(String nodeValue) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public short getNodeType() {
//        if (node.getChildCount() > 0) {
//            return Node.ELEMENT_NODE;
//        } else {
//            return Node.TEXT_NODE;
//        }
//    }
//
//    @Override
//    public Node getParentNode() {
//        return parent;
//    }
//    
//    @Override
//    public NodeList getChildNodes() {
//        return this.getSubnodes();
//    }
//
//    @Override
//    public Node getFirstChild() {
//        return this.getSubnodes().getFirst();
//    }
//
//    @Override
//    public Node getLastChild() {
//        return this.getSubnodes().getLast();
//    }
//
//    @Override
//    public Node getPreviousSibling() {
//        return this.parent == null ? null : this.parent.getSubnodes().item(this.index - 1);
//    }
//
//    @Override
//    public Node getNextSibling() {
//        return this.parent == null ? null : this.parent.getSubnodes().item(this.index + 1);
//    }
//
//    @Override
//    public NamedNodeMap getAttributes() {
//        return EmptyAttrsMap.INSTANCE;
//    }
//
//    @Override
//    public Document getOwnerDocument() {
//        return null;
//    }
//
//    @Override
//    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Node removeChild(Node oldChild) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Node appendChild(Node newChild) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean hasChildNodes() {
//        return this.getSubnodes().size() > 0;
//    }
//
//    @Override
//    public Node cloneNode(boolean deep) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void normalize() {
//        // do nothing
//    }
//
//    @Override
//    public boolean isSupported(String feature, String version) {
//        return false;
//    }
//
//    @Override
//    public String getNamespaceURI() {
//        return null;
//    }
//
//    @Override
//    public String getPrefix() {
//        return null;
//    }
//
//    @Override
//    public void setPrefix(String prefix) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public String getLocalName() {
//        return this.getNodeName();
//    }
//
//    @Override
//    public boolean hasAttributes() {
//        return false;
//    }
//
//    @Override
//    public String getBaseURI() {
//        return null;
//    }
//
//    @Override
//    public short compareDocumentPosition(Node other) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public String getTextContent() throws DOMException {
//        return this.getNodeValue();
//    }
//
//    @Override
//    public void setTextContent(String textContent) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean isSameNode(Node other) {
//        return this.equals(other);
//    }
//
//    @Override
//    public String lookupPrefix(String namespaceURI) {
//        return null;
//    }
//
//    @Override
//    public boolean isDefaultNamespace(String namespaceURI) {
//        return false;
//    }
//
//    @Override
//    public String lookupNamespaceURI(String prefix) {
//        return null;
//    }
//
//    @Override
//    public boolean isEqualNode(Node arg) {
//        return this.equals(arg);
//    }
//
//    @Override
//    public Object getFeature(String feature, String version) {
//        return null;
//    }
//
//    @Override
//    public Object setUserData(String key, Object data, UserDataHandler handler) {
//        if (this.userData == null) {
//            this.userData = new HashMap<>();
//        }
//        return this.userData.replace(key, data);
//    }
//
//    @Override
//    public Object getUserData(String key) {
//        return this.userData == null ? null : this.userData.get(key);
//    }
//
//    @Override
//    public String getTagName() {
//        return this.getLocalName();
//    }
//
//    @Override
//    public String getAttribute(String name) {
//        return null;
//    }
//
//    @Override
//    public void setAttribute(String name, String value) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void removeAttribute(String name) throws DOMException {
//        throw new UnsupportedOperationException();    
//    }
//
//    @Override
//    public Attr getAttributeNode(String name) {
//        return null;
//    }
//
//    @Override
//    public Attr setAttributeNode(Attr newAttr) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    public NodeInfo findFirstDescedantByName(String ruleName) {
//        var stack = new Stack<NodeInfo>();
//        this.getSubnodes().forEach(n -> stack.push((NodeInfo)n));
//        
//        while (!stack.isEmpty()) {
//            NodeInfo nodeInfo = stack.pop();
//        
//            if (nodeInfo.getLocalName().equals(ruleName)) {
//                return nodeInfo;
//            } else {
//                SubnodesList subnodes = nodeInfo.getSubnodes();
//                for (int i = subnodes.size() - 1; i >= 0; i--) {
//                    stack.push((NodeInfo) subnodes.get(i));
//                }
//            }
//        }
//        
//        return null;
//    }
//    
//    public SubnodesList findDescedantLayerByName(String ruleName) {
//        var result = new SubnodesList(5);
//        var stack = new Stack<NodeInfo>();
//        this.getSubnodes().forEach(n -> stack.push((NodeInfo)n));
//        
//        while (!stack.isEmpty()) {
//            NodeInfo nodeInfo = stack.pop();
//        
//            if (nodeInfo.getLocalName().equals(ruleName)) {
//                result.add(nodeInfo);
//            } else {
//                SubnodesList subnodes = nodeInfo.getSubnodes();
//                for (int i = subnodes.size() - 1; i >= 0; i--) {
//                    stack.push((NodeInfo) subnodes.get(i));
//                }
//            }
//        }
//        
//        return result;
//    }
//    
//    @Override
//    public NodeList getElementsByTagName(String name) {
//        Predicate<Node> condition = "*".equals(name) ? n -> true : n -> n.getLocalName().equals(name);
//        var result = new SubnodesList(5);
//        var stack = new Stack<NodeInfo>();
//        this.getSubnodes().forEach(n -> stack.push((NodeInfo)n));
//        
//        while (!stack.isEmpty()) {
//            NodeInfo nodeInfo = stack.pop();
//        
//            if (nodeInfo != null) {
//                stack.push(nodeInfo);
//                stack.push(null);
//                SubnodesList subnodes = nodeInfo.getSubnodes();
//                for (int i = subnodes.size() - 1; i >= 0; i--) {
//                    stack.push((NodeInfo) subnodes.get(i));
//                }
//            } else {
//                nodeInfo = stack.pop();
//                if (condition.test(nodeInfo)) {
//                    result.add(nodeInfo);
//                }
//            }
//        }
//        
//        return result;
//    }
//
//    @Override
//    public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
//        return null;
//    }
//
//    @Override
//    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
//        return null;
//    }
//
//    @Override
//    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
//        if (namespaceURI == null || namespaceURI.length() == 0) {
//            return this.getElementsByTagName(localName);
//        } else {
//            return null;
//        }
//    }
//
//    @Override
//    public boolean hasAttribute(String name) {
//        return false;
//    }
//
//    @Override
//    public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
//        return false;
//    }
//
//    @Override
//    public TypeInfo getSchemaTypeInfo() {
//        return null;
//    }
//
//    @Override
//    public void setIdAttribute(String name, boolean isId) throws DOMException {
//        throw new UnsupportedOperationException();            
//    }
//
//    @Override
//    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
//        throw new UnsupportedOperationException();
//    }
//}
