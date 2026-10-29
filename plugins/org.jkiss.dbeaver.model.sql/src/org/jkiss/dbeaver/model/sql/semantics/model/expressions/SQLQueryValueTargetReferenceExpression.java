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
package org.jkiss.dbeaver.model.sql.semantics.model.expressions;


import org.antlr.v4.runtime.Token;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;


/**
 * Describes one target of an {@code UPDATE ... SET} clause, whether it assigns a column or a dialect-supported variable.
 */
public class SQLQueryValueTargetReferenceExpression extends SQLQueryValueExpression {
    @Nullable
    private final SQLQuerySymbolEntry columnName;
    @Nullable
    private SQLQueryResultColumn column = null;
    @Nullable
    private SQLQueryLocalVariableInfo localVariable = null;
    @Nullable
    private SQLQueryVariableInfo scriptVariable = null;

    public SQLQueryValueTargetReferenceExpression(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQuerySymbolEntry columnName
    ) {
        super(syntaxNode);
        this.columnName = columnName;
    }

    @Nullable
    @Override
    public SQLQuerySymbolClass getAssociatedSymbolClass() {
        return SQLQuerySemanticUtils.getIdentifierSymbolClass(this.columnName);
    }

    @Nullable
    public SQLQuerySymbolEntry getColumnName() {
        return this.columnName;
    }

    @Nullable
    @Override
    public SQLQuerySymbol getColumnNameIfTrivialExpression() {
        return this.columnName == null ? null : this.columnName.getSymbol();
    }

    @Nullable
    @Override
    public SQLQueryResultColumn getColumnIfTrivialExpression() {
        return this.column;
    }

    @Nullable
    public SQLQueryLocalVariableInfo getLocalVariable() {
        return this.localVariable;
    }

    @Nullable
    public SQLQueryVariableInfo getScriptVariable() {
        return this.scriptVariable;
    }

    @Override
    protected void resolveRowSourcesImpl(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
    }

    @NotNull
    @Override
    protected SQLQueryExprType resolveValueTypeImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryExprType type;
        SQLQueryResultColumn resultColumn;
        SQLQueryLocalVariableInfo resultLocalVariable;
        SQLQueryVariableInfo resultScriptVariable;

        if (this.columnName != null) {
            boolean isVariableTarget = context.getConnection().dialect.getProceduralSyntaxFamily() ==
                SQLDialect.ProceduralSyntaxFamily.TSQL &&
                this.columnName.getSyntaxNode().getPayload() instanceof Token token &&
                token.getType() == SQLStandardLexer.BatchVariableName;
            if (isVariableTarget) {
                SQLQueryRowsSourceContext rowsSources = context.getRowsSources();
                resultLocalVariable = rowsSources.getLocalVariables().findLocalVariable(
                    this.columnName, SQLDialect.LocalVariableCapability.VALUE);
                resultScriptVariable = resultLocalVariable == null ? rowsSources.resolveScriptVariable(this.columnName) : null;
                if (resultLocalVariable != null) {
                    this.columnName.setDefinition(resultLocalVariable.declaration());
                    type = resultLocalVariable.type();
                } else if (resultScriptVariable != null) {
                    this.columnName.setDefinition(resultScriptVariable);
                    type = resultScriptVariable.type();
                } else {
                    this.columnName.getSymbol().setSymbolClass(SQLQuerySymbolClass.SQL_BATCH_VARIABLE);
                    type = SQLQueryExprType.UNKNOWN;
                }
                this.columnName.setOrigin(new SQLQuerySymbolOrigin.ScriptVariableRef(rowsSources));
                resultColumn = null;
            } else {
                resultLocalVariable = null;
                resultScriptVariable = null;
                SQLQuerySymbolOrigin columnRefOrigin = new SQLQuerySymbolOrigin.ColumnNameFromRowsData(context);
                resultColumn = context.resolveColumn(statistics.getMonitor(), this.columnName.getName());

                if (resultColumn != null || !context.getRowsSources().hasUnresolvedSource()) {
                    SQLQuerySemanticUtils.propagateColumnDefinition(this.columnName, resultColumn, statistics, columnRefOrigin);
                } else {
                    this.columnName.setOrigin(columnRefOrigin);
                }
                type = resultColumn != null ? resultColumn.type : SQLQueryExprType.UNKNOWN;
            }
        } else {
            statistics.appendError(this.getSyntaxNode(), "Invalid column reference");
            resultColumn = null;
            resultLocalVariable = null;
            resultScriptVariable = null;
            type = this.type;
        }
        this.column = resultColumn;
        this.localVariable = resultLocalVariable;
        this.scriptVariable = resultScriptVariable;
        return type;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitValueTargetRefExpr(this, arg);
    }

    @NotNull
    @Override
    public String toString() {
        String columnName = this.columnName == null ? "<NULL>" : this.columnName.getName();
        String type = this.type == null ? "<NULL>" : this.type.toString();
        return "ColumnReference[" + columnName + ":" + type + "]";
    }
}
