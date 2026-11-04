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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data editor features
 */
public interface AIBaseFeatures {

    DBRFeature SQL_AI_COMMAND = new DBRFeature("SQL_AI_COMMAND");

    String PARAM_ENGINE = "engine";
    String PARAM_DRIVER = "driver";
    String FUNCTION_NAME = "functionName";
    String PROMPT_TYPE = "promptType";

    DBRFeature AI_CHAT_FUNCTION_CALL = new DBRFeature("AI_CHAT_FUNCTION_CALL");

    @NotNull
    static Map<String, Object> buildFeatureParameters(
        @Nullable DBPDataSourceContainer container,
        @NotNull Map<String, Object> additionalInfo
    ) {
        AIConfigurationProfile profile = AISettingsManager.getInstance().getSettings().getDefaultConfigurationOrNull();
        String engineId = profile == null ? "" : profile.getEngineId();

        Map<String, Object> featureInfoMap = new LinkedHashMap<>(Map.of(
            PARAM_ENGINE, engineId
        ));
        if (container != null) {
            featureInfoMap.put(PARAM_DRIVER, container.getDriver().getPreconfiguredId());
        }
        featureInfoMap.putAll(additionalInfo);
        return featureInfoMap;
    }
}
