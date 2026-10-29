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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryObjectDataModel;
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
    private final List<SQLQuerySymbolEntry> transitionAliases;

    protected SQLQueryTriggerStatementModel(
        @NotNull STMTreeNode node, @Nullable SQLQueryComplexName name,
        @Nullable SQLQueryObjectDataModel existingObject,
        @Nullable SQLQueryRowsTableDataModel table, @NotNull List<SQLQuerySymbolEntry> transitionAliases,
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
        List<SQLQuerySymbolEntry> aliases = new ArrayList<>();
        for (STMTreeNode part : node.findChildrenOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_triggerHeaderPart])) {
            STMTreeNode tableNode = part.findFirstChildOfName(STMKnownRuleNames.tableName);
            if (tableNode != null) {
                table = recognizer.collectTableReference(tableNode, false);
            }
            for (STMTreeNode transition : part.findChildrenOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_triggerTransition])) {
                STMTreeNode alias = transition.findFirstChildOfName(STMKnownRuleNames.identifier);
                if (alias != null) {
                    SQLQuerySymbolEntry entry = recognizer.collectIdentifier(alias, null);
                    if (entry != null) {
                        aliases.add(entry);
                    }
                }
            }
        }
        STMTreeNode bodyNode = node.findFirstChildOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_triggerBody]);
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
        return this.transitionAliases;
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
        SQLQueryLocalVariablesContext bindings = context.getLocalVariables();
        for (String rawName : List.of("NEW", "OLD", "INSERTED", "DELETED")) {
            String canonicalName = SQLUtils.identifierToCanonicalForm(context.getDialect(), rawName, false, false);
            SQLQuerySymbolEntry alias = new SQLQuerySymbolEntry(this.table.getSyntaxNode(), canonicalName, rawName, null);
            SQLQueryLocalVariableInfo binding = new SQLQueryLocalVariableInfo(
                alias, SQLDialect.LocalVariableKind.ROW_REFERENCE, SQLQueryExprType.UNKNOWN, this.table, false);
            SQLQueryLocalVariablesContext next = bindings.withLocalVariable(binding);
            if (next != bindings) {
                alias.setDefinition(binding);
                bindings = next;
            }
        }
        for (SQLQuerySymbolEntry alias : this.transitionAliases) {
            SQLQueryLocalVariableInfo binding = new SQLQueryLocalVariableInfo(
                alias, SQLDialect.LocalVariableKind.ROW_REFERENCE, SQLQueryExprType.UNKNOWN, this.table, false);
            SQLQueryLocalVariablesContext next = bindings.withLocalVariable(binding);
            if (next != bindings) {
                alias.setDefinition(binding);
                bindings = next;
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
            super.tryResolveValueRelations(context, statistics);
    }
}
