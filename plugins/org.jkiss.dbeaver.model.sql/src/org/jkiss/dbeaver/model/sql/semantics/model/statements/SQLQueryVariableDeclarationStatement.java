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
import org.jkiss.dbeaver.model.sql.SQLDialectFeatures;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;

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
        @Nullable
        private final SQLQueryValueExpression initializer;
        @Nullable
        private SQLQueryRowsSourceContext initializerContext;
        @Nullable
        private final SQLQueryModelRecognizer.TypeReference typeReference;
        @NotNull
        private final List<String> modifiers;

        @NotNull
        public List<String> getModifiers() {
            return this.modifiers;
        }

        public Declaration(@NotNull SQLQuerySymbolEntry name, boolean parameter, @Nullable String typeRefString) {
            this(name, parameter, typeRefString, null, null, List.of());
        }

        private Declaration(
            @NotNull SQLQuerySymbolEntry name, boolean parameter, @Nullable String typeRefString,
            @Nullable SQLQueryValueExpression initializer,
            @Nullable SQLQueryModelRecognizer.TypeReference typeReference,
            @NotNull List<String> modifiers
        ) {
            this.name = name;
            this.parameter = parameter;
            this.typeRefString = typeRefString;
            this.initializer = initializer;
            this.typeReference = typeReference;
            this.modifiers = List.copyOf(modifiers);
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
    private final boolean individuallyTyped;
    private final boolean sharedType;

    private SQLQueryVariableDeclarationStatement(
        @NotNull STMTreeNode node, @NotNull List<Declaration> declarations, boolean individuallyTyped, boolean sharedType
    ) {
        super(node, declarations.stream().map(d -> d.initializer).filter(i -> i != null).toArray(SQLQueryValueExpression[]::new));
        this.declarations = List.copyOf(declarations);
        this.individuallyTyped = individuallyTyped;
        this.sharedType = sharedType;
    }

    @NotNull
    public static SQLQueryVariableDeclarationStatement recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        List<Declaration> declarations = new ArrayList<>();
        boolean individuallyTyped = false;
        boolean sharedType = false;
        if (node.getNodeKindId() == SQLStandardParser.RULE_routineArgument) {
            STMTreeNode identifier = node.findFirstChildOfName(STMKnownRuleNames.identifier);
            STMTreeNode type = node.findFirstChildOfName(STMKnownRuleNames.dataType);
            if (identifier != null && type != null) {
                SQLQuerySymbolEntry name = recognizer.collectLocalVariableName(identifier);
                STMTreeNode defaults = node.findFirstChildOfName(STMKnownRuleNames.routineParameterDefault);
                STMTreeNode value = defaults == null ? null : defaults.findFirstChildOfName(STMKnownRuleNames.valueExpression);
                if (name != null) {
                    SQLQueryModelRecognizer.TypeReference typeReference = recognizer.collectTypeReference(node);
                    Declaration declaration = new Declaration(name, true, typeReference.spelling(),
                        value == null ? null : recognizer.collectValueExpression(value, null), typeReference,
                        recognizer.collectSignatureModifiers(node));
                    declarations.add(declaration);
                }
            }
        } else {
            List<STMTreeNode> typed = node.findChildrenOfName(STMKnownRuleNames.localTypedVariable);
            individuallyTyped = typed.size() > 1;
            for (STMTreeNode item : typed) {
                STMTreeNode identifier = item.findFirstChildOfName(STMKnownRuleNames.identifier);
                SQLQuerySymbolEntry name = identifier == null ? null : recognizer.collectLocalVariableName(identifier);
                if (name != null) {
                    STMTreeNode type = item.findFirstChildOfName(STMKnownRuleNames.localVariableType);
                    STMTreeNode value = item.findFirstChildOfName(STMKnownRuleNames.valueExpression);
                    SQLQueryModelRecognizer.TypeReference typeReference = type == null ? null : recognizer.collectTypeReference(type);
                    declarations.add(new Declaration(name, false, typeReference == null ? null : typeReference.spelling(),
                        value == null ? null : recognizer.collectValueExpression(value, null),
                        typeReference, List.of()));
                }
            }
            if (typed.isEmpty()) {
                List<STMTreeNode> names = node.findChildrenOfName(STMKnownRuleNames.identifier);
                sharedType = names.size() > 1;
                STMTreeNode type = node.findFirstChildOfName(STMKnownRuleNames.localVariableType);
                STMTreeNode value = node.findFirstChildOfName(STMKnownRuleNames.valueExpression);
                SQLQueryValueExpression initializer = value == null ? null : recognizer.collectValueExpression(value, null);
                SQLQueryModelRecognizer.TypeReference typeReference = type == null ? null : recognizer.collectTypeReference(type);
                for (STMTreeNode child : names) {
                    SQLQuerySymbolEntry name = recognizer.collectLocalVariableName(child);
                    if (name != null) {
                        // A shared DEFAULT expression belongs to the declaration group, not to every name.
                        declarations.add(new Declaration(name, false, typeReference == null ? null : typeReference.spelling(),
                            initializer, typeReference, List.of()));
                        initializer = null;
                    }
                }
            }
        }
        return new SQLQueryVariableDeclarationStatement(node, declarations, individuallyTyped, sharedType);
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
        SQLDialectFeatures.ProceduralSyntaxFamily family = context.getDialect().getDialectFeatures().getProceduralSyntaxFamily();
        boolean unsupportedTyped = this.individuallyTyped && family == SQLDialectFeatures.ProceduralSyntaxFamily.FIREBIRD_PSQL;
        boolean unsupportedShared = this.sharedType && family != SQLDialectFeatures.ProceduralSyntaxFamily.GENERIC;
        if (unsupportedTyped || unsupportedShared) {
            statistics.appendError(this.getSyntaxNode(), "Unsupported local variable declaration for this dialect");
            return context;
        }
        for (Declaration declaration : this.declarations) {
            if (declaration.initializer != null) {
                declaration.initializerContext = context;
                declaration.initializer.resolveRowSources(context, statistics);
            }
            String typeRefString = declaration.getTypeRefString();
            SQLQueryExprType type = declaration.typeReference != null
                ? SQLQuerySemanticUtils.resolveTypeReference(context, statistics, declaration.typeReference)
                : typeRefString == null
                ? SQLQueryExprType.UNKNOWN
                : SQLQuerySemanticUtils.resolveTypeReference(
                    context, statistics, declaration.getName().getSyntaxNode(), typeRefString);
            declaration.setType(type);
            SQLDialectFeatures.LocalVariableKind kind = "refcursor".equalsIgnoreCase(typeRefString)
                ? SQLDialectFeatures.LocalVariableKind.CURSOR_VALUE : SQLDialectFeatures.LocalVariableKind.VALUE;
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
        for (Declaration declaration : this.declarations) {
            if (declaration.initializer != null && declaration.initializerContext != null &&
                !declaration.initializer.tryResolveValueRelations(declaration.initializerContext.makeEmptyTuple(), statistics)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitVariableDeclarationStatement(this, arg);
    }
}
