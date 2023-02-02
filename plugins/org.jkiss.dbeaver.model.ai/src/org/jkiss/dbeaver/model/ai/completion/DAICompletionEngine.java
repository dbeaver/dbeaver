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

package org.jkiss.dbeaver.model.ai.completion;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * Completion engine
 */
public interface DAICompletionEngine {

    /**
     * Completion engine name
     */
    String getEngineName();

    /**
     * Completion model name
     */
    String getModelName();

    /**
     * Do query completion
     */
    @NotNull
    List<DAICompletionResponse> performQueryCompletion(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBSLogicalDataSource dataSource,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DAICompletionRequest completionRequest,
        boolean returnOnlyCompletion,
        int maxResults
    ) throws DBException;

}
