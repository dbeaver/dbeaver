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
import org.jkiss.code.Nullable;

import java.util.List;

public record CopilotModel(
    @SerializedName("name") @NotNull String name,
    @SerializedName("id") @NotNull String id,
    @SerializedName("model_picker_enabled") boolean modelPickerEnabled,
    @SerializedName("policy") @Nullable CopilotModelPolicy policy,
    @SerializedName("capabilities") @Nullable CopilotModelCapabilities capabilities,
    @SerializedName("supported_endpoints") @Nullable List<String> supportedEndpoints
) {

    private static final String MODEL_TYPE_CHAT = "chat";
    private static final String POLICY_STATE_ENABLED = "enabled";

    public boolean isChatModel() {
        return capabilities == null || capabilities.type() == null || MODEL_TYPE_CHAT.equals(capabilities.type());
    }

    public boolean isDisabledByPolicy() {
        return policy != null && policy.state() != null && !POLICY_STATE_ENABLED.equals(policy.state());
    }

    public boolean declaresEndpoints() {
        return supportedEndpoints != null && !supportedEndpoints.isEmpty();
    }

    public record CopilotModelPolicy(@SerializedName("state") @Nullable String state) {
    }

    public record CopilotModelLimits(
        @SerializedName("max_context_window_tokens")
        Integer contextWindowTokens,
        @SerializedName("max_output_tokens")
        Integer maxOutputTokens,
        @SerializedName("max_prompt_tokens")
        Integer maxPromptTokens
    ) {
    };

    public record CopilotModelCapabilities(
        @Nullable
        String type,
        @Nullable
        CopilotModelLimits limits
    ) {
    }
}

