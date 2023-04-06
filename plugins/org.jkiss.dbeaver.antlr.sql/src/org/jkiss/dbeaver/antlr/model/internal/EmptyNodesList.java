package org.jkiss.dbeaver.antlr.model.internal;

import java.util.Collections;
import java.util.List;

import org.jkiss.dbeaver.antlr.model.internal.TreeRuleNode.SubnodesList;
import org.w3c.dom.NodeList;

public class EmptyNodesList implements NodeList, SubnodesList {
    
    public static EmptyNodesList INSTANCE = new EmptyNodesList();
    
    private EmptyNodesList() {
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public List<CustomXPathModelNodeBase> getCollection() {
        return Collections.emptyList();
    }

    @Override
    public CustomXPathModelNodeBase item(int index) {
        return null;
    }

    @Override
    public CustomXPathModelNodeBase getFirst() {
        return null;
    }

    @Override
    public CustomXPathModelNodeBase getLast() {
        return null;
    }
    
}
