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

import org.antlr.v4.runtime.Token;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectFeatures;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryComplexName;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueFlattenedExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueReferenceExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueVariableExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsProjectionModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQuerySelectionResultModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class SQLQuerySelectIntoModel extends SQLQueryRowsProjectionModel {

    private enum IntoTargetKind {
        NAME,
        BATCH_VARIABLE,
        PREFIXED_PARAMETER
    }

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

    @Override
    protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsSourceContext sourceContext = super.resolveRowSourcesImpl(context, statistics);
        resolveTargetsRowSources(this.targets, context, statistics);
        return sourceContext;
    }

    static void resolveTargetsRowSources(
        @Nullable SQLQuerySelectIntoTargetsList targetsList,
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SelectionTargetVisitor propagator = new SelectionTargetVisitor() {
            @Override
            public boolean visitRowsetTarget(RowsetSelectionTarget target) {
                target.table.resolveRowSources(context, statistics);
                return true;
            }

            @Override
            public boolean visitExpressionTarget(ValueSelectionTarget target) {
                target.expression.resolveRowSources(context, statistics);
                return true;
            }
        };

        if (targetsList != null) {
            List<SelectionTarget> targets = targetsList.getTargets();
            for (SelectionTarget target : targets) {
                if (target.getNode() != null) {
                    if (target instanceof IntoTargetReference reference) {
                        reference.resolveRowSources(context, statistics);
                    } else {
                        target.apply(propagator);
                    }
                }
            }
            targetsList.getTargetScope().setSymbolsOrigin(new SQLQuerySymbolOrigin.RowsSourceRef(context));
        }
    }

    @Override
    protected SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsDataContext sourceContext = super.resolveRowDataImpl(context, statistics);
        resolveTargetsRowData(this.targets, this.intoKeywordSyntaxNode, this.getSyntaxNode(), context, sourceContext, statistics);
        return sourceContext;
    }

    static void resolveTargetsRowData(
        @Nullable SQLQuerySelectIntoTargetsList targetsList, @Nullable STMTreeNode intoKeyword,
        @NotNull STMTreeNode syntaxNode, @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRowsDataContext sourceContext, @NotNull SQLQueryRecognitionContext statistics
    ) {
        SelectionTargetVisitor propagator = new SelectionTargetVisitor() {
            @Override
            public boolean visitRowsetTarget(RowsetSelectionTarget target) {
                return target.table.tryResolveValueRelations(context, statistics);
            }

            @Override
            public boolean visitExpressionTarget(ValueSelectionTarget target) {
                return target.expression.tryResolveValueRelations(context, statistics);
            }
        };

        if (targetsList != null) {

            List<SelectionTarget> targets = targetsList.getTargets();
            for (SelectionTarget target : targets) {
                if (target.getNode() != null) {
                    if (!target.apply(propagator)) {
                        return;
                    }
                }
            }

            SQLQueryRowsTableDataModel targetTable = null;
            if (targets.size() == 1) {
                if (targets.getFirst() instanceof RowsetSelectionTarget rowset) {
                    targetTable = rowset.table();
                } else if (targets.getFirst() instanceof IntoTargetReference reference) {
                    targetTable = reference.table;
                }
            }
            if (statistics.useRealMetadata() && targetTable != null) {
                SQLQueryRowsDataContext targetContext = targetTable.getRowsDataContext();
                int selectedColumns = sourceContext.getColumnsList().size();
                int expectedColumns = targetContext.getColumnsList().size();
                if (selectedColumns != expectedColumns && expectedColumns != 0) {
                    statistics.appendWarning(
                        Objects.requireNonNullElse(intoKeyword, syntaxNode),
                        "Selected result set has " + selectedColumns + " columns while target expected " + expectedColumns + " columns."
                    );
                }
            }
        }
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
        STMTreeNode intoClause = syntaxNode.findFirstChildOfName(STMKnownRuleNames.selectIntoClause);
        STMTreeNode intoKeywordNode = intoClause == null ? null : intoClause.findFirstChildOfName(STMKnownRuleNames.INTO_TERM);
        SQLQuerySelectIntoTargetsList targetsList = collectTargets(intoClause, recognizer);

        return SQLQueryRowsProjectionModel.recognize(
            syntaxNode,
            sourceModels,
            recognizer,
            (node, selectListScope, sourceModel, fromScope, filterExprs, filtersScope, resultModel, tailScope) ->
                new SQLQuerySelectIntoModel(node, intoKeywordNode, selectListScope, targetsList, sourceModel, fromScope, filterExprs, filtersScope, resultModel, tailScope)
        );
    }

    @Nullable
    static SQLQuerySelectIntoTargetsList collectTargets(
        @Nullable STMTreeNode intoClause, @NotNull SQLQueryModelRecognizer recognizer
    ) {
        if (intoClause == null) {
            return null;
        }
        STMTreeNode intoKeywordNode = intoClause.findFirstChildOfName(STMKnownRuleNames.INTO_TERM);
        STMTreeNode selectTargetList = intoClause.findFirstChildOfName(STMKnownRuleNames.selectTargetList);
        boolean proceduralStatement = isProceduralStatement(intoClause);

        try (SQLQueryModelRecognizer.LexicalScopeHolder targetScopeHolder = recognizer.openScope()) {
            SQLQueryLexicalScope targetScope = targetScopeHolder.lexicalScope;
            if (intoKeywordNode != null) {
                targetScope.registerSyntaxNode(intoKeywordNode);
            }
            if (selectTargetList != null) {
                List<SelectionTarget> targets = new LinkedList<>();
                SQLQuerySelectIntoTargetsList targetsList = new SQLQuerySelectIntoTargetsList(selectTargetList, targets, targetScope);

                targetScope.registerSyntaxNode(selectTargetList);
                for (STMTreeNode targetNode : selectTargetList.findChildrenOfName(STMKnownRuleNames.selectTargetItem)) {
                    STMTreeNode targetItemNode = targetNode.findFirstNonErrorChild();
                    if (targetItemNode != null) {
                        // Capture syntax and names now; the input context determines the target kind later.
                        if (targetItemNode.getNodeKindId() == SQLStandardParser.RULE_tableName) {
                            STMTreeNode qualified = targetItemNode.findFirstChildOfName(STMKnownRuleNames.qualifiedName);
                            STMTreeNode identifier = qualified == null ? null : qualified.findFirstChildOfName(STMKnownRuleNames.identifier);
                            STMTreeNode actual = identifier == null ? null : identifier.findFirstChildOfName(STMKnownRuleNames.actualIdentifier);
                            STMTreeNode term = actual == null ? null : actual.findFirstNonErrorChild();
                            boolean batchVariable = term != null && term.getPayload() instanceof Token token &&
                                token.getType() == SQLStandardLexer.BatchVariableName;
                            SQLQueryComplexName name;
                            if (batchVariable) {
                                SQLQuerySymbolEntry entry = recognizer.collectLocalVariableName(identifier);
                                name = entry == null ? null : new SQLQueryComplexName(targetItemNode, List.of(entry), 0, null);
                            } else {
                                name = recognizer.collectTableName(targetItemNode);
                            }
                            targets.add(new IntoTargetReference(
                                targetItemNode, name, proceduralStatement,
                                batchVariable ? IntoTargetKind.BATCH_VARIABLE : IntoTargetKind.NAME));
                        } else if (targetItemNode.getNodeKindId() == SQLStandardParser.RULE_parameterSpecification) {
                            STMTreeNode parameter = targetItemNode.findFirstChildOfName(STMKnownRuleNames.parameterName);
                            STMTreeNode identifier = parameter == null ? null : parameter.findFirstChildOfName(STMKnownRuleNames.identifier);
                            SQLQuerySymbolEntry name = identifier == null ? null : recognizer.collectLocalVariableName(identifier);
                            SQLQueryComplexName variable = name == null ? null : new SQLQueryComplexName(targetItemNode, List.of(name), 0, null);
                            targets.add(new IntoTargetReference(targetItemNode, variable, proceduralStatement,
                                IntoTargetKind.PREFIXED_PARAMETER));
                        } else {
                            targets.add(new ValueSelectionTarget(recognizer.collectValueExpression(targetItemNode, null)));
                        }
                    }
                }
                return targetsList;
            } else {
                return null;
            }
        }
    }

    private static boolean isProceduralStatement(@NotNull STMTreeNode node) {
        for (STMTreeNode parent = node.getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent.getNodeKindId() == SQLStandardParser.RULE_proceduralStatement) {
                return true;
            }
        }
        return false;
    }

    /** A bare INTO name can denote a rowset or a procedural value; resolve it using the input context. */
    private static final class IntoTargetReference extends SQLQueryNodeModel implements SelectionTarget {
        @Nullable
        private final SQLQueryComplexName name;
        private final boolean proceduralStatement;
        @NotNull
        private final IntoTargetKind kind;
        @Nullable
        private SQLQueryRowsTableDataModel table;
        @Nullable
        private SQLQueryValueExpression value;

        private IntoTargetReference(
            @NotNull STMTreeNode node, @Nullable SQLQueryComplexName name, boolean proceduralStatement,
            @NotNull IntoTargetKind kind
        ) {
            super(node.getRealInterval(), node);
            this.name = name;
            this.proceduralStatement = proceduralStatement;
            this.kind = kind;
        }

        private void resolveRowSources(
            @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
        ) {
            SQLDialect dialect = context.getDialect();
            SQLDialectFeatures.ProceduralSyntaxFamily family = dialect.getDialectFeatures().getProceduralSyntaxFamily();
            if (this.kind == IntoTargetKind.PREFIXED_PARAMETER) {
                this.value = this.proceduralStatement && family == SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL
                    ? new SQLQueryValueReferenceExpression(this.getSyntaxNode(), false, this.name)
                    : new SQLQueryValueFlattenedExpression(this.getSyntaxNode(), Collections.emptyList());
            } else if (this.kind == IntoTargetKind.BATCH_VARIABLE &&
                dialect.getDialectFeatures().getTrailingSelectIntoSupport() == SQLDialectFeatures.TrailingSelectIntoSupport.ANY) {
                SQLQuerySymbolEntry entry = this.name == null ? null : this.name.parts.getFirst();
                this.value = new SQLQueryValueVariableExpression(this.getSyntaxNode(), entry,
                    SQLQueryValueVariableExpression.VariableExpressionKind.BATCH_VARIABLE, this.getSyntaxNode().getTextContent());
            } else if (this.proceduralStatement && (family == SQLDialectFeatures.ProceduralSyntaxFamily.GENERIC ||
                family == SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL)) {
                this.value = new SQLQueryValueReferenceExpression(this.getSyntaxNode(), false, this.name);
            } else {
                this.table = new SQLQueryRowsTableDataModel(this.getSyntaxNode(), this.name, false);
                this.registerSubnode(this.table);
                this.table.resolveRowSources(context, statistics);
            }
            if (this.value != null) {
                this.registerSubnode(this.value);
                this.value.resolveRowSources(context, statistics);
            }
        }

        @NotNull
        @Override
        public SQLQueryNodeModel getNode() {
            return this;
        }

        @Override
        public boolean apply(@NotNull SelectionTargetVisitor visitor) {
            if (this.table != null) {
                return visitor.visitRowsetTarget(new RowsetSelectionTarget(this.table));
            }
            return this.value == null || visitor.visitExpressionTarget(new ValueSelectionTarget(this.value));
        }

        @Nullable
        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
            return this.table != null ? this.table.apply(visitor, arg)
                : this.value != null ? this.value.apply(visitor, arg) : null;
        }
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
    }

    public interface SelectionTargetVisitor {
        boolean visitRowsetTarget(RowsetSelectionTarget target);

        boolean visitExpressionTarget(ValueSelectionTarget target);
    }

    public interface SelectionTarget {
        SQLQueryNodeModel getNode();

        boolean apply(SelectionTargetVisitor visitor);
    }

    public record RowsetSelectionTarget(SQLQueryRowsTableDataModel table) implements SelectionTarget {
        @Override
        public SQLQueryNodeModel getNode() {
            return this.table;
        }

        @Override
        public boolean apply(SelectionTargetVisitor visitor) {
            return visitor.visitRowsetTarget(this);
        }
    }

    public record ValueSelectionTarget(SQLQueryValueExpression expression) implements SelectionTarget {
        @Override
        public SQLQueryNodeModel getNode() {
            return this.expression;
        }

        @Override
        public boolean apply(SelectionTargetVisitor visitor) {
            return visitor.visitExpressionTarget(this);
        }
    }

}
