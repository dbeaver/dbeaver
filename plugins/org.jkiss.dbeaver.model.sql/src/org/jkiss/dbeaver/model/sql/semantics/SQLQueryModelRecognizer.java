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
package org.jkiss.dbeaver.model.sql.semantics;


import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import org.jkiss.dbeaver.Log;import org.jkiss.dbeaver.DBException;
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
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryObjectDropModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableAlterModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableDropModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryDeleteModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryInsertModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryUpdateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.*;
import org.jkiss.dbeaver.model.sql.semantics.model.select.*;
import org.jkiss.dbeaver.model.stm.*;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.utils.Pair;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
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
    
    private final SQLQueryRecognitionContext recognitionContext;

    private final DBCExecutionContext executionContext;
    
    private final Set<String> reservedWords;

    private final SQLDialect dialect;
    
    private final LinkedList<SQLQueryLexicalScope> currentLexicalScopes = new LinkedList<>();

    private SQLQueryDataContext queryDataContext;

    private SQLQueryModelRecognizer(@NotNull SQLQueryRecognitionContext recognitionContext) {
        this.recognitionContext = recognitionContext;

        this.executionContext = recognitionContext.getExecutionContext();
        this.dialect = recognitionContext.getDialect();
        this.reservedWords = new HashSet<>(this.dialect.getReservedWords());
    }

    public SQLQueryDataContext getQueryDataContext() {
        return queryDataContext;
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
            return tree == null ? null : new SQLQueryModel(tree, null, Collections.emptySet());
        }
        this.queryDataContext = this.prepareDataContext(tree);
        STMTreeNode queryNode = tree.findFirstNonErrorChild();
        if (queryNode == null) {
            return new SQLQueryModel(tree, null, Collections.emptySet());
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
                        SQLQueryObjectDropModel.recognize(this, stmtBodyNode, RelationalObjectType.TYPE_PROCEDURE);
                    case SQLStandardParser.RULE_alterTableStatement ->
                        SQLQueryTableAlterModel.recognize(this, stmtBodyNode);
                    default -> null;
                };
            }
            default -> null;
        };

        if (contents != null) {
            SQLQueryModel model = new SQLQueryModel(tree, contents, symbolEntries);

            model.propagateContext(this.queryDataContext, this.recognitionContext);

            int actualTailPosition = model.getSyntaxNode().getRealInterval().b;
            SQLQueryNodeModel tailNode = model.findNodeContaining(actualTailPosition);
            if (tailNode != model) {
                SQLQueryLexicalScope nodeScope = tailNode.findLexicalScope(actualTailPosition);
                SQLQueryLexicalScope tailScope = new SQLQueryLexicalScope();
                tailScope.setInterval(Interval.of(actualTailPosition, Integer.MAX_VALUE));
                tailScope.setContext(nodeScope != null && nodeScope.getContext() != null
                    ? nodeScope.getContext()
                    : tailNode.getGivenDataContext());
                model.registerLexicalScope(tailScope);
            }

            for (SQLQuerySymbolEntry symbolEntry : this.symbolEntries) {
                if (symbolEntry.isNotClassified() && this.reservedWords.contains(symbolEntry.getRawName().toUpperCase())) {
                    // (keywords are uppercased in dialect)
                    // if non-reserved keyword was not classified as identifier, then highlight it as reserved
                    symbolEntry.getSymbol().setSymbolClass(SQLQuerySymbolClass.RESERVED);
                }
            }

            // var tt = new DebugGraphBuilder();
            // tt.traverseObjs(model);
            // tt.graph.saveToFile("c:/temp/outx.dgml");

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

        this.traverseForIdentifiers(tree,
            (e, c) -> {
                if (c.isNotClassified() && (e != null || !tryFallbackForStringLiteral.test(c))) {
                    c.getSymbol().setSymbolClass(SQLQuerySymbolClass.COLUMN);
                }
            },
            e -> {
                DBSEntity table = null;
                if (e.isNotClassified() || !tryFallbackForStringLiteral.test(e.entityName)) {
                    if (e.invalidPartsCount == 0) {
                        DBSObject object = this.queryDataContext.findRealObject(
                            recognitionContext.getMonitor(), RelationalObjectType.TYPE_UNKNOWN, e.toListOfStrings()
                        );
                        if (object != null) {
                            if (object instanceof DBSTable realTable) {
                                table = realTable;
                            } else if (object instanceof DBSView realView) {
                                table = realView;
                            }
                            e.setDefinition(object);
                        }
                    } else {
                        SQLQueryQualifiedName.performPartialResolution(this.queryDataContext, this.recognitionContext, e);
                    }
                }
                return table;
            },
            false
        );
        return new SQLQueryModel(tree, null, symbolEntries);
    }

    private void traverseForIdentifiers(
        @NotNull STMTreeNode root,
        @NotNull BiConsumer<DBSEntity, SQLQuerySymbolEntry> columnAction,
        @NotNull Function<SQLQueryQualifiedName, DBSEntity> entityAction,
        boolean forceUnquotted
    ) {
        List<STMTreeNode> refs = STMUtils.expandSubtree(
            root,
            null,
            Set.of(STMKnownRuleNames.columnReference, STMKnownRuleNames.columnName, STMKnownRuleNames.tableName)
        );
        for (STMTreeNode ref : refs) {
            switch (ref.getNodeKindId()) {
                case SQLStandardParser.RULE_columnName -> {
                    SQLQuerySymbolEntry columnName = this.collectIdentifier(ref, forceUnquotted);
                    if (columnName != null) {
                        columnAction.accept(null, columnName);
                    }
                }
                case SQLStandardParser.RULE_columnReference -> {
                    SQLQueryValueExpression expr = this.collectColumnReferenceExpression(ref, false);
                    SQLQueryQualifiedName tableName;
                    SQLQuerySymbolEntry columnName;
                    if (expr instanceof SQLQueryValueTupleReferenceExpression tulpeRef) {
                        tableName = tulpeRef.getTableName();
                        columnName = null;
                    } else if (expr instanceof SQLQueryValueColumnReferenceExpression columnRef) {
                        tableName = columnRef.getTableName();
                        columnName = columnRef.getColumnName();
                    } else {
                        tableName = null;
                        columnName = null;
                    }
                    DBSEntity table = tableName == null ? null : entityAction.apply(tableName);
                    if (columnName != null) {
                        columnAction.accept(table, columnName);
                    }
                }
                case SQLStandardParser.RULE_tableName -> {
                    SQLQueryQualifiedName tableName = this.collectTableName(ref, forceUnquotted);
                    if (tableName != null) {
                        entityAction.apply(tableName);
                    }
                }
                default -> throw new IllegalArgumentException("Unexpected value: " + ref.getNodeName());
            }
        }
    }

    @NotNull
    private SQLQueryDataContext prepareDataContext(@NotNull STMTreeNode root) {
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
                    rowsetPseudoColumns = rowsetsPc.isEmpty() ? s -> Collections.emptyList() : (
                        s -> SQLQueryRowsTableDataModel.prepareResultPseudoColumnsList(
                            this.dialect, s, null, rowsetsPc.stream()
                        )
                    );
                } catch (DBException e) {
                    this.recognitionContext.appendError(root, "Failed to obtain global pseudo-columns information", e);
                    rowsetPseudoColumns = s -> Collections.emptyList();
                }
            } else {
                rowsetPseudoColumns = s -> Collections.emptyList();
            }
            return new SQLQueryDataSourceContext(this.dialect, this.executionContext, globalPseudoColumns, rowsetPseudoColumns);
        } else {
            Set<String> allColumnNames = new HashSet<>();
            Set<List<String>> allTableNames = new HashSet<>();
            this.traverseForIdentifiers(
                root,
                (e, c) -> allColumnNames.add(c.getName()),
                e -> { allTableNames.add(e.toListOfStrings()); return null; },
                true);
            symbolEntries.clear();
            return new SQLQueryDummyDataSourceContext(this.dialect, allColumnNames, allTableNames);
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
            .map(this::collectIdentifier).toList();
        return result;
    }

    private static final Set<String> identifierDirectWrapperNames = Set.of(
        STMKnownRuleNames.correlationName,
        STMKnownRuleNames.authorizationIdentifier,
        STMKnownRuleNames.columnName,
        STMKnownRuleNames.queryName
    );
    
    @Nullable
    public SQLQuerySymbolEntry collectIdentifier(@NotNull STMTreeNode node) {
        return collectIdentifier(node, false);
    }
    
    @Nullable
    private SQLQuerySymbolEntry collectIdentifier(@NotNull STMTreeNode node, boolean forceUnquotted) {
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

        String rawIdentifierString = identifierTextNode.getTextContent();
        if (identifierTextNode.getPayload() instanceof Token t && t.getType() == SQLStandardLexer.Quotted) {
            SQLQuerySymbolEntry entry = this.registerSymbolEntry(identifierTextNode, rawIdentifierString, rawIdentifierString);
            // not canonicalizing the identifier because it is quoted,
            // but the QUOTED class will be assigned later after db entity resolution fail
            // entry.getSymbol().setSymbolClass(SQLQuerySymbolClass.QUOTED);
            return entry;
        } else {
            String actualIdentifierString = SQLUtils.identifierToCanonicalForm(dialect, rawIdentifierString, forceUnquotted, false);
            return this.registerSymbolEntry(identifierTextNode, actualIdentifierString, rawIdentifierString);
        }
    }

    @NotNull
    private SQLQuerySymbolEntry registerSymbolEntry(
        @NotNull STMTreeNode syntaxNode,
        @NotNull String name,
        @NotNull String rawName
    ) {
        SQLQuerySymbolEntry entry = new SQLQuerySymbolEntry(syntaxNode, name, rawName);
        this.symbolEntries.add(entry);
        this.registerScopeItem(entry);
        return entry;
    }

    private static final Set<String> tableNameContainers = Set.of(
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
        STMKnownRuleNames.tableName,
        STMKnownRuleNames.correlationName
    );

    @NotNull
    public SQLQueryRowsTableDataModel collectTableReference(@NotNull STMTreeNode node, boolean forDDL) {
        return new SQLQueryRowsTableDataModel(node, this.collectTableName(node), forDDL);
    }

    @Nullable
    public SQLQueryQualifiedName collectTableName(@NotNull STMTreeNode node) {
        return this.collectTableName(node, false);
    }

    @Nullable
    private SQLQueryQualifiedName collectTableName(@NotNull STMTreeNode node, boolean forceUnquotted) {
        List<STMTreeNode> actual = STMUtils.expandSubtree(node, tableNameContainers, actualTableNameContainers);
        if (actual.isEmpty()) {
            return null;
        } else {
            if (actual.size() > 1) {
                log.debug("Ambiguous tableName collection at " + node.getNodeName());
            }
            node = actual.get(0);
            if (node.getNodeName().equals(STMKnownRuleNames.tableName)) {
                return this.collectQualifiedName(node, forceUnquotted);
            } else {
                SQLQuerySymbolEntry nameEntry = collectIdentifier(node, forceUnquotted);
                return nameEntry == null ? null : this.registerScopeItem(
                    new SQLQueryQualifiedName(node, Collections.emptyList(), nameEntry, 0)
                );
            }
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
    public SQLQueryQualifiedName collectQualifiedName(@NotNull STMTreeNode node) {
        return this.collectQualifiedName(node, false);
    }

    @Nullable
    private SQLQueryQualifiedName collectQualifiedName(@NotNull STMTreeNode node, boolean forceUnquotted) { // qualifiedName
        Pair<List<SQLQuerySymbolEntry>, Integer> nameInfo = collectQualifiedNameParts(node, forceUnquotted);
        if (nameInfo == null) {
            return null;
        } else {
            List<SQLQuerySymbolEntry> nameParts = nameInfo.getFirst();
            int invalidPartsCount = nameInfo.getSecond();

            List<SQLQuerySymbolEntry> scopeName = nameParts.subList(0, nameParts.size() - 1);
            SQLQuerySymbolEntry entityName = nameParts.get(nameParts.size() - 1);

            return entityName == null ? null : this.registerScopeItem(new SQLQueryQualifiedName(node, scopeName, entityName, invalidPartsCount));
        }
    }

    @Nullable
    private Pair<List<SQLQuerySymbolEntry>, Integer> collectQualifiedNameParts(@NotNull STMTreeNode node, boolean forceUnquotted) {
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

        if (qualifiedNameNode.getChildCount() == 1 && !qualifiedNameNode.hasErrorChildren()) {
            SQLQuerySymbolEntry entityName = this.collectIdentifier(qualifiedNameNode.getChildNode(0), forceUnquotted);
            invalidPartsCount = entityName == null ? 1 : 0;
            nameParts = Collections.singletonList(entityName);
        } else {
            invalidPartsCount = 0;
            nameParts = new ArrayList<>(qualifiedNameNode.getChildCount());
            {
                boolean expectingName = true;
                for (int i = 0; i < qualifiedNameNode.getChildCount(); i++) {
                    STMTreeNode partNode = qualifiedNameNode.getChildNode(i);
                    if (expectingName) {
                        SQLQuerySymbolEntry namePart;
                        if (partNode.getNodeName().equals(STMKnownRuleNames.PERIOD_TERM)) {
                            namePart = null;
                        } else {
                            namePart = this.collectIdentifier(partNode, forceUnquotted);
                            expectingName = false;
                        }
                        nameParts.add(namePart);
                        invalidPartsCount += namePart == null ? 1 : 0;
                    } else {
                        if (partNode.getNodeName().equals(STMKnownRuleNames.PERIOD_TERM)) {
                            expectingName = true;
                        } else {
                            nameParts.add(null);
                            invalidPartsCount++;
                        }
                    }
                }
                if (expectingName) { // qualified name ends with PERIOD_TERM, so it is incomplete
                    nameParts.add(null);
                    invalidPartsCount++;
                }
            }
        }

        return Pair.of(nameParts, invalidPartsCount);
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
    public SQLQueryValueExpression collectValueExpression(@NotNull STMTreeNode node) {
        if (!knownValueExpressionRootNames.contains(node.getNodeName())) {
            log.debug("Search condition or value expression expected while facing with " + node.getNodeName());
            return new SQLQueryValueFlattenedExpression(node, Collections.emptyList());
        }
        
        if (knownRecognizableValueExpressionNames.contains(node.getNodeName())) {
            return collectKnownValueExpression(node);
        } else {
            try (LexicalScopeHolder sh = this.openScope()) {
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
                                childLists.peek().add(collectKnownValueExpression(rn));
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
                            SQLQueryValueExpression e = children.size() == 1 && children.get(0) instanceof SQLQueryValueFlattenedExpression child
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
                
                result.registerLexicalScope(sh.lexicalScope);
                return result;
            }
        }
    }

    @NotNull
    public SQLQueryValueExpression collectKnownValueExpression(@NotNull STMTreeNode node) {
        SQLQueryValueExpression result = switch (node.getNodeKindId()) {
            case SQLStandardParser.RULE_subquery -> new SQLQueryValueSubqueryExpression(node, this.collectQueryExpression(node));
            case SQLStandardParser.RULE_valueReference -> this.collectValueReferenceExpression(node, false);
            case SQLStandardParser.RULE_valueExpressionPrimary -> {
                STMTreeNode valueExprNode = node.findFirstChildOfName(STMKnownRuleNames.valueExpressionAtom);
                if (valueExprNode == null) {
                    yield null;
                } else {
                    SQLQueryValueExpression subexpr = this.collectValueExpression(valueExprNode);
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
                            this.registerSymbolEntry(node, rawName.substring(1), rawName),
                            SQLQueryValueVariableExpression.VariableExpressionKind.BATCH_VARIABLE,
                            rawName
                        );
                        case '$' -> new SQLQueryValueVariableExpression(
                            node,
                            this.registerSymbolEntry(node, rawName.substring(2, rawName.length() - 1), rawName),
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
                                this.registerSymbolEntry(node, name, varExprNode.getTextContent()),
                                SQLQueryValueVariableExpression.VariableExpressionKind.CLIENT_PARAMETER,
                                varExprNode.getTextContent()
                            );
                        }
                        case SQLStandardParser.RULE_anonymouseParameter -> {
                            STMTreeNode markNode = varExprNode.findLastNonErrorChild();
                            String mark = markNode == null ? SQLConstants.QUESTION : markNode.getTextContent();
                            this.registerSymbolEntry(node, mark, mark);
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
                        for (; i < subnodes.size() && step.getNodeKindId() == SQLStandardParser.RULE_valueRefIndexingStep; i++) {
                            step = subnodes.get(i);
                            slicingFlags[i] = step.findFirstChildOfName(STMKnownRuleNames.valueRefIndexingStepSlice) != null;
                        }
                        boolean[] slicingSpec = Arrays.copyOfRange(slicingFlags, s, i);
                        yield LazyExpr.of(new SQLQueryValueIndexingExpression(range, node, expr.getExpression(false), slicingSpec));
                    }
                    case SQLStandardParser.RULE_valueRefMemberStep -> {
                        i++;
                        STMTreeNode memberNameNode = step.findLastChildOfName(STMKnownRuleNames.identifier);
                        SQLQuerySymbolEntry memberName = memberNameNode == null ? null : this.collectIdentifier(memberNameNode);
                        yield LazyExpr.of(new SQLQueryValueMemberExpression(range, node, expr.getExpression(true), memberName));
                    }
                    default -> throw new UnsupportedOperationException(
                        "Value member expression expected while facing with " + node.getNodeName()
                    );
                };
            }
        }

        return expr != null ? expr.getExpression(rowRefAllowed) : new SQLQueryValueFlattenedExpression(node, Collections.emptyList());
    }

    @Nullable
    private SQLQueryValueExpression collectColumnReferenceExpression(@NotNull STMTreeNode head, boolean rowRefAllowed) {
        STMTreeNode nameNode = head.findFirstChildOfName(STMKnownRuleNames.qualifiedName);
        boolean hasTupleRef = head.findLastChildOfName(STMKnownRuleNames.tupleRefSuffix) != null;
        if (hasTupleRef) {
            STMTreeNode tableNameNode = head.findFirstChildOfName(STMKnownRuleNames.tableName);
            SQLQueryQualifiedName tableName = tableNameNode == null ? null : this.collectTableName(tableNameNode);
            return new SQLQueryValueTupleReferenceExpression(head, tableName != null ? tableName : this.makeUnknownTableName(head));
        } else if (nameNode == null) {
            return null;
        } else {
            Pair<List<SQLQuerySymbolEntry>, Integer> nameInfo = this.collectQualifiedNameParts(nameNode, false);
            if (nameInfo == null) {
                return null;
            } else {
                List<SQLQuerySymbolEntry> nameParts = nameInfo.getFirst();
                int invalidPartsCount = nameInfo.getSecond();
                SQLQuerySymbolEntry columnName = nameParts.get(nameParts.size() - 1);
                if (nameParts.size() == 1) {
                    if (columnName != null && invalidPartsCount == 0) {
                        return new SQLQueryValueColumnReferenceExpression(head, rowRefAllowed, null, columnName);
                    } else {
                        return null;
                    }
                } else {
                    List<SQLQuerySymbolEntry> tableScopeName = nameParts.subList(0, nameParts.size() - 2);
                    SQLQuerySymbolEntry tableEntityName = nameParts.get(nameParts.size() - 2);

                    int tableInvalidParts = columnName == null ? invalidPartsCount - 1 : invalidPartsCount;
                    SQLQueryQualifiedName tableName = tableEntityName == null
                        ? this.makeUnknownTableName(head)
                        : this.registerScopeItem(new SQLQueryQualifiedName(nameNode, tableScopeName, tableEntityName, tableInvalidParts));

                    return new SQLQueryValueColumnReferenceExpression(head, rowRefAllowed, tableName, columnName);
                }
            }
        }
    }

    @NotNull
    private SQLQueryQualifiedName makeUnknownTableName(@NotNull STMTreeNode node) {
        return new SQLQueryQualifiedName(
            node, Collections.emptyList(), new SQLQuerySymbolEntry(node, SQLConstants.QUESTION, SQLConstants.QUESTION), 0
        );
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
    
    private <T extends SQLQueryLexicalScopeItem> T registerScopeItem(T item) {
        SQLQueryLexicalScope scope = this.currentLexicalScopes.peekLast();
        if (scope != null) {
            scope.registerItem(item);
        }
        return item;
    }
    
    public class LexicalScopeHolder implements AutoCloseable {

        public final SQLQueryLexicalScope lexicalScope;
        
        public LexicalScopeHolder(SQLQueryLexicalScope scope) {
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

    /**
     * A debugging facility
     */
    private static class DebugGraphBuilder {
        private final DirectedGraph graph = new DirectedGraph();
        private final LinkedList<Pair<Object, Object>> stack = new LinkedList<>();
        private final Set<Object> done = new HashSet<>();
        private final Map<Object, DirectedGraph.Node> objs = new HashMap<>();

        private void expandObject(Object prev, Object o) {
            String propName = prev == null ? null : (String) ((Pair) prev).getFirst();
            Object src = prev == null ? null : ((Pair) prev).getSecond();
            if (o instanceof SQLQueryDataContext || o instanceof SQLQueryRowsSourceModel || o instanceof SQLQueryValueExpression) {
                DirectedGraph.Node node = objs.get(o);
                DirectedGraph.Node prevNode = objs.get(src);
                if (node == null) {
                    var color = o instanceof SQLQueryDataContext ? "#bbbbff"
                        : (o instanceof SQLQueryRowsSourceModel ? "#bbffbb"
                        : (o instanceof SQLQueryValueExpression ? "#ffbbbb"
                        : "#bbbbbb"));
                    node = graph.createNode(o.toString().substring(o.getClass().getPackageName().length()), color);
                    objs.put(o, node);
                }
                if (prevNode != null) {
                    graph.createEdge(prevNode, node, propName, null);
                }
                src = o;
                propName = "";
            }
            if (done.contains(o)) {
                return;
            }
            done.add(o);
            // System.out.println((prev == null ? "<NULL>" : prev.toString()) + " --> " + o.toString());

            if (o instanceof String || o.getClass().isPrimitive() || o.getClass().isEnum()) {
                return;
            } else if (o instanceof SQLQuerySymbol || o instanceof DBSObject || o instanceof DBCExecutionContext) {
                // || o instanceof SQLQueryColumnReferenceExpression) {
                return;
            } else if (o instanceof Iterable it) {
//                int index = 0;
//                for (Object y: it) {
//                    if (y != null && !done.contains(y)) {
//                        stack.addLast(new Pair<Object, Object>(new Pair(propName + "[" + (index++) + "]", src), y));
//                    }
//                }
                return;
            }

            Class t = o.getClass();
            while (t != Object.class) {
                for (Field f : t.getDeclaredFields()) {
                    try {
                        if (f.canAccess(o) || f.trySetAccessible()) {
                            Object x = f.get(o);
                            if (x != null) {
                                if (x instanceof String || x.getClass().isEnum()) {
                                    DirectedGraph.Node prevNode = objs.get(src);
                                    if (prevNode != null) {
                                        String text = x.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;").replace("\n", "&#10;");
//                                            DirectedGraphNode newNode = graph.createNode(text, null);
//                                            graph.createEdge(prevNode, newNode, propName, null);
                                        prevNode.label += "&#10;" + propName + "." + f.getName() + " = " + text;
                                    }
                                } else if (x instanceof Iterable it) {
                                    int index = 0;
                                    for (Object y : it) {
                                        if (y != null && !done.contains(y)) {
                                            stack.addLast(new Pair<Object, Object>(new Pair(propName + "[" + (index++) + "]", src), y));
                                        }
                                    }
                                } else {
                                    stack.addLast(new Pair(new Pair(propName + "." + f.getName(), src), x));
                                }
                            }
                        }
                    } catch (Throwable e) {
                    }
                }
                t = t.getSuperclass();
            }
        }

        public void traverseObjs(Object obj) {
            stack.addLast(new Pair<>(null, obj));
            while (!stack.isEmpty()) {
                Pair<Object, Object> p = stack.removeLast();
                this.expandObject(p.getFirst(), p.getSecond());
            }
        }
    }

}
