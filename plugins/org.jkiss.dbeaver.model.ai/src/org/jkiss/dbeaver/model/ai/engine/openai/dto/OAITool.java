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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;

import java.util.ArrayList;
import java.util.List;

public class OAITool {

    public static final String TYPE_FUNCTION = "function";

    public String type;
    public String name;
    public String description;
    public boolean strict;
    @NotNull
    public OAIToolParameters parameters = new OAIToolParameters();

    @NotNull
    public static OAITool fromDescriptor(@NotNull AIFunctionDescriptor fd) {
        OAITool tool = new OAITool();
        tool.type = OAITool.TYPE_FUNCTION;
        tool.name = fd.getFullId();
        tool.description = fd.getAiDescription();
        tool.parameters.type = OAIToolParameters.TYPE_OBJECT;
        List<String> requiredFields = new ArrayList<>();
        for (AIFunctionParameter param : fd.getParameters()) {
            OAIToolParameter tp = new OAIToolParameter();
            tp.type = param.getType();
            tp.description = param.getDescription();
            tp.enumItems = param.getValidValues();
            requiredFields.add(param.getName());
            tool.parameters.properties.put(param.getName(), tp);
        }
        tool.parameters.required = requiredFields.toArray(new String[0]);
        return tool;
    }

}
