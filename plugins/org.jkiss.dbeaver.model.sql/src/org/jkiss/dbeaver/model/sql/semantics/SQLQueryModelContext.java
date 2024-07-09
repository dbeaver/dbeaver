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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.dialect.LSMDialectRegistry;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDummyDataSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModelContent;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryObjectDropModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableDropModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryDeleteModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryInsertModel;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.SQLQueryUpdateModel;
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
public class SQLQueryModelContext {

    private final Set<SQLQuerySymbolEntry> symbolEntries = new HashSet<>();
    
    private final boolean isReadMetadataForSemanticAnalysis;

    private final DBCExecutionContext executionContext;
    
    private final Set<String> reservedWords;

    private final SQLSyntaxManager syntaxManager;
    private final SQLDialect dialect;
    
    private final LinkedList<SQLQueryLexicalScope> currentLexicalScopes = new LinkedList<>();

    private SQLQueryDataContext queryDataContext;

    public SQLQueryModelContext(@Nullable DBCExecutionContext executionContext, boolean isReadMetadataForSemanticAnalysis, @NotNull SQLSyntaxManager syntaxManager) {
        this.isReadMetadataForSemanticAnalysis = isReadMetadataForSemanticAnalysis;
        this.executionContext = executionContext;
        this.syntaxManager = syntaxManager;

        if (executionContext != null && executionContext.getDataSource() != null) {
            this.dialect = this.executionContext.getDataSource().getSQLDialect();
        } else {
            this.dialect = BasicSQLDialect.INSTANCE;
        }
        this.reservedWords = new HashSet<>(this.dialect.getReservedWords());
    }

    public SQLSyntaxManager getSyntaxManager() {
        return syntaxManager;
    }

    public SQLDialect getDialect() {
        return dialect;
    }

    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    public SQLQueryDataContext getQueryDataContext() {
        return queryDataContext;
    }

    /**
     * Provides the semantic model for the provided text
     */
    @Nullable
    public SQLQueryModel recognizeQuery(@NotNull String text, @NotNull DBRProgressMonitor monitor) {
        STMSource querySource = STMSource.fromString(text);
        LSMAnalyzer analyzer = LSMDialectRegistry.getInstance().getAnalyzerFactoryForDialect(this.dialect)
            .createAnalyzer(LSMAnalyzerParameters.forDialect(this.dialect, this.syntaxManager));
        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(querySource, new STMSkippingErrorListener());

        if (tree == null) {
            return null;
        }
        this.queryDataContext = this.prepareDataContext(tree);
        STMTreeNode queryNode = tree.getFirstStmChild();
        if (queryNode == null) {
            return null;
        }
        SQLQueryModelContent contents = switch (queryNode.getNodeKindId()) {
            case SQLStandardParser.RULE_directSqlDataStatement -> {
                STMTreeNode stmtBodyNode = queryNode.getLastStmtChild();
                // TODO collect CTE for insert-update-delete as well as recursive CTE
                yield switch (stmtBodyNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_deleteStatement ->
                            SQLQueryDeleteModel.createModel(this, stmtBodyNode);
                        case SQLStandardParser.RULE_insertStatement ->
                            SQLQueryInsertModel.createModel(this, stmtBodyNode);
                        case SQLStandardParser.RULE_updateStatement ->
                            SQLQueryUpdateModel.createModel(this, stmtBodyNode);
                        default -> this.collectQueryExpression(tree);
                    };
            }
            case SQLStandardParser.RULE_sqlSchemaStatement -> {
                STMTreeNode stmtBodyNode = queryNode.getFirstStmChild();
                yield switch (stmtBodyNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_createTableStatement -> null;
                        case SQLStandardParser.RULE_createViewStatement -> null;
                        case SQLStandardParser.RULE_dropTableStatement ->
                            SQLQueryTableDropModel.createModel(this, stmtBodyNode, false);
                        case SQLStandardParser.RULE_dropViewStatement ->
                            SQLQueryTableDropModel.createModel(this, stmtBodyNode, true);
                        case SQLStandardParser.RULE_dropProcedureStatement ->
                            SQLQueryObjectDropModel.createModel(this, stmtBodyNode, RelationalObjectType.TYPE_PROCEDURE);
                        default -> null;
                    };
            }
            default -> null;
        };

        if (contents != null) {
            SQLQueryModel model = new SQLQueryModel(tree, contents, symbolEntries);

            model.propagateContext(this.queryDataContext, new RecognitionContext(monitor));

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
                    SQLQueryQualifiedName tableName;
                    if (ref.getChildCount() > 1) {
                        tableName = this.collectTableName(ref.getFirstStmChild(), forceUnquotted);
                        if (tableName != null) {
                            entityAction.accept(tableName);
                        }
                    } else {
                        tableName = null;
                    }
                    STMTreeNode columnName = ref.getNodeKindId() == SQLStandardParser.RULE_columnName
                        ? ref 
                        : ref.findChildOfName(STMKnownRuleNames.columnName);
                    if (columnName != null) {
                        columnAction.accept(tableName, this.collectIdentifier(columnName, forceUnquotted));
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
        if (this.isReadMetadataForSemanticAnalysis
            && this.executionContext != null
            && this.executionContext.getDataSource() instanceof DBSObjectContainer
            && this.executionContext.getDataSource().getSQLDialect() instanceof BasicSQLDialect
        ) {
            return new SQLQueryDataSourceContext(this);
        } else {
            Set<String> allColumnNames = new HashSet<>();
            Set<List<String>> allTableNames = new HashSet<>();
            this.traverseForIdentifiers(
                root,
                (e, c) -> allColumnNames.add(c.getName()),
                e -> allTableNames.add(e.toListOfStrings()),
                true);
            symbolEntries.clear();
            return new SQLQueryDummyDataSourceContext(this, allColumnNames, allTableNames);
        }
    }

    private static class RecognitionContext implements SQLQueryRecognitionContext {
        private final DBRProgressMonitor monitor;

        public RecognitionContext(@NotNull DBRProgressMonitor monitor) {
            this.monitor = monitor;
        }

        @NotNull
        @Override
        public DBRProgressMonitor getMonitor() {
            return this.monitor;
        }

        @Override
        public void appendError(@NotNull SQLQuerySymbolEntry symbol, @NotNull String error, @NotNull DBException ex) {
            // System.out.println(symbol.getName() + ": " + error + ": " + ex.toString());
        }

        @Override
        public void appendError(@NotNull SQLQuerySymbolEntry symbol, @NotNull String error) {
            // System.out.println(symbol.getName() + ": " + error);
        }

        @Override
        public void appendError(@NotNull STMTreeNode treeNode, @NotNull String error) {
            // TODO generate problem markers
        }
    }

    @NotNull
    public SQLQueryRowsSourceModel collectQueryExpression(@NotNull STMTreeNode tree) {
        return new SQLQueryExpressionMapper(this).translate(tree);
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
                throw new UnsupportedOperationException("columnNameList (or its wrapper) expected while facing with " + node.getNodeName());
            }
            
            List<STMTreeNode> actual = STMUtils.expandSubtree(node, columnNameListWrapperNames, Set.of(STMKnownRuleNames.columnNameList));
            switch (actual.size()) {
                case 0 -> {
                    return Collections.emptyList();
                }
                case 1 -> {
                    node = actual.get(0);
                }
                default -> throw new UnsupportedOperationException("Ambiguous columnNameList collection at " + node.getNodeName());
            }
        }
        
        List<SQLQuerySymbolEntry> result = new ArrayList<>(node.getChildCount());
        for (int i = 0; i < node.getChildCount(); i += 2) {
            result.add(collectIdentifier(node.getStmChild(i)));
        }
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
    
    @NotNull
    public SQLQuerySymbolEntry collectIdentifier(@NotNull STMTreeNode node) {
        return collectIdentifier(node, false);
    }
    
    @NotNull
    private SQLQuerySymbolEntry collectIdentifier(@NotNull STMTreeNode node, boolean forceUnquotted) {
        // TODO refactor out all recognition-related exceptions, consider error node everywhere in parse tree and don't introduce unnecessary model nodes
        STMTreeNode actual = identifierDirectWrapperNames.contains(node.getNodeName()) ? node.getFirstStmChild() : node;
        if (!actual.getNodeName().equals(STMKnownRuleNames.identifier)) {
            throw new UnsupportedOperationException("identifier expected while facing with " + node.getNodeName());
        }
        STMTreeNode actualIdentifier = actual.findChildOfName(STMKnownRuleNames.actualIdentifier);
        if (actualIdentifier == null) {
            SQLQuerySymbolEntry entry = this.registerSymbolEntry(actual, actual.getTextContent(), actual.getTextContent());
            entry.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
            return entry;
        } else {
            STMTreeNode actualBody = actualIdentifier.getFirstStmChild();
            String rawIdentifierString = actualBody.getTextContent();
            if (actualBody.getPayload() instanceof Token t && t.getType() == SQLStandardLexer.Quotted) {
                SQLQuerySymbolEntry entry = this.registerSymbolEntry(actualBody, rawIdentifierString, rawIdentifierString);
                // not canonicalizing the identifier because it is quoted,
                // but the QUOTED class will be assigned later after db entity resolution fail
                // entry.getSymbol().setSymbolClass(SQLQuerySymbolClass.QUOTED);
                return entry;
            } else if (this.reservedWords.contains(rawIdentifierString.toUpperCase())) { // keywords are uppercased in dialect
                SQLQuerySymbolEntry entry = this.registerSymbolEntry(actualBody, rawIdentifierString, rawIdentifierString);
                entry.getSymbol().setSymbolClass(SQLQuerySymbolClass.RESERVED);
                return entry;
            } else {
                String actualIdentifierString = SQLUtils.identifierToCanonicalForm(dialect, rawIdentifierString, forceUnquotted, false);
                return this.registerSymbolEntry(actualBody, actualIdentifierString, rawIdentifierString);
            }
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
        return new SQLQueryRowsTableDataModel(this, node, collectTableName(node));
    }

    @Nullable
    private SQLQueryQualifiedName collectTableName(@NotNull STMTreeNode node) {
        return this.collectTableName(node, false);
    }

    @Nullable
    private SQLQueryQualifiedName collectTableName(@NotNull STMTreeNode node, boolean forceUnquotted) {
        List<STMTreeNode> actual = STMUtils.expandSubtree(node, tableNameContainers, actualTableNameContainers);
        return switch (actual.size()) {
            case 0 -> null;
            case 1 -> {
                node = actual.get(0);
                yield node.getNodeName().equals(STMKnownRuleNames.tableName) ? collectQualifiedName(node, forceUnquotted)
                        : this.registerScopeItem(new SQLQueryQualifiedName(node, collectIdentifier(node, forceUnquotted)));
            }
            default -> throw new UnsupportedOperationException("Ambiguous tableName collection at " + node.getNodeName());
        };
    }
    
    private static final Set<String> qualifiedNameDirectWrapperNames = Set.of(
        STMKnownRuleNames.tableName,
        STMKnownRuleNames.constraintName
    );
    
    @NotNull
    private SQLQueryQualifiedName collectQualifiedName(@NotNull STMTreeNode node, boolean forceUnquotted) { // qualifiedName
        STMTreeNode entityNameNode = qualifiedNameDirectWrapperNames.contains(node.getNodeName()) ? node.getFirstStmChild() : node;
        if (!entityNameNode.getNodeName().equals(STMKnownRuleNames.qualifiedName)) {
            throw new UnsupportedOperationException("identifier expected while facing with " + node.getNodeName());
        }
        
        SQLQuerySymbolEntry entityName = collectIdentifier(entityNameNode.getLastStmtChild(), forceUnquotted);
        if (entityNameNode.getChildCount() == 1) {
            return this.registerScopeItem(new SQLQueryQualifiedName(entityNameNode, entityName));
        } else {
            STMTreeNode schemaNameNode = entityNameNode.getFirstStmChild();
            SQLQuerySymbolEntry schemaName = collectIdentifier(
                schemaNameNode.getLastStmtChild(),
                forceUnquotted
            );
            if (schemaNameNode.getChildCount() == 1) {
                return this.registerScopeItem(new SQLQueryQualifiedName(entityNameNode, schemaName, entityName));
            } else {
                STMTreeNode catalogNameNode = schemaNameNode.getFirstStmChild();
                SQLQuerySymbolEntry catalogName = collectIdentifier(
                    catalogNameNode.getLastStmtChild(),
                    forceUnquotted
                );
                return this.registerScopeItem(new SQLQueryQualifiedName(entityNameNode, catalogName, schemaName, entityName));
            }    
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
        STMKnownRuleNames.rowValueConstructor
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
            throw new UnsupportedOperationException(
                "Search condition or value expression expected while facing with " + node.getNodeName()
            );
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
                        while (rn.getChildCount() == 1 && !knownRecognizableValueExpressionNames.contains(rn.getNodeName())) {
                            rn = rn.getFirstStmChild();
                        }
                        if (knownRecognizableValueExpressionNames.contains(rn.getNodeName())
                            || rn.getNodeName().equals(STMKnownRuleNames.valueExpressionPrimary)
                        ) {
                            childLists.peek().add(collectKnownValueExpression(rn));
                        } else {
                            stack.push(n);
                            stack.push(null);
                            childLists.push(new ArrayList<>(rn.getChildCount()));
                            for (int i = rn.getChildCount() - 1; i >= 0; i--) {
                                stack.push(rn.getStmChild(i));
                            }
                        }
                    } else {
                        // TODO register unexpected pieces in the lexical scope
                        STMTreeNode content = stack.pop();
                        List<SQLQueryValueExpression> children = childLists.pop();
                        if (!children.isEmpty()) {
                            SQLQueryValueExpression e = children.size() == 1 && children.get(0) instanceof SQLQueryValueFlattenedExpression c 
                                ? c 
                                : new SQLQueryValueFlattenedExpression(content, children);
                            childLists.peek().add(e);
                        }
                    }
                }
                
                List<SQLQueryValueExpression> roots = childLists.pop();
                SQLQueryValueExpression result = roots.isEmpty() ?
                    new SQLQueryValueFlattenedExpression(node, Collections.emptyList()) :
                    roots.get(0);
                
                result.registerLexicalScope(sh.lexicalScope);
                return result;
            }
        }
    }

    @NotNull
    public SQLQueryValueExpression collectKnownValueExpression(@NotNull STMTreeNode node) {
        return switch (node.getNodeKindId()) {
            case SQLStandardParser.RULE_subquery -> new SQLQueryValueSubqueryExpression(node, this.collectQueryExpression(node));
            case SQLStandardParser.RULE_valueReference -> this.collectValueReferenceExpression(node);
            case SQLStandardParser.RULE_valueExpressionPrimary -> {
                SQLQueryValueExpression subexpr = this.collectValueExpression(node.getFirstStmChild());
                STMTreeNode castSpecNode = node.findChildOfName(STMKnownRuleNames.valueExpressionCastSpec);
                if (castSpecNode != null) {
                    String typeName = castSpecNode.getStmChild(1).getTextContent();
                    yield new SQLQueryValueTypeCastExpression(node, subexpr, typeName);
                } else {
                    yield subexpr;
                }
            }
            case SQLStandardParser.RULE_variableExpression -> {
                STMTreeNode varExprNode = node.getFirstStmChild();
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
                        default -> throw new UnsupportedOperationException("Unsupported term variable expression: " + node.getTextContent());
                    };
                } else {
                    yield switch (varExprNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_namedParameter ->  {
                            yield new SQLQueryValueVariableExpression(
                                node,
                                this.registerSymbolEntry(node, varExprNode.getStmChild(1).getTextContent(), varExprNode.getTextContent()),
                                SQLQueryValueVariableExpression.VariableExpressionKind.CLIENT_PARAMETER,
                                varExprNode.getTextContent()
                            );
                        }
                        case SQLStandardParser.RULE_anonymouseParameter -> {
                            String mark = varExprNode.getFirstStmChild().getTextContent();
                            this.registerSymbolEntry(node, mark, mark);
                            yield new SQLQueryValueVariableExpression(
                                node,
                                null,
                                SQLQueryValueVariableExpression.VariableExpressionKind.CLIENT_PARAMETER,
                                varExprNode.getTextContent()
                            );
                        }
                        default -> throw new UnsupportedOperationException("Unsupported variable expression: " + node.getTextContent());
                    };
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
    }

    @NotNull
    private SQLQueryValueExpression makeValueConstantExpression(@NotNull STMTreeNode node, @NotNull SQLQueryExprType type) {
        return new SQLQueryValueConstantExpression(node, node.getTextContent(), type);
    }

    @NotNull
    private SQLQueryValueExpression collectValueReferenceExpression(@NotNull STMTreeNode node) {
        STMTreeNode head = node.getFirstStmChild();
        SQLQueryValueExpression expr = switch (head.getNodeKindId()) {
            case SQLStandardParser.RULE_columnReference -> {
                SQLQueryQualifiedName tableName = collectTableName(head.getFirstStmChild());
                STMTreeNode nameNode = head.findChildOfName(STMKnownRuleNames.columnName);
                if (nameNode != null) {
                    SQLQuerySymbolEntry columnName = collectIdentifier(nameNode);
                    yield head.getChildCount() == 1 ? new SQLQueryValueColumnReferenceExpression(head, columnName)
                      : new SQLQueryValueColumnReferenceExpression(head, tableName, columnName);
                } else {
                    yield new SQLQueryValueTupleReferenceExpression(head, tableName);
                }
            }
            case SQLStandardParser.RULE_valueRefNestedExpr -> this.collectValueReferenceExpression(head.getStmChild(1));
            default -> throw new UnsupportedOperationException(
                "Value reference expression expected while facing with " + head.getNodeName()
            );
        };
        
        int rangeStart = node.getRealInterval().a;
        boolean[] slicingFlags = new boolean[node.getChildCount()];
        for (int i = 1; i < node.getChildCount();) {
            STMTreeNode step = node.getStmChild(i);
            Interval range = new Interval(rangeStart, step.getRealInterval().b);
            expr = switch (step.getNodeKindId()) {
                case SQLStandardParser.RULE_valueRefIndexingStep -> {
                    int s = i;
                    for (; i < node.getChildCount() && step.getNodeKindId() == SQLStandardParser.RULE_valueRefIndexingStep; i++) {
                        step = node.getStmChild(i);
                        slicingFlags[i] = step.getStmChild(1).getNodeKindId() == SQLStandardParser.RULE_valueRefIndexingStepSlice;
                    }
                    boolean[] slicingSpec = Arrays.copyOfRange(slicingFlags, s, i);
                    yield new SQLQueryValueIndexingExpression(range, node, expr, slicingSpec);
                }
                case SQLStandardParser.RULE_valueRefMemberStep -> {
                    i++;
                    yield new SQLQueryValueMemberExpression(range, node, expr, this.collectIdentifier(step.getStmChild(1)));
                }
                default -> throw new UnsupportedOperationException(
                    "Value member expression expected while facing with " + node.getNodeName()
                );
            };
        }
        
        return expr;
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
            SQLQueryModelContext.this.endScope(this.lexicalScope);
        }
    }

    public LexicalScopeHolder openScope() {
        return new LexicalScopeHolder(this.beginScope());
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
