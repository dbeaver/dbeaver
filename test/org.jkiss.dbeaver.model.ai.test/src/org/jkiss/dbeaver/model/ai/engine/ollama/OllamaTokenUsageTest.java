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
package org.jkiss.dbeaver.model.ai.engine.ollama;

import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIUsage;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.ollama.dto.OllamaChatRequest;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class OllamaTokenUsageTest {
    private static final String RESPONSE = """
        {"model":"llama3","created_at":"2026-11-13T12:00:00Z",
         "message":{"role":"assistant","content":""},"done":true,
         "prompt_eval_count":37,"prompt_eval_duration":123456789,
         "eval_count":12,"eval_duration":987654321}
        """;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void streamingReportsUsageFromFinalChunk(boolean finalMessagePresent) throws Exception {
        String finalChunk = finalMessagePresent
            ? RESPONSE : RESPONSE.replace("\"message\":{\"role\":\"assistant\",\"content\":\"\"},", "");
        String response = """
            {"model":"llama3","created_at":"2026-11-13T12:00:00Z",
             "message":{"role":"assistant","content":"Hello"},"done":false}
            """.replace("\n", "") + "\n" + finalChunk.replace("\n", "") + "\n";
        String hostname = serve(response);
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        CountDownLatch completed = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            completed.countDown();
            return null;
        }).when(listener).completeBlock();

        try (OllamaClient client = new OllamaClient(hostname)) {
            client.createChatCompletionStream(Mockito.mock(DBRProgressMonitor.class), request(true), listener);
            Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS), "Stream did not complete");
        }

        Mockito.verify(listener).usage(new AIUsage(37, 0, 12, 0));
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
        ArgumentCaptor<AIEngineResponseChunk> chunks = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
        Mockito.verify(listener).nextChunk(chunks.capture());
        Assertions.assertEquals(List.of("Hello"), chunks.getValue().getChoices());
    }

    @Test
    void nonStreamingReportsUsage() throws Exception {
        String hostname = serve(RESPONSE);
        try (OllamaClient client = new OllamaClient(hostname)) {
            var response = client.chat(Mockito.mock(DBRProgressMonitor.class), request(false));
            Assertions.assertEquals(37, response.promptEvalCount());
            Assertions.assertEquals(12, response.evalCount());
        }
    }

    @NotNull
    private String serve(@NotNull String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @NotNull
    private static OllamaChatRequest request(boolean stream) {
        return OllamaChatRequest.builder().model("llama3").options(0, 4096).stream(stream).build();
    }
}
