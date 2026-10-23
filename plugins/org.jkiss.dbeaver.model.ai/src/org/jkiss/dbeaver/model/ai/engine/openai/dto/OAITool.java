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
package org.jkiss.dbeaver.model.ai.engine.openai.dto;

import com.google.gson.annotations.JsonAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;
import org.jkiss.dbeaver.model.ai.utils.JsonSchemaAdapter;

import java.util.ArrayList;
import java.util.List;

public class OAITool {

    public static final String TYPE_FUNCTION = "function";

    public String type;
    public String name;
    public String description;
    @JsonAdapter(JsonSchemaAdapter.class)
    public Object parameters;

    public OAITool() {
    }

    @NotNull
    public static OAITool fromDescriptor(@NotNull AIFunctionDescriptor fd) {
        OAITool tool = new OAITool();
        tool.type = OAITool.TYPE_FUNCTION;
        tool.name = fd.getFullId();
        tool.description = fd.getAiDescription();
        if (fd.getInputSchema() != null) {
            tool.parameters = fd.getInputSchema();
        } else if (fd.getParameters().length > 0) {
            OAIToolParameters parameters = new OAIToolParameters();
            parameters.type = OAIToolParameters.TYPE_OBJECT;
            List<String> requiredFields = new ArrayList<>();
            for (AIFunctionParameter param : fd.getParameters()) {
                OAIToolParameter tp = new OAIToolParameter();
                tp.type = param.getType();
                tp.description = param.getDescription();
                tp.enumItems = param.getValidValues();
                if (param.isRequired()) {
                    requiredFields.add(param.getName());
                }
                parameters.properties.put(param.getName(), tp);
            }
            if (!requiredFields.isEmpty()) {
                parameters.required = requiredFields.toArray(new String[0]);
            }
            tool.parameters = parameters;
        }
        return tool;
    }

}
