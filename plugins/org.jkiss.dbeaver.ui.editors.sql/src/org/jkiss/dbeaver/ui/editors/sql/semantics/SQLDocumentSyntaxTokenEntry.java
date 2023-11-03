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

import org.jkiss.dbeaver.model.sql.SQLScriptElement;

public class SQLDocumentSyntaxTokenEntry {
    public final SQLScriptElement scriptElement;
    public final SQLQuerySymbolEntry symbolEntry;
    public final int position, end;
    
    public SQLDocumentSyntaxTokenEntry(SQLScriptElement scriptElement, SQLQuerySymbolEntry symbolEntry) {
        this(scriptElement, symbolEntry, symbolEntry.getInterval().a + scriptElement.getOffset(), symbolEntry.getInterval().b + scriptElement.getOffset() + 1);
    }
    
    private SQLDocumentSyntaxTokenEntry(SQLScriptElement scriptElement, SQLQuerySymbolEntry symbolEntry, int position, int end) {
        this.scriptElement = scriptElement;
        this.symbolEntry = symbolEntry;
        this.position = position; 
        this.end = end; 
    }
    
    public int length() {
        return this.end - this.position;
    }
    
    public SQLDocumentSyntaxTokenEntry withInterval(int position, int end) {
        return new SQLDocumentSyntaxTokenEntry(this.scriptElement, this.symbolEntry, position, end);
    }
    
    @Override
    public String toString() {
        return "TokenEntry[@" + position + "+" + length() + ":" + symbolEntry + "]";
    }
}
