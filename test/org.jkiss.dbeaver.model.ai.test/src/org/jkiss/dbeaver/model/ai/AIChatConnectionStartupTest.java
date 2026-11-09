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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.quota.QuotaStatus;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIChatConnectionStartupTest extends DBeaverUnitTest {
    private final DBPProject project = Mockito.mock(DBPProject.class);
    private final DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
    private final Map<String, DBPDataSourceContainer> containers = new HashMap<>();
    private final Map<String, AtomicBoolean> connected = new HashMap<>();
    private final Map<String, Boolean> consent = new HashMap<>();
    private final DBRProgressMonitor progress = Mockito.mock(DBRProgressMonitor.class);
    private final AIChatResponseConsumer listener = Mockito.mock(AIChatResponseConsumer.class);
    private MockedConstruction<AIContextSettingsDataSource> settings;
    private MockedStatic<DBWorkbench> workbench;
    private DBPDataSourceContainer primary;
    private DBPDataSourceContainer additional;
    private DBPDataSourceContainer another;
    private AIChatConversation conversation;
    private AIChatSession session;
    private AIAssistant assistant;

    @BeforeEach
    public void setUp() throws DBException {
        Mockito.when(project.getId()).thenReturn("project");
        Mockito.when(project.getDataSourceRegistry()).thenReturn(registry);
        Mockito.when(registry.getDataSource(Mockito.anyString())).thenAnswer(invocation -> containers.get(invocation.getArgument(0)));
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        Mockito.when(workspace.getProjectById("project")).thenReturn(project);
        primary = container("primary");
        additional = container("additional");
        another = container("another");
        AIPromptGenerator prompt = Mockito.mock(AIPromptGenerator.class);
        Mockito.when(prompt.configureDatabaseContext(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        AIConfigurationProfile profile = Mockito.mock(AIConfigurationProfile.class);
        Mockito.when(profile.getEngineId()).thenReturn("engine");
        conversation = new AIChatConversation(UUID.randomUUID(), "Chat", prompt, List.of(), primary, 0, profile);
        conversation.addDataSource(additional);
        conversation.addDataSource(another);
        UserTokenQuotaService quota = Mockito.mock(UserTokenQuotaService.class);
        Mockito.when(quota.getUserQuotaStatus("session", "engine")).thenReturn(QuotaStatus.EMPTY);
        session = Mockito.spy(new AIChatSession(workspace, container -> Mockito.mock(DBCExecutionContext.class),
            Mockito.mock(AIChatStorage.class), monitor -> "session", quota));
        assistant = Mockito.mock(AIAssistant.class);
        Mockito.doReturn(assistant).when(session).getAssistant();
        settings = Mockito.mockConstruction(AIContextSettingsDataSource.class, (mock, construction) -> {
            DBPDataSourceContainer container = (DBPDataSourceContainer) construction.arguments().getFirst();
            Mockito.when(mock.getDataSourceContainer()).thenReturn(container);
            Mockito.when(mock.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
            Mockito.when(mock.isMetaTransferConfirmed()).thenAnswer(invocation -> consent.get(container.getId()));
        });
        DBPPlatform platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        workbench = Mockito.mockStatic(DBWorkbench.class);
        workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
    }

    @AfterEach
    public void tearDown() {
        workbench.close();
        settings.close();
    }

    @Test
    public void connectsAllAdditionalDatabases() throws DBException {
        session.connectAdditionalDataSources(progress, conversation, listener);
        Assertions.assertTrue(additional.isConnected());
        Assertions.assertTrue(another.isConnected());
        Mockito.verify(additional).connect(progress, true, true);
        Mockito.verify(another).connect(progress, true, true);
        Mockito.verify(primary, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verifyNoInteractions(listener);
    }

    @Test
    public void reusesAlreadyOpenConnections() throws DBException {
        connected.get("additional").set(true);
        session.connectAdditionalDataSources(progress, conversation, listener);
        Mockito.verify(additional, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verify(another).connect(progress, true, true);
    }

    @Test
    public void failedConnectionDoesNotPreventOtherConnections() throws DBException {
        Mockito.doReturn(false).when(additional).connect(progress, true, true);
        Mockito.when(additional.getConnectionError()).thenReturn("Server unavailable");
        session.connectAdditionalDataSources(progress, conversation, listener);
        Assertions.assertFalse(additional.isConnected());
        Assertions.assertTrue(another.isConnected());
        Mockito.verify(listener).warning(Mockito.argThat(message ->
            message.contains("additional") && message.contains("Server unavailable")));
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
        Mockito.verify(listener, Mockito.never()).complete(Mockito.anyList(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void skipsConnectionsWithoutMetadataConsent() throws DBException {
        consent.put("additional", false);
        session.connectAdditionalDataSources(progress, conversation, listener);
        Mockito.verify(additional, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verify(another).connect(progress, true, true);
        Mockito.verify(listener).warning(Mockito.argThat(message -> message.contains("not confirmed")));
    }

    @Test
    public void conversationOverrideCannotRestoreRevokedConsent() throws DBException {
        AIContextSettingsChatConversation override = Mockito.mock(AIContextSettingsChatConversation.class);
        Mockito.when(override.isMetaTransferConfirmed()).thenReturn(true);
        conversation.setCustomSettings(additional, override);
        consent.put("additional", false);
        session.connectAdditionalDataSources(progress, conversation, listener);
        Mockito.verify(additional, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Assertions.assertTrue(another.isConnected());
    }

    @Test
    public void deletedConnectionIsNotOpened() throws DBException {
        containers.remove("additional");
        session.connectAdditionalDataSources(progress, conversation, listener);
        Mockito.verify(additional, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verify(listener).warning(Mockito.argThat(message -> message.contains("no longer available")));
        Assertions.assertTrue(another.isConnected());
    }

    @Test
    public void cancelledRequestDoesNotOpenConnections() throws DBException {
        Mockito.when(progress.isCanceled()).thenReturn(true);
        session.connectAdditionalDataSources(progress, conversation, listener);
        Mockito.verify(additional, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verify(another, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void allConnectionsAreReadyBeforeTheFirstModelRequest() throws DBException {
        processAndCheckConnections(true);
    }

    @Test
    public void firstModelRequestContinuesWhenOneAdditionalDatabaseFails() throws DBException {
        Mockito.doReturn(false).when(additional).connect(progress, true, true);
        processAndCheckConnections(false);
        Mockito.verify(listener).warning(Mockito.anyString());
        Mockito.verify(listener, Mockito.never()).error(Mockito.any());
    }

    private void processAndCheckConnections(boolean additionalAvailable) throws DBException {
        Mockito.when(primary.isTemporary()).thenReturn(true);
        Mockito.when(assistant.generateTextStream(Mockito.same(progress), Mockito.same(session), Mockito.same(conversation),
            Mockito.any(), Mockito.same(listener))).thenAnswer(invocation -> {
                Assertions.assertTrue(primary.isConnected());
                Assertions.assertEquals(additionalAvailable, additional.isConnected());
                Assertions.assertTrue(another.isConnected());
                AIChatRequest request = invocation.getArgument(3);
                Assertions.assertSame(primary, request.context().getDataSource().getDataSourceContainer());
                return CompletableFuture.completedFuture(conversation);
            });
        Assertions.assertSame(conversation,
            session.processAICompletion(progress, conversation, listener, new AIContextSettingsDataSource(primary), null).join());
        Mockito.verify(assistant).generateTextStream(Mockito.same(progress), Mockito.same(session), Mockito.same(conversation),
            Mockito.any(), Mockito.same(listener));
    }

    @NotNull
    private DBPDataSourceContainer container(@NotNull String id) throws DBException {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        containers.put(id, container);
        AtomicBoolean state = new AtomicBoolean();
        connected.put(id, state);
        consent.put(id, true);
        Mockito.when(container.getId()).thenReturn(id);
        Mockito.when(container.getName()).thenReturn(id);
        Mockito.when(container.getProject()).thenReturn(project);
        Mockito.when(container.isConnected()).thenAnswer(invocation -> state.get());
        Mockito.when(container.connect(Mockito.any(), Mockito.eq(true), Mockito.eq(true))).thenAnswer(invocation -> {
            state.set(true);
            return true;
        });
        return container;
    }
}
