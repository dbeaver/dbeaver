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
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableDropModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryCallModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryDeleteModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryInsertModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryUpdateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.List;

/** Existing SQL semantic model embedded in a procedural statement. */
public final class SQLQueryEmbeddedStatement extends SQLQueryStatementModel {
    @Nullable
    private final SQLQueryModelContent sql;

    private SQLQueryEmbeddedStatement(@NotNull STMTreeNode node, @Nullable SQLQueryModelContent sql) {
        super(node, sql);
        this.sql = sql;
    }

    @NotNull
    public static SQLQueryEmbeddedStatement recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        SQLQueryModelContent sql = switch (node.getNodeKindId()) {
            case SQLStandardParser.RULE_directSqlDataStatement -> recognizeSql(recognizer, node);
            case SQLStandardParser.RULE_firebirdSelectIntoStatement -> {
                STMTreeNode query = node.findFirstChildOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_directSqlDataStatement]);
                yield query == null ? null : recognizeSql(recognizer, query);
            }
            case SQLStandardParser.RULE_dropTableStatement -> SQLQueryTableDropModel.recognize(recognizer, node, false);
            case SQLStandardParser.RULE_createTableStatement -> SQLQueryTableCreateModel.recognize(recognizer, node);
            case SQLStandardParser.RULE_callStatement -> SQLQueryCallModel.recognize(recognizer, node, RelationalObjectType.TYPE_PROCEDURE);
            default -> null;
        };
        return new SQLQueryEmbeddedStatement(node, sql);
    }

    @Nullable
    static SQLQueryModelContent recognizeSql(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        STMTreeNode body = node.findLastNonErrorChild();
        if (body == null) {
            return null;
        }
        return switch (body.getNodeKindId()) {
            case SQLStandardParser.RULE_deleteStatement -> SQLQueryDeleteModel.recognize(recognizer, body);
            case SQLStandardParser.RULE_insertStatement -> SQLQueryInsertModel.recognize(recognizer, body);
            case SQLStandardParser.RULE_updateStatement -> SQLQueryUpdateModel.recognize(recognizer, body);
            default -> recognizer.collectQueryExpression(node);
        };
    }

    @Nullable
    public SQLQueryModelContent getSql() {
        return this.sql;
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext resolveStatementImpl(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.sql != null) {
            this.sql.resolveObjectAndRowsReferences(context, statistics);
        }
        if (this.sql instanceof SQLQueryTableDropModel drop && drop.getTables() != null) {
            List<String> names = drop.getTables().stream()
                .map(SQLQueryRowsTableDataModel::getName)
                .filter(name -> name != null && name.invalidPartsCount == 0 && name.parts.size() == 1 &&
                    name.parts.getFirst() != null)
                .map(name -> name.parts.getFirst().getName())
                .toList();
            context = context.withoutTemporaryRelations(names);
        }
        return context;
    }

    @Override
    public boolean tryResolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        return this.sql == null || this.sql.tryResolveValueRelations(this.valueContext(context), statistics);
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitEmbeddedStatement(this, arg);
    }
}
