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

import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponse;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class OpenAIResponsesTest {
    private static final String REASONING = """
        {"id":"rs_1","type":"reasoning","summary":[],"encrypted_content":"opaque-reasoning"}
        """.strip();
    private static final String CALL = """
        {"type":"function_call","call_id":"call_1","name":"db_listTables","arguments":"{}"}
        """.strip();
    private static final String TEXT = """
        {"type":"message","role":"assistant","content":[{"type":"output_text","text":"Checking tables"}]}
        """.strip();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void returnsEverySynchronousFunctionCall(boolean textFirst) throws Exception {
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.createChatCompletion(Mockito.any(), Mockito.any())).thenReturn(response(
            "completed", (textFirst ? TEXT + "," : "") + CALL + "," + CALL.replace("call_1", "call_2")));
        try (OpenAIEngine<OpenAIProperties> engine = engine(client)) {
            AIEngineResponse response = engine.requestCompletion(new VoidProgressMonitor(), request());
            Assertions.assertEquals(List.of("call_1", "call_2"), response.getFunctionCalls().stream()
                .map(call -> call.getMessageMetadata().get(OpenAIConstants.TOOL_RESULT_CALL_ID)).toList());
        }
    }

    @Test
    void replaysEncryptedReasoningAfterSavingParallelToolResults() throws Exception {
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.createChatCompletion(Mockito.any(), Mockito.any())).thenReturn(response(
            "completed", REASONING + "," + CALL + "," + CALL.replace("call_1", "call_2")));
        try (OpenAIEngine<OpenAIProperties> engine = engine(client)) {
            AIEngineResponse response = engine.requestCompletion(new VoidProgressMonitor(), request());
            engine.requestCompletion(new VoidProgressMonitor(), new AIEngineRequest(response.getFunctionCalls().stream()
                .map(OpenAIResponsesTest::savedResult).toList()));
            ArgumentCaptor<OAIResponsesRequest> requests = ArgumentCaptor.forClass(OAIResponsesRequest.class);
            Mockito.verify(client, Mockito.times(2)).createChatCompletion(Mockito.any(), requests.capture());
            OAIResponsesRequest next = requests.getAllValues().getLast();
            Assertions.assertEquals(List.of("reasoning", "function_call", "function_call", "function_call_output", "function_call_output"),
                next.input.stream().map(message -> message.type).toList());
            Assertions.assertEquals(JSONUtils.GSON.fromJson(REASONING, Object.class),
                JSONUtils.GSON.fromJson(JSONUtils.GSON.toJson(next.input.getFirst()), Object.class));
        }
    }

    @Test
    void replaysCompletedStreamingReasoningWithoutDisplayingIt() throws Exception {
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        OpenAiAPIStreamConsumer consumer = new OpenAiAPIStreamConsumer(listener, "source");
        consumer.accept("data: {\"type\":\"response.output_item.added\",\"item\":"
            + REASONING.replace("opaque-reasoning", "partial") + "}");
        consumer.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + REASONING + "}");
        consumer.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL + "}");
        consumer.accept("data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}");
        consumer.complete();
        ArgumentCaptor<AIEngineResponseChunk> chunks = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
        Mockito.verify(listener).nextChunk(chunks.capture());
        OAIResponsesRequest next = OpenAiUtils.createOpenAiRequest(
            new AIEngineRequest(List.of(savedResult(chunks.getValue().getFunctionCall()))), "o3", null, "source", false);
        Assertions.assertEquals(List.of("reasoning", "function_call", "function_call_output"),
            next.input.stream().map(message -> message.type).toList());
        Assertions.assertTrue(JSONUtils.GSON.toJson(next.input.getFirst()).contains("opaque-reasoning"));
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"failed", "incomplete"})
    void reportsUnsuccessfulStreamingResponses(@NotNull String status) {
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        OpenAiAPIStreamConsumer consumer = new OpenAiAPIStreamConsumer(listener, "source");
        consumer.accept("data: {\"type\":\"response." + status + "\",\"response\":"
            + "{\"status\":\"" + status + "\",\"error\":{\"message\":\"Backend unavailable\"},"
            + "\"incomplete_details\":{\"reason\":\"max_output_tokens\"},\"output\":[]}}");
        ArgumentCaptor<Throwable> errors = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(listener).error(errors.capture());
        Assertions.assertInstanceOf(DBException.class, errors.getValue());
        Assertions.assertTrue(errors.getValue().getMessage().contains(status));
        Mockito.verify(listener, Mockito.never()).nextChunk(Mockito.any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"failed", "incomplete", "cancelled", "in_progress"})
    void rejectsUnsuccessfulSynchronousResponses(@NotNull String status) {
        Assertions.assertThrows(DBException.class,
            () -> OpenAiUtils.toEngineResponse(response(status, TEXT + "," + CALL), "source"));
    }

    @Test
    void preservesOrdinaryTextResponses() throws Exception {
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.createChatCompletion(Mockito.any(), Mockito.any())).thenReturn(response("completed", TEXT));
        try (OpenAIEngine<OpenAIProperties> engine = engine(client)) {
            AIEngineResponse response = engine.requestCompletion(new VoidProgressMonitor(), request());
            Assertions.assertEquals(List.of("Checking tables"), response.getVariants());
            Assertions.assertTrue(response.getFunctionCalls().isEmpty());
            Assertions.assertTrue(response.getMessageMetadata().isEmpty());
        }
    }

    @Test
    void continuesParallelCallsOverHttpWithEncryptedReasoning() throws Exception {
        List<String> bodies = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/responses", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String output = bodies.size() == 1 ? REASONING + "," + TEXT + "," + CALL + "," + CALL.replace("call_1", "call_2") : TEXT;
            byte[] bytes = ("{\"status\":\"completed\",\"output\":[" + output + "]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        OpenAIProperties properties = new OpenAIProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setModel("o3");
        properties.setToken("local-test-key");
        properties.setTemperature(0.5);
        try (MockedStatic<GeneralUtils> utils = Mockito.mockStatic(
            GeneralUtils.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
            OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties)) {
            utils.when(GeneralUtils::getProductTitle).thenReturn("Responses regression test");
            DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
            AIEngineResponse calls = engine.requestCompletion(monitor, request());
            Assertions.assertEquals(2, calls.getFunctionCalls().size());
            engine.requestCompletion(monitor, new AIEngineRequest(calls.getFunctionCalls().stream()
                .map(OpenAIResponsesTest::savedResult).toList()));
            OAIResponsesRequest next = JSONUtils.GSON.fromJson(bodies.getLast(), OAIResponsesRequest.class);
            Assertions.assertEquals(List.of("reasoning", "message", "function_call", "function_call",
                "function_call_output", "function_call_output"), next.input.stream().map(message -> message.type).toList());
            Assertions.assertEquals("Checking tables", next.input.get(1).getFullText());
            Assertions.assertEquals(List.of("reasoning.encrypted_content"), List.of(next.include));
            Assertions.assertTrue(bodies.getLast().contains("opaque-reasoning"));
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"failed", "incomplete"})
    void reportsTerminalErrorsOverHttpStreaming(@NotNull String status) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/responses", exchange -> {
            byte[] bytes = ("data: {\"type\":\"response.output_text.delta\",\"delta\":\"SELECT \"}\n\n"
                + "data: {\"type\":\"response." + status + "\",\"response\":{\"output\":[],"
                + "\"incomplete_details\":{\"reason\":\"max_output_tokens\"}}}\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try (MockedStatic<GeneralUtils> utils = Mockito.mockStatic(
            GeneralUtils.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
            OpenAIClientResponses client = new OpenAIClientResponses(
                "http://127.0.0.1:" + server.getAddress().getPort(), List.of(), false)) {
            utils.when(GeneralUtils::getProductTitle).thenReturn("Responses regression test");
            AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
            CountDownLatch done = new CountDownLatch(1);
            Mockito.doAnswer(invocation -> {
                done.countDown();
                return null;
            }).when(listener).error(Mockito.any());
            OAIResponsesRequest request = OpenAiUtils.createOpenAiRequest(request(), "o3", null);
            request.stream = true;
            client.createChatCompletionStream(Mockito.mock(DBRProgressMonitor.class), request, listener);
            Assertions.assertTrue(done.await(5, TimeUnit.SECONDS));
            Mockito.verify(listener).error(Mockito.isA(DBException.class));
            Mockito.verify(listener, Mockito.never()).completeBlock();
            ArgumentCaptor<AIEngineResponseChunk> chunks = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
            Mockito.verify(listener).nextChunk(chunks.capture());
            Assertions.assertEquals(List.of("SELECT "), chunks.getValue().getChoices());
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"eof", "failed", "incomplete", "error", "completed_then_error", "transport"})
    void discardsProvisionalCallsAndReasoningOnStreamFailure(@NotNull String ending) {
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        OpenAiAPIStreamConsumer stream = new OpenAiAPIStreamConsumer(listener, "source");
        stream.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + REASONING + "}");
        stream.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL + "}");
        switch (ending) {
            case "failed", "incomplete" -> stream.accept("data: {\"type\":\"response." + ending + "\"}");
            case "completed_then_error" -> {
                stream.accept("data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}");
                stream.accept("data: {\"type\":\"error\",\"code\":\"server_error\",\"message\":\"Unavailable\"}");
            }
            case "error" -> stream.accept("data: {\"type\":\"error\",\"code\":\"server_error\",\"message\":\"Unavailable\"}");
            case "transport" -> stream.error(new DBException("Connection lost"));
            default -> { }
        }
        stream.complete();
        Mockito.verify(listener).error(Mockito.any());
        Mockito.verify(listener, Mockito.never()).messageMetadata(Mockito.anyMap());
        Mockito.verify(listener, Mockito.never()).nextChunk(Mockito.any());
        Mockito.verify(listener, Mockito.never()).completeBlock();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nested", "nested_typed", "top_level"})
    void preservesErrorDetailsFromBothStreamFormats(@NotNull String format) {
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        OpenAiAPIStreamConsumer stream = new OpenAiAPIStreamConsumer(listener, "source");
        stream.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL + "}");
        String details = "\"code\":\"invalid_request\",\"message\":\"Test invalid API key\"";
        String event = switch (format) {
            case "nested" -> "{\"error\":{" + details + "}}";
            case "nested_typed" -> "{\"type\":\"error\",\"error\":{" + details + "}}";
            case "top_level" -> "{\"type\":\"error\"," + details + "}";
            default -> throw new IllegalArgumentException(format);
        };
        stream.accept("data: " + event);
        stream.complete();
        ArgumentCaptor<Throwable> failure = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(listener).error(failure.capture());
        Assertions.assertEquals("invalid_request: Test invalid API key", failure.getValue().getMessage());
        Mockito.verify(listener, Mockito.never()).nextChunk(Mockito.any());
        Mockito.verify(listener, Mockito.never()).messageMetadata(Mockito.anyMap());
        Mockito.verify(listener, Mockito.never()).completeBlock();
    }

    @NotNull
    private static OpenAIEngine<OpenAIProperties> engine(@NotNull OpenAIClientResponses client) {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setModel("o3");
        properties.setTemperature(0.5);
        return new OpenAIEngine<>(properties) {
            @NotNull
            @Override
            protected OpenAIClientResponses createClient() {
                return client;
            }
        };
    }

    @NotNull
    private static AIEngineRequest request() {
        return new AIEngineRequest(AIMessage.userMessage("Inspect tables and views"));
    }

    @NotNull
    private static OAIResponsesResponse response(@NotNull String status, @NotNull String output) {
        return JSONUtils.GSON.fromJson("{\"status\":\"" + status + "\",\"output\":[" + output + "]}", OAIResponsesResponse.class);
    }

    @NotNull
    private static AIMessage savedResult(@NotNull AIFunctionCall call) {
        AIFunctionCall restored = JSONUtils.GSON.fromJson(JSONUtils.GSON.toJson(call), AIFunctionCall.class);
        return AIMessage.functionCall(restored, new AIFunctionResult(AIFunctionType.INFORMATION, "table result"));
    }
}
