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
package org.jkiss.dbeaver.ui.editors.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

import java.util.Collection;
import java.util.HashSet;

public class SQLQueryModel extends SQLQueryNodeModel {
    @NotNull
    private final HashSet<SQLQuerySymbolEntry> symbolEntries;
    @Nullable
    private final SQLQueryModelContent queryContent;

    public SQLQueryModel(
        @NotNull Interval range,
        @Nullable SQLQueryModelContent queryContent,
        @NotNull HashSet<SQLQuerySymbolEntry> symbolEntries
    ) {
        super(range);
        this.queryContent = queryContent;
        this.symbolEntries = symbolEntries;
    }

    @NotNull
    public Collection<SQLQuerySymbolEntry> getAllSymbols() {
        return symbolEntries;
    }

    @Nullable
    public SQLQueryModelContent getQueryModel() {
        return this.queryContent;
    }

    public void propagateContext(@NotNull SQLQueryDataContext dataContext, @NotNull SQLQueryRecognitionContext recognitionContext) {
        if (this.queryContent != null) {
            this.queryContent.applyContext(dataContext, recognitionContext);
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitSelectionModel(this, arg);
    }
}
