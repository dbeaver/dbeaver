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
package org.jkiss.dbeaver.model.sql.semantics.model.select;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents INTERSECT, UNION and EXCEPT operation model in the context of SQL query semantics
 */
public class SQLQueryRowsSetCorrespondingOperationModel extends SQLQueryRowsSetOperationModel {
    @NotNull
    private final List<SQLQuerySymbolEntry> correspondingColumnNames;
    @NotNull
    private final SQLQueryRowsSetCorrespondingOperationKind kind;

    public SQLQueryRowsSetCorrespondingOperationModel(
        @NotNull Interval range,
        @NotNull STMTreeNode syntaxNode,
        @NotNull SQLQueryRowsSourceModel left,
        @NotNull SQLQueryRowsSourceModel right,
        @NotNull List<SQLQuerySymbolEntry> correspondingColumnNames,
        @NotNull SQLQueryRowsSetCorrespondingOperationKind kind
    ) {
        super(range, syntaxNode, left, right);
        this.correspondingColumnNames = correspondingColumnNames;
        this.kind = kind;
    }

    @NotNull
    public SQLQueryRowsSetCorrespondingOperationKind getKind() {
        return this.kind;
    }

    @NotNull
    private SQLQueryExprType obtainCommonType(@Nullable SQLQueryResultColumn leftDef, @Nullable SQLQueryResultColumn rightDef) {
        SQLQueryExprType type;
        
        if (leftDef == null && rightDef == null) {
            type = SQLQueryExprType.UNKNOWN;
        } else if (leftDef == null) {
            type = rightDef.type;
        } else if (rightDef == null) {
            type = leftDef.type; 
        } else {        
            type = SQLQueryExprType.tryCombineIfMatches(leftDef.type, rightDef.type);
            if (type == null) {
                type = SQLQueryExprType.UNKNOWN;
            }
        }
        
        return type;
    }
    
    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryDataContext left = this.left.propagateContext(context, statistics);
        SQLQueryDataContext right = this.right.propagateContext(context, statistics);

        List<SQLQueryResultColumn> resultColumns;
        boolean nonMatchingColumnSets = false;
        if (correspondingColumnNames.isEmpty()) { // require left and right to have the same tuples
            List<SQLQueryResultColumn> leftColumns = left.getColumnsList();
            List<SQLQueryResultColumn> rightColumns = right.getColumnsList();
            int resultColumnsCount = Math.max(leftColumns.size(), rightColumns.size());
            resultColumns = new ArrayList<>(resultColumnsCount);
            for (int i = 0; i < resultColumnsCount; i++) {
                if (i >= leftColumns.size()) {
                    resultColumns.add(rightColumns.get(i));
                    nonMatchingColumnSets = true;
                } else if (i >= rightColumns.size()) {
                    resultColumns.add(leftColumns.get(i));
                    nonMatchingColumnSets = true;
                } else {
                    SQLQueryResultColumn leftColumn = leftColumns.get(i);
                    SQLQueryResultColumn rightColumn = rightColumns.get(i);
                    SQLQueryExprType type = this.obtainCommonType(leftColumn, rightColumn);
                    SQLQuerySymbol symbol;
                    if (leftColumn.symbol.getName().equalsIgnoreCase(rightColumn.symbol.getName())) {
                        SQLQuerySymbolClass leftClass = leftColumn.symbol.getSymbolClass();
                        SQLQuerySymbolDefinition leftDef = leftColumn.symbol.getDefinition();
                        // new symbol after merge carries underlying info of the left column and combined entries set
                        symbol = leftColumn.symbol.merge(rightColumn.symbol);
                        symbol.setDefinition(leftDef);
                        if (symbol.getSymbolClass() == SQLQuerySymbolClass.UNKNOWN) {
                            symbol.setSymbolClass(leftClass);
                        }
                    } else {
                        symbol = leftColumn.symbol;
                    }
                    resultColumns.add(new SQLQueryResultColumn(i, symbol, this, null, null, type));
                }
            }
        } else { // require left and right to have columns subset as given with correspondingColumnNames
            SQLQuerySymbolOrigin columnNameOrigin = new SQLQuerySymbolOrigin.ColumnNameFromContext(left.combine(right));
            int resultColumnsCount = correspondingColumnNames.size();
            resultColumns = new ArrayList<>(resultColumnsCount);
            for (int i = 0; i < resultColumnsCount; i++) {
                SQLQuerySymbolEntry column = correspondingColumnNames.get(i);
                if (column.isNotClassified()) {
                    SQLQueryResultColumn leftDef = left.resolveColumn(statistics.getMonitor(), column.getName());
                    SQLQueryResultColumn rightDef = right.resolveColumn(statistics.getMonitor(), column.getName());

                    if (leftDef == null || rightDef == null) {
                        nonMatchingColumnSets = true;
                    }
                    SQLQueryExprType type = this.obtainCommonType(leftDef, rightDef);

                    column.getSymbol().setDefinition(column); // TODO combine multiple definitions
                    column.setOrigin(columnNameOrigin);
                    resultColumns.add(new SQLQueryResultColumn(i, column.getSymbol(), this, null, null, type));
                }
            }
        }

        if (nonMatchingColumnSets) {
            // TODO detailed messages per column
            statistics.appendError(this.getSyntaxNode(), "UNION, EXCEPT and INTERSECT require subsets column tuples to match");
        }
        // TODO multiple definitions per symbol
        return context.overrideResultTuple(this, resultColumns, Collections.emptyList());
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsSetCorrespondingOp(this, arg);
    }
}

