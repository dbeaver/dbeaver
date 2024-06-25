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


import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
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
    @NotNull
    private final SQLQuerySymbolEntry identifier;

    public SQLQueryValueMemberExpression(
        @NotNull Interval range,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryValueExpression owner,
        @NotNull SQLQuerySymbolEntry identifier
    ) {
        super(range, syntaxNode, owner);
        this.owner = owner;
        this.identifier = identifier;
    }

    @NotNull
    public SQLQueryValueExpression getMemberOwner() {
        return this.owner;
    }

    @NotNull
    public SQLQuerySymbolEntry getMemberIdentifier() {
        return this.identifier;
    }

    @NotNull
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.identifier.getSymbol();
    }
    
    @Override
    protected void propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.owner.propagateContext(context, statistics);

        if (this.identifier.isNotClassified()) {
            SQLQueryExprType type;
            try {
                type = this.owner.getValueType().findNamedMemberType(statistics.getMonitor(), this.identifier.getName());

                if (type != null) {
                    this.identifier.setDefinition(type.getDeclaratorDefinition());
                } else {
                    this.identifier.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
                }
            } catch (DBException e) {
                log.debug(e);
                statistics.appendError(this.identifier, "Failed to resolve member reference", e);
                type = null;
            }

            this.type = type != null ? type : SQLQueryExprType.UNKNOWN;
        }
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
