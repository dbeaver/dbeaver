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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AISettingsRegistry;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionEngine;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionRequest;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionResponse;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotChatRequest;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotChatResponse;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotMessage;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotSessionToken;
import org.jkiss.dbeaver.model.ai.openai.OpenAIModel;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class CopilotCompletionEngine implements DAICompletionEngine {
    private static final Log log = Log.getLog(CopilotCompletionEngine.class);

    private final DisposableLazyValue<CopilotClient, DBException> client = new DisposableLazyValue<>() {
        @Override
        protected CopilotClient initialize() throws DBException {
            return new CopilotClient();
        }

        @Override
        protected void onDispose(CopilotClient disposedValue) throws DBException {
            disposedValue.close();
        }
    };

    private volatile CopilotSessionToken sessionToken;

    @Override
    public int getMaxContextSize(@NotNull DBRProgressMonitor monitor) {
        return OpenAIModel.getByName(CopilotSettings.INSTANCE.modelName()).getMaxTokens();
    }

    @Override
    public DAICompletionResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException {
        CopilotChatRequest chatRequest = CopilotChatRequest.builder()
            .withModel(CopilotSettings.INSTANCE.modelName())
            .withMessages(request.messages().stream().map(CopilotMessage::from).toList())
            .withTemperature(CopilotSettings.INSTANCE.temperature())
            .withStream(false)
            .withIntent(false)
            .withTopP(1)
            .withN(1)
            .build();

        CopilotChatResponse chatResponse = client.evaluate().chat(monitor, requestSessionToken(monitor).token(), chatRequest);
        return new DAICompletionResponse(chatResponse.choices().get(0).message().content());
    }

    @Override
    public boolean hasValidConfiguration() {
        return CopilotSettings.INSTANCE.isValidConfiguration();
    }

    @Override
    public boolean isLoggingEnabled() {
        return CopilotSettings.INSTANCE.isLoggingEnabled();
    }

    @Override
    public void onSettingsUpdate(AISettingsRegistry registry) {

        try {
            client.dispose();
        } catch (DBException e) {
            log.error("Error disposing client", e);
        }

        synchronized (this) {
            sessionToken = null;
        }
    }

    private CopilotSessionToken requestSessionToken(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (sessionToken != null) {
            return sessionToken;
        }

        synchronized (this) {
            if (sessionToken != null) {
                return sessionToken;
            }

            return client.evaluate().sessionToken(monitor, CopilotSettings.INSTANCE.accessToken());
        }
    }
}
