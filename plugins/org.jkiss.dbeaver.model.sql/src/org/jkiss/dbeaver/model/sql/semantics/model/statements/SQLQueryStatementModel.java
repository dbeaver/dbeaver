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
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;

/** A statement transfers the bindings and temporary relations visible to its successor. */
public abstract class SQLQueryStatementModel extends SQLQueryModelContent {
    @Nullable
    private SQLQueryRowsSourceContext inputContext;

    protected SQLQueryStatementModel(@NotNull STMTreeNode node, @Nullable SQLQueryNodeModel... children) {
        super(node.getRealInterval(), node, children);
    }

    @NotNull
    public static SQLQueryStatementModel recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        return switch (node.getNodeKindId()) {
            case SQLStandardParser.RULE_proceduralBlock -> SQLQueryStatementBlock.recognize(recognizer, node);
            case SQLStandardParser.RULE_proceduralIf, SQLStandardParser.RULE_proceduralCase,
                SQLStandardParser.RULE_proceduralLoop, SQLStandardParser.RULE_proceduralRepeat,
                SQLStandardParser.RULE_proceduralTryCatch -> SQLQueryConditionalStatement.recognize(recognizer, node);
            case SQLStandardParser.RULE_routineArgument, SQLStandardParser.RULE_localVariableDeclaration,
                SQLStandardParser.RULE_firebirdVariableDeclaration ->
                SQLQueryVariableDeclarationStatement.recognize(recognizer, node);
            case SQLStandardParser.RULE_cursorDeclaration, SQLStandardParser.RULE_localRelationDeclaration ->
                SQLQueryRelationDeclarationStatement.recognize(recognizer, node);
            case SQLStandardParser.RULE_createTableStatement ->
                SQLQueryRelationDeclarationStatement.isTemporaryCreate(node)
                    ? SQLQueryRelationDeclarationStatement.recognize(recognizer, node)
                    : SQLQueryEmbeddedStatement.recognize(recognizer, node);
            case SQLStandardParser.RULE_cursorOperation -> SQLQueryCursorOperationStatement.recognize(recognizer, node);
            case SQLStandardParser.RULE_variableAssignmentStatement -> SQLQueryAssignmentStatement.recognize(recognizer, node);
            case SQLStandardParser.RULE_directSqlDataStatement, SQLStandardParser.RULE_dropTableStatement,
                SQLStandardParser.RULE_callStatement, SQLStandardParser.RULE_firebirdSelectIntoStatement ->
                SQLQueryEmbeddedStatement.recognize(recognizer, node);
            case SQLStandardParser.RULE_proceduralStatements, SQLStandardParser.RULE_proceduralDeclarations,
                SQLStandardParser.RULE_routineDeclarationsAndBlock, SQLStandardParser.RULE_firebirdBlock,
                SQLStandardParser.RULE_dollarBodyContents,
                SQLStandardParser.RULE_routineSignature -> SQLQueryStatementSequence.recognize(recognizer, node);
            case SQLStandardParser.RULE_proceduralStatement, SQLStandardParser.RULE_variableStatement,
                SQLStandardParser.RULE_sqlQueryBody,
                SQLStandardParser.RULE_sqlSchemaStatement, SQLStandardParser.RULE_routineDefinition,
                SQLStandardParser.RULE_routineBody, SQLStandardParser.RULE_routineBodyContent,
                SQLStandardParser.RULE_triggerBody, SQLStandardParser.RULE_dollarQuotedBody,
                SQLStandardParser.RULE_dollarBodyElement, SQLStandardParser.RULE_anonymousBlockStatement,
                SQLStandardParser.RULE_createNamedObjectStatement, SQLStandardParser.RULE_alterNamedObjectStatement,
                SQLStandardParser.RULE_createTriggerStatement, SQLStandardParser.RULE_alterTriggerStatement ->
                SQLQueryStatementSequence.recognize(recognizer, node);
            default -> new SQLQueryUnknownStatement(node);
        };
    }

    @NotNull
    static List<SQLQueryStatementModel> recognizeChildren(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        List<SQLQueryStatementModel> result = new ArrayList<>();
        for (STMTreeNode child : node.findNonErrorChildren()) {
            if (!(child.getPayload() instanceof Token)) {
                if (isSyntaxWrapper(child.getNodeKindId())) {
                    result.addAll(recognizeChildren(recognizer, child));
                } else {
                    SQLQueryStatementModel statement = recognize(recognizer, child);
                    if (!(statement instanceof SQLQueryUnknownStatement) ||
                        child.getNodeKindId() == SQLStandardParser.RULE_proceduralStatementTokens) {
                        result.add(statement);
                    }
                }
            }
        }
        return result;
    }

    private static boolean isSyntaxWrapper(int kind) {
        return switch (kind) {
            case SQLStandardParser.RULE_proceduralStatement, SQLStandardParser.RULE_routineDefinition,
                SQLStandardParser.RULE_routineBody, SQLStandardParser.RULE_routineBodyContent,
                SQLStandardParser.RULE_triggerBody, SQLStandardParser.RULE_dollarQuotedBody,
                SQLStandardParser.RULE_dollarBodyElement, SQLStandardParser.RULE_sqlQueryBody,
                SQLStandardParser.RULE_sqlSchemaStatement -> true;
            default -> false;
        };
    }

    /** Resolve once in source order, retaining the incoming context for the value phase. */
    @NotNull
    public final SQLQueryRowsSourceContext resolveStatement(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.inputContext = context;
        return this.resolveStatementImpl(context, statistics);
    }

    @NotNull
    protected abstract SQLQueryRowsSourceContext resolveStatementImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    );

    @Override
    public final void resolveObjectAndRowsReferences(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.resolveStatement(context, statistics);
    }

    @NotNull
    protected final SQLQueryRowsDataContext valueContext(@NotNull SQLQueryRowsDataContext fallback) {
        return this.inputContext == null ? fallback : this.inputContext.makeEmptyTuple();
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @Nullable T arg) {
        return visitor.visitStatement(this, arg);
    }
}
