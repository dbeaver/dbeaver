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
import org.eclipse.jface.text.IRegion;
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
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryCombinedContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SQLQueryCompletionContext {

    private static final Log log = Log.getLog(SQLQueryCompletionContext.class);

    private static final Set<String> statementStartKeywords = LSMInspections.prepareOffquerySyntaxInspection().predictedWords();
    private static final int statementStartKeywordMaxLength = statementStartKeywords.stream().mapToInt(String::length).max().orElse(0);

    private static final Set<SQLQuerySymbolClass> potentialKeywordPartClassification = Set.of(
        SQLQuerySymbolClass.UNKNOWN,
        SQLQuerySymbolClass.ERROR,
        SQLQuerySymbolClass.RESERVED
    );

    /**
     * Returns maximum length of all keywords
     */
    public static int getMaxKeywordLength() {
        return statementStartKeywordMaxLength;
    }

    /**
     * Empty completion context which always provides no completion items
     */
    public static SQLQueryCompletionContext prepareEmpty(int scriptItemOffset, int requestOffset) {
        return new SQLQueryCompletionContext(0, requestOffset) {

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
            public Collection<SQLQueryCompletionSet> prepareProposal(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request
            ) {
                return List.of(new SQLQueryCompletionSet(getRequestOffset(), 0, Collections.emptyList()));
            }
        };
    }

    /**
     * Prepare completion context for the script item at given offset treating current position as outside-of-query
     */
    @NotNull
    public static SQLQueryCompletionContext prepareOffquery(int scriptItemOffset, int requestOffset) {
        return new SQLQueryCompletionContext(scriptItemOffset, requestOffset) {
            private static final LSMInspections.SyntaxInspectionResult syntaxInspectionResult = LSMInspections.prepareOffquerySyntaxInspection();
            private static final Pattern KEYWORD_FILTER_PATTERN = Pattern.compile("([a-zA-Z0-9]+)");

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
            public Collection<SQLQueryCompletionSet> prepareProposal(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request
            ) {
                int lineStartOffset;
                String lineText;
                try {
                    IDocument doc = request.getDocument();
                    IRegion lineInfo = doc.getLineInformationOfOffset(this.getRequestOffset());
                    lineStartOffset = lineInfo.getOffset();
                    lineText = doc.get(lineStartOffset, lineInfo.getLength());
                } catch (BadLocationException ex) {
                    lineStartOffset = -1;
                    lineText = "";
                }

                // First keyword handling, when there is no query model
                Matcher m = KEYWORD_FILTER_PATTERN.matcher(lineText);
                SQLQueryWordEntry filter = null;
                if (m.find() && lineStartOffset >= 0) {
                    MatchResult mr = m.toMatchResult();
                    int inLineOffset = this.getRequestOffset() - lineStartOffset;
                    for (int i = 0; i < mr.groupCount(); i++) {
                        int start = mr.start(i);
                        int end = mr.end(i);
                        if (start <= inLineOffset && end >= inLineOffset) {
                            String filterKeyString = lineText.substring(m.start(), m.end()).toLowerCase();
                            int filterStart = start + lineStartOffset - scriptItemOffset;
                            filter = new SQLQueryWordEntry(filterStart, filterKeyString);
                            break;
                        }
                    }
                }

                SQLQueryCompletionSet keywordCompletions = this.prepareKeywordCompletions(statementStartKeywords, filter);
                return List.of(keywordCompletions);
            }
        };
    }

    private final int scriptItemOffset;
    private final int requestOffset;

    private SQLQueryCompletionContext(int scriptItemOffset, int requestOffset) {
        this.scriptItemOffset = scriptItemOffset;
        this.requestOffset = requestOffset;
    }

    public int getOffset() {
        return this.scriptItemOffset;
    }

    public int getRequestOffset() {
        return this.requestOffset;
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
    public abstract Collection<SQLQueryCompletionSet> prepareProposal(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SQLCompletionRequest request
    );

    @NotNull
    protected SQLQueryWordEntry makeFilterInfo(@Nullable SQLQueryWordEntry filterKey, @NotNull String filterString) {
        return new SQLQueryWordEntry(filterKey == null ? -1 : (this.getOffset() + filterKey.offset), filterString);
    }

    /**
     * Prepare completion context for the script item in the given contexts (execution, syntax and semantics)
     */
    public static SQLQueryCompletionContext prepare(
        @NotNull SQLScriptItemAtOffset scriptItem,
        int requestOffset,
        @Nullable DBCExecutionContext dbcExecutionContext,
        @NotNull LSMInspections.SyntaxInspectionResult syntaxInspectionResult,
        @NotNull SQLQueryModel.LexicalContextResolutionResult context,
        @Nullable SQLQueryLexicalScopeItem lexicalItem,
        @NotNull STMTreeTermNode[] nameNodes,
        boolean hasPeriod,
        @Nullable STMTreeTermNode currentTerm
    ) {
        return new SQLQueryCompletionContext(scriptItem.offset, requestOffset) {
            private final Set<DBSObjectContainer> exposedContexts = SQLQueryCompletionContext.obtainExposedContexts(dbcExecutionContext);
            private final SQLQueryDataContext.KnownSourcesInfo knownSources = context.deepestContext().collectKnownSources();

            @NotNull
            @Override
            public SQLQueryDataContext getDataContext() {
                return context.deepestContext();
            }

            @NotNull
            @Override
            public LSMInspections.SyntaxInspectionResult getInspectionResult() {
                return syntaxInspectionResult;
            }

            @NotNull
            @Override
            public Set<String> getAliasesInUse() {
                return context.nearestResultContext().collectKnownSources().getAliasesInUse();
            }

            @NotNull
            @Override
            public Set<DBSObjectContainer> getExposedContexts() {
                return this.exposedContexts;
            }

            @NotNull
            @Override
            public Collection<SQLQueryCompletionSet> prepareProposal(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request
            ) {
                int position = this.getRequestOffset() - this.getOffset();
                
                SQLQueryWordEntry currentWord = this.obtainCurrentWord(currentTerm, position);
                List<SQLQueryWordEntry> parts = this.obtainIdentifierParts(position);

                boolean keywordsAllowed = (lexicalItem == null || potentialKeywordPartClassification.contains(lexicalItem.getSymbolClass())) && !hasPeriod;
                SQLQueryCompletionSet keywordCompletions = keywordsAllowed
                    ? prepareKeywordCompletions(syntaxInspectionResult.predictedWords(), currentWord)
                    : null;

                SQLQueryCompletionSet columnRefCompletions = syntaxInspectionResult.expectingColumnReference() && nameNodes.length == 0
                    ? this.prepareColumnCompletions(monitor, null)
                    : null;

                SQLQueryCompletionSet tableRefCompletions = syntaxInspectionResult.expectingTableReference() && nameNodes.length == 0
                    ? this.prepareTableCompletions(monitor, request, null)
                    : null;

                SQLQueryCompletionSet lexicalItemCompletions = lexicalItem != null
                    ? this.prepareLexicalItemCompletions(monitor, request, lexicalItem, position)
                    : syntaxInspectionResult.expectingIdentifier() || nameNodes.length > 0 && (parts.size() > 1 || (parts.size() == 1 && parts.get(0) != null))
                        ? this.prepareInspectedIdentifierCompletions(monitor, request, parts)
                        : null;

                List<SQLQueryCompletionSet> completionSets = Stream.of(
                        columnRefCompletions, tableRefCompletions, lexicalItemCompletions, keywordCompletions)
                    .filter(s -> s != null && s.getItems().size() > 0)
                    .collect(Collectors.toList());

                return completionSets;
            }

            @Nullable
            private SQLQueryWordEntry obtainCurrentWord(STMTreeTermNode currentTerm, int position) {
                if (currentTerm == null) {
                    return null;
                }
                Interval wordRange = currentTerm.getRealInterval();
                if (wordRange.b >= position - 1 && currentTerm.symbol.getType() != SQLStandardLexer.Period) {
                    return new SQLQueryWordEntry(wordRange.a, currentTerm.getTextContent().substring(0, position - currentTerm.getRealInterval().a));
                } else {
                    return null;
                }
            }

            @Nullable
            private SQLQueryCompletionSet prepareInspectedIdentifierCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull List<SQLQueryWordEntry> parts
            ) {
                List<SQLQueryWordEntry> prefix = parts.subList(0, parts.size() - 1);
                SQLQueryWordEntry tail = parts.get(parts.size() - 1);
                if (tail != null) {
                    String[][] quoteStrs = request.getContext().getDataSource().getSQLDialect().getIdentifierQuoteStrings();
                    if (quoteStrs.length > 0) {
                        // The "word" being accomplished may be a quoted or a beginning of the quoted identifier,
                        // so we should remove potential quotes.
                        // TODO Consider identifiers containing escape-sequences
                        String qp = Stream.of(quoteStrs).flatMap(ss -> Stream.of(ss)).map(Pattern::quote).distinct().collect(Collectors.joining("|"));
                        tail = new SQLQueryWordEntry(tail.offset, tail.string.replaceAll(qp, ""));
                    }
                }

                SQLQueryCompletionSet result;
                if (syntaxInspectionResult.expectingColumnReference()) {
                    result = this.accomplishColumnReference(monitor, prefix, tail);
                } else if (syntaxInspectionResult.expectingTableReference()) {
                    result = this.accomplishTableReference(monitor, request, prefix, tail);
                } else {
                    result = null;
                }

                return result;
            }

            @Nullable
            private SQLQueryCompletionSet accomplishTableReference(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull List<SQLQueryWordEntry> prefix,
                @Nullable SQLQueryWordEntry tail
            ) {
                if (dbcExecutionContext == null || dbcExecutionContext.getDataSource() == null || !DBStructUtils.isConnectedContainer(dbcExecutionContext.getDataSource())) {
                    return null;
                } else if (prefix.isEmpty()) {
                    return this.prepareTableCompletions(monitor, request, tail);
                } else {
                    List<String> contextName = prefix.stream().map(e -> e.string).collect(Collectors.toList());
                    DBSObject prefixContext = SQLSearchUtils.findObjectByFQN(
                        monitor,
                        (DBSObjectContainer) dbcExecutionContext.getDataSource(),
                        dbcExecutionContext,
                        contextName,
                        false,
                        request.getWordDetector()
                    );

                    LinkedList<SQLQueryCompletionItem> items = new LinkedList<>();
                    if (prefixContext instanceof DBSObjectContainer container) {
                        SQLQueryCompletionItem.ContextObjectInfo
                            contextObject = this.prepareContextInfo(request, prefix, tail, prefixContext);
                        try {
                            this.collectTables(monitor, container, contextObject, tail, items);
                            this.collectContextSchemasAndCatalogs(monitor, List.of(container), contextObject, tail, items);
                        } catch (DBException e) {
                            log.error(e);
                        }
                    }
                    return this.makeFilteredCompletionSet(prefix.isEmpty() ? tail : prefix.get(0), items);
                }
            }

            @NotNull
            private SQLQueryCompletionSet accomplishColumnReference(
                @NotNull DBRProgressMonitor monitor,
                @NotNull List<SQLQueryWordEntry> prefix,
                @Nullable SQLQueryWordEntry tail
            ) {
                if (prefix.size() > 0) { // table-ref-prefixed column
                    List<Predicate<SourceResolutionResult>> sourcePredicates = new ArrayList<>(5);

                    List<String> tableName = prefix.stream().map(w -> w.string).toList();
                    SourceResolutionResult referencedSource = this.getDataContext().resolveSource(monitor, tableName);

                    if (prefix.size() == 1) {
                        SQLQueryWordEntry mayBeAliasName = prefix.get(0);
                        sourcePredicates.add(srr -> srr.aliasOrNull != null && srr.aliasOrNull.getName().toLowerCase().contains(mayBeAliasName.filterString));
                    }

                    sourcePredicates.add(srr -> {
                        if (srr.tableOrNull != null) {
                            List<String> parts = SQLQueryCompletionItem.prepareQualifiedNameParts(srr.tableOrNull, null);
                            int partsMatched = 0;
                            for (int i = prefix.size() - 1, j = parts.size() - 1; i >= 0 && j >= 0; i--, j--) {
                                if (parts.get(j).toLowerCase().contains(prefix.get(i).filterString)) {
                                    partsMatched++;
                                }
                            }
                            return partsMatched == prefix.size();
                        } else {
                            return false;
                        }
                    });

                    List<Pair<SourceResolutionResult, Boolean>> srr2 = this.knownSources.getResolutionResults().values().stream()
                        .map(rr -> {
                            if (referencedSource != null && rr.source == referencedSource.source) {
                                return Pair.of(rr, true);
                            } else {
                                return sourcePredicates.stream().anyMatch(p -> p.test(rr)) ? Pair.of(rr, false) : null;
                            }
                        }).filter(Objects::nonNull).toList();

                    List<SQLQueryCompletionItem.SQLColumnNameCompletionItem> cols = srr2.stream().flatMap(
                        srr -> srr.getFirst().source.getResultDataContext().getColumnsList().stream()
                            .map(c -> SQLQueryCompletionItem.forSubsetColumn(
                                makeFilterInfo(tail, c.symbol.getName()), c, srr.getFirst(), srr.getSecond()
                            ))
                    ).toList();

                    List<SQLQueryCompletionItem.SQLColumnNameCompletionItem> items = cols.stream()
                        .filter(c -> tail == null || c.getFilterInfo().filterString.contains(tail.filterString))
                        .toList();

                    return this.makeFilteredCompletionSet(prefix.get(0), items);
                } else { // table-ref not introduced yet or non-prefixed column, so try both cases
                    return this.prepareColumnCompletions(monitor, tail);
                }
            }

            @Nullable
            private SQLQueryCompletionSet prepareObjectComponentCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObject object,
                @NotNull SQLQueryWordEntry componentNamePart,
                @NotNull List<Class<? extends DBSObject>> componentTypes
            ) {
                return this.prepareObjectComponentCompletions(
                    monitor, object, componentNamePart, componentTypes, (e, o) -> SQLQueryCompletionItem.forDbObject(e, null, o)
                );
            }

            @Nullable
            private <T extends DBSObject> SQLQueryCompletionSet prepareObjectComponentCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObject object,
                @Nullable SQLQueryWordEntry componentNamePart,
                @NotNull List<Class<? extends T>> componentTypes,
                BiFunction<SQLQueryWordEntry, T, SQLQueryCompletionItem> queryCompletionItemProvider
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

                    List<SQLQueryCompletionItem> items = components.filter(a -> componentTypes.stream().anyMatch(t -> t.isInstance(a)))
                            .map(o -> Pair.of(makeFilterInfo(componentNamePart, o.getName()), o))
                            .filter(p -> componentNamePart == null || p.getFirst().filterString.contains(componentNamePart.filterString))
                        .map(p -> queryCompletionItemProvider.apply(p.getFirst(), (T) p.getSecond()))
                        .toList();
                    return this.makeFilteredCompletionSet(componentNamePart, items);
                } catch (DBException ex) {
                    log.error(ex);
                    return null;
                }
            }

            private List<SQLQueryWordEntry> obtainIdentifierParts(int position) {
                List<SQLQueryWordEntry> parts = new ArrayList<>(nameNodes.length);
                int i = 0;
                for (; i < nameNodes.length; i++) {
                    STMTreeTermNode term = nameNodes[i];
                    if (term.symbol.getType() != SQLStandardLexer.Period) {
                        if (term.getRealInterval().b + 1 < position) {
                            parts.add(new SQLQueryWordEntry(term.getRealInterval().a, term.getTextContent()));
                        } else {
                            break;
                        }
                    }
                }
                STMTreeTermNode currentNode = i >= nameNodes.length ? null : nameNodes[i];
                String currentPart = currentNode == null
                    ? null
                    : currentNode.getTextContent().substring(0, position - currentNode.getRealInterval().a);
                parts.add(currentPart == null ? null : new SQLQueryWordEntry(currentNode.getRealInterval().a, currentPart));
                return parts;
            }

            private SQLQuerySymbolDefinition unrollSymbolDefinition(SQLQuerySymbolDefinition def) {
                while (def instanceof SQLQuerySymbolEntry entry) {
                    def = entry.getDefinition();
                }
                return def;
            }

            @Nullable
            private SQLQueryCompletionSet prepareLexicalItemCompletions(
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
                                return this.prepareObjectComponentCompletions(monitor, byObjDef.getDbObject(), part, List.of(DBSEntity.class));
                            } else {
                                // schema was not resolved, so cannot accomplish its subitems
                                return null;
                            }
                        } else {
                            return this.prepareInspectedIdentifierCompletions(monitor, request, List.of(part));
                        }
                    } else if (schemaName != null
                        && (schemaRange = schemaName.getSyntaxNode().getRealInterval()).properlyContains(pos)
                    ) {
                        SQLQueryWordEntry part = new SQLQueryWordEntry(schemaName.getInterval().a, schemaName.getRawName().substring(0, position - schemaRange.a));
                        if (catalogName != null) {
                            SQLQuerySymbolDefinition scopeDef = this.unrollSymbolDefinition(schemaName.getDefinition());
                            if (scopeDef instanceof SQLQuerySymbolByDbObjectDefinition byObjDef) {
                                return this.prepareObjectComponentCompletions(monitor, byObjDef.getDbObject(), part, List.of(DBSSchema.class));
                            } else {
                                // catalog was not resolved, so cannot accomplish schema
                                return null;
                            }
                        } else {
                            return this.prepareObjectComponentCompletions(
                                monitor,
                                dbcExecutionContext.getDataSource(),
                                part,
                                List.of(DBSSchema.class)
                            );
                        }
                    } else if (catalogName != null
                        && (catalogRange = catalogName.getSyntaxNode().getRealInterval()).properlyContains(pos)
                    ) {
                        SQLQueryWordEntry part = new SQLQueryWordEntry(catalogName.getInterval().a, catalogName.getRawName().substring(0, position - catalogRange.a));
                        return this.prepareObjectComponentCompletions(monitor, dbcExecutionContext.getDataSource(), part, List.of(DBSCatalog.class));
                    } else {
                        throw new UnsupportedOperationException("Illegal SQLQueryQualifiedName");
                    }
                } else if (lexicalItem instanceof SQLQuerySymbolEntry entry) {
                    Interval nameRange = entry.getSyntaxNode().getRealInterval();
                    SQLQueryWordEntry part = new SQLQueryWordEntry(entry.getInterval().a, entry.getRawName().substring(0, position - nameRange.a));
                    return this.prepareInspectedIdentifierCompletions(monitor, request, List.of(part));
                } else {
                    throw new UnsupportedOperationException("Unexpected lexical item kind to complete " + lexicalItem.getClass().getName());
                }
            }

            private List<SQLQueryCompletionItem> prepareJoinConditionCompletions(
                @NotNull DBRProgressMonitor monitor,
                @Nullable SQLQueryWordEntry filterOrNull
            ) {
                class AssociationsResolutionContext {
                    public final Map<DBSEntityAttribute, List<SQLQueryCompletionItem.SQLColumnNameCompletionItem>> realColumnRefsByEntityAttribute = context.deepestContext().getColumnsList().stream()
                        .filter(rc -> rc.realAttr != null && rc.realAttr.getParentObject() == rc.realSource)
                        .collect(Collectors.groupingBy(rc -> rc.realAttr)).entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, g -> g.getValue().stream().map(
                            rc -> SQLQueryCompletionItem.forSubsetColumn(
                                makeFilterInfo(null, rc.symbol.getName()), rc, knownSources.getResolutionResults().get(rc.source), false
                            )
                        ).toList()));

                    private final Map<DBSEntity, Map<DBSEntityAttribute, List<DBSEntityAttribute>>> associatedAttrsByEntity = new HashMap<>();

                    public List<DBSEntityAttribute> findAssociations(DBSEntityAttribute key) {
                        return Optional.ofNullable(this.associatedAttrsByEntity.computeIfAbsent(key.getParentObject(), this::prepareAllAssociations).get(key)).orElse(Collections.emptyList());
                    }

                    private Map<DBSEntityAttribute, List<DBSEntityAttribute>> prepareAllAssociations(DBSEntity entity) {
                        try {
                            return Stream.concat(
                                Optional.ofNullable(entity.getAssociations(monitor)).stream().flatMap(Collection::stream)
                                    .filter(a -> knownSources.getReferencedTables().contains(a.getAssociatedEntity())),
                                Optional.ofNullable(entity.getReferences(monitor)).stream().flatMap(Collection::stream)
                                    .filter(r -> knownSources.getReferencedTables().contains(r.getParentObject()))
                            ).filter(c -> c instanceof DBSTableForeignKey)
                             .map(c -> {
                                 try {
                                     return ((DBSTableForeignKey) c).getAttributeReferences(new VoidProgressMonitor());
                                 } catch (DBException e) {
                                     return null;
                                 }
                             })
                             .filter(aa -> aa != null && aa.size() == 1 && aa.get(0) instanceof DBSTableForeignKeyColumn)
                             // TODO consider compound keys and filtered by the common path to the context of origin
                             .map(aa -> (DBSTableForeignKeyColumn) aa.get(0))
                             .map(attrRef -> {
                                 DBSEntityAttribute sourceAttr = attrRef.getAttribute();
                                 DBSEntityAttribute targetAttr = attrRef.getReferencedColumn();
                                 if (targetAttr != null && sourceAttr != null) {
                                     if (sourceAttr.getParentObject() == entity) {
                                         return Pair.of(sourceAttr, targetAttr);
                                     } else {
                                         return Pair.of(targetAttr, sourceAttr);
                                     }
                                 } else {
                                     return null;
                                 }
                             })
                             .filter(Objects::nonNull)
                             .collect(Collectors.groupingBy(Pair::getFirst, Collectors.mapping(Pair::getSecond, Collectors.toList())));
                        } catch (DBException e) {
                            return Collections.emptyMap();
                        }
                    }
                }

                LinkedList<SQLQueryCompletionItem> result = new LinkedList<>();

                if (this.knownSources.getReferencedTables().size() > 1 && this.knownSources.getResolutionResults().size() > 1
                    && context.deepestContext() instanceof SQLQueryCombinedContext joinContext && joinContext.isJoin()) {

                    AssociationsResolutionContext associations = new AssociationsResolutionContext();

                    for (SQLQueryResultColumn joinedColumn : joinContext.getRightParent().getColumnsList()) {
                        if (joinedColumn.realAttr != null) {
                            for (DBSEntityAttribute otherColumnAttribute : associations.findAssociations(joinedColumn.realAttr)) {
                                List<SQLQueryCompletionItem.SQLColumnNameCompletionItem> otherColumnRefs = associations.realColumnRefsByEntityAttribute.get(otherColumnAttribute);
                                if (otherColumnRefs != null) {
                                    for (SQLQueryCompletionItem.SQLColumnNameCompletionItem thisColumnRef : associations.realColumnRefsByEntityAttribute.get(joinedColumn.realAttr)) {
                                        if (thisColumnRef.columnInfo == joinedColumn) {
                                            for (SQLQueryCompletionItem.SQLColumnNameCompletionItem otherColumnRef : otherColumnRefs) {
                                                if (filterOrNull == null ||
                                                    thisColumnRef.getFilterInfo().filterString.contains(filterOrNull.filterString) ||
                                                    otherColumnRef.getFilterInfo().filterString.contains(filterOrNull.filterString)
                                                ) {
                                                    result.addLast(SQLQueryCompletionItem.forJoinCondition(thisColumnRef.getFilterInfo(), thisColumnRef, otherColumnRef));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return result;
            }

            @NotNull
            private SQLQueryCompletionSet prepareColumnCompletions(@NotNull DBRProgressMonitor monitor, @Nullable SQLQueryWordEntry filterOrNull) {
                // directly available column
                List<SQLQueryCompletionItem> subsetColumns = prepareCurrentTupleColumns(filterOrNull);
                // already referenced tables
                LinkedList<SQLQueryCompletionItem> tableRefs = new LinkedList<>();
                for (SourceResolutionResult rr : this.knownSources.getResolutionResults().values()) {
                    if (rr.aliasOrNull != null && !rr.isCteSubquery) {
                        SQLQueryWordEntry sourceAlias = makeFilterInfo(filterOrNull,  rr.aliasOrNull.getName());
                        if (filterOrNull == null || sourceAlias.filterString.contains(filterOrNull.filterString)) {
                            tableRefs.add(SQLQueryCompletionItem.forRowsSourceAlias(sourceAlias, rr.aliasOrNull, rr));
                        }
                    } else if (rr.tableOrNull != null) {
                        SQLQueryWordEntry tableName = makeFilterInfo(filterOrNull, rr.tableOrNull.getName());
                        if (filterOrNull == null || tableName.filterString.contains(filterOrNull.filterString)) {
                            tableRefs.add(SQLQueryCompletionItem.forRealTable(tableName, null, rr.tableOrNull, true));
                        }
                    }
                }

                List<SQLQueryCompletionItem> joinConditions = syntaxInspectionResult.expectingJoinCondition()
                    ? this.prepareJoinConditionCompletions(monitor, filterOrNull)
                    : Collections.emptyList();

                return this.makeFilteredCompletionSet(filterOrNull, Stream.of(subsetColumns, tableRefs, joinConditions).flatMap(Collection::stream).toList());
            }

            @NotNull
            @Override
            public List<SQLQueryCompletionItem> prepareCurrentTupleColumns() {
                return this.prepareCurrentTupleColumns(null);
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareCurrentTupleColumns(@Nullable SQLQueryWordEntry filterOrNull) {
                Stream<SQLQueryCompletionItem> subsetColumns = context.deepestContext().getColumnsList().stream()
                    .map(rc -> SQLQueryCompletionItem.forSubsetColumn(makeFilterInfo(filterOrNull, rc.symbol.getName()), rc, this.knownSources.getResolutionResults().get(rc.source), false));

                if (filterOrNull != null) {
                    subsetColumns = subsetColumns.filter(c -> c.getFilterInfo().filterString.contains(filterOrNull.filterString));
                }

                return subsetColumns.toList();
            }

            @NotNull
            private SQLQueryCompletionSet prepareTableCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @Nullable SQLQueryWordEntry filterOrNull
            ) {
                LinkedList<SQLQueryCompletionItem> completions = new LinkedList<>();
                for (SourceResolutionResult rr : this.knownSources.getResolutionResults().values()) {
                    if (rr.aliasOrNull != null && rr.isCteSubquery) {
                        SQLQueryWordEntry aliasName = makeFilterInfo(filterOrNull, rr.aliasOrNull.getName());
                        if (filterOrNull == null || aliasName.filterString.contains(filterOrNull.filterString)) {
                            completions.add(SQLQueryCompletionItem.forRowsSourceAlias(aliasName, rr.aliasOrNull, rr));
                        }
                    }
                }

                if (dbcExecutionContext != null) {
                    try {
                        DBCExecutionContextDefaults<?, ?> defaults = dbcExecutionContext.getContextDefaults();
                        if (defaults != null) {
                            DBSSchema defaultSchema = defaults.getDefaultSchema();
                            DBSCatalog defaultCatalog = defaults.getDefaultCatalog();
                            if (defaultCatalog == null && defaultSchema == null && dbcExecutionContext.getDataSource() instanceof DBSObjectContainer container) {
                                this.collectTables(monitor, container, null, filterOrNull, completions);
                            } else if ((request.getContext().isSearchGlobally() || defaultSchema == null) && defaultCatalog != null) {
                                this.collectTables(monitor, defaultCatalog, null, filterOrNull, completions);
                            } else if (defaultSchema != null) {
                                this.collectTables(monitor, defaultSchema, null, filterOrNull, completions);
                            }
                        }

                        this.collectContextSchemasAndCatalogs(monitor, this.exposedContexts, null, filterOrNull, completions);
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                
                return this.makeFilteredCompletionSet(filterOrNull, completions);
            }

            private void collectContextSchemasAndCatalogs(
                @NotNull DBRProgressMonitor monitor,
                @NotNull Collection<DBSObjectContainer> contexts,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull LinkedList<SQLQueryCompletionItem> completions
            ) throws DBException {
                for (DBSObjectContainer container : contexts) {
                    Collection<? extends DBSObject> children = container.getChildren(monitor);
                    for (DBSObject child : children) {
                        if (child instanceof DBSSchema || child instanceof DBSCatalog) {
                            SQLQueryWordEntry childName = makeFilterInfo(filterOrNull, child.getName());
                            if (filterOrNull == null || childName.filterString.contains(filterOrNull.filterString)) {
                                completions.addLast(SQLQueryCompletionItem.forDbObject(childName, contextObjext, child));
                            }
                        }
                    }
                }
            }

            private void collectTables(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                this.collectObjectsRecursively(
                    monitor, container, new HashSet<>(), accumulator, filterOrNull,
                    List.of(DBSTable.class, DBSView.class),
                    (e, o) -> SQLQueryCompletionItem.forRealTable(
                        e, contextObjext, o, knownSources.getReferencedTables().contains(o)
                    )
                );
            }

            private void collectSchemas(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator,
                @Nullable SQLQueryWordEntry filterOrNull
            ) throws DBException {
                this.collectObjectsRecursively(
                    monitor, container, new HashSet<>(), accumulator, filterOrNull,
                    List.of(DBSSchema.class), (e, s) -> SQLQueryCompletionItem.forDbObject(e, null, s)
                );
            }

            private void collectCatalogs(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator,
                @Nullable SQLQueryWordEntry filterOrNull
            ) throws DBException {
                this.collectObjectsRecursively(
                    monitor, container, new HashSet<>(), accumulator, filterOrNull,
                    List.of(DBSCatalog.class), (e, c) -> SQLQueryCompletionItem.forDbObject(e, null, c)
                );
            }

            private <T extends DBSObject> void collectObjectsRecursively(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull Set<DBSObject> alreadyReferencedObjects,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull List<Class<? extends T>> types,
                @NotNull BiFunction<SQLQueryWordEntry, T, SQLQueryCompletionItem> completionItemFabric
            ) throws DBException {
                Collection<? extends DBSObject> children = container.getChildren(monitor);
                for (DBSObject child : children) {
                    if (!DBUtils.isHiddenObject(child)) {
                        if (types.stream().anyMatch(t -> t.isInstance(child))) {
                            SQLQueryWordEntry childName = makeFilterInfo(filterOrNull, child.getName());
                            if (alreadyReferencedObjects.add(child) && (filterOrNull == null || childName.filterString.contains(filterOrNull.filterString))) {
                                accumulator.add(completionItemFabric.apply(childName, (T) child));
                            }
                        } else if (child instanceof DBSObjectContainer sc && DBStructUtils.isConnectedContainer(child)) {
                            collectObjectsRecursively(monitor, sc, alreadyReferencedObjects, accumulator, filterOrNull, types, completionItemFabric);
                        }
                    }
                }
            }

            private SQLQueryCompletionItem.ContextObjectInfo prepareContextInfo(@NotNull SQLCompletionRequest request, @NotNull List<SQLQueryWordEntry> prefix, @Nullable SQLQueryWordEntry tail, @NotNull DBSObject contextObject) {
                if (contextObject != null) {
                    int prefixStart = prefix.get(0).offset;
                    int requestPosition = tail != null ? tail.offset : (requestOffset - scriptItem.offset);
                    String prefixString = scriptItem.item.getOriginalText().substring(prefixStart, requestPosition);
                    return new SQLQueryCompletionItem.ContextObjectInfo(prefixString, contextObject);
                } else {
                    return null;
                }
            }
        };
    }

    protected SQLQueryCompletionSet prepareKeywordCompletions(@NotNull Set<String> keywords, @Nullable SQLQueryWordEntry filterOrNull) {
        Stream<SQLQueryCompletionItem> stream = keywords.stream().map(
                s -> SQLQueryCompletionItem.forReservedWord(makeFilterInfo(filterOrNull, s), s)
        );
        if (filterOrNull != null) {
            stream = stream.filter(s -> s.getFilterInfo().filterString.contains(filterOrNull.filterString));
        }
        return this.makeFilteredCompletionSet(filterOrNull, stream.toList());
    }

    protected SQLQueryCompletionSet makeFilteredCompletionSet(@Nullable SQLQueryWordEntry filterOrNull, List<? extends SQLQueryCompletionItem> items) {
        int replacementPosition = filterOrNull == null ? this.getRequestOffset() : this.getOffset() + filterOrNull.offset;
        int replacementLength = this.getRequestOffset() - replacementPosition;
        return new SQLQueryCompletionSet(replacementPosition, replacementLength, items);
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
