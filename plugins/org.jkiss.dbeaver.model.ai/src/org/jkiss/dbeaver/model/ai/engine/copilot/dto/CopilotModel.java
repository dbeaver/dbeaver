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
package org.jkiss.dbeaver.model.ai.engine.copilot.dto;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public record CopilotModel(
    @SerializedName("name") @NotNull String name,
    @SerializedName("id") @NotNull String id,
    @SerializedName("model_picker_enabled") boolean modelPickerEnabled,
    @SerializedName("policy") @Nullable CopilotModelPolicy policy
) {

    public boolean isEnabled() {
        return modelPickerEnabled && (policy == null || policy.state() == null || policy.state().equals("enabled"));
    }

    public record CopilotModelPolicy(@SerializedName("state") @Nullable String state) {
    }
}

