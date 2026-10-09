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
import org.jkiss.dbeaver.model.ai.qm.QMAIConversationHistory;
import org.jkiss.dbeaver.model.ai.quota.QuotaStatus;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AIChatSessionSaveTest extends DBeaverUnitTest {
    @Test
    public void serializesSavesAndTakesTheLatestMessageSnapshot() throws Exception {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        AIChatSession session = session(storage);
        AIChatConversation conversation = conversation();
        conversation.addMessage(AIMessage.userMessage("First"));
        List<QMAIConversationHistory> histories = new CopyOnWriteArrayList<>();
        CountDownLatch firstSaveEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstSave = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            histories.add(invocation.getArgument(1));
            if (histories.size() == 1) {
                firstSaveEntered.countDown();
                Assertions.assertTrue(releaseFirstSave.await(5, TimeUnit.SECONDS));
            }
            return null;
        }).when(storage).saveConversation(Mockito.eq("session"), Mockito.any());

        FutureTask<Void> firstSave = saveTask(session, conversation);
        FutureTask<Void> secondSave = saveTask(session, conversation);
        Thread firstThread = new Thread(firstSave);
        Thread secondThread = new Thread(secondSave);
        firstThread.start();
        try {
            Assertions.assertTrue(firstSaveEntered.await(5, TimeUnit.SECONDS));
            secondThread.start();
            awaitBlockedOrCompleted(secondThread, secondSave);
            Assertions.assertFalse(secondSave.isDone(), "The second save must wait for the first storage write");
            conversation.addMessage(AIMessage.userMessage("Second"));
        } finally {
            releaseFirstSave.countDown();
            firstThread.join(5000);
            secondThread.join(5000);
        }
        firstSave.get(5, TimeUnit.SECONDS);
        secondSave.get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(List.of(1, 2), histories.stream().map(history -> history.getMessages().size()).toList());
        Assertions.assertEquals(List.of(0, 1), histories.getLast().getMessages().stream().map(message -> message.id()).toList());
    }

    @Test
    public void serializesSettingsSaveWithCompletionContextSave() throws Exception {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        UserTokenQuotaService quotaService = Mockito.mock(UserTokenQuotaService.class);
        Mockito.when(quotaService.getUserQuotaStatus("session", "engine")).thenReturn(QuotaStatus.EMPTY);
        DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        AIChatSession session = Mockito.spy(new AIChatSession(Mockito.mock(DBPWorkspace.class),
            container -> executionContext, storage, monitor -> "session", quotaService));
        AIAssistant assistant = Mockito.mock(AIAssistant.class);
        Mockito.doReturn(assistant).when(session).getAssistant();
        AIChatConversation conversation = conversation();
        AIConfigurationProfile profile = Mockito.mock(AIConfigurationProfile.class);
        Mockito.when(profile.getEngineId()).thenReturn("engine");
        conversation.setProfile(profile);
        conversation.addMessage(AIMessage.userMessage("First"));
        AIChatResponseConsumer consumer = Mockito.mock(AIChatResponseConsumer.class);
        Mockito.when(assistant.generateTextStream(Mockito.any(), Mockito.eq(session), Mockito.eq(conversation),
            Mockito.any(), Mockito.eq(consumer))).thenReturn(CompletableFuture.completedFuture(conversation));

        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.isConnected()).thenReturn(true);
        Mockito.when(container.getExtension(AIContextSettingsDataSource.AI_DS_EXTENSION)).thenReturn(Map.of());
        AIContextSettings settings = Mockito.mock(AIContextSettings.class);
        Mockito.when(settings.getDataSourceContainer()).thenReturn(container);
        Mockito.when(settings.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        CountDownLatch contextPrepared = new CountDownLatch(1);
        CountDownLatch releaseContext = new CountDownLatch(1);
        Mockito.when(conversation.getPromptGenerator().configureDatabaseContext(Mockito.any(AIDatabaseContext.Builder.class)))
            .thenAnswer(invocation -> {
                contextPrepared.countDown();
                Assertions.assertTrue(releaseContext.await(5, TimeUnit.SECONDS));
                return invocation.getArgument(0);
            });

        List<QMAIConversationHistory> histories = new CopyOnWriteArrayList<>();
        CountDownLatch settingsSaveEntered = new CountDownLatch(1);
        CountDownLatch releaseSettingsSave = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            histories.add(invocation.getArgument(1));
            if (histories.size() == 1) {
                settingsSaveEntered.countDown();
                Assertions.assertTrue(releaseSettingsSave.await(5, TimeUnit.SECONDS));
            }
            return null;
        }).when(storage).saveConversation(Mockito.eq("session"), Mockito.any());

        FutureTask<Void> completion = new FutureTask<>(() -> {
            try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
                DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
                workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
                Assertions.assertSame(conversation,
                    session.processAICompletion(new VoidProgressMonitor(), conversation, consumer, settings, null).join());
            }
            return null;
        });
        FutureTask<Void> settingsSave = saveTask(session, conversation);
        Thread completionThread = new Thread(completion);
        Thread settingsThread = new Thread(settingsSave);
        completionThread.start();
        try {
            // pause completion after its image-loading lock so the test reaches the separate context-save path
            boolean prepared = contextPrepared.await(5, TimeUnit.SECONDS);
            if (!prepared && completion.isDone()) {
                completion.get(5, TimeUnit.SECONDS);
            }
            Assertions.assertTrue(prepared, "Completion must prepare its database context");
            settingsThread.start();
            Assertions.assertTrue(settingsSaveEntered.await(5, TimeUnit.SECONDS));
            conversation.addMessage(AIMessage.userMessage("Second"));
            releaseContext.countDown();
            Assertions.assertThrows(TimeoutException.class, () -> completion.get(1, TimeUnit.SECONDS));
            Assertions.assertEquals(1, histories.size(), "Completion must wait for the settings storage write");
        } finally {
            releaseContext.countDown();
            releaseSettingsSave.countDown();
            completionThread.join(5000);
            settingsThread.join(5000);
        }
        settingsSave.get(5, TimeUnit.SECONDS);
        completion.get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(List.of(1, 2), histories.stream().map(history -> history.getMessages().size()).toList());
        Assertions.assertEquals(1, histories.getLast().getContext().getObjects().size());
        Mockito.verify(assistant).generateTextStream(Mockito.any(), Mockito.eq(session), Mockito.eq(conversation),
            Mockito.any(), Mockito.eq(consumer));
        Mockito.verifyNoInteractions(consumer);
    }

    @Test
    public void savesWhilePromptSubmissionHoldsConversationMonitor() throws Exception {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        AIChatSession session = session(storage);
        AIChatConversation conversation = conversation();
        FutureTask<Void> save = saveTask(session, conversation);
        Thread saveThread = new Thread(save);
        try {
            // cloudbeaver waits for the save job while holding the conversation monitor
            synchronized (conversation) {
                saveThread.start();
                save.get(2, TimeUnit.SECONDS);
            }
        } finally {
            saveThread.join(5000);
        }
        Mockito.verify(storage).saveConversation(Mockito.eq("session"), Mockito.any());
    }

    @Test
    public void savesOtherConversationsWhileOneStorageWriteIsBlocked() throws Exception {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        AIChatSession session = session(storage);
        AIChatConversation first = conversation();
        AIChatConversation second = conversation();
        CountDownLatch firstSaveEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstSave = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            QMAIConversationHistory history = invocation.getArgument(1);
            if (history.getId().equals(first.getId().toString())) {
                firstSaveEntered.countDown();
                Assertions.assertTrue(releaseFirstSave.await(5, TimeUnit.SECONDS));
            }
            return null;
        }).when(storage).saveConversation(Mockito.eq("session"), Mockito.any());

        FutureTask<Void> firstSave = saveTask(session, first);
        FutureTask<Void> secondSave = saveTask(session, second);
        Thread firstThread = new Thread(firstSave);
        Thread secondThread = new Thread(secondSave);
        firstThread.start();
        try {
            Assertions.assertTrue(firstSaveEntered.await(5, TimeUnit.SECONDS));
            secondThread.start();
            secondSave.get(5, TimeUnit.SECONDS);
            Assertions.assertFalse(firstSave.isDone());
        } finally {
            releaseFirstSave.countDown();
            firstThread.join(5000);
            secondThread.join(5000);
        }
        firstSave.get(5, TimeUnit.SECONDS);
    }

    @Test
    public void allowsRetryAfterStorageFailure() throws DBException {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        AIChatSession session = session(storage);
        AIChatConversation conversation = conversation();
        DBException failure = new DBException("Storage unavailable");
        Mockito.doThrow(failure).doNothing().when(storage).saveConversation(Mockito.eq("session"), Mockito.any());

        Assertions.assertSame(failure, Assertions.assertThrows(DBException.class,
            () -> session.saveConversationSettings(new VoidProgressMonitor(), conversation)));
        session.saveConversationSettings(new VoidProgressMonitor(), conversation);
        Mockito.verify(storage, Mockito.times(2)).saveConversation(Mockito.eq("session"), Mockito.any());
    }

    private static void awaitBlockedOrCompleted(@NotNull Thread thread, @NotNull FutureTask<Void> task) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (thread.getState() != Thread.State.BLOCKED && !task.isDone()) {
            Assertions.assertTrue(System.nanoTime() < deadline, "The second save did not reach storage");
            Thread.sleep(1);
        }
    }

    @NotNull
    private static FutureTask<Void> saveTask(@NotNull AIChatSession session, @NotNull AIChatConversation conversation) {
        return new FutureTask<>(() -> {
            session.saveConversationSettings(new VoidProgressMonitor(), conversation);
            return null;
        });
    }

    @NotNull
    private static AIChatSession session(@NotNull AIChatStorage storage) {
        return new AIChatSession(Mockito.mock(DBPWorkspace.class), Mockito.mock(AIChatContextProvider.class), storage,
            monitor -> "session", Mockito.mock(UserTokenQuotaService.class));
    }

    @NotNull
    private static AIChatConversation conversation() {
        return new AIChatConversation(UUID.randomUUID(), "Chat", Mockito.mock(AIPromptGenerator.class), List.of(), null, 0, null);
    }
}
