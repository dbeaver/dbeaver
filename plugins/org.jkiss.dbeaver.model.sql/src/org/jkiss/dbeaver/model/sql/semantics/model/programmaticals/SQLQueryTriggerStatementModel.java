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
package org.jkiss.dbeaver.model.sql.semantics.model.programmaticals;

import org.antlr.v4.runtime.Token;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialectFeatures;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryObjectDataModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryStatementModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Trigger header table and transition rows are visible to the procedural body. */
public abstract class SQLQueryTriggerStatementModel extends SQLQueryProgrammaticalBodyStatementModel {
    @Nullable
    private final SQLQueryComplexName name;
    @Nullable
    private final SQLQueryObjectDataModel existingObject;
    @Nullable
    private final SQLQueryRowsTableDataModel table;
    @NotNull
    private final List<TransitionAlias> transitionAliases;
    @NotNull
    private final List<AffectedSubsetModel> transitionTables = new ArrayList<>();

    record TransitionAlias(@NotNull SQLQuerySymbolEntry name, boolean table) {
    }

    /** One transition-table name, with the affected table's columns but its own row-source identity. */
    public static final class AffectedSubsetModel extends SQLQueryRowsSourceModel {
        @NotNull
        private final SQLQueryRowsTableDataModel table;

        private AffectedSubsetModel(@NotNull STMTreeNode node, @NotNull SQLQueryRowsTableDataModel table) {
            super(node);
            this.table = table;
        }

        @NotNull
        @Override
        protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
            @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
        ) {
            return context.reset();
        }

        @NotNull
        @Override
        protected SQLQueryRowsDataContext resolveRowDataImpl(
            @NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics
        ) {
            List<SQLQueryResultColumn> columns = this.table.getRowsDataContext().getColumnsList().stream()
                .map(column -> column.withNewSource(this)).toList();
            return this.getRowsSources().makeTuple(this, columns, List.of());
        }

        @Nullable
        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @Nullable T arg) {
            return visitor.visitAffectedSubset(this, arg);
        }
    }

    protected SQLQueryTriggerStatementModel(
        @NotNull STMTreeNode node, @Nullable SQLQueryComplexName name,
        @Nullable SQLQueryObjectDataModel existingObject,
        @Nullable SQLQueryRowsTableDataModel table, @NotNull List<TransitionAlias> transitionAliases,
        @Nullable SQLQueryStatementModel body
    ) {
        super(node, body, existingObject, table);
        this.name = name;
        this.existingObject = existingObject;
        this.table = table;
        this.transitionAliases = List.copyOf(transitionAliases);
    }

    @NotNull
    public static SQLQueryTriggerStatementModel recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        boolean alter = node.getNodeKindId() == SQLStandardParser.RULE_alterTriggerStatement;
        STMTreeNode nameNode = node.findFirstChildOfName(STMKnownRuleNames.qualifiedName);
        SQLQueryComplexName name = nameNode == null ? null : recognizer.collectQualifiedName(nameNode);
        if (name != null && !alter) {
            name.parts.stream().filter(Objects::nonNull).forEach(p -> p.getSymbol().setSymbolClass(SQLQuerySymbolClass.OBJECT));
        }
        SQLQueryObjectDataModel existingObject = alter && name != null
            ? new SQLQueryObjectDataModel(nameNode, name, RelationalObjectType.TYPE_TRIGGER, Set.of()) : null;
        SQLQueryRowsTableDataModel table = null;
        List<TransitionAlias> aliases = new ArrayList<>();
        for (STMTreeNode part : node.findChildrenOfName(STMKnownRuleNames.triggerHeaderPart)) {
            STMTreeNode tableNode = part.findFirstChildOfName(STMKnownRuleNames.tableName);
            if (tableNode != null) {
                table = recognizer.collectTableReference(tableNode, false);
            }
            for (STMTreeNode transition : part.findChildrenOfName(STMKnownRuleNames.triggerTransition)) {
                STMTreeNode alias = transition.findFirstChildOfName(STMKnownRuleNames.identifier);
                if (alias != null) {
                    SQLQuerySymbolEntry entry = recognizer.collectIdentifier(alias, null);
                    if (entry != null) {
                        boolean tableAlias = transition.findNonErrorChildren().stream().anyMatch(child ->
                            child.getPayload() instanceof Token token && token.getType() == SQLStandardLexer.TABLE);
                        aliases.add(new TransitionAlias(entry, tableAlias));
                    }
                }
            }
        }
        STMTreeNode bodyNode = node.findFirstChildOfName(STMKnownRuleNames.triggerBody);
        SQLQueryStatementModel body = bodyNode == null ? null : SQLQueryStatementModel.recognize(recognizer, bodyNode);
        return alter
            ? new SQLQueryTriggerAlterModel(node, name, existingObject, table, aliases, body)
            : new SQLQueryTriggerCreateModel(node, name, table, aliases, body);
    }

    public boolean isAlter() {
        return this instanceof SQLQueryTriggerAlterModel;
    }

    @Nullable
    public SQLQueryComplexName getName() {
        return this.name;
    }

    @Nullable
    public SQLQueryObjectDataModel getExistingObject() {
        return this.existingObject;
    }

    @Nullable
    public SQLQueryRowsTableDataModel getTable() {
        return this.table;
    }

    @NotNull
    public List<SQLQuerySymbolEntry> getTransitionAliases() {
        return this.transitionAliases.stream().map(TransitionAlias::name).toList();
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @Nullable T arg) {
        return visitor.visitTriggerStatement(this, arg);
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext prepareBodyContext(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.existingObject != null) {
            this.existingObject.resolveRowSources(context, statistics);
        }
        if (this.table == null) {
            return context;
        }
        this.table.resolveRowSources(context, statistics);
        this.transitionTables.clear();
        SQLQueryLocalVariablesContext bindings = context.getLocalVariables();
        boolean tsql = context.getDialect().getDialectFeatures().getProceduralSyntaxFamily() ==
            SQLDialectFeatures.ProceduralSyntaxFamily.TSQL;
        for (String rawName : tsql ? List.of("INSERTED", "DELETED") : List.of("NEW", "OLD")) {
            String canonicalName = SQLUtils.identifierToCanonicalForm(context.getDialect(), rawName, false, false);
            SQLQuerySymbolEntry alias = new SQLQuerySymbolEntry(this.table.getSyntaxNode(), canonicalName, rawName, null);
            SQLDialectFeatures.LocalVariableKind kind = tsql
                ? SQLDialectFeatures.LocalVariableKind.ROWSET : SQLDialectFeatures.LocalVariableKind.ROW_REFERENCE;
            SQLQueryRowsSourceModel rows = this.table;
            if (tsql) {
                rows = new AffectedSubsetModel(alias.getSyntaxNode(), this.table);
            }
            SQLQueryLocalVariableInfo binding = new SQLQueryLocalVariableInfo(
                alias, kind, SQLQueryExprType.UNKNOWN, rows, false);
            SQLQueryLocalVariablesContext next = bindings.withLocalVariable(binding);
            if (next != bindings) {
                alias.setDefinition(binding);
                bindings = next;
                if (rows instanceof AffectedSubsetModel transitionTable) {
                    transitionTable.resolveRowSources(context, statistics);
                    this.transitionTables.add(transitionTable);
                }
            }
        }
        for (TransitionAlias transition : this.transitionAliases) {
            SQLQuerySymbolEntry alias = transition.name();
            SQLQueryRowsSourceModel rows = transition.table()
                ? new AffectedSubsetModel(alias.getSyntaxNode(), this.table) : this.table;
            SQLQueryLocalVariableInfo binding = new SQLQueryLocalVariableInfo(
                alias, transition.table() ? SQLDialectFeatures.LocalVariableKind.ROWSET
                    : SQLDialectFeatures.LocalVariableKind.ROW_REFERENCE, SQLQueryExprType.UNKNOWN, rows, false);
            SQLQueryLocalVariablesContext next = bindings.withLocalVariable(binding);
            if (next != bindings) {
                alias.setDefinition(binding);
                bindings = next;
                if (rows instanceof AffectedSubsetModel transitionTable) {
                    transitionTable.resolveRowSources(context, statistics);
                    this.transitionTables.add(transitionTable);
                }
            } else if (alias.isNotClassified()) {
                alias.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
            }
        }
        return context.withLocalVariables(bindings);
    }

    @Override
    public boolean tryResolveValueRelations(
        @NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        return (this.existingObject == null || this.existingObject.tryResolveValueRelations(context, statistics)) &&
            (this.table == null || this.table.tryResolveValueRelations(context, statistics)) &&
            this.transitionTables.stream().allMatch(table -> table.tryResolveValueRelations(context, statistics)) &&
            super.tryResolveValueRelations(context, statistics);
    }
}
