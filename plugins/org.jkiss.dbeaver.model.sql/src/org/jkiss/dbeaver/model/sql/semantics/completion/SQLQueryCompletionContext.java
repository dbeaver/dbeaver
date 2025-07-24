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
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryTupleRefEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
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

            @NotNull
            @Override
            public SQLQueryDataContextInfo getDataContext() {
                return SQLQueryDataContextInfo.empty();
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

            @NotNull
            @Override
            public SQLQueryDataContextInfo getDataContext() {
                return SQLQueryDataContextInfo.empty();
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

    @NotNull
    public abstract SQLQueryDataContextInfo getDataContext();

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

    public boolean isColumnNameConflicting(String name) {
        return false;
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
            private Map<String, Boolean> columnNameConflicts = null;

            private SQLQueryDataContextInfo nearestContext = SQLQueryDataContextInfo.empty();
            private SQLQueryDataContextInfo deepestContext = SQLQueryDataContextInfo.empty();

            private void setContextInfo(SQLQueryDataContextInfo contextInfo) {
                this.nearestContext = contextInfo;
                this.deepestContext = contextInfo;
            }

            private void tryApplyOriginContext() {
                if (context.symbolsOrigin() instanceof SQLQuerySymbolOrigin.RowsSourceRef rowsSourceOrigin) {
                    this.setContextInfo(SQLQueryDataContextInfo.makeFor(rowsSourceOrigin.getRowsSourceContext()));
                } else if (context.symbolsOrigin() instanceof SQLQuerySymbolOrigin.RowsDataRef rowsDataOrigin) {
                    this.setContextInfo(SQLQueryDataContextInfo.makeFor(rowsDataOrigin.getRowsDataContext()));
                }
            }

            @NotNull
            @Override
            public SQLQueryDataContextInfo getDataContext() {
                return this.deepestContext;
            }

            @NotNull
            @Override
            public LSMInspections.SyntaxInspectionResult getInspectionResult() {
                return syntaxInspectionResult;
            }

            @NotNull
            @Override
            public Set<String> getAliasesInUse() {
                return this.nearestContext.getKnownSources().getAliasesInUse();
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
                }  else if (this.nameNodesAreUseful(parts)) {
                    this.tryApplyOriginContext();
                    this.prepareInspectedIdentifierCompletions(monitor, request, parts, completionSets);
                } else if (context.symbolsOrigin() != null) {
                    this.accomplishFromKnownOrigin(monitor, request, context.symbolsOrigin(), null, completionSets);
                } else if (syntaxInspectionResult.expectingIdentifier()) {
                    this.tryApplyOriginContext();
                    this.prepareInspectedIdentifierCompletions(monitor, request, parts, completionSets);
                } else {
                    this.tryApplyOriginContext();
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
                    this.prepareNonPrefixedColumnCompletions(monitor, request, this.deepestContext, null, completionSets);
                }
                if (syntaxInspectionResult.expectingTableReference() && nameNodes.length == 0) {
                    this.prepareTableCompletions(monitor, request, this.deepestContext.getKnownSources(), null, completionSets);
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

            private void prepareInspectedIdentifierCompletions(@NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull List<SQLQueryWordEntry> parts,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                List<SQLQueryWordEntry> prefix = parts.subList(0, parts.size() - 1);
                SQLQueryWordEntry tail = parts.get(parts.size() - 1);
                if (tail != null) {
                    String[][] quoteStrs = request.getContext().getDataSource().getSQLDialect().getIdentifierQuoteStrings();
                    if (quoteStrs != null && quoteStrs.length > 0) {
                        // The "word" being accomplished may be a quoted or a beginning of the quoted identifier,
                        // so we should remove potential quotes.
                        // TODO Consider identifiers containing escape-sequences
                        String qp = Stream.of(quoteStrs).flatMap(ss -> Stream.of(ss)).map(Pattern::quote).distinct().collect(Collectors.joining("|"));
                        tail = new SQLQueryWordEntry(tail.offset, tail.string.replaceAll(qp, ""));

                        // TODO Consider force identifier quotation (see testQuotedNamesCompletion)
                    }
                }

                // using inferred context when semantics didn't provide the origin
                SQLQueryDataContextInfo defaultContext = this.deepestContext;

                if (syntaxInspectionResult.expectingColumnReference() || syntaxInspectionResult.expectingColumnName()) {
                    this.accomplishColumnReference(monitor, request, defaultContext, prefix, tail, results);
                } else if (syntaxInspectionResult.expectingTableReference()) {
                    this.accomplishTableReference(monitor, request, defaultContext, prefix, tail, results);
                } else {
                    // do nothing
                }
            }

            private void accomplishTableReference(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQueryDataContextInfo context,
                @NotNull List<SQLQueryWordEntry> prefix,
                @Nullable SQLQueryWordEntry tail,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                if (dbcExecutionContext == null || dbcExecutionContext.getDataSource() == null || !DBStructUtils.isConnectedContainer(dbcExecutionContext.getDataSource())) {
                    // do nothing
                } else if (prefix.isEmpty()) {
                    this.prepareTableCompletions(monitor, request, context.getKnownSources(), tail, results);
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
                        List<SQLQueryCompletionItem> items = this.accomplishTableReferences(
                            monitor,
                            request,
                            context.getKnownSources(),
                            prefixObject,
                            prefixInfo,
                            tail
                        );
                        this.makeFilteredCompletionSet(prefix.isEmpty() ? tail : prefix.get(0), items, results);
                    } else {
                        // do nothing
                    }
                }
            }

            private List<SQLQueryCompletionItem> accomplishTableReferences(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull  SQLQuerySourcesInfoCollection knownSources,
                @NotNull DBSObject prefixContext,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo prefixInfo,
                @Nullable SQLQueryWordEntry filterOrNull
            ) {
                LinkedList<SQLQueryCompletionItem> items = new LinkedList<>();
                if (prefixContext instanceof DBSObjectContainer container) {
                    Set<Class<?>> expectedTypes = new HashSet<>();
                    expectedTypes.add(DBSSchema.class);
                    expectedTypes.add(DBSCatalog.class);
                    expectedTypes.add(DBSTable.class);
                    expectedTypes.add(DBSView.class);
                    expectedTypes.add(DBSAlias.class);
                    if (request.getContext().isSearchProcedures()) {
                        expectedTypes.add(DBSProcedure.class);
                        expectedTypes.add(DBSPackage.class);
                    }
                    try {
                        this.collectImmediateChildren(
                            monitor,
                            knownSources,
                            List.of(container),
                            o -> expectedTypes.stream().anyMatch(c -> c.isAssignableFrom(o.getClass())),
                            prefixInfo,
                            filterOrNull,
                            items
                        );
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                return items;
            }

            private void collectImmediateChildren(
                @NotNull DBRProgressMonitor monitor,
                @NotNull  SQLQuerySourcesInfoCollection knownSources,
                @NotNull Collection<DBSObjectContainer> containers,
                @Nullable Predicate<DBSObject> filter,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                for (DBSObjectContainer container : containers) {
                    Collection<? extends DBSObject> children = container.getChildren(monitor);
                    for (DBSObject child : children) {
                        if (!DBUtils.isHiddenObject(child) && (filter == null || filter.test(child))) {
                            SQLQueryWordEntry childName = makeFilterInfo(filterOrNull, child.getName());
                            int score = childName.matches(filterOrNull, this.searchInsideWords);
                            if (score > 0) {
                                if (child instanceof DBSEntity o && (child instanceof DBSTable || child instanceof DBSView)) {
                                    accumulator.addLast(SQLQueryCompletionItem.forRealTable(score, childName, contextObjext, o,
                                        knownSources.getReferencedTables().contains(o)
                                    ));
                                } else {
                                    accumulator.addLast(this.makeDbObjectCompletionItem(score, childName, contextObjext, child));
                                }
                            }
                        }
                    }
                }
            }

            private SQLQueryCompletionItem makeDbObjectCompletionItem(
                int score,
                @NotNull SQLQueryWordEntry childName,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @NotNull DBSObject child
            ) {
                SQLQueryCompletionItem item;
                if (child instanceof DBSProcedure p) {
                    item = SQLQueryCompletionItem.forProcedureObject(score, childName, contextObjext, p);
                } else if (child instanceof DBSCatalog p) {
                    item = SQLQueryCompletionItem.forDbCatalogObject(score, childName, contextObjext, child);
                } else if (child instanceof DBSSchema p) {
                    item = SQLQueryCompletionItem.forDbSchemaObject(score, childName, contextObjext, child);
                } else {
                    item = SQLQueryCompletionItem.forDbObject(score, childName, contextObjext, child);
                }
                return item;
            }

            @NotNull
            private void accomplishColumnReference(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQueryDataContextInfo context,
                @NotNull List<SQLQueryWordEntry> prefix,
                @Nullable SQLQueryWordEntry tail,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                if (prefix.size() > 0) { // table-ref-prefixed column
                    this.preparePrefixedColumnCompletions(context, prefix, tail, results);
                } else { // table-ref not introduced yet or non-prefixed column, so try both cases
                    this.prepareNonPrefixedColumnCompletions(monitor, request, context, tail, results);
                }
            }

            private void preparePrefixedColumnCompletions(
                @NotNull SQLQueryDataContextInfo context,
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
                        for (SQLQueryResultColumn c : rr.source.getRowsDataContext().getColumnsList()) {
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
                                items.addLast(this.makeDbObjectCompletionItem(score, filter, null, o));
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

            private void prepareLexicalItemCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQueryLexicalScopeItem lexicalItem,
                int position,
                List<SQLQueryWordEntry> parts,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                if (lexicalItem instanceof SQLQueryTupleRefEntry tupleRef) {
                    this.accomplishFromKnownOriginOrFallback(monitor, request, tupleRef.getOrigin(), null, parts, results);
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
                SQLQueryCompletionContext completionContext = this;
                origin.apply(new SQLQuerySymbolOrigin.Visitor() {
                    @Override
                    public void visitDbObjectFromDbObject(SQLQuerySymbolOrigin.DbObjectFromDbObject origin) {
                        SQLQueryCompletionItem.ContextObjectInfo prefix = new SQLQueryCompletionItem.ContextObjectInfo(
                            "",
                            origin.getObject(),
                            true
                        );
                        if (origin.getMemberTypes().size() == 1 && origin.getMemberTypes().contains(RelationalObjectType.TYPE_UNKNOWN)) {
                            makeFilteredCompletionSet(
                                filterOrNull,
                                accomplishTableReferences(
                                    monitor,
                                    request,
                                    deepestContext.getKnownSources(),
                                    origin.getObject(),
                                    prefix,
                                    filterOrNull
                                ),
                                results
                            );
                        } else if (origin.getObject() instanceof DBSObjectContainer container) {
                            prepareObjectCompletions(
                                monitor,
                                request,
                                deepestContext.getKnownSources(),
                                List.of(container),
                                prefix,
                                origin.getMemberTypes(),
                                filterOrNull,
                                results
                            );
                        }
                    }

                    @Override
                    public void visitDbObjectRef(SQLQuerySymbolOrigin.DbObjectRef origin) {
                        SQLQueryDataContextInfo contextInfo = SQLQueryDataContextInfo.makeFor(origin.getRowsSourceContext());
                        setContextInfo(contextInfo);

                        if (origin.isIncludingRowsets()) {
                            prepareTableCompletions(monitor, request, contextInfo.getKnownSources(), filterOrNull, results);
                        } else {
                            Collection<DBSObjectContainer> container = obtainDefaultContext(monitor, request);
                            if (container != null) {
                                prepareObjectCompletions(
                                    monitor,
                                    request,
                                    contextInfo.getKnownSources(),
                                    container,
                                    null,
                                    origin.getObjectTypes(),
                                    filterOrNull,
                                    results
                                );
                            }
                            prepareContextSchemasAndCatalogs(monitor, exposedContexts, null, filterOrNull, results);
                        }
                    }

                    @Override
                    public void visitColumnRefFromReferencedContext(SQLQuerySymbolOrigin.ColumnRefFromReferencedContext origin) {
                        makeFilteredCompletionSet(filterOrNull, prepareTupleColumns(
                            SQLQueryDataContextInfo.makeFor(origin.getRowsSource().source.getRowsDataContext()),
                            filterOrNull,
                            false
                        ), results);
                    }

                    @Override
                    public void visitMemberOfType(SQLQuerySymbolOrigin.MemberOfType origin) {
                        accomplishMemberReference(monitor, origin.getType(), filterOrNull, results);
                    }

                    @Override
                    public void visitRowsSourceRef(@NotNull SQLQuerySymbolOrigin.RowsSourceRef rowsSourceRef) {
                        SQLQuerySourcesInfoCollection knownSources = rowsSourceRef.getRowsSourceContext().getKnownSources(false);
                        setContextInfo(SQLQueryDataContextInfo.makeFor(rowsSourceRef.getRowsSourceContext()));
                        prepareTableCompletions(monitor, request, knownSources, filterOrNull, results);
                    }

                    @Override
                    public void visitRowsDataRef(@NotNull SQLQuerySymbolOrigin.RowsDataRef rowsDataRef) {
                        SQLQueryDataContextInfo contextInfo = SQLQueryDataContextInfo.makeFor(rowsDataRef.getRowsDataContext());
                        setContextInfo(contextInfo);
                        prepareNonPrefixedColumnCompletions(monitor, request, contextInfo, filterOrNull, results);
                    }

                    @Override
                    public void visitExpandableRowsTupleRef(SQLQuerySymbolOrigin.ExpandableRowsTupleRef origin) {
                        SQLQueryRowsDataContext tupleSource = origin.getReferencedSource() != null
                            ? origin.getReferencedSource().source.getRowsDataContext()
                            : origin.getRowsDataContext();

                        SQLQueryDataContextInfo contextInfo = SQLQueryDataContextInfo.makeFor(tupleSource);
                        setContextInfo(contextInfo);
                        prepareTupleRefExpansionCompletiom(origin.getPlaceholder(), contextInfo, request, completionContext, monitor, results);
                    }

                    @Override
                    public void visitColumnNameFromRowsData(SQLQuerySymbolOrigin.ColumnNameFromRowsData origin) {
                        SQLQueryDataContextInfo contextInfo = SQLQueryDataContextInfo.makeFor(origin.getRowsDataContext());
                        setContextInfo(contextInfo);
                        makeFilteredCompletionSet(filterOrNull, prepareTupleColumns(contextInfo, filterOrNull, false), results);
                    }

                    @Override
                    public void visitSyntaxBasedFromRowsData(SQLQuerySymbolOrigin.SyntaxBasedFromRowsData origin) {
                        SQLQueryDataContextInfo contextInfo = SQLQueryDataContextInfo.makeFor(origin.getRowsDataContext());
                        setContextInfo(contextInfo);
                        prepareInspectedFreeCompletions(monitor, request, results);
                    }

                });
            }

            private void prepareTupleRefExpansionCompletiom(
                STMTreeNode placeholder,
                SQLQueryDataContextInfo contextInfo,
                @NotNull SQLCompletionRequest request,
                SQLQueryCompletionContext completionContext,
                @NotNull DBRProgressMonitor monitor,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                Interval placeholderInterval = placeholder.getRealInterval();
                if (getRequestOffset() - getOffset() == placeholderInterval.b + 1) {
                    SQLQueryWordEntry placeholderEntry = new SQLQueryWordEntry(
                        placeholderInterval.a,
                        placeholder.getTextContent()
                    );

                    SQLQueryCompletionTextProvider formatter = new SQLQueryCompletionTextProvider(
                        request,
                        completionContext,
                        monitor
                    );
                    String columnListString = prepareTupleColumns(contextInfo, null, true)
                        .stream()
                        .map(c -> c.apply(formatter))
                        .collect(Collectors.joining(", "));
                    request.setWordPart(SQLConstants.ASTERISK);

                    makeFilteredCompletionSet(placeholderEntry, List.of(
                        SQLQueryCompletionItem.forSpecialText(
                            1, makeFilterInfo(placeholderEntry, ""), columnListString, "Tuple columns expansion"
                        )
                    ), results);
                }
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
                @NotNull SQLQueryDataContextInfo context,
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

                if (context.getKnownSources().getReferencedTables().size() > 1 &&
                    context.getKnownSources().getResolutionResults().size() > 1 &&
                    context.isJoin()
                ) {

                    AssociationsResolutionContext associations = new AssociationsResolutionContext();

                    for (SQLQueryResultColumn joinedColumn : context.getRightParentColumnsList()) {
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
                                                    int score = Math.max(thisScore, otherScore);
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

            private void prepareNonPrefixedColumnCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQueryDataContextInfo context,
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

                LinkedList<SQLQueryCompletionItem> procedureItems = this.prepareProceduresCompletions(
                    monitor,
                    request,
                    context.getKnownSources(),
                    null,
                    filterOrNull
                );
                this.makeFilteredCompletionSet(
                    filterOrNull,
                    Stream.of(joinConditions, subsetColumns, tableRefs, procedureItems).flatMap(Collection::stream).toList(),
                    results
                );
            }

            @NotNull
            private LinkedList<SQLQueryCompletionItem> prepareProceduresCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQuerySourcesInfoCollection knownSources,
                @Nullable List<DBSObjectContainer> container,
                @Nullable SQLQueryWordEntry filterOrNull
            ) {
                Collection<DBSObjectContainer> objectContainers = container;
                if (objectContainers == null) {
                    objectContainers = this.obtainDefaultContext(monitor, request);
                }
                LinkedList<SQLQueryCompletionItem> proceduresItems = new LinkedList<>();
                try {
                    this.collectProcedures(monitor, request, objectContainers, null, filterOrNull, proceduresItems);
                    this.collectPackages(monitor, request, knownSources, this.exposedContexts, null, filterOrNull, proceduresItems);
                } catch (DBException ex) {
                    log.error(ex);
                }
                return proceduresItems;
            }

            @Override
            public boolean isColumnNameConflicting(String name) {
                if (this.columnNameConflicts == null) {
                    this.columnNameConflicts = this.getDataContext().getColumnsList().stream()
                        .collect(Collectors.groupingBy(c -> c.symbol.getName())).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().size() > 1));
                }
                return this.columnNameConflicts.get(name);
            }

            @NotNull
            private List<? extends SQLQueryCompletionItem> prepareTupleColumns(
                @NotNull SQLQueryDataContextInfo dataContext,
                @Nullable SQLQueryWordEntry filterOrNull,
                boolean useAbsoluteName
            ) {
                SQLQuerySourcesInfoCollection knownSources = dataContext.getKnownSources();
                Stream<? extends SQLQueryCompletionItem> subsetColumns = dataContext.getColumnsList().stream()
                    .map(rc -> {
                        SQLQueryWordEntry filterKey = makeFilterInfo(filterOrNull, rc.symbol.getName());
                        int score = filterKey.matches(filterOrNull, this.searchInsideWords);
                        return score <= 0 ? null : SQLQueryCompletionItem.forSubsetColumn(
                            score, filterKey, rc, knownSources.getResolutionResults().get(rc.source), useAbsoluteName
                        );
                    }).filter(Objects::nonNull);

                return subsetColumns.toList();
            }

            private void prepareTableCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQuerySourcesInfoCollection knownSources,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                LinkedList<SQLQueryCompletionItem> completions = new LinkedList<>();
                for (SourceResolutionResult rr : knownSources.getResolutionResults().values()) {
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
                        Collection<DBSObjectContainer> containers = this.obtainDefaultContext(monitor, request);
                        this.collectTables(monitor, knownSources, containers, null, filterOrNull, completions);
                        // usually we don't want procedures in FROM
                        //this.collectProcedures(monitor, request, containers, null, filterOrNull, completions);
                        this.collectPackages(monitor, request, knownSources, this.exposedContexts,  null, filterOrNull, completions);
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                
                this.makeFilteredCompletionSet(filterOrNull, completions, results);
                this.prepareContextSchemasAndCatalogs(monitor, this.exposedContexts, null, filterOrNull, results);
            }

            private void prepareObjectCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQuerySourcesInfoCollection knownSources,
                @NotNull Collection<DBSObjectContainer> contexts,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @NotNull Set<DBSObjectType> objectTypes,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                LinkedList<SQLQueryCompletionItem> completions = new LinkedList<>();
                Set<DBSObject> objs = new HashSet<>();
                try {
                    this.collectImmediateChildren(
                        monitor,
                        knownSources,
                        contexts,
                        o -> objectTypes.stream().anyMatch(t -> t.getTypeClass().isAssignableFrom(o.getClass())) && objs.add(o),
                        contextObjext,
                        filterOrNull,
                        completions
                    );
                    if (request.getContext().isSearchProcedures()
                        && objectTypes.stream().anyMatch(t -> DBSProcedure.class.isAssignableFrom(t.getTypeClass()))
                    ) {
                        this.collectProcedures(monitor, request, contexts, contextObjext, filterOrNull, completions);
                    }
                } catch (DBException e) {
                    log.error(e);
                }
                this.makeFilteredCompletionSet(filterOrNull, completions, results);
            }

            @Nullable
            private Collection<DBSObjectContainer> obtainDefaultContext(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request
            ) {
                if (dbcExecutionContext == null) {
                    return Collections.emptyList();
                }
                DBCExecutionContextDefaults<?, ?> defaults = dbcExecutionContext.getContextDefaults();
                if (defaults != null) {
                    DBSSchema defaultSchema = defaults.getDefaultSchema();
                    DBSCatalog defaultCatalog = defaults.getDefaultCatalog();
                    if (defaultCatalog == null && defaultSchema == null
                        && dbcExecutionContext.getDataSource() instanceof DBSObjectContainer container
                    ) {
                        return List.of(container);
                    } else if (defaultCatalog != null && request.getContext().isSearchGlobally()) {
                        Set<DBSObjectContainer> result = new HashSet<>();
                        findAllSchemaContainers(monitor, defaultCatalog, result);
                        return result;
                    } else if (defaultCatalog != null && defaultSchema == null) {
                        return List.of(defaultCatalog);
                    } else if (defaultSchema != null) {
                        return List.of(defaultSchema);
                    }
                } else if (dbcExecutionContext.getDataSource() instanceof DBSObjectContainer container) {
                    return List.of(container);
                }
                return Collections.emptyList();
            }

            private void findAllSchemaContainers(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull Set<DBSObjectContainer> result
            ) {
                try {
                    if (result.add(container)) {
                        Collection<? extends DBSObject> dbObjs = container.getChildren(monitor);
                        for (DBSObject obj : dbObjs) {
                            if (obj instanceof DBSObjectContainer child && (obj instanceof DBSCatalog || obj instanceof DBSSchema)) {
                                findAllSchemaContainers(monitor, child, result);
                            }
                        }
                    }
                } catch (DBException ex) {
                    log.error(ex);
                }
            }

            private void prepareContextSchemasAndCatalogs(
                @NotNull DBRProgressMonitor monitor,
                @NotNull Collection<DBSObjectContainer> contexts,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObject,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull List<SQLQueryCompletionSet> results
            ) {
                LinkedList<SQLQueryCompletionItem> completions = new LinkedList<>();
                try {
                    for (DBSObjectContainer container : contexts) {
                        Collection<? extends DBSObject> children = container.getChildren(monitor);
                        for (DBSObject child : children) {
                            if (child instanceof DBSSchema || child instanceof DBSCatalog) {
                                SQLQueryWordEntry childName = makeFilterInfo(filterOrNull, child.getName());
                                int score = childName.matches(filterOrNull, this.searchInsideWords);
                                if (score > 0) {
                                    completions.addLast(this.makeDbObjectCompletionItem(score, childName, contextObject, child));
                                }
                            }
                        }
                    }
                } catch (DBException ex) {
                    log.error(ex);
                }
                this.makeFilteredCompletionSet(filterOrNull, completions, results);
            }

            private void collectPackages(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull SQLQuerySourcesInfoCollection knownSources,
                @NotNull Collection<DBSObjectContainer> contexts,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                if (request.getContext().isSearchProcedures()) {
                    this.collectImmediateChildren(
                        monitor,
                        knownSources,
                        contexts,
                        o -> o instanceof DBSProcedureContainer,
                        contextObjext,
                        filterOrNull,
                        accumulator
                    );
                }
            }

            private void collectProcedures(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLCompletionRequest request,
                @NotNull Collection<DBSObjectContainer> containers,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                for (DBSObjectContainer container : containers) {
                    if (request.getContext().isSearchProcedures() && container instanceof DBSProcedureContainer pc
                        && request.getContext().getDataSource().getInfo().supportsStoredCode()
                    ) {
                        Collection<? extends DBSProcedure> procedures = pc.getProcedures(monitor);
                        if (procedures != null) {
                            for (DBSProcedure p : procedures) {
                                SQLQueryWordEntry childName = makeFilterInfo(filterOrNull, p.getName());
                                int score = childName.matches(filterOrNull, this.searchInsideWords);
                                if (score > 0) {
                                    accumulator.addLast(SQLQueryCompletionItem.forProcedureObject(score, childName, contextObjext, p));
                                }
                            }
                        }
                    }
                    if (filterOrNull != null && contextObjext == null) {
                        for (String fname : request.getContext().getDataSource().getSQLDialect().getFunctions()) {
                            SQLQueryWordEntry childName = makeFilterInfo(filterOrNull, fname);
                            int score = childName.matches(filterOrNull, this.searchInsideWords);
                            if (score > 0) {
                                accumulator.addLast(SQLQueryCompletionItem.forBuiltinFunction(score, childName, fname));
                            }
                        }
                    }
                }
            }

            private void collectTables(
                @NotNull DBRProgressMonitor monitor,
                @NotNull SQLQuerySourcesInfoCollection knownSources,
                @NotNull Collection<DBSObjectContainer> containers,
                @Nullable SQLQueryCompletionItem.ContextObjectInfo contextObjext,
                @Nullable SQLQueryWordEntry filterOrNull,
                @NotNull LinkedList<SQLQueryCompletionItem> accumulator
            ) throws DBException {
                this.collectImmediateChildren(
                    monitor, knownSources, containers,
                    o -> o instanceof DBSTable || o instanceof DBSView,
                    contextObjext, filterOrNull, accumulator
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

    protected void makeFilteredCompletionSet(
        @Nullable SQLQueryWordEntry filterOrNull,
        List<? extends SQLQueryCompletionItem> items,
        @NotNull List<SQLQueryCompletionSet> results
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

    /**
     * Gather and prepare the information of the completion request
     */
    @NotNull
    public static SQLQueryCompletionContext prepareCompletionContext(
        @NotNull SQLScriptItemAtOffset scriptItem,
        int offset,
        @Nullable DBCExecutionContext executionContext,
        @NotNull SQLDialect dialect
    ) {
        int position = offset - scriptItem.offset;

        SQLQueryModel model = scriptItem.item.getQueryModel();
        if (model != null) {
            if (scriptItem.item.hasContextBoundaryAtLength() && position >= scriptItem.item.length()) {
                return SQLQueryCompletionContext.prepareOffquery(scriptItem.offset, offset);
            } else {
                STMTreeNode syntaxNode = model.getSyntaxNode();
                Interval parsedInterval = syntaxNode.getRealInterval();
                if (parsedInterval.a < 0 || parsedInterval.b < 0) {
                    return SQLQueryCompletionContext.prepareEmpty(scriptItem.offset, offset);
                } else if (scriptItem.item.getOriginalText().length() <= SQLQueryCompletionContext.getMaxKeywordLength()
                    && LSMInspections.matchesAnyWord(scriptItem.item.getOriginalText())
                    && position <= scriptItem.item.getOriginalText().length()
                ) {
                    return SQLQueryCompletionContext.prepareOffquery(scriptItem.offset, offset);
                }

                LSMInspections inspections = new LSMInspections(dialect, syntaxNode);
                LSMInspections.SyntaxInspectionResult syntaxInspectionResult = inspections.prepareAbstractSyntaxInspection(position);
                if (syntaxInspectionResult == null) {
                    return SQLQueryCompletionContext.prepareOffquery(scriptItem.offset, offset);
                }

                SQLQueryModel.LexicalContextResolutionResult context = model.findLexicalContext(
                    Math.min(position, model.getSyntaxNode().getRealInterval().b + 1)
                );

                LSMInspections.NameInspectionResult nameInspectionResult = inspections.collectNameNodes(position);
                if (nameInspectionResult.positionToInspect() != position) {
                    syntaxInspectionResult = inspections.prepareAbstractSyntaxInspection(nameInspectionResult.positionToInspect());
                }
                ArrayDeque<STMTreeNode> nameNodes = nameInspectionResult.nameNodes();

                SQLQueryLexicalScopeItem lexicalItem = context.lexicalItem();
                // if (nameNodes.isEmpty()
                //     || (lexicalItem != null&& nameNodes.getLast().getRealInterval().b != lexicalItem.getSyntaxNode().getRealInterval().b)
                // ) {
                // no name nodes OR
                if ((lexicalItem instanceof SQLQuerySymbolEntry && (
                        nameNodes.isEmpty() || (
                            nameNodes.getFirst().getRealInterval().a > lexicalItem.getSyntaxNode().getRealInterval().a ||
                            nameNodes.getLast().getRealInterval().b < lexicalItem.getSyntaxNode().getRealInterval().b
                        )
                    )) || (lexicalItem instanceof SQLQueryTupleRefEntry e &&  e.getSyntaxNode().getRealInterval().b + 1 != position)
                ) {
                    // lexicalItem is identifier (not an isolated Period character) outside nameNodes (actually, WTF?!)
                    lexicalItem = null;
                }
                return SQLQueryCompletionContext.prepare(
                    scriptItem,
                    offset,
                    executionContext,
                    syntaxInspectionResult,
                    context,
                    lexicalItem,
                    nameNodes.toArray(STMTreeNode[]::new),
                    nameInspectionResult.hasPeriod(),
                    nameInspectionResult.currentTerm()
                );
            }
        } else {
            return SQLQueryCompletionContext.prepareEmpty(0, offset);
        }
    }

    public interface SQLQueryDataContextInfo {

        @NotNull
        SQLQuerySourcesInfoCollection getKnownSources();

        @NotNull
        List<SQLQueryResultColumn> getColumnsList();

        @Nullable
        SourceResolutionResult resolveSource(DBRProgressMonitor monitor, List<String> s);

        boolean isJoin();

        @NotNull
        List<? extends SQLQueryResultColumn> getRightParentColumnsList();

        static SQLQueryDataContextInfo empty() {
            return EMPTY_DATA_CONTEXT_INFO;
        }

        static SQLQueryDataContextInfo makeFor(@NotNull SQLQueryRowsSourceContext rowsSourceContext) {
            return new SQLQueryRowsSourceContextInfo(rowsSourceContext);
        }

        static SQLQueryDataContextInfo makeFor(@NotNull SQLQueryRowsDataContext rowsDataContext) {
            return new SQLQueryRowsDataContextInfo(rowsDataContext);
        }
    }

    private static class SQLQueryRowsSourceContextInfo implements SQLQueryDataContextInfo {
        @NotNull
        private final SQLQueryRowsSourceContext rowsSourceContext;
        @NotNull
        private final SQLQuerySourcesInfoCollection subquerySources;

        public SQLQueryRowsSourceContextInfo(@NotNull SQLQueryRowsSourceContext rowsSourceContext) {
            this.rowsSourceContext = rowsSourceContext;
            this.subquerySources = rowsSourceContext.getKnownSources(true);
        }

        @NotNull
        @Override
        public SQLQuerySourcesInfoCollection getKnownSources() {
            return this.subquerySources;
        }

        @NotNull
        @Override
        public List<SQLQueryResultColumn> getColumnsList() {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public SourceResolutionResult resolveSource(DBRProgressMonitor monitor, List<String> tableName) {
            List<DBSEntity> tables = rowsSourceContext.getConnectionInfo().findRealTables(monitor, tableName);
            return this.subquerySources.getResolutionResults().values()
                .stream().filter(r -> tables.contains(r.tableOrNull)).findFirst().orElse(null);
        }

        @Override
        public boolean isJoin() {
            return false;
        }

        @NotNull
        @Override
        public List<? extends SQLQueryResultColumn> getRightParentColumnsList() {
            return Collections.emptyList();
        }
    }

    private static class SQLQueryRowsDataContextInfo extends SQLQueryRowsSourceContextInfo {
        @NotNull
        private final SQLQueryRowsDataContext rowsDataContext;

        public SQLQueryRowsDataContextInfo(@NotNull SQLQueryRowsDataContext rowsDataContext) {
            super(rowsDataContext.getRowsSources());
            this.rowsDataContext = rowsDataContext;
        }

        @NotNull
        @Override
        public List<SQLQueryResultColumn> getColumnsList() {
            return this.rowsDataContext.getColumnsList();
        }

        @Override
        public boolean isJoin() {
            return this.rowsDataContext.getJoinInfo() != null;
        }

        @NotNull
        @Override
        public List<? extends SQLQueryResultColumn> getRightParentColumnsList() {
            return this.isJoin() ? this.rowsDataContext.getJoinInfo().right().getColumnsList() : Collections.emptyList();
        }
    }

    private static final SQLQueryDataContextInfo EMPTY_DATA_CONTEXT_INFO = new SQLQueryDataContextInfo() {
        static final SQLQuerySourcesInfoCollection EMPTY_SOURCES_COLLECTION = new SQLQuerySourcesInfoCollection() {
            @NotNull
            @Override
            public Map<SQLQueryRowsSourceModel, SourceResolutionResult> getResolutionResults() {
                return Collections.emptyMap();
            }

            @NotNull
            @Override
            public Set<DBSObject> getReferencedTables() {
                return Collections.emptySet();
            }

            @NotNull
            @Override
            public Set<String> getAliasesInUse() {
                return Collections.emptySet();
            }
        };

        @NotNull
        @Override
        public SQLQuerySourcesInfoCollection getKnownSources() {
            return EMPTY_SOURCES_COLLECTION;
        }

        @NotNull
        @Override
        public List<SQLQueryResultColumn> getColumnsList() {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public SourceResolutionResult resolveSource(DBRProgressMonitor monitor, List<String> s) {
            return null;
        }

        @Override
        public boolean isJoin() {
            return false;
        }

        @NotNull
        @Override
        public List<? extends SQLQueryResultColumn> getRightParentColumnsList() {
            return Collections.emptyList();
        }
    };
}
