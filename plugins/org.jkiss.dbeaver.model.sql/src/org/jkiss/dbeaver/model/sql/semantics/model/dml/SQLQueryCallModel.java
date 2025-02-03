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
package org.jkiss.dbeaver.model.sql.semantics.model.dml;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryObjectDataModel;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SQLQueryCallModel extends SQLQueryModelContent {

    @Nullable
    private final SQLQueryObjectDataModel object;

    @NotNull
    private final List<SQLQueryValueExpression> expressions;

    @Nullable
    private final SQLQueryLexicalScope nameScope;
    @Nullable
    private final SQLQueryLexicalScope tailScope;

    @Nullable
    private SQLQueryDataContext dataContext = null;

    private SQLQueryCallModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryObjectDataModel object,
        @NotNull List<SQLQueryValueExpression> expressions,
        @Nullable SQLQueryLexicalScope nameScope,
        @Nullable SQLQueryLexicalScope tailScope
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, expressions.toArray(SQLQueryNodeModel[]::new));
        this.object = object;
        this.expressions = expressions;
        this.nameScope = nameScope;
        this.tailScope = tailScope;

        if (object != null) {
            this.registerSubnode(object);
        }
        if (nameScope != null) {
            this.registerLexicalScope(nameScope);
        }
    }

    @Nullable
    public SQLQueryObjectDataModel getObject() {
        return object;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.dataContext;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.dataContext;
    }

    @NotNull
    public List<SQLQueryValueExpression> getExpressions() {
        return this.expressions;
    }

    @Override
    protected void applyContext(@NotNull SQLQueryDataContext dataContext, @NotNull SQLQueryRecognitionContext recognitionContext) {
        this.dataContext = dataContext;

        if (this.object != null) {
            this.object.propagateContext(dataContext, recognitionContext);
        }
        if (this.nameScope != null) {
            this.nameScope.setSymbolsOrigin(new SQLQuerySymbolOrigin.DbObjectFromContext(
                    dataContext, Set.of(RelationalObjectType.TYPE_PROCEDURE, RelationalObjectType.TYPE_PACKAGE), false
            ));
        }
        if (this.tailScope != null) {
            this.setTailOrigin(this.tailScope.getSymbolsOrigin());
        }
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitCallStatement(this, arg);
    }

    /**
     * Returns new instance of SQLQueryCallModel class by syntax tree
     */
    @NotNull
    public static SQLQueryModelContent recognize(
        @NotNull SQLQueryModelRecognizer recognizer,
        @NotNull STMTreeNode node,
        @NotNull DBSObjectType objectType
    ) {
        SQLQueryObjectDataModel procedure;
        SQLQueryLexicalScope nameScope;
        try (SQLQueryModelRecognizer.LexicalScopeHolder h = recognizer.openScope()) {
            nameScope = h.lexicalScope;
            STMTreeNode nameNode = node.findFirstChildOfName(STMKnownRuleNames.qualifiedName);
            SQLQueryQualifiedName name = nameNode == null ? null : recognizer.collectQualifiedName(nameNode);
            procedure = name == null
                ? null
                : new SQLQueryObjectDataModel(
                    nameNode,
                    name,
                    RelationalObjectType.TYPE_PROCEDURE,
                    Set.of(RelationalObjectType.TYPE_PACKAGE)
                );
        }

        SQLQueryLexicalScope tailScope;
        STMTreeNode paramsNode = node.findLastChildOfName(STMKnownRuleNames.callStatementParams);
        List<SQLQueryValueExpression> exprs;
        if (paramsNode == null) {
            tailScope = procedure == null ? nameScope : null;
            exprs = Collections.emptyList();
        } else {
            tailScope = null;
            exprs = paramsNode.findChildrenOfName(STMKnownRuleNames.anyValue)
                .stream()
                .map(recognizer::collectValueExpression)
                .toList();
        }

        STMTreeNode firstKeyword = node.findLastNonErrorChild();
        if (firstKeyword != null) {
            nameScope.setInterval(
                Interval.of(
                    firstKeyword.getRealInterval().b + 2, paramsNode == null ? Integer.MAX_VALUE : paramsNode.getRealInterval().a - 1
                )
            );
        }
        return new SQLQueryCallModel(node, procedure, exprs, nameScope, tailScope);
    }
}
