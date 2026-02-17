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
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Data editor features
 */
public interface AIBaseFeatures {
    String PARAM_DRIVER = "driver";
    String PARAM_ENGINE = "engine";

    DBRFeature CATEGORY_SQL_AI = DBRFeature.createCategory("SQL AI", "SQL AI features");
    DBRFeature SQL_AI_COMMAND = DBRFeature.createFeature(CATEGORY_SQL_AI, "Generate AI SQL by @ai command");

    static Consumer<Map<String, String>> createStatCollector(
        @NotNull DBRFeature feature,
        @Nullable DBPDataSourceContainer container,
        @Nullable Map<String, Object> additionalInfo
    ) {
        return executionResultsMap -> {
            HashMap<String, Object> resultsMap = new HashMap<>();
            if (executionResultsMap != null) {
                resultsMap.putAll(executionResultsMap);
            }
            if (additionalInfo != null) {
                resultsMap.putAll(additionalInfo);
            }
            feature.use(
                container == null ? resultsMap : AIBaseFeatures.buildFeatureParameters(container, resultsMap));
        };
    }

    @NotNull
    static Map<String, Object> buildFeatureParameters(
        @NotNull DBPDataSourceContainer container,
        @NotNull Map<String, Object> additionalInfo
    ) {
        HashMap<String, Object> featureInfoMap = new HashMap<>(Map.of(
            PARAM_DRIVER, container.getDriver().getPreconfiguredId(),
            PARAM_ENGINE, AIUtils.getActiveEngineDescriptor() == null ? "" : AIUtils.getActiveEngineDescriptor().getId()
        ));
        featureInfoMap.putAll(additionalInfo);
        return featureInfoMap;
    }
}
