package org.jkiss.dbeaver.antlr.model.internal;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class EmptyAttrsMap implements NamedNodeMap {
    
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
