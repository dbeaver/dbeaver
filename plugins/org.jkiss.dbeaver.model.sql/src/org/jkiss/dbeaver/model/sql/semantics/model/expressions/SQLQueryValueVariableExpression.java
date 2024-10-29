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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Script variable. One of :var, ${var} or @var depending on the VariableExpressionKind
 */
public class SQLQueryValueVariableExpression extends SQLQueryValueExpression {
    /**
     * Kind of the script variable
     */
    public enum VariableExpressionKind {
        /**
         * The variable of kind @var
         */
        BATCH_VARIABLE(SQLQuerySymbolClass.SQL_BATCH_VARIABLE),
        /**
         * The variable of kind ${var}
         */
        CLIENT_VARIABLE(SQLQuerySymbolClass.DBEAVER_VARIABLE),
        /**
         * The variable of kind :var
         */
        CLIENT_PARAMETER(SQLQuerySymbolClass.DBEAVER_PARAMETER);
        
        public final SQLQuerySymbolClass symbolClass;

        VariableExpressionKind(@NotNull SQLQuerySymbolClass symbolClass) {
            this.symbolClass = symbolClass;
        }
    }

    @Nullable
    private final SQLQuerySymbolEntry name;
    @NotNull
    private final VariableExpressionKind kind;
    @NotNull
    private final String rawName;
    
    public SQLQueryValueVariableExpression(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQuerySymbolEntry name,
        @NotNull VariableExpressionKind kind,
        @NotNull String rawName
    ) {
        super(syntaxNode);
        this.name = name;
        this.kind = kind;
        this.rawName = rawName;
    }
    
    @Nullable
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.kind == VariableExpressionKind.BATCH_VARIABLE ? this.name.getSymbol() : null;
    }

    @NotNull
    public VariableExpressionKind getKind() {
        return this.kind;
    }

    @NotNull
    public String getRawName() {
        return this.rawName;
    }
    
    @Override
    protected void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.name != null && this.name.isNotClassified()) {
            this.name.getSymbol().setSymbolClass(this.kind.symbolClass);
        }
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueVariableExpr(this, arg);
    }

    @NotNull
    @Override
    public String toString() {
        return "Variable[" + this.kind + ":" + this.name + "]";
    }
}