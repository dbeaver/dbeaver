package org.jkiss.dbeaver.parser;

import java.util.List;

public class ParseTreeNode {
    private final String ruleName;
    private final ParseTreeNode parent;
    private final List<ParseTreeNode> childs;

    public ParseTreeNode(String ruleName, ParseTreeNode parent, List<ParseTreeNode> childs) {
        this.ruleName = ruleName;
        this.parent = parent;
        this.childs = childs;
    }

    public String getRuleName() {
        return ruleName;
    }

    public ParseTreeNode getParent() {
        return parent;
    }

    public List<ParseTreeNode> getChilds() {
        return childs;
    }

}
