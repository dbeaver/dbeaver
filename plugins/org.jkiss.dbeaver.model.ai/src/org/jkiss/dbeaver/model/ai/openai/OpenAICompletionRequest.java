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
package org.jkiss.dbeaver.model.ai.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;

import java.util.List;
import java.util.Map;

public record OpenAICompletionRequest(
    String model,
    List<ChatMessage> messages,
    Double temperature,
    @JsonProperty("top_p")
    Double topP,
    Integer n,
    Boolean stream,
    List<String> stop,
    @JsonProperty("max_tokens")
    Integer maxTokens,
    @JsonProperty("presence_penalty")
    Double presencePenalty,
    @JsonProperty("frequency_penalty")
    Double frequencyPenalty,
    @JsonProperty("logit_bias")
    Map<String, Integer> logitBias,
    String user,
    List<?> functions,
    @JsonProperty("function_call")
    ChatCompletionRequest.ChatCompletionRequestFunctionCall functionCall,
    @JsonProperty("response_format")
    JsonNode responseFormat
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private List<ChatMessage> messages;
        private Double temperature;
        private Double topP;
        private Integer n;
        private Boolean stream;
        private List<String> stop;
        private Integer maxTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private String user;
        private List<?> functions;
        private ChatCompletionRequest.ChatCompletionRequestFunctionCall functionCall;
        private JsonNode responseFormat;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder functions(List<?> functions) {
            this.functions = functions;
            return this;
        }

        public Builder functionCall(ChatCompletionRequest.ChatCompletionRequestFunctionCall functionCall) {
            this.functionCall = functionCall;
            return this;
        }

        public Builder responseFormat(JsonNode responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public OpenAICompletionRequest build() {
            return new OpenAICompletionRequest(
                model,
                messages,
                temperature,
                topP,
                n,
                stream,
                stop,
                maxTokens,
                presencePenalty,
                frequencyPenalty,
                logitBias,
                user,
                functions,
                functionCall,
                responseFormat
            );
        }
    }
}
