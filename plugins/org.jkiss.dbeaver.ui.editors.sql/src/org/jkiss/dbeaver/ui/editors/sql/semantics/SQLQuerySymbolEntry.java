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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class SQLQuerySymbolEntry implements SQLQuerySymbolDefinition {
    private final Interval region;
    private final String name;
    private final String rawName;
    
    private SQLQuerySymbol symbol = null;
    private SQLQuerySymbolDefinition definition = null;
    
    public SQLQuerySymbolEntry(@NotNull Interval region, @NotNull String name, @NotNull String rawName) {
        this.region = region;
        this.name = name;
        this.rawName = rawName;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getRawName() {
        return rawName;
    }

    @NotNull
    public Interval getInterval() {
        return region;
    }

    @Nullable
    public SQLQuerySymbolDefinition getDefinition() {
        return this.definition != null ? this.definition 
                : (this.symbol != null ? this.symbol.getDefinition() : null);
    }
    
    public void setDefinition(@Nullable SQLQuerySymbolDefinition definition) {
        if (this.definition != null) {
            throw new UnsupportedOperationException("Symbol entry definition has already been set");
        } else {
            this.definition = definition;
            this.getSymbol().setSymbolClass(definition.getSymbolClass());
        }
    }

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
    
    public void merge(@Nullable SQLQuerySymbol symbol) {
        if (this.symbol != null) {
            
        } else {
            this.symbol = symbol;
            this.symbol.registerEntry(this);
        }
    }

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
        return super.toString() + "[" + this.name + ", " + this.getSymbolClass() + "]";
    }

    public boolean isNotClassified() {
        return this.symbol == null || this.symbol.getSymbolClass() == SQLQuerySymbolClass.UNKNOWN;
    }
}
