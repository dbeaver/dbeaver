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
package org.jkiss.dbeaver.model.ai.completion;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.ai.openai.GPTModel;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractAICompletionEngine<SERVICE, REQUEST> implements DAICompletionEngine<SERVICE> {

    @NotNull
    @Override
    public List<DAICompletionResponse> performQueryCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @NotNull DAICompletionMessage message,
        @NotNull IAIFormatter formatter
    ) throws DBException {
        String result = requestCompletion(monitor, context, List.of(message), formatter);
        DAICompletionResponse response = createCompletionResponse(context, result);
        return Collections.singletonList(response);
    }

    @NotNull
    @Override
    public List<DAICompletionResponse> performQueryCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @NotNull DAICompletionSession session,
        @NotNull IAIFormatter formatter
    ) throws DBException {
        final String result = requestCompletion(monitor, context, session.getMessages(), formatter);
        final DAICompletionResponse response = new DAICompletionResponse();
        response.setResultCompletion(result);
        return List.of(response);
    }

    public abstract Map<String, SERVICE> getServiceMap();

    protected abstract int getMaxTokens();

    @Nullable
    abstract protected String requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @NotNull List<DAICompletionMessage> messages,
        @NotNull IAIFormatter formatter
    ) throws DBException;

    @NotNull
    protected DAICompletionResponse createCompletionResponse(
        @NotNull DAICompletionContext context,
        @NotNull String result
    ) {
        DAICompletionResponse response = new DAICompletionResponse();
        response.setResultCompletion(result);
        return response;
    }

    @Nullable
    protected abstract String callCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<DAICompletionMessage> messages,
        @NotNull SERVICE service,
        @NotNull REQUEST completionRequest
    ) throws DBException;

    @NotNull
    protected DBSObjectContainer getScopeObject(
        @NotNull DAICompletionContext context,
        @NotNull DBCExecutionContext executionContext
    ) {
        DAICompletionScope scope = context.getScope();
        DBSObjectContainer mainObject = null;
        DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
        if (contextDefaults != null) {
            switch (scope) {
                case CURRENT_SCHEMA:
                    if (contextDefaults.getDefaultSchema() != null) {
                        mainObject = contextDefaults.getDefaultSchema();
                    } else {
                        mainObject = contextDefaults.getDefaultCatalog();
                    }
                    break;
                case CURRENT_DATABASE:
                    mainObject = contextDefaults.getDefaultCatalog();
                    break;
                default:
                    break;
            }
        }
        if (mainObject == null) {
            mainObject = ((DBSObjectContainer) executionContext.getDataSource());
        }
        return mainObject;
    }

    protected abstract REQUEST createCompletionRequest(@NotNull List<DAICompletionMessage> messages);

    protected abstract REQUEST createCompletionRequest(@NotNull List<DAICompletionMessage> messages, int responseSize);

    protected abstract SERVICE getServiceInstance(@NotNull DBCExecutionContext executionContext) throws DBException;

    protected abstract AIEngineSettings getSettings();

    @Nullable
    protected String processCompletion(
        @NotNull List<DAICompletionMessage> messages,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer mainObject,
        @Nullable String completionText,
        @NotNull IAIFormatter formatter,
        @NotNull GPTModel model
    ) {
        if (CommonUtils.isEmpty(completionText)) {
            return null;
        }

        if (!model.isChatAPI()) {
            completionText = "SELECT " + completionText.trim() + ";";
        }

        return formatter.postProcessGeneratedQuery(monitor, mainObject, executionContext, completionText).trim();
    }

}
