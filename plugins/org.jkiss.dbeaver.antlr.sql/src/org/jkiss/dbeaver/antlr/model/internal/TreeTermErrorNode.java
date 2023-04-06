package org.jkiss.dbeaver.antlr.model.internal;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNodeImpl;
import org.jkiss.dbeaver.antlr.model.internal.TreeRuleNode.SubnodesList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TreeTermErrorNode extends ErrorNodeImpl implements CustomXPathModelTextBase {
    
    private int index = -1;
    
    private Map<String, Object> userData;
    
    public TreeTermErrorNode(Token symbol) {
        super(symbol);
    }
    
    public int getIndex() {
        return index;
    }

    @Override
    public void fixup(Parser parser, int index) {
        this.index = index;
    }
    
    @Override
    public SubnodesList getSubnodes() {
        return EmptyNodesList.INSTANCE;
    }
    
    @Override
    public NodeList getChildNodes() {
        return EmptyNodesList.INSTANCE;
    }
    
    @Override
    public short getNodeType() {
        return Node.TEXT_NODE;
    }
    
    @Override
    public String getNodeName() {
        return "#text";
    }
    
    @Override
    public Map<String, Object> getUserDataMap(boolean createIfMissing) {
        return userData != null ? userData : (userData = new HashMap<>());
    }    

}
