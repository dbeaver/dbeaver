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

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsSourceModel;

import java.util.List;

// // TODO
//
//class SQLQueryCteSubqueryModel implements SQLQueryRowsSource {
//    private final SQLQueryCorrelationSpec correlation;
//    private final SQLQueryRowsSource subquery;
//}
//
//class SQLQueryCteModel {
//    private final boolean isRecursive;
//    private final Map<String, SQLQueryCteSubqueryModel> subqueries = new HashMap<>();
//}
public abstract class SQLQueryDataContext {
    
    public abstract List<SQLQueryResultColumn> getColumnsList();

    public abstract DBSEntity findRealTable(List<String> tableName);

    public abstract SQLQueryResultColumn resolveColumn(String simpleName);  // TODO consider ambiguous column names
    
    public SourceResolutionResult resolveSource(List<String> tableName) { // TODO consider ambiguous table names
        DBSEntity table = this.findRealTable(tableName);
        SQLQueryRowsSourceModel source = this.findRealSource(table);
        return source == null ? null : SourceResolutionResult.forRealTableByName(source, table); 
    }
    
    public abstract SQLQueryRowsSourceModel findRealSource(DBSEntity table);

    public final SQLQueryDataContext overrideResultTuple(List<SQLQueryResultColumn> columns) {
        return new SQLQueryResultTupleContext(this, columns);
    }
    
    public final SQLQueryDataContext combine(SQLQueryDataContext other) {
        return new SQLQueryCombinedContext(this, other);
    }
    
    public final SQLQueryDataContext extendWithRealTable(DBSEntity table, SQLQueryRowsSourceModel source) {
        return new SQLQueryTableRowsContext(this, table, source);
    }

    public final SQLQueryDataContext extendWithTableAlias(SQLQuerySymbol alias, SQLQueryRowsSourceModel source) {
        return new SQLQueryAliasedRowsContext(this, alias, source);
    }
    
    public final SQLQueryDataContext hideSources() {
        return new SQLQueryPureResultTupleContext(this);
    }

    public abstract SQLDialect getDialect();

    @NotNull
    public abstract SQLQueryRowsSourceModel getDefaultTable(@NotNull Interval range);
}
