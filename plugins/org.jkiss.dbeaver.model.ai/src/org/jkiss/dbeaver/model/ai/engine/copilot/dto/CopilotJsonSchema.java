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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;
import org.jkiss.dbeaver.model.ai.utils.JsonSchemaType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CopilotJsonSchema(
    @NotNull String type,
    @NotNull Map<String, Object> properties,
    @NotNull List<String> required,
    @NotNull Boolean additionalProperties,
    @NotNull Map<String, Object> defs,
    @NotNull Map<String, Object> definitions
) {
    @NotNull
    private static CopilotJsonSchema schemaFromDescriptor(@NotNull AIFunctionDescriptor descriptor) {
        Map<String, Object> properties = Arrays.stream(descriptor.getParameters())
            .collect(
                Collectors.toMap(
                    AIFunctionParameter::getName,
                    it -> new JsonSchemaType(
                        it.getType(),
                        it.getDescription(),
                        it.getValidValues(),
                        it.getDefaultValue()
                    )
                )
            );

        List<String> required = Arrays.stream(descriptor.getParameters()).filter(AIFunctionParameter::isRequired)
            .map(AIFunctionParameter::getName).toList();

        return new CopilotJsonSchema(
            "object",
            properties,
            required,
            false,
            Map.of(),
            Map.of()
        );
    }
}
