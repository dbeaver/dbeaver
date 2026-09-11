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
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelCatalogEntry;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @NotNull
    public AIModel toAIModel() {
        CopilotModelLimits limits = capabilities == null ? null : capabilities.limits();
        CopilotModelSupports supports = capabilities == null ? null : capabilities.supports();
        Set<AIModelFeature> features = new HashSet<>();
        if (isChatModel()) {
            features.add(AIModelFeature.CHAT);
        }
        if (supports != null) {
            if (Boolean.TRUE.equals(supports.streaming())) {
                features.add(AIModelFeature.STREAMING);
            }
            if (Boolean.TRUE.equals(supports.toolCalls())) {
                features.add(AIModelFeature.TOOL_CALL);
            }
            if (Boolean.TRUE.equals(supports.vision())) {
                features.add(AIModelFeature.VISION);
            }
            if (Boolean.TRUE.equals(supports.thinking()) || Boolean.TRUE.equals(supports.adaptiveThinking())
                || supports.reasoningEffort() != null && !supports.reasoningEffort().isEmpty()
            ) {
                features.add(AIModelFeature.REASONING);
            }
        }
        return new AIModel(
            id,
            limits == null ? null : limits.contextWindowTokens(),
            Set.copyOf(features),
            0.0,
            limits == null ? null : limits.maxPromptTokens(),
            limits == null ? null : limits.maxOutputTokens(),
            null
        );
    }

    @NotNull
    public AIModel toAIModel(@Nullable AIModelCatalogEntry catalogEntry) {
        AIModel nativeModel = toAIModel();
        if (catalogEntry == null) {
            return nativeModel;
        }
        AIModel result = catalogEntry.enrich(nativeModel);
        Set<AIModelFeature> features = new HashSet<>(result.features());
        CopilotModelSupports supports = capabilities == null ? null : capabilities.supports();
        if (supports != null) {
            applyNativeSupport(features, AIModelFeature.STREAMING, supports.streaming());
            applyNativeSupport(features, AIModelFeature.TOOL_CALL, supports.toolCalls());
            applyNativeSupport(features, AIModelFeature.VISION, supports.vision());
            if (supports.thinking() != null || supports.adaptiveThinking() != null || supports.reasoningEffort() != null) {
                applyNativeSupport(features, AIModelFeature.REASONING, nativeModel.features().contains(AIModelFeature.REASONING));
            }
        }
        return new AIModel(
            result.name(), result.contextWindowSize(), Set.copyOf(features), result.defaultTemperature(),
            result.inputTokenLimit(), result.outputTokenLimit(), result.maxTemperature()
        );
    }

    private static void applyNativeSupport(
        @NotNull Set<AIModelFeature> features,
        @NotNull AIModelFeature feature,
        @Nullable Boolean supported
    ) {
        if (Boolean.TRUE.equals(supported)) {
            features.add(feature);
        } else if (Boolean.FALSE.equals(supported)) {
            features.remove(feature);
        }
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
        CopilotModelLimits limits,
        @Nullable
        CopilotModelSupports supports
    ) {
        public CopilotModelCapabilities(@Nullable String type, @Nullable CopilotModelLimits limits) {
            this(type, limits, null);
        }
    }

    public record CopilotModelSupports(
        @Nullable Boolean streaming,
        @SerializedName("tool_calls") @Nullable Boolean toolCalls,
        @Nullable Boolean vision,
        @Nullable Boolean thinking,
        @SerializedName("adaptive_thinking") @Nullable Boolean adaptiveThinking,
        @SerializedName("reasoning_effort") @Nullable List<String> reasoningEffort
    ) {
    }
}
