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

import com.google.gson.annotations.SerializedName;
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
import java.util.stream.Collectors;

public class OAIMessage {
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_FUNCTION_CALL = "function_call";
    public static final String TYPE_FUNCTION_CALL_OUTPUT = "function_call_output";
    public static final String TYPE_FUNCTION_REASONING = "reasoning";

    public String id;
    public String type;
    public String status;
    public String role;
    public String name;
    public String arguments;
    @SerializedName("call_id")
    public String callId;
    public String output;
    public List<OAIMessageContent> content;

    public OAIMessage() {
    }

    public OAIMessage(@NotNull AIMessage msg) {
        this(msg, null);
    }

    public OAIMessage(@NotNull AIMessage msg, @Nullable String toolCallId) {
        if (msg.getFunctionCall() != null) {
            mapFunctionCall(msg.getFunctionCall());
            return;
        }
        if (msg.getFunctionCallName() != null && !CommonUtils.isEmpty(toolCallId)) {
            type = TYPE_FUNCTION_CALL_OUTPUT;
            callId = toolCallId;
            output = msg.getContent();
            return;
        }

        type = TYPE_MESSAGE;
        role = mapRole(msg.getRole());
        boolean input = switch (msg.getRole()) {
            case SYSTEM, USER -> true;
            default -> false;
        };
        content = List.of(new OAIMessageContent(input, msg.getContent()));
    }

    private void mapFunctionCall(@NotNull AIFunctionCall functionCall) {
        type = TYPE_FUNCTION_CALL;
        name = functionCall.getFunctionName();

        Map<String, Object> argumentsMap = functionCall.getArguments();
        arguments = argumentsMap == null ? "{}" : JSONUtils.GSON.toJson(argumentsMap);

        Map<String, String> additionalProperties = functionCall.getAdditionalProperties();
        if (additionalProperties != null) {
            callId = additionalProperties.get(OpenAIConstants.TOOL_RESULT_CALL_ID);
        }
    }

    @NotNull
    public String getFullText() {
        if (content == null) {
            return "";
        }
        return content.stream().map(c -> c.text).collect(Collectors.joining());
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
