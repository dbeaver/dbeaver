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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.ai.AIChatRequest;
import org.jkiss.dbeaver.model.ai.AIChatResponseConsumer;
import org.jkiss.dbeaver.model.ai.AIChatSession;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionSettings;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.ai.AIToolbox;
import org.jkiss.dbeaver.model.ai.AIToolboxManager;
import org.jkiss.dbeaver.model.ai.functions.AIFunctionParams;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AIAssistantFunctionErrorTest extends DBeaverUnitTest {
    @Test
    public void disabledFunctionCannotReadDataAfterModelRequestsIt() throws DBException {
        AIAssistantImpl assistant = Mockito.spy(new AIAssistantImpl(Mockito.mock(DBPWorkspace.class)));
        AIToolboxManager toolboxManager = Mockito.mock(AIToolboxManager.class);
        AIToolbox toolbox = Mockito.mock(AIToolbox.class);
        AIFunctionDescriptor function = Mockito.mock(AIFunctionDescriptor.class);
        AIFunctionSettings settings = new AIFunctionSettings();
        Mockito.when(toolboxManager.getFunctionSettings()).thenReturn(settings);
        Mockito.when(function.getToolbox()).thenReturn(toolbox);
        Mockito.when(function.isEnabledByDefault()).thenReturn(true);
        Mockito.when(function.getId()).thenReturn("readTableSampleRows");
        Mockito.when(toolbox.isEnabled()).thenReturn(true);
        Mockito.when(toolbox.isAccessible()).thenReturn(true);
        settings.getToolboxSettings(toolbox).setFunctionEnabled(function, false);
        Mockito.doReturn(toolboxManager).when(assistant).getToolboxManager();
        AIFunctionCall call = new AIFunctionCall("readTableSampleRows", Map.of());
        call.setFunction(function);

        Assertions.assertThrows(DBException.class, () -> assistant.callFunction(
            new AIFunctionContext(new VoidProgressMonitor(), null, Mockito.mock(AIPromptGenerator.class)), call));
        Mockito.verify(toolbox, Mockito.never()).callFunction(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void failedInformationFunctionReturnsErrorToModelAndContinuesBatch() throws Exception {
        AIAssistantImpl assistant = assistant();
        AIFunctionCall failedCall = call(AIFunctionType.INFORMATION);
        AIFunctionCall successfulCall = call(AIFunctionType.INFORMATION);
        DBException failure = new DBException("Container does not support tables");
        Mockito.doReturn(new AIFunctionResult(AIFunctionType.INFORMATION, "Tool failed", null, failure))
            .when(assistant).callFunction(Mockito.any(), Mockito.eq(failedCall));
        Mockito.doReturn(new AIFunctionResult(AIFunctionType.INFORMATION, "Table names: orders"))
            .when(assistant).callFunction(Mockito.any(), Mockito.eq(successfulCall));
        CompletableFuture<AIChatRequest> followUp = captureFollowUp(assistant);
        AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);

        processCalls(assistant, listener, List.of(failedCall, successfulCall));

        AIChatRequest request = followUp.get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(2, request.messages().size());
        Assertions.assertEquals(AIMessageType.FUNCTION, request.messages().getFirst().getRole());
        Assertions.assertSame(failure, request.messages().getFirst().getError());
        Assertions.assertEquals("Tool failed", request.messages().getFirst().getContent());
        Assertions.assertNull(request.messages().getLast().getError());
        var events = Mockito.inOrder(listener);
        events.verify(listener).processFunctionCall(Mockito.argThat(message -> message.getFunctionCall() == failedCall));
        events.verify(listener).processFunctionCall(Mockito.argThat(message -> message.getFunctionCall() == successfulCall));
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
        Mockito.verify(listener, Mockito.never()).complete(Mockito.anyList(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void failedActionSelectionIsReturnedToModelWithoutExecutingAction() throws Exception {
        assertUnavailableConnectionReturnsToModel(AIFunctionType.ACTION);
    }

    @Test
    public void failedInformationSelectionIsReturnedToModelWithoutReadingData() throws Exception {
        assertUnavailableConnectionReturnsToModel(AIFunctionType.INFORMATION);
    }

    @Test
    public void functionValidationFailureIsReturnedToModel() throws Exception {
        AIAssistantImpl assistant = assistant();
        AIFunctionCall call = call(AIFunctionType.INFORMATION);
        DBException failure = new DBException("Function is disabled");
        Mockito.doThrow(failure).when(assistant).callFunction(Mockito.any(), Mockito.eq(call));
        CompletableFuture<AIChatRequest> followUp = captureFollowUp(assistant);
        AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);

        processCalls(assistant, listener, List.of(call));

        AIChatRequest request = followUp.get(5, TimeUnit.SECONDS);
        Assertions.assertSame(failure, request.messages().getFirst().getError());
        Assertions.assertTrue(request.messages().getFirst().getContent().contains("Function is disabled"));
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
    }

    @Test
    public void unexpectedRuntimeFailureStillFinishesRequest() throws Exception {
        AIAssistantImpl assistant = assistant();
        AIFunctionCall call = call(AIFunctionType.INFORMATION);
        IllegalStateException failure = new IllegalStateException("Unexpected implementation failure");
        Mockito.doThrow(failure).when(assistant).callFunction(Mockito.any(), Mockito.eq(call));
        AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);
        CompletableFuture<Void> finished = new CompletableFuture<>();
        Mockito.doAnswer(invocation -> {
            finished.complete(null);
            return null;
        }).when(listener).complete(Mockito.anyList(), Mockito.eq(true), Mockito.eq(false));

        processCalls(assistant, listener, List.of(call));

        finished.get(5, TimeUnit.SECONDS);
        Mockito.verify(listener).error(failure);
        Mockito.verify(assistant, Mockito.never()).generateTextStream(Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any());
    }

    @Test
    public void failedFollowUpRequestFinishesConversation() throws Exception {
        AIAssistantImpl assistant = Mockito.spy(new AIAssistantImpl(Mockito.mock(DBPWorkspace.class)));
        Mockito.doReturn(Mockito.mock(AIToolboxManager.class)).when(assistant).getToolboxManager();
        AIFunctionDescriptor function = Mockito.mock(AIFunctionDescriptor.class);
        Mockito.when(function.getType()).thenReturn(AIFunctionType.INFORMATION);
        AIFunctionCall call = new AIFunctionCall("test_function", Map.of());
        call.setFunction(function);
        Mockito.doReturn(new AIFunctionResult(AIFunctionType.INFORMATION, "Function succeeded"))
            .when(assistant).callFunction(Mockito.any(), Mockito.eq(call));
        DBException failure = new DBException("Authentication expired");
        Mockito.doThrow(failure).when(assistant).generateTextStream(Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any());

        final AIChatSession session = Mockito.mock(AIChatSession.class);
        AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
        Mockito.when(conversation.isActive()).thenReturn(true);
        AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);
        CompletableFuture<Void> finished = new CompletableFuture<>();
        Mockito.doAnswer(invocation -> {
            finished.complete(null);
            return null;
        }).when(listener).complete(Mockito.anyList(), Mockito.eq(true), Mockito.eq(false));

        Method processFunctionCalls = AIAssistantImpl.class.getDeclaredMethod("processFunctionCalls",
            AIChatSession.class, AIChatConversation.class, AIChatResponseConsumer.class,
            AIFunctionContext.class, AIChatRequest.class, List.class);
        processFunctionCalls.setAccessible(true);
        processFunctionCalls.invoke(assistant, session, conversation, listener,
            new AIFunctionContext(new VoidProgressMonitor(), null, Mockito.mock(AIPromptGenerator.class)),
            new AIChatRequest(null, List.of(), null), List.of(call));

        finished.get(5, TimeUnit.SECONDS);
        Mockito.verify(listener).error(failure);
    }

    private static void assertUnavailableConnectionReturnsToModel(@NotNull AIFunctionType type) throws Exception {
        AIAssistantImpl assistant = assistant();
        AIFunctionCall call = call(type);
        call.setArguments(Map.of(AIFunctionParams.CONNECTION_ID_PARAM, "unavailable"));
        CompletableFuture<AIChatRequest> followUp = captureFollowUp(assistant);
        AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);

        processCalls(assistant, listener, List.of(call));

        AIChatRequest request = followUp.get(5, TimeUnit.SECONDS);
        Assertions.assertInstanceOf(DBException.class, request.messages().getFirst().getError());
        Assertions.assertTrue(request.messages().getFirst().getContent().contains("unavailable"));
        Mockito.verify(assistant, Mockito.never()).callFunction(Mockito.any(), Mockito.any());
        Mockito.verify(listener).processFunctionCall(Mockito.argThat(message -> message.getError() != null));
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
        Mockito.verify(listener, Mockito.never()).complete(Mockito.anyList(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @NotNull
    private static AIAssistantImpl assistant() {
        AIAssistantImpl assistant = Mockito.spy(new AIAssistantImpl(Mockito.mock(DBPWorkspace.class)));
        Mockito.doReturn(Mockito.mock(AIToolboxManager.class)).when(assistant).getToolboxManager();
        return assistant;
    }

    @NotNull
    private static AIFunctionCall call(@NotNull AIFunctionType type) {
        AIFunctionDescriptor descriptor = Mockito.mock(AIFunctionDescriptor.class);
        Mockito.when(descriptor.supportsConnectionRouting()).thenReturn(true);
        Mockito.when(descriptor.getType()).thenReturn(type);
        Mockito.when(descriptor.getId()).thenReturn("test_function");
        Mockito.when(descriptor.getFullId()).thenReturn("test_function");
        AIFunctionCall call = new AIFunctionCall("test_function", Map.of());
        call.setFunction(descriptor);
        return call;
    }

    @NotNull
    private static CompletableFuture<AIChatRequest> captureFollowUp(@NotNull AIAssistantImpl assistant) throws DBException {
        CompletableFuture<AIChatRequest> followUp = new CompletableFuture<>();
        Mockito.doAnswer(invocation -> {
            followUp.complete(invocation.getArgument(3));
            return CompletableFuture.completedFuture(invocation.getArgument(2));
        }).when(assistant).generateTextStream(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        return followUp;
    }

    private static void processCalls(
        @NotNull AIAssistantImpl assistant,
        @NotNull AIChatResponseConsumer listener,
        @NotNull List<AIFunctionCall> calls
    ) throws Exception {
        final AIChatSession session = Mockito.mock(AIChatSession.class);
        AIChatConversation conversation = Mockito.mock(AIChatConversation.class);
        Mockito.when(conversation.isActive()).thenReturn(true);
        Method method = AIAssistantImpl.class.getDeclaredMethod("processFunctionCalls",
            AIChatSession.class, AIChatConversation.class, AIChatResponseConsumer.class,
            AIFunctionContext.class, AIChatRequest.class, List.class);
        method.setAccessible(true);
        try (var runtime = Mockito.mockStatic(RuntimeUtils.class)) {
            runtime.when(() -> RuntimeUtils.scheduleJob(Mockito.anyString(), Mockito.any())).thenAnswer(invocation -> {
                DBRRunnableWithProgress runnable = invocation.getArgument(1);
                runnable.run(Mockito.mock(DBRProgressMonitor.class));
                return null;
            });
            method.invoke(assistant, session, conversation, listener,
                new AIFunctionContext(new VoidProgressMonitor(), null, Mockito.mock(AIPromptGenerator.class)),
                new AIChatRequest(null, List.of(), null), calls);
        }
    }

}
