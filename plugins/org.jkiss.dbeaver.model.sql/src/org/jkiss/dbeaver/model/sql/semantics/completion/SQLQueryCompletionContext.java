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
package org.jkiss.dbeaver.model.sql.semantics.completion;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SQLQueryCompletionContext {

    private static final Log log = Log.getLog(SQLQueryCompletionContext.class);

    private static final Set<String> statementStartKeywords = LSMInspections.prepareOffquerySyntaxInspection().predictedWords;
    private static final Collection<SQLQueryCompletionItem> statementStartKeywordCompletions = statementStartKeywords.stream().sorted().map(SQLQueryCompletionItem::forReservedWord).toList();
    private static final int statementStartKeywordMaxLength = statementStartKeywords.stream().mapToInt(String::length).max().orElse(0);

    /**
     * Returns maximum length of all keywords
     */
    public static int getMaxKeywordLength() {
        return statementStartKeywordMaxLength;
    }

    /**
     * Empty completion context which always provides no completion items
     */
    public static final SQLQueryCompletionContext EMPTY = new SQLQueryCompletionContext(0) {

        @Nullable
        @Override
        public SQLQueryDataContext getDataContext() {
            return null;
        }

        @NotNull
        @Override
        public LSMInspections.SyntaxInspectionResult getInspectionResult() {
            return LSMInspections.SyntaxInspectionResult.EMPTY;
        }

        @NotNull
        @Override
        public SQLQueryCompletionSet prepareProposal(
            @NotNull DBRProgressMonitor monitor,
            int position,
            @NotNull SQLCompletionRequest request
        ) {
            return new SQLQueryCompletionSet(position, 0, Collections.emptyList());
        }
    };

    /**
     * Prepare completion context for the script item at given offset treating current position as outside-of-query
     */
    @NotNull
    public static SQLQueryCompletionContext prepareOffquery(int scriptItemOffset) {
        return new SQLQueryCompletionContext(scriptItemOffset) {
            private static final LSMInspections.SyntaxInspectionResult syntaxInspectionResult = LSMInspections.prepareOffquerySyntaxInspection();


            @Nullable
            @Override
            public SQLQueryDataContext getDataContext() {
                return null;
            }

            @NotNull
            @Override
            public LSMInspections.SyntaxInspectionResult getInspectionResult() {
                return syntaxInspectionResult;
            }

            @NotNull
            @Override
            public SQLQueryCompletionSet prepareProposal(
                @NotNull DBRProgressMonitor monitor,
                int position,
                @NotNull SQLCompletionRequest request
            ) {
                return new SQLQueryCompletionSet(position, 0, statementStartKeywordCompletions);
            }
        };
    }

    private final int scriptItemOffset;

    private SQLQueryCompletionContext(int scriptItemOffset) {
        this.scriptItemOffset = scriptItemOffset;
    }

    public int getOffset() {
        return this.scriptItemOffset;
    }

    @Nullable
    public abstract SQLQueryDataContext getDataContext();

    @NotNull
    public abstract LSMInspections.SyntaxInspectionResult getInspectionResult();

    @NotNull
    public Set<String> getAliasesInUse() {
        return Collections.emptySet();
    }

    /**
     * Prepare a set of completion proposal items for a given position in the text of the script item
     */
    @NotNull
    public abstract SQLQueryCompletionSet prepareProposal(
        @NotNull DBRProgressMonitor monitor,
        int position,
        @NotNull SQLCompletionRequest request
    );

    /**
     * Prepare completion context for the script item in the given contexts (execution, syntax and semantics)
     */
    public static SQLQueryCompletionContext prepare(
        @NotNull SQLScriptItemAtOffset scriptItem,
        @Nullable DBCExecutionContext dbcExecutionContext,
        @NotNull LSMInspections.SyntaxInspectionResult syntaxInspectionResult,
        @NotNull SQLQueryDataContext context,
        @Nullable SQLQueryLexicalScopeItem lexicalItem,
        @NotNull STMTreeTermNode[] nameNodes
    ) {
        return new SQLQueryCompletionContext(scriptItem.offset) {
            private final Map<SQLQueryRowsSourceModel, SourceResolutionResult> referencedSources
                = context.collectKnownSources().getResolutionResults();
            private Set<String> aliasesInUse = null;

            @NotNull
            @Override
            public SQLQueryDataContext getDataContext() {
                return context;
            }

            @NotNull
            @Override
            public LSMInspections.SyntaxInspectionResult getInspectionResult() {
                return syntaxInspectionResult;
            }

            @NotNull
            @Override
            public Set<String> getAliasesInUse() {
                if (this.aliasesInUse == null) {
                    this.aliasesInUse = this.referencedSources.values().stream()
                        .map(srr -> srr.aliasOrNull)
                        .filter(Objects::nonNull)
                        .map(SQLQuerySymbol::getName)
                        .collect(Collectors.toSet());
                }
                return this.aliasesInUse;
            }

            @NotNull
            @Override
            public SQLQueryCompletionSet prepareProposal(
                @NotNull DBRProgressMonitor monitor,
                int position,
                @NotNull SQLCompletionRequest request
            ) {
                position -= this.getOffset();
                
                String currentWord = this.obtainCurrentWord(position);
                
                final List<SQLQueryCompletionItem> keywordCompletions = 
                    nameNodes.length > 1
                        ? Collections.emptyList()
                        : prepareKeywordCompletions(syntaxInspectionResult.predictedWords, currentWord);
                
                List<SQLQueryCompletionItem> columnRefCompletions = 
                    syntaxInspectionResult.expectingColumnReference && nameNodes.length == 0
                        ? this.prepareColumnCompletions(null) : Collections.emptyList();
                
                List<SQLQueryCompletionItem> tableRefCompletions = 
                    syntaxInspectionResult.expectingTableReference && nameNodes.length == 0
                        ? this.prepareTableCompletions(monitor, request) : Collections.emptyList();
                
                List<SQLQueryCompletionItem> lexicalItemCompletions = 
                    lexicalItem != null ? this.prepareLexicalItemCompletions(monitor, lexicalItem, position) :
                    syntaxInspectionResult.expectingIdentifier || nameNodes.length > 0
                        ? this.prepareIdentifierCompletions(monitor, position)
                        : Collections.emptyList();
                
                List<SQLQueryCompletionItem> completionItems = Stream.of(
                        columnRefCompletions, tableRefCompletions, lexicalItemCompletions, keywordCompletions
                ).flatMap(Collection::stream).collect(Collectors.toList());
                
                int replacementLength = currentWord == null ? 0 : currentWord.length();
                return new SQLQueryCompletionSet(position - replacementLength, replacementLength, completionItems);
            }

            @Nullable
            private String obtainCurrentWord(int position) {
                if (nameNodes.length == 0) {
                    return null;
                }
                STMTreeTermNode lastNode = nameNodes[nameNodes.length - 1];
                Interval wordRange = lastNode.getRealInterval();
                if (wordRange.b >= position - 1 && lastNode.symbol.getType() != SQLStandardLexer.Period) {
                    return lastNode.getTextContent().substring(0, position - lastNode.getRealInterval().a);
                } else {
                    return null;
                }
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareIdentifierCompletions(@NotNull DBRProgressMonitor monitor, int position) {
                List<String> parts = this.obtainIdentifierParts(position);
                return this.prepareIdentifierCompletions(monitor, parts, null);
            }
            
            private List<SQLQueryCompletionItem> prepareIdentifierCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull List<String> parts, Class<?> componentType
            ) {
                List<String> prefix = parts.subList(0, parts.size() - 1);
                String tail = parts.get(parts.size() - 1);

                List<SQLQueryCompletionItem> result;
                if (syntaxInspectionResult.expectingColumnReference) {
                    result = this.accomplishColumnReference(prefix, tail);
                } else if (syntaxInspectionResult.expectingTableReference) {
                    result = this.accomplishTableReference(monitor, componentType, prefix, tail);
                } else {
                    result = Collections.emptyList();
                }

                return result;
            }

            @NotNull
            private List<SQLQueryCompletionItem> accomplishTableReference(
                @NotNull DBRProgressMonitor monitor,
                @NotNull Class<?> componentType,
                @NotNull List<String> prefix,
                @NotNull String tail
            ) {
                if (dbcExecutionContext == null || dbcExecutionContext.getDataSource() == null || !DBStructUtils.isConnectedContainer(dbcExecutionContext.getDataSource())) {
                    return Collections.emptyList();
                } else {
                    DBSObject prefixContext = prefix.size() == 0
                        ? DBUtils.getSelectedObject(dbcExecutionContext)
                        : SQLSearchUtils.findObjectByFQN(
                            monitor,
                            (DBSObjectContainer) dbcExecutionContext.getDataSource(),
                            dbcExecutionContext,
                            prefix,
                            false,
                            new SQLIdentifierDetector(dbcExecutionContext.getDataSource().getSQLDialect())
                        );
                    return prefixContext == null
                        ? Collections.emptyList()
                        : this.prepareObjectComponentCompletions(monitor, prefixContext, tail, componentType);
                }
            }

            @NotNull
            private List<SQLQueryCompletionItem> accomplishColumnReference(@NotNull List<String> prefix, @NotNull String tail) {
                if (prefix.size() == 1) { // table-ref-prefixed column
                    String mayBeAliasName = prefix.get(0).toLowerCase();
                    SourceResolutionResult srr = this.referencedSources.values().stream()
                        .filter(rr -> rr.aliasOrNull != null && rr.aliasOrNull.getName().toLowerCase().contains(mayBeAliasName))
                        .findFirst().orElse(null);
                    if (srr != null) { // alias resolved, propose associated columns
                        return srr.source.getResultDataContext().getColumnsList().stream()
                            .filter(c -> c.symbol.getName().toLowerCase().contains(tail))
                            .map(c -> SQLQueryCompletionItem.forSubsetColumn(c, srr, false))
                            .toList();
                    }
                } else if (prefix.size() == 0) { // table-ref not introduced yet or non-prefixed column, so try both cases
                    return this.prepareColumnCompletions(tail);
                }
                return Collections.emptyList();
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareObjectComponentCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObject object,
                @NotNull String componentNamePart, Class<?> componentType
            ) {
                try {
                    Stream<? extends DBSObject> components;
                    if (object instanceof DBSEntity entity) {
                        List<? extends DBSEntityAttribute> attrs = entity.getAttributes(monitor);
                        if (attrs != null) {
                            components = attrs.stream();
                        } else {
                            components = Stream.empty();
                        }
                    } else if (object instanceof DBSObjectContainer container && DBStructUtils.isConnectedContainer(container)) {
                        components = container.getChildren(monitor).stream();
                    } else {
                        components = Stream.empty();
                    }

                    return components.filter(a -> (componentType == null || componentType.isInstance(a)) && a.getName().toLowerCase().contains(componentNamePart))
                        .map(SQLQueryCompletionItem::forDbObject)
                        .toList();
                } catch (DBException ex) {
                    return Collections.emptyList();
                }
            }

            private List<String> obtainIdentifierParts(int position) {
                List<String> parts = new ArrayList<>(nameNodes.length);
                int i = 0;
                for (; i < nameNodes.length; i++) {
                    STMTreeTermNode term = nameNodes[i];
                    if (term.symbol.getType() != SQLStandardLexer.Period) {
                        if (term.getRealInterval().b + 1 < position) {
                            parts.add(term.getTextContent().toLowerCase());
                        } else {
                            break;
                        }
                    }
                }
                STMTreeTermNode currentNode = i >= nameNodes.length ? null : nameNodes[i];
                String currentPart = currentNode == null
                    ? ""
                    : currentNode.getTextContent().substring(0, position - currentNode.getRealInterval().a);
                parts.add(currentPart.toLowerCase());
                return parts;
            }

            private SQLQuerySymbolDefinition unrollSymbolDefinition(SQLQuerySymbolDefinition def) {
                while (def instanceof SQLQuerySymbolEntry entry) {
                    def = entry.getDefinition();
                }
                return def;
            }

            private List<SQLQueryCompletionItem> prepareLexicalItemCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLQueryLexicalScopeItem lexicalItem,
                int position
            ) {
                Interval pos = Interval.of(position, position);
                // TODO fix scopes to resolve current lexical item properly when its possible,
                //      then reuse what already propagated through the model
                if (lexicalItem instanceof SQLQueryQualifiedName qname) {
                    Interval nameRange;
                    Interval schemaRange;
                    Interval catalogRange;
                    if ((nameRange = qname.entityName.getSyntaxNode().getRealInterval()).properlyContains(pos)) {
                        String part = qname.entityName.getRawName().substring(0, position - nameRange.a);
                        if (qname.schemaName != null) {
                            SQLQuerySymbolDefinition scopeDef = this.unrollSymbolDefinition(qname.schemaName.getDefinition());
                            if (scopeDef instanceof SQLQuerySymbolByDbObjectDefinition byObjDef) {
                                return this.prepareObjectComponentCompletions(monitor, byObjDef.getDbObject(), part, DBSEntity.class);
                            } else {
                                // schema was not resolved, so cannot accomplish its subitems
                                return Collections.emptyList();
                            }
                        } else {
                            return this.prepareIdentifierCompletions(monitor, List.of(part), DBSEntity.class);
                        }
                    } else if (qname.schemaName != null
                        && (schemaRange = qname.schemaName.getSyntaxNode().getRealInterval()).properlyContains(pos)
                    ) {
                        String part = qname.schemaName.getRawName().substring(0, position - schemaRange.a);
                        if (qname.catalogName != null) {
                            SQLQuerySymbolDefinition scopeDef = this.unrollSymbolDefinition(qname.schemaName.getDefinition());
                            if (scopeDef instanceof SQLQuerySymbolByDbObjectDefinition byObjDef) {
                                return this.prepareObjectComponentCompletions(monitor, byObjDef.getDbObject(), part, DBSSchema.class);
                            } else {
                                // catalog was not resolved, so cannot accomplish schema
                                return Collections.emptyList();
                            }
                        } else {
                            return this.prepareObjectComponentCompletions(
                                monitor,
                                dbcExecutionContext.getDataSource(),
                                part,
                                DBSSchema.class
                            );
                        }
                    } else if (qname.catalogName != null
                        && (catalogRange = qname.catalogName.getSyntaxNode().getRealInterval()).properlyContains(pos)
                    ) {
                        String part = qname.catalogName.getRawName().substring(0, position - catalogRange.a);
                        return this.prepareObjectComponentCompletions(monitor, dbcExecutionContext.getDataSource(), part, DBSCatalog.class);
                    } else {
                        throw new UnsupportedOperationException("Illegal SQLQueryQualifiedName");
                    }
                } else if (lexicalItem instanceof SQLQuerySymbolEntry entry) {
                    Interval nameRange = entry.getSyntaxNode().getRealInterval();
                    String part = entry.getRawName().substring(0, position - nameRange.a);
                    return this.prepareIdentifierCompletions(monitor, List.of(part), null);
                } else {
                    throw new UnsupportedOperationException("Unexpected lexical item kind to complete " + lexicalItem.getClass().getName());
                }
            }
            
            private List<SQLQueryCompletionItem> prepareKeywordCompletions(Set<String> keywords, String filter) {
                Stream<String> stream = keywords.stream();
                if (filter != null) {
                    stream = stream.filter(s -> s.toLowerCase().contains(filter));
                }
                return stream.map(SQLQueryCompletionItem::forReservedWord).toList();
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareColumnCompletions(@Nullable String filterOrNull) {
                // directly available column
                List<SQLQueryCompletionItem> subsetColumns = context.getColumnsList().stream()
                    .filter(c -> filterOrNull == null || c.symbol.getName().toLowerCase().contains(filterOrNull))
                    .map(rc -> SQLQueryCompletionItem.forSubsetColumn(rc, this.referencedSources.get(rc.source), true))
                    .toList();
                // already referenced tables
                LinkedList<SQLQueryCompletionItem> tableRefs = new LinkedList<>();
                for (SourceResolutionResult rr : this.referencedSources.values()) {
                    if (rr.aliasOrNull != null && !rr.isCteSubquery) {
                        if (filterOrNull == null || rr.aliasOrNull.getName().toLowerCase().contains(filterOrNull)) {
                            tableRefs.add(SQLQueryCompletionItem.forSubqueryAlias(rr.aliasOrNull, rr.source));
                        }
                    } else if (rr.tableOrNull != null) {
                        if (filterOrNull == null || rr.tableOrNull.getName().toLowerCase().contains(filterOrNull)) {
                            tableRefs.add(SQLQueryCompletionItem.forRealTable(rr.tableOrNull, true));
                        }
                    }
                }
                return Stream.of(subsetColumns, tableRefs).flatMap(Collection::stream).toList();
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareTableCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request
            ) {
                Set<DBSObject> alreadyReferencedObjects = new HashSet<>();
                
                LinkedList<SQLQueryCompletionItem> completions = new LinkedList<>();
                for (SourceResolutionResult rr : this.referencedSources.values()) {
                    if (rr.aliasOrNull != null && rr.isCteSubquery) {
                        completions.add(SQLQueryCompletionItem.forSubqueryAlias(rr.aliasOrNull, rr.source));
                    }
                    if (rr.tableOrNull != null) {
                        alreadyReferencedObjects.add(rr.tableOrNull);
                    }
                }

                if (dbcExecutionContext != null && dbcExecutionContext.getDataSource() != null) {
                    try {
                        DBCExecutionContextDefaults<?,?> defaults = dbcExecutionContext.getContextDefaults();
                        if (defaults != null) {
                            DBSSchema defaultSchema = defaults.getDefaultSchema();
                            DBSCatalog defaultCatalog = defaults.getDefaultCatalog();
                            if (request.getContext().isSearchGlobally() && defaultCatalog != null) {
                                this.collectTables(monitor, defaultCatalog, alreadyReferencedObjects, completions);
                            } else {
                                if (defaultSchema != null) {
                                    this.collectTables(monitor, defaultSchema, alreadyReferencedObjects, completions);
                                }
                            }
                            if (defaultCatalog != null) {
                                this.collectSchemas(monitor, defaultCatalog, completions);
                            }
                        }

                        if (dbcExecutionContext.getDataSource() instanceof DBSObjectContainer container) {
                            this.collectCatalogs(monitor, container, completions);
                        }
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                
                return completions;
            }
            

            private void collectTables(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull Set<DBSObject> alreadyReferencedTables,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                this.collectObjectsRecursively(
                    monitor, container, new HashSet<>(), accumulator,
                    List.of(DBSTable.class, DBSView.class), o -> SQLQueryCompletionItem.forRealTable(o, alreadyReferencedTables.contains(o))
                );
            }

            private void collectSchemas(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                this.collectObjectsRecursively(
                    monitor, container, new HashSet<>(), accumulator,
                    List.of(DBSSchema.class), SQLQueryCompletionItem::forDbObject
                );
            }

            private void collectCatalogs(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                this.collectObjectsRecursively(
                    monitor, container, new HashSet<>(), accumulator,
                    List.of(DBSCatalog.class), SQLQueryCompletionItem::forDbObject
                );
            }

            @NotNull
            private <T extends DBSObject> void collectObjectsRecursively(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull Set<DBSObject> alreadyReferencedObjects,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator,
                @NotNull List<Class<? extends T>> types,
                @NotNull Function<T, SQLQueryCompletionItem> completionItemFabric
            ) throws DBException {
                Collection<? extends DBSObject> children = container.getChildren(monitor);
                for (DBSObject child : children) {
                    if (!DBUtils.isHiddenObject(child)) {
                        if (types.stream().anyMatch(t -> t.isInstance(child)) && !alreadyReferencedObjects.contains(child)) {
                            accumulator.add(completionItemFabric.apply((T) child));
                        } else if (child instanceof DBSObjectContainer sc && DBStructUtils.isConnectedContainer(child)) {
                            collectObjectsRecursively(monitor, sc, alreadyReferencedObjects, accumulator, types, completionItemFabric);
                        }
                    }
                }
            }
        };
    }
}
