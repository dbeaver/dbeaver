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
package org.jkiss.dbeaver.model.ai.copilot.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record CopilotChatRequest(
    String model,
    boolean intent,
    List<CopilotMessage> messages,
    boolean stream,
    int n,
    @SerializedName("top_p") int topP,
    double temperature
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private boolean intent;
        private List<CopilotMessage> messages;
        private boolean stream;
        private int n;
        private int topP;
        private double temperature;

        public Builder withModel(String model) {
            this.model = model;
            return this;
        }

        public Builder withIntent(boolean intent) {
            this.intent = intent;
            return this;
        }

        public Builder withMessages(List<CopilotMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder withStream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder withN(int n) {
            this.n = n;
            return this;
        }

        public Builder withTopP(int topP) {
            this.topP = topP;
            return this;
        }

        public Builder withTemperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public CopilotChatRequest build() {
            return new CopilotChatRequest(model, intent, messages, stream, n, topP, temperature);
        }
    }
}
