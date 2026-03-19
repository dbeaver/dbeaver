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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.engine.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class OAIMessageFactory {
    private OAIMessageFactory() {
    }

    @NotNull
    public static OAIMessage fromAIMessage(@NotNull AIMessage msg) {
        return fromAIMessage(msg, null);
    }

    @NotNull
    public static OAIMessage fromAIMessage(@NotNull AIMessage msg, @Nullable String toolCallId) {
        if (msg.getFunctionCall() != null) {
            return fromFunctionCall(msg.getFunctionCall());
        }
        if (msg.getFunctionCallName() != null && !CommonUtils.isEmpty(toolCallId)) {
            return fromFunctionCallOutput(toolCallId, msg.getContent());
        }

        OAIMessage message = new OAIMessage();
        message.type = OAIMessage.TYPE_MESSAGE;
        message.role = mapRole(msg.getRole());
        boolean input = switch (msg.getRole()) {
            case SYSTEM, USER -> true;
            default -> false;
        };
        message.content = List.of(new OAIMessageContent(input, msg.getContent()));
        return message;
    }

    @NotNull
    public static OAIMessage fromFunctionCall(@NotNull AIFunctionCall functionCall) {
        OAIMessage message = new OAIMessage();
        message.type = OAIMessage.TYPE_FUNCTION_CALL;
        message.name = functionCall.getFunctionName();

        Map<String, Object> argumentsMap = functionCall.getArguments();
        message.arguments = argumentsMap == null ? "{}" : JSONUtils.GSON.toJson(argumentsMap);

        Map<String, String> additionalProperties = functionCall.getMessageMetadata();
        if (additionalProperties != null) {
            message.callId = additionalProperties.get(OpenAIConstants.TOOL_RESULT_CALL_ID);
        }
        return message;
    }

    @NotNull
    public static OAIMessage fromFunctionCallOutput(@NotNull String toolCallId, @NotNull String output) {
        OAIMessage message = new OAIMessage();
        message.type = OAIMessage.TYPE_FUNCTION_CALL_OUTPUT;
        message.callId = toolCallId;
        message.output = output;
        return message;
    }

    @Nullable
    private static String mapRole(@NotNull AIMessageType role) {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT, FUNCTION -> "assistant";
            default -> null;
        };
    }
}
