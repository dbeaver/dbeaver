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

import java.util.List;

public record CopilotChatRequest(
    @NotNull String model,
    boolean intent,
    @NotNull List<CopilotMessage> messages,
    @NotNull List<CopilotFunction> tools,
    boolean stream,
    @SerializedName("n") int responseCount,
    @SerializedName("top_p") int topP,
    double temperature
) {
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        @NotNull
        private String model;
        private boolean intent;
        @NotNull
        private List<CopilotMessage> messages;
        @NotNull
        private List<CopilotFunction> tools;
        private boolean stream;
        private int responseCount;
        private int topP;
        private double temperature;

        @NotNull
        public Builder withModel(@NotNull String model) {
            this.model = model;
            return this;
        }

        @NotNull
        public Builder withIntent(boolean intent) {
            this.intent = intent;
            return this;
        }

        @NotNull
        public Builder withMessages(@NotNull List<CopilotMessage> messages) {
            this.messages = messages;
            return this;
        }

        @NotNull
        public Builder withTools(@NotNull List<CopilotFunction> tools) {
            this.tools = tools;
            return this;
        }

        @NotNull
        public Builder withStream(boolean stream) {
            this.stream = stream;
            return this;
        }

        @NotNull
        public Builder withN(int responseCount) {
            this.responseCount = responseCount;
            return this;
        }

        @NotNull
        public Builder withTopP(int topP) {
            this.topP = topP;
            return this;
        }

        @NotNull
        public Builder withTemperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        @NotNull
        public CopilotChatRequest build() {
            return new CopilotChatRequest(model, intent, messages, tools, stream, responseCount, topP, temperature);
        }
    }
}
