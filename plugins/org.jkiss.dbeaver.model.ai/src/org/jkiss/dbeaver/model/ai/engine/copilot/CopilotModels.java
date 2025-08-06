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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModels;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CopilotModels {
    private CopilotModels() {
    }

    public static final Map<String, AIModel> KNOWN_MODELS = Stream.of(
        new AIModel("claude-3.5-sonnet", 200_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("claude-3.7-sonnet", 200_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("claude-3.7-sonnet-thought", 200_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("claude-sonnet-4", 200_000, Set.of(AIModelFeature.CHAT)),

        new AIModel("gemini-2.5-pro", 1_000_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("gemini-2.0-flash-001", 1_000_000, Set.of(AIModelFeature.CHAT))
    ).collect(Collectors.toMap(
        AIModel::name,
        Function.identity()
    ));

    @Nullable
    public static Integer getContextWindowSize(@Nullable String model) {
        if (model == null) {
            return null;
        }

        AIModel copilotModel = KNOWN_MODELS.get(model);
        if (copilotModel != null) {
            return copilotModel.contextWindowSize();
        }

        AIModel openaiModel = OpenAIModels.KNOWN_MODELS.get(model);
        if (openaiModel != null) {
            return openaiModel.contextWindowSize();
        }

        return null;
    }
}
