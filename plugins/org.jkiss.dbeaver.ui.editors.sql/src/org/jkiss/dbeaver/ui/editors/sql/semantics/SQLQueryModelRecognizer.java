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
package org.jkiss.dbeaver.ui.editors.sql.semantics;


import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.sql.dialect.LSMDialectRegistry;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.stm.*;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataSourceContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDummyDataSourceContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.*;
import org.jkiss.utils.Pair;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public class SQLQueryModelRecognizer {

    
    private final HashSet<SQLQuerySymbolEntry> symbolEntries = new HashSet<>();
    
    private final boolean isReadMetadataForSemanticAnalysis;

    private final DBCExecutionContext executionContext;
    
    private final Set<String> reservedWords; 

    private SQLQueryDataContext queryDataContext;
    
    @FunctionalInterface
    private interface TreeMapperCallback<T, C> {
        T apply(STMTreeNode node, List<T> children, C context);
    }
    
    private static class TreeMapper<T, C> {
        private interface MapperFrame {
            void doWork();
        }    
        
        private interface MapperResultFrame<T> extends MapperFrame {
            void aggregate(T result);
        }
            
        private abstract class MapperNodeFrame implements MapperFrame {
            public final STMTreeNode node;
            public final MapperResultFrame<T> parent;
            
            public MapperNodeFrame(@NotNull STMTreeNode node, @NotNull MapperResultFrame<T> parent) {
                this.node = node;
                this.parent = parent;
            }
        }
        
        private class MapperQueuedNodeFrame extends MapperNodeFrame {

            public MapperQueuedNodeFrame(@NotNull STMTreeNode node, @NotNull MapperResultFrame<T> parent) {
                super(node, parent);
            }
            
            @Override
            public void doWork() {
                TreeMapperCallback<T, C> translation = translations.get(node.getNodeName());
                MapperResultFrame<T> aggregator = translation == null ? parent : new MapperDataPendingNodeFrame(node, parent, translation);
                
                if (translation != null) {
                    stack.push(aggregator);
                }
                for (int i = node.getChildCount() - 1; i >= 0; i--) {
                    if (transparentNodeNames.contains(node.getNodeName())) {
                        stack.push(new MapperQueuedNodeFrame((STMTreeNode) node.getChild(i), aggregator));
                    }
                }
            }
        }
        
        private class MapperDataPendingNodeFrame extends MapperNodeFrame implements MapperResultFrame<T> {
            public final List<T> childrenData = new LinkedList<>();
            public final TreeMapperCallback<T, C> translation;
            
            public MapperDataPendingNodeFrame(
                @NotNull STMTreeNode node,
                @NotNull MapperResultFrame<T> parent,
                @NotNull TreeMapperCallback<T, C> translation
            ) {
                super(node, parent);
                this.translation = translation;
            }
            
            @Override
            public void aggregate(@NotNull T result) {
                this.childrenData.add(result);
            }
            
            @Override
            public void doWork() {
                this.parent.aggregate(this.translation.apply(this.node, this.childrenData, TreeMapper.this.context));
            }
        }
        
        private class MapperRootFrame implements MapperResultFrame<T> {
            public final STMTreeNode node;
            public T result = null;
            
            public MapperRootFrame(@NotNull STMTreeNode node) {
                this.node = node;
            }

            @Override
            public void aggregate(@NotNull T result) {
                this.result = result;
            }
            
            @Override
            public void doWork() {
                stack.push(new MapperQueuedNodeFrame(node, this));
            }
        }
        
        private final Class<T> mappingResultType;
        private final Set<String> transparentNodeNames;
        private final Map<String, TreeMapperCallback<T, C>> translations;
        private final Stack<MapperFrame> stack = new Stack<>();
        private final C context;
        
        public TreeMapper(
            @NotNull Class<T> mappingResultType,
            @NotNull Set<String> transparentNodeNames,
            @NotNull Map<String, TreeMapperCallback<T, C>> translations,
            @NotNull C context
        ) {
            this.mappingResultType = mappingResultType;
            this.transparentNodeNames = transparentNodeNames;
            this.translations = translations;
            this.context = context;
        }

        public T translate(@NotNull STMTreeNode root) {
            MapperRootFrame rootFrame = new MapperRootFrame(root);
            stack.push(rootFrame);
            while (stack.size() > 0) {
                stack.pop().doWork();
            }
            return rootFrame.result;
        }
    }
    
    private static class QueryExpressionMapper extends TreeMapper<SQLQueryRowsSourceModel, SQLQueryModelRecognizer> {
        private static final Set<String> queryExpressionSubtreeNodeNames = Set.of(
            STMKnownRuleNames.sqlQuery,
            STMKnownRuleNames.directSqlDataStatement,
            STMKnownRuleNames.selectStatement,
            // STMKnownRuleNames.withClause, // TODO
                
            STMKnownRuleNames.subquery,
            STMKnownRuleNames.unionTerm,
            STMKnownRuleNames.exceptTerm,
            STMKnownRuleNames.nonJoinQueryExpression,
            STMKnownRuleNames.nonJoinQueryTerm,
            STMKnownRuleNames.intersectTerm,
            STMKnownRuleNames.nonJoinQueryPrimary,
            STMKnownRuleNames.simpleTable,
            STMKnownRuleNames.querySpecification,
            STMKnownRuleNames.tableExpression,
            STMKnownRuleNames.queryPrimary,
            STMKnownRuleNames.queryTerm,
            STMKnownRuleNames.queryExpression,
            STMKnownRuleNames.selectStatementSingleRow,
            STMKnownRuleNames.fromClause,
            STMKnownRuleNames.nonjoinedTableReference,
            STMKnownRuleNames.tableReference,
            STMKnownRuleNames.joinedTable,
            STMKnownRuleNames.derivedTable,
            STMKnownRuleNames.tableSubquery,
            STMKnownRuleNames.crossJoinTerm,
            STMKnownRuleNames.naturalJoinTerm,
            STMKnownRuleNames.explicitTable
        );
        
        private static final Map<String, TreeMapperCallback<SQLQueryRowsSourceModel, SQLQueryModelRecognizer>> translations = Map.ofEntries(
            Map.entry(STMKnownRuleNames.queryExpression, (n, cc, r) -> {
                if (cc.isEmpty()) {
                    return r.queryDataContext.getDefaultTable(n.getRealInterval());
                } else {
                    SQLQueryRowsSourceModel source = cc.get(0);
                    for (int i = 1; i < cc.size(); i++) {
                        STMTreeNode childNode = n.getStmChild(i);
                        List<SQLQuerySymbolEntry> corresponding = r.collectColumnNameList(childNode);
                        SQLQueryRowsSourceModel nextSource = cc.get(i);
                        Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                        SQLQueryRowsSetCorrespondingOperationKind opKind = switch (childNode.getNodeKindId()) {
                            case SQLStandardParser.RULE_exceptTerm -> SQLQueryRowsSetCorrespondingOperationKind.EXCEPT;
                            case SQLStandardParser.RULE_unionTerm -> SQLQueryRowsSetCorrespondingOperationKind.UNION;
                            default -> throw new UnsupportedOperationException("Unexpected child node kind at queryExpression");
                        };
                        source = new SQLQueryRowsSetCorrespondingOperationModel(range, source, nextSource, corresponding, opKind); 
                    }
                    return source;
                }
            }),
            Map.entry(STMKnownRuleNames.nonJoinQueryTerm, (n, cc, r) -> {
                if (cc.isEmpty()) {
                    return r.queryDataContext.getDefaultTable(n.getRealInterval());
                } else {
                    SQLQueryRowsSourceModel source = cc.get(0);
                    for (int i = 1; i < cc.size(); i++) {
                        STMTreeNode childNode = n.getStmChild(i);
                        List<SQLQuerySymbolEntry> corresponding = r.collectColumnNameList(childNode);
                        SQLQueryRowsSourceModel nextSource = cc.get(i);
                        Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                        SQLQueryRowsSetCorrespondingOperationKind opKind = switch (childNode.getNodeKindId()) {
                            case SQLStandardParser.RULE_intersectTerm -> SQLQueryRowsSetCorrespondingOperationKind.INTERSECT;
                            default -> throw new UnsupportedOperationException("Unexpected child node kind at nonJoinQueryTerm");
                        };
                        source = new SQLQueryRowsSetCorrespondingOperationModel(range, source, nextSource, corresponding, opKind); 
                    }
                    return source;
                }
            }),
            Map.entry(STMKnownRuleNames.joinedTable, (n, cc, r) -> {
                // joinedTable: (nonjoinedTableReference|(LeftParen joinedTable RightParen)) (naturalJoinTerm|crossJoinTerm)+;
                if (cc.isEmpty()) {
                    return r.queryDataContext.getDefaultTable(n.getRealInterval());
                } else {
                    SQLQueryRowsSourceModel source = cc.get(0);
                    for (int i = 1; i < cc.size(); i++) {
                        final SQLQueryRowsSourceModel currSource = source;
                        final SQLQueryRowsSourceModel nextSource = cc.get(i);
                        // TODO see second case of the first source if parens are correctly ignored here
                        STMTreeNode childNode = n.getStmChild(i);
                        Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                        source = switch (childNode.getNodeKindId()) {
                            case SQLStandardParser.RULE_naturalJoinTerm ->
                                Optional.ofNullable(childNode.findChildOfName(STMKnownRuleNames.joinSpecification))
                                    .map(cn -> cn.findChildOfName(STMKnownRuleNames.joinCondition))
                                    .map(cn -> cn.findChildOfName(STMKnownRuleNames.searchCondition))
                                    .map(r::collectValueExpression)
                                    .map(e -> new SQLQueryRowsNaturalJoinModel(range, currSource, nextSource, e))
                                    .orElseGet(() -> new SQLQueryRowsNaturalJoinModel(range, currSource, nextSource, r.collectColumnNameList(childNode)));
                            case SQLStandardParser.RULE_crossJoinTerm -> new SQLQueryRowsCrossJoinModel(range, currSource, nextSource);
                            default -> throw new UnsupportedOperationException("Unexpected child node kind at queryExpression");
                        };
                    }
                    return source;
                }
            }),
            Map.entry(STMKnownRuleNames.fromClause, (n, cc, r) -> {
                if (cc.isEmpty()) {
                    return r.queryDataContext.getDefaultTable(n.getRealInterval());
                } else {
                    SQLQueryRowsSourceModel source = cc.get(0);
                    for (int i = 1; i < cc.size(); i++) {
                        STMTreeNode childNode = n.getStmChild(1 + i * 2);
                        SQLQueryRowsSourceModel nextSource = cc.get(i);
                        Interval range = Interval.of(n.getRealInterval().a, childNode.getRealInterval().b);
                        source = switch (childNode.getNodeKindId()) {
                            case SQLStandardParser.RULE_tableReference -> new SQLQueryRowsCrossJoinModel(range, source, nextSource);
                            default -> throw new UnsupportedOperationException("Unexpected child node kind at fromClause");
                        };
                    }
                    return source;
                }
            }),
            Map.entry(STMKnownRuleNames.querySpecification, (n, cc, r) -> {
                STMTreeNode selectListNode = n.findChildOfName(STMKnownRuleNames.selectList);
                SQLQuerySelectionResultModel resultModel = new SQLQuerySelectionResultModel(
                    selectListNode.getRealInterval(),
                    (selectListNode.getChildCount() + 1) / 2
                );
                for (int i = 0; i < selectListNode.getChildCount(); i += 2) {
                    STMTreeNode selectSublist = selectListNode.getStmChild(i);
                    if (selectSublist.getChildCount() > 0) {
                        STMTreeNode sublistNode = selectSublist.getStmChild(0);
                        if (sublistNode != null) {
                            Interval range = sublistNode.getRealInterval();
                            switch (sublistNode.getNodeKindId()) { // selectSublist: (Asterisk|derivedColumn|qualifier Period Asterisk
                                case SQLStandardParser.RULE_derivedColumn -> {
                                    // derivedColumn: valueExpression (asClause)?; asClause: (AS)? columnName;
                                    SQLQueryValueExpression expr = r.collectValueExpression(sublistNode.getStmChild(0));
                                    if (sublistNode.getChildCount() > 1) {
                                        STMTreeNode asClause = sublistNode.getStmChild(1);
                                        SQLQuerySymbolEntry asColumnName = r.collectIdentifier(
                                            asClause.getStmChild(asClause.getChildCount() - 1)
                                        );
                                        resultModel.addColumnSpec(range, expr, asColumnName);
                                    } else {
                                        resultModel.addColumnSpec(range, expr);
                                    }
                                }
                                case SQLStandardParser.RULE_qualifier -> {
                                    resultModel.addTupleSpec(range, r.collectTableName(sublistNode));
                                }
                                case SQLStandardParser.RULE_anyUnexpected -> {
                                    // error in query text, ignoring it
                                }
                                default -> {
                                    resultModel.addCompleteTupleSpec(range);
                                }
                            }
                        }
                    }
                }
                SQLQueryRowsSourceModel source = cc.isEmpty() ? r.queryDataContext.getDefaultTable(n.getRealInterval()) : cc.get(0);
                STMTreeNode tableExpr = n.findChildOfName(STMKnownRuleNames.tableExpression);
                if (tableExpr != null) {
                    // tableExpression: fromClause whereClause? groupByClause? havingClause? orderByClause? limitClause?;
                    SQLQueryValueExpression whereExpr = Optional.ofNullable(tableExpr.findChildOfName(STMKnownRuleNames.whereClause))
                        .map(r::collectValueExpression).orElse(null);
                    SQLQueryValueExpression havingClause = Optional.ofNullable(tableExpr.findChildOfName(STMKnownRuleNames.havingClause))
                        .map(r::collectValueExpression).orElse(null);
                    SQLQueryValueExpression groupByClause = Optional.ofNullable(tableExpr.findChildOfName(STMKnownRuleNames.groupByClause))
                        .map(r::collectValueExpression).orElse(null);
                    SQLQueryValueExpression orderByClause = Optional.ofNullable(tableExpr.findChildOfName(STMKnownRuleNames.orderByClause))
                        .map(r::collectValueExpression).orElse(null);
                    return new SQLQueryRowsProjectionModel(n.getRealInterval(), source, resultModel, whereExpr, havingClause, groupByClause, orderByClause);
                } else {
                    return new SQLQueryRowsProjectionModel(n.getRealInterval(), source, resultModel);
                }
            }),
            Map.entry(STMKnownRuleNames.nonjoinedTableReference, (n, cc, r) -> {
                // can they both be missing?
                SQLQueryRowsSourceModel source;
                if (cc.isEmpty()) {
                    STMTreeNode tableNameNode = n.findChildOfName(STMKnownRuleNames.tableName);
                    if (tableNameNode != null) {
                        source = r.collectTableReference(tableNameNode);
                    } else {
                        source = r.queryDataContext.getDefaultTable(n.getRealInterval());
                    }
                } else {
                    source = cc.get(0);
                }
                // TODO column reference at PARTITION clause
                if (n.getChildCount() > 1) {
                    STMTreeNode lastSubnode = n.getStmChild(n.getChildCount() - 1);
                    if (lastSubnode.getNodeName().equals(STMKnownRuleNames.correlationSpecification)) {
                        SQLQuerySymbolEntry correlationName = r.collectIdentifier(
                            lastSubnode.getStmChild(lastSubnode.getChildCount() == 1 || lastSubnode.getChildCount() == 4 ? 0 : 1)
                        ); 
                        source = new SQLQueryRowsCorrelatedSourceModel(
                            n.getRealInterval(), source, correlationName, r.collectColumnNameList(lastSubnode), !cc.isEmpty()
                        );
                    }
                }
                return source;
            }),
            Map.entry(STMKnownRuleNames.explicitTable, (n, cc, r) -> r.collectTableReference(n)),
            Map.entry(STMKnownRuleNames.tableValueConstructor, (n, cc, r) -> {
                return new SQLQueryRowsTableValueModel(n.getRealInterval()); // TODO
            })
        );

        public QueryExpressionMapper(SQLQueryModelRecognizer recognizer) {
            super(SQLQueryRowsSourceModel.class, queryExpressionSubtreeNodeNames, translations, recognizer);
        }
    }

    public SQLQueryModelRecognizer(@Nullable DBCExecutionContext executionContext, boolean isReadMetadataForSemanticAnalysis) {
        this.isReadMetadataForSemanticAnalysis = isReadMetadataForSemanticAnalysis;
        this.executionContext = executionContext;
        this.reservedWords = new HashSet<>(this.obtainSqlDialect().getReservedWords());
    }
    
    private void traverseForIdentifiers(
        @NotNull STMTreeNode root,
        @NotNull Consumer<SQLQuerySymbolEntry> columnAction,
        @NotNull Consumer<SQLQueryQualifiedName> entityAction,
        boolean forceUnquotted
    ) {
        List<STMTreeNode> refs = STMUtils.expandSubtree(root, null, Set.of(STMKnownRuleNames.columnReference, STMKnownRuleNames.columnName, STMKnownRuleNames.tableName));
        for (STMTreeNode ref : refs) {
            switch (ref.getNodeKindId()) {
                case SQLStandardParser.RULE_columnReference, SQLStandardParser.RULE_columnName -> {
                    if (ref.getChildCount() > 1) {
                        SQLQueryQualifiedName tableName = this.collectTableName(ref.getStmChild(0), forceUnquotted);
                        if (tableName != null) {
                            entityAction.accept(tableName);
                        }
                    }
                    columnAction.accept(this.collectIdentifier(ref.getStmChild(ref.getChildCount() - 1), forceUnquotted));
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
            return new SQLQueryDataSourceContext(this.executionContext, this.executionContext.getDataSource().getSQLDialect());
        } else {
            Set<String> allColumnNames = new HashSet<>();
            Set<List<String>> allTableNames = new HashSet<>();
            this.traverseForIdentifiers(root, c -> allColumnNames.add(c.getName()), e -> allTableNames.add(e.toListOfStrings()), true);
            symbolEntries.clear();
            return new SQLQueryDummyDataSourceContext(this.obtainSqlDialect(), allColumnNames, allTableNames);
        }
    }

    @NotNull
    private SQLDialect obtainSqlDialect() {
        if (this.executionContext != null && this.executionContext.getDataSource() != null) {
            return this.executionContext.getDataSource().getSQLDialect();
        } else {
            return BasicSQLDialect.INSTANCE;
        }
    }


    /**
     * A debugging facility
     */

    private final SQLQueryRecognitionContext recognitionContext = new SQLQueryRecognitionContext() {

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
            // TODO Auto-generated method stub

        }
    };

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
            stack.addLast(new Pair(null, obj));
            while (stack.size() > 0) {
                Pair p = stack.removeLast();
                this.expandObject(p.getFirst(), p.getSecond());
            }
        }
    }

    @Nullable
    public SQLQuerySelectionModel recognizeQuery(@NotNull String text) {
        STMSource querySource = STMSource.fromString(text);
        LSMAnalyzer analyzer = LSMDialectRegistry.getInstance().getAnalyzerForDialect(this.obtainSqlDialect());
        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(querySource, new STMSkippingErrorListener());
        
        if (tree != null) {
            this.queryDataContext = this.prepareDataContext(tree);

            SQLQueryRowsSourceModel source = this.collectQueryExpression(tree);
            if (source != null) {
                SQLQuerySelectionModel model = new SQLQuerySelectionModel(tree.getRealInterval(), source, symbolEntries);

                model.propagateContex(this.queryDataContext, recognitionContext);

                // var tt = new DebugGraphBuilder();
                // tt.traverseObjs(model);
                // tt.graph.saveToFile("c:/temp/outx.dgml");

                return model;
            } else {
                // TODO log query model collection error
            }

            this.traverseForIdentifiers(tree, 
                c -> { 
                    if (c.isNotClassified()) {
                        c.getSymbol().setSymbolClass(SQLQuerySymbolClass.COLUMN);
                    }
                }, 
                e -> {
                    if (e.isNotClassified()) {
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
            return new SQLQuerySelectionModel(tree.getRealInterval(), null, symbolEntries);
        } else {
            return null;
        }
    }

    @Nullable
    private SQLQueryRowsSourceModel collectQueryExpression(@NotNull STMTreeNode tree) {
        QueryExpressionMapper queryMapper = new QueryExpressionMapper(this);
        return queryMapper.translate(tree);
    }
    
    
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
        STMKnownRuleNames.viewDefinition,
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
    private List<SQLQuerySymbolEntry> collectColumnNameList(@NotNull STMTreeNode node) {
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
        STMKnownRuleNames.columnName
    );
    
    @NotNull
    private SQLQuerySymbolEntry collectIdentifier(@NotNull STMTreeNode node) {
        return collectIdentifier(node, false);
    }
    
    @NotNull
    private SQLQuerySymbolEntry collectIdentifier(@NotNull STMTreeNode node, boolean forceUnquotted) {
        STMTreeNode actual = identifierDirectWrapperNames.contains(node.getNodeName()) ? node.getStmChild(0) : node;
        if (!actual.getNodeName().equals(STMKnownRuleNames.identifier)) {
            throw new UnsupportedOperationException("identifier expected while facing with " + node.getNodeName());
        }
        STMTreeNode actualBody = actual.findChildOfName(STMKnownRuleNames.actualIdentifier).getStmChild(0);
        String rawIdentifierString = actualBody.getTextContent();
        if (actualBody.getPayload() instanceof Token t && t.getType() == SQLStandardLexer.Quotted) {
            SQLQuerySymbolEntry entry = new SQLQuerySymbolEntry(actualBody.getRealInterval(), rawIdentifierString, rawIdentifierString);
            this.symbolEntries.add(entry);
            entry.getSymbol().setSymbolClass(SQLQuerySymbolClass.QUOTED);
            return entry;
        } else if (this.reservedWords.contains(rawIdentifierString)) {
            SQLQuerySymbolEntry entry = new SQLQuerySymbolEntry(actualBody.getRealInterval(), rawIdentifierString, rawIdentifierString);
            this.symbolEntries.add(entry);
            entry.getSymbol().setSymbolClass(SQLQuerySymbolClass.RESERVED);
            return entry;
        } else {
            SQLDialect dialect = this.obtainSqlDialect();
            String actualIdentifierString = SQLUtils.identifierToCanonicalForm(dialect, rawIdentifierString, forceUnquotted, false);
            SQLQuerySymbolEntry entry = new SQLQuerySymbolEntry(actualBody.getRealInterval(), actualIdentifierString, rawIdentifierString);
            this.symbolEntries.add(entry);
            return entry;
        }
    }
    
    private static final Set<String> tableNameContainers = Set.of(
        STMKnownRuleNames.referencedTableAndColumns,
        STMKnownRuleNames.qualifier,
        STMKnownRuleNames.nonjoinedTableReference,
        STMKnownRuleNames.explicitTable,
        STMKnownRuleNames.tableDefinition,
        STMKnownRuleNames.viewDefinition,
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
    private SQLQueryRowsTableDataModel collectTableReference(@NotNull STMTreeNode node) {
        return new SQLQueryRowsTableDataModel(node.getRealInterval(), collectTableName(node));
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
                        : new SQLQueryQualifiedName(collectIdentifier(node, forceUnquotted));
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
        STMTreeNode entityNameNode = qualifiedNameDirectWrapperNames.contains(node.getNodeName()) ? node.getStmChild(0) : node;
        if (!entityNameNode.getNodeName().equals(STMKnownRuleNames.qualifiedName)) {
            throw new UnsupportedOperationException("identifier expected while facing with " + node.getNodeName());
        }
        
        SQLQuerySymbolEntry entityName = collectIdentifier(entityNameNode.getStmChild(entityNameNode.getChildCount() - 1), forceUnquotted);
        if (entityNameNode.getChildCount() == 1) {
            return new SQLQueryQualifiedName(entityName);
        } else {
            STMTreeNode schemaNameNode = entityNameNode.getStmChild(0);
            SQLQuerySymbolEntry schemaName = collectIdentifier(
                schemaNameNode.getStmChild(schemaNameNode.getChildCount() - 1),
                forceUnquotted
            );
            if (schemaNameNode.getChildCount() == 1) {
                return new SQLQueryQualifiedName(schemaName, entityName);
            } else {
                STMTreeNode catalogNameNode = schemaNameNode.getStmChild(0);
                SQLQuerySymbolEntry catalogName = collectIdentifier(
                    catalogNameNode.getStmChild(catalogNameNode.getChildCount() - 1),
                    forceUnquotted
                );
                return new SQLQueryQualifiedName(catalogName, schemaName, entityName);
            }    
        }
    }
    
    private static final Set<String> knownValueExpressionRootNames = Set.of(
        STMKnownRuleNames.valueExpression,
        STMKnownRuleNames.searchCondition,
        STMKnownRuleNames.havingClause,
        STMKnownRuleNames.whereClause,
        STMKnownRuleNames.groupByClause,
        STMKnownRuleNames.orderByClause
    );
        
    private static final Set<String> knownRecognizableValueExpressionNames = Set.of(
        STMKnownRuleNames.subquery,
        STMKnownRuleNames.columnReference
    );

    @NotNull
    private SQLQueryValueExpression collectValueExpression(@NotNull STMTreeNode node) {
        if (!knownValueExpressionRootNames.contains(node.getNodeName())) {
            throw new UnsupportedOperationException(
                "Search condition or value expression expected while facing with " + node.getNodeName()
            );
        }
        
        if (knownRecognizableValueExpressionNames.contains(node.getNodeName())) {
            return collectKnownValueExpression(node);
        } else {
            Stack<STMTreeNode> stack = new Stack<>();
            Stack<List<SQLQueryValueExpression>> childLists = new Stack<>();
            stack.add(node);
            childLists.push(new ArrayList<>(1));

            while (stack.size() > 0) {
                STMTreeNode n = stack.pop();
                
                if (n != null) {
                    STMTreeNode rn = n;
                    while (rn.getChildCount() == 1 && !knownRecognizableValueExpressionNames.contains(rn.getNodeName())) {
                        rn = rn.getStmChild(0);
                    }
                    if (knownRecognizableValueExpressionNames.contains(rn.getNodeName())) {
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
                    STMTreeNode content = stack.pop();
                    List<SQLQueryValueExpression> children = childLists.pop();
                    if (children.size() > 0) {
                        SQLQueryValueExpression e = children.size() == 1 && children.get(0) instanceof SQLQueryValueFlattenedExpression c ?
                            c :
                            new SQLQueryValueFlattenedExpression(content.getRealInterval(), content.getTextContent(), children);
                        childLists.peek().add(e);
                    }
                }
            }
            
            List<SQLQueryValueExpression> roots = childLists.pop();
            return roots.isEmpty() ?
                new SQLQueryValueFlattenedExpression(node.getRealInterval(), node.getTextContent(), Collections.emptyList()) :
                roots.get(0);
        }
    }

    @NotNull
    private SQLQueryValueExpression collectKnownValueExpression(@NotNull STMTreeNode node) {
        Interval range = node.getRealInterval();
        return switch (node.getNodeKindId()) {
            case SQLStandardParser.RULE_subquery -> new SQLQueryValueSubqueryExpression(range, this.collectQueryExpression(node));
            case SQLStandardParser.RULE_columnReference -> {
                SQLQuerySymbolEntry columnName = collectIdentifier(node.getStmChild(node.getChildCount() - 1));
                yield node.getChildCount() == 1 ? new SQLQueryValueColumnReferenceExpression(range, columnName)
                    : new SQLQueryValueColumnReferenceExpression(range, collectTableName(node.getStmChild(0)), columnName);
            }
            default -> throw new UnsupportedOperationException(
                "Subquery of columnReference expected while facing with " + node.getNodeName()
            );
        };
    }
}
