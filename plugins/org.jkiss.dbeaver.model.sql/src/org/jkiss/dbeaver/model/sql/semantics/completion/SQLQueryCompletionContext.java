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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SQLQueryCompletionContext {

    private static final Log log = Log.getLog(SQLQueryCompletionContext.class);

    private static final Set<String> statementStartKeywords = LSMInspections.prepareOffquerySyntaxInspection().predictedWords;
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
            private static Pattern KEYWORD_FILTER_PATTERN = Pattern.compile("[a-zA-Z0-9]+$");

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
                int lineStartOffset;
                String lineHead;
                try {
                    IDocument doc = request.getDocument();
                    int line = doc.getLineOfOffset(position);
                    lineStartOffset = doc.getLineOffset(line);
                    lineHead = doc.get(lineStartOffset, position - lineStartOffset);
                } catch (BadLocationException ex) {
                    lineHead = "";
                    lineStartOffset = -1;
                }

                Matcher m = KEYWORD_FILTER_PATTERN.matcher(lineHead);
                Collection<SQLQueryCompletionItem> keywordCompletions;
                int replacementLength;
                if (m.find()) {
                    String filterKeyString = lineHead.substring(m.start(), m.end()).toLowerCase();
                    int filterStart = m.start() + lineStartOffset;
                    replacementLength = filterKeyString.length();
                    keywordCompletions = statementStartKeywords.stream()
                        .map(s -> SQLQueryCompletionItem.forReservedWord(new SQLQueryWordEntry(filterStart, s.toLowerCase()), s))
                        .filter(c -> c.getFilterInfo().string().contains(filterKeyString))
                        .toList();
                } else {
                    replacementLength = 0;
                    keywordCompletions = statementStartKeywords.stream()
                        .map(s -> SQLQueryCompletionItem.forReservedWord(new SQLQueryWordEntry(-1, s.toLowerCase()), s))
                        .toList();
                }

                return new SQLQueryCompletionSet(position - replacementLength, replacementLength, keywordCompletions);
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
     * Returns contexts participating in identifiers resolution
     */
    @NotNull
    public Set<DBSObjectContainer> getExposedContexts() {
        return Collections.emptySet();
    }

    @NotNull
    public List<SQLQueryCompletionItem> prepareCurrentTupleColumns() {
        return Collections.emptyList();
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

    @NotNull
    protected SQLQueryWordEntry makeFilterInfo(@Nullable SQLQueryWordEntry filterKey, @NotNull String filterString) {
        return new SQLQueryWordEntry(filterKey == null ? -1 : (this.getOffset() + filterKey.offset()), filterString);
    }

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
            private final Set<DBSObjectContainer> exposedContexts = SQLQueryCompletionContext.obtainExposedContexts(dbcExecutionContext);
            private final SQLQueryDataContext.KnownSourcesInfo knownSources = context.collectKnownSources();

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
                return this.knownSources.getAliasesInUse();
            }

            @NotNull
            @Override
            public Set<DBSObjectContainer> getExposedContexts() {
                return this.exposedContexts;
            }

            @NotNull
            @Override
            public SQLQueryCompletionSet prepareProposal(
                @NotNull DBRProgressMonitor monitor,
                int position,
                @NotNull SQLCompletionRequest request
            ) {
                position -= this.getOffset();
                
                SQLQueryWordEntry currentWord = this.obtainCurrentWord(position);
                
                final List<SQLQueryCompletionItem> keywordCompletions = prepareKeywordCompletions(syntaxInspectionResult.predictedWords, currentWord);

                List<SQLQueryCompletionItem> columnRefCompletions = syntaxInspectionResult.expectingColumnReference && nameNodes.length == 0
                    ? this.prepareColumnCompletions(null)
                    : Collections.emptyList();

                List<SQLQueryCompletionItem> tableRefCompletions = syntaxInspectionResult.expectingTableReference && nameNodes.length == 0
                    ? this.prepareTableCompletions(monitor, request)
                    : Collections.emptyList();
                
                List<SQLQueryCompletionItem> lexicalItemCompletions = lexicalItem != null
                    ? this.prepareLexicalItemCompletions(monitor, request, lexicalItem, position)
                    : syntaxInspectionResult.expectingIdentifier || nameNodes.length > 0
                        ? this.prepareIdentifierCompletions(monitor, request, position)
                        : Collections.emptyList();
                
                List<SQLQueryCompletionItem> completionItems = Stream.of(
                    columnRefCompletions, tableRefCompletions, lexicalItemCompletions, keywordCompletions)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
                
                int replacementLength = currentWord == null ? 0 : currentWord.string().length();
                return new SQLQueryCompletionSet(this.getOffset() + position - replacementLength, replacementLength, completionItems);
            }

            @Nullable
            private SQLQueryWordEntry obtainCurrentWord(int position) {
                if (nameNodes.length == 0) {
                    return null;
                }
                STMTreeTermNode lastNode = nameNodes[nameNodes.length - 1];
                Interval wordRange = lastNode.getRealInterval();
                if (wordRange.b >= position - 1 && lastNode.symbol.getType() != SQLStandardLexer.Period) {
                    return new SQLQueryWordEntry(wordRange.a, lastNode.getTextContent().substring(0, position - lastNode.getRealInterval().a).toLowerCase());
                } else {
                    return null;
                }
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareIdentifierCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                int position
            ) {
                List<SQLQueryWordEntry> parts = this.obtainIdentifierParts(position);
                return this.prepareIdentifierCompletions(monitor, request, parts, null);
            }
            
            private List<SQLQueryCompletionItem> prepareIdentifierCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull List<SQLQueryWordEntry> parts, Class<?> componentType
            ) {
                List<SQLQueryWordEntry> prefix = parts.subList(0, parts.size() - 1);
                SQLQueryWordEntry tail = parts.get(parts.size() - 1);

                List<SQLQueryCompletionItem> result;
                if (syntaxInspectionResult.expectingColumnReference) {
                    result = this.accomplishColumnReference(prefix, tail);
                } else if (syntaxInspectionResult.expectingTableReference) {
                    result = this.accomplishTableReference(monitor, request, componentType, prefix, tail);
                } else {
                    result = Collections.emptyList();
                }

                return result;
            }

            @NotNull
            private List<SQLQueryCompletionItem> accomplishTableReference(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull Class<?> componentType,
                @NotNull List<SQLQueryWordEntry> prefix,
                @NotNull SQLQueryWordEntry tail
            ) {
                if (dbcExecutionContext == null || dbcExecutionContext.getDataSource() == null || !DBStructUtils.isConnectedContainer(dbcExecutionContext.getDataSource())) {
                    return Collections.emptyList();
                } else {
                    List<String> contextName = prefix.stream().map(e -> e.string()).collect(Collectors.toList());
                    DBSObject prefixContext = prefix.size() == 0
                        ? DBUtils.getSelectedObject(dbcExecutionContext)
                        : SQLSearchUtils.findObjectByFQN(
                            monitor,
                            (DBSObjectContainer) dbcExecutionContext.getDataSource(),
                            dbcExecutionContext,
                            contextName,
                            false,
                            request.getWordDetector()
                        );
                    return prefixContext == null
                        ? Collections.emptyList()
                        : this.prepareObjectComponentCompletions(
                            monitor, prefixContext, tail, componentType,
                            (e, x) -> SQLQueryCompletionItem.forRealTable(e, (DBSEntity) x, knownSources.getReferencedTables().contains(x))
                        );
                }
            }

            @NotNull
            private List<SQLQueryCompletionItem> accomplishColumnReference(@NotNull List<SQLQueryWordEntry> prefix, @NotNull SQLQueryWordEntry tail) {
                if (prefix.size() == 1) { // table-ref-prefixed column
                    SQLQueryWordEntry mayBeAliasName = prefix.get(0);
                    return this.knownSources.getResolutionResults().values().stream()
                        .filter(rr -> rr.aliasOrNull != null && rr.aliasOrNull.getName().toLowerCase().contains(mayBeAliasName.string())) // TODO consider table full name
                        .flatMap(srr -> srr.source.getResultDataContext().getColumnsList().stream()
                            .map(c -> SQLQueryCompletionItem.forSubsetColumn(makeFilterInfo(tail, c.symbol.getName().toLowerCase()), c, srr, false))
                            .filter(c -> tail == null || c.getFilterInfo().string().contains(tail.string()))
                        ).toList();
                } else if (prefix.size() == 0) { // table-ref not introduced yet or non-prefixed column, so try both cases
                    return this.prepareColumnCompletions(tail);
                }
                return Collections.emptyList();
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareObjectComponentCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObject object,
                @NotNull SQLQueryWordEntry componentNamePart,
                Class<?> componentType
            ) {
                return prepareObjectComponentCompletions(
                    monitor, object, componentNamePart, componentType, SQLQueryCompletionItem::forDbObject
                );
            }
            @NotNull
            private List<SQLQueryCompletionItem> prepareObjectComponentCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObject object,
                @NotNull SQLQueryWordEntry componentNamePart,
                Class<?> componentType,
                BiFunction<SQLQueryWordEntry, DBSObject, SQLQueryCompletionItem> queryCompletionItemProvider
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

                    return components.filter(a -> (componentType == null || componentType.isInstance(a)))
                            .map(o -> Pair.of(o.getName().toLowerCase(), o))
                            .filter(p -> componentNamePart == null || p.getFirst().contains(componentNamePart.string()))
                        .map(p -> queryCompletionItemProvider.apply(makeFilterInfo(componentNamePart, p.getFirst()), p.getSecond()))
                        .toList();
                } catch (DBException ex) {
                    return Collections.emptyList();
                }
            }

            private List<SQLQueryWordEntry> obtainIdentifierParts(int position) {
                List<SQLQueryWordEntry> parts = new ArrayList<>(nameNodes.length);
                int i = 0;
                for (; i < nameNodes.length; i++) {
                    STMTreeTermNode term = nameNodes[i];
                    if (term.symbol.getType() != SQLStandardLexer.Period) {
                        if (term.getRealInterval().b + 1 < position) {
                            parts.add(new SQLQueryWordEntry(term.getRealInterval().a, term.getTextContent().toLowerCase()));
                        } else {
                            break;
                        }
                    }
                }
                STMTreeTermNode currentNode = i >= nameNodes.length ? null : nameNodes[i];
                String currentPart = currentNode == null
                    ? null
                    : currentNode.getTextContent().substring(0, position - currentNode.getRealInterval().a);
                parts.add(currentPart == null ? null : new SQLQueryWordEntry(currentNode.getRealInterval().a, currentPart.toLowerCase()));
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
                @NotNull SQLCompletionRequest request,
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
                    // TODO consider deeper hierarchy
                    SQLQuerySymbolEntry schemaName = qname.scopeName.size() <= 0 ? null : qname.scopeName.get(qname.scopeName.size() - 1);
                    SQLQuerySymbolEntry catalogName = qname.scopeName.size() <= 1 ? null : qname.scopeName.get(qname.scopeName.size() - 2);

                    if ((nameRange = qname.entityName.getSyntaxNode().getRealInterval()).properlyContains(pos)) {
                        SQLQueryWordEntry part = new SQLQueryWordEntry(qname.entityName.getInterval().a, qname.entityName.getRawName().substring(0, position - nameRange.a));
                        if (schemaName != null) {
                            SQLQuerySymbolDefinition scopeDef = this.unrollSymbolDefinition(schemaName.getDefinition());
                            if (scopeDef instanceof SQLQuerySymbolByDbObjectDefinition byObjDef) {
                                return this.prepareObjectComponentCompletions(monitor, byObjDef.getDbObject(), part, DBSEntity.class);
                            } else {
                                // schema was not resolved, so cannot accomplish its subitems
                                return Collections.emptyList();
                            }
                        } else {
                            return this.prepareIdentifierCompletions(monitor, request, List.of(part), DBSEntity.class);
                        }
                    } else if (schemaName != null
                        && (schemaRange = schemaName.getSyntaxNode().getRealInterval()).properlyContains(pos)
                    ) {
                        SQLQueryWordEntry part = new SQLQueryWordEntry(schemaName.getInterval().a, schemaName.getRawName().substring(0, position - schemaRange.a));
                        if (catalogName != null) {
                            SQLQuerySymbolDefinition scopeDef = this.unrollSymbolDefinition(schemaName.getDefinition());
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
                    } else if (catalogName != null
                        && (catalogRange = catalogName.getSyntaxNode().getRealInterval()).properlyContains(pos)
                    ) {
                        SQLQueryWordEntry part = new SQLQueryWordEntry(catalogName.getInterval().a, catalogName.getRawName().substring(0, position - catalogRange.a));
                        return this.prepareObjectComponentCompletions(monitor, dbcExecutionContext.getDataSource(), part, DBSCatalog.class);
                    } else {
                        throw new UnsupportedOperationException("Illegal SQLQueryQualifiedName");
                    }
                } else if (lexicalItem instanceof SQLQuerySymbolEntry entry) {
                    Interval nameRange = entry.getSyntaxNode().getRealInterval();
                    SQLQueryWordEntry part = new SQLQueryWordEntry(entry.getInterval().a, entry.getRawName().substring(0, position - nameRange.a));
                    return this.prepareIdentifierCompletions(monitor, request, List.of(part), null);
                } else {
                    throw new UnsupportedOperationException("Unexpected lexical item kind to complete " + lexicalItem.getClass().getName());
                }
            }
            
            private List<SQLQueryCompletionItem> prepareKeywordCompletions(@NotNull Set<String> keywords, @Nullable SQLQueryWordEntry filter) {
                Stream<SQLQueryCompletionItem> stream = keywords.stream().map(
                    s -> SQLQueryCompletionItem.forReservedWord(makeFilterInfo(filter, s.toLowerCase()), s)
                );
                if (filter != null) {
                    stream = stream.filter(s -> s.getFilterInfo().string().contains(filter.string()));
                }
                return stream.toList();
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareColumnCompletions(@Nullable SQLQueryWordEntry filterOrNull) {
                // directly available column
                List<SQLQueryCompletionItem> subsetColumns = prepareCurrentTupleColumns(filterOrNull);
                // already referenced tables
                LinkedList<SQLQueryCompletionItem> tableRefs = new LinkedList<>();
                for (SourceResolutionResult rr : this.knownSources.getResolutionResults().values()) {
                    if (rr.aliasOrNull != null && !rr.isCteSubquery) {
                        String sourceAliasFilterString = rr.aliasOrNull.getName().toLowerCase();
                        if (filterOrNull == null || sourceAliasFilterString.contains(filterOrNull.string())) {
                            tableRefs.add(SQLQueryCompletionItem.forRowsSourceAlias(makeFilterInfo(filterOrNull, sourceAliasFilterString), rr.aliasOrNull, rr));
                        }
                    } else if (rr.tableOrNull != null) {
                        String tableNameFilterString = rr.tableOrNull.getName().toLowerCase();
                        if (filterOrNull == null || tableNameFilterString.contains(filterOrNull.string())) {
                            tableRefs.add(SQLQueryCompletionItem.forRealTable(makeFilterInfo(filterOrNull, tableNameFilterString), rr.tableOrNull, true));
                        }
                    }
                }
                return Stream.of(subsetColumns, tableRefs).flatMap(Collection::stream).toList();
            }

            @NotNull
            @Override
            public List<SQLQueryCompletionItem> prepareCurrentTupleColumns() {
                return this.prepareCurrentTupleColumns(null);
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareCurrentTupleColumns(@Nullable SQLQueryWordEntry filterOrNull) {
                Stream<SQLQueryCompletionItem> subsetColumns = context.getColumnsList().stream()
                    .map(rc -> SQLQueryCompletionItem.forSubsetColumn(makeFilterInfo(filterOrNull, rc.symbol.getName().toLowerCase()), rc, this.knownSources.getResolutionResults().get(rc.source), true));

                if (filterOrNull != null) {
                    subsetColumns = subsetColumns.filter(c -> c.getFilterInfo().string().contains(filterOrNull.string()));
                }

                return subsetColumns.toList();
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareTableCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request
            ) {
                LinkedList<SQLQueryCompletionItem> completions = new LinkedList<>();
                for (SourceResolutionResult rr : this.knownSources.getResolutionResults().values()) {
                    if (rr.aliasOrNull != null && rr.isCteSubquery) {
                        completions.add(SQLQueryCompletionItem.forRowsSourceAlias(
                            makeFilterInfo(null, rr.aliasOrNull.getName().toLowerCase()), rr.aliasOrNull, rr
                        ));
                    }
                }

                if (dbcExecutionContext != null) {
                    try {
                        DBCExecutionContextDefaults<?, ?> defaults = dbcExecutionContext.getContextDefaults();
                        if (defaults != null) {
                            DBSSchema defaultSchema = defaults.getDefaultSchema();
                            DBSCatalog defaultCatalog = defaults.getDefaultCatalog();
                            if (defaultCatalog == null && defaultSchema == null && dbcExecutionContext.getDataSource() instanceof DBSObjectContainer container) {
                                this.collectTables(monitor, container, completions);
                            } else if (request.getContext().isSearchGlobally() && defaultCatalog != null) {
                                this.collectTables(monitor, defaultCatalog, completions);
                            } else {
                                if (defaultSchema != null) {
                                    this.collectTables(monitor, defaultSchema, completions);
                                }
                            }
                        }

                        this.collectContextSchemasAndCatalogs(monitor, completions);
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                
                return completions;
            }

            private void collectContextSchemasAndCatalogs(@NotNull DBRProgressMonitor monitor, @NotNull LinkedList<SQLQueryCompletionItem> completions) throws DBException {
                for (DBSObjectContainer container : this.exposedContexts) {
                    Collection<? extends DBSObject> children = container.getChildren(monitor);
                    for (DBSObject child : children) {
                        if (child instanceof DBSSchema || child instanceof DBSCatalog) {
                            completions.addLast(SQLQueryCompletionItem.forDbObject(makeFilterInfo(null, child.getName().toLowerCase()), child));
                        }
                    }
                }
            }

            private void collectTables(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                this.collectObjectsRecursively(
                    monitor, container, new HashSet<>(), accumulator,
                    List.of(DBSTable.class, DBSView.class),
                    (e, o) -> SQLQueryCompletionItem.forRealTable(e, o, knownSources.getReferencedTables().contains(o))
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
                @NotNull BiFunction<SQLQueryWordEntry, T, SQLQueryCompletionItem> completionItemFabric
            ) throws DBException {
                Collection<? extends DBSObject> children = container.getChildren(monitor);
                for (DBSObject child : children) {
                    if (!DBUtils.isHiddenObject(child)) {
                        if (types.stream().anyMatch(t -> t.isInstance(child))) {
                            if (alreadyReferencedObjects.add(child)) {
                                accumulator.add(completionItemFabric.apply(makeFilterInfo(null, child.getName().toLowerCase()), (T) child));
                            }
                        } else if (child instanceof DBSObjectContainer sc && DBStructUtils.isConnectedContainer(child)) {
                            collectObjectsRecursively(monitor, sc, alreadyReferencedObjects, accumulator, types, completionItemFabric);
                        }
                    }
                }
            }
        };
    }

    @NotNull
    private static Set<DBSObjectContainer> obtainExposedContexts(@Nullable DBCExecutionContext dbcExecutionContext) {
        Set<DBSObjectContainer> exposedContexts = new LinkedHashSet<>();
        if (dbcExecutionContext != null) {
            for (
                DBSObject contextObject = DBUtils.getSelectedObject(dbcExecutionContext);
                contextObject != null;
                contextObject = contextObject.getParentObject()
            ) {
                if (contextObject instanceof DBSObjectContainer container) {
                    exposedContexts.add(container);
                }
            }

            DBPDataSource dataSource = dbcExecutionContext.getDataSource();
            if (dataSource instanceof DBSObjectContainer container) {
                exposedContexts.add(container);
            }
        }
        return exposedContexts;
    }
}
