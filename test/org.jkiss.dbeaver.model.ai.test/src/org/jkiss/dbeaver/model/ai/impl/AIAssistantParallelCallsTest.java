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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class AIAssistantParallelCallsTest {
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
        Mockito.doReturn(false).when(assistant).isLoggingEnabled();
        AIEngineRequestFactory factory = Mockito.mock(AIEngineRequestFactory.class);
        Mockito.doReturn(factory).when(assistant).getRequestFactory();
        Mockito.when(factory.build(Mockito.any(), Mockito.eq(assistant), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyList()))
            .thenReturn(new AIEngineRequest(AIMessage.userMessage("Inspect database")));
        AIFunctionCall first = new AIFunctionCall("tables", Map.of());
        AIFunctionCall second = new AIFunctionCall("views", Map.of());
        AIEngine<?> engine = Mockito.mock(AIEngine.class);
        Mockito.when(engine.requestCompletion(Mockito.any(), Mockito.any()))
            .thenReturn(new AIEngineResponse(List.of(first, second), null));
        AIEngineResponseConsumer listener = Mockito.mock(AIEngineResponseConsumer.class);
        Method execute = AIAssistantImpl.class.getDeclaredMethod("executeEngineStreamRequest", DBRProgressMonitor.class,
            AIConfigurationProfile.class, AIFunctionContext.class, AIEngine.class, List.class, AIEngineResponseConsumer.class);
        execute.setAccessible(true);
        try (MockedStatic<AIUtils> utils = Mockito.mockStatic(AIUtils.class)) {
            utils.when(AIUtils::useStreamMode).thenReturn(false);
            execute.invoke(assistant, Mockito.mock(DBRProgressMonitor.class), Mockito.mock(AIConfigurationProfile.class),
                Mockito.mock(AIFunctionContext.class), engine, List.of(), listener);
        }
        ArgumentCaptor<AIEngineResponseChunk> chunks = ArgumentCaptor.forClass(AIEngineResponseChunk.class);
        Mockito.verify(listener, Mockito.times(2)).nextChunk(chunks.capture());
        Assertions.assertEquals(List.of(first, second), chunks.getAllValues().stream()
            .map(AIEngineResponseChunk::getFunctionCall).toList());
        Mockito.verify(listener).completeBlock();
    }
}
