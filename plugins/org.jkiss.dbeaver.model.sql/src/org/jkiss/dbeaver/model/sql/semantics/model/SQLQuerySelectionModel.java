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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;

import java.util.Collection;
import java.util.HashSet;

public class SQLQuerySelectionModel extends SQLQueryNodeModel {

    private final HashSet<SQLQuerySymbolEntry> symbolEntries;
    private final SQLQueryRowsSourceModel resultSource;

    public SQLQuerySelectionModel(
        @NotNull Interval range,
        @Nullable SQLQueryRowsSourceModel resultSource,
        @NotNull HashSet<SQLQuerySymbolEntry> symbolEntries
    ) {
        super(range, resultSource);
        this.resultSource = resultSource;
        this.symbolEntries = symbolEntries;
    }

    @NotNull
    public Collection<SQLQuerySymbolEntry> getAllSymbols() {
        return symbolEntries;
    }

    @Nullable
    public SQLQueryRowsSourceModel getResultSource() {
        return this.resultSource;
    }
    
    @Override
    public SQLQueryDataContext getDataContext() {
        return this.resultSource == null ? null : this.resultSource.getDataContext();
    }

    public void propagateContex(@NotNull SQLQueryDataContext dataContext, @NotNull SQLQueryRecognitionContext recognitionContext) {
        if (this.resultSource != null) {
            this.resultSource.propagateContext(dataContext, recognitionContext);
        }
    }
    
    public SQLQueryNodeModel findNodeContaining(int textOffset) {
        SQLQueryNodeModel node = this;
        SQLQueryNodeModel nested = node.findChildNodeContaining(textOffset); 
        while (nested != null) {
            node = nested;
            nested = nested.findChildNodeContaining(textOffset);
        }
        return node;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitSelectionModel(this, arg);
    }
}
