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
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

/** Top-level routine, trigger or anonymous program; procedural steps belong to the statement hierarchy. */
public abstract class SQLQueryProgrammaticalStatementModel extends SQLQueryModelContent {
    protected SQLQueryProgrammaticalStatementModel(@NotNull STMTreeNode node, @Nullable SQLQueryNodeModel... children) {
        super(node.getRealInterval(), node, children);
    }

    @Nullable
    public static SQLQueryProgrammaticalStatementModel recognize(
        @NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node
    ) {
        STMTreeNode statement = node;
        if (statement.getNodeKindId() == SQLStandardParser.RULE_sqlQueryBody) {
            statement = statement.findFirstNonErrorChild();
        }
        if (statement != null && statement.getNodeKindId() == SQLStandardParser.RULE_sqlSchemaStatement) {
            statement = statement.findFirstNonErrorChild();
        }
        if (statement == null) {
            return null;
        }
        return switch (statement.getNodeKindId()) {
            case SQLStandardParser.RULE_createNamedObjectStatement, SQLStandardParser.RULE_alterNamedObjectStatement ->
                SQLQueryRoutineStatementModel.recognize(recognizer, statement);
            case SQLStandardParser.RULE_dropNamedObjectStatement, SQLStandardParser.RULE_dropProcedureStatement ->
                SQLQueryRoutineDropModel.recognize(recognizer, statement);
            case SQLStandardParser.RULE_createTriggerStatement, SQLStandardParser.RULE_alterTriggerStatement ->
                SQLQueryTriggerStatementModel.recognize(recognizer, statement);
            case SQLStandardParser.RULE_dropTriggerStatement -> SQLQueryTriggerDropModel.recognize(recognizer, statement);
            case SQLStandardParser.RULE_anonymousBlockStatement -> SQLQueryAnonymousBlockModel.recognize(recognizer, statement);
            default -> null;
        };
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @Nullable T arg) {
        return visitor.visitProgrammaticalStatement(this, arg);
    }
}
