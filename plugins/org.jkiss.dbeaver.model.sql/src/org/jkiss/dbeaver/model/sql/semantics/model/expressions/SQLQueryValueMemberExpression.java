/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
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
    @Nullable
    private final SQLQueryMemberAccessEntry memberAccessEntry;

    public SQLQueryValueMemberExpression(
        @NotNull Interval range,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryValueExpression owner,
        @Nullable SQLQuerySymbolEntry identifier,
        @Nullable SQLQueryMemberAccessEntry memberAccessEntry
    ) {
        super(range, syntaxNode, owner);
        this.owner = owner;
        this.identifier = identifier;
        this.memberAccessEntry = memberAccessEntry;
    }

    @Nullable
    @Override
    public SQLQuerySymbolClass getAssociatedSymbolClass() {
        return SQLQuerySemanticUtils.getIdentifierSymbolClass(this.identifier);
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
    protected void resolveRowSourcesImpl(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.owner.resolveRowSources(context, statistics);
    }

    @NotNull
    @Override
    protected SQLQueryExprType resolveValueTypeImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.resolveTypeImpl(statistics);
        return this.type;
    }

    private void resolveTypeImpl(@NotNull SQLQueryRecognitionContext statistics) {
        SQLQuerySymbolOrigin memberOrigin = new SQLQuerySymbolOrigin.MemberOfType(this.owner.getValueType());

        if (this.identifier == null) {
            this.type = SQLQueryExprType.UNKNOWN;
            if (this.memberAccessEntry != null) {
                this.memberAccessEntry.setOrigin(memberOrigin);
            }
        } else if (this.identifier.isNotClassified()) {
            SQLQueryExprType type = SQLQuerySemanticUtils.tryResolveMemberReference(statistics, this.owner.getValueType(), this.identifier, memberOrigin);
            this.type = type != null ? type : SQLQueryExprType.UNKNOWN;
        }
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueMemberReferenceExpr(this, arg);
    }

    @Override
    public String toString() {
        return "ValueMember[(" + this.owner + ")." +
            (this.identifier == null ? "<NULL>" : this.identifier.getName()) + ":" + this.type + "]";
    }
}
