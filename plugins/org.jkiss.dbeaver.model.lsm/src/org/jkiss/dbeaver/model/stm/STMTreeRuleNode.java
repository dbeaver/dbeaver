/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.stm;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.code.NotNull;


public class STMTreeRuleNode extends ParserRuleContext implements STMTreeNode {
    
    private String nodeName = null;

    private boolean hasErrorChildren = false;
    
    public STMTreeRuleNode() {
        super();
    }
    
    public STMTreeRuleNode(@NotNull ParserRuleContext parent, int invokingStateNumber) {
        super(parent, invokingStateNumber);
    }
    
    @Override
    public int getAtnState() {
        return super.invokingState;
    }
    
    @Override
    public int getNodeKindId() {
        return super.getRuleContext().getRuleIndex();
    }

    @Override
    public void fixup(@NotNull STMParserOverrides parserCtx) {
        nodeName = Trees.getNodeText(this, parserCtx);
        for (int i = 0; i < getChildCount(); i++) {
            ((STMTreeNode) getChild(i)).fixup(parserCtx);
        }
    }

    @NotNull
    public String getNodeName() {
        return nodeName;
    }
    
    @NotNull
    public Interval getRealInterval() {
        if (this.start == null || this.stop == null) {
            return Interval.INVALID;
        }
        int start = this.getStart().getStartIndex();
        int end = this.getStop().getStopIndex();
        return new Interval(start, Math.max(start, end));
    }

    @NotNull
    @Override
    public String getTextContent() {
        Interval textRange = this.getRealInterval();
        return this.getStart().getInputStream().getText(textRange);
    }

    @NotNull
    @Override
    public RuleContext addChild(@NotNull RuleContext ruleInvocation) {
        if (!(ruleInvocation instanceof STMTreeNode)) {
            throw new IllegalStateException();
        } else {
            return super.addChild(ruleInvocation);
        }
    }

    @NotNull
    @Override
    public TerminalNode addChild(@NotNull Token matchedToken) {
        return super.addChild(new STMTreeTermNode(matchedToken));
    }

    @NotNull
    @Override
    public TerminalNode addChild(@NotNull TerminalNode t) {
        if (!(t instanceof STMTreeNode)) {
            throw new IllegalStateException();
        } else {
            return super.addChild(t);
        }
    }

    @NotNull
    @Override
    public <T extends ParseTree> T addAnyChild(@NotNull T t) {
        if (!(t instanceof STMTreeNode)) {
            throw new IllegalStateException();
        } else {
            this.hasErrorChildren |= t instanceof ErrorNode;
            return super.addAnyChild(t);
        }
    }

    @NotNull
    @Override
    public ErrorNode addErrorNode(@NotNull Token badToken) {
        return super.addAnyChild(new STMTreeTermErrorNode(badToken));
    }

    @NotNull
    @Override
    public ErrorNode addErrorNode(@NotNull ErrorNode errorNode) {
        if (!(errorNode instanceof STMTreeNode)) {
            throw new IllegalStateException();
        } else {
            return super.addErrorNode(errorNode);
        }
    }
    
    @Override
    public STMTreeNode getChildNode(int index) {
        return (STMTreeNode) super.getChild(index);
    }

    /**
     * Returns true, if some parsing errors happen, while analysing this node
     */
    public boolean hasErrorChildren() {
        return this.hasErrorChildren;
    }
}
