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
package org.jkiss.dbeaver.model.ai.internal;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

import java.util.HashMap;
import java.util.Map;

public interface AIFeatures {
    String PARAM_ENGINE = "engine";
    String PARAM_DRIVER = "driver";
    String FUNCTION_NAME = "functionName";

    DBRFeature CATEGORY_AI_CALLS = DBRFeature.createCategory("AI Chat", "AI Chat features");


    DBRFeature AI_CHAT_FUNCTION_CALL = DBRFeature.createFeature(CATEGORY_AI_CALLS, "AI Chat function call");

    @NotNull
    static Map<String, Object> buildFeatureParameters(
        @Nullable DBPDataSourceContainer container,
        @NotNull Map<String, Object> additionalInfo
    ) {
        HashMap<String, Object> featureInfoMap = new HashMap<>(Map.of(
            PARAM_ENGINE, AIUtils.getActiveEngineDescriptor() == null ? "" : AIUtils.getActiveEngineDescriptor().getId()
        ));
        if (container != null) {
            featureInfoMap.put(PARAM_DRIVER, container.getDriver().getPreconfiguredId());
        }
        featureInfoMap.putAll(additionalInfo);
        return featureInfoMap;
    }
    
}
