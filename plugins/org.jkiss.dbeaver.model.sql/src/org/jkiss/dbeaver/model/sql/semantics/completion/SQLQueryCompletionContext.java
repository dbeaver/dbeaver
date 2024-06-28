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
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SQLQueryCompletionContext {

    private static final Log log = Log.getLog(SQLQueryCompletionContext.class);

    /**
     * Empty completion context which always provides no completion items
     */
    public static final SQLQueryCompletionContext EMPTY = new SQLQueryCompletionContext(0) {
        @NotNull
        @Override
        public SQLQueryCompletionSet prepareProposal(@NotNull DBRProgressMonitor monitor, int position) {
            return new SQLQueryCompletionSet(position, 0, Collections.emptyList());
        }
    };

    /**
     * Prepare completion context for the script item at given offset treating current position as outside-of-query
     */
    @NotNull
    public static SQLQueryCompletionContext prepareOffquery(int scriptItemOffset) {
        return new SQLQueryCompletionContext(scriptItemOffset) {
            private static final Collection<SQLQueryCompletionItem> keywords = LSMInspections.prepareOffquerySyntaxInspection()
                .predictedWords.stream().sorted().map(SQLQueryCompletionItem::forReservedWord).collect(Collectors.toList());
            
            @NotNull
            @Override
            public SQLQueryCompletionSet prepareProposal(@NotNull DBRProgressMonitor monitor, int position) {
                return new SQLQueryCompletionSet(position, 0, keywords);
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

    /**
     * Prepare a set of completion proposal items for a given position in the text of the script item
     */
    @NotNull
    public abstract SQLQueryCompletionSet prepareProposal(@NotNull DBRProgressMonitor monitor, int position);

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
            final Map<SQLQueryRowsSourceModel, SourceResolutionResult> referencedSources = context.collectKnownSources().getResolutionResults();

            @NotNull
            @Override
            public SQLQueryCompletionSet prepareProposal(@NotNull DBRProgressMonitor monitor, int position) {
                position -= this.getOffset();
                
                String currentWord = this.obtainCurrentWord(position);
                
                final List<SQLQueryCompletionItem> keywordCompletions = 
                        nameNodes.length > 1
                            ? Collections.emptyList()
                            : prepareKeywordCompletions(syntaxInspectionResult.predictedWords, currentWord);
                
                List<SQLQueryCompletionItem> columnRefCompletions = 
                        syntaxInspectionResult.expectingColumnReference ? this.prepareColumnCompletions() : Collections.emptyList();
                
                List<SQLQueryCompletionItem> tableRefCompletions = 
                        syntaxInspectionResult.expectingTableReference ? this.prepareTableCompletions(monitor) : Collections.emptyList();
                
                List<SQLQueryCompletionItem> lexicalItemCompletions = 
                        lexicalItem != null ? this.prepareLexicalItemCompletions(monitor, lexicalItem, position) :
                        syntaxInspectionResult.expectingIdentifier || nameNodes.length > 0
                            ? this.prepareIdentifierCompletions(monitor, position)
                            : Collections.emptyList();
                
                List<SQLQueryCompletionItem> completionItems = Stream.of(
                        keywordCompletions, columnRefCompletions, tableRefCompletions, lexicalItemCompletions
                ).flatMap(Collection::stream).sorted(Comparator.comparing(SQLQueryCompletionItem::getText)).collect(Collectors.toList());
                
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

                // TODO deeper syntax inspection needed to properly decide what to look for here

                
                if (prefix.size() == 1) {
                    String mayBeAliasName = prefix.get(0).toLowerCase();
                    SourceResolutionResult srr = this.referencedSources.values().stream()
                        .filter(rr -> rr.aliasOrNull != null && rr.aliasOrNull.getName().toLowerCase().equals(mayBeAliasName))
                        .findFirst().orElse(null);
                    if (srr != null) {
                        return srr.source.getResultDataContext().getColumnsList().stream()
                            .filter(c -> c.symbol.getName().toLowerCase().contains(tail))
                            .map(c -> SQLQueryCompletionItem.forSubsetColumn(c, srr, false))
                            .toList();
                    }
                } else if (prefix.size() == 0) {
                    List<SQLQueryCompletionItem> subqueries = this.referencedSources.values().stream()
                        .filter(rr -> rr.aliasOrNull != null && rr.aliasOrNull.getName().toLowerCase().contains(tail))
                        .map(rr -> SQLQueryCompletionItem.forSubqueryAlias(rr.aliasOrNull))
                        .toList();
                    if (!subqueries.isEmpty()) {
                        return subqueries;
                    }
                }
                
                if (dbcExecutionContext == null || dbcExecutionContext.getDataSource() == null) {
                    return Collections.emptyList();
                } else {
                    DBSObject prefixContext = prefix.size() == 0 ? dbcExecutionContext.getDataSource() : SQLSearchUtils.findObjectByFQN(
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
            private List<SQLQueryCompletionItem> prepareObjectComponentCompletions(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObject object,
                @NotNull String componentNamePart, Class<?> componentType
            ) {
                try {
                    Stream<? extends DBPNamedObject> components;
                    if (object instanceof DBSEntity entity) {
                        List<? extends DBSEntityAttribute> attrs = entity.getAttributes(monitor);
                        components = attrs == null ? Stream.empty() : attrs.stream();
                    } else if (object instanceof DBSObjectContainer container) {
                        components = container.getChildren(monitor).stream()
                            .filter(c -> c instanceof DBPNamedObject)
                            .map(c -> (DBPNamedObject) c);
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
                            parts.add(term.getTextContent());
                        } else {
                            break;
                        }
                    }
                }
                STMTreeTermNode currentNode = i >= nameNodes.length ? null : nameNodes[i];
                String currentPart = currentNode == null
                    ? ""
                    : currentNode.getTextContent().substring(0, position - currentNode.getRealInterval().a);
                parts.add(currentPart);
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
            private List<SQLQueryCompletionItem> prepareColumnCompletions() {
                // directly available column
                List<SQLQueryCompletionItem> subsetColumns = context.getColumnsList().stream().map(
                    rc -> SQLQueryCompletionItem.forSubsetColumn(rc, this.referencedSources.get(rc.source), true)
                ).toList();
                // already referenced tables
                LinkedList<SQLQueryCompletionItem> tableRefs = new LinkedList<>();
                for (SourceResolutionResult rr : this.referencedSources.values()) {
                    if (rr.aliasOrNull != null && !rr.isCteSubquery) {
                        tableRefs.add(SQLQueryCompletionItem.forSubqueryAlias(rr.aliasOrNull));
                    } else if (rr.tableOrNull != null) {
                        tableRefs.add(SQLQueryCompletionItem.forRealTable(rr.tableOrNull, true));
                    }
                }
                return Stream.of(subsetColumns, tableRefs).flatMap(Collection::stream).toList();
            }

            @NotNull
            private List<SQLQueryCompletionItem> prepareTableCompletions(@NotNull DBRProgressMonitor monitor) {
                Set<DBSEntity> alreadyReferencedTables = new HashSet<>();
                
                LinkedList<SQLQueryCompletionItem> tableRefs = new LinkedList<>();
                for (SourceResolutionResult rr : this.referencedSources.values()) {
                    if (rr.aliasOrNull != null && rr.isCteSubquery) {
                        tableRefs.add(SQLQueryCompletionItem.forSubqueryAlias(rr.aliasOrNull));
                    }
                    if (rr.tableOrNull != null) {
                        alreadyReferencedTables.add(rr.tableOrNull);
                    }
                }

                if (dbcExecutionContext != null && dbcExecutionContext.getDataSource() != null) {
                    try {
                        if (dbcExecutionContext.getDataSource().getDefaultInstance() instanceof DBSObjectContainer container) {
                            collectTables(monitor, container, alreadyReferencedTables, tableRefs);    
                        } else if (dbcExecutionContext.getDataSource() instanceof DBSObjectContainer container2) {
                            collectTables(monitor, container2, alreadyReferencedTables, tableRefs);
                        }
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                
                return tableRefs;
            }
            

            private void collectTables(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBSObjectContainer container,
                @NotNull Set<DBSEntity> alreadyReferencedTables,
                @NotNull LinkedList<SQLQueryCompletionItem> tableRefs
            ) throws DBException {
                Collection<? extends DBSObject> children = container.getChildren(monitor);
                for (DBSObject child : children) {
                    if (!DBUtils.isHiddenObject(child)) {
                        if (child instanceof DBSTable tab && !alreadyReferencedTables.contains(tab)) {
                            tableRefs.add(SQLQueryCompletionItem.forRealTable(tab, false));
                        } else if (child instanceof DBSView view && !alreadyReferencedTables.contains(view)) {
                            tableRefs.add(SQLQueryCompletionItem.forRealTable(view, false));
                        } else if (child instanceof DBSObjectContainer sc) {
                            collectTables(monitor, sc, alreadyReferencedTables, tableRefs);
                        }
                    }
                }
            }
        };
    }
}
