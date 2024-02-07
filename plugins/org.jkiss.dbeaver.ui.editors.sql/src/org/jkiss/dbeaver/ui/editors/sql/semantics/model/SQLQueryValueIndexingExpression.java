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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryExprType;

public class SQLQueryValueIndexingExpression extends SQLQueryValueExpression {

    private static final Log log = Log.getLog(SQLQueryValueIndexingExpression.class);

    private final String content;
    
    private final SQLQueryValueExpression owner;
    private final boolean[] slicingDepthSpec;
    
    public SQLQueryValueIndexingExpression(
        @NotNull Interval region,
        @NotNull String content,
        @NotNull SQLQueryValueExpression owner,
        @NotNull boolean[] slicingDepthSpec
    ) {
        super(region);
        this.content = content;
        this.owner = owner;
        this.slicingDepthSpec = slicingDepthSpec;
    }

    @NotNull
    public String getExprContent() {
        return this.content;
    }
    
    @Override
    void propagateContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
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
