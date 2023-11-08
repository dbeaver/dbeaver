/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class SQLQueryResultSublistSpec {
    @NotNull
    protected abstract Stream<SQLQuerySymbol> expand(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics);
}

class SQLQueryResultColumnSpec extends SQLQueryResultSublistSpec {
    private final SQLQueryValueExpression valueExpression;
    private final SQLQuerySymbolEntry alias;
    
    public SQLQueryResultColumnSpec(@NotNull SQLQueryValueExpression valueExpression) {
        this(valueExpression, null);
    }   
    
    public SQLQueryResultColumnSpec(@NotNull SQLQueryValueExpression valueExpression, @Nullable SQLQuerySymbolEntry alias) {
        this.valueExpression = valueExpression;
        this.alias = alias;
    }    

    @NotNull
    @Override
    protected Stream<SQLQuerySymbol> expand(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.valueExpression.propagateContext(context, statistics);

        SQLQuerySymbol columnName;
        if (this.alias != null) {
            columnName = this.alias.getSymbol();
            columnName.setDefinition(this.alias);
            columnName.setSymbolClass(SQLQuerySymbolClass.COLUMN_DERIVED);
        } else {
            columnName = this.valueExpression.getColumnNameIfTrivialExpression();
            if (columnName == null) {
                columnName = new SQLQuerySymbol("?"); 
            }
        }
        
        return Stream.of(columnName);
    }
}

class SQLQueryResultTupleSpec extends SQLQueryResultSublistSpec {
    private final SQLQueryQualifiedName tableName;

    public SQLQueryResultTupleSpec(SQLQueryQualifiedName tableName) {
        this.tableName = tableName;
    }
    
    @NotNull
    @Override
    protected Stream<SQLQuerySymbol> expand(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        SourceResolutionResult rr = context.resolveSource(this.tableName.toListOfStrings()); // TODO consider multiple joins of one table
        if (rr != null) {
            this.tableName.setDefinition(rr);
            return rr.source.getDataContext().getColumnsList().stream();
        } else {
            this.tableName.setSymbolClass(SQLQuerySymbolClass.ERROR);
            statistics.appendError(this.tableName.entityName, "The table doesn't participate in this subquery context");
            return Stream.empty();
        }
    }
}

class SQLQueryResultCompleteTupleSpec extends SQLQueryResultSublistSpec {
    public SQLQueryResultCompleteTupleSpec() {
        // do nothing
    }
    
    @NotNull
    @Override
    protected Stream<SQLQuerySymbol> expand(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        return context.getColumnsList().stream();
    }
}

class SQLQuerySelectionResultModel {
    private final List<SQLQueryResultSublistSpec> sublists;

    public SQLQuerySelectionResultModel(@NotNull List<SQLQueryResultSublistSpec> sublists) {
        this.sublists = sublists;
    }

    public List<SQLQuerySymbol> expandColumns(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        return this.sublists.stream().flatMap(s -> s.expand(context, statistics)).collect(Collectors.toList());                
    }
}
