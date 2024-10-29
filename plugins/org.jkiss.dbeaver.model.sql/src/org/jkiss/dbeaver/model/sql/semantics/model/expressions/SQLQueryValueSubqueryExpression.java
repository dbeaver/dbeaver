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
package org.jkiss.dbeaver.model.sql.semantics.model.expressions;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Describes subquery
 */
public class SQLQueryValueSubqueryExpression extends SQLQueryValueExpression {
    @NotNull
    private final SQLQueryRowsSourceModel source;

    public SQLQueryValueSubqueryExpression(@NotNull STMTreeNode syntaxNode, @NotNull SQLQueryRowsSourceModel source) {
        super(syntaxNode, source);
        this.source = source;
    }

    @NotNull
    public SQLQueryRowsSourceModel getSource() {
        return this.source;
    }

    @Override
    protected void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.source.propagateContext(context, statistics);
        this.type = SQLQueryExprType.forScalarSubquery(this.source);
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueSubqueryExpr(this, arg);
    }
}
