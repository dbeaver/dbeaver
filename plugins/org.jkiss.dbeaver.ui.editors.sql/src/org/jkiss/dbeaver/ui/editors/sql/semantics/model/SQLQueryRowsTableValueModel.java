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
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;

import java.util.List;
import java.util.stream.Collectors;


public class SQLQueryRowsTableValueModel extends SQLQueryRowsSourceModel {
    private final List<SQLQueryValueExpression> values;
    
    public SQLQueryRowsTableValueModel(@NotNull Interval range, @NotNull List<SQLQueryValueExpression> values) {
        super(range);
        this.values = values;
    }

    @NotNull
    public List<SQLQueryValueExpression> getValues() {
        return values;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.values.forEach(v -> v.propagateContext(context, statistics));
        return context.hideSources().overrideResultTuple(this.values.stream()
            .map(e -> new SQLQueryResultColumn(new SQLQuerySymbol("?"), this, null, null, SQLQueryExprType.UNKNOWN))
            .collect(Collectors.toList()));
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsTableValue(this, arg);
    }
}