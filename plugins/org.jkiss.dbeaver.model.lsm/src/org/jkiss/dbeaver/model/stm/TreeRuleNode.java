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
package org.jkiss.dbeaver.model.stm;


import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jkiss.code.NotNull;


public class TreeRuleNode extends ParserRuleContext implements STMTreeNode {
    
    public TreeRuleNode() {
        super();
    }
    
    public TreeRuleNode(@NotNull ParserRuleContext parent, int invokingStateNumber) {
        super(parent, invokingStateNumber);
    }

    @NotNull
    public Interval getRealInterval() {
        return new Interval(this.getStart().getStartIndex(), this.getStop().getStopIndex());
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
        return super.addChild(new TreeTermNode(matchedToken));
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
        if (!(errorNode instanceof STMTreeNode)) {
            throw new IllegalStateException();
        } else {
            return super.addErrorNode(errorNode);
        }
    }
}
