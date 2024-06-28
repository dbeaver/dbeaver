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
package org.jkiss.dbeaver.model.sql.semantics.model.select;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;

/**
 * Describes subquery of a common table expression
 */
public class SQLQueryRowsCteSubqueryModel extends SQLQueryRowsSourceModel {
    @NotNull
    public final SQLQuerySymbolEntry subqueryName;
    @NotNull
    public final List<SQLQuerySymbolEntry> columNames;
    @NotNull
    public final SQLQueryRowsSourceModel source;

    public SQLQueryRowsCteSubqueryModel(
        @NotNull SQLQueryModelContext context,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQuerySymbolEntry subqueryName,
        @NotNull List<SQLQuerySymbolEntry> columNames,
        @NotNull SQLQueryRowsSourceModel source
    ) {
        super(context, syntaxNode);
        this.subqueryName = subqueryName;
        this.columNames = columNames;
        this.source = source;
    }

    /**
     * Associate CTE subquery alias symbol with its definition
     */
    public void prepareAliasDefinition() {
        this.subqueryName.getSymbol().setDefinition(this.subqueryName);
        if (this.subqueryName.isNotClassified()) {
            this.subqueryName.getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE_ALIAS);
        }
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        return context; // just apply given context
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
        return visitor.visitRowsCteSubquery(this, node);
    }
}