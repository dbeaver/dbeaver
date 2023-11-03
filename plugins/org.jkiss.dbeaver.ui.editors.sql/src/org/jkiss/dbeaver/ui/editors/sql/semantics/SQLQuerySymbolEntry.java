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

public class SQLQuerySymbolEntry implements SQLQuerySymbolDefinition {
    private final Interval region;
    private final String name;
    
    private SQLQuerySymbol symbol = null;
    private SQLQuerySymbolDefinition definition = null;
    
    public SQLQuerySymbolEntry(Interval region, String name) {
        this.region = region;
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public Interval getInterval() {
        return region;
    }
    
    public SQLQuerySymbolDefinition getDefinition() {
        return this.definition != null ? this.definition 
                : (this.symbol != null ? this.symbol.getDefinition() : null);
    }
    
    public void setDefinition(SQLQuerySymbolDefinition definition) {
        if (this.definition != null) {
            throw new UnsupportedOperationException("Symbol entry definition has already been set");
        } else {
            this.definition = definition;
            this.getSymbol().setSymbolClass(definition.getSymbolClass());
        }
    }
    
    public SQLQuerySymbol getSymbol() {
        if (symbol == null) {
            symbol = new SQLQuerySymbol(name);
            symbol.registerEntry(this);
        }
        return symbol;
    }

    @Override
    public SQLQuerySymbolClass getSymbolClass() {
        return this.getSymbol().getSymbolClass();
    }
    
    public void merge(SQLQuerySymbol symbol) {
        if (this.symbol != null) {
            
        } else {
            this.symbol = symbol;
            this.symbol.registerEntry(this);
        }
    }
    
    public SQLQuerySymbol merge(SQLQuerySymbolEntry other) {
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
    static void updateSymbol(SQLQuerySymbolEntry entry, SQLQuerySymbol newSymbol) {
        entry.symbol = newSymbol;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + this.name + ", " + this.getSymbolClass() + "]";
    }
}
