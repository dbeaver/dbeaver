/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics.model.ddl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryQualifiedName;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SQLQueryColumnSpec extends SQLQueryNodeModel {

    private static final Map<String, SQLQueryColumnConstraintKind> constraintKindByNodeName = Map.of(
        STMKnownRuleNames.columnConstraintNotNull, SQLQueryColumnConstraintKind.NOT_NULL,
        STMKnownRuleNames.columnConstraintUnique, SQLQueryColumnConstraintKind.UNIQUE,
        STMKnownRuleNames.columnConstraintPrimaryKey, SQLQueryColumnConstraintKind.PRIMARY_KEY,
        STMKnownRuleNames.referencesSpecification, SQLQueryColumnConstraintKind.REFERENCES,
        STMKnownRuleNames.checkConstraintDefinition, SQLQueryColumnConstraintKind.CHECK
    );

    @Nullable
    private final SQLQuerySymbolEntry columnName;
    @Nullable
    private final String typeName;
    @Nullable
    private final SQLQueryValueExpression defaultValueExpression;
    @NotNull
    private final List<SQLQueryColumnConstraintSpec> constraints;

    public SQLQueryColumnSpec(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQuerySymbolEntry columnName,
        @Nullable String typeName,
        @Nullable SQLQueryValueExpression defaultValueExpression,
        @NotNull List<SQLQueryColumnConstraintSpec> constraints
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, defaultValueExpression);
        this.columnName = columnName;
        this.typeName = typeName;
        this.defaultValueExpression = defaultValueExpression;
        this.constraints = List.copyOf(constraints);

        this.constraints.forEach(this::registerSubnode);
    }

    @Nullable
    public SQLQuerySymbolEntry getColumnName() {
        return this.columnName;
    }

    @Nullable
    public String getTypeName() {
        return this.typeName;
    }

    public SQLQueryExprType getDeclaredColumnType() {
        return this.typeName != null ? SQLQueryExprType.forExplicitTypeRef(this.typeName) : SQLQueryExprType.UNKNOWN;
    }

    @Nullable
    public SQLQueryValueExpression getDefaultValueExpression() {
        return this.defaultValueExpression;
    }

    @NotNull
    public List<SQLQueryColumnConstraintSpec> getConstraints() {
        return this.constraints;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitColumnSpec(this, arg);
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return null;
    }

    /**
     * Propagate semantics context and establish relations through the query model
     */
    public void propagateContext(
        @NotNull SQLQueryDataContext dataContext,
        @Nullable SQLQueryDataContext tableContext,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        if (this.defaultValueExpression != null && tableContext != null) {
            this.defaultValueExpression.propagateContext(tableContext, statistics);
        }
        for (SQLQueryColumnConstraintSpec constraintSpec : this.constraints) {
            constraintSpec.propagateContext(dataContext, tableContext, statistics);
        }
    }

    public static SQLQueryColumnSpec recognize(SQLQueryModelRecognizer recognizer, STMTreeNode node) {
        SQLQuerySymbolEntry columnName = Optional.ofNullable(node.findFirstChildOfName(STMKnownRuleNames.columnName))
            .map(n -> recognizer.collectIdentifier(n, null))
            .orElse(null);
        String typeName = Optional.ofNullable(node.findFirstChildOfName(STMKnownRuleNames.dataType))
            .map(STMTreeNode::getTextContent).orElse(null);

        STMTreeNode defaultValueNode = node.findFirstChildOfName(STMKnownRuleNames.defaultClause);
        SQLQueryValueExpression defaultValueExpr = defaultValueNode == null ? null : recognizer.collectValueExpression(defaultValueNode);

        LinkedList<SQLQueryColumnConstraintSpec> constraints = new LinkedList<>();
        for (STMTreeNode subnode : node.findChildrenOfName(STMKnownRuleNames.columnConstraintDefinition)) {
            SQLQueryQualifiedName constraintName = Optional.ofNullable(subnode.findFirstChildOfName(STMKnownRuleNames.constraintNameDefinition))
                .map(n -> n.findFirstChildOfName(STMKnownRuleNames.constraintName))
                .map(recognizer::collectQualifiedName)
                .orElse(null);

            STMTreeNode constraintNode = Optional.ofNullable(subnode.findFirstChildOfName(STMKnownRuleNames.columnConstraint))
                .map(STMTreeNode::findFirstNonErrorChild).orElse(null);
            SQLQueryColumnConstraintKind constraintKind;
            SQLQueryRowsTableDataModel referencedTable = null;
            List<SQLQuerySymbolEntry> referencedColumns = null;
            SQLQueryValueExpression checkExpression = null;
            if (constraintNode != null) {
                constraintKind = constraintKindByNodeName.get(constraintNode.getNodeName());
                switch (constraintKind) {
                    case CHECK ->
                        checkExpression = recognizer.collectValueExpression(constraintNode);
                    case REFERENCES -> {
                        STMTreeNode refNode = constraintNode.findFirstChildOfName(STMKnownRuleNames.referencedTableAndColumns);
                        if (refNode != null) {
                            referencedTable = recognizer.collectTableReference(refNode, true);
                            referencedColumns = recognizer.collectColumnNameList(refNode);
                        }
                    }
                }
            } else {
                constraintKind = SQLQueryColumnConstraintKind.UNKNOWN;
            }
            constraints.addLast(new SQLQueryColumnConstraintSpec(subnode, constraintName, constraintKind, referencedTable, referencedColumns, checkExpression));
        }

        return new SQLQueryColumnSpec(node, columnName, typeName, defaultValueExpr, constraints);
    }

}