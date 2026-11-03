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
package org.jkiss.dbeaver.model.ai.engine.ollama;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.AIUsage;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.engine.ollama.dto.OllamaChatMessage;
import org.jkiss.dbeaver.model.ai.engine.ollama.dto.OllamaChatRequest;
import org.jkiss.dbeaver.model.ai.engine.ollama.dto.OllamaChatResponse;
import org.jkiss.dbeaver.model.ai.engine.ollama.dto.OllamaToolCall;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OllamaCompletionEngine extends BaseCompletionEngine<OllamaProperties> {
    private static final Log log = Log.getLog(OllamaCompletionEngine.class);

    public OllamaCompletionEngine(@NotNull OllamaProperties properties) {
        super(properties);
    }

    private final DisposableLazyValue<OllamaClient, DBException> client = new DisposableLazyValue<>() {
        @NotNull
        @Override
        protected OllamaClient initialize() throws DBException {
            OllamaClient newClient = createClient();
            newClient.setTimeout(properties.getTimeout());
            return newClient;
        }

        @Override
        protected void onDispose(@NotNull OllamaClient disposedValue) {
            disposedValue.close();
        }
    };

    @NotNull
    protected OllamaClient createClient() throws DBException {
        String hostname = properties.getEffectiveHostname();
        if (CommonUtils.isEmptyTrimmed(hostname)) {
            throw new DBException("Ollama host is not configured");
        }
        return new OllamaClient(hostname, properties.getEffectiveToken());
    }

    @NotNull
    @Override
    public List<AIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        OllamaClient ollamaClient = client.getInstance();
        List<AIModel> models = new ArrayList<>();
        for (var model : ollamaClient.loadModels(monitor)) {
            if (monitor.isCanceled()) {
                throw new DBException("Model metadata loading was canceled");
            }
            try {
                models.add(ollamaClient.getModelInformation(monitor, model.name()).toAIModel(model.name()));
            } catch (DBException e) {
                if (monitor.isCanceled()) {
                    throw e;
                }
                log.debug("Error reading Ollama model metadata: " + model.name(), e);
                models.add(new AIModel(model.name(), null, Set.of(AIModelFeature.CHAT)));
            }
        }
        return models;
    }

    @NotNull
    @Override
    public AIEngineResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        OllamaChatRequest chatRequest = OllamaChatRequest.builder()
            .addMessages(request.getMessages())
            .functions(request.getFunctions())
            .options(properties.getTemperature(), properties.getContextWindowSize())
            .model(properties.getEffectiveModel())
            .build();

        OllamaChatResponse chat = client.getInstance().chat(monitor, chatRequest);

        AIUsage usage = new AIUsage(chat.promptEvalCount(), 0, chat.evalCount(), 0);
        OllamaChatMessage message = chat.message();
        List<OllamaToolCall> toolCalls = message == null ? null : message.toolCalls();
        if (!CommonUtils.isEmpty(toolCalls)) {
            return new AIEngineResponse(toolCalls.getFirst().toFunctionCall(), usage);
        }

        return new AIEngineResponse(
            AIMessageType.ASSISTANT,
            List.of(message == null || message.content() == null ? "" : message.content()),
            usage
        );
    }

    @Override
    public void requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        OllamaChatRequest chatRequest = OllamaChatRequest.builder()
            .addMessages(request.getMessages())
            .functions(request.getFunctions())
            .options(properties.getTemperature(), properties.getContextWindowSize())
            .model(properties.getEffectiveModel())
            .stream(true)
            .build();

        client.getInstance().createChatCompletionStream(monitor, chatRequest, listener);
    }

    @Override
    public int getContextWindowSize(@NotNull DBRProgressMonitor monitor) throws DBException {
        return properties.getContextWindowSize();
    }

    @Override
    public void close() throws DBException {
        client.dispose();
    }
}
