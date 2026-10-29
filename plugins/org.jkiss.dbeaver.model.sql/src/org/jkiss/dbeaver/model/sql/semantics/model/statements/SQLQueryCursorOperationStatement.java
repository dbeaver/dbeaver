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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;

/** OPEN, FETCH, CLOSE and DEALLOCATE references to a declared cursor. */
public final class SQLQueryCursorOperationStatement extends SQLQueryStatementModel {
    @NotNull
    private final List<SQLQuerySymbolEntry> names;
    @Nullable
    private final SQLQueryModelContent sql;

    private SQLQueryCursorOperationStatement(
        @NotNull STMTreeNode node, @NotNull List<SQLQuerySymbolEntry> names, @Nullable SQLQueryModelContent sql
    ) {
        super(node, sql);
        this.names = List.copyOf(names);
        this.sql = sql;
    }

    @NotNull
    public static SQLQueryCursorOperationStatement recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        List<SQLQuerySymbolEntry> names = new ArrayList<>();
        for (STMTreeNode nameNode : node.findChildrenOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_cursorName])) {
            SQLQuerySymbolEntry name = SQLQueryRelationDeclarationStatement.collectCursorName(recognizer, nameNode);
            if (name != null) {
                names.add(name);
            }
        }
        STMTreeNode query = node.findFirstChildOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_directSqlDataStatement]);
        return new SQLQueryCursorOperationStatement(node, names,
            query == null ? null : SQLQueryEmbeddedStatement.recognizeSql(recognizer, query));
    }

    @NotNull
    public List<SQLQuerySymbolEntry> getNames() {
        return this.names;
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext resolveStatementImpl(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        for (int i = 0; i < this.names.size(); i++) {
            SQLQuerySymbolEntry name = this.names.get(i);
            SQLQueryLocalVariableInfo binding = context.getLocalVariables().findLocalVariable(name,
                i == 0 ? SQLDialect.LocalVariableCapability.CURSOR : SQLDialect.LocalVariableCapability.VALUE);
            if (binding != null && name.isNotClassified()) {
                name.setDefinition(binding.declaration());
            }
        }
        if (this.sql != null) {
            this.sql.resolveObjectAndRowsReferences(context, statistics);
        }
        return context;
    }

    @Override
    public boolean tryResolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        return this.sql == null || this.sql.tryResolveValueRelations(this.valueContext(context), statistics);
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitCursorOperationStatement(this, arg);
    }
}
