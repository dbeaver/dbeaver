/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Symbol entry in the text
 */
public class SQLQuerySymbolEntry extends SQLQueryLexicalScopeItem implements SQLQuerySymbolDefinition {
    @NotNull
    private final String name;
    @NotNull
    private final String rawName;
    @Nullable
    private final SQLQueryMemberAccessEntry memberAccessEntry;

    @Nullable
    private SQLQuerySymbol symbol = null;
    @Nullable
    private SQLQuerySymbolDefinition definition = null;

    public SQLQuerySymbolEntry(@NotNull STMTreeNode syntaxNode, @NotNull String name, @NotNull String rawName, @Nullable SQLQueryMemberAccessEntry memberAccessEntry) {
        super(syntaxNode);
        this.name = name;
        this.rawName = rawName;
        this.memberAccessEntry = memberAccessEntry;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getRawName() {
        return rawName;
    }

    public SQLQueryMemberAccessEntry getMemberAccess() {
        return this.memberAccessEntry;
    }

    @NotNull
    public Interval getInterval() {
        return this.syntaxNode.getRealInterval();
    }

    @Nullable
    public SQLQuerySymbolDefinition getDefinition() {
        return this.definition != null ? this.definition 
                : (this.symbol != null ? this.symbol.getDefinition() : null);
    }

    /**
     * Set symbol definition to the token
     */
    public void setDefinition(@Nullable SQLQuerySymbolDefinition definition) {
        if (this.definition != null) {
            throw new UnsupportedOperationException("Symbol entry definition has already been set");
        } else {
            if (this.symbol != null && this.symbol.getDefinition() != null) {
                this.definition = definition;
            } else {
                this.getSymbol().setDefinition(definition);
            }
        }
    }

    @Override
    public void setOrigin(SQLQuerySymbolOrigin origin) {
        super.setOrigin(origin);

        if (this.memberAccessEntry != null) {
            this.memberAccessEntry.setOrigin(origin);
        }
    }

    /**
     * Returns symbol associated with this symbol entry
     */
    @NotNull
    public SQLQuerySymbol getSymbol() {
        if (symbol == null) {
            symbol = new SQLQuerySymbol(name);
            symbol.registerEntry(this);
        }
        return symbol;
    }

    @NotNull
    @Override
    public SQLQuerySymbolClass getSymbolClass() {
        return this.getSymbol().getSymbolClass();
    }

    /**
     * Merge the other symbol with this one
     */
    public void merge(@NotNull SQLQuerySymbol symbol) {
        if (this.symbol != null) {
            // TODO: illegal operation?
        } else {
            this.symbol = symbol;
            this.symbol.registerEntry(this);
        }
    }

    /**
     * Merge the other symbol with this one and returns a new symbol
     */
    @NotNull
    public SQLQuerySymbol merge(@NotNull SQLQuerySymbolEntry other) {
        SQLQuerySymbol symbol;
        if (this.symbol != null && other.symbol != null) {
            symbol = this.symbol.merge(other.symbol);
        } else {
            if (this.symbol == null && other.symbol == null) {
                symbol = new SQLQuerySymbol(this.name);
                symbol.registerEntry(other);
                symbol.registerEntry(this);
                this.symbol = symbol;
                other.symbol = symbol;
            } else if (this.symbol != null) {
                symbol = this.symbol;
                symbol.registerEntry(other);
                other.symbol = symbol;
            } else {
                symbol = other.symbol;
                symbol.registerEntry(this);
                this.symbol = symbol;
            }
        }
        return symbol;
    }
    
    // private operation for symbol merging
    static void updateSymbol(@NotNull SQLQuerySymbolEntry entry, @NotNull SQLQuerySymbol newSymbol) {
        entry.symbol = newSymbol;
    }

    @Override
    public String toString() {
        return this.name + " (" + this.getSymbolClass() + ")";
    }

    public boolean isNotClassified() {
        return this.symbol == null || this.symbol.getSymbolClass() == SQLQuerySymbolClass.UNKNOWN;
    }
}
