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
package org.jkiss.dbeaver.model.ai.engine.openai;

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessage;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessageContent;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesChunk;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OpenAiAPIStreamConsumer implements Consumer<String> {
    public static final String EVENT_TYPE_TEXT_DELTA = "response.output_text.delta";
    protected static final Gson GSON = JSONUtils.GSON;
    private static final Log log = Log.getLog(OpenAiAPIStreamConsumer.class);
    private static final String DATA_EVENT = "data: ";
    private static final String EVENT_EVENT = "event: ";
    private static final String EVENT_TYPE_RESPONSE_COMPLETED = "response.completed";
    private static final String EVENT_TYPE_ITEM_DONE = "response.output_item.done";
    private static final String EVENT_TYPE_ARGUMENTS_DELTA = "response.function_call_arguments.delta";
    private final AIEngineResponseConsumer listener;
    private boolean functionCall;

    public OpenAiAPIStreamConsumer(@NotNull AIEngineResponseConsumer listener) {
        this.listener = listener;
    }

    @Override
    public void accept(@Nullable String event) {
        if (CommonUtils.isEmpty(event)) {
            return;
        }
        if (event.startsWith(DATA_EVENT)) {
            String data = event.substring(DATA_EVENT.length()).trim();
            try {
                OAIResponsesChunk chunk = GSON.fromJson(data, OAIResponsesChunk.class);
                if (chunk.error != null) {
                    listener.error(new DBException(chunk.error.code + ": " + chunk.error.message));
                    return;
                }
                if (EVENT_TYPE_RESPONSE_COMPLETED.equals(chunk.type)) {
                    listener.usage(chunk.response.getAIUsage());
                } else {

                    if (chunk.item != null && OAIMessage.TYPE_FUNCTION_CALL.equals(chunk.item.type)) {
                        if (EVENT_TYPE_ITEM_DONE.equals(chunk.type)) {
                            listener.nextChunk(new AIEngineResponseChunk(
                                OpenAiUtils.createFunctionCall(chunk.item)));
                            functionCall = false;
                        } else {
                            functionCall = true;
                        }
                        return;
                    }
                    if (!functionCall) {
                        List<String> choices = processChunk(chunk);

                        if (!choices.isEmpty()) {
                            listener.nextChunk(new AIEngineResponseChunk(choices));
                        }
                    }
                }
            } catch (Exception e) {
                listener.error(e);
            }
        } else if (event.startsWith(EVENT_EVENT)) {
            String eventType = event.substring(EVENT_EVENT.length()).trim();
            if (!CommonUtils.isEmpty(eventType)) {
                switch (eventType) {
                    case "response.created":
                    case "response.in_progress":
                    case "response.output_item.added":
                    case EVENT_TYPE_TEXT_DELTA:
                    case "response.output_text.done":
                    case "response.content_part.done":
                    case "response.output_item.done":
                    case EVENT_TYPE_RESPONSE_COMPLETED, "error":
                        break;
                    default:
                        log.debug("Unknown OpenAI event type: " + eventType);
                }
            }
        } else {
            log.debug("Unknown OpenAI event: " + event);
        }
    }

    @NotNull
    private static List<String> processChunk(@NotNull OAIResponsesChunk chunk) {
        List<String> choices = new ArrayList<>();
        if (EVENT_TYPE_TEXT_DELTA.equals(chunk.type)) {
            choices.add(chunk.delta);
        } else if (chunk.response != null) {
            for (OAIMessage msg : chunk.response.output) {
                for (OAIMessageContent content : msg.content) {
                    if (!CommonUtils.isEmpty(content.text)) {
                        choices.add(content.text);
                    }
                }
            }
        }
        return choices;
    }
}
