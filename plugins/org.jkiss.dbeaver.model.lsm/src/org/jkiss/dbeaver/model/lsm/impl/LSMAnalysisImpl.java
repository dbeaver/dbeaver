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
package org.jkiss.dbeaver.model.lsm.impl;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.LSMAnalysis;
import org.jkiss.dbeaver.model.lsm.LSMElement;
import org.jkiss.dbeaver.model.lsm.LSMParser;
import org.jkiss.dbeaver.model.lsm.LSMSkippingErrorListener;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModelMappingResult;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.Sql92Parser;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.Sql92Parser.*;
import org.jkiss.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class LSMAnalysisImpl<T extends LSMElement, M extends AbstractSyntaxNode & LSMElement> implements LSMAnalysis<T> {

    private static final Logger log = LoggerFactory.getLogger(LSMAnalysisImpl.class);
    private final LSMSourceImpl source;
    private final LSMAnalysisCaseImpl<T, M> analysisCase;
    private final SyntaxModel syntaxModel;

    CompletableFuture<Tree> tree;
    CompletableFuture<T> model;
    
    public LSMAnalysisImpl(LSMSourceImpl source, LSMAnalysisCaseImpl<T, M> analysisCase, SyntaxModel syntaxModel) {
        this.source = source;
        this.analysisCase = analysisCase;
        this.syntaxModel = syntaxModel;
        
        this.tree = new CompletableFuture<>();
        this.model = new CompletableFuture<>();
    }

    @Nullable
    Future<Tree> getTree(@Nullable ANTLRErrorListener errorListener) {
        if (!this.tree.isDone()) {
            LSMParser parser = errorListener == null
                ? analysisCase.createParser(source)
                : analysisCase.createParser(source, errorListener);
            Tree newTree = null;
            if (parser != null) {
                newTree = parser.parse();
            }
            if (newTree != null) {
                this.tree.complete(newTree);
            }
        }
        return this.tree;
    }

    @Nullable
    @Override
    public Future<T> getModel() {
        try {
            if (!this.model.isDone()) {
                Future<Tree> futureTree = this.getTree(null);
                if (futureTree != null) {
                    SyntaxModelMappingResult<M> result = this.syntaxModel.map(futureTree.get(), analysisCase.getModelRootType());

                    @SuppressWarnings("unchecked")
                    T model = (T) result.getModel();
                    if (model != null) {
                        this.model.complete(model);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            this.model.completeExceptionally(e);
        }
        return this.model;
    }
    
    
    @Nullable
    @Override
    public List<Pair<String, String>> getTableAndAliasFromSources() {
        Tree tree = null;
        try {
            Future<Tree> futureTree = getTree(new LSMSkippingErrorListener());
            if (futureTree != null) {
                tree = futureTree.get();
            } else {
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Exception occurred during syntax analysis", e);
        }

        if (!(tree instanceof SqlQueryContext)) {
            return null;
        }
        SqlQueryContext query = (SqlQueryContext) tree;
        List<Pair<String, String>> result = new ArrayList<>();
        for (NonjoinedTableReferenceContext treeNode : getTableReferences(query)) {
            Pair<String, String> tableAndAlias = getTableAndAlias(treeNode);
            if (tableAndAlias != null) {
                result.add(tableAndAlias);
            }
        }
        return result;
    }


    @NotNull
    private static List<NonjoinedTableReferenceContext> getTableReferences(@NotNull ParseTree query) {
        List<NonjoinedTableReferenceContext> result = new ArrayList<>();
        Stack<Tree> stack = new Stack<>();
        stack.push(query);

        while (stack.size() > 0) {
            Tree tree = stack.pop();
            if (tree instanceof SqlQueryContext ||
                    tree instanceof DirectSqlStatementContext ||
                    tree instanceof DirectSqlDataStatementContext ||
                    tree instanceof SelectStatementContext ||
                    tree instanceof QueryExpressionContext ||
                    tree instanceof NonJoinQueryTermContext ||
                    tree instanceof QueryPrimaryContext ||
                    tree instanceof NonJoinQueryPrimaryContext ||
                    tree instanceof SimpleTableContext ||
                    tree instanceof JoinedTableContext
            ) {
                for (int i = 0; i < tree.getChildCount(); i++) {
                    stack.push(tree.getChild(i));
                }
            } else if (tree instanceof QuerySpecificationContext) {
                QuerySpecificationContext querySpecification = (QuerySpecificationContext) tree;
                TableExpressionContext tableExpression = querySpecification.tableExpression();
                if (tableExpression != null) {
                    FromClauseContext fromClause = tableExpression.fromClause();
                    if (fromClause != null) {
                        result.addAll(getNonJoinedTableReferences(fromClause.tableReference(), stack));
                    }
                }
            } else if (tree instanceof NonjoinedTableReferenceContext) {
                result.add((NonjoinedTableReferenceContext) tree);
            } else if (tree instanceof NaturalJoinTermContext) {
                NaturalJoinTermContext naturalJoin = (NaturalJoinTermContext) tree;
                TableReferenceContext tableReference = naturalJoin.tableReference();
                if (tableReference != null) {
                    Sql92Parser.NonjoinedTableReferenceContext tableRef = tableReference.nonjoinedTableReference();
                    if (tableRef != null) {
                        result.add(tableRef);
                    }
                }
            } else if (tree instanceof CrossJoinTermContext) {
                CrossJoinTermContext naturalJoin = (CrossJoinTermContext) tree;
                TableReferenceContext tableReference = naturalJoin.tableReference();
                if (tableReference != null) {
                    NonjoinedTableReferenceContext tableRef = tableReference.nonjoinedTableReference();
                    if (tableRef != null) {
                        result.add(tableRef);
                    }
                }
            }
        }
        return result;
    }

    @NotNull
    private static List<NonjoinedTableReferenceContext> getNonJoinedTableReferences(
        @NotNull List<TableReferenceContext> tableReferences,
        @NotNull Stack<Tree> stack
    ) {
        List<NonjoinedTableReferenceContext> result = new ArrayList<>();
        for (TableReferenceContext tableRef : tableReferences) {
            if (tableRef.nonjoinedTableReference() != null) {
                result.add(tableRef.nonjoinedTableReference());
            } else if (tableRef.joinedTable() != null) {
                stack.push(tableRef.joinedTable());
            }
        }
        return result;
    }

    @Nullable
    private static Pair<String, String> getTableAndAlias(@NotNull NonjoinedTableReferenceContext nonJoinedTableReference) {
        CorrelationSpecificationContext correlationSpec = nonJoinedTableReference.correlationSpecification();
        String alias = null;
        String tableName = null;
        if (correlationSpec != null) {
            alias = correlationSpec.correlationName().getText();
            
        }
        if (nonJoinedTableReference.tableName() != null) {
            tableName = nonJoinedTableReference.tableName().getText();
        }
        return new Pair<>(tableName, alias);
    }


}
