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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolDefinition;

import java.util.List;
import java.util.Objects;

/**
 * Redefines result tuple leaving the aggregated sources from the parent context
 */
public class SQLQueryResultTupleContext extends SQLQuerySyntaxContext {
    private final List<SQLQuerySymbol> columns;

    public SQLQueryResultTupleContext(@NotNull SQLQueryDataContext parent, @NotNull List<SQLQuerySymbol> columns) {
        super(parent);
        this.columns = columns;
    }

    @NotNull
    @Override
    public List<SQLQuerySymbol> getColumnsList() {
        return this.columns;
    }

    @Nullable
    @Override
    public SQLQuerySymbolDefinition resolveColumn(@NotNull String columnName) {  // TODO consider reporting ambiguity
        return columns.stream().filter(c -> c.getName().equals(columnName)).map(SQLQuerySymbol::getDefinition)
            .filter(Objects::nonNull).findFirst().orElse(null);
    }
}

