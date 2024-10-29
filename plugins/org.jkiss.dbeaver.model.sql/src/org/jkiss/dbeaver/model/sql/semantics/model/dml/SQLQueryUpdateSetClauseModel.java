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
package org.jkiss.dbeaver.model.sql.semantics.model.dml;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMUtils;

import java.util.List;

/**
 * Describes SET clause of the UPDATE statement
 */
public class SQLQueryUpdateSetClauseModel extends SQLQueryNodeModel {
    @NotNull
    public final List<SQLQueryValueExpression> targets;
    @NotNull
    public final List<SQLQueryValueExpression> sources;
    @NotNull
    public final String contents;

    public SQLQueryUpdateSetClauseModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQueryValueExpression> targets,
        @NotNull List<SQLQueryValueExpression> sources,
        @NotNull String contents
    ) {
        super(
            syntaxNode.getRealInterval(),
            syntaxNode,
            STMUtils.combineLists(targets, sources).toArray(SQLQueryValueExpression[]::new)
        );
        this.targets = targets;
        this.sources = sources;
        this.contents = contents;
    }

    @Override
    protected <R, T> R applyImpl(
        @NotNull SQLQueryNodeModelVisitor<T, R> visitor,
        @NotNull T arg
    ) {
        return visitor.visitTableStatementUpdateSetClause(this, arg);
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.sources.get(0).getGivenDataContext();
    }

    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.sources.get(0).getResultDataContext();
    }
}
