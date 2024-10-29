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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SQLQuerySymbol {
    private final String name;
    private final Set<SQLQuerySymbolEntry> entries = new HashSet<>();
    
    private SQLQuerySymbolClass symbolClass = SQLQuerySymbolClass.UNKNOWN;
    private SQLQuerySymbolDefinition definition = null;

    public SQLQuerySymbol(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public SQLQuerySymbolClass getSymbolClass() {
        return this.symbolClass;
    }

    /**
     * Set symbol class to this symbol.
     * Throws IllegalStateException, if symbol has been already classified
     */
    public void setSymbolClass(@NotNull SQLQuerySymbolClass symbolClass) {
        if (this.symbolClass != SQLQuerySymbolClass.UNKNOWN) {
            throw new IllegalStateException("Symbol already classified");
        } else {
            this.symbolClass = symbolClass;
        }
    }

    @NotNull
    public Collection<SQLQuerySymbolEntry> getEntries() {
        return this.entries;
    }

    @Nullable
    public SQLQuerySymbolDefinition getDefinition() {
        return this.definition;
    }

    /**
     * Set symbol definition to this symbol.
     * Throws IllegalStateException, if symbol definition has already been set
     */
    public void setDefinition(@Nullable SQLQuerySymbolDefinition definition) {
        if (this.definition != null) {
            throw new IllegalStateException("Symbol definition has already been set");
        } else if (definition != null) {
            this.definition = definition;
            this.setSymbolClass(definition.getSymbolClass());
        }
    }

    /**
     * Register symbol entry.
     * Throws IllegalStateException, if symbol has already been registered
     */
    public void registerEntry(@NotNull SQLQuerySymbolEntry entry) {
        if (!entry.getName().equals(this.name)) {
            throw new IllegalStateException("Cannot treat symbols '" + entry.getName() + "' as an instance of '" + this.name + "'");
        }
        
        this.entries.add(entry);
    }

    /**
     * Merge this symbol with the other one
     */
    @NotNull
    public SQLQuerySymbol merge(@NotNull SQLQuerySymbol other) { // TODO merge multiple definitions and check for symbolClass
        if (!other.name.equals(this.name)) {
            throw new UnsupportedOperationException("Cannot treat different symbols as one ('" + this.name + "' and '" + other.name + "')");
        }
        
        SQLQuerySymbol result = new SQLQuerySymbol(this.name);
        result.entries.addAll(this.entries);
        result.entries.addAll(other.entries);
        result.entries.forEach(e -> SQLQuerySymbolEntry.updateSymbol(e, result));
        return result;
    }
    
    @Override
    public String toString() {
        return super.toString() + "[" + this.name + "]";
    }
}
