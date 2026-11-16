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
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Objects;

class AIAssistantParallelCallsTest {
    @Test
    void attachesProviderStateToCompletedAssistantMessage() {
        AIEngine<?> engine = Mockito.mock(AIEngine.class);
        Mockito.doReturn(Mockito.mock(AIEngineProperties.class)).when(engine).getProperties();
        AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
        Mockito.when(conversation.isActive()).thenReturn(true);
        AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);
        AIEngineResponseConsumerImpl consumer = new AIEngineResponseConsumerImpl(listener, Mockito.mock(DBRProgressMonitor.class),
            engine, conversation, Mockito.mock(AIEngineDescriptor.class), calls -> Assertions.fail("Unexpected function call"));
        Map<String, String> metadata = Map.of("provider-state", "opaque-state");
        consumer.nextChunk(new AIEngineResponseChunk(List.of("Answer")));
        consumer.messageMetadata(metadata);
        consumer.completeBlock();
        ArgumentCaptor<List<AIMessageMeta>> completed = ArgumentCaptor.forClass(List.class);
        Mockito.verify(listener).complete(completed.capture(), Mockito.eq(true), Mockito.eq(false));
        Assertions.assertEquals(metadata, completed.getValue().getFirst().responseMetadata());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void processesEverySynchronousCall(boolean actions) throws Exception {
        AIAssistantImpl assistant = Mockito.spy(new AIAssistantImpl(Mockito.mock(DBPWorkspace.class)));
        Mockito.doNothing().when(assistant).checkAiEnablement();
        AIEngineDescriptor descriptor = Mockito.mock(AIEngineDescriptor.class);
        AIConfigurationProfile profile = Mockito.mock(AIConfigurationProfile.class);
        Mockito.when(profile.getEngineDescriptor()).thenReturn(descriptor);
        AIEngine<?> engine = Mockito.mock(AIEngine.class);
        Mockito.doReturn(engine).when(descriptor).createEngineInstance(profile);
        AIEngineProperties properties = Mockito.mock(AIEngineProperties.class);
        Mockito.when(properties.getModelDisplayName()).thenReturn("test-model");
        Mockito.doReturn(properties).when(engine).getProperties();
        AIEngineRequest initial = new AIEngineRequest(AIMessage.userMessage("Inspect tables and views"));
        Mockito.doReturn(initial).when(assistant).buildAiEngineRequest(Mockito.any(), Mockito.eq(profile),
            Mockito.any(), Mockito.anyList(), Mockito.eq(engine));
        AIFunctionCall first = new AIFunctionCall("tables", Map.of());
        AIFunctionCall second = new AIFunctionCall("views", Map.of());
        AIFunctionType functionType = actions ? AIFunctionType.ACTION : AIFunctionType.INFORMATION;
        Mockito.doReturn(new AIFunctionResult(functionType, "table result"))
            .when(assistant).callFunction(Mockito.any(), Mockito.eq(first));
        Mockito.doReturn(new AIFunctionResult(functionType, "view result"))
            .when(assistant).callFunction(Mockito.any(), Mockito.eq(second));
        Mockito.doReturn(new AIEngineResponse(List.of(first, second), null),
            new AIEngineResponse(AIMessageType.ASSISTANT, List.of("done"), null))
            .when(assistant).requestCompletion(Mockito.eq(engine), Mockito.eq(profile), Mockito.any(), Mockito.any());

        AIAssistantResponse response = assistant.generateText(Mockito.mock(DBRProgressMonitor.class), profile,
            Mockito.mock(AIFunctionContext.class), initial.getMessages());
        Assertions.assertEquals(actions ? "table result\nview result" : "done", response.getText());
        ArgumentCaptor<AIEngineRequest> requests = ArgumentCaptor.forClass(AIEngineRequest.class);
        Mockito.verify(assistant, Mockito.times(actions ? 1 : 2)).requestCompletion(
            Mockito.eq(engine), Mockito.eq(profile), Mockito.any(), requests.capture());
        if (!actions) {
            Assertions.assertEquals(List.of(first, second), requests.getAllValues().getLast().getMessages().stream()
                .map(AIMessage::getFunctionCall).filter(Objects::nonNull).toList());
        }
        Mockito.verify(assistant).callFunction(Mockito.any(), Mockito.eq(first));
        Mockito.verify(assistant).callFunction(Mockito.any(), Mockito.eq(second));
    }

    @Test
    void forwardsAllCallsWhenChatStreamingIsDisabled() throws Exception {
        AIAssistantImpl assistant = Mockito.spy(new AIAssistantImpl(Mockito.mock(DBPWorkspace.class)));
        Mockito.doNothing().when(assistant).checkAiEnablement();
        Mockito.doReturn(false).when(assistant).isLoggingEnabled();
        Mockito.doReturn(Mockito.mock(AIToolboxManager.class)).when(assistant).getToolboxManager();
        AIEngineRequestFactory factory = Mockito.mock(AIEngineRequestFactory.class);
        Mockito.doReturn(factory).when(assistant).getRequestFactory();
        Mockito.when(factory.build(Mockito.any(), Mockito.eq(assistant), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyList()))
            .thenReturn(new AIEngineRequest(AIMessage.userMessage("Inspect database")));
        AIFunctionDescriptor function = Mockito.mock(AIFunctionDescriptor.class);
        Mockito.when(function.getParameters()).thenReturn(new AIFunctionParameter[0]);
        AIFunctionCall first = new AIFunctionCall("tables", Map.of());
        AIFunctionCall second = new AIFunctionCall("views", Map.of());
        first.setFunction(function);
        second.setFunction(function);
        AIEngine<?> engine = Mockito.mock(AIEngine.class);
        Mockito.doReturn(Mockito.mock(AIEngineProperties.class)).when(engine).getProperties();
        Mockito.when(engine.requestCompletion(Mockito.any(), Mockito.any()))
            .thenReturn(new AIEngineResponse(List.of(first, second), null));
        AIEngineDescriptor descriptor = Mockito.mock(AIEngineDescriptor.class);
        AIConfigurationProfile profile = Mockito.mock(AIConfigurationProfile.class);
        Mockito.when(profile.getEngineDescriptor()).thenReturn(descriptor);
        Mockito.doReturn(engine).when(descriptor).createEngineInstance(profile);
        AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
        Mockito.when(conversation.getProfile()).thenReturn(profile);
        Mockito.when(conversation.isActive()).thenReturn(true);
        Mockito.when(conversation.getPromptGenerator()).thenReturn(Mockito.mock(AIPromptGenerator.class));
        AIChatSession session = Mockito.mock(AIChatSession.class);
        Mockito.when(session.getAssistant()).thenReturn(assistant);
        AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);
        try (MockedStatic<AIUtils> utils = Mockito.mockStatic(
            AIUtils.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            utils.when(AIUtils::useStreamMode).thenReturn(false);
            assistant.generateTextStream(Mockito.mock(DBRProgressMonitor.class), session, conversation,
                new AIChatRequest(null, List.of(AIMessage.userMessage("Inspect database")), null), listener);
        }
        ArgumentCaptor<AIMessage> confirmation = ArgumentCaptor.forClass(AIMessage.class);
        Mockito.verify(conversation).addMessage(confirmation.capture());
        Assertions.assertInstanceOf(AIFunctionCallConfirmation.class, confirmation.getValue().getConfirmation());
        Assertions.assertEquals(List.of(first, second),
            ((AIFunctionCallConfirmation) confirmation.getValue().getConfirmation()).getFunctionCalls());
        Mockito.verify(listener).complete(Mockito.anyList(), Mockito.eq(false), Mockito.eq(false));
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
    }
}
