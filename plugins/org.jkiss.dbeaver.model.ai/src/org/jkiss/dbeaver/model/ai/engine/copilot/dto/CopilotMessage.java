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

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotConstants;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public record CopilotMessage(
    @Nullable String role,
    @Nullable String content,
    @Nullable
    @SerializedName("tool_calls")
    List<CopilotChatResponse.ToolCall> toolCalls,
    @Nullable
    @SerializedName("tool_call_id")
    String toolCallId
) {
    private static final String USER_ROLE = "user";
    private static final String ASSISTANT_ROLE = "assistant";
    private static final String TOOL_ROLE = "tool";
    private static final String SYSTEM_ROLE = "system";

    @NotNull
    public static List<CopilotMessage> from(@NotNull AIMessage message) {
        AIFunctionCall functionCall = message.getFunctionCall();
        if (functionCall != null) {
            String callId = CommonUtils.toString(functionCall.getMessageMetadata().get(CopilotConstants.TOOL_RESULT_CALL_ID));
            if (CommonUtils.isNotEmpty(callId)) {
                List<CopilotMessage> result = new ArrayList<>();
                // Assistant message with tool call
                result.add(new CopilotMessage(
                    ASSISTANT_ROLE,
                    null,
                    List.of(new CopilotChatResponse.ToolCall(
                        0,
                        callId,
                        "function",
                        new CopilotChatResponse.Function(
                            functionCall.getFunctionName(),
                            JSONUtils.GSON.toJson(functionCall.getArguments())
                        )
                    )),
                    null
                ));
                if (message.getRole() == AIMessageType.FUNCTION) {
                    // Tool message with result
                    result.add(new CopilotMessage(
                        TOOL_ROLE,
                        message.getContent(),
                        null,
                        callId
                    ));
                }
                return result;
            }
        }
        return List.of(new CopilotMessage(mapRole(message.getRole()), message.getContent(), null, null));
    }

    @Nullable
    private static String mapRole(@NotNull AIMessageType role) {
        return switch (role) {
            case USER -> USER_ROLE;
            case ASSISTANT -> ASSISTANT_ROLE;
            case SYSTEM -> SYSTEM_ROLE;
            case FUNCTION -> TOOL_ROLE;
            default -> null;
        };
    }
}
