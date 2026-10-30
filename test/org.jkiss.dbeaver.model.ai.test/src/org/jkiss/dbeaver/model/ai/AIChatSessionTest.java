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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.quota.QuotaStatus;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.qm.QMUnavailableException;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AIChatSessionTest extends DBeaverUnitTest {
    @Test
    public void initializesHistoryWithCallerMonitor() throws DBException {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        Thread caller = Thread.currentThread();
        Mockito.when(storage.findConversationSummaries("session")).thenAnswer(invocation -> {
            Assertions.assertSame(caller, Thread.currentThread());
            return List.of();
        });
        AIChatSession session = new AIChatSession(
            Mockito.mock(DBPWorkspace.class), Mockito.mock(AIChatContextProvider.class), storage,
            actualMonitor -> {
                Assertions.assertSame(monitor, actualMonitor);
                return "session";
            }, Mockito.mock(UserTokenQuotaService.class)
        );

        session.init(monitor);
        session.init(monitor);
        Assertions.assertTrue(session.getAllConversations(null).isEmpty());
        Mockito.verify(storage).findConversationSummaries("session");
    }

    @Test
    public void concurrentRequestsWaitForInitialHistory() throws Exception {
        DBWorkbench.getPlatform();
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        CountDownLatch historyLoadStarted = new CountDownLatch(1);
        CountDownLatch releaseHistory = new CountDownLatch(1);
        Mockito.when(storage.findConversationSummaries("session")).thenAnswer(invocation -> {
            historyLoadStarted.countDown();
            if (!releaseHistory.await(5, TimeUnit.SECONDS)) {
                throw new DBException("Test history load timed out");
            }
            return List.of();
        });
        AIChatSession session = new AIChatSession(
            Mockito.mock(DBPWorkspace.class), Mockito.mock(AIChatContextProvider.class), storage,
            monitor -> "session", Mockito.mock(UserTokenQuotaService.class)
        );

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<List<AIChatConversation>> first = executor.submit(() -> session.getAllConversations(null));
            Assertions.assertTrue(historyLoadStarted.await(5, TimeUnit.SECONDS));
            Future<List<AIChatConversation>> second = executor.submit(() -> session.getAllConversations(null));
            try {
                Assertions.assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
            } finally {
                releaseHistory.countDown();
            }
            Assertions.assertTrue(first.get(5, TimeUnit.SECONDS).isEmpty());
            Assertions.assertTrue(second.get(5, TimeUnit.SECONDS).isEmpty());
        } finally {
            releaseHistory.countDown();
        }
    }

    @Test
    public void propagatesHistoryLoadFailureAndAllowsRetry() throws DBException {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        QMUnavailableException failure = new QMUnavailableException();
        Mockito.when(storage.findConversationSummaries("session")).thenThrow(failure).thenReturn(List.of());
        AIChatSession session = new AIChatSession(
            Mockito.mock(DBPWorkspace.class), Mockito.mock(AIChatContextProvider.class), storage,
            monitor -> "session", Mockito.mock(UserTokenQuotaService.class)
        );

        Assertions.assertSame(failure, Assertions.assertThrows(DBException.class, () -> session.init(new VoidProgressMonitor())));
        session.init(new VoidProgressMonitor());
        Assertions.assertTrue(session.getAllConversations(null).isEmpty());
        Assertions.assertTrue(session.getAllConversations(null).isEmpty());
        Mockito.verify(storage, Mockito.times(2)).findConversationSummaries("session");
    }

    @Test
    public void propagatesSessionProviderFailure() {
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        QMUnavailableException failure = new QMUnavailableException();
        AIChatSession session = new AIChatSession(
            Mockito.mock(DBPWorkspace.class), Mockito.mock(AIChatContextProvider.class), storage,
            monitor -> {
                throw failure;
            }, Mockito.mock(UserTokenQuotaService.class)
        );

        Assertions.assertSame(failure, Assertions.assertThrows(DBException.class, () -> session.init(new VoidProgressMonitor())));
        Mockito.verifyNoInteractions(storage);
    }

    @Test
    public void reportsEmptyCustomScopeWithoutConnectionWarning() throws DBException {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.isConnected()).thenReturn(true);
        AIContextSettings settings = Mockito.mock(AIContextSettings.class);
        Mockito.when(settings.getDataSourceContainer()).thenReturn(container);
        Mockito.when(settings.getScope()).thenReturn(AIDatabaseScope.CUSTOM);

        AIChatContextProvider contextProvider = Mockito.mock(AIChatContextProvider.class);
        Mockito.when(contextProvider.getExecutionContext(container)).thenReturn(Mockito.mock(DBCExecutionContext.class));
        UserTokenQuotaService quotaService = Mockito.mock(UserTokenQuotaService.class);
        Mockito.when(quotaService.getUserQuotaStatus("session", "engine")).thenReturn(QuotaStatus.EMPTY);
        AIChatSession session = new AIChatSession(
            Mockito.mock(DBPWorkspace.class), contextProvider, Mockito.mock(AIChatStorage.class), monitor -> "session", quotaService
        );
        AIConfigurationProfile profile = Mockito.mock(AIConfigurationProfile.class);
        Mockito.when(profile.getEngineId()).thenReturn("engine");
        AIPromptGenerator promptGenerator = Mockito.mock(AIPromptGenerator.class);
        Mockito.when(promptGenerator.configureDatabaseContext(Mockito.any(AIDatabaseContext.Builder.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        AIChatConversation conversation = new AIChatConversation(
            UUID.randomUUID(), "Chat", promptGenerator, List.of(), container, 0, profile
        );
        AIChatResponseConsumer consumer = Mockito.mock(AIChatResponseConsumer.class);

        Assertions.assertSame(
            conversation,
            session.processAICompletion(new VoidProgressMonitor(), conversation, consumer, settings, null).join()
        );

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(consumer).error(error.capture());
        Assertions.assertEquals(
            "Custom scope is empty. Add database objects or select a non-custom scope in the AI context settings.",
            error.getValue().getMessage()
        );
        Mockito.verify(consumer, Mockito.never()).warning(Mockito.anyString());
        Mockito.verify(consumer).complete(List.of(), true, false);
    }
}
