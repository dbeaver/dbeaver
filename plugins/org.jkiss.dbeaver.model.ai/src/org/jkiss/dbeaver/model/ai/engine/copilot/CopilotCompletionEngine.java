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
package org.jkiss.dbeaver.model.ai.engine.copilot;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatChunk;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatRequest;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotMessage;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotSessionToken;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Flow;

public class CopilotCompletionEngine implements AIEngine {
    private static final Log log = Log.getLog(CopilotCompletionEngine.class);

    private final DisposableLazyValue<CopilotClient, DBException> client = new DisposableLazyValue<>() {
        @NotNull
        @Override
        protected CopilotClient initialize() {
            return new CopilotClient();
        }

        @Override
        protected void onDispose(@NotNull CopilotClient disposedValue) {
            disposedValue.close();
        }
    };

    private final CopilotProperties properties;

    private volatile CopilotSessionToken sessionToken;

    public CopilotCompletionEngine(CopilotProperties properties) {
        this.properties = properties;
    }

    @NotNull
    @Override
    public List<AIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        return client.getInstance().loadModels(monitor, requestSessionToken(monitor).token()).stream()
            .map(model -> CopilotModels.KNOWN_MODELS.getOrDefault(
                model.id(),
                new AIModel(model.id(), null, Set.of())
            ))
            .toList();
    }

    @NotNull
    @Override
    public AIEngineResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        CopilotChatRequest chatRequest = CopilotChatRequest.builder()
            .withModel(getModelName())
            .withMessages(request.messages().stream().map(CopilotMessage::from).toList())
            .withTemperature(properties.getTemperature())
            .withStream(false)
            .withIntent(false)
            .withTopP(1)
            .withN(1)
            .build();

        List<String> choices = client.getInstance().chat(monitor, requestSessionToken(monitor).token(), chatRequest)
            .choices()
            .stream()
            .map(it -> it.message().content())
            .toList();

        return new AIEngineResponse(choices);
    }

    @NotNull
    @Override
    public Flow.Publisher<AIEngineResponseChunk> requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        CopilotChatRequest chatRequest = CopilotChatRequest.builder()
            .withModel(getModelName())
            .withMessages(request.messages().stream().map(CopilotMessage::from).toList())
            .withTemperature(properties.getTemperature())
            .withStream(true)
            .withIntent(false)
            .withTopP(1)
            .withN(1)
            .build();

        Flow.Publisher<CopilotChatChunk> chunkPublisher = client.getInstance().createChatCompletionStream(
            monitor,
            requestSessionToken(monitor).token(),
            chatRequest
        );


        return subscriber -> chunkPublisher.subscribe(
            new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(CopilotChatChunk chunk) {
                    List<String> choices = chunk.choices().stream()
                        .takeWhile(it -> it.delta().content() != null)
                        .map(it -> it.delta().content())
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
            }
        );
    }

    @Override
    public int getContextWindowSize(DBRProgressMonitor monitor) throws DBException {
        Integer contextWindowSize = properties.getContextWindowSize();
        if (contextWindowSize != null) {
            return contextWindowSize;
        }

        throw new DBException("Context window size is not defined in Copilot properties. " +
            "Please set it explicitly or use a known model with a predefined context window size.");
    }

    @Override
    public void close() throws DBException {
        client.dispose();
    }

    private CopilotSessionToken requestSessionToken(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (sessionToken != null) {
            return sessionToken;
        }

        synchronized (this) {
            if (sessionToken != null) {
                return sessionToken;
            }

            return client.getInstance().requestSessionToken(monitor, properties.getToken());
        }
    }

    public String getModelName() throws DBException {
        return CommonUtils.toString(
            properties.getModel(),
            OpenAIConstants.DEFAULT_MODEL
        );
    }
}
