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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.ListNode;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SQLQueryRowsSourceContext {

    private static final Log log = Log.getLog(SQLQueryRowsSourceContext.class);

    @NotNull
    private final SQLQueryConnectionContext connectionInfo;

    private final boolean hasUnresolvedSource;

    @NotNull
    private final UnmodifiableMap<SQLQueryComplexName, SourceResolutionResult> rowsSources;

    @NotNull
    private final UnmodifiableMap<String, SourceResolutionResult> dynamicTableSources;

    @NotNull
    private final UnmodifiableMap<String, SourceResolutionResult> sourcesByLoweredAlias;

    @Nullable
    private ListNode<SQLQueryRowsSourceContext> targetRowContexts = null;

    @Nullable
    private ListNode<SQLQueryRowsDataContext> targetDataContexts = null;

    @Nullable
    private Supplier<SQLQueryRowsDataContext> relatedContextProvider = null;

    public SQLQueryRowsSourceContext(@NotNull SQLQueryConnectionContext connectionInfo) {
        this(connectionInfo, false, UnmodifiableMap.emptyMap(), null);
    }

    private SQLQueryRowsSourceContext(
        @NotNull SQLQueryConnectionContext connectionInfo,
        boolean hasUnresolvedSource,
        @NotNull UnmodifiableMap<String, SourceResolutionResult> dynamicTableSources,
        @Nullable Supplier<SQLQueryRowsDataContext> relatedContextProvider
    ) {
        this.connectionInfo = connectionInfo;
        this.hasUnresolvedSource = hasUnresolvedSource;
        this.rowsSources = UnmodifiableMap.emptyMap();
        this.dynamicTableSources = dynamicTableSources;
        this.sourcesByLoweredAlias = UnmodifiableMap.emptyMap();
        this.relatedContextProvider = relatedContextProvider;
    }

    private SQLQueryRowsSourceContext(
        @NotNull SQLQueryRowsSourceContext parent,
        boolean hasUnresolvedSource,
        @NotNull UnmodifiableMap<SQLQueryComplexName, SourceResolutionResult> rowsSources,
        @NotNull UnmodifiableMap<String, SourceResolutionResult> dynamicTableSources,
        @NotNull UnmodifiableMap<String, SourceResolutionResult> sourcesByLoweredAlias,
        @Nullable Supplier<SQLQueryRowsDataContext> relatedContextProvider
    ) {
        parent.registerConsumingContext(this);
        this.connectionInfo = parent.connectionInfo;
        this.hasUnresolvedSource = hasUnresolvedSource;
        this.rowsSources = rowsSources;
        this.dynamicTableSources = dynamicTableSources;
        this.sourcesByLoweredAlias = sourcesByLoweredAlias;
        this.relatedContextProvider = relatedContextProvider;
    }

    private void registerConsumingContext(@NotNull SQLQueryRowsSourceContext context) {
        this.targetRowContexts = ListNode.push(this.targetRowContexts, context);
    }

    void registerConsumingContext(@NotNull SQLQueryRowsDataContext context) {
        this.targetDataContexts = ListNode.push(this.targetDataContexts, context);
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
        return new SQLQueryRowsSourceContext(this.connectionInfo, false, this.dynamicTableSources, this.relatedContextProvider);
    }

    /**
     * Prepare new semantic context by hiding all the involved rows sources such as subqueries and table references and marking this context as having unresolved rowset references
     */
    @NotNull
    public final SQLQueryRowsSourceContext resetAsUnresolved() {
        return new SQLQueryRowsSourceContext(this.connectionInfo, true, this.dynamicTableSources, this.relatedContextProvider);
    }

    @NotNull
    public SQLQueryRowsSourceContext setRelatedContextProvider(@NotNull Supplier<SQLQueryRowsDataContext>  relatedContextProvider) {
        return new SQLQueryRowsSourceContext(
            this,
            this.hasUnresolvedSource,
            this.rowsSources,
            this.dynamicTableSources,
            this.sourcesByLoweredAlias,
            relatedContextProvider
        );
    }

    @Nullable
    public Supplier<SQLQueryRowsDataContext> getRelatedContextProvider() {
        return this.relatedContextProvider;
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
    public SourceResolutionResult findDynamicRowsSource(@NotNull SQLQuerySymbolEntry name) {
        return this.dynamicTableSources.get(name.getName().toLowerCase());
    }

    /**
     * Find semantic model item responsible for the representation of the data rows source having a given name
     * (table reference, named subquery, etc)
     *
     * @implNote TODO consider ambiguous table names
     */
    @Nullable
    public SourceResolutionInfo findReferencedSource(@NotNull SQLQueryComplexName name) {
        if (!name.parts.isEmpty()) {
            SQLQuerySymbolEntry entry = name.parts.getFirst();
            SourceResolutionResult result = this.findSourceByAlias(entry.getName());
            if (result != null) {
                SQLQueryMemberAccessEntry endingPeriod;
                if (name.parts.size() > 1 && name.parts.get(1) != null) {
                    endingPeriod = name.parts.get(1).getMemberAccess();
                } else if (name.parts.size() == 2 && name.parts.get(1) == null) {
                    endingPeriod = name.endingPeriodNode;
                } else {
                    endingPeriod = null;
                }
                SQLQueryComplexName key = new SQLQueryComplexName(entry.getSyntaxNode(), List.of(entry), 0, endingPeriod);
                return new SourceResolutionInfo(result, key);
            }
        }

        SQLQueryComplexName namePart = name;
        while (namePart != null) {
            SourceResolutionResult result = this.rowsSources.get(namePart);
            if (result != null) {
                return new SourceResolutionInfo(result, namePart);
            } else {
                namePart = namePart.trimEnd();
            }
        }
        return null;
    }

    public record SourceResolutionInfo(
        @NotNull SourceResolutionResult target,
        @NotNull SQLQueryComplexName key
    ) {
    }

    /**
     * Get the resolved query source by its name
     */
    @Nullable
    public SourceResolutionResult findReferencedSourceExact(@NotNull SQLQueryComplexName name) {
        if (name.stringParts.size() == 1 && name.invalidPartsCount == 0) {
            SourceResolutionResult result = this.findSourceByAlias(name.stringParts.getFirst());
            if (result != null) {
                return result;
            }
        }
        return this.rowsSources.get(name);
    }

    @Nullable
    private SourceResolutionResult findSourceByAlias(@NotNull String aliasName) {
        return this.sourcesByLoweredAlias.get(aliasName.toLowerCase());
    }

    /**
     * Prepare new semantic context by combining this context with the other given context
     */
    @NotNull
    public SQLQueryRowsSourceContext combine(@NotNull SQLQueryRowsSourceContext other) {
        return this.setRowsSources(
            this.rowsSources.combine(other.rowsSources),
            this.sourcesByLoweredAlias.combine(other.sourcesByLoweredAlias),
            this.dynamicTableSources.combine(other.dynamicTableSources),
            SQLQueryRowsSourceContext.this.hasUnresolvedSource || other.hasUnresolvedSource,
            null
        );
    }

    /**
     * Prepare new semantic context by introducing rows source
     */
    @NotNull
    public final SQLQueryRowsSourceContext appendSource(
        @NotNull SQLQueryRowsSourceModel source,
        @NotNull SQLQueryComplexName classifiedName,
        @Nullable DBSEntity tableOrNull
    ) {
        SourceResolutionResult srr = new SourceResolutionResult(source, classifiedName, tableOrNull, null);

        ArrayList<Map.Entry<SQLQueryComplexName, SourceResolutionResult>> newSourceEntries = new ArrayList<>(5);
        newSourceEntries.add(Map.entry(classifiedName, srr));

        if (tableOrNull != null && classifiedName.parts.getFirst().getDefinition() instanceof SQLQuerySymbolByDbObjectDefinition subparent) {
            for (SQLQueryComplexName nameFragment = classifiedName.trimStart(); nameFragment != null; nameFragment = nameFragment.trimStart()) {
                newSourceEntries.add(Map.entry(nameFragment, srr));
            }
            SQLQueryComplexName synthesizedName = classifiedName;
            for (DBSObject o = subparent.getDbObject().getParentObject(); o != null && !(o instanceof DBPDataSource); o = o.getParentObject()) {
                String canonicalName = SQLUtils.identifierToCanonicalForm(this.connectionInfo.dialect, o.getName(), false, true);
                SQLQuerySymbolEntry entry = new SQLQuerySymbolEntry(classifiedName.syntaxNode, canonicalName, o.getName(), null);
                entry.setDefinition(new SQLQuerySymbolByDbObjectDefinition(o, SQLQuerySemanticUtils.inferSymbolClass(o)));
                synthesizedName = synthesizedName.prepend(entry);
                newSourceEntries.add(Map.entry(synthesizedName, srr));
            }
        }

        return this.setRowsSources(
            this.rowsSources.put(newSourceEntries),
            this.sourcesByLoweredAlias,
            this.dynamicTableSources,
            this.hasUnresolvedSource,
            this.relatedContextProvider
        );
    }

    /**
     * Associate alias with the resolved query source
     */
    @NotNull
    public final SQLQueryRowsSourceContext replaceWithAlias(
        @NotNull SQLQueryRowsSourceModel oldSource,
        @NotNull SQLQueryRowsSourceModel newSource,
        @NotNull SQLQuerySymbolEntry alias
    ) {
        List<Map.Entry<SQLQueryComplexName, SourceResolutionResult>> oldEntries = this.rowsSources.entrySet().stream()
            .filter(s -> s.getValue().source == oldSource).toList();

        DBSEntity oldEntryTable = oldEntries.isEmpty() ? null : oldEntries.getFirst().getValue().tableOrNull;
        SourceResolutionResult newEntry = new SourceResolutionResult(newSource, null, oldEntryTable, alias.getSymbol());

        return this.setRowsSources(
            this.rowsSources.remove(oldEntries),
            this.sourcesByLoweredAlias.put(alias.getName().toLowerCase(), newEntry),
            this.dynamicTableSources,
            this.hasUnresolvedSource,
            this.relatedContextProvider
        );
    }

    /**
     * Prepare new semantic context by introducing rows source
     */
    @NotNull
    public final SQLQueryRowsSourceContext appendCteSources(@NotNull List<Pair<SQLQuerySymbolEntry, SQLQueryRowsSourceModel>> sources) {
        ArrayList<Map.Entry<String, SourceResolutionResult>> newSourceEntries = new ArrayList<>(sources.size());
        for (Pair<SQLQuerySymbolEntry, ? extends SQLQueryRowsSourceModel> entry : sources) {
            SQLQuerySymbolEntry alias = entry.getFirst();
            if (alias != null) {
                SQLQueryRowsSourceModel sourceModel = entry.getSecond();
                SQLQueryComplexName name = new SQLQueryComplexName(alias.getSyntaxNode(), List.of(alias), 0, null);
                newSourceEntries.add(Map.entry(
                    alias.getName().toLowerCase(),
                    new SourceResolutionResult(sourceModel, name, null, alias.getSymbol())
                ));
            }
        }
        return this.setDynamicRowsSources(this.dynamicTableSources.put(newSourceEntries));
    }

    @NotNull
    public final SQLQueryRowsSourceContext setCteSourcesFrom(@NotNull SQLQueryRowsSourceContext context) {
        return this.setDynamicRowsSources(context.dynamicTableSources);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create empty data context
     */
    @NotNull
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
    @NotNull
    public SQLQueryRowsDataContext makeTuple(
        @NotNull List<SQLQueryResultColumn> columns,
        @NotNull List<SQLQueryResultPseudoColumn> pseudoColumns
    ) {
        return new SQLQueryRowsDataContext(this, columns, pseudoColumns);
    }

    /**
     * Create row tuple model acting as a context for column references resolution
     */
    @NotNull
    public final SQLQueryRowsDataContext makeTuple(
        @Nullable SQLQueryRowsSourceModel source,
        @NotNull List<SQLQueryResultColumn> columns,
        @NotNull List<SQLQueryResultPseudoColumn> pseudoColumns
    ) {
        // TODO: review pseudoattributes behavior in DDL expressions (not handling for now)
        List<SQLQueryResultPseudoColumn> allPseudoColumns = source == null
            ? pseudoColumns
            : STMUtils.combineLists(this.connectionInfo.obtainRowsetPseudoColumns(source), pseudoColumns);
        return new SQLQueryRowsDataContext(this, columns, allPseudoColumns);
    }

    /**
     * Create row tuple model acting as a context for column references resolution
     */
    @NotNull
    public final SQLQueryRowsDataContext makeTuple(
        @Nullable SQLQueryRowsSourceModel source,
        @NotNull Pair<List<SQLQueryResultColumn>, List<SQLQueryResultPseudoColumn>> columnsAndPseudoColumns
    ) {
        return this.makeTuple(source, columnsAndPseudoColumns.getFirst(), columnsAndPseudoColumns.getSecond());
    }

    /**
     * Create row tuple model acting as a context for column references resolution
     */
    @NotNull
    public SQLQueryRowsDataContext makeJoinTuple(
        @NotNull List<SQLQueryResultColumn> columns,
        @NotNull List<SQLQueryResultPseudoColumn> pseudoColumns,
        @NotNull SQLQueryRowsDataContext.JoinInfo joinInfo
    ) {
        return new SQLQueryRowsDataContext(this, columns, pseudoColumns, joinInfo);
    }

    /**
     * Returns information about resolved sources with ability to separately provide tables and aliases used in the query
     */
    @NotNull
    public SQLQuerySourcesInfoCollection getKnownSources(boolean forQuerySubscope) {
        ListNode<SQLQueryRowsSourceContext> queue = ListNode.of(this);
        Set<SQLQueryRowsSourceContext> queued = new HashSet<>();
        queued.add(this);

        Set<SourceResolutionResult>  allSourceResolutions = new HashSet<>();

        while (queue != null) {
            SQLQueryRowsSourceContext source = queue.data;
            queue = queue.next;

            allSourceResolutions.addAll(source.rowsSources.values());
            allSourceResolutions.addAll(source.sourcesByLoweredAlias.values());
            allSourceResolutions.addAll(source.dynamicTableSources.values());

            if (forQuerySubscope) {
                for (ListNode<SQLQueryRowsSourceContext> item = source.targetRowContexts; item != null; item = item.next) {
                    if (queued.add(item.data)) {
                        queue = ListNode.push(queue, item.data);
                    }
                }
            }
        }

        return new SQLQuerySourcesInfoCollection() {
            // combine inferred sources (from the underlying query expression) and dynamically provided (from the enclosing CTE)
            private final Map<SQLQueryRowsSourceModel, SourceResolutionResult> resolutionResults =
                allSourceResolutions.stream().collect(Collectors.toMap(s -> s.source, Function.identity()));

            @NotNull
            private final Set<DBSEntity> referencedTables = allSourceResolutions.stream()
                .map(s -> s.tableOrNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            private final Set<String> aliasesInUse = allSourceResolutions.stream()
                .map(s -> s.aliasOrNull)
                .filter(Objects::nonNull)
                .map(SQLQuerySymbol::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

            @NotNull
            @Override
            public Map<SQLQueryRowsSourceModel, SourceResolutionResult> getResolutionResults() {
                return this.resolutionResults;
            }

            @NotNull
            @Override
            public Set<DBSEntity> getReferencedTables() {
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
    private SQLQueryRowsSourceContext setRowsSources(
        @NotNull UnmodifiableMap<SQLQueryComplexName, SourceResolutionResult> rowsSources,
        @NotNull UnmodifiableMap<String, SourceResolutionResult> sourcesByLoweredAlias,
        @NotNull UnmodifiableMap<String, SourceResolutionResult> dynamicTableSources,
        boolean hasUnresolvedSource,
        @Nullable Supplier<SQLQueryRowsDataContext> relatedContextProvider
    ) {
        return new SQLQueryRowsSourceContext(
            this,
            hasUnresolvedSource,
            rowsSources,
            dynamicTableSources,
            sourcesByLoweredAlias,
            relatedContextProvider
        );
    }

    @NotNull
    private SQLQueryRowsSourceContext setDynamicRowsSources(@NotNull UnmodifiableMap<String, SourceResolutionResult> dynamicTableSources) {
        return new SQLQueryRowsSourceContext(
            this,
            this.hasUnresolvedSource,
            this.rowsSources,
            dynamicTableSources,
            this.sourcesByLoweredAlias,
            this.relatedContextProvider
        );
    }

}
