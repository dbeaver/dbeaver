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
package org.jkiss.dbeaver.antlr.model.internal;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.dbeaver.antlr.model.AbstractSyntaxNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TreeRuleNode extends ParserRuleContext implements Element, XTreeElementBase {

    public interface SubnodesList extends NodeList {

        List<XTreeNodeBase> getCollection();

        XTreeNodeBase item(int index);

        XTreeNodeBase getFirst();

        XTreeNodeBase getLast();
    }
    
    private class SubnodesListImpl implements SubnodesList {
        
        public List<XTreeNodeBase> getCollection() {
            return (List<XTreeNodeBase>)(Object)TreeRuleNode.this.children;
        }
        
        @Override
        public XTreeNodeBase item(int index) {
            List<XTreeNodeBase> items = getCollection();
            return index < 0 || index >= items.size() ? null : items.get(index);
        }

        @Override
        public int getLength() {
            return getCollection().size();
        }

        public XTreeNodeBase getFirst() {
            List<XTreeNodeBase> items = getCollection();
            return items.size() > 0 ? items.get(0) : null;
        }

        public XTreeNodeBase getLast() {
            List<XTreeNodeBase> items = getCollection();
            return items.size() > 0 ? items.get(items.size() - 1) : null;
        }
    }
    
    private final SubnodesList subnodes = new SubnodesListImpl();
    
    private String nodeName = null;
    private int index = -1;
    private AbstractSyntaxNode model;
    private Map<String, Object> userData;
    
    public TreeRuleNode() {
        super();
    }

    public TreeRuleNode(ParserRuleContext parent, int invokingStateNumber) {
        super(parent, invokingStateNumber);
    }    
    
    @Override
    public int getIndex() {
        return index;
    }
    
    public void setModel(AbstractSyntaxNode model) {
        if (this.model != null) {
            throw new IllegalStateException();
        } else {
            this.model = model;
        }
    }
    
    public AbstractSyntaxNode getModel() {
        return this.model;
    }
    
    @Override
    public void fixup(Parser parserCtx, int index) {
        this.index = index;
        if (this.getChildCount() > 0) {
            nodeName = Trees.getNodeText(this, parserCtx);
            List<XTreeNodeBase> subnodes = getSubnodes().getCollection();
            for (int i = 0; i < subnodes.size(); i++) {
                subnodes.get(i).fixup(parserCtx, i);
            }
        } else {
            throw new IllegalStateException(); // Should never happen?
        }
    }
    
    @Override
    public RuleContext addChild(RuleContext ruleInvocation) {
        if (!(ruleInvocation instanceof XTreeNodeBase)) {
            throw new IllegalStateException();
        } else {
            return super.addChild(ruleInvocation);
        }
    }
    
    @Override
    public TerminalNode addChild(Token matchedToken) {
        return super.addChild(new TreeTermNode(matchedToken));
    }
    
    @Override
    public TerminalNode addChild(TerminalNode t) {
        if (!(t instanceof XTreeNodeBase)) {
            throw new IllegalStateException();
        } else {
            return super.addChild(t);
        }
    }
    
    @Override
    public <T extends ParseTree> T addAnyChild(T t) {
        if (!(t instanceof XTreeNodeBase)) {
            throw new IllegalStateException();
        } else {
            return super.addAnyChild(t);
        }
    }
    
    @Override
    public ErrorNode addErrorNode(Token badToken) {
        return super.addAnyChild(new TreeTermErrorNode(badToken));
    }
    
    @Override
    public ErrorNode addErrorNode(ErrorNode errorNode) {
        if (!(errorNode instanceof XTreeNodeBase)) {
            throw new IllegalStateException();
        } else {
            return super.addErrorNode(errorNode);
        }
    }
    
    public SubnodesList getSubnodes() {
        return this.subnodes;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public short getNodeType() {
        return Node.ELEMENT_NODE;
    }
    
    @Override
    public Map<String, Object> getUserDataMap(boolean createIfMissing) {
        return userData != null ? userData : (userData = new HashMap<>());
    }
}
