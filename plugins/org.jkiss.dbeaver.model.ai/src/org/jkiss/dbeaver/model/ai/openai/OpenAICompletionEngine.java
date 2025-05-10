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
package org.jkiss.dbeaver.model.ai.openai;

import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AISettingsRegistry;
import org.jkiss.dbeaver.model.ai.completion.*;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;
import java.util.concurrent.Flow;

public class OpenAICompletionEngine implements DAICompletionEngine {
    private static final Log log = Log.getLog(OpenAICompletionEngine.class);

    private final DisposableLazyValue<OpenAIClient, DBException> openAiService = new DisposableLazyValue<>() {
        @Override
        protected OpenAIClient initialize() throws DBException {
            return createClient();
        }

        @Override
        protected void onDispose(OpenAIClient disposedValue) throws DBException {
            disposedValue.close();
        }
    };

    @Override
    public int getMaxContextSize(@NotNull DBRProgressMonitor monitor) {
        return OpenAISettings.INSTANCE.model().getMaxTokens();
    }

    @Override
    public DAICompletionResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException {
        ChatCompletionResult completionResult = complete(monitor, request.messages(), getMaxContextSize(monitor));
        List<DAICompletionChoice> choices = completionResult.getChoices().stream()
            .map(it -> new DAICompletionChoice(it.getMessage().getContent(), it.getFinishReason()))
            .toList();

        return new DAICompletionResponse(choices);
    }

    @Override
    public Flow.Publisher<DAICompletionChunk> requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException {
        Flow.Publisher<ChatCompletionChunk> publisher = openAiService.evaluate()
            .createChatCompletionStream(monitor, ChatCompletionRequest.builder()
                .messages(fromMessages(request.messages()))
                .temperature(temperature())
                .frequencyPenalty(0.0)
                .presencePenalty(0.0)
                .maxTokens(AIConstants.MAX_RESPONSE_TOKENS)
                .n(1)
                .model(model())
                .stream(true)
                .build()
            );

        return subscriber -> publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscriber.onSubscribe(subscription);
            }

            @Override
            public void onNext(ChatCompletionChunk item) {
                List<DAICompletionChoice> choices = item.getChoices().stream()
                    .filter(it -> it.getMessage() != null)
                    .takeWhile(it -> it.getMessage().getContent() != null)
                    .map(it -> new DAICompletionChoice(it.getMessage().getContent(), it.getFinishReason()))
                    .toList();

                subscriber.onNext(new DAICompletionChunk(choices));
            }

            @Override
            public void onError(Throwable throwable) {
                subscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        });
    }

    @Override
    public void onSettingsUpdate(AISettingsRegistry registry) {
        try {
            openAiService.dispose();
        } catch (DBException e) {
            log.error("Error disposing OpenAI service", e);
        }
    }

    @Override
    public boolean hasValidConfiguration() {
        return OpenAISettings.INSTANCE.isValidConfiguration();
    }

    @Override
    public boolean isLoggingEnabled() {
        return OpenAISettings.INSTANCE.isLoggingEnabled();
    }

    @NotNull
    protected ChatCompletionResult complete(
        @NotNull DBRProgressMonitor monitor,
        List<DAIChatMessage> messages,
        int maxTokens
    ) throws DBException {
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
            .messages(fromMessages(messages))
            .temperature(temperature())
            .frequencyPenalty(0.0)
            .presencePenalty(0.0)
            .maxTokens(AIConstants.MAX_RESPONSE_TOKENS)
            .n(1)
            .model(model())
            .build();

        return openAiService.evaluate().createChatCompletion(monitor, completionRequest);
    }

    private static List<ChatMessage> fromMessages(List<DAIChatMessage> messages) {
        return messages.stream()
            .map(m -> new ChatMessage(mapRole(m.role()), m.content()))
            .toList();
    }

    private static String mapRole(DAIChatRole role) {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
        };
    }

    protected OpenAIClient createClient() throws DBException {
        return new OpenAIClient(
            "https://api.openai.com/v1/",
            List.of(new OpenAIRequestFilter(OpenAISettings.INSTANCE.token()))
        );
    }

    protected String model() {
        return OpenAISettings.INSTANCE.model().getName();
    }

    protected double temperature() {
        return OpenAISettings.INSTANCE.temperature();
    }
}
