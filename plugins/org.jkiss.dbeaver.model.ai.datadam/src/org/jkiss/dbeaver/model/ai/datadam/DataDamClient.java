/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.datadam;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientChat;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientResponses;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

// gateway speaks only /v1/chat/completions, so route responses-api calls to the chat client
public class DataDamClient extends OpenAIClientResponses {

    private final OpenAIClientChat chatClient;

    public DataDamClient(@NotNull String baseUrl, @NotNull List<HttpRequestFilter> requestFilters) {
        super(baseUrl, requestFilters);
        this.chatClient = new OpenAIClientChat(baseUrl, requestFilters);
    }

    @NotNull
    @Override
    public OAIResponsesResponse createChatCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OAIResponsesRequest completionRequest
    ) throws DBException {
        return chatClient.createChatCompletion(monitor, completionRequest);
    }

    @Override
    public void createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OAIResponsesRequest completionRequest,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        chatClient.createChatCompletionStream(monitor, completionRequest, listener);
    }

    @Override
    public void close() {
        super.close();
        chatClient.close();
    }
}
