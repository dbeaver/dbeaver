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
package org.jkiss.dbeaver.model.ai.engine.ollama.dto;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public record OllamaChatRequest(
    @NotNull String model,
    @NotNull List<OllamaChatMessage> messages,
    @Nullable List<OllamaTool> tools,
    boolean stream,
    @NotNull OllamaChatOptions options,
    boolean useServerDefaultTemperature
) {

    public OllamaChatRequest(
        @NotNull String model,
        @NotNull List<OllamaChatMessage> messages,
        @Nullable List<OllamaTool> tools,
        boolean stream,
        @NotNull OllamaChatOptions options
    ) {
        this(model, messages, tools, stream, options, false);
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private final List<OllamaChatMessage> messages = new ArrayList<>();
        private List<OllamaTool> tools;
        private boolean stream = false;
        private OllamaChatOptions options;
        private boolean useServerDefaultTemperature;

        @NotNull
        public Builder model(@NotNull String model) {
            this.model = model;
            return this;
        }

        @NotNull
        public Builder addMessage(@NotNull AIMessage daiMessage) {
            AIFunctionCall functionCall = daiMessage.getFunctionCall();
            if (functionCall != null) {
                messages.add(new OllamaChatMessage(
                    OllamaRole.ASSISTANT,
                    "",
                    List.of(new OllamaToolCall(functionCall)),
                    null
                ));
                if (daiMessage.getRole() == AIMessageType.FUNCTION) {
                    messages.add(new OllamaChatMessage(
                        OllamaRole.TOOL,
                        daiMessage.getContent(),
                        null,
                        functionCall.getFunctionName()
                    ));
                }
            } else {
                messages.add(new OllamaChatMessage(
                    OllamaRole.from(daiMessage.getRole()), daiMessage.getContent(), null, null,
                    daiMessage.getImages().isEmpty() ? null : daiMessage.getImages().stream().map(image -> image.data()).toList()));
            }
            return this;
        }

        @NotNull
        public Builder functions(@Nullable List<AIFunctionDescriptor> functions) {
            if (!CommonUtils.isEmpty(functions)) {
                this.tools = functions.stream().map(OllamaTool::new).toList();
            }
            return this;
        }

        @NotNull
        public Builder addMessages(@NotNull List<AIMessage> messageList) {
            for (AIMessage daiMessage : messageList) {
                addMessage(daiMessage);
            }
            return this;
        }

        @NotNull
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        @NotNull
        public Builder options(double temperature, int contextSize) {
            return optionsWithTemperature(Double.valueOf(temperature), contextSize);
        }

        @NotNull
        public Builder optionsWithTemperature(@Nullable Double temperature, int contextSize) {
            this.options = new OllamaChatOptions(temperature == null ? 0.0 : temperature, contextSize);
            this.useServerDefaultTemperature = temperature == null;
            return this;
        }

        @NotNull
        public OllamaChatRequest build() {
            if (model == null || options == null) {
                throw new IllegalStateException("Model and Options must be provided");
            }
            return new OllamaChatRequest(model, messages, tools, stream, options, useServerDefaultTemperature);
        }
    }

}
