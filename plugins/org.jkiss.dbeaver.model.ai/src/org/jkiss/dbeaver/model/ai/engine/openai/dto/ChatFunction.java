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
import org.jkiss.code.NotNull;

import java.util.function.Function;

public class ChatFunction {

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
    @SerializedName("parameters")
    private Class<?> parametersClass;

    private transient Function<Object, Object> executor;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private Class<?> parameters;
        private Function<Object, Object> executor;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public <T> Builder executor(Class<T> requestClass, Function<T, Object> executor) {
            this.parameters = requestClass;
            this.executor = (Function<Object, Object>) executor;
            return this;
        }

        public ChatFunction build() {
            ChatFunction chatFunction = new ChatFunction();
            chatFunction.name = name;
            chatFunction.description = description;
            chatFunction.parametersClass = parameters;
            chatFunction.executor = executor;
            return chatFunction;
        }
    }
}