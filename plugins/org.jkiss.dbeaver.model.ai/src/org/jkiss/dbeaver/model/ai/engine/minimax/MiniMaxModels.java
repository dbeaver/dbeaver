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
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Known MiniMax models
 */
public final class MiniMaxModels {

    private MiniMaxModels() {
    }

    public static final Map<String, AIModel> KNOWN_MODELS = AIUtils.modelMap(
        new AIModel(
            "MiniMax-M3", //$NON-NLS-1$
            512_000,
            Set.of(AIModelFeature.CHAT, AIModelFeature.STREAMING),
            MiniMaxConstants.DEFAULT_TEMPERATURE
        ),
        new AIModel(
            "MiniMax-M2.7", //$NON-NLS-1$
            204_800,
            Set.of(AIModelFeature.CHAT, AIModelFeature.STREAMING),
            MiniMaxConstants.DEFAULT_TEMPERATURE
        ),
        new AIModel(
            "MiniMax-M2.7-highspeed", //$NON-NLS-1$
            204_800,
            Set.of(AIModelFeature.CHAT, AIModelFeature.STREAMING),
            MiniMaxConstants.DEFAULT_TEMPERATURE
        )
    );

    @NotNull
    public static Optional<AIModel> getModelByName(@Nullable String modelName) {
        return AIUtils.getModelByName(KNOWN_MODELS, modelName);
    }
}
