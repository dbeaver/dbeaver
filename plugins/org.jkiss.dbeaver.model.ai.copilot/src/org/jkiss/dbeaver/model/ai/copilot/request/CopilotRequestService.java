/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.copilot.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionMessage;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.List;

public class CopilotRequestService {

    /**
     * Create a request to the chat endpoint
     *
     * @param model model name
     * @param messages list of messages
     * @param temperature temperature
     * @param token token
     * @return HttpRequest
     * @throws DBCException on error
     */
    public static HttpRequest createChatRequest(
        String model,
        List<DAICompletionMessage> messages,
        Double temperature,
        String token
    ) throws DBCException {
        String requestJson = createRequestJson(model, messages, temperature);
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.header("Content-type", "application/json"); //$NON-NLS-1$
        try {
            URI uri = new URI("https://api.githubcopilot.com").resolve("/chat/completions");
            builder.uri(uri);
            builder.header("authorization", "Bearer " + token);
            builder.header("Editor-Version", "vscode/1.80.1"); // TODO replace after partnership

            builder.POST(HttpRequest.BodyPublishers.ofString(requestJson));
        } catch (URISyntaxException e) {
            throw new DBCException("Incorrect URI", e);
        }
        return builder.build();
    }

    private static String createRequestJson(
        String model,
        List<DAICompletionMessage> messages,
        Double temperature
    ) throws DBCException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("model", model);
        jsonObject.addProperty("intent", false);
        JsonArray elements = new JsonArray();
        jsonObject.add("messages", elements); //$NON-NLS-1$
        for (DAICompletionMessage daiCompletionMessage : messages) {
            JsonObject messageJson = getJsonObject(daiCompletionMessage);
            elements.add(messageJson);
        }
        jsonObject.addProperty("stream", false);
        jsonObject.addProperty("n", 1); //$NON-NLS-1$
        jsonObject.addProperty("top_p", 1); //$NON-NLS-1$

        jsonObject.addProperty("temperature", temperature); //$NON-NLS-1$
        return jsonObject.toString();
    }

    @NotNull
    private static JsonObject getJsonObject(DAICompletionMessage daiCompletionMessage) throws DBCException {
        JsonObject messageJson = new JsonObject();
        DAICompletionMessage.Role role = daiCompletionMessage.getRole();
        switch (role) {
            case USER ->
                messageJson.addProperty("role", "user");
            case ASSISTANT ->
                messageJson.addProperty("role", "assistant");
            case SYSTEM ->
                messageJson.addProperty("role", "system");
            default -> throw new DBCException("Unexpected value: " + role);
        }
        messageJson.addProperty("content", daiCompletionMessage.getContent()); //$NON-NLS-1$
        return messageJson;
    }
}
