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
package org.jkiss.dbeaver.model.ai.engine.copilot;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIAssistant;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageMeta;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.ai.AIUsage;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponse;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatRequest;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotSessionToken;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.ai.impl.AIEngineRequestFactory;
import org.jkiss.dbeaver.model.ai.impl.DummyTokenCounter;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;
import java.util.Map;

class CopilotReasoningHistoryTest {
    private static final String REASONING = """
        [{"id":"rs_1","type":"reasoning","summary":[],"encrypted_content":"opaque-state"}]
        """.strip();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void importsForeignAssistantAndToolStateAsText(boolean streaming) throws Exception {
        CopilotClientResponses mockClient = client();
        try (CopilotCompletionEngine<CopilotProperties> engine = engine(properties(), mockClient)) {
            AIEngineRequest request = history("foreign-provider");
            OAIResponsesRequest captured = send(engine, mockClient, request, streaming);
            Assertions.assertTrue(captured.input.stream().allMatch(item -> "message".equals(item.type) && "user".equals(item.role)));
            Assertions.assertFalse(JSONUtils.GSON.toJson(captured).contains("opaque-state"));
            Assertions.assertTrue(captured.input.stream().anyMatch(item -> item.getFullText().contains("old answer")));
            Assertions.assertTrue(captured.input.stream().anyMatch(item -> item.getFullText().contains("table result")));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void replaysMatchingStateAndImportsItAfterChangingModels(boolean streaming) throws Exception {
        CopilotClientResponses mockClient = client();
        CopilotProperties properties = properties();
        try (CopilotCompletionEngine<CopilotProperties> engine = engine(properties, mockClient)) {
            OAIResponsesRequest initial = send(engine, mockClient, new AIEngineRequest(AIMessage.userMessage("test")), streaming);
            Assertions.assertNotNull(initial.reasoningSource);
            AIEngineRequest history = history(initial.reasoningSource);
            OAIResponsesRequest matching = send(engine, mockClient, history, streaming);
            Assertions.assertTrue(matching.input.stream().anyMatch(item -> "reasoning".equals(item.type)));
            Assertions.assertTrue(matching.input.stream().anyMatch(item -> "function_call".equals(item.type)));
            properties.setModel("another-model");
            OAIResponsesRequest changed = send(engine, mockClient, history, streaming);
            Assertions.assertNotEquals(initial.reasoningSource, changed.reasoningSource);
            Assertions.assertTrue(changed.input.stream().allMatch(item -> "message".equals(item.type)));
            Assertions.assertFalse(JSONUtils.GSON.toJson(changed).contains("opaque-state"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesEverySynchronousResponsesCall(boolean textFirst) throws Exception {
        CopilotClientResponses mockClient = client();
        String call = "{\"type\":\"function_call\",\"call_id\":\"call_1\",\"name\":\"db_listTables\",\"arguments\":\"{}\"}";
        String text = "{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"Checking\"}]}";
        Mockito.when(mockClient.chat(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(JSONUtils.GSON.fromJson(
            "{\"status\":\"completed\",\"output\":[" + (textFirst ? text + "," : "")
                + call + "," + call.replace("call_1", "call_2") + "]}", OAIResponsesResponse.class));
        try (CopilotCompletionEngine<CopilotProperties> engine = engine(properties(), mockClient)) {
            AIEngineResponse response = engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class),
                new AIEngineRequest(AIMessage.userMessage("Inspect tables")));
            Assertions.assertEquals(List.of("call_1", "call_2"), response.getFunctionCalls().stream()
                .map(item -> item.getMessageMetadata().get(OpenAIConstants.TOOL_RESULT_CALL_ID)).toList());
        }
    }

    @Test
    void boundsNativeReasoningThroughTheCommonRequestFactory() throws Exception {
        CopilotClientResponses mockClient = client();
        CopilotProperties properties = properties();
        properties.setContextWindowSize(4000);
        try (CopilotCompletionEngine<CopilotProperties> engine = engine(properties, mockClient)) {
            String source = send(engine, mockClient, new AIEngineRequest(AIMessage.userMessage("test")), false).reasoningSource;
            AIMessageMeta meta = new AIMessageMeta("prompt", "copilot", "test-model", new AIUsage(1, 0, 6001, 6000), Duration.ZERO, 0,
                Map.of(OpenAIConstants.REASONING_SOURCE, source, OpenAIConstants.TOOL_CALL_REASONING, REASONING,
                    OpenAIConstants.REASONING_TOKENS, "6000"));
            List<AIMessage> history = List.of(AIMessage.userMessage("Old question"),
                AIMessage.assistantMessage("Old answer", List.of(meta)), AIMessage.userMessage("New question"));
            AIPromptGenerator prompt = Mockito.mock(AIPromptGenerator.class);
            Mockito.when(prompt.build(Mockito.any(), Mockito.any())).thenReturn("System");
            AIFunctionContext context = Mockito.mock(AIFunctionContext.class);
            Mockito.when(context.getPrompt()).thenReturn(prompt);
            AIEngineRequestFactory factory = new AIEngineRequestFactory(new DummyTokenCounter()) {
                @NotNull
                @Override
                protected RequestFunctions determineRequestTools(
                    @NotNull AIAssistant assistant,
                    @NotNull AIConfigurationProfile profile,
                    @NotNull AIFunctionContext functionContext,
                    long userMessageCount
                ) {
                    return new RequestFunctions(List.of(), List.of());
                }
            };
            AIEngineRequest next = factory.build(Mockito.mock(DBRProgressMonitor.class), Mockito.mock(AIAssistant.class),
                Mockito.mock(AIConfigurationProfile.class), engine, context, history);
            Assertions.assertTrue(next.wasPromptTruncated());
            Assertions.assertEquals(List.of("System", "New question"), next.getMessages().stream().map(AIMessage::getContent).toList());
            OAIResponsesRequest wire = send(engine, mockClient, next, false);
            Assertions.assertTrue(wire.input.stream().noneMatch(item -> "reasoning".equals(item.type)));
        }
    }

    @NotNull
    private static AIEngineRequest history(@NotNull String source) {
        Map<String, String> metadata = Map.of(OpenAIConstants.TOOL_CALL_REASONING, REASONING.replace("rs_1", "rs_assistant"),
            OpenAIConstants.REASONING_SOURCE, source);
        AIMessageMeta meta = new AIMessageMeta("prompt", "test", "test-model", new AIUsage(0, 0, 0, 0), Duration.ZERO, 0, metadata);
        AIFunctionCall call = new AIFunctionCall("db_listTables", Map.of(), Map.of(
            OpenAIConstants.TOOL_CALL_REASONING, REASONING.replace("rs_1", "rs_tool"), OpenAIConstants.REASONING_SOURCE, source,
            OpenAIConstants.TOOL_RESULT_CALL_ID, "call_1"));
        return new AIEngineRequest(List.of(AIMessage.assistantMessage("old answer", List.of(meta)),
            AIMessage.functionCall(call, new AIFunctionResult(AIFunctionType.INFORMATION, "table result")),
            AIMessage.userMessage("continue")));
    }

    @NotNull
    private static CopilotProperties properties() {
        CopilotProperties properties = new CopilotProperties();
        properties.setModel("test-model");
        properties.setTemperature(0.5);
        return properties;
    }

    @NotNull
    private static CopilotClientResponses client() throws Exception {
        CopilotClientResponses mockClient = Mockito.mock(CopilotClientResponses.class);
        Mockito.when(mockClient.chat(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(JSONUtils.GSON.fromJson(
            "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"done\"}]}]}",
            OAIResponsesResponse.class));
        return mockClient;
    }

    @NotNull
    private static CopilotCompletionEngine<CopilotProperties> engine(
        @NotNull CopilotProperties properties,
        @NotNull CopilotClientResponses mockClient
    ) {
        return new CopilotCompletionEngine<>(properties) {
            @NotNull
            @Override
            protected CopilotClientResponses createClient(@NotNull String baseAuthUrl) {
                return mockClient;
            }

            @NotNull
            @Override
            protected CopilotSessionToken requestSessionToken(@NotNull DBRProgressMonitor monitor) {
                return new CopilotSessionToken("test-token", null);
            }
        };
    }

    @NotNull
    private static OAIResponsesRequest send(
        @NotNull CopilotCompletionEngine<CopilotProperties> engine,
        @NotNull CopilotClientResponses mockClient,
        @NotNull AIEngineRequest request,
        boolean streaming
    ) throws Exception {
        ArgumentCaptor<Pair<OAIResponsesRequest, CopilotChatRequest>> requests = ArgumentCaptor.forClass(Pair.class);
        if (streaming) {
            engine.requestCompletionStream(Mockito.mock(DBRProgressMonitor.class), request, Mockito.mock(AIEngineResponseConsumer.class));
            Mockito.verify(mockClient).createChatCompletionStream(Mockito.any(), Mockito.any(), requests.capture(), Mockito.any());
        } else {
            engine.requestCompletion(Mockito.mock(DBRProgressMonitor.class), request);
            Mockito.verify(mockClient).chat(Mockito.any(), Mockito.any(), requests.capture());
        }
        Mockito.clearInvocations(mockClient);
        return requests.getValue().getFirst();
    }
}
