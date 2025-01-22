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
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMTreeTermErrorNode;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.utils.Pair;

import java.util.*;
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

                List<SQLQueryCompletionSet> results = new ArrayList<>();
                this.prepareKeywordCompletions(statementStartKeywords, filter, results);
                return results;
            }
        };
    }

    private final int scriptItemOffset;
    private final int requestOffset;

    protected boolean searchInsideWords;

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
    public List<? extends SQLQueryCompletionItem> prepareCurrentTupleColumns() {
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
        @NotNull STMTreeNode[] nameNodes,
        boolean hasPeriod,
        @Nullable STMTreeNode currentTerm
    ) {
        return new SQLQueryCompletionContext(scriptItem.offset, requestOffset) {
            private final Set<DBSObjectContainer> exposedContexts = SQLQueryCompletionContext.obtainExposedContexts(dbcExecutionContext);

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
                return context.nearestResultContext().getKnownSources().getAliasesInUse();
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
                this.searchInsideWords = request.getContext().isSearchInsideNames();

                int position = this.getRequestOffset() - this.getOffset();
                
                SQLQueryWordEntry currentWord = this.obtainCurrentWord(currentTerm, position);
                List<SQLQueryWordEntry> parts = this.obtainIdentifierParts(position);

                List<SQLQueryCompletionSet> completionSets = new LinkedList<>();

                if (lexicalItem != null) {
                    this.prepareLexicalItemCompletions(monitor, request, lexicalItem, position, parts, completionSets);
                }  else if (syntaxInspectionResult.expectingIdentifier() || this.nameNodesAreUseful(parts)) {
                    this.prepareInspectedIdentifierCompletions(monitor, request, parts, completionSets);
                } else if (context.symbolsOrigin() != null) {
                    this.accomplishFromKnownOrigin(monitor, request, context.symbolsOrigin(), null, completionSets);
                } else {
                    this.prepareInspectedFreeCompletions(monitor, request, completionSets);
                }

                boolean keywordsAllowed = (lexicalItem == null || (lexicalItem.getOrigin() != null && !lexicalItem.getOrigin().isChained()) || (lexicalItem.getSymbolClass() != null && potentialKeywordPartClassification.contains(lexicalItem.getSymbolClass()))) && !hasPeriod;
                if (keywordsAllowed) {
                    this.prepareKeywordCompletions(syntaxInspectionResult.predictedWords(), currentWord, completionSets);
                }

                completionSets.removeIf(c -> c == null || c.getItems().isEmpty());

                return completionSets;
            }

            private void prepareInspectedFreeCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull List<SQLQueryCompletionSet> completionSets
            ) {
                if ((syntaxInspectionResult.expectingColumnName() || syntaxInspectionResult.expectingColumnReference())
                    && nameNodes.length == 0
                ) {
                    this.prepareNonPrefixedColumnCompletions(monitor, context.deepestContext(), null, completionSets);
                }
                if (syntaxInspectionResult.expectingTableReference() && nameNodes.length == 0) {
                    this.prepareTableCompletions(monitor, request, context.deepestContext(), null, completionSets);
                }
            }

            private boolean nameNodesAreUseful(@NotNull List<SQLQueryWordEntry> parts) {
                return nameNodes.length > 0 && (parts.size() > 1 || (parts.size() == 1 && parts.get(0) != null));
            }

            @Nullable
            private SQLQueryWordEntry obtainCurrentWord(STMTreeNode currentTerm, int position) {
                if (currentTerm == null) {
                    return null;
                }
                Interval wordRange = currentTerm.getRealInterval();
                if (wordRange.b >= position - 1 && ((currentTerm instanceof STMTreeTermNode t && t.symbol.getType() != SQLStandardLexer.Period) || currentTerm instanceof STMTreeTermErrorNode)) {
                    return new SQLQueryWordEntry(wordRange.a, currentTerm.getTextContent().substring(0, position - currentTerm.getRealInterval().a));
                } else {
                    return null;
                }
            }

            @Nullable
            private void prepareInspectedIdentifierCompletions(@NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull List<SQLQueryWordEntry> parts,
                @NotNull List<SQLQueryCompletionSet> results
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

                // using inferred context when semantics didn't provide the origin
                SQLQueryDataContext defaultContext = context.deepestContext();

                if (syntaxInspectionResult.expectingColumnReference() || syntaxInspectionResult.expectingColumnName()) {
                    this.accomplishColumnReference(monitor, defaultContext, prefix, tail, results);
                } else if (syntaxInspectionResult.expectingTableReference()) {
                    this.accomplishTableReference(monitor, request, defaultContext, prefix, tail, results);
                } else {
                    // do nothing
                }
            }

            @Nullable
            private void accomplishTableReference(@NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQueryDataContext context,
                @NotNull List<SQLQueryWordEntry> prefix,
                @Nullable SQLQueryWordEntry tail,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                if (dbcExecutionContext == null || dbcExecutionContext.getDataSource() == null || !DBStructUtils.isConnectedContainer(dbcExecutionContext.getDataSource())) {
                    // do nothing
                } else if (prefix.isEmpty()) {
                    this.prepareTableCompletions(monitor, request, context, tail, results);
                } else {
                    List<String> contextName = prefix.stream().map(e -> e.string).collect(Collectors.toList());
                    DBSObject prefixObject = SQLSearchUtils.findObjectByFQN(
                        monitor,
                        (DBSObjectContainer) dbcExecutionContext.getDataSource(),
                        dbcExecutionContext,
                        contextName,
                        false,
                        request.getWordDetector()
                    );

                    if (prefixObject != null) {
                        SQLQueryCompletionItem.ContextObjectInfo prefixInfo = this.prepareContextInfo(request, prefix, tail, prefixObject);
                        List<SQLQueryCompletionItem> items = this.accomplishTableReferences(monitor, context, prefixObject, prefixInfo, tail);
                        this.makeFilteredCompletionSet(prefix.isEmpty() ? tail : prefix.get(0), items, results);
                    } else {
                        // do nothing
                    }
                }
            }

            private List<SQLQueryCompletionItem> accomplishTableReferences(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLQueryDataContext context,
                @NotNull DBSObject prefixContext,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo prefixInfo,
                @Nullable SQLQueryWordEntry filterOrNull
            ) {
                LinkedList<SQLQueryCompletionItem> items = new LinkedList<>();
                if (prefixContext instanceof DBSObjectContainer container) {
                    try {
                        this.collectImmediateChildren(monitor, context, container, o -> true, prefixInfo, filterOrNull, items);
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                return items;
            }

            private void collectImmediateChildren(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLQueryDataContext context,
                @NotNull DBSObjectContainer container,
                @Nullable Predicate<DBSObject> filter,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                Collection<? extends DBSObject> children = container.getChildren(monitor);
                for (DBSObject child : children) {
                    if (!DBUtils.isHiddenObject(child) && (filter == null || filter.test(child))) {
                        SQLQueryWordEntry childName = makeFilterInfo(filterOrNull, child.getName());
                        int score = childName.matches(filterOrNull, this.searchInsideWords);
                        if (score > 0) {
                            if (child instanceof DBSEntity o && (child instanceof DBSTable || child instanceof DBSView)) {
                                accumulator.addLast(SQLQueryCompletionItem.forRealTable(
                                    score,
                                    childName,
                                    contextObjext,
                                    o,
                                    context.getKnownSources().getReferencedTables().contains(o)
                                ));
                            } else if (child instanceof DBSSchema || child instanceof DBSCatalog) {
                                accumulator.addLast(SQLQueryCompletionItem.forDbObject(
                                    score,
                                    childName,
                                    contextObjext,
                                    child
                                ));
                            }
                        }
                    }
                }
            }

            @NotNull
            private void accomplishColumnReference(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLQueryDataContext context,
                @NotNull List<SQLQueryWordEntry> prefix,
                @Nullable SQLQueryWordEntry tail,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                if (prefix.size() > 0) { // table-ref-prefixed column
                    this.preparePrefixedColumnCompletions(context, prefix, tail, results);
                } else { // table-ref not introduced yet or non-prefixed column, so try both cases
                    this.prepareNonPrefixedColumnCompletions(monitor, context, tail, results);
                }
            }

            private void preparePrefixedColumnCompletions(
                @NotNull SQLQueryDataContext context,
                @NotNull List<SQLQueryWordEntry> prefix,
                @Nullable SQLQueryWordEntry tail,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                LinkedList<SQLQueryCompletionItem> byAliasItems = new LinkedList<>();
                LinkedList<SQLQueryCompletionItem> byFullNameItems = new LinkedList<>();

                for (SourceResolutionResult rr : context.getKnownSources().getResolutionResults().values()) {

                    boolean sourceAliasMatch;
                    if (prefix.size() == 1) {
                        SQLQueryWordEntry mayBeAliasName = prefix.get(0);
                        sourceAliasMatch = rr.aliasOrNull != null && rr.aliasOrNull.getName().equalsIgnoreCase(mayBeAliasName.filterString);
                    } else {
                        sourceAliasMatch = false;
                    }

                    boolean sourceFullnameMatch;
                    if (rr.tableOrNull != null) {
                        List<String> parts = SQLQueryCompletionItem.prepareQualifiedNameParts(rr.tableOrNull, null);
                        int partsMatched = 0;
                        for (int i = prefix.size() - 1, j = parts.size() - 1; i >= 0 && j >= 0; i--, j--) {
                            if (parts.get(j).equalsIgnoreCase(prefix.get(i).filterString)) { // TODO consider comparison mode here
                                partsMatched++;
                            }
                        }
                        sourceFullnameMatch = partsMatched == prefix.size();
                    } else {
                        sourceFullnameMatch = false;
                    }

                    if (sourceAliasMatch || sourceFullnameMatch) {
                        for (SQLQueryResultColumn c : rr.source.getResultDataContext().getColumnsList()) {
                            SQLQueryWordEntry key = makeFilterInfo(tail, c.symbol.getName());
                            int nameScore = key.matches(tail, this.searchInsideWords);
                            if (nameScore > 0) {
                                if (sourceAliasMatch) {
                                    byAliasItems.addLast(SQLQueryCompletionItem.forSubsetColumn(nameScore, key, c, rr, false));
                                }
                                if (sourceFullnameMatch) {
                                    byFullNameItems.addLast(SQLQueryCompletionItem.forSubsetColumn(nameScore, key, c, rr, true));
                                }
                            }
                        }
                    }
                }

                if (byAliasItems.size() > 0) {
                    this.makeFilteredCompletionSet(tail, byAliasItems, results);
                }
                if (byFullNameItems.size() > 0) {
                    this.makeFilteredCompletionSet(prefix.get(0), byFullNameItems, results);
                }
            }

            @Nullable
            private void prepareObjectComponentCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObject object,
                @NotNull SQLQueryWordEntry componentNamePart,
                @NotNull List<Class<? extends DBSObject>> componentTypes,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                try {
                    Collection<? extends DBSObject> components;
                    if (object instanceof DBSEntity entity) {
                        List<? extends DBSEntityAttribute> attrs = entity.getAttributes(monitor);
                        if (attrs != null) {
                            components = attrs;
                        } else {
                            components = Collections.emptyList();
                        }
                    } else if (object instanceof DBSObjectContainer container && DBStructUtils.isConnectedContainer(container)) {
                        components = container.getChildren(monitor);
                    } else {
                        components = Collections.emptyList();
                    }

                    LinkedList<SQLQueryCompletionItem> items = new LinkedList<>();
                    for (DBSObject o : components) {
                        if (componentTypes.stream().anyMatch(t -> t.isInstance(o))) {
                            SQLQueryWordEntry filter = makeFilterInfo(componentNamePart, o.getName());
                            int score = filter.matches(componentNamePart, this.searchInsideWords);
                            if (score > 0) {
                                items.addLast(SQLQueryCompletionItem.forDbObject(score, filter, null, o));
                            }
                        }
                    }
                    this.makeFilteredCompletionSet(componentNamePart, items, results);
                } catch (DBException ex) {
                    log.error(ex);
                }
            }

            private List<SQLQueryWordEntry> obtainIdentifierParts(int position) {
                List<SQLQueryWordEntry> parts = new ArrayList<>(nameNodes.length);
                int i = 0;
                for (; i < nameNodes.length; i++) {
                    STMTreeNode term = nameNodes[i];
                    if ((term instanceof STMTreeTermNode t && t.symbol.getType() != SQLStandardLexer.Period)||term instanceof  STMTreeTermErrorNode) {
                        if (term.getRealInterval().b + 1 < position) {
                            parts.add(new SQLQueryWordEntry(term.getRealInterval().a, term.getTextContent()));
                        } else {
                            break;
                        }
                    }
                }
                STMTreeNode currentNode = i >= nameNodes.length ? null : nameNodes[i];
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
            private void prepareLexicalItemCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQueryLexicalScopeItem lexicalItem,
                int position,
                List<SQLQueryWordEntry> parts,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                if (lexicalItem instanceof SQLQueryQualifiedName qname) {
                    Interval pos = Interval.of(position - 1, position - 1);
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
                                this.prepareObjectComponentCompletions(monitor, byObjDef.getDbObject(), part, List.of(DBSEntity.class), results);
                            } else {
                                // schema was not resolved, so cannot accomplish its subitems
                            }
                        } else {
                            this.prepareInspectedIdentifierCompletions(monitor, request, List.of(part), results);
                        }
                    } else if (schemaName != null && (schemaRange = schemaName.getSyntaxNode().getRealInterval()).properlyContains(pos)) {
                        SQLQueryWordEntry part = new SQLQueryWordEntry(schemaName.getInterval().a, schemaName.getRawName().substring(0, position - schemaRange.a));
                        if (catalogName != null) {
                            SQLQuerySymbolDefinition scopeDef = this.unrollSymbolDefinition(schemaName.getDefinition());
                            if (scopeDef instanceof SQLQuerySymbolByDbObjectDefinition byObjDef) {
                                this.prepareObjectComponentCompletions(monitor, byObjDef.getDbObject(), part, List.of(DBSSchema.class), results);
                            } else {
                                // catalog was not resolved, so cannot accomplish schema
                            }
                        } else {
                            this.prepareObjectComponentCompletions(monitor, dbcExecutionContext.getDataSource(), part, List.of(DBSSchema.class), results);
                        }
                    } else if (dbcExecutionContext != null && catalogName != null && (catalogRange = catalogName.getSyntaxNode().getRealInterval()).properlyContains(pos)) {
                        SQLQueryWordEntry part = new SQLQueryWordEntry(catalogName.getInterval().a, catalogName.getRawName().substring(0, position - catalogRange.a));
                        this.prepareObjectComponentCompletions(monitor, dbcExecutionContext.getDataSource(), part, List.of(DBSCatalog.class), results);
                    } else {
                        throw new UnsupportedOperationException("Illegal SQLQueryQualifiedName");
                    }
                } else if (lexicalItem instanceof SQLQueryMemberAccessEntry entry) {
                    this.accomplishFromKnownOriginOrFallback(monitor, request, entry.getOrigin(), null, parts, results);
                } else if (lexicalItem instanceof SQLQuerySymbolEntry entry) {
                    Interval nameRange = entry.getSyntaxNode().getRealInterval();
                    SQLQueryWordEntry namePart = new SQLQueryWordEntry(nameRange.a, entry.getRawName().substring(0, position - nameRange.a));
                    this.accomplishFromKnownOriginOrFallback(monitor, request, entry.getOrigin(), namePart, parts, results);
                } else {
                    throw new UnsupportedOperationException("Unexpected lexical item kind to complete " + lexicalItem.getClass().getName());
                }
            }

            @Nullable
            private void accomplishFromKnownOriginOrFallback(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @Nullable SQLQuerySymbolOrigin origin,
                @Nullable SQLQueryWordEntry originBasedFilterOrNull,
                @NotNull List<SQLQueryWordEntry> parts,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                if (origin != null) {
                    this.accomplishFromKnownOrigin(monitor, request, origin, originBasedFilterOrNull, results);
                } else if (this.nameNodesAreUseful(parts)) {
                    this.prepareInspectedIdentifierCompletions(monitor, request, parts, results);
                } else {
                    // do nothing
                }
            }

            private void accomplishFromKnownOrigin(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQuerySymbolOrigin origin,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                origin.apply(new SQLQuerySymbolOrigin.Visitor() {
                    @Override
                    public void visitDbObjectFromDbObject(SQLQuerySymbolOrigin.DbObjectFromDbObject origin) {
                        SQLQueryCompletionItem.ContextObjectInfo prefixInfo = new SQLQueryCompletionItem.ContextObjectInfo("", origin.getObject(), true);
                        makeFilteredCompletionSet(
                            filterOrNull,
                            accomplishTableReferences(monitor, context.deepestContext(), origin.getObject(), prefixInfo, filterOrNull),
                            results
                        );
                    }

                    @Override
                    public void visitDbObjectFromContext(SQLQuerySymbolOrigin.DbObjectFromContext origin) {
                        prepareTableCompletions(monitor, request, origin.getDataContext(), filterOrNull, results);
                    }

                    @Override
                    public void visitRowsetRefFromContext(SQLQuerySymbolOrigin.RowsetRefFromContext origin) {
                        prepareTableCompletions(monitor, request, origin.getDataContext(), filterOrNull, results);
                    }

                    @Override
                    public void visitValueRefFromContext(SQLQuerySymbolOrigin.ValueRefFromContext origin) {
                        prepareNonPrefixedColumnCompletions(monitor, origin.getDataContext(), filterOrNull, results);
                    }

                    @Override
                    public void visitColumnRefFromReferencedContext(SQLQuerySymbolOrigin.ColumnRefFromReferencedContext origin) {
                        makeFilteredCompletionSet(
                            filterOrNull,
                            prepareTupleColumns(origin.getRowsSource().source.getResultDataContext(), filterOrNull, false),
                            results
                        );
                    }

                    @Override
                    public void visitColumnNameFromContext(SQLQuerySymbolOrigin.ColumnNameFromContext origin) {
                        makeFilteredCompletionSet(filterOrNull, prepareTupleColumns(origin.getDataContext(), filterOrNull, false), results);
                    }

                    @Override
                    public void visitMemberOfType(SQLQuerySymbolOrigin.MemberOfType origin) {
                        accomplishMemberReference(monitor, origin.getType(), filterOrNull, results);
                    }

                    @Override
                    public void visitDataContextSymbol(SQLQuerySymbolOrigin.DataContextSymbolOrigin origin) {
                        prepareInspectedFreeCompletions(monitor, request, results);
                    }
                });
            }

            private void accomplishMemberReference(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLQueryExprType compositeType,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                LinkedList<SQLQueryCompletionItem> items = new LinkedList<>();
                try {
                    List<SQLQueryExprType.SQLQueryExprTypeMemberInfo> members = compositeType.getNamedMembers(monitor);
                    for (SQLQueryExprType.SQLQueryExprTypeMemberInfo member : members) {
                        SQLQueryWordEntry itemKey = makeFilterInfo(filterOrNull, member.name());
                        int score = itemKey.matches(filterOrNull, searchInsideWords);
                        if (score > 0) {
                            SQLQueryCompletionItem item;
                            if (member.column() != null) {
                                item = SQLQueryCompletionItem.forSubsetColumn(score, itemKey, member.column(), null, false);
                            } else if (member.attribute() != null) {
                                item = SQLQueryCompletionItem.forCompositeField(score, itemKey, member.attribute(), member);
                            } else {
                                throw new UnsupportedOperationException("Unexpected named member kind to complete.");
                            }
                            items.addLast(item);
                        }
                    }
                } catch (DBException e) {
                    log.error(e);
                }
                makeFilteredCompletionSet(filterOrNull, items, results);
            }

            private List<SQLQueryCompletionItem> prepareJoinConditionCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLQueryDataContext context,
                @Nullable SQLQueryWordEntry filterOrNull
            ) {
                class AssociationsResolutionContext {
                    public final Map<DBSEntityAttribute, List<SQLQueryCompletionItem.SQLColumnNameCompletionItem>> realColumnRefsByEntityAttribute = context.getColumnsList().stream()
                        .filter(rc -> rc.realAttr != null && rc.realAttr.getParentObject() == rc.realSource)
                        .collect(Collectors.groupingBy(rc -> rc.realAttr)).entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, g -> g.getValue().stream().map(rc -> {
                            SQLQueryWordEntry word = makeFilterInfo(null, rc.symbol.getName());
                            int score = word.matches(filterOrNull, searchInsideWords);
                            return SQLQueryCompletionItem.forSubsetColumn(
                                score, word, rc, context.getKnownSources().getResolutionResults().get(rc.source), true
                            );
                        }).toList()));

                    private final Map<DBSEntity, Map<DBSEntityAttribute, List<DBSEntityAttribute>>> associatedAttrsByEntity = new HashMap<>();

                    public List<DBSEntityAttribute> findAssociations(DBSEntityAttribute key) {
                        return Optional.ofNullable(this.associatedAttrsByEntity.computeIfAbsent(key.getParentObject(), this::prepareAllAssociations).get(key)).orElse(Collections.emptyList());
                    }

                    private Map<DBSEntityAttribute, List<DBSEntityAttribute>> prepareAllAssociations(DBSEntity entity) {
                        try {
                            return Stream.concat(
                                Optional.ofNullable(entity.getAssociations(monitor)).stream().flatMap(Collection::stream)
                                    .filter(a -> context.getKnownSources().getReferencedTables().contains(a.getAssociatedEntity())),
                                Optional.ofNullable(entity.getReferences(monitor)).stream().flatMap(Collection::stream)
                                    .filter(r -> context.getKnownSources().getReferencedTables().contains(r.getParentObject()))
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

                if (context.getKnownSources().getReferencedTables().size() > 1 && context.getKnownSources().getResolutionResults().size() > 1
                    && context instanceof SQLQueryCombinedContext joinContext && joinContext.isJoin()) {

                    AssociationsResolutionContext associations = new AssociationsResolutionContext();

                    for (SQLQueryResultColumn joinedColumn : joinContext.getRightParent().getColumnsList()) {
                        if (joinedColumn.realAttr != null) {
                            for (DBSEntityAttribute otherColumnAttribute : associations.findAssociations(joinedColumn.realAttr)) {
                                List<SQLQueryCompletionItem.SQLColumnNameCompletionItem> otherColumnRefs = associations.realColumnRefsByEntityAttribute.get(otherColumnAttribute);
                                if (otherColumnRefs != null) {
                                    for (SQLQueryCompletionItem.SQLColumnNameCompletionItem thisColumnRef : associations.realColumnRefsByEntityAttribute.get(joinedColumn.realAttr)) {
                                        if (thisColumnRef.columnInfo == joinedColumn) {
                                            for (SQLQueryCompletionItem.SQLColumnNameCompletionItem otherColumnRef : otherColumnRefs) {
                                                int thisScore = thisColumnRef.getScore();
                                                int otherScore = otherColumnRef.getScore();
                                                if (thisScore > 0 || otherScore > 0) {
                                                    int score = thisScore >= otherScore ? thisScore : otherScore;
                                                    SQLQueryWordEntry word = (thisScore >= otherScore ? thisColumnRef : otherColumnRef).getFilterInfo();

                                                    result.addLast(SQLQueryCompletionItem.forJoinCondition(
                                                        score, word,
                                                        thisColumnRef,
                                                        otherColumnRef
                                                    ));
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
            private void prepareNonPrefixedColumnCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLQueryDataContext context,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                // directly available column
                List<? extends SQLQueryCompletionItem> subsetColumns = this.prepareTupleColumns(context, filterOrNull, true);
                // already referenced tables
                LinkedList<SQLQueryCompletionItem> tableRefs = new LinkedList<>();
                if (syntaxInspectionResult.expectingColumnReference()) {
                    for (SourceResolutionResult rr : context.getKnownSources().getResolutionResults().values()) {
                        if (rr.aliasOrNull != null && !rr.isCteSubquery) {
                            SQLQueryWordEntry sourceAlias = makeFilterInfo(filterOrNull, rr.aliasOrNull.getName());
                            int score = sourceAlias.matches(filterOrNull, this.searchInsideWords);
                            if (score > 0) {
                                tableRefs.add(SQLQueryCompletionItem.forRowsSourceAlias(score, sourceAlias, rr.aliasOrNull, rr));
                            }
                        } else if (rr.tableOrNull != null) {
                            SQLQueryWordEntry tableName = makeFilterInfo(filterOrNull, rr.tableOrNull.getName());
                            int score = tableName.matches(filterOrNull, this.searchInsideWords);
                            if (score > 0) {
                                tableRefs.add(SQLQueryCompletionItem.forRealTable(score, tableName, null, rr.tableOrNull, true));
                            }
                        }
                    }
                }

                List<SQLQueryCompletionItem> joinConditions = syntaxInspectionResult.expectingJoinCondition()
                    ? this.prepareJoinConditionCompletions(monitor, context, filterOrNull)
                    : Collections.emptyList();

                this.makeFilteredCompletionSet(filterOrNull, Stream.of(subsetColumns, tableRefs, joinConditions).flatMap(Collection::stream).toList(), results);
            }

            @NotNull
            @Override
            public List<? extends SQLQueryCompletionItem> prepareCurrentTupleColumns() {
                return this.prepareTupleColumns(context.deepestContext(), null, true);
            }

            @NotNull
            private List<? extends SQLQueryCompletionItem> prepareTupleColumns(@NotNull SQLQueryDataContext context, @Nullable SQLQueryWordEntry filterOrNull, boolean absolute) {
                Stream<? extends SQLQueryCompletionItem> subsetColumns = context.getColumnsList().stream()
                    .map(rc -> {
                        SQLQueryWordEntry filterKey = makeFilterInfo(filterOrNull, rc.symbol.getName());
                        int score = filterKey.matches(filterOrNull, this.searchInsideWords);
                        return score <= 0 ? null : SQLQueryCompletionItem.forSubsetColumn(score, filterKey, rc, context.getKnownSources().getResolutionResults().get(rc.source), absolute);
                    }).filter(Objects::nonNull);

                return subsetColumns.toList();
            }

            @NotNull
            private void prepareTableCompletions(@NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQueryDataContext context,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                LinkedList<SQLQueryCompletionItem> completions = new LinkedList<>();
                for (SourceResolutionResult rr : context.getKnownSources().getResolutionResults().values()) {
                    if (rr.aliasOrNull != null && rr.isCteSubquery) {
                        SQLQueryWordEntry aliasName = makeFilterInfo(filterOrNull, rr.aliasOrNull.getName());
                        int score = aliasName.matches(filterOrNull, this.searchInsideWords);
                        if (score > 0) {
                            completions.add(SQLQueryCompletionItem.forRowsSourceAlias(score, aliasName, rr.aliasOrNull, rr));
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
                                this.collectTables(monitor, context, container, null, filterOrNull, completions);
                            } else if ((request.getContext().isSearchGlobally() || defaultSchema == null) && defaultCatalog != null) {
                                this.collectTables(monitor, context, defaultCatalog, null, filterOrNull, completions);
                            } else if (defaultSchema != null) {
                                this.collectTables(monitor, context, defaultSchema, null, filterOrNull, completions);
                            }
                        }

                        this.collectContextSchemasAndCatalogs(monitor, this.exposedContexts, null, filterOrNull, completions);
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                
                this.makeFilteredCompletionSet(filterOrNull, completions, results);
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
                            int score = childName.matches(filterOrNull, this.searchInsideWords);
                            if (score > 0) {
                                completions.addLast(SQLQueryCompletionItem.forDbObject(score, childName, contextObjext, child));
                            }
                        }
                    }
                }
            }
            private void collectTables(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLQueryDataContext context,
                @NotNull DBSObjectContainer container,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                this.collectImmediateChildren(
                    monitor, context, container,
                    o -> o instanceof DBSTable || o instanceof DBSView,
                    null, filterOrNull, accumulator
                );
            }

            private SQLQueryCompletionItem.ContextObjectInfo prepareContextInfo(@NotNull SQLCompletionRequest request, @NotNull List<SQLQueryWordEntry> prefix, @Nullable SQLQueryWordEntry tail, @NotNull DBSObject contextObject) {
                if (contextObject != null) {
                    int prefixStart = prefix.get(0).offset;
                    int requestPosition = tail != null ? tail.offset : (requestOffset - scriptItem.offset);
                    String prefixString = scriptItem.item.getOriginalText().substring(prefixStart, requestPosition);
                    return new SQLQueryCompletionItem.ContextObjectInfo(prefixString, contextObject, false);
                } else {
                    return null;
                }
            }
        };
    }

    protected void prepareKeywordCompletions(
        @NotNull Set<String> keywords,
        @Nullable SQLQueryWordEntry filterOrNull,
        @NotNull List<SQLQueryCompletionSet> results
    ) {
        LinkedList<SQLQueryCompletionItem> items = new LinkedList<>();
        for (String s : keywords) {
            SQLQueryWordEntry filterWord = makeFilterInfo(filterOrNull, s);
            int score = filterWord.matches(filterOrNull, this.searchInsideWords);
            if (score > 0) {
                items.addLast(SQLQueryCompletionItem.forReservedWord(score, filterWord, s));
            }
        }
        this.makeFilteredCompletionSet(filterOrNull, items, results);
    }

    protected void makeFilteredCompletionSet(@Nullable SQLQueryWordEntry filterOrNull, List<? extends SQLQueryCompletionItem> items, @NotNull List<SQLQueryCompletionSet> results
    ) {
        int replacementPosition = filterOrNull == null ? this.getRequestOffset() : this.getOffset() + filterOrNull.offset;
        int replacementLength = this.getRequestOffset() - replacementPosition;
        results.add(new SQLQueryCompletionSet(replacementPosition, replacementLength, items));
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

    @FunctionalInterface
    private interface CompletionItemProducer<T> {
        SQLQueryCompletionItem produce(int score, SQLQueryWordEntry key, T object);
    }
}
