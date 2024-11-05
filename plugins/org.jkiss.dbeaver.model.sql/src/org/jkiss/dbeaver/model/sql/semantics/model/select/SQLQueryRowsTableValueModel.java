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
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Describes a table constructed by VALUES clause
 */
public class SQLQueryRowsTableValueModel extends SQLQueryRowsSourceModel {
    @NotNull
    private final List<SQLQueryValueExpression> values;
    private final boolean isIncomplete;
    
    public SQLQueryRowsTableValueModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQueryValueExpression> values,
        boolean isIncomplete) {
        super(syntaxNode);
        this.values = values;
        this.isIncomplete = isIncomplete;
    }

    @NotNull
    public List<SQLQueryValueExpression> getValues() {
        return values;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        LinkedList<SQLQueryResultColumn> resultColumns = new LinkedList<>();
        for (SQLQueryValueExpression value : this.values) {
            value.propagateContext(context, statistics);
            resultColumns.addLast(new SQLQueryResultColumn(resultColumns.size(), new SQLQuerySymbol("?"), this, null, null, SQLQueryExprType.UNKNOWN));
        }
        context = context.hideSources().overrideResultTuple(this, List.copyOf(resultColumns), Collections.emptyList());

        if (this.isIncomplete) {
            context = context.markHasUnresolvedSource();
        }

        return context;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsTableValue(this, arg);
    }
}