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

import org.jkiss.dbeaver.model.lsm.interfaces.LSMAnalysis;
import org.jkiss.dbeaver.model.lsm.interfaces.LSMAnalysisCase;
import org.jkiss.dbeaver.model.lsm.interfaces.LSMDialect;
import org.jkiss.dbeaver.model.lsm.interfaces.LSMNode;
import org.jkiss.dbeaver.model.lsm.interfaces.LSMSource;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


public class LSMDialectImpl implements LSMDialect {
    
    private final Map<Class<? extends LSMNode>, LSMAnalysisCase<? extends LSMNode, ? extends AbstractSyntaxNode>> casesByContractType;
    private final SyntaxModel syntaxModel;
    
    public LSMDialectImpl(
        Map<Class<? extends LSMNode>, LSMAnalysisCase<? extends LSMNode, ? extends AbstractSyntaxNode>> casesByModelType,
        SyntaxModel syntaxModel
    ) {
        this.casesByContractType = casesByModelType;
        this.syntaxModel = syntaxModel;
    }

    @Override
    public Collection<LSMAnalysisCase<? extends LSMNode, ? extends AbstractSyntaxNode>> getSupportedCases() {
        return casesByContractType.values();
    }

    @Override
    public <T extends LSMNode> LSMAnalysisCase<T, ? extends AbstractSyntaxNode> findAnalysisCase(Class<T> expectedContractType) {
        @SuppressWarnings("unchecked")
        LSMAnalysisCase<T, ? extends AbstractSyntaxNode> result = (LSMAnalysisCase<T, ? extends AbstractSyntaxNode>) casesByContractType.get(expectedContractType);
        return result;
    }

    @Override
    public <T extends LSMNode> Future<LSMAnalysis<T>> prepareAnalysis(LSMSource source, LSMAnalysisCase<T, ? extends AbstractSyntaxNode> analysisCase) {
        LSMSourceImpl source2 = source.coerce(LSMSourceImpl.class);
        @SuppressWarnings("unchecked")
        LSMAnalysisCaseImpl<T, ? extends AbstractSyntaxNode> analysisCase2 = analysisCase.coerce(LSMAnalysisCaseImpl.class);
        return CompletableFuture.completedFuture(new LSMAnalysisImpl<>(source2, analysisCase2, syntaxModel));
    }

}
