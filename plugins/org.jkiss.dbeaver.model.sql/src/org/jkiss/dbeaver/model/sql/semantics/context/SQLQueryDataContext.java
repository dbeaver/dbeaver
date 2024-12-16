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
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsCorrelatedSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.function.Function;

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
     * Returns flag demonstrating whether all the rows' sources were correctly resolved or not
     */
    public abstract boolean hasUndresolvedSource();

    /**
     * Get pseudo columns of the query result tuple
     */
    @NotNull
    public abstract List<SQLQueryResultPseudoColumn> getPseudoColumnsList();

    /**
     * Find real table referenced by its name in the database
     */
    @Nullable
    public abstract DBSEntity findRealTable(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName);

    /**
     * Find real object of given type referenced by its name in the database
     */
    @Nullable
    public abstract DBSObject findRealObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectType objectType,
        @NotNull List<String> objectName
    );

    /**
     * Find column referenced by its name in the result tuple
     *
     * @implNote TODO consider ambiguous column names
     */
    @Nullable
    public abstract SQLQueryResultColumn resolveColumn(@NotNull DBRProgressMonitor monitor, @NotNull String simpleName);

    /**
     * Find pseudo column referenced by its name in the result tuple
     *
     * @implNote TODO consider ambiguous column names
     */
    @Nullable
    public abstract SQLQueryResultPseudoColumn resolvePseudoColumn(@NotNull DBRProgressMonitor monitor, @NotNull String name);

    /**
     * Find global pseudo column referenced by its name in the result tuple
     *
     * @implNote TODO consider ambiguous column names
     */
    @Nullable
    public abstract SQLQueryResultPseudoColumn resolveGlobalPseudoColumn(@NotNull DBRProgressMonitor monitor, @NotNull String name);

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
    public final SQLQueryDataContext overrideResultTuple(
        @Nullable SQLQueryRowsSourceModel source,
        @NotNull List<SQLQueryResultColumn> columns,
        @NotNull List<SQLQueryResultPseudoColumn> pseudoColumns
    ) {
        // TODO: review pseudoattributes behavior in DDL expressions (not handling for now)
        List<SQLQueryResultPseudoColumn> allPseudoColumns = source == null
            ? pseudoColumns
            : STMUtils.combineLists(this.prepareRowsetPseudoColumns(source), pseudoColumns);
        return new SQLQueryResultTupleContext(this, columns, allPseudoColumns);
    }

    @NotNull
    public final SQLQueryDataContext overrideResultTuple(
        @Nullable SQLQueryRowsSourceModel source,
        @NotNull Pair<List<SQLQueryResultColumn>, List<SQLQueryResultPseudoColumn>> columnsAndPseudoColumns
    ) {
        return this.overrideResultTuple(source, columnsAndPseudoColumns.getFirst(), columnsAndPseudoColumns.getSecond());
    }


    /**
     * Prepare new semantic context by combining this context with the other given context
     */
    @NotNull
    public final SQLQueryDataContext combine(@NotNull SQLQueryDataContext other) {
        return new SQLQueryCombinedContext(this, other, false);
    }

    /**
     * Prepare new semantic context by combining this context with the other given context
     */
    @NotNull
    public final SQLQueryDataContext combineForJoin(@NotNull SQLQueryDataContext other) {
        return new SQLQueryCombinedContext(this, other, true);
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
     * Prepare new semantic context by introducing hasUnresolvedSource flag
     */
    public final SQLQueryDataContext markHasUnresolvedSource() {
        return new SQLQueryWithUndresolvedSourceRowsContext(this);
    }

    /**
     * Get SQL dialect used adjust identifiers during semantics resolution
     */
    @NotNull
    public abstract SQLDialect getDialect();

    /**
     * Representation of the information about rows sources involved in semantic model
     */
    public static class KnownSourcesInfo {
        @NotNull
        private final Map<SQLQueryRowsSourceModel, SourceResolutionResult> sources = new LinkedHashMap<>();
        @NotNull
        private final Set<DBSObject> referencedTables = new LinkedHashSet<>();
        @NotNull
        private final Set<String> aliasesInUse = new HashSet<>();

        private final Map<SQLQueryRowsSourceModel, SourceResolutionResult> sourcesView = Collections.unmodifiableMap(sources);
        private final Set<DBSObject> referencedTablesView = Collections.unmodifiableSet(this.referencedTables);
        private final Set<String> aliasesInUseView = Collections.unmodifiableSet(this.aliasesInUse);

        public void registerTableReference(@NotNull SQLQueryRowsSourceModel source, @NotNull DBSEntity table) {
            SQLQueryRowsSourceModel sourceModel = source instanceof SQLQueryRowsCorrelatedSourceModel cc && cc.getCorrelationColumNames().isEmpty()
                ? cc.getSource() : source;
            this.sources.compute(sourceModel, (k, v) -> v == null
                ? SourceResolutionResult.forRealTableByName(sourceModel, table) : SourceResolutionResult.withRealTable(v, table));
            this.referencedTables.add(table);
        }

        public void registerAlias(@NotNull SQLQueryRowsSourceModel source, @NotNull SQLQuerySymbol alias) {
            SQLQueryRowsSourceModel sourceModel = source instanceof SQLQueryRowsCorrelatedSourceModel cc && cc.getCorrelationColumNames().isEmpty()
                ? cc.getSource() : source;
            this.sources.compute(sourceModel, (k, v) -> v == null
                ? SourceResolutionResult.forSourceByAlias(sourceModel, alias) : SourceResolutionResult.withAlias(v, alias));
            this.aliasesInUse.add(alias.getName());
        }

        @NotNull
        public Map<SQLQueryRowsSourceModel, SourceResolutionResult> getResolutionResults() {
            return this.sourcesView;
        }

        @NotNull
        public Set<DBSObject> getReferencedTables() {
            return this.referencedTablesView;
        }

        @NotNull
        public Set<String> getAliasesInUse() {
            return this.aliasesInUseView;
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

    protected abstract List<SQLQueryResultPseudoColumn> prepareRowsetPseudoColumns(@NotNull SQLQueryRowsSourceModel source);
}
