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

import org.antlr.v4.runtime.Token;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryColumnSpec;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsLocalTableModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;

/** Cursor, table variable or temporary table declaration. */
public final class SQLQueryRelationDeclarationStatement extends SQLQueryStatementModel {
    @Nullable
    private final SQLQuerySymbolEntry name;
    @Nullable
    private final SQLDialect.LocalVariableKind kind;
    private final boolean temporary;
    @Nullable
    private final SQLQueryRowsLocalTableModel rows;
    @Nullable
    private final SQLQueryModelContent sql;

    private SQLQueryRelationDeclarationStatement(
        @NotNull STMTreeNode node, @Nullable SQLQuerySymbolEntry name,
        @Nullable SQLDialect.LocalVariableKind kind, boolean temporary, @Nullable SQLQueryRowsLocalTableModel rows,
        @Nullable SQLQueryModelContent sql
    ) {
        super(node, sql, rows);
        this.name = name;
        this.kind = kind;
        this.temporary = temporary;
        this.rows = rows;
        this.sql = sql;
    }

    @NotNull
    public static SQLQueryRelationDeclarationStatement recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        int kind = node.getNodeKindId();
        boolean cursor = kind == SQLStandardParser.RULE_cursorDeclaration;
        boolean create = kind == SQLStandardParser.RULE_createTableStatement;
        STMTreeNode tableName = node.findFirstChildOfName(STMKnownRuleNames.tableName);
        STMTreeNode cursorName = node.findFirstChildOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_cursorName]);
        boolean temporary = create
            ? isTemporaryCreate(node)
            : tableName != null || hasTemporaryModifier(node);
        SQLQuerySymbolEntry name;
        if (create) {
            SQLQueryComplexName qualified = temporary ? recognizer.collectTableName(node) : null;
            name = qualified == null || qualified.parts.isEmpty() ? null : qualified.parts.getLast();
        } else if (tableName != null) {
            SQLQueryComplexName qualified = recognizer.collectQualifiedName(tableName);
            name = qualified == null || qualified.parts.isEmpty() ? null : qualified.parts.getLast();
        } else {
            name = collectCursorName(recognizer, cursorName);
        }
        SQLQueryModelContent sql = null;
        if (cursor) {
            STMTreeNode query = node.findFirstChildOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_directSqlDataStatement]);
            if (query != null) {
                sql = SQLQueryEmbeddedStatement.recognizeSql(recognizer, query);
            }
        }
        SQLDialect.LocalVariableKind bindingKind = null;
        SQLQueryRowsLocalTableModel rows = null;
        if (name != null) {
            bindingKind = cursor ? SQLDialect.LocalVariableKind.CURSOR : SQLDialect.LocalVariableKind.ROWSET;
            if (!cursor) {
                List<SQLQueryColumnSpec> columns = new ArrayList<>();
                STMTreeNode elements = node.findFirstChildOfName(STMKnownRuleNames.tableElementList);
                if (elements != null) {
                    for (STMTreeNode element : elements.findChildrenOfName(STMKnownRuleNames.tableElement)) {
                        STMTreeNode column = element.findFirstChildOfName(STMKnownRuleNames.columnDefinition);
                        if (column != null) {
                            columns.add(SQLQueryColumnSpec.recognize(recognizer, column));
                        }
                    }
                }
                rows = new SQLQueryRowsLocalTableModel(node, name, columns);
            }
        }
        return new SQLQueryRelationDeclarationStatement(node, name, bindingKind, temporary, rows, sql);
    }

    static boolean isTemporaryCreate(@NotNull STMTreeNode node) {
        STMTreeNode head = node.findFirstChildOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_createTableHead]);
        STMTreeNode table = node.findFirstChildOfName(STMKnownRuleNames.tableName);
        return head != null && (hasTemporaryModifier(head) ||
            table != null && table.getTextContent().startsWith("#"));
    }

    private static boolean hasTemporaryModifier(@NotNull STMTreeNode node) {
        return node.findNonErrorChildren().stream().anyMatch(child -> child.getPayload() instanceof Token token &&
            (token.getType() == SQLStandardLexer.TEMP || token.getType() == SQLStandardLexer.TEMPORARY));
    }

    @Nullable
    static SQLQuerySymbolEntry collectCursorName(@NotNull SQLQueryModelRecognizer recognizer, @Nullable STMTreeNode node) {
        STMTreeNode identifier = node == null ? null : node.findFirstChildOfName(STMKnownRuleNames.identifier);
        return identifier == null ? null : recognizer.collectIdentifier(identifier, null);
    }

    @Nullable
    public SQLQuerySymbolEntry getName() {
        return this.name;
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext resolveStatementImpl(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.sql != null) {
            this.sql.resolveObjectAndRowsReferences(context, statistics);
        }
        if (this.name != null && this.kind != null) {
            if (this.rows != null) {
                this.rows.resolveRowSources(context, statistics);
            }
            SQLQueryLocalVariableInfo binding = new SQLQueryLocalVariableInfo(
                this.name, this.kind, SQLQueryExprType.UNKNOWN, this.rows, false);
            if (this.temporary) {
                context = context.withTemporaryRelation(binding);
                this.name.setDefinition(binding);
            } else {
                SQLQueryLocalVariablesContext next = context.getLocalVariables().withLocalVariable(binding);
                if (next == context.getLocalVariables()) {
                    statistics.appendError(this.name.getSyntaxNode(), "Local relation is already declared in this scope");
                    if (this.name.isNotClassified()) {
                        this.name.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
                    }
                } else {
                    this.name.setDefinition(binding);
                    context = context.withLocalVariables(next);
                }
            }
        }
        return context;
    }

    @Override
    public boolean tryResolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SQLQueryRowsDataContext input = this.valueContext(context);
        return (this.sql == null || this.sql.tryResolveValueRelations(input, statistics)) &&
            (this.rows == null || this.rows.tryResolveValueRelations(input, statistics));
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitRelationDeclarationStatement(this, arg);
    }
}
