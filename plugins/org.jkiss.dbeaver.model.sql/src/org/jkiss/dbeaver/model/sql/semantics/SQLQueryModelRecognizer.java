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
import org.jkiss.dbeaver.Log;
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
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDummyDataSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.Pair;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

        if (tree == null) {
            return null;
        }
        this.queryDataContext = this.prepareDataContext(tree);
        STMTreeNode queryNode = tree.findFirstNonErrorChild();
        if (queryNode == null) {
            return null;
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
                if (e.isNotClassified() && (e.catalogName != null || e.schemaName != null ||
                    !tryFallbackForStringLiteral.test(e.entityName))
                ) {
                    e.entityName.getSymbol().setSymbolClass(SQLQuerySymbolClass.TABLE);
                    if (e.schemaName != null) {
                        e.schemaName.getSymbol().setSymbolClass(SQLQuerySymbolClass.SCHEMA);
                        if (e.catalogName != null) {
                            e.catalogName.getSymbol().setSymbolClass(SQLQuerySymbolClass.CATALOG);
                        }
                    }
                }
            },
            false
        );
        return new SQLQueryModel(tree, null, symbolEntries);
    }

    private void traverseForIdentifiers(
        @NotNull STMTreeNode root,
        @NotNull BiConsumer<SQLQueryQualifiedName, SQLQuerySymbolEntry> columnAction,
        @NotNull Consumer<SQLQueryQualifiedName> entityAction,
        boolean forceUnquotted
    ) {
        List<STMTreeNode> refs = STMUtils.expandSubtree(
            root,
            null,
            Set.of(STMKnownRuleNames.columnReference, STMKnownRuleNames.columnName, STMKnownRuleNames.tableName)
        );
        for (STMTreeNode ref : refs) {
            switch (ref.getNodeKindId()) {
                case SQLStandardParser.RULE_columnReference, SQLStandardParser.RULE_columnName -> {
                    STMTreeNode tableQualifierNode = ref.findFirstChildOfName(STMKnownRuleNames.qualifier);
                    SQLQueryQualifiedName tableName;
                    if (tableQualifierNode != null) {
                        tableName = this.collectTableName(tableQualifierNode, forceUnquotted);
                        if (tableName != null) {
                            entityAction.accept(tableName);
                        }
                    } else {
                        tableName = null;
                    }
                    STMTreeNode columnNameNode = ref.getNodeKindId() == SQLStandardParser.RULE_columnName
                        ? ref 
                        : ref.findFirstChildOfName(STMKnownRuleNames.columnName);
                    if (columnNameNode != null) {
                        SQLQuerySymbolEntry columnName = this.collectIdentifier(columnNameNode, forceUnquotted);
                        if (columnName != null) {
                            columnAction.accept(tableName, columnName);
                        }
                    }
                }
                case SQLStandardParser.RULE_tableName -> {
                    SQLQueryQualifiedName tableName = this.collectTableName(ref, forceUnquotted);
                    if (tableName != null) {
                        entityAction.accept(tableName);
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
            && this.executionContext.getDataSource().getSQLDialect() instanceof BasicSQLDialect
        ) {
            return new SQLQueryDataSourceContext(this.dialect, this.executionContext);
        } else {
            Set<String> allColumnNames = new HashSet<>();
            Set<List<String>> allTableNames = new HashSet<>();
            this.traverseForIdentifiers(
                root,
                (e, c) -> allColumnNames.add(c.getName()),
                e -> allTableNames.add(e.toListOfStrings()),
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
        STMKnownRuleNames.unqualifiedSchemaName,
        STMKnownRuleNames.catalogName,
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
        STMKnownRuleNames.qualifier,
        STMKnownRuleNames.nonjoinedTableReference,
        STMKnownRuleNames.explicitTable,
        STMKnownRuleNames.createTableStatement,
        STMKnownRuleNames.createViewStatement,
        STMKnownRuleNames.alterTableStatement,
        STMKnownRuleNames.dropTableStatement,
        STMKnownRuleNames.dropViewStatement,
        STMKnownRuleNames.deleteStatement,
        STMKnownRuleNames.insertStatement,
        STMKnownRuleNames.updateStatement,
        STMKnownRuleNames.correlationSpecification
    );

    private static final Set<String> actualTableNameContainers = Set.of(
        STMKnownRuleNames.tableName,
        STMKnownRuleNames.correlationName
    );

    @NotNull
    public SQLQueryRowsTableDataModel collectTableReference(@NotNull STMTreeNode node) {
        return new SQLQueryRowsTableDataModel(node, this.collectTableName(node));
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
                return nameEntry == null ? null : this.registerScopeItem(new SQLQueryQualifiedName(node, nameEntry));
            }
        }
    }

    private static final Set<String> qualifiedNameDirectWrapperNames = Set.of(
        STMKnownRuleNames.tableName,
        STMKnownRuleNames.constraintName
    );

    @Nullable
    public SQLQueryQualifiedName collectQualifiedName(@NotNull STMTreeNode node) {
        return this.collectQualifiedName(node, false);
    }

    @Nullable
    private SQLQueryQualifiedName collectQualifiedName(@NotNull STMTreeNode node, boolean forceUnquotted) { // qualifiedName
        STMTreeNode qualifiedNameNode = qualifiedNameDirectWrapperNames.contains(node.getNodeName())
            ? node.findFirstChildOfName(STMKnownRuleNames.qualifiedName)
            : node;
        if (qualifiedNameNode == null) {
            return null;
        } else if (!qualifiedNameNode.getNodeName().equals(STMKnownRuleNames.qualifiedName)) {
            log.debug("qualifiedName expected while facing with " + node.getNodeName());
            return null;
        }

        STMTreeNode entityNameNode = qualifiedNameNode.findLastChildOfName(STMKnownRuleNames.identifier);
        STMTreeNode schemaNameNode = qualifiedNameNode.findFirstChildOfName(STMKnownRuleNames.schemaName);
        if (entityNameNode == null) {
            return null;
        }

        // TODO consider partially valid qualified names to resolve and highlight name fragments (catalog and schema)

        SQLQuerySymbolEntry entityName = this.collectIdentifier(entityNameNode, forceUnquotted);
        if (entityName == null) {
            return null;
        } else if (schemaNameNode == null) {
            return this.registerScopeItem(new SQLQueryQualifiedName(qualifiedNameNode, entityName));
        }

        STMTreeNode unqualifiedSchemaNameNode = schemaNameNode.findLastChildOfName(STMKnownRuleNames.unqualifiedSchemaName);
        STMTreeNode catalogNameNode = schemaNameNode.findFirstChildOfName(STMKnownRuleNames.catalogName);
        if (unqualifiedSchemaNameNode == null) {
            return null;
        }

        SQLQuerySymbolEntry schemaName = this.collectIdentifier(unqualifiedSchemaNameNode, forceUnquotted);
        if (schemaName == null) {
            return null;
        } else if (catalogNameNode == null) {
            return this.registerScopeItem(new SQLQueryQualifiedName(qualifiedNameNode, schemaName, entityName));
        }

        SQLQuerySymbolEntry catalogName = this.collectIdentifier(catalogNameNode, forceUnquotted);
        if (catalogName == null) {
            return null;
        } else {
            return this.registerScopeItem(new SQLQueryQualifiedName(qualifiedNameNode, catalogName, schemaName, entityName));
        }
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
            case SQLStandardParser.RULE_valueReference -> this.collectValueReferenceExpression(node);
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
    private SQLQueryValueExpression collectValueReferenceExpression(@NotNull STMTreeNode node) {
        List<STMTreeNode> subnodes = node.findNonErrorChildren();
        SQLQueryValueExpression expr;
        if (subnodes.size() > 0) {
            STMTreeNode head = subnodes.get(0);
            expr = switch (head.getNodeKindId()) {
                case SQLStandardParser.RULE_columnReference -> {
                    STMTreeNode tableNameNode = head.findFirstNonErrorChild();
                    SQLQueryQualifiedName tableName = tableNameNode == null ? null : this.collectTableName(tableNameNode);
                    STMTreeNode nameNode = head.findFirstChildOfName(STMKnownRuleNames.columnName);
                    if (nameNode != null) {
                        SQLQuerySymbolEntry columnName = this.collectIdentifier(nameNode);
                        if (columnName != null) {
                            yield tableName == null
                                ? new SQLQueryValueColumnReferenceExpression(head, columnName)
                                : new SQLQueryValueColumnReferenceExpression(head, tableName, columnName);
                        } else {
                            yield null;
                        }
                    } else {
                        yield new SQLQueryValueTupleReferenceExpression(head, tableName != null ? tableName : new SQLQueryQualifiedName(
                            head, new SQLQuerySymbolEntry(head, SQLConstants.QUESTION, SQLConstants.QUESTION)
                        ));
                    }
                }
                case SQLStandardParser.RULE_valueRefNestedExpr -> {
                    STMTreeNode valueRefNode = head.findFirstChildOfName(STMKnownRuleNames.valueReference);
                    yield valueRefNode == null ? null : this.collectValueReferenceExpression(valueRefNode);
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
                        yield new SQLQueryValueIndexingExpression(range, node, expr, slicingSpec);
                    }
                    case SQLStandardParser.RULE_valueRefMemberStep -> {
                        i++;
                        STMTreeNode memberNameNode = step.findLastChildOfName(STMKnownRuleNames.identifier);
                        SQLQuerySymbolEntry memberName = memberNameNode == null ? null : this.collectIdentifier(memberNameNode);
                        yield new SQLQueryValueMemberExpression(range, node, expr, memberName);
                    }
                    default -> throw new UnsupportedOperationException(
                        "Value member expression expected while facing with " + node.getNodeName()
                    );
                };
            }
        }

        return expr != null ? expr : new SQLQueryValueFlattenedExpression(node, Collections.emptyList());
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
