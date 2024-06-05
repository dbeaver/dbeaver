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
package org.jkiss.dbeaver.model.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryRowsCorrelatedSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantic context query information about entities involved in the semantics model
 */
public abstract class SQLQueryDataContext {

    /**
     * Get columns of the query result tuple
     */
    @NotNull
    public abstract List<SQLQueryResultColumn> getColumnsList();

    /**
     * Find real table referenced by its name in the database
     */
    @Nullable
    public abstract DBSEntity findRealTable(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName);

    /**
     * Find column referenced by its name in the result tuple
     *
     * @implNote TODO consider ambiguous column names
     */
    @Nullable
    public abstract SQLQueryResultColumn resolveColumn(@NotNull DBRProgressMonitor monitor, @NotNull String simpleName);

    /**
     * Find semantic model item responsible for the representation of the data rows source having a given name
     * (table reference, named subquery, etc)
     *
     * @implNote TODO consider ambiguous table names
     */
    @Nullable
    public SourceResolutionResult resolveSource(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        DBSEntity table = this.findRealTable(monitor, tableName);
        SQLQueryRowsSourceModel source = table == null ? null : this.findRealSource(table);
        return source == null ? null : SourceResolutionResult.forRealTableByName(source, table); 
    }

    /**
     * Find semantic model item responsible for the representation of the real table where it is already referenced
     */
    @Nullable
    public abstract SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table);

    /**
     * Prepare new semantic context by overriding result tuple columns information
     */
    @NotNull
    public final SQLQueryDataContext overrideResultTuple(@NotNull List<SQLQueryResultColumn> columns) {
        return new SQLQueryResultTupleContext(this, columns);
    }

    /**
     * Prepare new semantic context by combining this context with the other given context
     */
    @NotNull
    public final SQLQueryDataContext combine(@NotNull SQLQueryDataContext other) {
        return new SQLQueryCombinedContext(this, other);
    }

    /**
     * Prepare new semantic context by introducing real table reference
     */
    @NotNull
    public final SQLQueryDataContext extendWithRealTable(@NotNull DBSEntity table, @NotNull SQLQueryRowsSourceModel source) {
        return new SQLQueryTableRowsContext(this, table, source);
    }

    /**
     * Prepare new semantic context by introducing rows source alias (table reference, named subquery, etc)
     */
    @NotNull
    public final SQLQueryDataContext extendWithTableAlias(@NotNull SQLQuerySymbol alias, @NotNull SQLQueryRowsSourceModel source) {
        return new SQLQueryAliasedRowsContext(this, alias, source);
    }

    /**
     * Prepare new semantic context by hiding all the involved rows sources such as subqueries and table references
     */
    @NotNull
    public final SQLQueryDataContext hideSources() {
        return new SQLQueryPureResultTupleContext(this);
    }

    /**
     * Get SQL dialect used adjust identifiers during semantics resolution
     */
    @NotNull
    public abstract SQLDialect getDialect();

    /**
     * Get fake table rows source used to represent invalid query fragments when the adequate semantic model item cannot be constructed
     */
    @NotNull
    public abstract SQLQueryRowsSourceModel getDefaultTable(@NotNull STMTreeNode syntaxNode);

    /**
     * Representation of the information about rows sources involved in semantic model
     */
    public static class KnownSourcesInfo {
        @NotNull
        private final Map<SQLQueryRowsSourceModel, SourceResolutionResult> sources = new HashMap<>();

        public void registerTableReference(@NotNull SQLQueryRowsSourceModel source, @NotNull DBSEntity table) {
            if (source instanceof SQLQueryRowsCorrelatedSourceModel cc && cc.getCorrelationColumNames().isEmpty()) {
                source = cc.getSource();
            }
            SQLQueryRowsSourceModel ssource = source;
            this.sources.compute(source, (k, v) -> v == null ? SourceResolutionResult.forRealTableByName(ssource, table) : SourceResolutionResult.withRealTable(v, table));
        }

        public void registerAlias(@NotNull SQLQueryRowsSourceModel source, @NotNull SQLQuerySymbol alias) {
            if (source instanceof SQLQueryRowsCorrelatedSourceModel cc && cc.getCorrelationColumNames().isEmpty()) {
                source = cc.getSource();
            }
            SQLQueryRowsSourceModel ssource = source;
            this.sources.compute(source, (k, v) -> v == null ? SourceResolutionResult.forSourceByAlias(ssource, alias) : SourceResolutionResult.withAlias(v, alias));
        }

        @NotNull
        public Map<SQLQueryRowsSourceModel, SourceResolutionResult> getResolutionResults() {
            return Collections.unmodifiableMap(this.sources);
        }
    }

    /**
     * Aggregate information about all the rows sources involved in this semantic context
     */
    public KnownSourcesInfo collectKnownSources() {
        KnownSourcesInfo result = new KnownSourcesInfo();
        this.collectKnownSourcesImpl(result);
        return result;
    }
    
    protected abstract void collectKnownSourcesImpl(@NotNull KnownSourcesInfo result);
}
