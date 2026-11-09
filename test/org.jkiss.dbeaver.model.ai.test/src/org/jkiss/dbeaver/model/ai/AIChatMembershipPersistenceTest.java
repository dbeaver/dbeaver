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
import org.jkiss.dbeaver.model.ai.qm.*;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class AIChatMembershipPersistenceTest extends DBeaverUnitTest {
    private final DBPProject project = Mockito.mock(DBPProject.class);
    private final DBPDataSourceContainer primary = Mockito.mock(DBPDataSourceContainer.class);
    private final DBPDataSourceContainer additional = Mockito.mock(DBPDataSourceContainer.class);
    private final AIChatContextProvider provider = Mockito.mock(AIChatContextProvider.class);
    private final AIChatStorage storage = Mockito.mock(AIChatStorage.class);
    private final AIChatListener listener = Mockito.mock(AIChatListener.class);
    private AIChatConversation conversation;
    private AIChatSession session;

    @BeforeEach
    public void setUp() {
        Mockito.when(project.getId()).thenReturn("project");
        configure(primary, "primary");
        configure(additional, "additional");
        AIPromptGenerator prompt = Mockito.mock(AIPromptGenerator.class);
        Mockito.when(prompt.generatorId()).thenReturn("test");
        conversation = new AIChatConversation(UUID.randomUUID(), "Chat", prompt, List.of(), primary, 0, null);
        session = session(storage);
        session.addListener(listener);
    }

    @Test
    public void addingConnectionSavesMembershipAndNotifiesWithoutConnecting() throws DBException {
        session.addDataSource(new VoidProgressMonitor(), conversation, additional);
        ArgumentCaptor<QMAIConversationHistory> saved = ArgumentCaptor.forClass(QMAIConversationHistory.class);
        var order = Mockito.inOrder(storage, listener);
        order.verify(storage).saveConversation(Mockito.eq("session"), saved.capture());
        order.verify(listener).conversationConnectionsChanged(conversation);
        Assertions.assertEquals(List.of(new QMAIDataSource("project", "additional")),
            saved.getValue().getContext().getAdditionalDataSources());
        assertNoDatabaseAccess();
    }

    @Test
    public void removingConnectionSavesMembershipAndNotifiesWithoutConnecting() throws DBException {
        conversation.addDataSource(additional);
        session.removeDataSource(new VoidProgressMonitor(), conversation, "additional");
        ArgumentCaptor<QMAIConversationHistory> saved = ArgumentCaptor.forClass(QMAIConversationHistory.class);
        Mockito.verify(storage).saveConversation(Mockito.eq("session"), saved.capture());
        Assertions.assertTrue(saved.getValue().getContext().getAdditionalDataSources().isEmpty());
        Mockito.verify(listener).conversationConnectionsChanged(conversation);
        assertNoDatabaseAccess();
    }

    @Test
    public void savingSettingsDoesNotOpenDatabaseConnections() throws DBException {
        session.saveConversationSettings(new VoidProgressMonitor(), conversation);
        Mockito.verify(storage).saveConversation(Mockito.eq("session"), Mockito.any());
        assertNoDatabaseAccess();
    }

    @Test
    public void addingSeveralConnectionsDoesNotRetryTheOfflinePrimary() throws DBException {
        DBPDataSourceContainer second = Mockito.mock(DBPDataSourceContainer.class);
        configure(second, "second");
        session.addDataSource(new VoidProgressMonitor(), conversation, additional);
        session.addDataSource(new VoidProgressMonitor(), conversation, second);
        Mockito.verify(storage, Mockito.times(2)).saveConversation(Mockito.eq("session"), Mockito.any());
        Mockito.verify(listener, Mockito.times(2)).conversationConnectionsChanged(conversation);
        assertNoDatabaseAccess();
        Mockito.verify(second, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void unchangedMembershipDoesNotSaveOrNotify() throws DBException {
        conversation.addDataSource(additional);
        session.addDataSource(new VoidProgressMonitor(), conversation, additional);
        session.removeDataSource(new VoidProgressMonitor(), conversation, "missing");
        Mockito.verifyNoInteractions(storage, listener);
        assertNoDatabaseAccess();
    }

    @Test
    public void chipNotificationDoesNotWaitForAnOfflineConnectionAttempt() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        Mockito.when(primary.connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean())).thenAnswer(invocation -> {
            release.await(5, TimeUnit.SECONDS);
            return false;
        });
        FutureTask<Void> operation = new FutureTask<>(() -> {
            session.addDataSource(new VoidProgressMonitor(), conversation, additional);
            return null;
        });
        Thread worker = new Thread(operation);
        worker.start();
        try {
            operation.get(2, TimeUnit.SECONDS);
            Mockito.verify(listener).conversationConnectionsChanged(conversation);
            Mockito.verify(storage).saveConversation(Mockito.eq("session"), Mockito.any());
        } finally {
            release.countDown();
            worker.join(5000);
        }
        assertNoDatabaseAccess();
    }

    @Test
    public void settingsOnlySavePreservesPreviouslyRecordedContextObjects() throws DBException {
        QMAIChatStorageInMemory memory = new QMAIChatStorageInMemory();
        QMAIContextObject object = new QMAIContextObject("used_table", QMAIContextObjectType.TABLE);
        QMAIConversationHistory history = QMAIChatHistoryMapper.toQMAIChatHistory(conversation, null, null);
        history.setContext(new QMAIContext(null, Set.of(object)));
        memory.saveConversation("session", history);
        session(memory).addDataSource(new VoidProgressMonitor(), conversation, additional);
        QMAIContext saved = memory.findConversations("session").getFirst().getContext();
        Assertions.assertEquals(Set.of(object), saved.getObjects());
        Assertions.assertEquals(List.of(new QMAIDataSource("project", "additional")), saved.getAdditionalDataSources());
        assertNoDatabaseAccess();
    }

    @NotNull
    private AIChatSession session(@NotNull AIChatStorage chatStorage) {
        AIChatSession result = Mockito.spy(new AIChatSession(Mockito.mock(DBPWorkspace.class), provider,
            chatStorage, monitor -> "session", Mockito.mock(UserTokenQuotaService.class)));
        AIContextSettings settings = Mockito.mock(AIContextSettings.class);
        Mockito.when(settings.saveSettingsToString()).thenReturn("{\"scope\":\"CURRENT_DATASOURCE\"}");
        Mockito.doReturn(settings).when(result).getConversationSettings(conversation);
        return result;
    }

    private void configure(@NotNull DBPDataSourceContainer container, @NotNull String id) {
        Mockito.when(container.getId()).thenReturn(id);
        Mockito.when(container.getProject()).thenReturn(project);
    }

    private void assertNoDatabaseAccess() throws DBException {
        Mockito.verify(primary, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verify(additional, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verify(primary, Mockito.never()).getDataSource();
        Mockito.verifyNoInteractions(provider);
    }
}
