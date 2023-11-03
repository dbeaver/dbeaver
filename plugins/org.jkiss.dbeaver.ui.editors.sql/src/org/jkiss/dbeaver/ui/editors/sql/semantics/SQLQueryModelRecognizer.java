/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import java.util.*;
import static java.util.Map.entry;
import java.util.stream.Collectors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.sql.dialect.LSMDialectRegistry;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMSkippingErrorListener;
import org.jkiss.dbeaver.model.stm.STMSource;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;

interface SQLQueryRecognitionContext {
    void appendError(STMTreeNode treeNode, String error);
    void appendError(SQLQuerySymbolEntry symbol, String error);
    void appendError(SQLQuerySymbolEntry symbol, String error, DBException ex);
}

public class SQLQueryModelRecognizer {
    
    @FunctionalInterface
    private static interface TreeMapperCallback<T, C> {
        T apply(STMTreeNode node, List<T> children, C context);
    }
    
    private static class TreeMapper<T, C> {
        private interface MapperFrame {
            public abstract void doWork();
        }    
        
        private interface MapperResultFrame<T> extends MapperFrame {
            void aggregate(T result);
        }
            
        private abstract class MapperNodeFrame implements MapperFrame {
            public final STMTreeNode node;
            public final MapperResultFrame<T> parent;
            
            public MapperNodeFrame(STMTreeNode node, MapperResultFrame<T> parent) {
                this.node = node;
                this.parent = parent;
            }
        }
        
        private class MapperQueuedNodeFrame extends MapperNodeFrame {

            public MapperQueuedNodeFrame(STMTreeNode node, MapperResultFrame<T> parent) {
                super(node, parent);
            }
            
            @Override
            public void doWork() {
                TreeMapperCallback<T, C> translation = translations.get(node.getNodeName());
//                System.out.println("\texpanding " + node.getNodeName() + " as " + (translation == null ? "TRANSPARENT" : "HANDLED"));
                MapperResultFrame<T> aggregator = translation == null ? parent : new MapperDataPendingNodeFrame(node, parent, translation);
                
                if (translation != null) {
                    stack.push(aggregator);
                }
                for (int i = node.getChildCount() - 1; i >= 0; i--) {
                    if (transparentNodeNames.contains(node.getNodeName())) {
//                        System.out.println("\t\tenqueued " + ((STMTreeNode)node.getChild(i)).getNodeName());
                        stack.push(new MapperQueuedNodeFrame((STMTreeNode) node.getChild(i), aggregator));
                    } else {
//                        System.out.println("\t\tdropped " + ((STMTreeNode)node.getChild(i)).getNodeName());
                    }
                }
            }
        }
        
        private class MapperDataPendingNodeFrame extends MapperNodeFrame implements MapperResultFrame<T> {
            public final List<T> childrenData = new LinkedList<>();
            public final TreeMapperCallback<T, C> translation;
            
            public MapperDataPendingNodeFrame(STMTreeNode node, MapperResultFrame<T> parent, TreeMapperCallback<T, C> translation) {
                super(node, parent);
                this.translation = translation;
            }
            
            @Override
            public void aggregate(T result) {
                this.childrenData.add(result);
            }
            
            @Override
            public void doWork() {
//                System.out.println("\ttranslating " + this.node.getNodeName() + " and collecting " + childrenData.size() + " children");
                this.parent.aggregate(this.translation.apply(this.node, this.childrenData, TreeMapper.this.context));
            }
        }
        
        private class MapperRootFrame implements MapperResultFrame<T> {
            public final STMTreeNode node;
            public T result = null;
            
            public MapperRootFrame(STMTreeNode node) {
                this.node = node;
            }

            @Override
            public void aggregate(T result) {
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
        
        public TreeMapper(Class<T> mappingResultType, Set<String> transparentNodeNames, Map<String, TreeMapperCallback<T, C>> translations, C context) {
            this.mappingResultType = mappingResultType;
            this.transparentNodeNames = transparentNodeNames;
            this.translations = translations;
            this.context = context;
        }

        public T translate(STMTreeNode root) {
            MapperRootFrame rootFrame = new MapperRootFrame(root);
            stack.push(rootFrame);

//            System.out.println("TreeMapper started");
            while (stack.size() > 0) {
                stack.pop().doWork();
            }
//            System.out.println("TreeMapper finished");
            
            return rootFrame.result;
        }
    }
    
    private static class QueryExpressionMapper extends TreeMapper<SQLQueryRowsSource, SQLQueryModelRecognizer> {
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
        
        private static final Map<String, TreeMapperCallback<SQLQueryRowsSource, SQLQueryModelRecognizer>> translations = Map.ofEntries(
            entry(STMKnownRuleNames.queryExpression, (n, cc, r) -> {
                SQLQueryRowsSource source = cc.get(0);
                for (int i = 1; i < cc.size(); i++) {
                    STMTreeNode childNode = n.getStmChild(i);
                    List<SQLQuerySymbolEntry> corresponding = r.collectColumnNameList(childNode);
                    SQLQueryRowsSource nextSource = cc.get(i);
                    source = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_exceptTerm -> new SQLQueryExceptModel(source, nextSource, corresponding);
                        case SQLStandardParser.RULE_unionTerm -> new SQLQueryUnionModel(source, nextSource, corresponding);
                        default -> throw new UnsupportedOperationException("Unexpected child node kind at queryExpression");
                    };
                }
                return source;
            }),
            entry(STMKnownRuleNames.nonJoinQueryTerm, (n, cc, r) -> {
                SQLQueryRowsSource source = cc.get(0);
                for (int i = 1; i < cc.size(); i++) {
                    STMTreeNode childNode = n.getStmChild(i);
                    List<SQLQuerySymbolEntry> corresponding = r.collectColumnNameList(childNode);
                    SQLQueryRowsSource nextSource = cc.get(i);
                    source = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_intersectTerm -> new SQLQueryIntersectModel(source, nextSource, corresponding);
                        default -> throw new UnsupportedOperationException("Unexpected child node kind at nonJoinQueryTerm");
                    };
                }
                return source;
            }),
            entry(STMKnownRuleNames.joinedTable, (n, cc, r) -> {
                SQLQueryRowsSource source = cc.get(0);
                for (int i = 1; i < cc.size(); i++) {
                    final SQLQueryRowsSource currSource = source, nextSource = cc.get(i);
                    STMTreeNode childNode = n.getStmChild(i);
                    source = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_naturalJoinTerm -> Optional.ofNullable(childNode.findChildOfName(STMKnownRuleNames.joinSpecification))
                                .map(cn -> cn.findChildOfName(STMKnownRuleNames.joinCondition))
                                .map(cn -> cn.findChildOfName(STMKnownRuleNames.searchCondition))
                                .map(cn -> r.collectValueExpression(cn)
                                ).map(e -> new SQLQueryNaturalJoinModel(currSource, nextSource, e))
                                 .orElseGet(() -> new SQLQueryNaturalJoinModel(currSource, nextSource, r.collectColumnNameList(childNode)));
                        case SQLStandardParser.RULE_crossJoinTerm -> new SQLQueryCrossJoinModel(currSource, nextSource);
                        default -> throw new UnsupportedOperationException("Unexpected child node kind at queryExpression");
                    };
                }
                return source;
            }),
            entry(STMKnownRuleNames.fromClause, (n, cc, r) -> {
                SQLQueryRowsSource source = cc.get(0);
                for (int i = 1; i < cc.size(); i++) {
                    STMTreeNode childNode = n.getStmChild(1 + i * 2);
                    SQLQueryRowsSource nextSource = cc.get(i);
                    source = switch (childNode.getNodeKindId()) {
                        case SQLStandardParser.RULE_tableReference -> new SQLQueryCrossJoinModel(source, nextSource);
                        default -> throw new UnsupportedOperationException("Unexpected child node kind at fromClause");
                    };
                }
                return source;
            }),
            entry(STMKnownRuleNames.tableExpression, (n, cc, r) -> {
                // tableExpression: fromClause whereClause? groupByClause? havingClause? orderByClause? limitClause?;
                SQLQueryValueExpression whereExpr = Optional.ofNullable(n.findChildOfName(STMKnownRuleNames.whereClause))
                        .map(r::collectValueExpression).orElse(null);
                SQLQueryValueExpression havingClause = Optional.ofNullable(n.findChildOfName(STMKnownRuleNames.havingClause))
                        .map(r::collectValueExpression).orElse(null);
                SQLQueryValueExpression groupByClause = Optional.ofNullable(n.findChildOfName(STMKnownRuleNames.groupByClause))
                        .map(r::collectValueExpression).orElse(null);
                SQLQueryValueExpression orderByClause = Optional.ofNullable(n.findChildOfName(STMKnownRuleNames.orderByClause))
                        .map(r::collectValueExpression).orElse(null);
                return new SQLQuerySelectionFilterModel(cc.get(0), whereExpr, havingClause, groupByClause, orderByClause);
            }),
            entry(STMKnownRuleNames.querySpecification, (n, cc, r) -> {
                STMTreeNode selectListNode = n.findChildOfName(STMKnownRuleNames.selectList);
                List<SQLQueryResultSublistSpec> sublists = new ArrayList<>((selectListNode.getChildCount() + 1) / 2);
                for (int i = 0; i < selectListNode.getChildCount(); i += 2) {
                    STMTreeNode selectSublist = selectListNode.getStmChild(i);
                    STMTreeNode sublistNode = selectSublist.getStmChild(0);
                    sublists.add(switch (sublistNode.getNodeKindId()) { // selectSublist: (Asterisk|derivedColumn|qualifier Period Asterisk
                        case SQLStandardParser.RULE_derivedColumn -> { // derivedColumn: valueExpression (asClause)?; asClause: (AS)? columnName;
                            SQLQueryValueExpression expr = r.collectValueExpression(sublistNode.getStmChild(0));
                            if (sublistNode.getChildCount() > 1) {
                                STMTreeNode asClause = sublistNode.getStmChild(1);
                                SQLQuerySymbolEntry asColumnName = r.collectIdentifier(asClause.getStmChild(asClause.getChildCount() - 1));
                                yield new SQLQueryResultColumnSpec(expr, asColumnName);
                            } else {
                                yield new SQLQueryResultColumnSpec(expr);
                            }
                        }
                        default -> selectSublist.getChildCount() == 1 ? new SQLQueryResultCompleteTupleSpec()
                            : new SQLQueryResultTupleSpec(r.collectTableName(sublistNode));
                    });
                }

                return new SQLQueryProjectionModel(cc.get(0), new SQLQuerySelectionResultModel(sublists));
            }),
            entry(STMKnownRuleNames.nonjoinedTableReference, (n, cc, r) -> { 
                SQLQueryRowsSource source = cc.size() > 0 ? cc.get(0) : r.collectTableReference(n.getStmChild(0));
                if (n.getChildCount() > 1) {
                    STMTreeNode lastSubnode = n.getStmChild(n.getChildCount() - 1);
                    if (lastSubnode.getNodeName().equals(STMKnownRuleNames.correlationSpecification)) {
                        SQLQuerySymbolEntry correlationName = r.collectIdentifier(
                            lastSubnode.getStmChild(lastSubnode.getChildCount() == 1 || lastSubnode.getChildCount() == 4 ? 0 : 1)
                        ); 
                        source = new SQLQueryCorrelatedRowsSource(source, correlationName, r.collectColumnNameList(lastSubnode));
                    }
                }
                return source;
            }),
            entry(STMKnownRuleNames.explicitTable, (n, cc, r) -> {
                return r.collectTableReference(n);
            }),
            entry(STMKnownRuleNames.tableValueConstructor, (n, cc, r) -> {
                return new SQLTableValueRowsSource();                 // TODO
            })
        );

        public QueryExpressionMapper(SQLQueryModelRecognizer recognizer) {
            super(SQLQueryRowsSource.class, queryExpressionSubtreeNodeNames, translations, recognizer);
        }
    }
    
    private final DBCExecutionContext executionContext;
    
    private final HashSet<SQLQuerySymbolEntry> symbolEntries = new HashSet<>();
    
    private final SQLQueryRecognitionContext recognitionContext = new SQLQueryRecognitionContext() {
        
        @Override
        public void appendError(SQLQuerySymbolEntry symbol, String error, DBException ex) {
            System.out.println(symbol.getName() + ": " + error + ": " + ex.toString());
        }
        
        @Override
        public void appendError(SQLQuerySymbolEntry symbol, String error) {
            System.out.println(symbol.getName() + ": " + error);
        }
        
        @Override
        public void appendError(STMTreeNode treeNode, String error) {
            // TODO Auto-generated method stub
            
        }
    };


    public SQLQueryModelRecognizer(DBCExecutionContext executionContext) {
        this.executionContext = executionContext;
    }
    
    private SQLQueryDataContext prepareDataContext(STMTreeNode root) {
        if (this.executionContext != null && this.executionContext.getDataSource() instanceof DBSObjectContainer container) {
            return new SQLQueryDataSourceContext(this.executionContext);
        } else {
            List<SQLQueryColumnReferenceExpression> knownColumns= STMUtils.expandSubtree(root, null, Set.of(STMKnownRuleNames.columnReference)).stream()
                    .map(r -> (SQLQueryColumnReferenceExpression)collectKnownValueExpression(r))
                    .collect(Collectors.toList());
            return new SQLQueryFakeDataSourceContext(knownColumns);
        }
    }
    
    private SQLDialect obtainSqlDialect() {
        if (this.executionContext != null && this.executionContext.getDataSource() != null) {
            return this.executionContext.getDataSource().getSQLDialect();
        } else {
            return BasicSQLDialect.INSTANCE;
        }
    }
    
    public SQLQuerySelectionModel recognizeQuery(String text) {
        STMSource querySource = STMSource.fromString(text);
        LSMAnalyzer analyzer = LSMDialectRegistry.getInstance().getAnalyzerForDialect(this.obtainSqlDialect());
        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(querySource, new STMSkippingErrorListener());
        
        SQLQueryRowsSource source = this.collectQueryExpression(tree);
        if (source != null) {
        
            SQLQuerySelectionModel model = new SQLQuerySelectionModel(source, symbolEntries);
            
            SQLQueryDataContext context = this.prepareDataContext(tree);
            model.propagateContex(context, recognitionContext);
            
            // model.collectTableRefs(query);
            
            /*
            done:
                collect union recursively if any
            almost done:
                collect selection sources (referenced, subqueries, direct table, query spec, table-values, explicit tables - and correlation alias)
             
            collect cte recursively
            collect result model
            collect conditionals (where, group, having, orderings, etc)
            resolve and merge all symbols
            classify all symbols
            */

            return model;
        } else {
            return null;
        }
    }
    
    private SQLQueryRowsSource collectQueryExpression(STMTreeNode tree) {
        QueryExpressionMapper queryMapper = new QueryExpressionMapper(this);
        SQLQueryRowsSource rowsSource = queryMapper.translate(tree);
        return rowsSource; 
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
    
    private List<SQLQuerySymbolEntry> collectColumnNameList(STMTreeNode node) {
        if (!node.getNodeName().equals(STMKnownRuleNames.columnNameList)) {
            if (!columnNameListWrapperNames.contains(node.getNodeName())) {
                throw new UnsupportedOperationException("columnNameList (or its wrapper) expected while facing with " + node.getNodeName());
            }
            
            List<STMTreeNode> actual = STMUtils.expandSubtree(node, columnNameListWrapperNames, Set.of(STMKnownRuleNames.columnNameList));
            if (actual.size() > 1) {
                throw new UnsupportedOperationException("Ambiguous columnNameList collection at " + node.getNodeName());
            } else if (actual.size() == 0) {
                return Collections.emptyList(); 
            } else {
                node = actual.get(0);
            }
        }
        
        List<SQLQuerySymbolEntry> result = new ArrayList<>(node.getChildCount());
        for (int i = 0; i < node.getChildCount(); i++) {
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
    
    private SQLQuerySymbolEntry collectIdentifier(STMTreeNode node) {
        STMTreeNode actual = identifierDirectWrapperNames.contains(node.getNodeName()) ? node.getStmChild(0) : node;
        if (!actual.getNodeName().equals(STMKnownRuleNames.identifier)) {
            throw new UnsupportedOperationException("identifier expected while facing with " + node.getNodeName());
        }
        String identifierString = actual.getTextContent(); // TODO: consider escaping and such, see parser grammar for the details
        SQLQuerySymbolEntry entry = new SQLQuerySymbolEntry(actual.getRealInterval(), identifierString);
        symbolEntries.add(entry);
        return entry;
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
    
    private SQLQueryTableDataModel collectTableReference(STMTreeNode node) {
        return new SQLQueryTableDataModel(collectTableName(node));
    }

    private SQLQueryQualifiedName collectTableName(STMTreeNode node) {
        List<STMTreeNode> actual = STMUtils.expandSubtree(node, tableNameContainers, actualTableNameContainers);
        if (actual.size() > 1) {
            throw new UnsupportedOperationException("Ambiguous tableName collection at " + node.getNodeName());
        } else if (actual.size() == 0) {
            return null;
        } else {
            node = actual.get(0);
        }
        return node.getNodeName().equals(STMKnownRuleNames.tableName) ? collectQualifiedName(node)
                                            : new SQLQueryQualifiedName(collectIdentifier(node));
    }
    
    private static final Set<String> qualifiedNameDirectWrapperNames = Set.of(
        STMKnownRuleNames.tableName,
        STMKnownRuleNames.constraintName
    );
    
    private SQLQueryQualifiedName collectQualifiedName(STMTreeNode node) { // qualifiedName
        STMTreeNode entityNameNode = qualifiedNameDirectWrapperNames.contains(node.getNodeName()) ? node.getStmChild(0) : node;
        if (!entityNameNode.getNodeName().equals(STMKnownRuleNames.qualifiedName)) {
            throw new UnsupportedOperationException("identifier expected while facing with " + node.getNodeName());
        }
        
        SQLQuerySymbolEntry entityName = collectIdentifier(entityNameNode.getStmChild(entityNameNode.getChildCount() - 1));
        if (entityNameNode.getChildCount() == 1) {
            return new SQLQueryQualifiedName(entityName);
        } else {
            STMTreeNode schemaNameNode = entityNameNode.getStmChild(0);
            SQLQuerySymbolEntry schemaName = collectIdentifier(schemaNameNode.getStmChild(schemaNameNode.getChildCount() - 1));
            if (schemaNameNode.getChildCount() == 1) {
                return new SQLQueryQualifiedName(schemaName, entityName);
            } else {
                STMTreeNode catalogNameNode = schemaNameNode.getStmChild(0);
                SQLQuerySymbolEntry catalogName = collectIdentifier(catalogNameNode.getStmChild(catalogNameNode.getChildCount() - 1));
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
    
    private SQLQueryValueExpression collectValueExpression(STMTreeNode node) {
        if (!knownValueExpressionRootNames.contains(node.getNodeName())) {
            throw new UnsupportedOperationException("Search condition or value expression expected while facing with " + node.getNodeName());
        }
        
        List<STMTreeNode> knownExprs = STMUtils.expandSubtree(node, null, knownRecognizableValueExpressionNames);
        return knownExprs.size() == 1 ? collectKnownValueExpression(knownExprs.get(0))
                : new SQLQueryFlattenedExpression(knownExprs.stream().map(e -> collectKnownValueExpression(e)).collect(Collectors.toList()));
    }
    
    private SQLQueryValueExpression collectKnownValueExpression(STMTreeNode node) {
        return switch (node.getNodeKindId()) {
        case SQLStandardParser.RULE_subquery -> new SQLQuerySubqueryExpression(this.collectQueryExpression(node));
        case SQLStandardParser.RULE_columnReference -> {
            SQLQuerySymbolEntry columnName = collectIdentifier(node.getStmChild(node.getChildCount() - 1));
            yield node.getChildCount() == 1 ? new SQLQueryColumnReferenceExpression(columnName) 
                    : new SQLQueryColumnReferenceExpression(collectTableName(node.getStmChild(0)), columnName);
        }
        default -> throw new UnsupportedOperationException("Unexpected expression node kind");
        };
    }
}
