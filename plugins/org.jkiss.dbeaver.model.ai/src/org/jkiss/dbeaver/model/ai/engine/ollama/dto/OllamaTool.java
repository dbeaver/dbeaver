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

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public record OllamaTool(@NotNull String type, @NotNull Function function) {
    public static final String TYPE_FUNCTION = "function";

    public OllamaTool(@NotNull AIFunctionDescriptor descriptor) {
        this(
            TYPE_FUNCTION,
            new Function(
                descriptor.getFullId(),
                descriptor.getAiDescription(),
                new Parameters(
                    Parameters.TYPE_OBJECT,
                    createProperties(descriptor),
                    Arrays.stream(descriptor.getParameters())
                        .filter(AIFunctionParameter::isRequired)
                        .map(AIFunctionParameter::getName)
                        .toArray(String[]::new)
                )
            )
        );
    }

    @NotNull
    private static Map<String, Parameters.Parameter> createProperties(@NotNull AIFunctionDescriptor descriptor) {
        Map<String, Parameters.Parameter> properties = new LinkedHashMap<>();
        for (AIFunctionParameter param : descriptor.getParameters()) {
            properties.put(
                param.getName(),
                new Parameters.Parameter(param.getType(), param.getDescription(), param.getValidValues())
            );
        }
        return properties;
    }

    public record Function(@NotNull String name, @Nullable String description, @NotNull Parameters parameters) {
    }

    public record Parameters(@NotNull String type, @NotNull Map<String, Parameter> properties, @NotNull String[] required) {
        public static final String TYPE_OBJECT = "object";

        public record Parameter(
            @NotNull String type,
            @Nullable String description,
            @SerializedName("enum") @Nullable String[] enumValues
        ) {
        }
    }
}
