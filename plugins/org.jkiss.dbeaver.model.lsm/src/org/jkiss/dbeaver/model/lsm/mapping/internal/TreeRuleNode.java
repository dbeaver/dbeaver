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
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TreeRuleNode extends ParserRuleContext implements Element, XTreeElementBase {

    public interface SubnodesList extends NodeList {

        @NotNull
        List<XTreeNodeBase> getCollection();

        @Nullable
        XTreeNodeBase item(int index);

        @Nullable
        XTreeNodeBase getFirst();

        @Nullable
        XTreeNodeBase getLast();
    }
    
    private class SubnodesListImpl implements SubnodesList {

        @NotNull
        public List<XTreeNodeBase> getCollection() {
            return (List<XTreeNodeBase>) (Object) TreeRuleNode.this.children;
        }

        @Nullable
        @Override
        public XTreeNodeBase item(int index) {
            List<XTreeNodeBase> items = getCollection();
            return index < 0 || index >= items.size() ? null : items.get(index);
        }

        @Override
        public int getLength() {
            return getCollection().size();
        }

        @Nullable
        public XTreeNodeBase getFirst() {
            List<XTreeNodeBase> items = getCollection();
            return items.size() > 0 ? items.get(0) : null;
        }

        @Nullable
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

    public TreeRuleNode(@NotNull ParserRuleContext parent, int invokingStateNumber) {
        super(parent, invokingStateNumber);
    }    
    
    @Override
    public int getIndex() {
        return index;
    }
    
    public void setModel(@Nullable AbstractSyntaxNode model) {
        if (this.model != null) {
            throw new IllegalStateException();
        } else {
            this.model = model;
        }
    }

    @Nullable
    public AbstractSyntaxNode getModel() {
        return this.model;
    }

    @NotNull
    @Override
    public Interval getRealInterval() {
        return new Interval(this.getStart().getStartIndex(), this.getStop().getStopIndex());
    }
    
    @Override
    public void fixup(@NotNull Parser parserCtx, int index) {
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

    @NotNull
    @Override
    public RuleContext addChild(@NotNull RuleContext ruleInvocation) {
        if (!(ruleInvocation instanceof XTreeNodeBase)) {
            throw new IllegalStateException();
        } else {
            return super.addChild(ruleInvocation);
        }
    }

    @NotNull
    @Override
    public TerminalNode addChild(@NotNull Token matchedToken) {
        return super.addChild(new TreeTermNode(matchedToken));
    }

    @NotNull
    @Override
    public TerminalNode addChild(@NotNull TerminalNode t) {
        if (!(t instanceof XTreeNodeBase)) {
            throw new IllegalStateException();
        } else {
            return super.addChild(t);
        }
    }

    @NotNull
    @Override
    public <T extends ParseTree> T addAnyChild(@NotNull T t) {
        if (!(t instanceof XTreeNodeBase)) {
            throw new IllegalStateException();
        } else {
            return super.addAnyChild(t);
        }
    }

    @NotNull
    @Override
    public ErrorNode addErrorNode(@NotNull Token badToken) {
        return super.addAnyChild(new TreeTermErrorNode(badToken));
    }

    @NotNull
    @Override
    public ErrorNode addErrorNode(@NotNull ErrorNode errorNode) {
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
