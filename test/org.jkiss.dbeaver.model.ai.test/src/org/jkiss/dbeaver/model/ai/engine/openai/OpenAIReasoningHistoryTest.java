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
import org.jkiss.dbeaver.model.ai.AIAssistant;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageMeta;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.ai.AIUsage;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponse;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessage;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessageContent;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.ai.impl.AIEngineRequestFactory;
import org.jkiss.dbeaver.model.ai.impl.DummyTokenCounter;
import org.jkiss.dbeaver.model.ai.qm.QMAIChatHistoryMapper;
import org.jkiss.dbeaver.model.ai.qm.QMAIMessageMeta;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class OpenAIReasoningHistoryTest {
    private static final String REASONING = """
        {"id":"rs_1","type":"reasoning","summary":[],"encrypted_content":"opaque-state",
         "content":[{"type":"reasoning_text","text":"Ask for a schema"}]}
        """.strip().replace("\n", "");
    private static final String TEXT = """
        {"type":"message","role":"assistant","content":[{"type":"output_text","text":"Which schema?"}]}
        """.strip();
    private static final String CALL = """
        {"type":"function_call","call_id":"call_1","name":"db_listTables","arguments":"{}"}
        """.strip();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void boundsReplayedReasoningAndDropsWholeHistoricalTurns(boolean configuredWindow) throws Exception {
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.createChatCompletion(Mockito.any(), Mockito.any())).thenReturn(response(TEXT));
        OpenAIProperties properties = properties();
        int window = configuredWindow ? 1000 : AIConstants.DEFAULT_CONTEXT_WINDOW_SIZE;
        properties.setContextWindowSize(configuredWindow ? window : null);
        try (OpenAIEngine<OpenAIProperties> engine = engine(properties, client)) {
            String source = engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class), request(List.of()))
                .getMessageMetadata().get(OpenAIConstants.REASONING_SOURCE);
            List<AIMessage> history = new ArrayList<>();
            for (int turn = 0; turn < 3; turn++) {
                OAIMessage reasoning = JSONUtils.GSON.fromJson(REASONING, OAIMessage.class);
                reasoning.id = "rs_" + turn;
                reasoning.content = List.of(new OAIMessageContent(false, "r ".repeat(window / 3 + 10)));
                history.add(AIMessage.userMessage("Turn " + turn));
                history.add(AIMessage.assistantMessage("Done", List.of(metadata(OpenAiUtils.createReasoningMetadata(
                    List.of(reasoning), source)))));
            }
            history.add(AIMessage.userMessage("Continue"));
            AIEngineRequest next = request(history);
            engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class), next);
            ArgumentCaptor<OAIResponsesRequest> requests = ArgumentCaptor.forClass(OAIResponsesRequest.class);
            Mockito.verify(client, Mockito.times(2)).createChatCompletion(Mockito.any(), requests.capture());
            OAIResponsesRequest wire = requests.getAllValues().getLast();
            Assertions.assertTrue(next.wasPromptTruncated());
            Assertions.assertEquals(List.of("rs_1", "rs_2"), wire.input.stream()
                .filter(item -> "reasoning".equals(item.type)).map(item -> item.id).toList());
            Assertions.assertFalse(wire.input.stream().anyMatch(item -> "Turn 0".equals(item.getFullText())));
            Assertions.assertTrue(wire.input.stream().mapToInt(item -> new DummyTokenCounter().count(item.getFullText())).sum() <= window);
            Assertions.assertEquals(7, history.size());
        }
    }

    @Test
    void countsOpaqueReasoningUsageOnceForParallelCallsAndKeepsTheGroupAtomic() throws Exception {
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        OAIResponsesResponse original = JSONUtils.GSON.fromJson("{\"status\":\"completed\",\"output\":[" + REASONING
            + "," + CALL + "," + CALL.replace("call_1", "call_2") + "],\"usage\":{\"input_tokens\":1,\"output_tokens\":2001,"
            + "\"output_tokens_details\":{\"reasoning_tokens\":2000}}}", OAIResponsesResponse.class);
        original.output.getFirst().content = null;
        Mockito.when(client.createChatCompletion(Mockito.any(), Mockito.any())).thenReturn(original, response(TEXT));
        OpenAIProperties properties = properties();
        try (OpenAIEngine<OpenAIProperties> engine = engine(properties, client)) {
            AIEngineResponse result = engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class), request(List.of()));
            List<AIMessage> active = new ArrayList<>(List.of(AIMessage.userMessage("Inspect both")));
            result.getFunctionCalls().forEach(call -> active.add(AIMessage.functionCall(call,
                new AIFunctionResult(AIFunctionType.INFORMATION, "table"))));
            var counter = engine.getContextTokenCounter(new DummyTokenCounter()::count, true);
            Assertions.assertTrue(counter.applyAsInt(active) >= 2000);
            Assertions.assertTrue(counter.applyAsInt(active) < 2100);
            properties.setContextWindowSize(1000);
            Assertions.assertThrows(DBException.class,
                () -> engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class), request(active)));
            List<AIMessage> old = new ArrayList<>(active);
            old.add(AIMessage.userMessage("Next question"));
            engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class), request(old));
            ArgumentCaptor<OAIResponsesRequest> requests = ArgumentCaptor.forClass(OAIResponsesRequest.class);
            Mockito.verify(client, Mockito.times(2)).createChatCompletion(Mockito.any(), requests.capture());
            Assertions.assertEquals(List.of("message"), requests.getAllValues().getLast().input.stream().map(item -> item.type).toList());
            Assertions.assertEquals("Next question", requests.getAllValues().getLast().input.getFirst().getFullText());
            Assertions.assertEquals(2, result.getFunctionCalls().size());
        }
    }

    @Test
    void boundsSystemAndUserTextWithTheResponsesCounter() throws Exception {
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.createChatCompletion(Mockito.any(), Mockito.any())).thenReturn(response(TEXT));
        OpenAIProperties properties = properties();
        properties.setContextWindowSize(100);
        try (OpenAIEngine<OpenAIProperties> engine = engine(properties, client)) {
            AIEngineRequest next = request(List.of(AIMessage.systemMessage("s ".repeat(200)), AIMessage.userMessage("u ".repeat(200))));
            engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class), next);
            ArgumentCaptor<OAIResponsesRequest> sent = ArgumentCaptor.forClass(OAIResponsesRequest.class);
            Mockito.verify(client).createChatCompletion(Mockito.any(), sent.capture());
            Assertions.assertTrue(next.wasPromptTruncated());
            Assertions.assertTrue(sent.getValue().input.stream()
                .mapToInt(item -> new DummyTokenCounter().count(item.getFullText())).sum() <= 100);
        }
    }

    @Test
    void appliesTheResponsesCounterThroughTheCommonRequestFactory() throws Exception {
        OpenAIProperties properties = properties();
        properties.setContextWindowSize(4000);
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.createChatCompletion(Mockito.any(), Mockito.any())).thenReturn(response(REASONING + "," + TEXT));
        try (OpenAIEngine<OpenAIProperties> engine = engine(properties, client)) {
            AIEngineResponse first = engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class), request(List.of()));
            Map<String, String> metadata = new java.util.HashMap<>(first.getMessageMetadata());
            metadata.put(OpenAIConstants.REASONING_TOKENS, "3000");
            List<AIMessage> history = List.of(AIMessage.userMessage("Old question"),
                AIMessage.assistantMessage("Old answer", List.of(metadata(metadata))), AIMessage.userMessage("New question"));
            AIAssistant assistant = Mockito.mock(AIAssistant.class);
            Mockito.when(assistant.isFunctionSupported(Mockito.any())).thenReturn(true);
            AIPromptGenerator prompt = Mockito.mock(AIPromptGenerator.class);
            Mockito.when(prompt.build(Mockito.any(), Mockito.any())).thenReturn("System");
            AIFunctionContext context = Mockito.mock(AIFunctionContext.class);
            Mockito.when(context.getPrompt()).thenReturn(prompt);
            AIEngineRequestFactory factory = new AIEngineRequestFactory(new DummyTokenCounter()) {
                @NotNull
                @Override
                protected RequestFunctions determineRequestTools(
                    @NotNull AIAssistant currentAssistant,
                    @NotNull AIConfigurationProfile profile,
                    @NotNull AIFunctionContext functionContext,
                    long userMessageCount
                ) {
                    return new RequestFunctions(List.of(), List.of());
                }
            };
            AIEngineRequest next = factory.build(Mockito.mock(DBRProgressMonitor.class), assistant,
                Mockito.mock(AIConfigurationProfile.class), engine, context, history);
            Assertions.assertTrue(next.wasPromptTruncated());
            Assertions.assertEquals(List.of("System", "New question"), next.getMessages().stream().map(AIMessage::getContent).toList());
        }
    }

    @Test
    void countsFinalStreamingUsageOnceForTheWholeParallelGroup() throws Exception {
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        OpenAiAPIStreamConsumer stream = new OpenAiAPIStreamConsumer(listener, "source");
        OAIMessage reasoning = JSONUtils.GSON.fromJson(REASONING, OAIMessage.class);
        reasoning.content = null;
        stream.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + JSONUtils.GSON.toJson(reasoning) + "}");
        stream.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL + "}");
        stream.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL.replace("call_1", "call_2") + "}");
        stream.accept("data: {\"type\":\"response.completed\",\"response\":{\"output\":[],\"usage\":{"
            + "\"input_tokens\":1,\"output_tokens\":2001,\"output_tokens_details\":{\"reasoning_tokens\":2000}}}}");
        ArgumentCaptor<AIEngineResponseChunk> chunks = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
        Mockito.verify(listener, Mockito.times(2)).nextChunk(chunks.capture());
        List<AIMessage> history = chunks.getAllValues().stream().map(chunk -> {
            Assertions.assertEquals("2000", chunk.getFunctionCall().getMessageMetadata().get(OpenAIConstants.REASONING_TOKENS));
            return AIMessage.functionCall(chunk.getFunctionCall(), new AIFunctionResult(AIFunctionType.INFORMATION, "table"));
        }).toList();
        int tokens = OpenAiUtils.countContextTokens(history, "source", false, new DummyTokenCounter()::count);
        Assertions.assertTrue(tokens >= 2000);
        Assertions.assertTrue(tokens < 2100);
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
    }

    @NotNull
    private static OpenAIEngine<OpenAIProperties> engine(@NotNull OpenAIProperties properties, @NotNull OpenAIClientResponses client) {
        return new OpenAIEngine<>(properties) {
            @NotNull
            @Override
            protected OpenAIClientResponses createClient() {
                return client;
            }
        };
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void replaysPlainAnswerReasoningWithToolsAfterHistoryMapping(boolean stream) throws Exception {
        List<OAIResponsesRequest> requests = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/responses", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(JSONUtils.GSON.fromJson(body, OAIResponsesRequest.class));
            String response = stream
                ? "data: {\"type\":\"response.output_item.done\",\"item\":" + REASONING + "}\n\n"
                    + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Which schema?\"}\n\n"
                    + "data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\",\"output\":[]}}\n\n"
                : "{\"status\":\"completed\",\"output\":[" + REASONING + "," + TEXT + "]}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", stream ? "text/event-stream" : "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        OpenAIProperties properties = properties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        try (MockedStatic<GeneralUtils> utils = Mockito.mockStatic(GeneralUtils.class);
            OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties)) {
            utils.when(GeneralUtils::getProductTitle).thenReturn("Reasoning history test");
            List<AIMessage> history = new ArrayList<>(List.of(AIMessage.userMessage("Inspect a schema")));
            AIMessage answer = send(engine, request(history), stream);
            AIMessageMeta restored = QMAIChatHistoryMapper.toAIMessageMeta(
                QMAIChatHistoryMapper.toQMAIMessageMeta(answer.getMeta().getFirst()));
            history.add(AIMessage.assistantMessage(answer.getContent(), List.of(restored)));
            history.add(AIMessage.userMessage("public"));
            send(engine, request(history), stream);
            Assertions.assertEquals(2, requests.size());
            Assertions.assertEquals(1, requests.getLast().tools.size());
            Assertions.assertEquals(List.of("message", "reasoning", "message", "message"), requests.getLast().input.stream()
                .map(message -> message.type).toList());
            Assertions.assertEquals("opaque-state", requests.getLast().input.get(1).encryptedContent);
            Assertions.assertEquals("Ask for a schema", requests.getLast().input.get(1).getFullText());
            Assertions.assertNull(requests.getLast().reasoningSource);
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"endpoint", "model", "authentication"})
    void excludesForeignReasoningAndToolStateWhenConfigurationChanges(@NotNull String change) throws Exception {
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.createChatCompletion(Mockito.any(), Mockito.any())).thenReturn(
            response(REASONING + "," + CALL), response(TEXT));
        OpenAIProperties properties = properties();
        try (OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties) {
            @NotNull
            @Override
            protected OpenAIClientResponses createClient() {
                return client;
            }
        }) {
            AIEngineResponse original = engine.requestCompletion(new VoidProgressMonitor(), request(List.of()));
            var call = original.getFunctionCalls().getFirst();
            Map<String, String> originalMetadata = Map.copyOf(call.getMessageMetadata());
            AIMessage toolResult = AIMessage.functionCall(call, new AIFunctionResult(AIFunctionType.INFORMATION, "table_11"));
            AIMessage answer = AIMessage.assistantMessage("Found a table", List.of(metadata(originalMetadata)));
            switch (change) {
                case "endpoint" -> properties.setBaseUrl("https://another-provider.invalid/v1");
                case "model" -> properties.setModel("another-model");
                case "authentication" -> properties.setAuthentication(OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT);
                default -> throw new AssertionError(change);
            }
            engine.requestCompletion(new VoidProgressMonitor(), request(List.of(toolResult, answer)));
            ArgumentCaptor<OAIResponsesRequest> requests = ArgumentCaptor.forClass(OAIResponsesRequest.class);
            Mockito.verify(client, Mockito.times(2)).createChatCompletion(Mockito.any(), requests.capture());
            OAIResponsesRequest next = requests.getAllValues().getLast();
            Assertions.assertEquals(List.of("message", "message"), next.input.stream().map(message -> message.type).toList());
            Assertions.assertTrue(next.input.stream().allMatch(message -> "user".equals(message.role)));
            Assertions.assertTrue(next.input.getFirst().getFullText().contains("table_11"));
            Assertions.assertEquals(originalMetadata, call.getMessageMetadata());
        }
    }

    @Test
    void restoresMetadataWrittenBeforeProviderStateWasAdded() {
        AIMessageMeta message = JSONUtils.GSON.fromJson("{\"type\":\"prompt\",\"engineId\":\"openai\"}", AIMessageMeta.class);
        QMAIMessageMeta stored = JSONUtils.GSON.fromJson("{\"type\":\"prompt\",\"engineId\":\"openai\"}", QMAIMessageMeta.class);
        Assertions.assertTrue(message.responseMetadata().isEmpty());
        Assertions.assertTrue(stored.responseMetadata().isEmpty());
    }

    @NotNull
    private static AIMessage send(
        @NotNull OpenAIEngine<OpenAIProperties> engine,
        @NotNull AIEngineRequest request,
        boolean stream
    ) throws Exception {
        if (!stream) {
            AIEngineResponse response = engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class), request);
            return AIMessage.assistantMessage(response.getVariants().getFirst(), List.of(metadata(response.getMessageMetadata())));
        }
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        StringBuilder text = new StringBuilder();
        AtomicReference<Map<String, String>> metadata = new AtomicReference<>(Map.of());
        CountDownLatch done = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            AIEngineResponseChunk chunk = invocation.getArgument(0);
            chunk.getChoices().forEach(text::append);
            return null;
        }).when(listener).nextChunk(Mockito.any());
        Mockito.doAnswer(invocation -> {
            metadata.set(invocation.getArgument(0));
            return null;
        }).when(listener).messageMetadata(Mockito.anyMap());
        Mockito.doAnswer(invocation -> {
            done.countDown();
            return null;
        }).when(listener).completeBlock();
        engine.requestCompletionStream(Mockito.mock(DBRProgressMonitor.class), request, listener);
        Assertions.assertTrue(done.await(5, TimeUnit.SECONDS));
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
        return AIMessage.assistantMessage(text.toString(), List.of(metadata(metadata.get())));
    }

    @NotNull
    private static AIMessageMeta metadata(@NotNull Map<String, String> metadata) {
        return new AIMessageMeta("prompt", "openai", "test-model", new AIUsage(0, 0, 0, 0), Duration.ZERO, 0, metadata);
    }

    @NotNull
    private static AIEngineRequest request(@NotNull List<AIMessage> history) {
        AIFunctionDescriptor function = Mockito.mock(AIFunctionDescriptor.class);
        Mockito.when(function.getFullId()).thenReturn("db_listTables");
        Mockito.when(function.getParameters()).thenReturn(new AIFunctionParameter[0]);
        AIEngineRequest request = new AIEngineRequest(history);
        request.setFunctions(List.of(function));
        return request;
    }

    @NotNull
    private static OpenAIProperties properties() {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setModel("test-model");
        properties.setTemperature(0.5);
        properties.setToken("local-test-token");
        return properties;
    }

    @NotNull
    private static OAIResponsesResponse response(@NotNull String output) {
        return JSONUtils.GSON.fromJson("{\"status\":\"completed\",\"output\":[" + output + "]}", OAIResponsesResponse.class);
    }
}
