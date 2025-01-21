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
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsProjectionModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQuerySelectionResultModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class SQLQuerySelectIntoModel extends SQLQueryRowsProjectionModel {

    @Nullable
    private final STMTreeNode intoKeywordSyntaxNode;
    @Nullable
    private final SQLQuerySelectIntoTargetsList targets;

    public SQLQuerySelectIntoModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable STMTreeNode intoKeywordSyntaxNode,
        @NotNull SQLQueryLexicalScope selectListScope,
        @Nullable SQLQuerySelectIntoTargetsList targets,
        @NotNull SQLQueryRowsSourceModel fromSource,
        @Nullable SQLQueryLexicalScope fromScope,
        @NotNull FiltersData<SQLQueryValueExpression> filterExprs,
        @NotNull FiltersData<SQLQueryLexicalScope> filterScopes,
        @NotNull SQLQuerySelectionResultModel result,
        @Nullable SQLQueryLexicalScope tailScope
    ) {
        super(syntaxNode, selectListScope, fromSource, fromScope, filterExprs, filterScopes, result, tailScope);
        this.intoKeywordSyntaxNode = intoKeywordSyntaxNode;
        this.targets = targets;
    }

    @Nullable
    public SQLQuerySelectIntoTargetsList getTargets() {
        return this.targets;
    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLQueryDataContext sourceContext = super.propagateContextImpl(context, statistics);

        SelectionTargetVisitor propagator = new SelectionTargetVisitor() {
            @Override
            public void visitRowsetTarget(RowsetSelectionTarget target) {
                target.table.propagateContext(context, statistics);
            }

            @Override
            public void visitExpressionTarget(ValueSelectionTarget target) {
                target.expression.propagateContext(context, statistics);
            }
        };

        if (this.targets != null) {

            List<SQLQuerySelectIntoModel.SelectionTarget> targets = this.targets.getTargets();
            for (SelectionTarget target : targets) {
                if (target.getNode() != null) {
                    target.apply(propagator);
                }
            }

            if (statistics.useRealMetadata() && targets.size() == 1 && targets.get(0) instanceof RowsetSelectionTarget target) {
                SQLQueryDataContext targetContext = target.getNode().getResultDataContext();
                if (targetContext != null) {
                    int selectedColumns = sourceContext.getColumnsList().size();
                    int expectedColumns = targetContext.getColumnsList().size();
                    if (selectedColumns != expectedColumns && expectedColumns != 0) {
                        statistics.appendWarning(
                            Objects.requireNonNullElse(this.intoKeywordSyntaxNode, this.getSyntaxNode()),
                            "Selected result set has " + selectedColumns + " columns while target expected " + expectedColumns + " columns."
                        );
                    }
                }
            }

            this.targets.getTargetScope().setSymbolsOrigin(new SQLQuerySymbolOrigin.RowsetRefFromContext(context));
        }
        return context;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsProjectionInto(this, arg);
    }

    public static SQLQueryRowsSourceModel recognize(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQueryRowsSourceModel> sourceModels,
        @NotNull SQLQueryModelRecognizer recognizer
    ) {
        STMTreeNode intoKeywordNode = syntaxNode.findFirstChildOfName(STMKnownRuleNames.INTO_TERM);
        STMTreeNode selectTargetList = syntaxNode.findLastChildOfName(STMKnownRuleNames.selectTargetList);


        SQLQuerySelectIntoTargetsList targetsList;
        try (SQLQueryModelRecognizer.LexicalScopeHolder targetScopeHolder = recognizer.openScope()) {
            SQLQueryLexicalScope targetScope = targetScopeHolder.lexicalScope;
            if (intoKeywordNode != null) {
                targetScope.registerSyntaxNode(intoKeywordNode);
            }
            if (selectTargetList != null) {
                List<SelectionTarget> targets = new LinkedList<>();
                targetsList = new SQLQuerySelectIntoTargetsList(selectTargetList, targets, targetScope);

                targetScope.registerSyntaxNode(selectTargetList);
                for (STMTreeNode targetNode : selectTargetList.findChildrenOfName(STMKnownRuleNames.selectTargetItem)) {
                    STMTreeNode targetItemNode = targetNode.findFirstNonErrorChild();
                    if (targetItemNode != null) {
                        targets.add(
                            switch (targetItemNode.getNodeKindId()) {
                                case SQLStandardParser.RULE_tableName ->
                                    new RowsetSelectionTarget(recognizer.collectTableReference(targetItemNode, false));
                                default -> new ValueSelectionTarget(recognizer.collectValueExpression(targetItemNode));
                            }
                        );
                    }
                }
            } else {
                targetsList = null;
            }
        }

        return SQLQueryRowsProjectionModel.recognize(
            syntaxNode,
            sourceModels,
            recognizer,
            (node, selectListScope, sourceModel, fromScope, filterExprs, filtersScope, resultModel, tailScope) ->
                new SQLQuerySelectIntoModel(node, intoKeywordNode, selectListScope, targetsList, sourceModel, fromScope, filterExprs, filtersScope, resultModel, tailScope)
        );
    }

    public static class SQLQuerySelectIntoTargetsList extends SQLQueryNodeModel {

        @NotNull
        private final List<SelectionTarget> targets;
        @NotNull
        private final SQLQueryLexicalScope targetScope;

        public SQLQuerySelectIntoTargetsList(
            @NotNull STMTreeNode syntaxNode,
            @NotNull List<SelectionTarget> targets,
            @NotNull SQLQueryLexicalScope targetScope
        ) {
            super(syntaxNode.getRealInterval(), syntaxNode);

            this.targets = targets;
            this.targetScope = targetScope;

            targets.stream().map(SelectionTarget::getNode).filter(Objects::nonNull).forEach(this::registerSubnode);
            this.registerLexicalScope(targetScope);
        }


        @NotNull
        public List<SelectionTarget> getTargets() {
            return this.targets;
        }

        public List<SQLQueryNodeModel> getTargetNodes() {
            return this.targets.stream().map(SelectionTarget::getNode).filter(Objects::nonNull).toList();
        }

        @NotNull
        public SQLQueryLexicalScope getTargetScope() {
            return this.targetScope;
        }

        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
            return visitor.visitRowsProjectionIntoTargetsList(this, arg);
        }

        @Nullable
        @Override
        public SQLQueryDataContext getGivenDataContext() {
            return this.targetScope.getSymbolsOrigin().getDataContext();
        }

        @Nullable
        @Override
        public SQLQueryDataContext getResultDataContext() {
            return this.targetScope.getSymbolsOrigin().getDataContext();
        }
    }

    public interface SelectionTargetVisitor {
        void visitRowsetTarget(RowsetSelectionTarget target);

        void visitExpressionTarget(ValueSelectionTarget target);
    }

    public interface SelectionTarget {
        SQLQueryNodeModel getNode();

        void apply(SelectionTargetVisitor visitor);
    }

    public record RowsetSelectionTarget(SQLQueryRowsTableDataModel table) implements SelectionTarget {
        @Override
        public SQLQueryNodeModel getNode() {
            return this.table;
        }

        @Override
        public void apply(SelectionTargetVisitor visitor) {
            visitor.visitRowsetTarget(this);
        }
    }

    public record ValueSelectionTarget(SQLQueryValueExpression expression) implements SelectionTarget {
        @Override
        public SQLQueryNodeModel getNode() {
            return this.expression;
        }

        @Override
        public void apply(SelectionTargetVisitor visitor) {
            visitor.visitExpressionTarget(this);
        }
    }

}
