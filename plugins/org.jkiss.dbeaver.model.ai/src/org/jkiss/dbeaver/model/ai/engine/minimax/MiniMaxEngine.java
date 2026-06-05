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
package org.jkiss.dbeaver.model.ai.engine.minimax;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClient;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientLegacy;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIEngine;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * MiniMax AI engine. Uses the OpenAI-compatible chat completions API.
 */
public class MiniMaxEngine extends OpenAIEngine<MiniMaxProperties> {

    public MiniMaxEngine(@NotNull MiniMaxProperties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public List<AIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        return new ArrayList<>(MiniMaxModels.KNOWN_MODELS.values());
    }

    @Override
    public int getContextWindowSize(@NotNull DBRProgressMonitor monitor) throws DBException {
        Integer contextWindowSize = properties.getContextWindowSize();
        if (contextWindowSize != null) {
            return contextWindowSize;
        }
        // Default context window size for MiniMax models (M3 = 512K)
        return 512_000;
    }

    @NotNull
    @Override
    protected OpenAIClient createClient() throws DBException {
        String token = properties.getToken();
        if (token == null || token.isEmpty()) {
            throw new DBException("MiniMax API token is not set"); //$NON-NLS-1$
        }
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = MiniMaxConstants.MINIMAX_ENDPOINT;
        }
        // MiniMax uses OpenAI-compatible chat/completions API (legacy format)
        return OpenAIClientLegacy.createClient(baseUrl, token);
    }

    @Nullable
    @Override
    protected String model() throws DBException {
        String model = properties.getModel();
        if (model == null || model.isEmpty()) {
            return MiniMaxConstants.DEFAULT_MODEL;
        }
        return model;
    }

    @Override
    protected double temperature() throws DBException {
        return MiniMaxProperties.clampTemperature(properties.getTemperature());
    }

    /**
     * MiniMax uses the legacy chat/completions API which does not support
     * the OpenAI Responses API streaming format. Fall back to a synchronous
     * request and emit the result as a single chunk.
     */
    @Override
    public void requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        AIEngineResponse response = requestCompletion(monitor, request);
        List<String> variants = response.getVariants();
        if (variants != null && !variants.isEmpty()) {
            listener.nextChunk(new AIEngineResponseChunk(variants));
        }
        listener.usage(response.getUsage());
        listener.completeBlock();
    }
}
