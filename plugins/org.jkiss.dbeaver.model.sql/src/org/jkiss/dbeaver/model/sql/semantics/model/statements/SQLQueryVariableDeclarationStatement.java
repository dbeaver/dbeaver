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
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Declares one or more local variables, or a routine parameter with its explicit type. */
public final class SQLQueryVariableDeclarationStatement extends SQLQueryStatementModel {
    public static final class Declaration {
        @NotNull
        private final SQLQuerySymbolEntry name;
        private final boolean parameter;
        @Nullable
        private final String typeRefString;
        @Nullable
        private SQLQueryExprType type;

        public Declaration(@NotNull SQLQuerySymbolEntry name, boolean parameter, @Nullable String typeRefString) {
            this.name = name;
            this.parameter = parameter;
            this.typeRefString = typeRefString;
        }

        @NotNull
        public SQLQuerySymbolEntry getName() {
            return this.name;
        }

        public boolean isParameter() {
            return this.parameter;
        }

        @Nullable
        public String getTypeRefString() {
            return this.typeRefString;
        }

        @NotNull
        public SQLQueryExprType getType() {
            if (this.type == null) {
                throw new IllegalStateException("Local variable type has not been resolved");
            }
            return this.type;
        }

        private void setType(@NotNull SQLQueryExprType type) {
            if (this.type != null) {
                throw new IllegalStateException("Local variable type has already been resolved");
            }
            this.type = type;
        }
    }

    @NotNull
    private final List<Declaration> declarations;
    @Nullable
    private final SQLQueryValueExpression initializer;

    private SQLQueryVariableDeclarationStatement(
        @NotNull STMTreeNode node,
        @NotNull List<Declaration> declarations,
        @Nullable SQLQueryValueExpression initializer
    ) {
        super(node, initializer);
        this.declarations = List.copyOf(declarations);
        this.initializer = initializer;
    }

    @NotNull
    public static SQLQueryVariableDeclarationStatement recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        List<Declaration> declarations = new ArrayList<>();
        SQLQueryValueExpression initializer = null;
        if (node.getNodeKindId() == SQLStandardParser.RULE_routineArgument) {
            List<STMTreeNode> terms = node.findNonErrorChildren();
            int index = 0;
            if (!terms.isEmpty()) {
                String first = terms.getFirst().getTextContent().toUpperCase(Locale.ROOT);
                if (first.equals("IN") || first.equals("OUT") || first.equals("INOUT")) {
                    index++;
                }
            }
            if (index + 1 < terms.size() && terms.get(index).getPayload() instanceof Token) {
                SQLQuerySymbolEntry name = recognizer.collectUnstructuredName(terms.get(index));
                if (name != null) {
                    StringBuilder typeName = new StringBuilder(terms.get(index + 1).getTextContent());
                    for (int partIndex = index + 2; partIndex + 1 < terms.size() &&
                        ".".equals(terms.get(partIndex).getTextContent()); partIndex += 2) {
                        typeName.append('.').append(terms.get(partIndex + 1).getTextContent());
                    }
                    declarations.add(new Declaration(name, true, typeName.toString()));
                }
            }
        } else {
            STMTreeNode type = node.findFirstChildOfName(STMKnownRuleNames.dataType);
            for (STMTreeNode child : node.findChildrenOfName(SQLStandardParser.ruleNames[SQLStandardParser.RULE_cursorName])) {
                SQLQuerySymbolEntry name = SQLQueryRelationDeclarationStatement.collectCursorName(recognizer, child);
                if (name != null) {
                    declarations.add(new Declaration(name, false, type == null ? null : type.getTextContent()));
                }
            }
            STMTreeNode value = node.findFirstChildOfName(STMKnownRuleNames.valueExpression);
            if (value != null) {
                initializer = recognizer.collectValueExpression(value, null);
            }
        }
        return new SQLQueryVariableDeclarationStatement(node, declarations, initializer);
    }

    @NotNull
    public List<Declaration> getDeclarations() {
        return this.declarations;
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext resolveStatementImpl(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.initializer != null) {
            this.initializer.resolveRowSources(context, statistics);
        }
        for (Declaration declaration : this.declarations) {
            String typeRefString = declaration.getTypeRefString();
            SQLQueryExprType type = typeRefString == null
                ? SQLQueryExprType.UNKNOWN
                : SQLQuerySemanticUtils.resolveTypeReference(
                    context, statistics, declaration.getName().getSyntaxNode(), typeRefString);
            declaration.setType(type);
            SQLDialect.LocalVariableKind kind = "refcursor".equalsIgnoreCase(typeRefString)
                ? SQLDialect.LocalVariableKind.CURSOR_VALUE : SQLDialect.LocalVariableKind.VALUE;
            SQLQueryLocalVariableInfo binding = new SQLQueryLocalVariableInfo(
                declaration.getName(), kind, declaration.getType(), null, declaration.isParameter());
            SQLQueryLocalVariablesContext next = context.getLocalVariables().withLocalVariable(binding);
            if (next == context.getLocalVariables()) {
                statistics.appendError(declaration.getName().getSyntaxNode(), "Local variable is already declared in this scope");
                if (declaration.getName().isNotClassified()) {
                    declaration.getName().getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
                }
            } else {
                declaration.getName().setDefinition(binding);
                context = context.withLocalVariables(next);
            }
        }
        return context;
    }

    @Override
    public boolean tryResolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        return this.initializer == null || this.initializer.tryResolveValueRelations(this.valueContext(context), statistics);
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitVariableDeclarationStatement(this, arg);
    }
}
