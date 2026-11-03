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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.functions.AIFunctionParams;
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.qm.QMAIContext;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.ai.qm.QMAIDataSource;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionInternalRegistry;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class AIMultiConnectionTest extends DBeaverUnitTest {
    private final DBPProject project = project("project");
    private final DBPDataSourceContainer primary = container("primary", project);
    private final DBPDataSourceContainer additional = container("additional", project);

    @Test
    public void defaultsToPrimaryAndResolvesOnlyAttachedConnections() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        Assertions.assertSame(primary, conversation.getDataSource(null));
        Assertions.assertSame(primary, conversation.getDataSource(""));
        Assertions.assertSame(primary, conversation.getDataSource("  "));
        Assertions.assertSame(primary, conversation.getDataSource("primary"));
        Assertions.assertSame(additional, conversation.getDataSource("additional"));
        Assertions.assertThrows(DBException.class, () -> conversation.getDataSource("other"));
    }

    @Test
    public void avoidsDuplicateConnectionsAndKeepsPrimaryFixed() throws DBException {
        AIChatConversation conversation = conversation(primary);
        Assertions.assertFalse(conversation.addDataSource(primary));
        Assertions.assertTrue(conversation.addDataSource(additional));
        Assertions.assertFalse(conversation.addDataSource(container("additional", project)));
        Assertions.assertEquals(List.of(primary, additional), conversation.getDataSources());
        Assertions.assertFalse(conversation.removeDataSource("primary"));
        Assertions.assertSame(primary, conversation.getDataSource());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> conversation.getDataSources().clear());
    }

    @Test
    public void rejectsConnectionsFromAnotherProjectAndWithoutPrimary() {
        Assertions.assertThrows(DBException.class,
            () -> conversation(primary).addDataSource(container("other", project("other-project"))));
        AIChatConversation conversation = conversation(null);
        Assertions.assertThrows(DBException.class, () -> conversation.addDataSource(additional));
        Assertions.assertNull(conversation.getDataSource());
    }

    @Test
    public void removedConnectionCannotBeResolved() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        Assertions.assertTrue(conversation.removeDataSource("additional"));
        Assertions.assertThrows(DBException.class, () -> conversation.getDataSource("additional"));
        Assertions.assertEquals(List.of(primary), conversation.getDataSources());
    }

    @Test
    public void membershipCannotChangeDuringRequest() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        conversation.startConversation();
        Assertions.assertThrows(DBException.class, () -> conversation.addDataSource(container("other", project)));
        Assertions.assertThrows(DBException.class, () -> conversation.removeDataSource("additional"));
        conversation.promptProcessed(true);
        Assertions.assertTrue(conversation.removeDataSource("additional"));
    }

    @Test
    public void selectsDatabaseContextAndPreservesCallCounter() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        AIDatabaseContext primaryContext = databaseContext(primary);
        AIDatabaseContext additionalContext = databaseContext(additional);
        AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), primaryContext,
            conversation.getPromptGenerator(), conversation, connectionId ->
                conversation.getDataSource(connectionId) == primary ? primaryContext : additionalContext);
        AIFunctionContext selected = context.withConnection("additional");
        Assertions.assertSame(additionalContext, selected.getContext());
        Assertions.assertSame(primaryContext, context.getContext());
        Assertions.assertSame(primaryContext, selected.withConnection(null).getContext());
        selected.addFunctionCall(new AIFunctionCall());
        Assertions.assertEquals(1, context.getFunctionCalls().size());
        conversation.removeDataSource("additional");
        Assertions.assertThrows(DBException.class, () -> context.withConnection("additional"));
    }

    @Test
    public void singleContextRejectsUnrelatedConnectionIds() throws DBException {
        AIDatabaseContext primaryContext = databaseContext(primary);
        AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), primaryContext,
            Mockito.mock(AIPromptGenerator.class));
        Assertions.assertSame(primaryContext, context.withConnection("primary").getContext());
        Assertions.assertSame(primaryContext, context.withConnection("").getContext());
        Assertions.assertThrows(DBException.class, () -> context.withConnection("other"));
        Assertions.assertThrows(DBException.class, () -> context.forFunction(Mockito.mock(AIFunctionDescriptor.class),
            Map.of(AIFunctionParams.CONNECTION_ID_PARAM, 123)));
    }

    @Test
    public void registryRoutesDatabaseFunctionAndRejectsForeignIdBeforeCallingIt() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        AIDatabaseContext primaryContext = databaseContext(primary);
        AIDatabaseContext additionalContext = databaseContext(additional);
        AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), primaryContext,
            conversation.getPromptGenerator(), conversation, connectionId ->
                conversation.getDataSource(connectionId) == primary ? primaryContext : additionalContext);
        AIFunctionDescriptor descriptor = Mockito.mock(AIFunctionDescriptor.class);
        AIFunction function = Mockito.mock(AIFunction.class);
        Mockito.when(descriptor.getInstance()).thenReturn(function);
        Mockito.when(descriptor.getParameter(AIFunctionParams.CONNECTION_ID_PARAM))
            .thenReturn(Mockito.mock(AIFunctionParameter.class));
        AIFunctionInternalRegistry registry = Mockito.mock(AIFunctionInternalRegistry.class, Mockito.CALLS_REAL_METHODS);
        registry.callFunction(context, descriptor, Map.of(AIFunctionParams.CONNECTION_ID_PARAM, "additional"));
        Mockito.verify(function).callFunction(Mockito.argThat(selected -> selected.getContext() == additionalContext), Mockito.anyMap());
        Mockito.clearInvocations(function);
        Assertions.assertThrows(DBException.class,
            () -> registry.callFunction(context, descriptor, Map.of(AIFunctionParams.CONNECTION_ID_PARAM, "other")));
        Mockito.verifyNoInteractions(function);
        registry.callFunction(context, descriptor, Map.of());
        Mockito.verify(function).callFunction(Mockito.argThat(selected -> selected.getContext() == primaryContext), Mockito.anyMap());
    }

    @Test
    public void legacyHistoryAndAdditionalConnectionsRoundTrip() {
        QMAIContext legacy = new QMAIContext("{\"confirmed\":true,\"scope\":\"CURRENT_SCHEMA\"}", Set.of());
        Assertions.assertTrue(legacy.getAdditionalDataSources().isEmpty());
        List<QMAIDataSource> sources = List.of(new QMAIDataSource("project", "additional"));
        QMAIContext saved = legacy.withAdditionalDataSources(sources);
        QMAIContext restored = new QMAIContext(saved.getContextJson(), Set.of());
        Assertions.assertEquals(sources, restored.getAdditionalDataSources());
        Assertions.assertTrue(restored.getContextJson().contains("\"confirmed\":true"));
        Assertions.assertTrue(new QMAIContext(null, Set.of()).getAdditionalDataSources().isEmpty());
    }

    @Test
    public void deletedConnectionIsRejectedBeforeOpeningDatabase() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        Mockito.when(workspace.getProjectById("project")).thenReturn(project);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(Mockito.mock(DBPDataSourceRegistry.class));
        AIChatContextProvider provider = Mockito.mock(AIChatContextProvider.class);
        AIChatSession session = new AIChatSession(workspace, provider,
            Mockito.mock(AIChatStorage.class), monitor -> "session",
            Mockito.mock(UserTokenQuotaService.class));
        AIFunctionContext context = session.createFunctionContext(new VoidProgressMonitor(), conversation, databaseContext(primary));
        Assertions.assertThrows(DBException.class, () -> context.withConnection("additional"));
        Mockito.verifyNoInteractions(provider);
    }

    @Test
    public void additionalConnectionRequiresItsOwnMetadataConsent() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        AIChatContextProvider provider = Mockito.mock(AIChatContextProvider.class);
        AIChatSession session = registeredSession(provider);
        try (var settings = Mockito.mockConstruction(AIContextSettingsDataSource.class)) {
            AIFunctionContext context = session.createFunctionContext(new VoidProgressMonitor(), conversation, databaseContext(primary));
            DBException exception = Assertions.assertThrows(DBException.class, () -> context.withConnection("additional"));
            Assertions.assertTrue(exception.getMessage().contains("not confirmed"));
            Assertions.assertEquals(1, settings.constructed().size());
            Mockito.verifyNoInteractions(provider);
        }
    }

    @Test
    public void additionalConnectionUsesItsOwnScope() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        Mockito.when(additional.isConnected()).thenReturn(true);
        Mockito.when(conversation.getPromptGenerator().configureDatabaseContext(Mockito.any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        AIChatContextProvider provider = Mockito.mock(AIChatContextProvider.class);
        Mockito.when(provider.getExecutionContext(additional)).thenReturn(Mockito.mock(DBCExecutionContext.class));
        AIChatSession session = registeredSession(provider);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        try (var platformMock = Mockito.mockStatic(DBWorkbench.class);
             var settings = Mockito.mockConstruction(AIContextSettingsDataSource.class, (mock, construction) -> {
            Mockito.when(mock.isMetaTransferConfirmed()).thenReturn(true);
            Mockito.when(mock.getDataSourceContainer()).thenReturn(additional);
            Mockito.when(mock.getScope()).thenReturn(AIDatabaseScope.CUSTOM);
        })) {
            platformMock.when(DBWorkbench::getPlatform).thenReturn(platform);
            AIFunctionContext context = session.createFunctionContext(new VoidProgressMonitor(), conversation, databaseContext(primary));
            DBException exception = Assertions.assertThrows(DBException.class, () -> context.withConnection("additional"));
            Assertions.assertTrue(exception.getMessage().startsWith("Custom scope is empty"));
            Mockito.verify(provider).getExecutionContext(additional);
            Mockito.verify(provider, Mockito.never()).getExecutionContext(primary);
        }
    }

    @Test
    public void additionalConnectionKeepsItsExecutionContextAndScope() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        Mockito.when(additional.isConnected()).thenReturn(true);
        DBCExecutionContext primaryExecutionContext = Mockito.mock(DBCExecutionContext.class);
        DBCExecutionContext additionalExecutionContext = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(conversation.getPromptGenerator().configureDatabaseContext(Mockito.any())).thenAnswer(invocation ->
            ((AIDatabaseContext.Builder) invocation.getArgument(0))
                .setExecutionContext(primaryExecutionContext).setScope(AIDatabaseScope.CURRENT_DATABASE));
        AIChatContextProvider provider = Mockito.mock(AIChatContextProvider.class);
        Mockito.when(provider.getExecutionContext(additional)).thenReturn(additionalExecutionContext);
        AIChatSession session = registeredSession(provider);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        try (var platformMock = Mockito.mockStatic(DBWorkbench.class);
             var settings = Mockito.mockConstruction(AIContextSettingsDataSource.class, (mock, construction) -> {
                 Mockito.when(mock.isMetaTransferConfirmed()).thenReturn(true);
                 Mockito.when(mock.getDataSourceContainer()).thenReturn(additional);
                 Mockito.when(mock.getScope()).thenReturn(AIDatabaseScope.CURRENT_SCHEMA);
             })) {
            platformMock.when(DBWorkbench::getPlatform).thenReturn(platform);
            AIFunctionContext context = session.createFunctionContext(new VoidProgressMonitor(), conversation, databaseContext(primary));
            AIDatabaseContext selected = context.withConnection("additional").getContext();
            Assertions.assertSame(additional, selected.getDataSource().getDataSourceContainer());
            Assertions.assertSame(additionalExecutionContext, selected.getExecutionContext());
            Assertions.assertEquals(AIDatabaseScope.CURRENT_SCHEMA, selected.getScope());
            Mockito.verify(conversation.getPromptGenerator(), Mockito.never()).configureDatabaseContext(Mockito.any());
        }
    }

    @Test
    public void primaryCannotUseAnotherConnectionsContext() throws DBException {
        AIChatConversation conversation = conversation(primary);
        AIChatSession session = registeredSession(Mockito.mock(AIChatContextProvider.class));
        AIFunctionContext context = session.createFunctionContext(new VoidProgressMonitor(), conversation, databaseContext(additional));
        Assertions.assertThrows(DBException.class, () -> context.withConnection(null));
    }

    @Test
    public void sourceAndTargetOverrideCommonConnectionIndependently() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        AIDatabaseContext primaryContext = databaseContext(primary);
        AIDatabaseContext additionalContext = databaseContext(additional);
        AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), primaryContext,
            conversation.getPromptGenerator(), conversation, connectionId ->
                conversation.getDataSource(connectionId) == primary ? primaryContext : additionalContext);
        AIFunctionContext selected = context.withConnection("additional");
        Assertions.assertSame(additionalContext, selected.withConnectionParameter(Map.of(),
            AIFunctionParams.SOURCE_CONNECTION_ID_PARAM).getContext());
        Assertions.assertSame(primaryContext, selected.withConnectionParameter(Map.of(
            AIFunctionParams.TARGET_CONNECTION_ID_PARAM, ""), AIFunctionParams.TARGET_CONNECTION_ID_PARAM).getContext());
        Assertions.assertSame(additionalContext, context.withConnectionParameter(Map.of(
            AIFunctionParams.SOURCE_CONNECTION_ID_PARAM, "additional"), AIFunctionParams.SOURCE_CONNECTION_ID_PARAM).getContext());
    }

    @Test
    public void invalidTargetPreventsTheWholeOperation() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        AIDatabaseContext primaryContext = databaseContext(primary);
        AIDatabaseContext additionalContext = databaseContext(additional);
        AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), primaryContext,
            conversation.getPromptGenerator(), conversation, connectionId ->
                conversation.getDataSource(connectionId) == primary ? primaryContext : additionalContext);
        AIFunctionDescriptor descriptor = Mockito.mock(AIFunctionDescriptor.class);
        AIFunction function = Mockito.mock(AIFunction.class);
        Mockito.when(descriptor.getInstance()).thenReturn(function);
        AIFunctionInternalRegistry registry = Mockito.mock(AIFunctionInternalRegistry.class, Mockito.CALLS_REAL_METHODS);
        Assertions.assertThrows(DBException.class, () -> registry.callFunction(context, descriptor, Map.of(
            AIFunctionParams.SOURCE_CONNECTION_ID_PARAM, "additional",
            AIFunctionParams.TARGET_CONNECTION_ID_PARAM, "other")));
        Mockito.verifyNoInteractions(function);
        Assertions.assertThrows(DBException.class, () -> registry.callFunction(context, descriptor, Map.of(
            AIFunctionParams.TARGET_CONNECTION_ID_PARAM, 123)));
        Mockito.verifyNoInteractions(function);
    }

    @Test
    public void sourceAndTargetTransformersReceiveTheirOwnContexts() throws DBException {
        AIChatConversation conversation = conversation(primary);
        conversation.addDataSource(additional);
        AIDatabaseContext primaryContext = databaseContext(primary);
        AIDatabaseContext additionalContext = databaseContext(additional);
        AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), primaryContext,
            conversation.getPromptGenerator(), conversation, connectionId ->
                conversation.getDataSource(connectionId) == primary ? primaryContext : additionalContext);
        AIFunctionDescriptor descriptor = Mockito.mock(AIFunctionDescriptor.class);
        AIFunctionParameter tableParameter = Mockito.mock(AIFunctionParameter.class);
        Mockito.when(tableParameter.getTransformer()).thenReturn((databaseContext, functionContext, function, parameter, value) -> {
            Assertions.assertSame(databaseContext, functionContext.getContext());
            return databaseContext.getDataSource().getDataSourceContainer().getId();
        });
        Mockito.when(tableParameter.getTransformerSuffix()).thenReturn("id");
        Mockito.when(descriptor.getParameter("sourceTable")).thenReturn(tableParameter);
        Mockito.when(descriptor.getParameter("targetTable")).thenReturn(tableParameter);
        Mockito.when(descriptor.getParameter(AIFunctionParams.SOURCE_CONNECTION_ID_PARAM))
            .thenReturn(Mockito.mock(AIFunctionParameter.class));
        Mockito.when(descriptor.getParameter(AIFunctionParams.TARGET_CONNECTION_ID_PARAM))
            .thenReturn(Mockito.mock(AIFunctionParameter.class));
        AIFunctionCall call = new AIFunctionCall("transferData", Map.of(
            "sourceTable", "orders", "targetTable", "orders_copy",
            AIFunctionParams.SOURCE_CONNECTION_ID_PARAM, "additional",
            AIFunctionParams.TARGET_CONNECTION_ID_PARAM, "primary"));
        call.setFunction(descriptor);
        call.transformArguments(primaryContext, context);
        Assertions.assertEquals("additional", call.getArguments().get("sourceTable_id"));
        Assertions.assertEquals("primary", call.getArguments().get("targetTable_id"));
    }

    @Test
    public void temporaryPrimaryDoesNotRequireRegistryEntry() throws DBException {
        AIChatConversation conversation = conversation(primary);
        Mockito.when(primary.isTemporary()).thenReturn(true);
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        Mockito.when(workspace.getProjectById("project")).thenReturn(project);
        AIChatSession session = new AIChatSession(workspace, Mockito.mock(AIChatContextProvider.class),
            Mockito.mock(AIChatStorage.class), monitor -> "session", Mockito.mock(UserTokenQuotaService.class));
        AIDatabaseContext primaryContext = databaseContext(primary);
        AIFunctionContext context = session.createFunctionContext(new VoidProgressMonitor(), conversation, primaryContext);
        Assertions.assertSame(primaryContext, context.withConnection(null).getContext());
    }

    @Test
    public void backgroundConnectionChangesDoNotSwitchConversation() throws Exception {
        AIChatConversation conversation = conversation(primary);
        Mockito.when(primary.isTemporary()).thenReturn(true);
        AIChatSession session = registeredSession(Mockito.mock(AIChatContextProvider.class));
        AIChatListener listener = Mockito.mock(AIChatListener.class);
        session.addListener(listener);
        FutureTask<Void> task = new FutureTask<>(() -> {
            session.addDataSource(new VoidProgressMonitor(), conversation, additional);
            session.addDataSource(new VoidProgressMonitor(), conversation, additional);
            session.removeDataSource(new VoidProgressMonitor(), conversation, "missing");
            session.removeDataSource(new VoidProgressMonitor(), conversation, "additional");
            return null;
        });
        Thread thread = new Thread(task);
        thread.start();
        try {
            task.get(5, TimeUnit.SECONDS);
        } finally {
            task.cancel(true);
            thread.join(5000);
        }
        Mockito.verify(listener, Mockito.times(2)).conversationConnectionsChanged(conversation);
        Mockito.verify(listener, Mockito.never()).conversationChanged(Mockito.any());
        Assertions.assertEquals(List.of(primary), conversation.getDataSources());
        Assertions.assertSame(primary, conversation.getDataSource());
    }

    @NotNull
    private AIChatSession registeredSession(@NotNull AIChatContextProvider provider) {
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        Mockito.when(workspace.getProjectById("project")).thenReturn(project);
        DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(registry);
        Mockito.when(registry.getDataSource("primary")).thenReturn(primary);
        Mockito.when(registry.getDataSource("additional")).thenReturn(additional);
        return new AIChatSession(workspace, provider, Mockito.mock(AIChatStorage.class), monitor -> "session",
            Mockito.mock(UserTokenQuotaService.class));
    }

    @NotNull
    private static AIChatConversation conversation(@Nullable DBPDataSourceContainer primary) {
        return new AIChatConversation(UUID.randomUUID(), "Chat", Mockito.mock(AIPromptGenerator.class), List.of(), primary, 0, null);
    }

    @NotNull
    private static DBPProject project(@NotNull String id) {
        DBPProject project = Mockito.mock(DBPProject.class);
        Mockito.when(project.getId()).thenReturn(id);
        return project;
    }

    @NotNull
    private static DBPDataSourceContainer container(@NotNull String id, @NotNull DBPProject project) {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getId()).thenReturn(id);
        Mockito.when(container.getProject()).thenReturn(project);
        return container;
    }

    @NotNull
    private static AIDatabaseContext databaseContext(@NotNull DBPDataSourceContainer container) {
        AIDatabaseContext context = Mockito.mock(AIDatabaseContext.class);
        DBSLogicalDataSource dataSource = Mockito.mock(DBSLogicalDataSource.class);
        Mockito.when(context.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getDataSourceContainer()).thenReturn(container);
        return context;
    }
}
