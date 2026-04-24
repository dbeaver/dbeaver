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
package org.jkiss.dbeaver.model.ai.engine.copilot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatResponse;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public final class CopilotUtils {
    public static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .create();

    @NotNull
    public static AIFunctionCall createFunctionCall(
        @NotNull CopilotToolCallAccumulator accumulator
    ) throws DBException {
        Map<String, Object> arguments;
        try {
            arguments = GSON.fromJson(accumulator.arguments(), JSONUtils.MAP_TYPE_TOKEN);
        } catch (JsonSyntaxException e) {
            throw new DBException("Error parsing function call arguments", e);
        }

        Map<String, String> metadata = CommonUtils.isEmpty(accumulator.id()) ? null :
            Map.of(CopilotConstants.TOOL_RESULT_CALL_ID, accumulator.id());
        return new AIFunctionCall(CommonUtils.notEmpty(accumulator.name()), arguments, metadata);
    }

    @NotNull
    public static AIFunctionCall createFunctionCall(
        @NotNull CopilotChatResponse.ToolCall toolCall
    ) throws DBException {
        CopilotChatResponse.Function function = toolCall.function();
        if (function == null) {
            throw new DBException("Copilot tool call doesn't contain function payload");
        }

        CopilotToolCallAccumulator accumulator = new CopilotToolCallAccumulator();
        accumulator.setId(toolCall.id());
        accumulator.setName(function.name());
        if (function.arguments() != null) {
            accumulator.appendArguments(function.arguments());
        }
        return createFunctionCall(accumulator);
    }
}
