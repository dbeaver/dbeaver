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
import org.jkiss.dbeaver.model.ai.LegacyAISettings;
import org.jkiss.dbeaver.model.ai.completion.*;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotChatChunk;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotChatRequest;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotMessage;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotSessionToken;
import org.jkiss.dbeaver.model.ai.openai.OpenAIModel;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.concurrent.Flow;

public class CopilotCompletionEngine implements DAICompletionEngine {
    private static final Log log = Log.getLog(CopilotCompletionEngine.class);

    private final AISettingsRegistry registry;
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

    public CopilotCompletionEngine(AISettingsRegistry registry) {
        this.registry = registry;
    }

    @Override
    public int getMaxContextSize(@NotNull DBRProgressMonitor monitor) throws DBException {
        return OpenAIModel.getByName(getModelName()).getMaxTokens();
    }

    @Override
    public DAICompletionResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException {
        CopilotChatRequest chatRequest = CopilotChatRequest.builder()
            .withModel(getModelName())
            .withMessages(request.messages().stream().map(CopilotMessage::from).toList())
            .withTemperature(getProperties().getTemperature())
            .withStream(false)
            .withIntent(false)
            .withTopP(1)
            .withN(1)
            .build();

        List<DAICompletionChoice> choices = client.evaluate().chat(monitor, requestSessionToken(monitor).token(), chatRequest)
            .choices()
            .stream()
            .map(it -> new DAICompletionChoice(it.message().content(), null))
            .toList();

        return new DAICompletionResponse(choices);
    }

    @Override
    public Flow.Publisher<DAICompletionChunk> requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException {
        CopilotChatRequest chatRequest = CopilotChatRequest.builder()
            .withModel(getModelName())
            .withMessages(request.messages().stream().map(CopilotMessage::from).toList())
            .withTemperature(getProperties().getTemperature())
            .withStream(true)
            .withIntent(false)
            .withTopP(1)
            .withN(1)
            .build();

        Flow.Publisher<CopilotChatChunk> chunkPublisher = client.evaluate().createChatCompletionStream(
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
                    List<DAICompletionChoice> choices = chunk.choices().stream()
                        .takeWhile(it -> it.delta().content() != null)
                        .map(it -> new DAICompletionChoice(it.delta().content(), null))
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
            }
        );
    }

    @Override
    public boolean hasValidConfiguration() throws DBException {
        return getProperties().isValidConfiguration();
    }

    @Override
    public boolean isLoggingEnabled() throws DBException {
        return getProperties().isLoggingEnabled();
    }

    @Override
    public void onSettingsUpdate(@NotNull AISettingsRegistry registry) {

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

            return client.evaluate().sessionToken(monitor, getProperties().getToken());
        }
    }

    public String getModelName() throws DBException {
        return CommonUtils.toString(
            getProperties().getModel(),
            OpenAIModel.GPT_TURBO.getName()
        );
    }

    private CopilotProperties getProperties() throws DBException {
        return registry.getSettings().<LegacyAISettings<CopilotProperties>> getEngineConfiguration(
            CopilotConstants.COPILOT_ENGINE
        ).getProperties();
    }
}
