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

import org.antlr.v4.runtime.tree.Tree;
import org.jkiss.dbeaver.model.lsm.interfaces.LSMAnalysis;
import org.jkiss.dbeaver.model.lsm.interfaces.LSMNode;
import org.jkiss.dbeaver.model.lsm.interfaces.LSMParser;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModelMappingResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class LSMAnalysisImpl<T extends LSMNode, M extends AbstractSyntaxNode & LSMNode> implements LSMAnalysis<T> {
    
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
    
    Future<Tree> getTree() {
        if (!this.tree.isDone()) {
            LSMParser parser = analysisCase.createParser(source);
            
            Tree tree = parser.parse();
            this.tree.complete(tree);
        }
        return this.tree;
    }
    
    @Override
    public Future<T> getModel() {
        try {
            if (!this.model.isDone()) {
                Tree tree = this.getTree().get();
                
                SyntaxModelMappingResult<M> result = this.syntaxModel.map(tree, analysisCase.getModelRootType());
                
                @SuppressWarnings("unchecked")
                T model = (T) result.getModel();
                this.model.complete(model);
            }
        } catch (InterruptedException | ExecutionException e) {
            this.model.completeExceptionally(e);
        }
        return this.model;
    }

}
