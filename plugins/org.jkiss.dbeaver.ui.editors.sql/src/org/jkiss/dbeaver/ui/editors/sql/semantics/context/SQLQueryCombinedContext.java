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
package org.jkiss.dbeaver.ui.editors.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsSourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents combination of two contexts made as a result of some subsets merging operation
 */
public class SQLQueryCombinedContext extends SQLQueryResultTupleContext {
    private final SQLQueryDataContext otherParent;

    public SQLQueryCombinedContext(@NotNull SQLQueryDataContext left, @NotNull SQLQueryDataContext right) {
        super(left, combineColumns(left.getColumnsList(), right.getColumnsList()));
        this.otherParent = right;
    }

    @NotNull
    @Override
    public SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table) {
        return anyOfTwo(parent.findRealSource(table), otherParent.findRealSource(table)); // TODO consider ambiguity
    }

    @NotNull
    @Override
    public DBSEntity findRealTable(@NotNull List<String> tableName) {
        return anyOfTwo(parent.findRealTable(tableName), otherParent.findRealTable(tableName)); // TODO consider ambiguity
    }

    @NotNull
    @Override
    public SourceResolutionResult resolveSource(@NotNull List<String> tableName) {
        return anyOfTwo(parent.resolveSource(tableName), otherParent.resolveSource(tableName)); // TODO consider ambiguity
    }


    @NotNull
    private static List<SQLQueryResultColumn> combineColumns(
        @NotNull List<SQLQueryResultColumn> leftColumns,
        @NotNull List<SQLQueryResultColumn> rightColumns
    ) {
        List<SQLQueryResultColumn> symbols = new ArrayList<>(leftColumns.size() + rightColumns.size());
        symbols.addAll(leftColumns);
        symbols.addAll(rightColumns);
        return symbols;
    }

    private static <T> T anyOfTwo(T a, T b) {
        return a != null ? a : b;
    }
}
