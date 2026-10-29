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
package org.jkiss.dbeaver.model.sql.semantics.model.statements;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;

/** Ordered execution of semantic children; syntax-only wrappers are transparent. */
public final class SQLQueryStatementSequence extends SQLQueryStatementModel {
    @NotNull
    private final List<SQLQueryStatementModel> statements;

    private SQLQueryStatementSequence(@NotNull STMTreeNode node, @NotNull List<SQLQueryStatementModel> statements) {
        super(node, statements.toArray(SQLQueryStatementModel[]::new));
        this.statements = List.copyOf(statements);
    }

    @NotNull
    public static SQLQueryStatementSequence recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        return new SQLQueryStatementSequence(node, recognizeChildren(recognizer, node));
    }

    @NotNull
    public List<SQLQueryStatementModel> getStatements() {
        return this.statements;
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext resolveStatementImpl(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        for (SQLQueryStatementModel statement : this.statements) {
            if (statistics.getMonitor().isCanceled()) {
                break;
            }
            context = statement.resolveStatement(context, statistics);
        }
        return context;
    }

    @Override
    public boolean tryResolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        for (SQLQueryStatementModel statement : this.statements) {
            if (statistics.getMonitor().isCanceled() || !statement.tryResolveValueRelations(this.valueContext(context), statistics)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitStatementSequence(this, arg);
    }
}
