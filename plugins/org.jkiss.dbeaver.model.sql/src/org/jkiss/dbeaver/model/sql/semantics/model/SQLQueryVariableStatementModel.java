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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariablesSubset;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A sequence of item-local variable declarations and assignments followed by an optional regular query body.
 */
public class SQLQueryVariableStatementModel extends SQLQueryModelContent {

    @NotNull
    private final List<SQLQueryVariableClause> variableClauses;
    @Nullable
    private final SQLQueryModelContent body;
    @Nullable
    private SQLQueryRowsSourceContext resultingContext;

    public SQLQueryVariableStatementModel(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQueryVariableClause> variableClauses,
        @Nullable SQLQueryModelContent body
    ) {
        super(
            syntaxNode.getRealInterval(),
            syntaxNode,
            Stream.concat(
                variableClauses.stream(),
                Stream.of(body)
            ).toArray(SQLQueryNodeModel[]::new)
        );
        this.variableClauses = variableClauses;
        this.body = body;
    }

    @NotNull
    public List<SQLQueryVariableClause> getVariableClauses() {
        return this.variableClauses;
    }

    @Nullable
    public SQLQueryModelContent getBody() {
        return this.body;
    }

    @NotNull
    public SQLQueryVariablesSubset getResultingVariables() {
        if (this.resultingContext == null) {
            throw new IllegalStateException(
                "Unresolved resulting variables, relations should be established before accessing this property"
            );
        }
        return this.resultingContext.getVariablesSubset();
    }

    @Override
    public void resolveObjectAndRowsReferences(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        for (SQLQueryVariableClause clause : this.variableClauses) {
            clause.resolveRelations(context, statistics);
            context = context.registerScriptVariable(
                clause.getVariableName(),
                clause.getType(),
                clause.getKind(),
                clause.getInterval().a
            );
        }
        this.resultingContext = context;

        if (this.body != null) {
            this.body.resolveObjectAndRowsReferences(context, statistics);
        }
    }

    @Override
    public boolean tryResolveValueRelations(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        return this.body == null || this.body.tryResolveValueRelations(
            this.resultingContext == null ? context : this.resultingContext.makeEmptyTuple(),
            statistics
        );
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitVariableStatement(this, arg);
    }

    @NotNull
    public static SQLQueryVariableStatementModel recognize(
        @NotNull SQLQueryModelRecognizer recognizer,
        @NotNull STMTreeNode scopeNode,
        boolean forVariablesTracking
    ) {
        List<SQLQueryVariableClause> clauses = new ArrayList<>();

        for (STMTreeNode statementNode : scopeNode.findChildrenOfName(STMKnownRuleNames.variableStatement)) {
            STMTreeNode statement = statementNode.findFirstNonErrorChild();
            if (statement == null) {
                continue;
            }
            if (statement.getNodeKindId() == SQLStandardParser.RULE_variableDeclarationStatement) {
                for (STMTreeNode declarationNode : statement.findChildrenOfName(STMKnownRuleNames.variableDeclaration)) {
                    SQLQuerySymbolEntry name = collectVariableName(recognizer, declarationNode);
                    if (name != null) {
                        STMTreeNode dataTypeNode = declarationNode.findLastChildOfName(STMKnownRuleNames.dataType);
                        String typeName = dataTypeNode == null ? null : dataTypeNode.getTextContent();
                        SQLQueryValueExpression valueExpr = collectVariableValue(
                            recognizer, declarationNode.findFirstChildOfName(STMKnownRuleNames.variableInitializer)
                        );
                        clauses.add(new SQLQueryVariableClause(
                            declarationNode,
                            SQLQueryVariableInfo.OperationKind.DECLARATION,
                            name,
                            typeName,
                            valueExpr
                        ));
                    }
                }
            } else if (statement.getNodeKindId() == SQLStandardParser.RULE_variableAssignmentStatement) {
                for (STMTreeNode assignmentNode : statement.findChildrenOfName(STMKnownRuleNames.variableAssignment)) {
                    SQLQuerySymbolEntry name = collectVariableName(recognizer, assignmentNode);
                    if (name != null) {
                        SQLQueryValueExpression valueExpr = collectVariableValue(recognizer, assignmentNode);
                        clauses.add(new SQLQueryVariableClause(
                            assignmentNode,
                            SQLQueryVariableInfo.OperationKind.ASSIGNMENT,
                            name,
                            null,
                            valueExpr
                        ));
                    }
                }
            }
        }

        STMTreeNode bodyNode = scopeNode.findFirstChildOfName(STMKnownRuleNames.sqlQueryBody);
        SQLQueryModelContent body = forVariablesTracking || bodyNode == null ? null : recognizer.recognizeQueryBody(bodyNode);
        return new SQLQueryVariableStatementModel(scopeNode, clauses, body);
    }

    @Nullable
    private static SQLQuerySymbolEntry collectVariableName(
        @NotNull SQLQueryModelRecognizer recognizer,
        @NotNull STMTreeNode variableNameContainer
    ) {
        STMTreeNode nameNode = variableNameContainer.findFirstChildOfName(STMKnownRuleNames.variableName);
        if (nameNode != null) {
            STMTreeNode expressionNode = nameNode.findFirstChildOfName(STMKnownRuleNames.variableExpression);
            if (expressionNode != null) {
                return recognizer.collectTargetVariableName(expressionNode);
            }
            STMTreeNode identifierNode = nameNode.findFirstChildOfName(STMKnownRuleNames.identifier);
            if (identifierNode != null) {
                return recognizer.collectIdentifier(identifierNode, null);
            }
        }
        return null;
    }

    @Nullable
    private static SQLQueryValueExpression collectVariableValue(
        @NotNull SQLQueryModelRecognizer recognizer,
        @Nullable STMTreeNode container
    ) {
        STMTreeNode valueNode = container == null
            ? null
            : container.findFirstChildOfName(STMKnownRuleNames.valueExpression);
        return valueNode == null ? null : recognizer.collectValueExpression(valueNode, null);
    }
}
