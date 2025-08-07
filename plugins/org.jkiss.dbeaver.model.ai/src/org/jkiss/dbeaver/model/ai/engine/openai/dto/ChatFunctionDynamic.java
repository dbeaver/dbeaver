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

import org.jkiss.code.NotNull;


public class ChatFunctionDynamic {

    /**
     * The name of the function being called.
     */
    @NotNull
    private String name;

    /**
     * A description of what the function does, used by the model to choose when and how to call the function.
     */
    private String description;

    /**
     * The parameters the functions accepts.
     */
    private ChatFunctionParameters parameters;

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ChatFunctionParameters getParameters() {
        return parameters;
    }

    public void setParameters(ChatFunctionParameters parameters) {
        this.parameters = parameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private ChatFunctionParameters
            parameters = new ChatFunctionParameters();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parameters(ChatFunctionParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder addProperty(ChatFunctionProperty property) {
            this.parameters.addProperty(property);
            return this;
        }

        public ChatFunctionDynamic build() {
            ChatFunctionDynamic chatFunction = new ChatFunctionDynamic();
            chatFunction.name = name;
            chatFunction.description = description;
            chatFunction.parameters = parameters;
            return chatFunction;
        }
    }
}
