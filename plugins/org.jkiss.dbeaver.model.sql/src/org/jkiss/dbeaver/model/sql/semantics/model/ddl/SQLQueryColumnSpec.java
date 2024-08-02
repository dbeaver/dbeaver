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
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryQualifiedName;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueTypeCastExpression;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SQLQueryColumnSpec extends SQLQueryNodeModel {

    @NotNull
    private final SQLQuerySymbolEntry columnName;
    @NotNull
    private final String typeName;
    @Nullable
    private final SQLQueryValueExpression defaultValueExpression;
    @NotNull
    private final List<SQLQueryColumnConstraintSpec> constraints;

    public SQLQueryColumnSpec(@NotNull STMTreeNode syntaxNode, @NotNull SQLQuerySymbolEntry columnName, @NotNull String typeName, @Nullable SQLQueryValueExpression defaultValueExpression, @NotNull List<SQLQueryColumnConstraintSpec> constraints) {
        super(syntaxNode.getRealInterval(), syntaxNode, defaultValueExpression);
        this.columnName = columnName;
        this.typeName = typeName;
        this.defaultValueExpression = defaultValueExpression;
        this.constraints = List.copyOf(constraints);

        this.constraints.forEach(this::registerSubnode);
    }

    @NotNull
    public SQLQuerySymbolEntry getColumnName() {
        return this.columnName;
    }

    @NotNull
    public String getTypeName() {
        return this.typeName;
    }

    @Nullable
    public SQLQueryValueExpression getDefaultValueExpression() {
        return this.defaultValueExpression;
    }

    @NotNull
    public List<SQLQueryColumnConstraintSpec> getConstraints() {
        return this.constraints;
    }

    private static final Map<String, SQLQueryColumnConstraintKind> constraintKindByNodeName = Map.of(
        STMKnownRuleNames.columnConstraintNotNull, SQLQueryColumnConstraintKind.NOT_NULL,
        STMKnownRuleNames.columnConstraintUnique, SQLQueryColumnConstraintKind.UNIQUE,
        STMKnownRuleNames.columnConstraintPrimaryKey, SQLQueryColumnConstraintKind.PRIMARY_KEY,
        STMKnownRuleNames.referencesSpecification, SQLQueryColumnConstraintKind.REFERENCES,
        STMKnownRuleNames.checkConstraintDefinition, SQLQueryColumnConstraintKind.CHECK
    );

    public static SQLQueryColumnSpec recognize(SQLQueryModelRecognizer recognizer, STMTreeNode node) {
        SQLQuerySymbolEntry columnName = recognizer.collectIdentifier(node.findChildOfName(STMKnownRuleNames.columnName));
        String typeName = node.findChildOfName(STMKnownRuleNames.dataType).getTextContent();

        STMTreeNode defaultValueNode = node.findChildOfName(STMKnownRuleNames.defaultClause);
        SQLQueryValueExpression defaultValueExprssion = defaultValueNode == null ? null : recognizer.collectValueExpression(defaultValueNode);

        LinkedList<SQLQueryColumnConstraintSpec> constraints = new LinkedList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            STMTreeNode subnode = node.getStmChild(i);
            if (subnode.getNodeKindId() == SQLStandardParser.RULE_columnConstraintDefinition) {
                STMTreeNode constraintNameNode = subnode.findChildOfName(STMKnownRuleNames.constraintNameDefinition);
                SQLQueryQualifiedName constraintName = constraintNameNode == null ? null : recognizer.collectQualifiedName(constraintNameNode.findChildOfName(STMKnownRuleNames.constraintName));

                STMTreeNode constraintNode = subnode.findChildOfName(STMKnownRuleNames.columnConstraint).getStmChild(0);
                SQLQueryColumnConstraintKind constraintKind = constraintKindByNodeName.get(constraintNode.getNodeName());

                SQLQueryRowsTableDataModel referencedTable = null;
                List<SQLQuerySymbolEntry> referencedColumns = null;
                SQLQueryValueExpression checkExpression = null;
                switch (constraintKind) {
                    case CHECK -> checkExpression = recognizer.collectValueExpression(constraintNode.getStmChild(0));
                    case REFERENCES -> {
                        STMTreeNode refNode = constraintNode.findChildOfName(STMKnownRuleNames.referencedTableAndColumns);
                        referencedTable = recognizer.collectTableReference(refNode);
                        referencedColumns = recognizer.collectColumnNameList(refNode);
                    }
                }

                constraints.add(new SQLQueryColumnConstraintSpec(subnode, constraintName, constraintKind, referencedTable, referencedColumns, checkExpression));
            }
        }

        return new SQLQueryColumnSpec(node, columnName, typeName, defaultValueExprssion, constraints);
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
}