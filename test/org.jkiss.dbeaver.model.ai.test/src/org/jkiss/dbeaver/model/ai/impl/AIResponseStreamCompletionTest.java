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

package org.jkiss.dbeaver.model.ai.impl;

import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIAssistant;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.ai.AIChatMessage;
import org.jkiss.dbeaver.model.ai.AIChatResponseConsumer;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageMeta;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotClientResponses;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatRequest;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotSessionToken;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientChat;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientResponses;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIEngine;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAiAPIStreamConsumer;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAiClientBase;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAiUtils;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.qm.QMAIChatHistoryMapper;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class AIResponseStreamCompletionTest {
    private static final String CALL = """
        {"type":"function_call","call_id":"call_1","name":"db_listTables","arguments":"{}"}
        """.strip();

    @Test
    void buffersAllCallsUntilSuccessfulStreamCompletion() {
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        OpenAiAPIStreamConsumer consumer = new OpenAiAPIStreamConsumer(listener);
        consumer.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL + "}");
        consumer.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL.replace("call_1", "call_2") + "}");
        Mockito.verifyNoInteractions(listener);
        consumer.accept("data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}");
        Mockito.verify(listener, Mockito.never()).nextChunk(Mockito.any());
        consumer.complete();
        consumer.complete();
        ArgumentCaptor<AIEngineResponseChunk> chunks = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
        Mockito.verify(listener, Mockito.times(2)).nextChunk(chunks.capture());
        Assertions.assertEquals(List.of("call_1", "call_2"), chunks.getAllValues().stream()
            .map(chunk -> chunk.getFunctionCall().getMessageMetadata().get(OpenAIConstants.TOOL_RESULT_CALL_ID)).toList());
        Mockito.verify(listener).completeBlock();
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"failed", "incomplete", "error", "malformed", "eof", "invalid_completed", "completed_then_error"})
    void discardsCallsFromUnsuccessfulStreams(@NotNull String ending) {
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        OpenAiAPIStreamConsumer consumer = new OpenAiAPIStreamConsumer(listener);
        consumer.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL + "}");
        streamEnding(ending).lines().forEach(consumer);
        consumer.complete();
        // a later event must not revive a rejected response
        consumer.accept("data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}");
        consumer.complete();
        Mockito.verify(listener).error(Mockito.any());
        Mockito.verify(listener, Mockito.never()).nextChunk(Mockito.any());
        Mockito.verify(listener, Mockito.never()).completeBlock();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void discardsCallsAfterTransportErrors(boolean completed) {
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        OpenAiAPIStreamConsumer consumer = new OpenAiAPIStreamConsumer(listener);
        consumer.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL + "}");
        if (completed) {
            streamEnding("completed").lines().forEach(consumer);
        }
        consumer.error(new DBException("Connection lost"));
        consumer.complete();
        Mockito.verify(listener).error(Mockito.any());
        Mockito.verify(listener, Mockito.never()).nextChunk(Mockito.any());
        Mockito.verify(listener, Mockito.never()).completeBlock();
    }

    @ParameterizedTest
    @MethodSource("streamCases")
    void dispatchesHttpStreamCallsOnlyAfterSuccessfulCompletion(@NotNull String clientType, @NotNull String ending) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = ("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL + "}\n\n"
                + streamEnding(ending)).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length + ("transport_error".equals(ending) ? 32 : 0));
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        AtomicInteger dispatched = new AtomicInteger();
        AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);
        try (MockedStatic<GeneralUtils> utils = Mockito.mockStatic(
            GeneralUtils.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
            AutoCloseable client = streamClient(clientType, "http://127.0.0.1:" + server.getAddress().getPort())) {
            utils.when(GeneralUtils::getProductTitle).thenReturn("Responses regression test");
            AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
            Mockito.when(conversation.isActive()).thenReturn(true);
            CountDownLatch done = new CountDownLatch(1);
            Mockito.doAnswer(invocation -> {
                if (!"completed".equals(ending)) {
                    done.countDown();
                }
                return null;
            }).when(listener).complete(Mockito.anyList(), Mockito.anyBoolean(), Mockito.anyBoolean());
            OAIResponsesRequest request = OpenAiUtils.createOpenAiRequest(
                new AIEngineRequest(AIMessage.userMessage("Inspect tables")), "o3", null);
            request.stream = true;
            request.reasoningSource = "source";
            DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
            OpenAIEngine<?> engine = Mockito.mock(OpenAIEngine.class, Mockito.RETURNS_DEEP_STUBS);
            AIEngineResponseConsumerImpl consumer = new AIEngineResponseConsumerImpl(
                listener, Mockito.mock(DBRProgressMonitor.class), engine, conversation,
                Mockito.mock(AIEngineDescriptor.class), calls -> {
                    Assertions.assertEquals("source", calls.getFirst().getMessageMetadata().get(OpenAIConstants.REASONING_SOURCE));
                    dispatched.addAndGet(calls.size());
                    done.countDown();
                });
            if (client instanceof OpenAiClientBase openai) {
                openai.createChatCompletionStream(monitor, request, consumer);
            } else if (client instanceof CopilotClientResponses copilot) {
                CopilotSessionToken session = new CopilotSessionToken("test-token",
                    new CopilotSessionToken.Endpoints("http://127.0.0.1:" + server.getAddress().getPort()));
                copilot.createChatCompletionStream(monitor, session,
                    new Pair<>(request, CopilotChatRequest.builder().withModel("o3").withMessages(List.of()).withTools(List.of()).build()),
                    consumer);
            }
            Assertions.assertTrue(done.await(5, TimeUnit.SECONDS));
        } finally {
            server.stop(0);
        }
        if ("completed".equals(ending)) {
            Assertions.assertEquals(1, dispatched.get());
            Mockito.verify(listener, Mockito.never()).error(Mockito.any());
        } else {
            Assertions.assertEquals(0, dispatched.get());
            Mockito.verify(listener).error(Mockito.any());
        }
    }

    @Test
    void preservesInterruptedResponseStateAfterReloadingHistory() {
        OpenAIEngine<?> engine = Mockito.mock(OpenAIEngine.class, Mockito.RETURNS_DEEP_STUBS);
        AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
        Mockito.when(conversation.isActive()).thenReturn(true);
        AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);
        AIEngineResponseConsumerImpl consumer = new AIEngineResponseConsumerImpl(listener,
            Mockito.mock(DBRProgressMonitor.class), engine, conversation, Mockito.mock(AIEngineDescriptor.class),
            calls -> Assertions.fail("Unexpected calls"));
        OpenAiAPIStreamConsumer stream = new OpenAiAPIStreamConsumer(consumer, "source");
        stream.accept("data: {\"type\":\"response.output_text.delta\",\"delta\":\"SELECT \"}");
        stream.complete();
        ArgumentCaptor<List<AIMessageMeta>> metadata = ArgumentCaptor.forClass(List.class);
        Mockito.verify(listener).complete(metadata.capture(), Mockito.eq(true), Mockito.eq(false));
        Assertions.assertNull(metadata.getValue().getFirst().usage());
        AIMessage partial = AIMessage.assistantMessage("SELECT ", metadata.getValue());
        List<AIChatMessage> restored = QMAIChatHistoryMapper.toAIMessages(Mockito.mock(AIAssistant.class),
            QMAIChatHistoryMapper.toQMAIChatMessages(List.of(new AIChatMessage(1, partial))));
        AIMessage message = restored.getFirst().message();
        Assertions.assertNull(message.getMeta().getFirst().usage());
        Assertions.assertEquals("true", message.getMeta().getFirst().responseMetadata().get(AIConstants.RESPONSE_INCOMPLETE));
        OAIResponsesRequest request = OpenAiUtils.createOpenAiRequest(new AIEngineRequest(List.of(message)), "o3", null, "source");
        Assertions.assertEquals("user", request.input.getFirst().role);
        Assertions.assertTrue(request.input.getFirst().getFullText().contains("Previous assistant response:"));
    }

    @NotNull
    private static Stream<Arguments> streamCases() {
        return Stream.of("responses", "chat", "copilot").flatMap(client ->
            Stream.of("failed", "incomplete", "error", "malformed", "eof", "transport_error", "completed")
                .map(ending -> Arguments.of(client, ending)));
    }

    @NotNull
    private static AutoCloseable streamClient(@NotNull String clientType, @NotNull String baseUrl) {
        return switch (clientType) {
            case "chat" -> new OpenAIClientChat(baseUrl, List.of());
            case "copilot" -> new CopilotClientResponses(baseUrl) {};
            default -> new OpenAIClientResponses(baseUrl, List.of(), false);
        };
    }

    @NotNull
    private static String streamEnding(@NotNull String ending) {
        return switch (ending) {
            case "eof", "transport_error" -> "";
            case "error" -> "data: {\"type\":\"error\",\"error\":{\"code\":\"server_error\",\"message\":\"Unavailable\"}}\n\n";
            case "malformed" -> "data: []\n\n";
            case "invalid_completed" -> "data: {\"type\":\"response.completed\",\"response\":{\"status\":\"incomplete\"}}\n\n";
            case "completed_then_error" -> streamEnding("completed") + streamEnding("error");
            default -> "data: {\"type\":\"response." + ending + "\",\"response\":{\"status\":\"" + ending + "\"}}\n\n";
        };
    }
}
