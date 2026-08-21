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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIError;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesChunk;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.utils.CommonUtils;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for the ChatGPT account (subscription) backend.
 * <p>
 * That backend answers with an event stream even when a plain response was requested, and its final
 * {@code response.completed} event carries an empty output, so a non-streaming completion is assembled
 * from the {@code response.output_item.done} events.
 */
public class OpenAIAccountClient extends OpenAIClientResponses {
    private static final String EVENT_TYPE_ERROR = "error";

    public OpenAIAccountClient(@NotNull OpenAIProperties properties) {
        super(
            OpenAIAccountAuthenticator.CODEX_ENDPOINT,
            List.of(new OpenAIAccountRequestFilter(properties)),
            false
        );
    }

    @NotNull
    @Override
    protected HttpRequest createCompletionRequest(@NotNull OAIResponsesRequest completionRequest) throws DBException {
        completionRequest.stream = true;
        return super.createCompletionRequest(completionRequest);
    }

    @NotNull
    @Override
    protected OAIResponsesResponse parseCompletionResponse(@NotNull String responseBody) throws DBException {
        OAIResponsesResponse response = new OAIResponsesResponse();
        response.output = new ArrayList<>();

        for (String line : responseBody.split("\n")) {
            if (!line.startsWith(OpenAiAPIStreamConsumer.DATA_EVENT)) {
                continue;
            }
            String data = line.substring(OpenAiAPIStreamConsumer.DATA_EVENT.length()).trim();
            if (!data.startsWith("{")) {
                // stream terminators like "data: [DONE]" carry no payload
                continue;
            }
            OAIResponsesChunk chunk = GSON.fromJson(data, OAIResponsesChunk.class);
            if (EVENT_TYPE_ERROR.equals(chunk.type)) {
                // error events keep their payload at the top level: {"type":"error","code":...,"message":...}
                OAIError error = GSON.fromJson(data, OAIError.class);
                throw new DBException(CommonUtils.isEmpty(error.message) ? data : error.message);
            }
            if (OpenAiAPIStreamConsumer.EVENT_TYPE_ITEM_DONE.equals(chunk.type) && chunk.item != null) {
                response.output.add(chunk.item);
            } else if (OpenAiAPIStreamConsumer.EVENT_TYPE_RESPONSE_COMPLETED.equals(chunk.type) && chunk.response != null) {
                response.usage = chunk.response.usage;
            }
        }

        if (response.output.isEmpty()) {
            throw new DBException("Incomplete response from the ChatGPT account API");
        }
        return response;
    }
}
