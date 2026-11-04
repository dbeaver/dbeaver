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

import org.antlr.v4.runtime.Token;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialectFeatures;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryComplexName;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySemanticUtils;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariablesContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryObjectDataModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryStatementModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryVariableDeclarationStatement;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.*;

/** CREATE or ALTER FUNCTION/PROCEDURE. Parameters are declared before resolving the body. */
public abstract class SQLQueryRoutineStatementModel extends SQLQueryProgrammaticalBodyStatementModel {
    /** A signature position is either a named declaration or an unnamed type reference. */
    public record SignatureArgument(
        @Nullable SQLQueryVariableDeclarationStatement declaration,
        @Nullable SQLQueryModelRecognizer.TypeReference typeReference,
        @Nullable SQLQueryExprType resolvedType
    ) {
        public SignatureArgument {
            if ((declaration == null) == (typeReference == null)) {
                throw new IllegalArgumentException("A signature argument must have exactly one declaration or type reference");
            }
        }
    }

    @Nullable
    private final SQLQueryComplexName name;
    @Nullable
    private final SQLQueryObjectDataModel existingObject;
    @NotNull
    private List<SignatureArgument> arguments;
    @NotNull
    private SQLQueryRoutineReturn returnSpec;

    protected SQLQueryRoutineStatementModel(
        @NotNull STMTreeNode node, @Nullable SQLQueryComplexName name,
        @Nullable SQLQueryObjectDataModel existingObject,
        @NotNull List<SignatureArgument> arguments,
        @NotNull SQLQueryRoutineReturn returnSpec,
        @Nullable SQLQueryStatementModel body
    ) {
        super(node, body, existingObject);
        arguments.stream().map(SignatureArgument::declaration).filter(Objects::nonNull).forEach(this::registerSubnode);
        this.name = name;
        this.existingObject = existingObject;
        this.arguments = List.copyOf(arguments);
        this.returnSpec = returnSpec;
    }

    @Nullable
    public static SQLQueryRoutineStatementModel recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        boolean alter = node.getNodeKindId() == SQLStandardParser.RULE_alterNamedObjectStatement;
        STMTreeNode kind = node.findFirstChildOfName(
            alter ? STMKnownRuleNames.alterObjectKind : STMKnownRuleNames.createObjectKind);
        if (kind == null || kind.findNonErrorChildren().stream().noneMatch(child ->
            STMKnownRuleNames.FUNCTION_TERM.equals(child.getNodeName())
                || STMKnownRuleNames.PROCEDURE_TERM.equals(child.getNodeName())
                || STMKnownRuleNames.PROC_TERM.equals(child.getNodeName()))) {
            return null;
        }
        STMTreeNode nameNode = node.findFirstChildOfName(STMKnownRuleNames.qualifiedName);
        SQLQueryComplexName name = nameNode == null ? null : recognizer.collectQualifiedName(nameNode);
        if (name != null && !alter) {
            name.parts.stream().filter(Objects::nonNull).forEach(p -> p.getSymbol().setSymbolClass(SQLQuerySymbolClass.OBJECT));
        }
        final SQLQueryObjectDataModel existingObject = alter && name != null
            ? new SQLQueryObjectDataModel(nameNode, name, RelationalObjectType.TYPE_PROCEDURE, Set.of()) : null;
        STMTreeNode definition = node.findFirstChildOfName(STMKnownRuleNames.routineDefinition);
        STMTreeNode signature = definition == null ? null : definition.findFirstChildOfName(
            STMKnownRuleNames.routineSignature);
        if (signature == null) {
            signature = node.findFirstChildOfName(STMKnownRuleNames.routineSignature);
        }
        List<SignatureArgument> arguments = new ArrayList<>();
        if (signature != null) {
            for (STMTreeNode argument : signature.findChildrenOfName(STMKnownRuleNames.routineArgument)) {
                if (argument.findFirstChildOfName(STMKnownRuleNames.dataType) != null) {
                    if (argument.findFirstChildOfName(STMKnownRuleNames.identifier) != null) {
                        arguments.add(new SignatureArgument(
                            (SQLQueryVariableDeclarationStatement) SQLQueryStatementModel.recognize(recognizer, argument), null, null));
                    } else {
                        arguments.add(new SignatureArgument(null, recognizer.collectTypeReference(argument), null));
                    }
                }
            }
        }
        SQLQueryRoutineReturn returnSpec = new SQLQueryRoutineReturn.None();
        if (definition != null) {
            STMTreeNode preamble = definition.findFirstChildOfName(STMKnownRuleNames.routinePreamble);
            STMTreeNode returnClause = preamble == null ? null : preamble.findFirstChildOfName(STMKnownRuleNames.routineReturnClause);
            if (returnClause != null) {
                STMTreeNode returnType = returnClause.findFirstChildOfName(STMKnownRuleNames.dataType);
                STMTreeNode resultList = returnClause.findFirstChildOfName(STMKnownRuleNames.routineResultList);
                if (returnType != null) {
                    returnSpec = new SQLQueryRoutineReturn.Scalar(recognizer.collectTypeReference(returnType), null);
                } else if (resultList != null) {
                    List<SQLQueryRoutineReturn.ResultField> fields = new ArrayList<>();
                    for (STMTreeNode field : resultList.findChildrenOfName(STMKnownRuleNames.routineResultField)) {
                        if (field.findFirstChildOfName(STMKnownRuleNames.dataType) != null) {
                            STMTreeNode identifier = field.findFirstChildOfName(STMKnownRuleNames.identifier);
                            SQLQuerySymbolEntry fieldName = identifier == null ? null : recognizer.collectLocalVariableName(identifier);
                            fields.add(new SQLQueryRoutineReturn.ResultField(fieldName, recognizer.collectTypeReference(field), null));
                        }
                    }
                    boolean table = returnClause.findNonErrorChildren().stream().anyMatch(child ->
                        child.getPayload() instanceof Token token && token.getType() == SQLStandardLexer.TABLE);
                    returnSpec = table ? new SQLQueryRoutineReturn.TableColumns(fields)
                        : new SQLQueryRoutineReturn.OutputParameters(fields);
                } else {
                    returnSpec = new SQLQueryRoutineReturn.Incomplete(returnClause);
                }
            }
        }
        STMTreeNode trailing = definition == null ? null : definition.findFirstChildOfName(STMKnownRuleNames.routineTrailing);
        STMTreeNode bodyNode = trailing == null ? null : trailing.findFirstChildOfName(STMKnownRuleNames.routineBody);
        SQLQueryStatementModel body = bodyNode == null || bodyNode.findFirstChildOfName(
            STMKnownRuleNames.routineBodyContent) == null
            ? null : SQLQueryStatementModel.recognize(recognizer, bodyNode);
        return alter
            ? new SQLQueryRoutineAlterModel(node, name, existingObject, arguments, returnSpec, body)
            : new SQLQueryRoutineCreateModel(node, name, arguments, returnSpec, body);
    }

    public boolean isAlter() {
        return this instanceof SQLQueryRoutineAlterModel;
    }

    @Nullable
    public SQLQueryComplexName getName() {
        return this.name;
    }

    @Nullable
    public SQLQueryObjectDataModel getExistingObject() {
        return this.existingObject;
    }

    @NotNull
    public List<SignatureArgument> getArguments() {
        return this.arguments;
    }

    @NotNull
    public List<SQLQueryExprType> getReturnTypes() {
        return this.returnSpec instanceof SQLQueryRoutineReturn.Scalar scalar && scalar.resolvedType() != null
            ? List.of(scalar.resolvedType()) : List.of();
    }

    @NotNull
    public List<SQLQueryExprType> getReturnTableTypes() {
        List<SQLQueryRoutineReturn.ResultField> fields = switch (this.returnSpec) {
            case SQLQueryRoutineReturn.TableColumns table -> table.fields();
            case SQLQueryRoutineReturn.OutputParameters output -> output.fields();
            default -> List.of();
        };
        return fields.stream().map(SQLQueryRoutineReturn.ResultField::resolvedType)
            .filter(Objects::nonNull).toList();
    }

    @NotNull
    public SQLQueryRoutineReturn getReturnSpec() {
        return this.returnSpec;
    }

    @Nullable
    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @Nullable T arg) {
        return visitor.visitRoutineStatement(this, arg);
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext prepareBodyContext(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.existingObject != null) {
            this.existingObject.resolveRowSources(context, statistics);
        }
        List<SignatureArgument> resolvedArguments = new ArrayList<>(this.arguments.size());
        for (SignatureArgument argument : this.arguments) {
            if (statistics.getMonitor().isCanceled()) {
                break;
            }
            SQLQueryVariableDeclarationStatement declaration = argument.declaration();
            if (declaration != null) {
                context = declaration.resolveStatement(context, statistics);
                SQLQueryExprType type = declaration.getDeclarations().isEmpty()
                    ? null : declaration.getDeclarations().getFirst().getType();
                resolvedArguments.add(new SignatureArgument(declaration, null, type));
            } else {
                SQLQueryModelRecognizer.TypeReference reference = Objects.requireNonNull(argument.typeReference());
                SQLQueryExprType type = SQLQuerySemanticUtils.resolveTypeReference(context, statistics, reference);
                resolvedArguments.add(new SignatureArgument(null, reference, type));
            }
        }
        this.arguments = List.copyOf(resolvedArguments);
        if (this.returnSpec instanceof SQLQueryRoutineReturn.Scalar scalar) {
            this.returnSpec = new SQLQueryRoutineReturn.Scalar(scalar.reference(),
                SQLQuerySemanticUtils.resolveTypeReference(context, statistics, scalar.reference()));
        } else if (this.returnSpec instanceof SQLQueryRoutineReturn.TableColumns ||
            this.returnSpec instanceof SQLQueryRoutineReturn.OutputParameters) {
            boolean table = this.returnSpec instanceof SQLQueryRoutineReturn.TableColumns;
            List<SQLQueryRoutineReturn.ResultField> fields = table
                ? ((SQLQueryRoutineReturn.TableColumns) this.returnSpec).fields()
                : ((SQLQueryRoutineReturn.OutputParameters) this.returnSpec).fields();
            boolean bindNames = !table || context.getDialect().getDialectFeatures().areRoutineTableResultColumnsBodyVisible();
            List<SQLQueryRoutineReturn.ResultField> resolved = new ArrayList<>(fields.size());
            for (SQLQueryRoutineReturn.ResultField field : fields) {
                if (statistics.getMonitor().isCanceled()) {
                    break;
                }
                SQLQueryExprType type = SQLQuerySemanticUtils.resolveTypeReference(context, statistics, field.reference());
                resolved.add(new SQLQueryRoutineReturn.ResultField(field.name(), field.reference(), type));
                if (bindNames) {
                    context = this.bindResultField(context, statistics, field.name(), type);
                }
            }
            this.returnSpec = table ? new SQLQueryRoutineReturn.TableColumns(resolved)
                : new SQLQueryRoutineReturn.OutputParameters(resolved);
        }
        return context;
    }

    @NotNull
    private SQLQueryRowsSourceContext bindResultField(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics,
        @Nullable SQLQuerySymbolEntry name, @NotNull SQLQueryExprType type
    ) {
        if (name == null) {
            return context;
        }
        SQLQueryLocalVariableInfo binding = new SQLQueryLocalVariableInfo(
            name, SQLDialectFeatures.LocalVariableKind.VALUE, type, null, true);
        SQLQueryLocalVariablesContext next = context.getLocalVariables().withLocalVariable(binding);
        if (next == context.getLocalVariables()) {
            statistics.appendError(name.getSyntaxNode(), "Local variable is already declared in this scope");
            if (name.isNotClassified()) {
                name.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
            }
            return context;
        }
        name.setDefinition(binding);
        return context.withLocalVariables(next);
    }

    @Override
    public boolean tryResolveValueRelations(
        @NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.existingObject != null && !this.existingObject.tryResolveValueRelations(context, statistics)) {
            return false;
        }
        for (SignatureArgument argument : this.arguments) {
            if (statistics.getMonitor().isCanceled() || (argument.declaration() != null
                && !argument.declaration().tryResolveValueRelations(context, statistics))) {
                return false;
            }
        }
        return super.tryResolveValueRelations(context, statistics);
    }
}
