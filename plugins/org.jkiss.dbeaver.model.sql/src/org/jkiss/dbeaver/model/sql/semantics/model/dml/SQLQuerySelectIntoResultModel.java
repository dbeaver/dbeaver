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
package org.jkiss.dbeaver.model.sql.semantics.model.dml;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Objects;

/** INTO applies to the complete SELECT result, including any set operation. */
public final class SQLQuerySelectIntoResultModel extends SQLQueryRowsSourceModel {
    @NotNull
    private final SQLQueryRowsSourceModel resultQuery;
    @NotNull
    private final SQLQuerySelectIntoModel.SQLQuerySelectIntoTargetsList targets;
    @NotNull
    private final STMTreeNode intoKeyword;

    private SQLQuerySelectIntoResultModel(
        @NotNull STMTreeNode node, @NotNull SQLQueryRowsSourceModel resultQuery,
        @NotNull STMTreeNode intoKeyword, @NotNull SQLQuerySelectIntoModel.SQLQuerySelectIntoTargetsList targets
    ) {
        super(node, resultQuery, targets);
        this.resultQuery = resultQuery;
        this.intoKeyword = intoKeyword;
        this.targets = targets;
    }

    @NotNull
    public SQLQueryRowsSourceModel getResultQuery() {
        return this.resultQuery;
    }

    @NotNull
    public SQLQuerySelectIntoModel.SQLQuerySelectIntoTargetsList getTargets() {
        return this.targets;
    }

    @NotNull
    public static SQLQuerySelectIntoResultModel recognize(
        @NotNull STMTreeNode statement, @NotNull SQLQueryRowsSourceModel result,
        @NotNull SQLQueryModelRecognizer recognizer
    ) {
        STMTreeNode clause = Objects.requireNonNull(statement.findFirstChildOfName(STMKnownRuleNames.selectIntoClause));
        SQLQuerySelectIntoModel.SQLQuerySelectIntoTargetsList targets = Objects.requireNonNull(
            SQLQuerySelectIntoModel.collectTargets(clause, recognizer));
        STMTreeNode keyword = Objects.requireNonNull(clause.findFirstChildOfName(STMKnownRuleNames.INTO_TERM));
        return new SQLQuerySelectIntoResultModel(statement, result, keyword, targets);
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsSourceContext source = this.resultQuery.resolveRowSources(context, statistics);
        SQLQuerySelectIntoModel.resolveTargetsRowSources(this.targets, context, statistics);
        return source;
    }

    @NotNull
    @Override
    protected SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsDataContext result = this.resultQuery.getRowsDataContext();
        SQLQuerySelectIntoModel.resolveTargetsRowData(
            this.targets, this.intoKeyword, this.getSyntaxNode(), context, result, statistics);
        return result;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitRowsSelectIntoResult(this, arg);
    }
}
