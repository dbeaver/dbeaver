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
package org.jkiss.dbeaver.model.sql.semantics;


import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.dialect.LSMDialectRegistry;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryMemberAccessEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryTupleRefEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryObjectDropModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableAlterModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableDropModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryCallModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryDeleteModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryInsertModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryUpdateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.*;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.*;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Responsible for semantics model preparation based on the parsing result
 */
public class SQLQueryModelRecognizer {

    private static final Log log = Log.getLog(SQLQueryModelRecognizer.class);

    private final Set<SQLQuerySymbolEntry> symbolEntries = new HashSet<>();
    private final TreeMap<Integer, SQLQueryLexicalScopeItem> lexicalItems = new TreeMap<>();
    
    private final SQLQueryRecognitionContext recognitionContext;

    private final DBCExecutionContext executionContext;
    
    private final Set<String> reservedWords;

    private final SQLDialect dialect;
    
    private final LinkedList<SQLQueryLexicalScope> currentLexicalScopes = new LinkedList<>();

    private SQLQueryModelRecognizer(@NotNull SQLQueryRecognitionContext recognitionContext) {
        this.recognitionContext = recognitionContext;

        this.executionContext = recognitionContext.getExecutionContext();
        this.dialect = recognitionContext.getDialect();
        this.reservedWords = new HashSet<>(this.dialect.getReservedWords());
    }

    /**
     * Provides the semantic model for the provided text
     */
    @Nullable
    private SQLQueryModel recognizeQuery(@NotNull String text) {
        STMSource querySource = STMSource.fromString(text);
        LSMAnalyzer analyzer = LSMDialectRegistry.getInstance().getAnalyzerFactoryForDialect(this.dialect)
            .createAnalyzer(LSMAnalyzerParameters.forDialect(this.dialect, this.recognitionContext.getSyntaxManager()));
        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(querySource, new STMSkippingErrorListener());

        if (tree == null || (tree.start == tree.stop && !LSMInspections.prepareOffquerySyntaxInspection().predictedTokenIds().contains(tree.start.getType()))) {
            return tree == null ? null : new SQLQueryModel(tree, null, Collections.emptySet(), Collections.emptyList());
        }
        SQLQueryConnectionContext connectionContext = this.prepareConnectionContext(tree);
        SQLQueryRowsSourceContext rootRowsContext = new SQLQueryRowsSourceContext(connectionContext);
        STMTreeNode queryNode = tree.findFirstNonErrorChild();
        if (queryNode == null) {
            return new SQLQueryModel(tree, null, Collections.emptySet(), Collections.emptyList());
        }
        SQLQueryModelContent contents = switch (queryNode.getNodeKindId()) {
            case SQLStandardParser.RULE_directSqlDataStatement -> {
                STMTreeNode stmtBodyNode = queryNode.findLastNonErrorChild();
                // TODO collect CTE for insert-update-delete as well as recursive CTE
                yield stmtBodyNode == null ? null : switch (stmtBodyNode.getNodeKindId()) {
                    case SQLStandardParser.RULE_deleteStatement ->
                        SQLQueryDeleteModel.recognize(this, stmtBodyNode);
                    case SQLStandardParser.RULE_insertStatement ->
                        SQLQueryInsertModel.recognize(this, stmtBodyNode);
                    case SQLStandardParser.RULE_updateStatement ->
                        SQLQueryUpdateModel.recognize(this, stmtBodyNode);
                    default -> this.collectQueryExpression(tree);
                };
            }
            case SQLStandardParser.RULE_sqlSchemaStatement -> {
                STMTreeNode stmtBodyNode = queryNode.findFirstNonErrorChild();
                yield stmtBodyNode == null ? null : switch (stmtBodyNode.getNodeKindId()) {
                    case SQLStandardParser.RULE_createTableStatement ->
                        SQLQueryTableCreateModel.recognize(this, stmtBodyNode);
                    case SQLStandardParser.RULE_createViewStatement -> null;
                    case SQLStandardParser.RULE_dropTableStatement ->
                        SQLQueryTableDropModel.recognize(this, stmtBodyNode, false);
                    case SQLStandardParser.RULE_dropViewStatement ->
                        SQLQueryTableDropModel.recognize(this, stmtBodyNode, true);
                    case SQLStandardParser.RULE_dropProcedureStatement ->
                        SQLQueryObjectDropModel.recognize(this, stmtBodyNode, RelationalObjectType.TYPE_PROCEDURE, Collections.emptySet());
                    case SQLStandardParser.RULE_alterTableStatement ->
                        SQLQueryTableAlterModel.recognize(this, stmtBodyNode);
                    default -> null;
                };
            }
            case SQLStandardParser.RULE_selectStatementSingleRow -> {
                STMTreeNode stmtBodyNode = queryNode.findFirstNonErrorChild();
                yield stmtBodyNode == null ? null : this.collectQueryExpression(tree);
            }
            case SQLStandardParser.RULE_callStatement -> SQLQueryCallModel.recognize(this, queryNode, RelationalObjectType.TYPE_PROCEDURE);
            default -> null;
        };

        if (contents != null) {
            SQLQueryModel model = new SQLQueryModel(tree, contents, this.symbolEntries, this.lexicalItems.values().stream().toList());
            model.resolveRelations(rootRowsContext, this.recognitionContext);

            for (SQLQuerySymbolEntry symbolEntry : this.symbolEntries) {
                if (symbolEntry.isNotClassified() && this.reservedWords.contains(symbolEntry.getRawName().toUpperCase())) {
                    // (keywords are uppercased in dialect)
                    // if non-reserved by parser keyword was not classified as identifier, then highlight it as reserved by dialect
                    symbolEntry.getSymbol().setSymbolClass(SQLQuerySymbolClass.RESERVED);
                }
            }

            return model;
        }

        // TODO log query model collection error
        Predicate<SQLQuerySymbolEntry> tryFallbackForStringLiteral = s -> {
            String rawString = s.getRawName();
            SQLQuerySymbolClass forcedClass;
            if (this.dialect.isQuotedString(rawString)) {
                forcedClass = SQLQuerySymbolClass.STRING;
            } else {
                forcedClass = tryFallbackSymbolForStringLiteral(this.dialect, s, false);
            }
            boolean forced = forcedClass != null;
            if (forced) {
                s.getSymbol().setSymbolClass(forcedClass);
            }
            return forced;
        };


        classifySymbolsWithoutModel(connectionContext, tree, tryFallbackForStringLiteral, rootRowsContext);
        return new SQLQueryModel(tree, null, this.symbolEntries, this.lexicalItems.values().stream().toList());
    }

    /**
     *
     * Fallback when no model:
     *  1. collect and classify all name identifier (FROMs), classify them as [cat.][<sch.>...]<tab>
     *  2. classify all their entries as complex-name prefixes
     *  3. classify suffixes of these complex-names as columns
     *  4. classify tails of these complex-names as complex-type members
     */
    private void classifySymbolsWithoutModel(
        @NotNull SQLQueryConnectionContext connectionContext,
        @NotNull STMTreeRuleNode tree,
        @NotNull Predicate<SQLQuerySymbolEntry> tryFallbackForStringLiteral,
        @NotNull SQLQueryRowsSourceContext rootRowsContext
    ) {
        var objectNameOrigin = new SQLQuerySymbolOrigin.DbObjectRef(
            new SQLQueryRowsSourceContext(connectionContext), Set.of(RelationalObjectType.TYPE_UNKNOWN), true
        );
        Map<SQLQueryComplexName, SQLQueryComplexName> allTableNames = new HashMap<>();
        Set<String> allTableAliases = new HashSet<>();
        LinkedList<SQLQuerySymbolEntry> allMaybeColumns = new LinkedList<>();
        LinkedList<SQLQueryComplexName> allValueRefs = new LinkedList<>();

        this.traverseForIdentifiers(
            tree,
            allMaybeColumns::add,
            entityName -> {
                if (entityName.isNotClassified() || !tryFallbackForStringLiteral.test(entityName.parts.getLast())) {
                    if (!this.recognitionContext.useRealMetadata() || connectionContext.isDummy()) {
                        for (SQLQuerySymbolEntry part : entityName.parts) {
                            if (part != null) {
                                part.getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE);
                            }
                        }
                    } else {
                        SQLQuerySemanticUtils.performPartialResolution(
                            rootRowsContext,
                            this.recognitionContext,
                            entityName,
                            objectNameOrigin,
                            SQLQuerySymbolOrigin.DbObjectFilterMode.DEFAULT,
                            SQLQuerySymbolClass.OBJECT
                        );
                    }
                }
                allTableNames.put(entityName, entityName);
            },
            allValueRefs::addLast,
            aliasEntry -> {
                aliasEntry.getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE_ALIAS);
                allTableAliases.add(aliasEntry.getName().toLowerCase());
            },
            true
        );
        for (SQLQueryComplexName valueRef : allValueRefs) {
            SQLQueryComplexName prefix = valueRef;
            while (prefix != null && !prefix.parts.isEmpty()) {
                SQLQueryComplexName table = allTableNames.get(prefix);
                if (table != null) {
                    for (int i = 0; i < prefix.parts.size(); i++) {
                        SQLQuerySymbolEntry a = prefix.parts.get(i);
                        SQLQuerySymbolEntry b = table.parts.get(i);
                        if (a != null && b != null) {
                            a.setDefinition(b.getDefinition());
                            a.setOrigin(b.getOrigin());
                            if (a.isNotClassified()) {
                                a.getSymbol().setSymbolClass(b.getSymbolClass());
                            }
                        }
                    }
                    break;
                }
                prefix = prefix.trimEnd();
            }
            List<SQLQuerySymbolEntry> tail;
            if (prefix == null && valueRef.parts.size() > 1 && valueRef.parts.getFirst() != null && allTableAliases.contains(valueRef.parts.getFirst().getName().toLowerCase())) {
                valueRef.parts.getFirst().getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE_ALIAS);
                tail = valueRef.parts.subList(1, valueRef.parts.size());
            } else {
                tail = valueRef.parts.subList(prefix == null ? 0 : prefix.parts.size(), valueRef.parts.size());
            }
            for (SQLQuerySymbolEntry column : tail) {
                if (column != null) {
                    column.getSymbol().setSymbolClass(SQLQuerySymbolClass.COLUMN);
                }
            }
        }
        for (SQLQuerySymbolEntry maybeColumn : allMaybeColumns) {
            if (maybeColumn.isNotClassified() && !tryFallbackForStringLiteral.test(maybeColumn)) {
                maybeColumn.getSymbol().setSymbolClass(SQLQuerySymbolClass.COLUMN);
            }
        }
    }

    private void traverseForIdentifiers(
        @NotNull STMTreeNode root,
        @NotNull Consumer<SQLQuerySymbolEntry> columnAction,
        @NotNull Consumer<SQLQueryComplexName> entityAction,
        @NotNull Consumer<SQLQueryComplexName> valueRefAction,
        @NotNull Consumer<SQLQuerySymbolEntry> entityAliasAction,
        boolean forceUnquotted
    ) {
        List<STMTreeNode> refs = STMUtils.expandSubtree(
            root,
            null,
            Set.of(STMKnownRuleNames.columnReference, STMKnownRuleNames.columnName, STMKnownRuleNames.tableName, STMKnownRuleNames.correlationName)
        );
        for (STMTreeNode ref : refs) {
            switch (ref.getNodeKindId()) {
                case SQLStandardParser.RULE_columnName -> {
                    SQLQuerySymbolEntry columnName = this.collectIdentifier(ref, forceUnquotted, null);
                    if (columnName != null) {
                        columnAction.accept(columnName);
                    }
                }
                case SQLStandardParser.RULE_columnReference -> {
                    SQLQueryValueExpression expr = this.collectColumnReferenceExpression(ref, false);
                    if (expr instanceof SQLQueryValueTupleReferenceExpression tupleRef) {
                        entityAction.accept(tupleRef.getTableName());
                    } else if (expr instanceof SQLQueryValueColumnReferenceExpression columnRef) {
                        columnAction.accept(columnRef.getColumnName());
                    } else if (expr instanceof SQLQueryValueReferenceExpression valueRef && valueRef.getName() != null) {
                        for (SQLQuerySymbolEntry c : valueRef.getName().parts) {
                            if (c != null) {
                                columnAction.accept(c);
                            }
                        }
                        valueRefAction.accept(valueRef.getName());
                    }
                }
                case SQLStandardParser.RULE_tableName -> {
                    SQLQueryComplexName tableName = this.collectTableName(ref, forceUnquotted);
                    if (tableName != null) {
                        entityAction.accept(tableName);
                    }
                }
                case SQLStandardParser.RULE_correlationName -> {
                    SQLQuerySymbolEntry entityAlias = this.collectIdentifier(ref, forceUnquotted, null);
                    if (entityAlias != null) {
                        entityAliasAction.accept(entityAlias);
                    }
                }
                default -> throw new IllegalArgumentException("Unexpected value: " + ref.getNodeName());
            }
        }
    }

    @NotNull
    private SQLQueryConnectionContext prepareConnectionContext(@NotNull STMTreeNode root) {
        if (this.recognitionContext.useRealMetadata()
            && this.executionContext != null
            && this.executionContext.getDataSource() instanceof DBSObjectContainer
            && this.executionContext.getDataSource().getSQLDialect() instanceof BasicSQLDialect basicSQLDialect
        ) {
            Map<String, SQLQueryResultPseudoColumn> globalPseudoColumns = Stream.of(basicSQLDialect.getGlobalVariables())
                .map(v -> new SQLQueryResultPseudoColumn(
                    new SQLQuerySymbol(SQLUtils.identifierToCanonicalForm(basicSQLDialect, v.name(), false, false)),
                    null, null, SQLQueryExprType.forPredefined(v.type()),
                    DBDPseudoAttribute.PropagationPolicy.GLOBAL_VARIABLE, v.description()
                )).collect(Collectors.toMap(c -> c.symbol.getName(), c -> c));;

            Function<SQLQueryRowsSourceModel, List<SQLQueryResultPseudoColumn>> rowsetPseudoColumns;
            if (this.executionContext.getDataSource() instanceof DBDPseudoAttributeContainer pac) {
                try {
                    DBDPseudoAttribute[] pc = pac.getAllPseudoAttributes(this.recognitionContext.getMonitor());
                    List<DBDPseudoAttribute> rowsetsPc = Stream.of(pc).filter(a -> a.getPropagationPolicy().providedByRowset).toList();
                    rowsetPseudoColumns = rowsetsPc.isEmpty()
                        ? s -> Collections.emptyList()
                        : s -> SQLQuerySemanticUtils.prepareResultPseudoColumnsList(this.dialect, s, null, rowsetsPc.stream());
                } catch (DBException e) {
                    this.recognitionContext.appendError(root, "Failed to obtain global pseudo-columns information", e);
                    rowsetPseudoColumns = s -> Collections.emptyList();
                }
            } else {
                rowsetPseudoColumns = s -> Collections.emptyList();
            }
            return new SQLQueryConnectionRealContext(
                this.dialect,
                new SQLIdentifierDetector(this.dialect),
                this.executionContext,
                this.recognitionContext.validateFunctions(),
                globalPseudoColumns,
                rowsetPseudoColumns
            );
        } else { // Don't have active connection, use dummy context
            Set<String> allColumnNames = new HashSet<>();
            Set<List<String>> allTableNames = new HashSet<>();
            this.traverseForIdentifiers(
                root,
                (columnName) -> allColumnNames.add(columnName.getName()),
                entityName -> allTableNames.add(entityName.stringParts),
                valueRef -> { },
                columnAlias -> { },
                true
            );
            symbolEntries.clear();
            return new SQLQueryConnectionDummyContext(this.dialect, allColumnNames, allTableNames);
        }
    }

    @NotNull
    public SQLQueryRowsSourceModel collectQueryExpression(@NotNull STMTreeNode tree) {
        // expression mapper is a stateful thing, so it cannot be reused for multiple subtrees and should be local only
        // its configuration is already static internally and shared between all instances avoiding repeated initialization
        SQLQueryExpressionMapper queryExpressionMapper = new SQLQueryExpressionMapper(this);
        return queryExpressionMapper.translate(tree);
    }

    @NotNull
    private static final Set<String> columnNameListWrapperNames = Set.of(
        STMKnownRuleNames.correspondingSpec,
        STMKnownRuleNames.referencedTableAndColumns,
        STMKnownRuleNames.correlationSpecification,
        STMKnownRuleNames.nonjoinedTableReference,
        STMKnownRuleNames.namedColumnsJoin,
        STMKnownRuleNames.joinSpecification,
        STMKnownRuleNames.naturalJoinTerm,
        STMKnownRuleNames.unionTerm,
        STMKnownRuleNames.exceptTerm,
        STMKnownRuleNames.intersectTerm,
        STMKnownRuleNames.uniqueConstraintDefinition,
        STMKnownRuleNames.createViewStatement,
        STMKnownRuleNames.insertColumnsAndSource,

        STMKnownRuleNames.referenceColumnList,
        STMKnownRuleNames.referencingColumns,
        STMKnownRuleNames.derivedColumnList,
        STMKnownRuleNames.joinColumnList,
        STMKnownRuleNames.correspondingColumnList,
        STMKnownRuleNames.uniqueColumnList,
        STMKnownRuleNames.viewColumnList,
        STMKnownRuleNames.insertColumnList
    );

    @NotNull
    public List<SQLQuerySymbolEntry> collectColumnNameList(@NotNull STMTreeNode node) {
        if (!node.getNodeName().equals(STMKnownRuleNames.columnNameList)) {
            if (!columnNameListWrapperNames.contains(node.getNodeName())) {
                log.debug("columnNameList (or its wrapper) expected while facing with " + node.getNodeName());
                return Collections.emptyList();
            }
            
            List<STMTreeNode> actual = STMUtils.expandSubtree(node, columnNameListWrapperNames, Set.of(STMKnownRuleNames.columnNameList));
            switch (actual.size()) {
                case 0 -> {
                    return Collections.emptyList();
                }
                case 1 -> {
                    node = actual.get(0);
                }
                default -> {
                    log.debug("Ambiguous columnNameList collection at " + node.getNodeName());
                    return Collections.emptyList();
                }
            }
        }

        List<SQLQuerySymbolEntry> result = node.findChildrenOfName(STMKnownRuleNames.columnName).stream()
            .map(n -> this.collectIdentifier(n, null)).toList();
        return result;
    }

    private static final Set<String> identifierDirectWrapperNames = Set.of(
        STMKnownRuleNames.correlationName,
        STMKnownRuleNames.authorizationIdentifier,
        STMKnownRuleNames.columnName,
        STMKnownRuleNames.queryName
    );
    
    @Nullable
    public SQLQuerySymbolEntry collectIdentifier(@NotNull STMTreeNode node, @Nullable STMTreeNode periodNode) {
        return this.collectIdentifier(node, false, periodNode);
    }
    
    @Nullable
    private SQLQuerySymbolEntry collectIdentifier(@NotNull STMTreeNode node, boolean forceUnquotted, @Nullable STMTreeNode periodNode) {
        STMTreeNode identifierNode = identifierDirectWrapperNames.contains(node.getNodeName())
            ? node.findFirstChildOfName(STMKnownRuleNames.identifier)
            : node;
        if (identifierNode == null) {
            return null;
        } else if (!identifierNode.getNodeName().equals(STMKnownRuleNames.identifier)) {
            log.debug("identifier expected while facing with " + identifierNode.getNodeName());
            return null;
        }

        STMTreeNode actualIdentifierNode = identifierNode.findLastChildOfName(STMKnownRuleNames.actualIdentifier);
        if (actualIdentifierNode == null) {
            return null;
        }

        STMTreeNode identifierTextNode = actualIdentifierNode.findFirstNonErrorChild();
        if (identifierTextNode == null) {
            return null;
        }

        SQLQueryMemberAccessEntry memberAccessEntry = periodNode == null ? null : this.registerScopeItem(new SQLQueryMemberAccessEntry(periodNode));

        String rawIdentifierString = identifierTextNode.getTextContent();
        if (identifierTextNode.getPayload() instanceof Token t && t.getType() == SQLStandardLexer.Quotted) {
            SQLQuerySymbolEntry entry = this.registerSymbolEntry(identifierTextNode, rawIdentifierString, rawIdentifierString, memberAccessEntry);
            // not canonicalizing the identifier because it is quoted,
            // but the QUOTED class will be assigned later after db entity resolution fail
            // entry.getSymbol().setSymbolClass(SQLQuerySymbolClass.QUOTED);
            return entry;
        } else {
            String actualIdentifierString = SQLUtils.identifierToCanonicalForm(dialect, rawIdentifierString, forceUnquotted, false);
            return this.registerSymbolEntry(identifierTextNode, actualIdentifierString, rawIdentifierString, memberAccessEntry);
        }
    }

    @NotNull
    private SQLQuerySymbolEntry registerSymbolEntry(
        @NotNull STMTreeNode syntaxNode,
        @NotNull String name,
        @NotNull String rawName,
        @Nullable SQLQueryMemberAccessEntry memberAccessEntry
    ) {
        SQLQuerySymbolEntry entry = new SQLQuerySymbolEntry(syntaxNode, name, rawName, memberAccessEntry);
        this.symbolEntries.add(entry);
        this.registerScopeItem(entry);
        return entry;
    }

    private static final Set<String> tableNameContainers = Set.of(
        STMKnownRuleNames.selectTargetItem,
        STMKnownRuleNames.referencedTableAndColumns,
        STMKnownRuleNames.nonjoinedTableReference,
        STMKnownRuleNames.explicitTable,
        STMKnownRuleNames.createTableStatement,
        STMKnownRuleNames.createViewStatement,
        STMKnownRuleNames.alterTableStatement,
        STMKnownRuleNames.dropTableStatement,
        STMKnownRuleNames.dropViewStatement,
        STMKnownRuleNames.deleteStatement,
        STMKnownRuleNames.insertStatement,
        STMKnownRuleNames.updateStatement
    );

    private static final Set<String> actualTableNameContainers = Set.of(
        STMKnownRuleNames.tableName
    );

    @NotNull
    public SQLQueryRowsTableDataModel collectTableReference(@NotNull STMTreeNode node, boolean forDDL) {
        return new SQLQueryRowsTableDataModel(node, this.collectTableName(node), forDDL);
    }

    @Nullable
    public SQLQueryComplexName collectTableName(@NotNull STMTreeNode node) {
        return this.collectTableName(node, false);
    }

    @Nullable
    private SQLQueryComplexName collectTableName(@NotNull STMTreeNode node, boolean forceUnquotted) {
        List<STMTreeNode> actual = STMUtils.expandSubtree(node, tableNameContainers, actualTableNameContainers);
        if (actual.size() > 1) {
            log.debug("Ambiguous tableName collection at " + node.getNodeName());
        }
        if (actual.isEmpty()) {
            return null;
        } else {
            return this.collectQualifiedName(actual.getFirst(), forceUnquotted);
        }
    }

    private static final Set<String> qualifiedNameDirectWrapperNames = Set.of(
        STMKnownRuleNames.characterSetSpecification,
        STMKnownRuleNames.characterSetName,
        STMKnownRuleNames.schemaName,
        STMKnownRuleNames.tableName,
        STMKnownRuleNames.constraintName
    );

    @Nullable
    public SQLQueryComplexName collectQualifiedName(@NotNull STMTreeNode node) {
        return this.collectQualifiedName(node, false);
    }

    @Nullable
    private SQLQueryComplexName collectQualifiedName(@NotNull STMTreeNode node, boolean forceUnquotted) {
        STMTreeNode qualifiedNameNode = qualifiedNameDirectWrapperNames.contains(node.getNodeName())
            ? node.findFirstChildOfName(STMKnownRuleNames.qualifiedName)
            : node;
        if (qualifiedNameNode == null) {
            return null;
        } else if (!qualifiedNameNode.getNodeName().equals(STMKnownRuleNames.qualifiedName)) {
            log.debug("qualifiedName expected while facing with " + node.getNodeName());
            return null;
        }

        List<SQLQuerySymbolEntry> nameParts;
        int invalidPartsCount;
        SQLQueryMemberAccessEntry endingMemberAccessEntry = null;

        if (qualifiedNameNode.getChildCount() == 1 && !qualifiedNameNode.hasErrorChildren()) {
            SQLQuerySymbolEntry entityName = this.collectIdentifier(qualifiedNameNode.getChildNode(0), forceUnquotted, null);
            if (entityName == null) {
                return null;
            } else {
                invalidPartsCount = 0;
                nameParts = Collections.singletonList(entityName);
            }
        } else {
            invalidPartsCount = 0;
            nameParts = new ArrayList<>(qualifiedNameNode.getChildCount());
            {
                boolean expectingName = true;
                STMTreeNode periodNode = null;
                for (int i = 0; i < qualifiedNameNode.getChildCount(); i++) {
                    STMTreeNode partNode = qualifiedNameNode.getChildNode(i);
                    if (expectingName) {
                        SQLQuerySymbolEntry namePart;
                        if (partNode.getNodeName().equals(STMKnownRuleNames.PERIOD_TERM)) {
                            namePart = null;
                            if (periodNode != null && endingMemberAccessEntry == null) {
                                endingMemberAccessEntry = new SQLQueryMemberAccessEntry(periodNode);
                            }
                            periodNode = null;
                        } else {
                            namePart = this.collectIdentifier(partNode, forceUnquotted, periodNode);
                            expectingName = false;
                            periodNode = null;
                        }
                        nameParts.add(namePart);
                        invalidPartsCount += namePart == null ? 1 : 0;
                    } else {
                        if (partNode.getNodeName().equals(STMKnownRuleNames.PERIOD_TERM)) {
                            expectingName = true;
                            periodNode = partNode;
                        } else {
                            nameParts.add(null);
                            invalidPartsCount++;
                            periodNode = null;
                        }
                    }
                }
                if (expectingName) { // qualified name ends with PERIOD_TERM, so it is incomplete
                    nameParts.add(null);
                    invalidPartsCount++;
                    if (endingMemberAccessEntry != null) {
                        this.registerScopeItem(endingMemberAccessEntry);
                    } else if (periodNode != null) {
                        this.registerScopeItem(endingMemberAccessEntry = new SQLQueryMemberAccessEntry(periodNode));
                    }
                }
            }
        }

        return new SQLQueryComplexName(node, nameParts, invalidPartsCount, endingMemberAccessEntry);
    }

    private static final Set<String> knownValueExpressionRootNames = Set.of(
        STMKnownRuleNames.valueExpression,
        STMKnownRuleNames.valueExpressionAtom,
        STMKnownRuleNames.searchCondition,
        STMKnownRuleNames.havingClause,
        STMKnownRuleNames.whereClause,
        STMKnownRuleNames.groupByClause,
        STMKnownRuleNames.orderByClause,
        STMKnownRuleNames.rowValueConstructor,
        STMKnownRuleNames.defaultClause,
        STMKnownRuleNames.checkConstraintDefinition
    );

    private static final Set<String> knownRecognizableValueExpressionNames = Set.of(
        STMKnownRuleNames.functionCallExpression,
        STMKnownRuleNames.subquery,
        STMKnownRuleNames.columnReference,
        STMKnownRuleNames.valueReference,
        STMKnownRuleNames.variableExpression,
        STMKnownRuleNames.truthValue,
        STMKnownRuleNames.unsignedNumericLiteral,
        STMKnownRuleNames.signedNumericLiteral,
        STMKnownRuleNames.characterStringLiteral,
        STMKnownRuleNames.datetimeLiteral,
        STMKnownRuleNames.columnIndex
    );

    @NotNull
    public SQLQueryValueExpression collectValueExpression(@NotNull STMTreeNode node, @Nullable SQLQueryLexicalScope scope) {
        if (!knownValueExpressionRootNames.contains(node.getNodeName())) {
            log.debug("Search condition or value expression expected while facing with " + node.getNodeName());
            return new SQLQueryValueFlattenedExpression(node, Collections.emptyList());
        }
        
        if (knownRecognizableValueExpressionNames.contains(node.getNodeName())) {
            return collectKnownValueExpression(node, scope);
        } else {
            LexicalScopeHolder exprScopeHolder = scope != null ? null : this.openScope();
            try {
                Stack<STMTreeNode> stack = new Stack<>();
                Stack<List<SQLQueryValueExpression>> childLists = new Stack<>();
                stack.add(node);
                childLists.push(new ArrayList<>(1));

                while (!stack.isEmpty()) {
                    STMTreeNode n = stack.pop();

                    if (n != null) {
                        STMTreeNode rn = n;
                        while (rn != null && rn.getChildCount() == 1 && !knownRecognizableValueExpressionNames.contains(rn.getNodeName())) {
                            rn = rn.findFirstNonErrorChild();
                        }
                        if (rn != null) {
                            if (knownRecognizableValueExpressionNames.contains(rn.getNodeName())
                                || rn.getNodeName().equals(STMKnownRuleNames.valueExpressionPrimary)
                            ) {
                                childLists.peek().add(this.collectKnownValueExpression(rn, scope));
                            } else {
                                stack.push(n);
                                stack.push(null);
                                List<STMTreeNode> children = rn.findNonErrorChildren();
                                childLists.push(new ArrayList<>(children.size()));
                                for (int i = children.size() - 1; i >= 0; i--) {
                                    stack.push(children.get(i));
                                }
                            }
                        }
                    } else {
                        // TODO register unexpected pieces in the lexical scope
                        STMTreeNode content = stack.pop();
                        List<SQLQueryValueExpression> children = childLists.pop();
                        if (!children.isEmpty()) {
                            SQLQueryValueExpression e =
                                children.size() == 1 && children.get(0) instanceof SQLQueryValueFlattenedExpression child
                                    ? child
                                    : new SQLQueryValueFlattenedExpression(content, children);
                            childLists.peek().add(e);
                        }
                    }
                }

                List<SQLQueryValueExpression> roots = childLists.pop();
                SQLQueryValueExpression result = roots.isEmpty()
                    ? new SQLQueryValueFlattenedExpression(node, Collections.emptyList())
                    : roots.get(0);

                if (exprScopeHolder != null) {
                    result.registerLexicalScope(exprScopeHolder.lexicalScope);
                }
                return result;
            } finally {
                if (exprScopeHolder != null) {
                    exprScopeHolder.close();;
                }
            }
        }
    }

    @NotNull
    public SQLQueryValueExpression collectKnownValueExpression(@NotNull STMTreeNode node, @Nullable SQLQueryLexicalScope scope) {
        SQLQueryValueExpression result = switch (node.getNodeKindId()) {
            case SQLStandardParser.RULE_functionCallExpression -> this.collectFunctionCall(node, scope, false);
            case SQLStandardParser.RULE_subquery -> new SQLQueryValueSubqueryExpression(node, this.collectQueryExpression(node));
            case SQLStandardParser.RULE_valueReference -> this.collectValueReferenceExpression(node, false);
            case SQLStandardParser.RULE_valueExpressionPrimary -> {
                STMTreeNode valueExprNode = node.findFirstChildOfName(STMKnownRuleNames.valueExpressionAtom);
                if (valueExprNode == null) {
                    yield null;
                } else {
                    SQLQueryValueExpression subexpr = this.collectValueExpression(valueExprNode, scope);
                    STMTreeNode castSpecNode = node.findFirstChildOfName(STMKnownRuleNames.valueExpressionCastSpec);
                    if (castSpecNode != null) {
                        STMTreeNode dataTypeNode = castSpecNode.findLastChildOfName(STMKnownRuleNames.dataType);
                        String typeName = dataTypeNode == null ? "UNKNOWN" : dataTypeNode.getTextContent();
                        yield new SQLQueryValueTypeCastExpression(node, subexpr, typeName);
                    } else {
                        yield subexpr;
                    }
                }
            }
            case SQLStandardParser.RULE_variableExpression -> {
                STMTreeNode varExprNode = node.findFirstNonErrorChild();
                if (varExprNode instanceof STMTreeTermNode varExprTermNode) {
                    String rawName = varExprTermNode.getTextContent();
                    yield switch (rawName.charAt(0)) {
                        case '@' -> new SQLQueryValueVariableExpression(
                            node,
                            this.registerSymbolEntry(node, rawName.substring(1), rawName, null),
                            SQLQueryValueVariableExpression.VariableExpressionKind.BATCH_VARIABLE,
                            rawName
                        );
                        case '$' -> new SQLQueryValueVariableExpression(
                            node,
                            this.registerSymbolEntry(node, rawName.substring(2, rawName.length() - 1), rawName, null),
                            SQLQueryValueVariableExpression.VariableExpressionKind.CLIENT_VARIABLE,
                            rawName
                        );
                        default -> {
                            log.debug("Unsupported term variable expression: " + node.getTextContent());
                            yield null;
                        }
                    };
                } else if (varExprNode != null) {
                    yield switch (varExprNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_namedParameter ->  {
                            STMTreeNode identifierNode = varExprNode.findLastNonErrorChild();
                            String name = identifierNode == null ? SQLConstants.QUESTION : identifierNode.getTextContent();
                            yield new SQLQueryValueVariableExpression(
                                node,
                                this.registerSymbolEntry(node, name, varExprNode.getTextContent(), null),
                                SQLQueryValueVariableExpression.VariableExpressionKind.CLIENT_PARAMETER,
                                varExprNode.getTextContent()
                            );
                        }
                        case SQLStandardParser.RULE_anonymouseParameter -> {
                            STMTreeNode markNode = varExprNode.findLastNonErrorChild();
                            String mark = markNode == null ? SQLConstants.QUESTION : markNode.getTextContent();
                            this.registerSymbolEntry(node, mark, mark, null);
                            yield new SQLQueryValueVariableExpression(
                                node,
                                null,
                                SQLQueryValueVariableExpression.VariableExpressionKind.CLIENT_PARAMETER,
                                varExprNode.getTextContent()
                            );
                        }
                        default -> {
                            log.debug("Unsupported variable expression: " + node.getTextContent());
                            yield null;
                        }
                    };
                } else {
                    yield null;
                }
            }
            case SQLStandardParser.RULE_columnIndex -> this.makeValueConstantExpression(node, SQLQueryExprType.NUMERIC);
            case SQLStandardParser.RULE_truthValue -> this.makeValueConstantExpression(node, SQLQueryExprType.BOOLEAN);
            case SQLStandardParser.RULE_unsignedNumericLiteral -> this.makeValueConstantExpression(node, SQLQueryExprType.NUMERIC);
            case SQLStandardParser.RULE_signedNumericLiteral -> this.makeValueConstantExpression(node, SQLQueryExprType.NUMERIC);
            case SQLStandardParser.RULE_characterStringLiteral -> this.makeValueConstantExpression(node, SQLQueryExprType.STRING);
            case SQLStandardParser.RULE_datetimeLiteral -> this.makeValueConstantExpression(node, SQLQueryExprType.DATETIME);
            default -> throw new UnsupportedOperationException("Unknown expression kind " + node.getNodeName());
        };
        return result != null ? result : new SQLQueryValueFlattenedExpression(node, Collections.emptyList());
    }

    @NotNull
    private SQLQueryValueExpression makeValueConstantExpression(@NotNull STMTreeNode node, @NotNull SQLQueryExprType type) {
        return new SQLQueryValueConstantExpression(node, node.getTextContent(), type);
    }

    @NotNull
    private SQLQueryValueExpression collectValueReferenceExpression(@NotNull STMTreeNode node, boolean rowRefAllowed) {
        interface LazyExpr {
            SQLQueryValueExpression getExpression(boolean rowRefAllowed);

            static LazyExpr of(SQLQueryValueExpression expr) {
                return b -> expr;
            }
        }

        List<STMTreeNode> subnodes = node.findNonErrorChildren();
        LazyExpr expr;
        if (subnodes.size() > 0) {
            STMTreeNode head = subnodes.get(0);
            expr = switch (head.getNodeKindId()) {
                case SQLStandardParser.RULE_columnReference -> b -> this.collectColumnReferenceExpression(head, b);
                case SQLStandardParser.RULE_valueRefNestedExpr -> {
                    STMTreeNode valueRefNode = head.findFirstChildOfName(STMKnownRuleNames.valueReference);
                    yield valueRefNode == null ? null : b -> this.collectValueReferenceExpression(valueRefNode, b);
                }
                default -> {
                    log.debug("Value reference expression expected while facing with " + head.getNodeName());
                    yield null;
                }
            };
        } else {
            expr = null;
        }

        if (expr != null && subnodes.size() > 1) {
            int rangeStart = node.getRealInterval().a;
            boolean[] slicingFlags = new boolean[subnodes.size()];
            for (int i = 1; i < subnodes.size(); ) {
                STMTreeNode step = subnodes.get(i);
                Interval range = new Interval(rangeStart, step.getRealInterval().b);
                expr = switch (step.getNodeKindId()) {
                    case SQLStandardParser.RULE_valueRefIndexingStep -> {
                        int s = i;
                        for (; i < subnodes.size() && (step = subnodes.get(i)).getNodeKindId() == SQLStandardParser.RULE_valueRefIndexingStep; i++) {
                            slicingFlags[i] = step.findFirstChildOfName(STMKnownRuleNames.valueRefIndexingStepSlice) != null;
                        }
                        boolean[] slicingSpec = Arrays.copyOfRange(slicingFlags, s, i);
                        yield LazyExpr.of(new SQLQueryValueIndexingExpression(range, node, expr.getExpression(false), slicingSpec));
                    }
                    case SQLStandardParser.RULE_valueRefMemberStep -> {
                        i++;
                        STMTreeNode periodNode = step.findLastChildOfName(STMKnownRuleNames.PERIOD_TERM);
                        STMTreeNode memberNameNode = step.findLastChildOfName(STMKnownRuleNames.identifier);
                        SQLQuerySymbolEntry memberName = memberNameNode == null ? null : this.collectIdentifier(memberNameNode, periodNode);
                        SQLQueryMemberAccessEntry memberAccessEntry = memberName != null ? memberName.getMemberAccess() : this.registerScopeItem(new SQLQueryMemberAccessEntry(periodNode));
                        yield LazyExpr.of(new SQLQueryValueMemberExpression(range, node, expr.getExpression(true), memberName, memberAccessEntry));
                    }
                    default -> throw new UnsupportedOperationException(
                        "Value member expression expected while facing with " + step.getNodeName()
                    );
                };
            }
        }

        return expr != null ? expr.getExpression(rowRefAllowed) : new SQLQueryValueFlattenedExpression(node, Collections.emptyList());
    }

    @Nullable
    private SQLQueryValueExpression collectColumnReferenceExpression(@NotNull STMTreeNode head, boolean rowRefAllowed) {
        STMTreeNode nameNode = head.findFirstChildOfName(STMKnownRuleNames.qualifiedName);
        if (nameNode == null) {
            return null;
        }
        SQLQueryComplexName name = this.collectQualifiedName(nameNode);
        if (name == null) {
            return null;
        }
        STMTreeNode tupleRefNode = head.findLastChildOfName(STMKnownRuleNames.tupleRefSuffix);
        if (tupleRefNode == null) {
            return new SQLQueryValueReferenceExpression(head, rowRefAllowed, name);
        } else {
            STMTreeNode asteriskNode = tupleRefNode.findFirstChildOfName(STMKnownRuleNames.ASTERISK_TERM);
            SQLQueryTupleRefEntry tupleRefEntry = asteriskNode == null ? null : new SQLQueryTupleRefEntry(asteriskNode);
            STMTreeNode periodNode = tupleRefNode.findFirstChildOfName(STMKnownRuleNames.PERIOD_TERM);
            SQLQueryMemberAccessEntry memberAccessEntry = periodNode == null
                ? null
                : this.registerScopeItem(new SQLQueryMemberAccessEntry(periodNode));
            return new SQLQueryValueTupleReferenceExpression(head, name, memberAccessEntry, tupleRefEntry);
        }
    }

    @NotNull
    public SQLQueryValueFunctionExpression collectFunctionCall(@NotNull STMTreeNode callNode, @Nullable SQLQueryLexicalScope scope, boolean forRows) {
        // TODO handle callNode.hasErrorChildren()
        STMTreeNode nameNode = callNode.findFirstChildOfName(STMKnownRuleNames.functionCallTargetName);
        SQLQueryComplexName name;
        if (nameNode != null) {
            STMTreeNode actualNameNode = nameNode.findFirstChildOfName(STMKnownRuleNames.qualifiedName);
            if (actualNameNode != null) {
                name = this.collectQualifiedName(actualNameNode);
            } else {
                STMTreeNode term = nameNode.findFirstNonErrorChild();
                if (term != null) {
                    String nameString = term.getTextContent();
                    String canonicalNameString = SQLUtils.identifierToCanonicalForm(dialect, nameString, false, false);
                    SQLQuerySymbolEntry nameEntry = this.registerSymbolEntry(term, canonicalNameString, nameString, null);
                    name = new SQLQueryComplexName(term, List.of(nameEntry), 0, null);
                } else {
                    name = null;
                }
            }
        } else {
            name = null;
        }

        List<STMTreeNode> argNodes = callNode.findChildrenOfName(STMKnownRuleNames.functionCallOperand);
        List<SQLQueryValueExpression> argExprs = new ArrayList<>(argNodes.size());
        for (STMTreeNode argNode : argNodes) {
            STMTreeNode value = argNode.findFirstNonErrorChild();
            STMTreeNode n = value == null ? null : value.findFirstNonErrorChild();
            SQLQueryValueExpression expr = n != null
                ? this.collectValueExpression(n, scope)
                : new SQLQueryValueFlattenedExpression(argNode, Collections.emptyList());
            argExprs.add(expr);
        }

        return new SQLQueryValueFunctionExpression(callNode, name, argExprs, forRows);
    }

    /**
     * Set the query symbol class to the quoted identifier, depends on the quote type
     */
    @Nullable
    public static SQLQuerySymbolClass tryFallbackSymbolForStringLiteral(
        @NotNull SQLDialect dialect,
        @NotNull SQLQuerySymbolEntry symbolEntry,
        boolean isColumnResolved
    ) {
        SQLQuerySymbolClass forcedClass = null;
        boolean isQuotedIdentifier = dialect.isQuotedIdentifier(symbolEntry.getRawName());
        char quoteChar = symbolEntry.getRawName().charAt(0);
        if ((!isQuotedIdentifier && (quoteChar == '"' || quoteChar == '`' || quoteChar == '\''))
            || (isQuotedIdentifier && !isColumnResolved)) {
            forcedClass = switch (quoteChar) {
                case '\'' -> SQLQuerySymbolClass.STRING;
                case '"', '`' -> SQLQuerySymbolClass.QUOTED;
                default -> null;
            };
        }
        return forcedClass;
    }

    private SQLQueryLexicalScope beginScope() {
        SQLQueryLexicalScope scope = new SQLQueryLexicalScope();
        this.currentLexicalScopes.addLast(scope);
        return scope;
    }

    private void endScope(SQLQueryLexicalScope scope) {
        if (this.currentLexicalScopes.peekLast() != scope) {
            throw new IllegalStateException();
        }
        this.currentLexicalScopes.removeLast();
    }

    /**
     * Add new lexical item to the query context
     */
    public <T extends SQLQueryLexicalScopeItem> T registerScopeItem(T item) {
        SQLQueryLexicalScope scope = this.currentLexicalScopes.peekLast();
        if (scope != null) {
            scope.registerItem(item);
        }
        this.lexicalItems.put(item.getSyntaxNode().getRealInterval().a, item);
        return item;
    }
    
    public class LexicalScopeHolder implements AutoCloseable {

        @NotNull
        public final SQLQueryLexicalScope lexicalScope;
        
        public LexicalScopeHolder(@NotNull SQLQueryLexicalScope scope) {
            this.lexicalScope = scope;
        }

        @Override
        public void close() {
            SQLQueryModelRecognizer.this.endScope(this.lexicalScope);
        }
    }

    public LexicalScopeHolder openScope() {
        return new LexicalScopeHolder(this.beginScope());
    }

    @Nullable
    public static SQLQueryModel recognizeQuery(@NotNull SQLQueryRecognitionContext recognitionContext, @NotNull String queryText) {
        SQLQueryModelRecognizer recognizer = new SQLQueryModelRecognizer(recognitionContext);
        return recognizer.recognizeQuery(queryText);
    }
}
