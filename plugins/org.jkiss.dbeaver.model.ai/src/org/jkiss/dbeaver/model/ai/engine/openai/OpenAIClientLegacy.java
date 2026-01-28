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
package org.jkiss.dbeaver.model.ai.engine.openai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageMeta;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessage;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.legacy.ChatCompletionRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.legacy.ChatCompletionResult;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.legacy.ChatMessage;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class OpenAIClientLegacy extends OpenAIClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Gson GSON = new GsonBuilder().create();

    public OpenAIClientLegacy(
        @NotNull String baseUrl,
        @NotNull List<HttpRequestFilter> requestFilters
    ) {
        super(baseUrl, requestFilters);
    }

    @NotNull
    public static OpenAIClientLegacy createClient(@NotNull String baseUrl, @NotNull String token) {
        return new OpenAIClientLegacy(
            baseUrl,
            List.of(new OpenAIRequestFilter(token))
        );
    }

    @NotNull
    @Override
    public OAIResponsesResponse createChatCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OAIResponsesRequest completionRequest
    ) throws DBException {
        Instant now = Instant.now();
        ChatCompletionRequest chatRequest = new ChatCompletionRequest();
        chatRequest.setMessages(completionRequest.input.stream().map(om -> new ChatMessage(
            om.role,
            om.content.getFirst().text,
            om.name
        )).toList());
        chatRequest.setModel(completionRequest.model);
        if (completionRequest.temperature != null) {
            chatRequest.setTemperature(completionRequest.temperature);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(baseUrl, "chat/completions"))
            .POST(HttpRequest.BodyPublishers.ofString(serializeValue(chatRequest)))
            .timeout(TIMEOUT)
            .build();

        HttpRequest modifiedRequest = applyFilters(request);
        String response = client.send(monitor, modifiedRequest);
        ChatCompletionResult chatCompletionResult = GSON.fromJson(response, ChatCompletionResult.class);

        int systemPromptLength = completionRequest.input.stream()
            .filter(it -> it.role.toLowerCase(Locale.ROOT).equals("system"))
            .mapToInt(it -> it.content.getFirst().text.length())
            .sum();

        AIMessageMeta messageMeta = new AIMessageMeta(
            OpenAIConstants.OPENAI_ENGINE,
            completionRequest.model,
            chatCompletionResult.getAIUsage(),
            Duration.between(now, Instant.now()),
            systemPromptLength
        );

        OAIResponsesResponse oaiResponse = new OAIResponsesResponse();
        oaiResponse.output = chatCompletionResult.getChoices().stream().map(
            c -> new OAIMessage(
                new AIMessage(
                    AIMessageType.ASSISTANT,
                    c.getMessage().getContent(),
                    messageMeta
                )
            )).toList();
        return oaiResponse;
    }
}
