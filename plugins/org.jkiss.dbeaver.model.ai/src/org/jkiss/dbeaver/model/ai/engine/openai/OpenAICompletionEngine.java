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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.ChatCompletionChunk;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.ChatCompletionRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.ChatCompletionResult;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.ChatMessage;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;
import java.util.concurrent.Flow;

public class OpenAICompletionEngine<PROPS extends OpenAIBaseProperties>
    extends BaseCompletionEngine {

    private final DisposableLazyValue<OpenAIClient, DBException> openAiService = new DisposableLazyValue<>() {
        @NotNull
        @Override
        protected OpenAIClient initialize() throws DBException {
            return createClient();
        }

        @Override
        protected void onDispose(@NotNull OpenAIClient disposedValue) {
            disposedValue.close();
        }
    };

    protected final PROPS properties;

    public OpenAICompletionEngine(PROPS properties) {
        this.properties = properties;
    }

    @NotNull
    @Override
    public List<AIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        return openAiService.getInstance().getModels(monitor)
            .stream()
            .map(model -> OpenAIModels.KNOWN_MODELS.getOrDefault(
                model.id(),
                new AIModel(model.id(), null, OpenAIModels.getModelFeatures(model.id()))
            ))
            .toList();
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
        ChatCompletionRequest ccr = new ChatCompletionRequest();
        ccr.setMessages(fromMessages(request.messages()));
        ccr.setTemperature(temperature());
        ccr.setFrequencyPenalty(0.0);
        ccr.setPresencePenalty(0.0);
        ccr.setMaxTokens(AIConstants.MAX_RESPONSE_TOKENS);
        ccr.setN(1);
        ccr.setModel(model());
        ccr.setStream(true);
        Flow.Publisher<ChatCompletionChunk> publisher = openAiService.getInstance().createChatCompletionStream(monitor, ccr);

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
    public int getContextWindowSize(DBRProgressMonitor monitor) throws DBException {
        Integer contextWindowSize = properties.getContextWindowSize();
        if (contextWindowSize != null) {
            return contextWindowSize;
        }

        throw new DBException("Context window size is not set for the model: " + model());
    }

    @Override
    public void close() throws DBException {
        openAiService.dispose();
    }

    @NotNull
    protected ChatCompletionResult complete(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<AIMessage> messages
    ) throws DBException {
        ChatCompletionRequest completionRequest = new ChatCompletionRequest();
        completionRequest.setMessages(fromMessages(messages));
        completionRequest.setTemperature(temperature());
        completionRequest.setFrequencyPenalty(0.0);
        completionRequest.setPresencePenalty(0.0);
        completionRequest.setMaxTokens(AIConstants.MAX_RESPONSE_TOKENS);
        completionRequest.setN(1);
        completionRequest.setModel(model());

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

    @NotNull
    protected OpenAIClient createClient() throws DBException {
        String token = properties.getToken();
        if (token == null || token.isEmpty()) {
            throw new DBException("OpenAI API token is not set");
        }

        return OpenAIClient.createClient(token);
    }

    @Nullable
    protected String model() throws DBException {
        return properties.getModel();
    }

    protected double temperature() throws DBException {
        return properties.getTemperature();
    }
}
