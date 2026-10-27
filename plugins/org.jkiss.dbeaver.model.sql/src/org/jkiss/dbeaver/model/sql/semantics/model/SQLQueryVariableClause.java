/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.utils.CommonUtils;

public class SQLQueryVariableClause extends SQLQueryNodeModel {
    @NotNull
    private final SQLQueryVariableInfo.OperationKind kind;
    @NotNull
    private final SQLQuerySymbolEntry variableName;
    @Nullable
    private final String typeRefString;
    @Nullable
    private final SQLQueryValueExpression valueExpression;

    @Nullable
    private SQLQueryExprType type = null;

    protected SQLQueryVariableClause(
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryVariableInfo.OperationKind kind,
        @NotNull SQLQuerySymbolEntry variableName,
        @Nullable String typeRefString,
        @Nullable SQLQueryValueExpression valueExpression
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, valueExpression);
        this.kind = kind;
        this.variableName = variableName;
        this.typeRefString = typeRefString;
        this.valueExpression = valueExpression;
    }

    @NotNull
    public SQLQueryVariableInfo.OperationKind getKind() {
        return this.kind;
    }

    @NotNull
    public SQLQuerySymbolEntry getVariableName() {
        return this.variableName;
    }

    @NotNull
    public SQLQueryExprType getType() {
        if (this.type == null) {
            throw new IllegalStateException("Unresolved variable type, relations should be established before accessing this property");
        }
        return this.type;
    }

    @Nullable
    public SQLQueryValueExpression getValueExpression() {
        return this.valueExpression;
    }

    public void resolveRelations(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
        if (this.valueExpression != null) {
            this.valueExpression.resolveRowSources(context, statistics);
            this.valueExpression.tryResolveValueRelations(context.makeEmptyTuple(), statistics);
        }

        SQLQueryExprType type;
        if (!CommonUtils.isEmpty(this.typeRefString)) { // DECLARE typically have explicit type
            type = SQLQueryExprType.forExplicitTypeRef(this.typeRefString);
        } else { // otherwise look for existing definition for this name (redeclare or SET)
            SQLQueryVariableInfo def = context.resolveScriptVariable(this.variableName);
            if (def != null) {
                type = def.type();
                this.variableName.setDefinition(def);
            } else if (this.valueExpression != null) { // last resort
                type = this.valueExpression.getValueType();
            } else {
                type = SQLQueryExprType.UNKNOWN;
            }
        }
        if (this.variableName.isNotClassified()) {
            this.variableName.getSymbol().setSymbolClass(SQLQuerySymbolClass.SQL_BATCH_VARIABLE);
            this.variableName.setOrigin(new SQLQuerySymbolOrigin.ScriptVariableRef(context));
        }
        this.type = type;
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitVariableClause(this, arg);
    }
}
