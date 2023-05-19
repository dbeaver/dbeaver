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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.LSMAnalysisCase;
import org.jkiss.dbeaver.model.lsm.LSMElement;
import org.jkiss.dbeaver.model.lsm.LSMParser;
import org.jkiss.dbeaver.model.lsm.LSMSource;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;

public abstract class LSMAnalysisCaseImpl<T extends LSMElement, M extends AbstractSyntaxNode & LSMElement>
    implements LSMAnalysisCase<T, M> {

    private final Class<T> modelContractType;
    private final Class<M> modelRootType;
    
    public LSMAnalysisCaseImpl(
        @NotNull Class<T> modelContractType,
        @NotNull Class<M> modelRootType
    ) {
        this.modelContractType = modelContractType;
        this.modelRootType = modelRootType;
    }

    @NotNull
    @Override
    public Class<T> getModelContractType() {
        return modelContractType;
    }

    @NotNull
    @Override
    public Class<M> getModelRootType() {
        return modelRootType;
    }

    @Nullable
    @Override
    public abstract LSMParser createParser(@NotNull LSMSource source, @Nullable ANTLRErrorListener errorListener);
}
