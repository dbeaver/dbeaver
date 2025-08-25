/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.SQLQueryValueFunctionExpression;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SQLQueryRowsTableProcModel extends SQLQueryRowsSourceModel {
    private static final Log log = Log.getLog(SQLQueryRowsTableProcModel.class);
    @NotNull
    private final SQLQueryValueFunctionExpression callExpr;

    public SQLQueryRowsTableProcModel(@NotNull SQLQueryValueFunctionExpression callExpr) {
        super(callExpr.getSyntaxNode());
        this.callExpr = callExpr;
    }

    @NotNull
    public SQLQueryValueFunctionExpression getFunctionExpression() {
        return this.callExpr;
    }

    @Override
    protected SQLQueryRowsSourceContext resolveRowSourcesImpl(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.callExpr.resolveRowSources(context, statistics);
        return context.reset();
    }

    @Override
    protected SQLQueryRowsDataContext resolveRowDataImpl(
        @NotNull SQLQueryRowsDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    ) {
        this.callExpr.resolveValueRelations(context, statistics);
        SQLQueryExprType procResult = this.callExpr.getValueType();
        LinkedList<SQLQueryResultColumn> resultColumns = new LinkedList<>();
        if (procResult != SQLQueryExprType.UNKNOWN) {
            try {
                SQLQueryExprType rowType = procResult.findIndexedItemType(statistics.getMonitor(), 1, null);
                if (rowType != null) {
                    for (SQLQueryExprType.SQLQueryExprTypeMemberInfo field : rowType.getNamedMembers(statistics.getMonitor())) {
                        resultColumns.addLast(
                            new SQLQueryResultColumn(
                                resultColumns.size(),
                                new SQLQuerySymbol(field.name()),
                                this,
                                null,
                                field.attribute(),
                                field.type()
                            )
                        );
                    }
                } else {
                    statistics.appendError(
                        this.getSyntaxNode(),
                        this.callExpr.getProcName().getNameString() + " is not a rowset-producing procedure"
                    );
                }
            } catch (DBException e) {
                String message = "Failed to resolve function result fields for " + this.callExpr.getProcName();
                log.debug(message, e);
                statistics.appendError(this.getSyntaxNode(), message);
                this.getRowsSources().resetAsUnresolved().makeEmptyTuple();
            }
        }
        return this.getRowsSources().makeTuple(this, List.copyOf(resultColumns), Collections.emptyList());
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitRowsTableProc(this, arg);
    }
}
