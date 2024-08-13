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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Describes arrays
 */
public class SQLQueryValueIndexingExpression extends SQLQueryValueExpression {

    private static final Log log = Log.getLog(SQLQueryValueIndexingExpression.class);

    @NotNull
    private final SQLQueryValueExpression owner;
    @NotNull
    private final boolean[] slicingDepthSpec;
    
    public SQLQueryValueIndexingExpression(
        @NotNull Interval region,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryValueExpression owner,
        @NotNull boolean[] slicingDepthSpec
    ) {
        super(region, syntaxNode, owner);
        this.owner = owner;
        this.slicingDepthSpec = slicingDepthSpec;
    }
    
    @Override
    protected void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.owner.propagateContext(context, statistics);
        
        SQLQueryExprType type = this.owner.getValueType();
        try {
            type = type.findIndexedItemType(statistics.getMonitor(), slicingDepthSpec.length, slicingDepthSpec);
        } catch (DBException e) {
            // TODO statistics.appendError(null, null);
            log.debug(e);
            type = null;
        }
        
        this.type = type != null ? type : SQLQueryExprType.UNKNOWN;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueIndexingExpr(this, arg);
    }
    
}
