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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/**
 * Describes a member access to the element of the composite type
 */
public class SQLQueryValueMemberExpression extends SQLQueryValueExpression {

    private static final Log log = Log.getLog(SQLQueryValueMemberExpression.class);

    @NotNull
    private final SQLQueryValueExpression owner;
    @Nullable
    private final SQLQuerySymbolEntry identifier;

    public SQLQueryValueMemberExpression(
        @NotNull Interval range,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryValueExpression owner,
        @Nullable SQLQuerySymbolEntry identifier
    ) {
        super(range, syntaxNode, owner);
        this.owner = owner;
        this.identifier = identifier;
    }

    @NotNull
    public SQLQueryValueExpression getMemberOwner() {
        return this.owner;
    }

    @Nullable
    public SQLQuerySymbolEntry getMemberIdentifier() {
        return this.identifier;
    }

    @Nullable
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.identifier == null ? null : this.identifier.getSymbol();
    }
    
    @Override
    protected void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.owner.propagateContext(context, statistics);

        if (this.identifier == null) {
            this.type = SQLQueryExprType.UNKNOWN;
        } else if (this.identifier.isNotClassified()) {
            SQLQueryExprType type = tryResolveMemberReference(statistics, this.owner.getValueType(), this.identifier);
            this.type = type != null ? type : SQLQueryExprType.UNKNOWN;
        }
    }

    @Nullable
    public static SQLQueryExprType tryResolveMemberReference(
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull SQLQueryExprType valueType,
        @NotNull SQLQuerySymbolEntry identifier
    ) {
        SQLQueryExprType type;
        try {
            type = valueType.findNamedMemberType(statistics.getMonitor(), identifier.getName());

            if (type != null) {
                identifier.setDefinition(type.getDeclaratorDefinition());
            } else {
                identifier.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
                statistics.appendError(
                    identifier,
                    "Failed to resolve member reference " + identifier.getName() + " for " + valueType.getDisplayName()
                );
            }
        } catch (DBException e) {
            log.debug(e);
            statistics.appendError(
                identifier,
                "Failed to resolve member reference " + identifier.getName() + " for " + valueType.getDisplayName(),
                e
            );
            type = null;
        }
        return type;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueMemberReferenceExpr(this, arg);
    }

    @Override
    public String toString() {
        return "ValueMember[(" + this.owner.toString() + ")." + this.identifier.getName() + ":" + this.type.toString() + "]";
    }
}
