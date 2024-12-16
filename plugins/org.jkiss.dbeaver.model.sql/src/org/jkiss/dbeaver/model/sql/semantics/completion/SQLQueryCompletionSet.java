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
package org.jkiss.dbeaver.model.sql.semantics.completion;

import java.util.Collection;

public class SQLQueryCompletionSet {
    private final int replacementPosition;
    private final int replacementLength;
    private final Collection<? extends SQLQueryCompletionItem> items;
    
    public SQLQueryCompletionSet(int replacementPosition, int replacementLength, Collection<? extends SQLQueryCompletionItem> items) {
        this.replacementPosition = replacementPosition;
        this.replacementLength = replacementLength;
        this.items = items;
    }
    
    public int getReplacementPosition() {
        return this.replacementPosition;
    }
    
    public int getReplacementLength() {
        return this.replacementLength;
    }
    
    public Collection<? extends SQLQueryCompletionItem> getItems() {
        return this.items;
    }
}
