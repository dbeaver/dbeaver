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
package org.jkiss.dbeaver.model.ai.engine.openai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public interface OpenAIBaseProperties extends AIEngineProperties {

    @Nullable
    String getBaseUrl();

    @Nullable
    String getToken();

    @Nullable
    default String getEffectiveToken() {
        return getToken();
    }

    @Nullable
    default String getEffectiveBaseUrl() {
        return getBaseUrl();
    }

    @NotNull
    default Map<String, String> getCustomHeaders() {
        return Map.of();
    }

    default boolean isTokenRequired() {
        return isDefaultBaseUrl(getEffectiveBaseUrl());
    }

    static boolean isDefaultBaseUrl(@Nullable String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return true;
        }
        String normalizedUrl = baseUrl.trim();
        return OpenAIClientResponses.OPENAI_ENDPOINT.equalsIgnoreCase(
            normalizedUrl.endsWith("/") ? normalizedUrl : normalizedUrl + "/"
        );
    }

    default boolean isStreamingEnabled() {
        return true;
    }

    @Override
    default boolean isValidConfiguration() {
        return (!isTokenRequired() || !CommonUtils.isEmptyTrimmed(getEffectiveToken()))
            && OpenAIRequestFilter.findInvalidHeader(getCustomHeaders()) == null;
    }

}
