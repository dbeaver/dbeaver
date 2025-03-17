/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.copilot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.AISettingsRegistry;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionContext;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionMessage;
import org.jkiss.dbeaver.model.ai.completion.HttpClientCompletionEngine;
import org.jkiss.dbeaver.model.ai.copilot.auth.CopilotAuthService;
import org.jkiss.dbeaver.model.ai.copilot.request.CopilotRequestService;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.ai.metadata.MetadataProcessor;
import org.jkiss.dbeaver.model.ai.openai.GPTModel;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class CopilotCompletionEngine extends HttpClientCompletionEngine {
    protected static final Gson gson = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .setPrettyPrinting()
        .create();

    private static final Log log = Log.getLog(CopilotCompletionEngine.class);

    private String sessionToken = null;

    @Override
    public String getEngineName() {
        return CopilotConstants.COPILOT_ENGINE;
    }

    @Override
    public boolean isValidConfiguration() {
        return getSettings().getProperties().get(CopilotConstants.COPILOT_ACCESS_TOKEN) != null;
    }

    public String getModelName() {
        return CommonUtils.toString(getSettings().getProperties().get(AIConstants.GPT_MODEL), GPTModel.GPT_TURBO16.getName());
    }

    @Override
    protected int getMaxTokens() {
        return GPTModel.getByName(getModelName()).getMaxTokens();
    }

    @Nullable
    @Override
    protected String requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @NotNull List<DAICompletionMessage> messages,
        @NotNull IAIFormatter formatter,
        boolean chatCompletion
    ) throws DBException {
        final DBCExecutionContext executionContext = context.getExecutionContext();
        DBSObjectContainer mainObject = getScopeObject(context, executionContext);
        final DAICompletionMessage metadataMessage = MetadataProcessor.INSTANCE.createMetadataMessage(
            monitor,
            context,
            mainObject,
            formatter,
            getInstructions(chatCompletion),
            getMaxTokens() - AIConstants.MAX_RESPONSE_TOKENS
        );

        final List<DAICompletionMessage> mergedMessages = new ArrayList<>();
        mergedMessages.add(metadataMessage);
        mergedMessages.addAll(messages);

        if (sessionToken == null) {
            acquireSessionToken(monitor);
        }
        HttpRequest completionRequest = createCompletionRequest(chatCompletion, mergedMessages);
        HttpClient service = getServiceInstance(executionContext);
        String completionText = callCompletion(monitor, chatCompletion, mergedMessages, service, completionRequest);
        if (CommonUtils.toBoolean(getSettings().getProperties().get(AIConstants.AI_LOG_QUERY))) {
            log.debug("Copilot response:\n" + completionText);
        }
        return processCompletion(mergedMessages, monitor, executionContext, mainObject, completionText, formatter, true);
    }

    @Nullable
    @Override
    protected String callCompletion(
        @NotNull DBRProgressMonitor monitor,
        boolean chatMode,
        @NotNull List<DAICompletionMessage> messages,
        @NotNull HttpClient client,
        @NotNull HttpRequest completionRequest
    ) throws DBException {
        monitor.subTask("Request Copilot completion");
        try {
            HttpResponse<String> response = sendRequest(monitor, client, completionRequest);
            CopilotResult copilotResult = gson.fromJson(response.body(), CopilotResult.class);
            if (copilotResult.choices.length >= 1) {
                return copilotResult.choices[0].message.content;
            } else {
                return "";
            }
        } catch (Exception e) {
            throw new DBException("Error requesting completion", e);
        }
    }

    @Override
    protected HttpRequest createCompletionRequest(
        boolean chatMode,
        @NotNull List<DAICompletionMessage> messages
    ) throws DBCException {
        return createCompletionRequest(chatMode, messages, getMaxTokens());
    }

    @Override
    protected HttpRequest createCompletionRequest(
        boolean chatMode,
        @NotNull List<DAICompletionMessage> messages,
        int maxTokens
    ) throws DBCException {
        Double temperature =
            CommonUtils.toDouble(getSettings().getProperties().get(AIConstants.AI_TEMPERATURE), 0.0);
        return CopilotRequestService.createChatRequest(getModelName(), messages, temperature, sessionToken);
    }

    @Override
    protected AIEngineSettings getSettings() {
        return AISettingsRegistry.getInstance().getSettings().getEngineConfiguration(CopilotConstants.COPILOT_ENGINE);
    }

    private String acquireSessionToken(DBRProgressMonitor monitor) throws DBException {
        if (sessionToken == null) {
            String token = (String) getSettings().getProperties().get(CopilotConstants.COPILOT_ACCESS_TOKEN);
            try {
                sessionToken = CopilotAuthService.requestCopilotSessionToken(token, monitor);
            } catch (URISyntaxException | IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return sessionToken;
    }

    private record CopilotResult(Choice[] choices) {
        private record Choice(Message message) {
            private record Message(String content) {

            }
        }
    }
}
