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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.ai.AIChatResponseConsumer;
import org.jkiss.dbeaver.model.ai.AIChatSession;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageMeta;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAiAPIStreamConsumer;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAiUtils;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessage;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.ai.qm.QMAIChatHistoryMapper;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class AIResponseHistoryTest {
    private static final String REASONING = """
        {"id":"rs_1","type":"reasoning","summary":[],"encrypted_content":"opaque"}
        """.strip();
    private static final String CALL = """
        {"type":"function_call","call_id":"call_1","name":"db_listTables","arguments":"{}"}
        """.strip();
    private static final String TEXT = """
        {"type":"message","role":"assistant","content":[{"type":"output_text","text":"Checking tables"}]}
        """.strip();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesResponseOrderWhenTheLaterCallIsDeclined(boolean streaming) throws Exception {
        OAIResponsesResponse response = response(REASONING + "," + CALL + "," + REASONING.replace("rs_1", "rs_2") + ","
            + CALL.replace("call_1", "call_2") + "," + REASONING.replace("rs_1", "rs_3"));
        List<AIFunctionCall> calls;
        if (streaming) {
            AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
            stream(response, new OpenAiAPIStreamConsumer(listener, "source"));
            ArgumentCaptor<AIEngineResponseChunk> chunks = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
            Mockito.verify(listener, Mockito.times(2)).nextChunk(chunks.capture());
            calls = chunks.getAllValues().stream().map(AIEngineResponseChunk::getFunctionCall).toList();
        } else {
            calls = OpenAiUtils.toEngineResponse(response, "source").getFunctionCalls();
        }
        AIFunctionCall first = calls.getFirst();
        AIFunctionCall second = calls.getLast();
        AIChatSession session = Mockito.mock(AIChatSession.class, Mockito.CALLS_REAL_METHODS);
        AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
        session.declineFunctionCalls(conversation, List.of(second));
        ArgumentCaptor<List<AIMessage>> pending = ArgumentCaptor.forClass(List.class);
        Mockito.verify(conversation).addPendingDeclinedFunctionCallMessages(pending.capture());
        List<AIMessage> history = new ArrayList<>(pending.getValue());
        history.add(AIMessage.functionCall(first, new AIFunctionResult(AIFunctionType.INFORMATION, "table")));
        Map<String, String> firstMetadata = Map.copyOf(first.getMessageMetadata());
        Map<String, String> secondMetadata = Map.copyOf(second.getMessageMetadata());
        OAIResponsesRequest next = request(history, "source", false);
        Assertions.assertEquals(firstMetadata, first.getMessageMetadata());
        Assertions.assertEquals(secondMetadata, second.getMessageMetadata());
        Assertions.assertSame(second, history.get(1).getFunctionCall());
        Assertions.assertSame(first, history.get(2).getFunctionCall());
        Assertions.assertEquals(JSONUtils.GSON.toJson(response.output), JSONUtils.GSON.toJson(next.input.subList(1, 6)));
        Assertions.assertEquals(List.of("call_1", "call_2"), next.input.subList(6, 8).stream().map(item -> item.callId).toList());
        Assertions.assertEquals(history.get(2).getContent(), next.input.get(6).output);
        Assertions.assertTrue(next.input.get(7).output.contains("declined"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"before", "between", "after"})
    void restoresStreamedCommentaryAndCallsAsOneOrderedResponse(@NotNull String position) {
        String calls = CALL + "," + REASONING.replace("rs_1", "rs_2") + "," + CALL.replace("call_1", "call_2");
        String output = switch (position) {
            case "before" -> REASONING + "," + TEXT + "," + calls;
            case "between" -> REASONING + "," + CALL + "," + REASONING.replace("rs_1", "rs_2") + "," + TEXT + ","
                + CALL.replace("call_1", "call_2");
            case "after" -> REASONING + "," + calls + "," + TEXT;
            default -> throw new IllegalArgumentException(position);
        };
        OAIResponsesResponse response = response(output);
        AIEngine<?> engine = Mockito.mock(AIEngine.class);
        Mockito.doReturn(Mockito.mock(AIEngineProperties.class)).when(engine).getProperties();
        AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
        Mockito.when(conversation.isActive()).thenReturn(true);
        AIChatResponseConsumer chat = Mockito.mock(AIChatResponseConsumer.class);
        AtomicReference<List<AIFunctionCall>> callsRef = new AtomicReference<>();
        AIEngineResponseConsumerImpl consumer = new AIEngineResponseConsumerImpl(chat,
            Mockito.mock(DBRProgressMonitor.class), engine, conversation, Mockito.mock(AIEngineDescriptor.class), callsRef::set);
        stream(response, new OpenAiAPIStreamConsumer(consumer, "source"));
        ArgumentCaptor<List<AIMessageMeta>> metadata = ArgumentCaptor.forClass(List.class);
        Mockito.verify(chat).complete(metadata.capture(), Mockito.eq(false), Mockito.eq(false));
        Mockito.verify(chat).nextMessageChunk("Checking tables");
        Mockito.verify(chat, Mockito.never()).error(Mockito.any());
        AIMessageMeta restored = QMAIChatHistoryMapper.toAIMessageMeta(
            QMAIChatHistoryMapper.toQMAIMessageMeta(metadata.getValue().getFirst()));
        List<AIMessage> history = new ArrayList<>();
        history.add(AIMessage.assistantMessage("Checking tables", List.of(restored)));
        history.add(AIMessage.systemMessage("A call was declined"));
        // the session saves commentary before tool results, even when the native output puts it later
        for (AIFunctionCall call : callsRef.get().reversed()) {
            AIFunctionCall reopened = JSONUtils.GSON.fromJson(JSONUtils.GSON.toJson(call), AIFunctionCall.class);
            history.add(AIMessage.functionCall(reopened, new AIFunctionResult(AIFunctionType.INFORMATION, "table")));
        }
        for (boolean reasoningRequired : List.of(false, true)) {
            OAIResponsesRequest next = request(history, "source", reasoningRequired);
            Assertions.assertEquals(JSONUtils.GSON.toJson(response.output), JSONUtils.GSON.toJson(next.input.subList(0, 5)));
            Assertions.assertEquals(List.of("call_1", "call_2"), next.input.subList(5, 7).stream().map(item -> item.callId).toList());
            Assertions.assertEquals(8, next.input.size());
            Assertions.assertEquals("system", next.input.getLast().role);
            Assertions.assertEquals("A call was declined", next.input.getLast().getFullText());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "foreign"})
    void importsUnsafeGroupsWithoutOrphanedCallsOrReasoning(@NotNull String reason) throws Exception {
        List<AIFunctionCall> calls = OpenAiUtils.toEngineResponse(response(REASONING + "," + CALL + ","
            + CALL.replace("call_1", "call_2")), "source").getFunctionCalls();
        List<AIMessage> history = new ArrayList<>(calls.stream().map(call -> AIMessage.functionCall(
            call, new AIFunctionResult(AIFunctionType.INFORMATION, "table"))).toList());
        String source = "source";
        switch (reason) {
            case "missing" -> history.removeLast();
            case "foreign" -> source = "other-source";
            default -> throw new IllegalArgumentException(reason);
        }
        OAIResponsesRequest next = request(history, source, true);
        Assertions.assertEquals(history.size(), next.input.size());
        Assertions.assertTrue(next.input.stream().allMatch(item -> "message".equals(item.type) && "user".equals(item.role)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesPlainAnswerReasoningAfterHistoryReload(boolean streaming) throws Exception {
        OAIResponsesResponse response = response(REASONING + "," + TEXT + "," + REASONING.replace("rs_1", "rs_2") + "," + TEXT);
        Map<String, String> state;
        if (streaming) {
            AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
            stream(response, new OpenAiAPIStreamConsumer(listener, "source"));
            ArgumentCaptor<Map<String, String>> metadata = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(listener).messageMetadata(metadata.capture());
            state = metadata.getValue();
        } else {
            state = OpenAiUtils.toEngineResponse(response, "source").getMessageMetadata();
        }
        AIMessageMeta metadata = new AIMessageMeta("prompt", "openai", "model", null, Duration.ZERO, 0, state);
        AIMessageMeta reopened = QMAIChatHistoryMapper.toAIMessageMeta(QMAIChatHistoryMapper.toQMAIMessageMeta(metadata));
        OAIResponsesRequest next = request(List.of(AIMessage.assistantMessage("Checking tables", List.of(reopened)),
            AIMessage.userMessage("Continue")), "source", true);
        Assertions.assertEquals(JSONUtils.GSON.toJson(response.output), JSONUtils.GSON.toJson(next.input.subList(0, 4)));
        Assertions.assertEquals("user", next.input.getLast().role);
        Assertions.assertEquals("Continue", next.input.getLast().getFullText());
        Assertions.assertNull(reopened.usage());
    }

    @Test
    void leavesNativeHistoryDisabledForLegacyCallers() throws Exception {
        AIFunctionCall call = OpenAiUtils.toEngineResponse(response(REASONING + "," + CALL), "source").getFunctionCall();
        AIMessage result = AIMessage.functionCall(call, new AIFunctionResult(AIFunctionType.INFORMATION, "table"));
        OAIResponsesRequest next = OpenAiUtils.createOpenAiRequest(new AIEngineRequest(List.of(result)), "model", null);
        Assertions.assertEquals(List.of("function_call", "function_call_output"), next.input.stream().map(item -> item.type).toList());
        Assertions.assertNull(next.include);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void keepsTruncatedNativeHistoryWithinTheExistingWindow(boolean reasoningRequired) throws Exception {
        String answer = "a".repeat(8000);
        var response = OpenAiUtils.toEngineResponse(response(REASONING + "," + TEXT.replace("Checking tables", answer)), "source");
        AIMessageMeta meta = new AIMessageMeta("prompt", "openai", "model", null, Duration.ZERO, 0, response.getMessageMetadata());
        AIMessage original = AIMessage.assistantMessage(answer, List.of(meta));
        List<AIMessage> history = List.of(AIMessage.userMessage("Old question"), original, AIMessage.userMessage("Continue"));
        DummyTokenCounter counter = new DummyTokenCounter();
        int contextWindow = 2048;
        int replyReserve = 256;
        List<AIMessage> trimmed = ChatTruncator.builder().maxTokens(contextWindow).reserveForSystem(0)
            .reserveForReply(replyReserve).reserveForOverhead(128).tokenCounter(counter).build().tryTruncate(history);
        Assertions.assertNotNull(trimmed);
        AIMessage shortened = trimmed.stream().filter(message -> message.getRole() == original.getRole()).findFirst().orElseThrow();
        Assertions.assertTrue(shortened.getContent().length() < answer.length());
        OAIResponsesRequest next = request(trimmed, "source", reasoningRequired);
        Assertions.assertTrue(next.input.stream().mapToInt(item -> counter.count(item.getFullText())).sum()
            <= contextWindow - replyReserve);
        Assertions.assertEquals("Previous assistant response:\n" + shortened.getContent(), next.input.getFirst().getFullText());
        Assertions.assertTrue(next.input.stream().allMatch(item -> "message".equals(item.type)));
        Assertions.assertEquals(answer, original.getContent());
        Assertions.assertFalse(meta.responseMetadata().containsKey(AIMessage.RESPONSE_CONTENT_MODIFIED));
        Assertions.assertEquals(original.getMeta(), original.withContent(answer).getMeta());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidatesTheWholeToolGroupWhenCommentaryChanges(boolean commentaryFirst) throws Exception {
        var response = OpenAiUtils.toEngineResponse(response(REASONING + "," + TEXT + "," + CALL), "source");
        AIMessageMeta meta = new AIMessageMeta("prompt", "openai", "model", null, Duration.ZERO, 0, response.getMessageMetadata());
        AIMessage original = AIMessage.assistantMessage("Checking tables", List.of(meta));
        AIMessage shortened = original.withContent("Checking");
        AIMessage result = AIMessage.functionCall(response.getFunctionCall(), new AIFunctionResult(AIFunctionType.INFORMATION, "table"));
        List<AIMessage> history = commentaryFirst ? List.of(shortened, result) : List.of(result, shortened);
        OAIResponsesRequest next = request(history, "source", true);
        Assertions.assertEquals(2, next.input.size());
        Assertions.assertTrue(next.input.stream().allMatch(item -> "message".equals(item.type) && "user".equals(item.role)));
        Assertions.assertFalse(JSONUtils.GSON.toJson(next.input).contains("Checking tables"));
        Assertions.assertFalse(meta.responseMetadata().containsKey(AIMessage.RESPONSE_CONTENT_MODIFIED));
        Assertions.assertFalse(response.getFunctionCall().getMessageMetadata().containsKey(AIMessage.RESPONSE_CONTENT_MODIFIED));
    }

    @Test
    void importsATruncatedToolResultWithoutNativeState() throws Exception {
        var response = OpenAiUtils.toEngineResponse(response(REASONING + "," + CALL), "source");
        AIMessage result = AIMessage.functionCall(response.getFunctionCall(),
            new AIFunctionResult(AIFunctionType.INFORMATION, "a".repeat(8000)));
        List<AIMessage> history = List.of(AIMessage.userMessage("Inspect"), result, AIMessage.userMessage("Continue"));
        List<AIMessage> trimmed = ChatTruncator.builder().maxTokens(2048).reserveForSystem(0)
            .reserveForReply(256).reserveForOverhead(128).tokenCounter(new DummyTokenCounter()).build().tryTruncate(history);
        Assertions.assertNotNull(trimmed);
        OAIResponsesRequest next = request(trimmed, "source", true);
        Assertions.assertTrue(next.input.stream().allMatch(item -> "message".equals(item.type) && "user".equals(item.role)));
        Assertions.assertTrue(next.input.stream().anyMatch(item -> item.getFullText().contains("was completed")));
        Assertions.assertNotNull(result.getFunctionCall());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void doesNotReplayCommentaryRemovedEntirelyByTheExistingLimiter(boolean withReasoning) throws Exception {
        String text = "a".repeat(8000);
        OAIResponsesResponse response = response((withReasoning ? REASONING + "," : "")
            + TEXT.replace("Checking tables", text) + "," + CALL);
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        AtomicReference<Map<String, String>> state = new AtomicReference<>(Map.of());
        AtomicReference<AIFunctionCall> call = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            state.set(invocation.getArgument(0));
            return null;
        }).when(listener).messageMetadata(Mockito.anyMap());
        Mockito.doAnswer(invocation -> {
            AIEngineResponseChunk chunk = invocation.getArgument(0);
            if (chunk.getFunctionCall() != null) {
                call.set(chunk.getFunctionCall());
            }
            return null;
        }).when(listener).nextChunk(Mockito.any());
        OpenAiAPIStreamConsumer stream = new OpenAiAPIStreamConsumer(listener, "source");
        for (OAIMessage item : response.output) {
            if ("message".equals(item.type)) {
                stream.accept("data: {\"type\":\"response.output_text.delta\",\"delta\":" + JSONUtils.GSON.toJson(text) + "}");
            }
            stream.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + JSONUtils.GSON.toJson(item) + "}");
        }
        stream.accept("data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}");
        stream.complete();
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
        Assertions.assertNotNull(call.get());
        AIMessageMeta meta = new AIMessageMeta("prompt", "openai", "model", null, Duration.ZERO, 0, state.get());
        AIMessage commentary = AIMessage.assistantMessage(text, List.of(meta));
        AIMessage finalAnswer = AIMessage.assistantMessage("Done", List.of());
        AIMessage user = AIMessage.userMessage("Continue");
        DummyTokenCounter counter = new DummyTokenCounter();
        int contextWindow = 2048;
        int replyReserve = 256;
        int budget = contextWindow - replyReserve - 128;
        int remaining = budget - counter.count(finalAnswer.getContent()) - counter.count(user.getContent());
        AIMessage sample = AIMessage.functionCall(call.get(), new AIFunctionResult(AIFunctionType.INFORMATION, "x"));
        int valueLength = remaining * DummyTokenCounter.TOKEN_TO_CHAR_RATIO - (sample.getContent().length() - 1);
        AIMessage result = AIMessage.functionCall(call.get(), new AIFunctionResult(AIFunctionType.INFORMATION, "b".repeat(valueLength)));
        List<AIMessage> history = List.of(AIMessage.userMessage("Inspect"), commentary, result, finalAnswer, user);
        List<AIMessage> trimmed = ChatTruncator.builder().maxTokens(contextWindow).reserveForSystem(0)
            .reserveForReply(replyReserve).reserveForOverhead(128).tokenCounter(counter).build().tryTruncate(history);
        Assertions.assertNotNull(trimmed);
        Assertions.assertEquals(List.of(result, finalAnswer, user), trimmed);
        Map<String, String> callState = Map.copyOf(call.get().getMessageMetadata());
        OAIResponsesRequest next = request(trimmed, "source", false);
        Assertions.assertFalse(JSONUtils.GSON.toJson(next.input).contains(text));
        Assertions.assertTrue(next.input.stream().mapToInt(item -> counter.count(item.getFullText())
            + (item.output == null ? 0 : counter.count(item.output))
            + (item.arguments == null ? 0 : counter.count(item.arguments))).sum() <= contextWindow - replyReserve);
        Assertions.assertEquals(callState, call.get().getMessageMetadata());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " \n", "\u2003"})
    void replaysToolOnlyHistoryAfterWhitespaceStreaming(@NotNull String text) {
        OAIResponsesResponse response = response(REASONING + "," + TEXT.replace("\"Checking tables\"", JSONUtils.GSON.toJson(text))
            + "," + CALL);
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        OpenAiAPIStreamConsumer stream = new OpenAiAPIStreamConsumer(listener, "source");
        stream.accept("data: {\"type\":\"response.output_text.delta\",\"delta\":" + JSONUtils.GSON.toJson(text) + "}");
        stream.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + CALL + "}");
        stream.accept("data: {\"type\":\"response.completed\",\"response\":" + JSONUtils.GSON.toJson(response) + "}");
        stream.complete();
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
        ArgumentCaptor<AIEngineResponseChunk> chunks = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
        Mockito.verify(listener, Mockito.times(2)).nextChunk(chunks.capture());
        AIFunctionCall call = chunks.getValue().getFunctionCall();
        Assertions.assertNotNull(call);
        AIMessage result = AIMessage.functionCall(call, new AIFunctionResult(AIFunctionType.INFORMATION, "table"));
        OAIResponsesRequest next = request(List.of(result), "source", true);
        Assertions.assertEquals(JSONUtils.GSON.toJson(response.output), JSONUtils.GSON.toJson(next.input.subList(0, 3)));
        Assertions.assertEquals("function_call_output", next.input.getLast().type);
    }

    @NotNull
    private static OAIResponsesRequest request(@NotNull List<AIMessage> history, @NotNull String source, boolean reasoningRequired) {
        AIEngineRequest request = new AIEngineRequest(history);
        AIFunctionDescriptor function = Mockito.mock(AIFunctionDescriptor.class);
        Mockito.when(function.getFullId()).thenReturn("db_listTables");
        Mockito.when(function.getParameters()).thenReturn(new AIFunctionParameter[0]);
        request.setFunctions(List.of(function));
        return OpenAiUtils.createOpenAiRequest(request, "o3", null, source, reasoningRequired);
    }

    @NotNull
    private static OAIResponsesResponse response(@NotNull String output) {
        return JSONUtils.GSON.fromJson("{\"status\":\"completed\",\"output\":[" + output + "]}", OAIResponsesResponse.class);
    }

    private static void stream(@NotNull OAIResponsesResponse response, @NotNull OpenAiAPIStreamConsumer stream) {
        for (OAIMessage item : response.output) {
            if ("message".equals(item.type)) {
                stream.accept("data: {\"type\":\"response.output_text.delta\",\"delta\":\"Checking tables\"}");
            }
            stream.accept("data: {\"type\":\"response.output_item.done\",\"item\":" + JSONUtils.GSON.toJson(item) + "}");
        }
        // exercise replay from SSE items even if the terminal event omits its output array
        stream.accept("data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}");
        stream.complete();
    }
}
