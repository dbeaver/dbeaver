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

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

public abstract class SQLQueryValueExpression extends SQLQueryNodeModel {

    @NotNull
    protected SQLQueryExprType type = SQLQueryExprType.UNKNOWN;
    @Nullable
    protected SQLQueryDataContext dataContext = null;

    public SQLQueryValueExpression(@NotNull STMTreeNode syntaxNode, @Nullable SQLQueryNodeModel ... subnodes) {
        this(syntaxNode.getRealInterval(), syntaxNode, subnodes);
    }

    public SQLQueryValueExpression(@NotNull Interval region, STMTreeNode syntaxNode, @Nullable SQLQueryNodeModel ... subnodes) {
        super(region, syntaxNode, subnodes);
    }

    @Nullable
    public String getExprContent() {
        return this.getSyntaxNode().getTextContent();
    }
    
    @NotNull
    public final SQLQueryExprType getValueType() {
        return type;
    }

    @Nullable
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return null;
    }

    @Nullable
    public SQLQueryResultColumn getColumnIfTrivialExpression() {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.dataContext;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.dataContext;
    }

    /**
     *  Propagate semantics context and establish relations through the query model
     */
    public final void propagateContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.dataContext = context;
        this.propagateContextImpl(context, statistics);
    }
    
    protected abstract void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics);
}

