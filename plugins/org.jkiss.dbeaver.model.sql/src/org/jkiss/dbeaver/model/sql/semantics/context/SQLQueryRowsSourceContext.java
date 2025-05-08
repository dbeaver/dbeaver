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
package org.jkiss.dbeaver.model.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryQualifiedName;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class SQLQueryRowsSourceContext {

    private static final Log log = Log.getLog(SQLQueryRowsSourceContext.class);

    /**
     * Describes the result of the query source resolution for the specified identifier name
     */
    public static class KnownRowsSourceInfo extends SourceResolutionResult {

        @Nullable
        public final SQLQueryComplexName referenceName;

        protected KnownRowsSourceInfo(
            @NotNull SQLQueryRowsSourceModel source,
            @Nullable SQLQueryComplexName referenceName,
            @Nullable DBSEntity tableOrNull,
            @Nullable SQLQuerySymbol aliasOrNull
        ) {
            super(source, tableOrNull, aliasOrNull);
            this.referenceName = referenceName;
        }
    }

    @NotNull
    private final SQLQueryConnectionContext connectionInfo;

    private final boolean hasUnresolvedSource;

    @NotNull
    private final Map<SQLQueryComplexName, KnownRowsSourceInfo> rowsSources;

    @NotNull
    private final Map<SQLQueryComplexName, KnownRowsSourceInfo> dynamicTableSources;

    public SQLQueryRowsSourceContext(@NotNull SQLQueryConnectionContext connectionInfo) {
        this.connectionInfo = connectionInfo;
        this.hasUnresolvedSource = false;
        this.rowsSources = Collections.emptyMap();
        this.dynamicTableSources = Collections.emptyMap();
    }

    private SQLQueryRowsSourceContext(
        @NotNull SQLQueryConnectionContext connectionInfo,
        boolean hasUnresolvedSource,
        @NotNull Map<SQLQueryComplexName, KnownRowsSourceInfo> rowsSources,
        @NotNull Map<SQLQueryComplexName, KnownRowsSourceInfo> dynamicTableSources
    ) {
        this.connectionInfo = connectionInfo;
        this.hasUnresolvedSource = hasUnresolvedSource;
        this.rowsSources = rowsSources;
        this.dynamicTableSources = dynamicTableSources;
    }

    @NotNull
    public SQLDialect getDialect() {
        return this.connectionInfo.dialect;
    }

    @NotNull
    public SQLQueryConnectionContext getConnectionInfo() {
        return this.connectionInfo;
    }

    /**
     * Prepare new semantic context by hiding all the involved rows sources such as subqueries and table references
     */
    @NotNull
    public final SQLQueryRowsSourceContext reset() {
        return new SQLQueryRowsSourceContext(this.connectionInfo);
    }

    /**
     * Returns flag demonstrating whether all the rows' sources were correctly resolved or not
     */
    public boolean hasUnresolvedSource() {
        return this.hasUnresolvedSource;
    }

    /**
     * Find semantic model item responsible for the representation of the dynamic table data like CTE being treated as an extra table
     */
    @Nullable
    public KnownRowsSourceInfo findDynamicRowsSource(@NotNull SQLQueryComplexName name) {
        return this.dynamicTableSources.get(name);
    }

    /**
     * Find semantic model item responsible for the representation of the data rows source having a given name
     * (table reference, named subquery, etc)
     *
     * @implNote TODO consider ambiguous table names
     */
    @Nullable
    public KnownRowsSourceInfo findReferencedSource(@NotNull SQLQueryComplexName name) {
        SQLQueryComplexName namePart = name;
        while (namePart != null) {
            KnownRowsSourceInfo entry = this.rowsSources.get(namePart);
            if (entry != null) {
                return entry;
            } else {
                namePart = namePart.trimEnd();
            }
        }
        return null;
    }

    /**
     * Get the resolved query source by its name
     */
    @Nullable
    public KnownRowsSourceInfo findReferencedSourceExact(@NotNull SQLQueryComplexName name) {
        return this.rowsSources.get(name);
    }

    /**
     * Prepare new semantic context by combining this context with the other given context
     */
    @NotNull
    public SQLQueryRowsSourceContext combine(@NotNull SQLQueryRowsSourceContext other) {
        return this.setRowsSources(new HashMap<>() {
            {
                putAll(other.rowsSources);
                putAll(SQLQueryRowsSourceContext.this.rowsSources);
            }
        });
    }

    /**
     * Prepare new semantic context by introducing rows source
     */
    @NotNull
    public final SQLQueryRowsSourceContext appendSource(
        @NotNull SQLQueryRowsSourceModel source,
        @NotNull SQLQueryComplexName name,
        @Nullable DBSEntity tableOrNull
    ) {
        return this.setRowsSources(new HashMap<>() {
            {
                putAll(SQLQueryRowsSourceContext.this.rowsSources);
                put(name, new KnownRowsSourceInfo(source, name, tableOrNull, null));
            }
        });
    }

    /**
     * Associate alias with the resolved query source
     */
    @NotNull
    public final SQLQueryRowsSourceContext appendAlias(@NotNull SQLQueryRowsSourceModel source, @NotNull SQLQuerySymbol alias) {
        return this.setRowsSources(new HashMap<>() {
            {
                putAll(SQLQueryRowsSourceContext.this.rowsSources);

                KnownRowsSourceInfo entry = this.values().stream().filter(s -> s.source == source).findFirst().orElse(null);
                KnownRowsSourceInfo newEntry;
                if (entry != null) {
                    newEntry = new KnownRowsSourceInfo(entry.source, entry.referenceName, entry.tableOrNull, alias);
                    put(entry.referenceName, newEntry);
                } else {
                    newEntry = new KnownRowsSourceInfo(source, null, null, alias);
                }
                put(new SQLQueryComplexName(alias.getName()), newEntry);
            }
        });
    }

    /**
     * Prepare new semantic context by introducing rows source
     */
    @NotNull
    public final SQLQueryRowsSourceContext appendCteSources(@NotNull List<Pair<SQLQuerySymbolEntry, SQLQueryRowsSourceModel>> sources) {
        return this.setDynamicRowsSources(new HashMap<>() {
            {
                putAll(SQLQueryRowsSourceContext.this.rowsSources);
                for (Pair<SQLQuerySymbolEntry, ? extends SQLQueryRowsSourceModel> entry : sources) {
                    SQLQuerySymbolEntry alias = entry.getFirst();
                    SQLQueryRowsSourceModel sourceModel = entry.getSecond();
                    SQLQueryComplexName name = new SQLQueryComplexName(new SQLQueryQualifiedName(
                        alias.getSyntaxNode(), Collections.emptyList(), alias, 0, null
                    ));
                    put(name, new KnownRowsSourceInfo(sourceModel, name, null, null));
                }
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create empty data context
     */
    public SQLQueryRowsDataContext makeEmptyTuple() {
        return new SQLQueryRowsDataContext(
            this,
            Collections.emptyList(),
            Collections.emptyList()
        );
    }

    /**
     * Create row tuple model acting as a context for column references resolution
     */
    public SQLQueryRowsDataContext makeTuple(
        @NotNull List<SQLQueryResultColumn> columns,
        @NotNull List<SQLQueryResultPseudoColumn> pseudoColumns
    ) {
        return new SQLQueryRowsDataContext(this, columns, pseudoColumns);
    }

    /**
     * Create row tuple model acting as a context for column references resolution
     */
    public final SQLQueryRowsDataContext makeTuple(
        @Nullable SQLQueryRowsSourceModel source,
        @NotNull List<SQLQueryResultColumn> columns,
        @NotNull List<SQLQueryResultPseudoColumn> pseudoColumns
    ) {
        // TODO: review pseudoattributes behavior in DDL expressions (not handling for now)
        List<SQLQueryResultPseudoColumn> allPseudoColumns = source == null
            ? pseudoColumns
            : STMUtils.combineLists(this.connectionInfo.rowsetPseudoColumnsProvider.apply(source), pseudoColumns);
        return new SQLQueryRowsDataContext(this, columns, allPseudoColumns);
    }

    /**
     * Returns information about resolved sources with ability to separately provide tables and aliases used in the query
     */
    @NotNull
    public SQLQuerySourcesInfoCollection getKnownSources() {
        return new SQLQuerySourcesInfoCollection() {

            private final Map<SQLQueryRowsSourceModel, SourceResolutionResult> resolutionResults =
                new HashSet<>(rowsSources.values()).stream().collect(Collectors.toMap(s -> s.source, s -> s));

            private final Set<DBSObject> referencedTables = rowsSources.values().stream().map(s -> s.tableOrNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            private final Set<String> aliasesInUse = rowsSources.values().stream().map(s -> s.aliasOrNull)
                .filter(Objects::nonNull)
                .map(SQLQuerySymbol::getName)
                .collect(Collectors.toSet());

            @NotNull
            @Override
            public Map<SQLQueryRowsSourceModel, SourceResolutionResult> getResolutionResults() {
                return this.resolutionResults;
            }

            @NotNull
            @Override
            public Set<DBSObject> getReferencedTables() {
                return this.referencedTables;
            }

            @NotNull
            @Override
            public Set<String> getAliasesInUse() {
                return this.aliasesInUse;
            }
        };
    }

    @NotNull
    private SQLQueryRowsSourceContext setRowsSources(@NotNull Map<SQLQueryComplexName, KnownRowsSourceInfo> rowsSources) {
        return new SQLQueryRowsSourceContext(this.connectionInfo, this.hasUnresolvedSource, rowsSources, this.dynamicTableSources);
    }

    @NotNull
    private SQLQueryRowsSourceContext setDynamicRowsSources(@NotNull Map<SQLQueryComplexName, KnownRowsSourceInfo> dynamicTableSources) {
        return new SQLQueryRowsSourceContext(this.connectionInfo, this.hasUnresolvedSource, this.rowsSources, dynamicTableSources);
    }

}
