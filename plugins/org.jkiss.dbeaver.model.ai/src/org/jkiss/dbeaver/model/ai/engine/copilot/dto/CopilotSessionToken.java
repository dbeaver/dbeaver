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
package org.jkiss.dbeaver.model.ai.engine.copilot.dto;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.utils.CommonUtils;

@SuppressWarnings("checkstyle:RecordComponentName")
public record CopilotSessionToken(
    @SerializedName("token") String token,
    @SerializedName("endpoints") Endpoints endpoints
) {
    public static final String DEFAULT_API_ENDPOINT = "https://api.githubcopilot.com";

    @NotNull
    public String apiBaseUrl() {
        String api = endpoints != null ? endpoints.api() : null;
        if (CommonUtils.isEmpty(api)) {
            api = DEFAULT_API_ENDPOINT;
        }
        return api.endsWith("/") ? api.substring(0, api.length() - 1) : api;
    }

    public record Endpoints(@SerializedName("api") String api) {
    }
}
