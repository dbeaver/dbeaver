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
package org.jkiss.dbeaver.model.ai.engine.openai;

import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.registry.AISettingsRegistry;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;
import java.util.concurrent.Flow;

public class OpenAICompletionEngine<PROPS extends OpenAIBaseProperties> extends BaseCompletionEngine<PROPS> {
    private static final Log log = Log.getLog(OpenAICompletionEngine.class);
    public static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/";

    private final DisposableLazyValue<OpenAIClient, DBException> openAiService = new DisposableLazyValue<>() {
        @NotNull
        @Override
        protected OpenAIClient initialize() throws DBException {
            return createClient();
        }

        @Override
        protected void onDispose(OpenAIClient disposedValue) {
            disposedValue.close();
        }
    };

    public OpenAICompletionEngine(AISettingsRegistry registry) {
        super(registry);
    }

    @Override
    public int getMaxContextSize(@NotNull DBRProgressMonitor monitor) throws DBException {
        return OpenAIModel.getByName(getProperties().getModel()).getMaxTokens();
    }

    @NotNull
    @Override
    public AIEngineResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        ChatCompletionResult completionResult = complete(monitor, request.messages());
        List<String> choices = completionResult.getChoices().stream()
            .map(it -> it.getMessage().getContent())
            .toList();

        return new AIEngineResponse(choices);
    }

    @NotNull
    @Override
    public Flow.Publisher<AIEngineResponseChunk> requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        Flow.Publisher<ChatCompletionChunk> publisher = openAiService.getInstance()
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
                List<String> choices = item.getChoices().stream()
                    .filter(it -> it.getMessage() != null)
                    .takeWhile(it -> it.getMessage().getContent() != null)
                    .map(it -> it.getMessage().getContent())
                    .toList();

                subscriber.onNext(new AIEngineResponseChunk(choices));
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
    public void onSettingsUpdate(@NotNull AISettingsRegistry registry) {
        try {
            openAiService.dispose();
        } catch (DBException e) {
            log.error("Error disposing OpenAI service", e);
        }
    }

    @NotNull
    protected ChatCompletionResult complete(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<AIMessage> messages
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

        return openAiService.getInstance().createChatCompletion(monitor, completionRequest);
    }

    @NotNull
    private static List<ChatMessage> fromMessages(@NotNull List<AIMessage> messages) {
        return messages.stream()
            .map(m -> new ChatMessage(mapRole(m.getRole()), m.getContent()))
            .toList();
    }

    private static String mapRole(AIMessageType role) {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
            default -> null;
        };
    }

    protected OpenAIClient createClient() throws DBException {
        return new OpenAIClient(
            OPENAI_ENDPOINT,
            List.of(new OpenAIRequestFilter(getProperties().getToken()))
        );
    }

    protected String model() throws DBException {
        return OpenAIModel.getByName(getProperties().getModel()).getName();
    }

    protected double temperature() throws DBException {
        return getProperties().getTemperature();
    }

    @Override
    protected PROPS getProperties() throws DBException {
        return registry.getSettings().<LegacyAISettings<PROPS>> getEngineConfiguration(OpenAIConstants.OPENAI_ENGINE)
            .getProperties();
    }
}
