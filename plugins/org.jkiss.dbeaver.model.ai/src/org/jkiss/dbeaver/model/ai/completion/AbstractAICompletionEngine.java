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
import org.jkiss.dbeaver.model.ai.AICompletionConstants;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractAICompletionEngine<SERVICE, REQUEST> implements DAICompletionEngine<SERVICE> {

    @NotNull
    @Override
    public List<DAICompletionResponse> performQueryCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request,
        @NotNull IAIFormatter formatter
    ) throws DBException {
        String result = requestCompletion(monitor, request, formatter, null);
        DAICompletionResponse response = createCompletionResponse(request.getContext(), result);
        return Collections.singletonList(response);
    }

    @NotNull
    @Override
    public List<DAICompletionResponse> performQueryCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request,
        @NotNull DAICompletionSession session,
        @NotNull IAIFormatter formatter
    ) throws DBException {
        final String result = requestCompletion(monitor, request, formatter, session);
        final DAICompletionResponse response = new DAICompletionResponse();
        response.setResultCompletion(result);

        // Add request to the session history
        session.add(request);

        return List.of(response);
    }

    public abstract Map<String, SERVICE> getServiceMap();

    protected abstract int getMaxTokens();

    @Nullable
    abstract protected String requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request,
        @NotNull IAIFormatter formatter,
        @Nullable DAICompletionSession session
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
        @NotNull String modifiedRequest,
        @NotNull SERVICE service,
        @NotNull REQUEST completionRequest
    ) throws DBException;

    @NotNull
    protected DBSObjectContainer getScopeObject(
        @NotNull DAICompletionRequest request,
        @NotNull DBCExecutionContext executionContext
    ) {
        DAICompletionScope scope = request.getContext().getScope();
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

    protected abstract REQUEST createCompletionRequest(@NotNull String request);

    protected abstract REQUEST createCompletionRequest(@NotNull String request, int responseSize);

    protected abstract SERVICE getServiceInstance(@NotNull DBCExecutionContext executionContext) throws DBException;

    protected abstract AIEngineSettings getSettings();

    @Nullable
    protected String processCompletion(
        @NotNull DAICompletionRequest request,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer mainObject,
        @Nullable String completionText,
        @NotNull IAIFormatter formatter
    ) {
        if (CommonUtils.isEmpty(completionText)) {
            return null;
        }
        completionText = "SELECT " + completionText.trim() + ";";

        completionText = formatter.postProcessGeneratedQuery(monitor, mainObject, executionContext, completionText);
        if (DBWorkbench.getPlatform().getPreferenceStore()
            .getBoolean(AICompletionConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT)) {
            String[] lines = request.getPromptText().split("\n");
            StringBuilder completionTextBuilder = new StringBuilder(completionText);
            for (String line : lines) {
                if (!CommonUtils.isEmpty(line)) {
                    String[] singleLineComments = mainObject.getDataSource().getSQLDialect().getSingleLineComments();
                    if (singleLineComments == null || singleLineComments.length == 0) {
                        completionTextBuilder.insert(0, "-- " + line.trim() + "\n");
                    } else {
                        completionTextBuilder.insert(0, singleLineComments[0] + " " + line.trim() + "\n");
                    }
                }
            }
            completionText = completionTextBuilder.toString();
        }
        return completionText.trim();
    }

}
