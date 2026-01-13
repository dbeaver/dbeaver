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
package org.jkiss.dbeaver.model.ai.engine.openai.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public abstract class OAIResponsesBase {

    public String[] include;

    @SerializedName("max_output_tokens")
    public Integer maxOutputTokens;
    @SerializedName("max_tool_calls")
    public Integer maxToolCalls;

    public Map<String, String> metadata;

    // Model ID used to generate the response, like gpt-4o or o3
    public String model;

    // Whether to allow the model to run tool calls in parallel.
    @SerializedName("parallel_tool_calls")
    public Boolean parallel_tool_calls;
    // The unique ID of the previous response to the model. Use this to create multi-turn conversations. Learn more about conversation state. Cannot be used in conjunction with conversation.
    @SerializedName("previous_response_id")
    public String previous_response_id;

    @SerializedName("prompt")
    public OAIResponsesPrompt prompt;

    @SerializedName("prompt_cache_key")
    public String promptCacheKey;

    @SerializedName("reasoning")
    public OAIResponsesReasoning reasoning;

    @SerializedName("safety_identifier")
    public String safetyIdentifier;

    @SerializedName("service_tier")
    public String serviceTier;
    @SerializedName("store")
    public Boolean store;

    @SerializedName("stream")
    public Boolean stream;

    @SerializedName("stream_options")
    public OAIResponsesStreamOptions stream_options;

    @SerializedName("temperature")
    public Double temperature;

    @SerializedName("text")
    public OAIResponsesText text;

    @SerializedName("tool_choice")
    public String toolChoice;

    @SerializedName("tools")
    public List<OAITool> tools;

    @SerializedName("top_logprobs")
    public Integer topLogprobs;

    @SerializedName("top_p")
    public Float topP;

    @SerializedName("truncation")
    public String truncation;

    @SerializedName("usage")
    public OAIUsage usage;
}
