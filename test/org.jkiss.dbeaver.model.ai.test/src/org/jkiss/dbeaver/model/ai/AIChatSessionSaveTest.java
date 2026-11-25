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
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.qm.QMAIConversationHistory;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class AIChatSessionSaveTest extends DBeaverUnitTest {
    @Test
    public void deletionWaitsForInFlightSaveAndRejectsLaterSaves() throws Exception {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        AIChatSession session = session(storage);
        session.init(new VoidProgressMonitor());
        AIChatConversation conversation = conversation();
        session.addConversation(conversation);
        CountDownLatch saveEntered = new CountDownLatch(1);
        CountDownLatch releaseSave = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            saveEntered.countDown();
            Assertions.assertTrue(releaseSave.await(2, TimeUnit.SECONDS));
            return null;
        }).when(storage).saveConversation(Mockito.eq("session"), Mockito.any());
        FutureTask<Void> save = saveTask(session, conversation);
        FutureTask<Void> removal = new FutureTask<>(() -> {
            conversation.cancelConversation();
            session.removeConversation(conversation);
            return null;
        });
        Thread saveThread = new Thread(save);
        Thread removalThread = new Thread(removal);
        saveThread.start();
        try {
            Assertions.assertTrue(saveEntered.await(2, TimeUnit.SECONDS));
            removalThread.start();
            awaitBlockedOrCompleted(removalThread, removal);
            Assertions.assertFalse(removal.isDone());
        } finally {
            releaseSave.countDown();
            saveThread.join(2000);
            removalThread.join(2000);
        }
        save.get(2, TimeUnit.SECONDS);
        removal.get(2, TimeUnit.SECONDS);
        session.saveConversationSettings(new VoidProgressMonitor(), conversation);

        var order = Mockito.inOrder(storage);
        order.verify(storage).saveConversation(Mockito.eq("session"), Mockito.any());
        order.verify(storage).deleteConversation(conversation.getId().toString());
        Mockito.verify(storage, Mockito.times(1)).saveConversation(Mockito.eq("session"), Mockito.any());
        Assertions.assertFalse(session.hasConversation(conversation.getId()));
    }

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
