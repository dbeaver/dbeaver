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

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.jkiss.code.NotNull;


public class STMTreeTermNode extends TerminalNodeImpl implements STMTreeNode {
    
    private String nodeName = null;
    private final int atnState;
    
    public STMTreeTermNode(@NotNull Token symbol) {
        super(symbol);
        this.atnState = -1;
    }
    
    public STMTreeTermNode(@NotNull Token symbol, int atnState) {
        super(symbol);
        this.atnState = atnState;
    }

    public int getAtnState() {
        return this.atnState;
    }

    @Override
    public void fixup(@NotNull STMParserOverrides parserCtx) {
        this.nodeName = parserCtx.getVocabulary().getSymbolicName(this.getSymbol().getType());
    }

    @NotNull
    @Override
    public String getNodeName() {
        return this.nodeName;
    }
    
    @NotNull
    public Interval getRealInterval() {
        return new Interval(this.getSymbol().getStartIndex(), this.getSymbol().getStopIndex());
    }

    @NotNull
    @Override
    public String getTextContent() {
        Interval textRange = this.getRealInterval();
        return this.getSymbol().getInputStream().getText(textRange);
    }

    @Override
    public boolean hasErrorChildren() {
        return false;
    }
}
