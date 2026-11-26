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

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryLexicalScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMUtils;

import java.util.*;
import java.util.stream.Collectors;

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
    @Nullable
    private final SQLQueryLexicalScope targetScope;
    @Nullable
    private final SQLQueryLexicalScope sourceScope;

    private SQLQueryUpdateSetClauseModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQueryValueExpression> targets,
        @NotNull List<SQLQueryValueExpression> sources,
        @NotNull String contents,
        @Nullable SQLQueryLexicalScope targetScope,
        @Nullable SQLQueryLexicalScope sourceScope
    ) {
        super(
            syntaxNode.getRealInterval(),
            syntaxNode,
            STMUtils.combineLists(targets, sources).toArray(SQLQueryValueExpression[]::new)
        );
        this.targets = targets;
        this.sources = sources;
        this.contents = contents;
        this.targetScope = targetScope;
        this.sourceScope = sourceScope;
        // Clause-local scopes are found before the parent UPDATE's gaps at the same cursor position.
        if (targetScope != null) {
            this.registerLexicalScope(targetScope);
        }
        if (sourceScope != null) {
            this.registerLexicalScope(sourceScope);
        }
    }

    /** End includes the caret before the next assignment comma, but not the next statement clause. */
    @NotNull
    public static SQLQueryUpdateSetClauseModel recognize(
        @NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node, int end
    ) {
        STMTreeNode equals = node.findFirstChildOfName(SQLStandardLexer.VOCABULARY.getSymbolicName(SQLStandardLexer.EqualsOperator));
        STMTreeNode targetNode = node.findFirstNonErrorChild();
        SQLQueryLexicalScope targetScope;
        List<SQLQueryValueExpression> targets;
        if (targetNode == null || targetNode == equals || targetNode.getNodeKindId() == SQLStandardParser.RULE_anyUnexpected) {
            // With no structured target, the UPDATE model owns the target gap instead of this clause.
            targetScope = null;
            targets = Collections.emptyList();
        } else {
            try (SQLQueryModelRecognizer.LexicalScopeHolder holder = recognizer.openScope()) {
                targetScope = holder.lexicalScope;
                targets = switch (targetNode.getNodeKindId()) {
                    case SQLStandardParser.RULE_setTarget -> List.of(recognizer.collectTargetValueExpression(targetNode, targetScope));
                    case SQLStandardParser.RULE_setTargetList -> STMUtils.expandSubtree(
                        targetNode, Set.of(STMKnownRuleNames.setTargetList), Set.of(STMKnownRuleNames.setTarget)
                    ).stream().map(cn -> recognizer.collectTargetValueExpression(cn, targetScope)).collect(Collectors.toList());
                    default -> throw new UnsupportedOperationException(
                        "Set target list expected while facing with " + targetNode.getNodeName()
                    );
                };
            }
            // End at `=`.a - 1; without `=`, include the caret at the next comma's start.
            targetScope.setInterval(Interval.of(node.getRealInterval().a, equals == null ? end : equals.getRealInterval().a - 1));
        }

        SQLQueryLexicalScope sourceScope;
        List<SQLQueryValueExpression> sources;
        if (equals != null) {
            try (SQLQueryModelRecognizer.LexicalScopeHolder holder = recognizer.openScope()) {
                sourceScope = holder.lexicalScope;
                STMTreeNode updateSourceNode = node.findFirstChildOfName(STMKnownRuleNames.updateSource);
                sources = updateSourceNode == null ? Collections.emptyList() : STMUtils.expandSubtree(
                    updateSourceNode, Set.of(STMKnownRuleNames.updateSource), Set.of(STMKnownRuleNames.updateValue)
                ).stream()
                    .map(STMTreeNode::findFirstNonErrorChild)
                    .filter(Objects::nonNull)
                    .map(cn -> recognizer.collectValueExpression(cn, sourceScope))
                    .collect(Collectors.toList());
            }
            // `SET target=|source|,`: include carets just after `=` and immediately before the comma.
            sourceScope.setInterval(Interval.of(equals.getRealInterval().b + 1, end));
        } else {
            // With no `=` (for example, `SET a=1, b  |, c=3`), the parsed target stays target-side.
            sourceScope = null;
            sources = Collections.emptyList();
        }

        return new SQLQueryUpdateSetClauseModel(node, targets, sources, node.getTextContent(), targetScope, sourceScope);
    }

    @Nullable
    public SQLQueryLexicalScope getTargetScope() {
        return this.targetScope;
    }

    @Nullable
    public SQLQueryLexicalScope getSourceScope() {
        return this.sourceScope;
    }

    @Nullable
    public SQLQueryLexicalScope getLastScope() {
        // Completion after an unfinished clause follows the RHS only when `=` was present.
        return this.sourceScope != null ? this.sourceScope : this.targetScope;
    }

    @Override
    protected <R, T> R applyImpl(
        @NotNull SQLQueryNodeModelVisitor<T, R> visitor,
        @NotNull T arg
    ) {
        return visitor.visitTableStatementUpdateSetClause(this, arg);
    }

}
