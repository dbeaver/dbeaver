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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.*;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


public class LSMDialectImpl implements LSMDialect {
    
    private final Map<Class<? extends LSMElement>, LSMAnalysisCase<? extends LSMElement, ? extends AbstractSyntaxNode>> casesByContractType;
    private final SyntaxModel syntaxModel;
    
    public LSMDialectImpl(
        Map<Class<? extends LSMElement>, LSMAnalysisCase<? extends LSMElement, ? extends AbstractSyntaxNode>> casesByModelType,
        SyntaxModel syntaxModel
    ) {
        this.casesByContractType = casesByModelType;
        this.syntaxModel = syntaxModel;
    }

    @Override
    public Collection<LSMAnalysisCase<? extends LSMElement, ? extends AbstractSyntaxNode>> getSupportedCases() {
        return casesByContractType.values();
    }

    @Nullable
    @Override
    public <T extends LSMElement> LSMAnalysisCase<T, ? extends AbstractSyntaxNode> findAnalysisCase(
        @NotNull Class<T> expectedContractType
    ) throws LSMException {
        @SuppressWarnings("unchecked")
        var result = (LSMAnalysisCase<T, ? extends AbstractSyntaxNode>) casesByContractType.get(expectedContractType);
        if (result == null) {
            for (var entry : casesByContractType.entrySet()) {
                if (expectedContractType.isAssignableFrom(entry.getKey())) {
                    return (LSMAnalysisCase<T, ? extends AbstractSyntaxNode>) entry.getValue();
                }
            }
            throw new LSMException("Can evaluate parser resul to " + expectedContractType.getName());
        }
        return result;
    }

    @NotNull
    @Override
    public <T extends LSMElement> Future<LSMAnalysis<T>> prepareAnalysis(
        @NotNull LSMSource source,
        @NotNull LSMAnalysisCase<T, ? extends AbstractSyntaxNode> analysisCase
    ) {
        LSMSourceImpl source2 = source.coerce(LSMSourceImpl.class);
        @SuppressWarnings("unchecked")
        LSMAnalysisCaseImpl<T, ? extends AbstractSyntaxNode> analysisCase2 = analysisCase.coerce(LSMAnalysisCaseImpl.class);
        return CompletableFuture.completedFuture(new LSMAnalysisImpl<>(source2, analysisCase2, syntaxModel));
    }

}
