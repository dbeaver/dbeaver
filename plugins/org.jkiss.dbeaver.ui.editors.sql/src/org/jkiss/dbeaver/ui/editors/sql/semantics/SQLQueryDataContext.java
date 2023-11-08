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

import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.util.*;

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

class SourceResolutionResult {
    public final SQLQueryRowsSource source;
    public final DBSEntity tableOrNull;
    public final SQLQuerySymbol aliasOrNull;
    
    private SourceResolutionResult(SQLQueryRowsSource source, DBSEntity tableOrNull, SQLQuerySymbol aliasOrNull) {
        this.source = source;
        this.tableOrNull = tableOrNull;
        this.aliasOrNull = aliasOrNull;
    }
    
    public static SourceResolutionResult forRealTableByName(SQLQueryRowsSource source, DBSEntity table) {
        return new SourceResolutionResult(source, table, null);
    }
    
    public static SourceResolutionResult forSourceByAlias(SQLQueryRowsSource source, SQLQuerySymbol alias) {
        return new SourceResolutionResult(source, null, alias);
    }
}

abstract class SQLQueryDataContext {
    
    abstract List<SQLQuerySymbol> getColumnsList();
    
    abstract DBSEntity findRealTable(List<String> tableName);

    abstract SQLQuerySymbolDefinition resolveColumn(String simpleName);  // TODO consider ambiguous column names

//    abstract SQLQuerySymbolDefinition resolveColumn(List<String> tableName, String columnName);
    
    public SourceResolutionResult resolveSource(List<String> tableName) { // TODO consider ambiguous table names
        DBSEntity table = this.findRealTable(tableName);
        SQLQueryRowsSource source = this.findRealSource(table);;
        return source == null ? null : SourceResolutionResult.forRealTableByName(source, table); 
    }
    
    abstract SQLQueryRowsSource findRealSource(DBSEntity table);

    public final SQLQueryDataContext overrideResultTuple(List<SQLQuerySymbol> columns) {
        return new SQLQueryResultTupleContext(this, columns);
    }
    
    public final SQLQueryDataContext combine(SQLQueryDataContext other) {
        return new SQLQueryCombinedContext(this, other);
    }
    
    public final SQLQueryDataContext extendWithRealTable(DBSEntity table, SQLQueryRowsSource source) {
        return new SQLQueryTableRowsContext(this, table, source);
    }

    public final SQLQueryDataContext extendWithTableAlias(SQLQuerySymbol alias, SQLQueryRowsSource source) {
        return new SQLQueryAliasedRowsContext(this, alias, source);
    }
    
    public final SQLQueryDataContext hideSources() {
        return new SQLQueryPureResultTupleContext(this);
    }

    abstract SQLDialect getDialect();
}

/**
 * Represents underlying database context having real tables
 */
class SQLQueryDataSourceContext extends SQLQueryDataContext {
    private final DBCExecutionContext executionContext;
    private final SQLDialect dialect;

    public SQLQueryDataSourceContext(DBCExecutionContext executionContext, SQLDialect dialect) {
        this.executionContext = executionContext;
        this.dialect = dialect;
    }
    
    @Override
    public List<SQLQuerySymbol> getColumnsList() {
        return Collections.emptyList();
    }
    
    @Override
    public DBSEntity findRealTable(List<String> tableName) {
        // System.out.println("looking for " + tableName);
        if (this.executionContext.getDataSource() instanceof DBSObjectContainer container) {
            List<String> tableName2 = new ArrayList<>(tableName);
            DBSObject obj = SQLSearchUtils.findObjectByFQN(new VoidProgressMonitor(), container, this.executionContext, tableName2, false, null);
            return obj instanceof DBSTable table ? table : (obj instanceof DBSView view ? view : null);
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    @Override
    public SQLQueryRowsSource findRealSource(DBSEntity table) {
        return null;
    }
    
//    @Override
//    public SQLQuerySymbolDefinition resolveColumn(List<String> tableName, String columnName) {
//        if (tableName.size() == 0) {
//            return null; 
//        } else {
//            SQLQueryRowsSource table = this.resolveSource(tableName);
//            return table.getDataContext().resolveColumn(columnName);
//        }
//    }
    
    @Override
    public SQLQuerySymbolDefinition resolveColumn(String simpleName) {
        return null;
    }
    
    @Override
    SQLDialect getDialect() {
        return this.dialect;
    }
}

/**
 * Represents any derived context based on relational operations in parent context 
 */
abstract class SQLQuerySyntaxContext extends SQLQueryDataContext {
    protected final SQLQueryDataContext parent;

    public SQLQuerySyntaxContext(SQLQueryDataContext parent) {
        this.parent = parent;
    }
    
    @Override
    public List<SQLQuerySymbol> getColumnsList() {
        return this.parent.getColumnsList();
    }
    
    @Override
    public DBSEntity findRealTable(List<String> tableName) {
        return this.parent.findRealTable(tableName);
    }
    
    @Override
    public SQLQueryRowsSource findRealSource(DBSEntity table) {
        return this.parent.findRealSource(table);
    }
    
//    @Override
//    public SQLQuerySymbolDefinition resolveColumn(List<String> tableName, String columnName) {
//        return this.parent.resolveColumn(tableName, columnName);
//    }
    
    @Override
    public SQLQuerySymbolDefinition resolveColumn(String columnName) {
        return this.parent.resolveColumn(columnName);
    }
    
    @Override
    public SourceResolutionResult resolveSource(List<String> tableName) {
        return this.parent.resolveSource(tableName);
    }
    
    @Override
    SQLDialect getDialect() {
        return this.parent.getDialect();
    }
}

/**
 * Redefines result tuple leaving the aggregated sources from the parent context 
 */
class SQLQueryResultTupleContext extends SQLQuerySyntaxContext {
    private final List<SQLQuerySymbol> columns;

    public SQLQueryResultTupleContext(SQLQueryDataContext parent, List<SQLQuerySymbol> columns) {
        super(parent);
        this.columns = columns;
    }    
    
    @Override
    public List<SQLQuerySymbol> getColumnsList() {
        return this.columns;
    }
    
    @Override
    public SQLQuerySymbolDefinition resolveColumn(String columnName) {  // TODO consider reporting ambiguity
        return columns.stream().filter(c -> c.getName().equals(columnName)).map(c -> c.getDefinition())
                               .filter(c -> c != null).findFirst().orElse(null);
    }
}

/**
 * Represents combination of two contexts made as a result of some subsets merging operation
 */
class SQLQueryCombinedContext extends SQLQueryResultTupleContext {
    private final SQLQueryDataContext otherParent;

    public SQLQueryCombinedContext(SQLQueryDataContext left, SQLQueryDataContext right) {
        super(left, combineColumns(left.getColumnsList(), right.getColumnsList()));
        this.otherParent = right;
    }
    
    @Override
    public SQLQueryRowsSource findRealSource(DBSEntity table) {
        return anyOfTwo(parent.findRealSource(table), otherParent.findRealSource(table)); // TODO consider ambiguity
    }
    
    @Override
    public DBSEntity findRealTable(List<String> tableName) {
        return anyOfTwo(parent.findRealTable(tableName), otherParent.findRealTable(tableName)); // TODO consider ambiguity
    }
    
    @Override
    public SourceResolutionResult resolveSource(List<String> tableName) {
        return anyOfTwo(parent.resolveSource(tableName), otherParent.resolveSource(tableName)); // TODO consider ambiguity
    } 
    

    private static List<SQLQuerySymbol> combineColumns(List<SQLQuerySymbol> leftColumns, List<SQLQuerySymbol> rightColumns) {
        List<SQLQuerySymbol> symbols = new ArrayList<>(leftColumns.size() + rightColumns.size());
        symbols.addAll(leftColumns);
        symbols.addAll(rightColumns);
        return symbols;
    }
    
    private static <T> T anyOfTwo(T a, T b) {
        return a != null ? a : b;
    }
}

/**
 * Hides all the aggregated row sources leaving just the result tuple columns coming from parent context 
 */
class SQLQueryPureResultTupleContext extends SQLQuerySyntaxContext {

    public SQLQueryPureResultTupleContext(SQLQueryDataContext parent) {
        super(parent);
    }
    
//    @Override
//    public SQLQuerySymbolDefinition resolveColumn(List<String> tableName, String columnName) { return null; }
    
    @Override
    public SourceResolutionResult resolveSource(List<String> tableName) { return null; }
    
    @Override
    public SQLQueryRowsSource findRealSource(DBSEntity table) { return null; }
}

/**
 * Represents any source typically introduced with real table reference in parent context
 */
class SQLQueryTableRowsContext extends SQLQuerySyntaxContext {
    private final DBSEntity table;
    private final SQLQueryRowsSource source;

    public SQLQueryTableRowsContext(SQLQueryDataContext parent, DBSEntity table, SQLQueryRowsSource source) {
        super(parent);
        this.table = table;
        this.source = source;
    }
    
    @Override
    public SQLQueryRowsSource findRealSource(DBSEntity table) {
        return this.table.equals(table) ? this.source : super.findRealSource(table);
    }
}

/**
 * Represents aliased source introduced with table correlation in parent context 
 */
class SQLQueryAliasedRowsContext extends SQLQuerySyntaxContext {
    private final SQLQuerySymbol alias;
    private final SQLQueryRowsSource source;

    public SQLQueryAliasedRowsContext(SQLQueryDataContext parent, SQLQuerySymbol alias, SQLQueryRowsSource source) {
        super(parent);
        this.alias = alias;
        this.source = source;
    }

    @Override
    public SourceResolutionResult resolveSource(List<String> tableName) {
        return tableName.size() == 1 && tableName.get(0).equals(this.alias.getName()) 
            ? SourceResolutionResult.forSourceByAlias(this.source, this.alias) 
            : super.resolveSource(tableName);
    }
}
