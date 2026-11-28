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
package org.jkiss.dbeaver.model.datadam.rpc;

import com.dbeaver.datadam.dam.remote.api.model.DDConnectedRemoteDam;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.datadam.transport.DBDamChannel;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Timeout(5)
class DBDamNameServiceTest {
    private static final String CLIENT_ID = "desktop-client";
    private static final String DEFAULT_NAME = "DBeaver Test: workstation";
    private final DDAccountClient account = Mockito.mock(DDAccountClient.class);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void initializesAndCachesTheNameOnTheSameSessionAfterDelayedRegistrationOrFailure(boolean requestFails) throws Exception {
        DBWorkbench.getPlatform();
        var channel = Mockito.mock(DBDamChannel.class);
        var status = new AtomicReference<>(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null));
        var observer = new AtomicReference<Consumer<DDRemoteDamChannelStatus>>();
        Mockito.when(channel.getStatus()).thenAnswer(invocation -> status.get());
        Mockito.when(channel.getClientDamId()).thenReturn(CLIENT_ID);
        Mockito.when(channel.getMonitor()).thenReturn(new DDConnectionMonitor());
        Mockito.when(channel.observeState(Mockito.any())).thenAnswer(invocation -> {
            observer.set(invocation.getArgument(0));
            observer.get().accept(status.get());
            return (DDRemoteDamSubscription) () -> {};
        });
        if (requestFails) {
            Mockito.when(account.getDam(CLIENT_ID)).thenThrow(new DBException("Unavailable")).thenReturn(dam(null));
        } else {
            Mockito.when(account.getDam(CLIENT_ID)).thenReturn(null, dam(null));
        }
        Mockito.when(account.canRenameDam()).thenReturn(true);
        Mockito.when(account.renameDam(Mockito.eq(CLIENT_ID), Mockito.anyString()))
            .thenAnswer(invocation -> dam(invocation.getArgument(1)));
        var naming = new DBDamNameService(() -> true);
        var retries = new LinkedBlockingQueue<IJobChangeEvent>();
        var completed = new CountDownLatch(1);
        try (var session = new DBDamSessionService(
            Mockito.mock(DBDamAccessService.class), channel, Mockito.mock(DBDamRpcRegistrationRegistry.class)
        )) {
            var jobs = new JobChangeAdapter() {
                @Override
                public void scheduled(@NotNull IJobChangeEvent event) {
                    if (event.getJob().getName().equals("Initialize DataDam desktop name") && event.getDelay() > 0) {
                        retries.add(event);
                    }
                }

                @Override
                public void done(@NotNull IJobChangeEvent event) {
                    if (session.getCachedDam() != null) {
                        completed.countDown();
                    }
                }
            };
            Job.getJobManager().addJobChangeListener(jobs);
            try {
                naming.activateService(session, account);
                Mockito.verifyNoInteractions(account);
                status.set(new DDRemoteDamChannelStatus(
                    DDRemoteDamChannelState.CONNECTED, new DDRemoteDamSessionInfo("deployment", "instance", "session")));
                observer.get().accept(status.get());
                var retry = retries.poll(2, TimeUnit.SECONDS);
                Assertions.assertNotNull(retry);
                Assertions.assertEquals(5_000, retry.getDelay());
                retry.getJob().wakeUp();
                Assertions.assertTrue(completed.await(2, TimeUnit.SECONDS));
                Assertions.assertEquals(DBDamNameService.suggestDamName(), session.getCachedDam().damName());
                Assertions.assertEquals(DDConnectionMonitor.EventType.NAME_CHANGED, channel.getMonitor().getEvents().getLast().type());
                Mockito.verify(channel, Mockito.never()).start();
                Mockito.verify(account).renameDam(CLIENT_ID, DBDamNameService.suggestDamName());
            } finally {
                naming.deactivateService();
                Job.getJobManager().removeJobChangeListener(jobs);
            }
        }
    }

    @Test
    void persistsTheGeneratedNameAndReturnsTheServerResult() throws Exception {
        var saved = dam(DEFAULT_NAME);
        var monitor = new DDConnectionMonitor();
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(dam(null));
        Mockito.when(account.canRenameDam()).thenReturn(true);
        Mockito.when(account.renameDam(CLIENT_ID, DEFAULT_NAME)).thenReturn(saved);

        Assertions.assertSame(saved, DBDamNameService.getOrInitializeName(account, CLIENT_ID, () -> DEFAULT_NAME, monitor));
        monitor.damNameLoaded(saved.damName());
        Assertions.assertEquals(1, monitor.getEvents().size());
        Assertions.assertEquals(DDConnectionMonitor.EventType.NAME_CHANGED, monitor.getEvents().getFirst().type());
        Assertions.assertEquals(DEFAULT_NAME, monitor.getEvents().getFirst().details());

        var order = Mockito.inOrder(account);
        order.verify(account).getDam(CLIENT_ID);
        order.verify(account).canRenameDam();
        order.verify(account).renameDam(CLIENT_ID, DEFAULT_NAME);
    }

    @Test
    void preservesAnExistingServerNameWithoutGeneratingAnotherName() throws Exception {
        var existing = dam("My database workstation");
        var monitor = new DDConnectionMonitor();
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(existing);

        Assertions.assertSame(existing, DBDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("An already named Dam must not generate a name"), monitor));
        monitor.damNameLoaded(existing.damName());
        Assertions.assertTrue(monitor.getEvents().isEmpty());

        Mockito.verify(account).getDam(CLIENT_ID);
        Mockito.verifyNoMoreInteractions(account);
    }

    @Test
    void leavesAnUnregisteredDamUntouched() throws Exception {
        Assertions.assertNull(DBDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("A name must not be generated before server registration")));
        Mockito.verify(account).getDam(CLIENT_ID);
        Mockito.verifyNoMoreInteractions(account);
    }

    @Test
    void initializesNameWhenRegistrationBecomesVisibleOnRetry() throws Exception {
        var saved = dam(DEFAULT_NAME);
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(null, dam(null), saved);
        Mockito.when(account.canRenameDam()).thenReturn(true);
        Mockito.when(account.renameDam(CLIENT_ID, DEFAULT_NAME)).thenReturn(saved);

        Assertions.assertNull(DBDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("A name must not be generated before server registration")));
        Assertions.assertSame(saved, DBDamNameService.getOrInitializeName(account, CLIENT_ID, () -> DEFAULT_NAME));
        Assertions.assertSame(saved, DBDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("The saved name must be preserved")));

        Mockito.verify(account).renameDam(CLIENT_ID, DEFAULT_NAME);
    }

    @Test
    void respectsRenamePermissionWithoutShowingAnUnsavedSuggestion() throws Exception {
        var unnamed = dam(null);
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(unnamed);

        Assertions.assertSame(unnamed, DBDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("A name must not be generated without rename permission")));

        Mockito.verify(account).getDam(CLIENT_ID);
        Mockito.verify(account).canRenameDam();
        Mockito.verifyNoMoreInteractions(account);
    }

    @Test
    void reconnectDoesNotRenameAnAlreadyInitializedDam() throws Exception {
        var saved = dam(DEFAULT_NAME);
        var monitor = new DDConnectionMonitor();
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(dam(null), saved);
        Mockito.when(account.canRenameDam()).thenReturn(true);
        Mockito.when(account.renameDam(CLIENT_ID, DEFAULT_NAME)).thenReturn(saved);

        DBDamNameService.getOrInitializeName(account, CLIENT_ID, () -> DEFAULT_NAME, monitor);
        Assertions.assertSame(saved, DBDamNameService.getOrInitializeName(account, CLIENT_ID,
            () -> Assertions.fail("Reconnect must preserve the saved name"), monitor));
        monitor.damNameLoaded(saved.damName());
        Assertions.assertEquals(1, monitor.getEvents().size());

        Mockito.verify(account).renameDam(CLIENT_ID, DEFAULT_NAME);
    }

    @Test
    void retriesAnUnpersistedNameAfterARequestFailure() throws Exception {
        var saved = dam(DEFAULT_NAME);
        var monitor = new DDConnectionMonitor();
        Mockito.when(account.getDam(CLIENT_ID)).thenReturn(dam(null));
        Mockito.when(account.canRenameDam()).thenReturn(true);
        Mockito.when(account.renameDam(CLIENT_ID, DEFAULT_NAME)).thenThrow(new DBException("Unavailable")).thenReturn(saved);

        Assertions.assertThrows(DBException.class,
            () -> DBDamNameService.getOrInitializeName(account, CLIENT_ID, () -> DEFAULT_NAME, monitor));
        Assertions.assertTrue(monitor.getEvents().isEmpty());
        Assertions.assertSame(saved, DBDamNameService.getOrInitializeName(account, CLIENT_ID, () -> DEFAULT_NAME, monitor));
        Assertions.assertEquals(1, monitor.getEvents().size());
    }

    @NotNull
    private static DDConnectedRemoteDam dam(@Nullable String name) {
        var now = OffsetDateTime.now();
        return new DDConnectedRemoteDam("server-dam", "deployment", "instance", "workspace", null, true, 1,
            List.of(new DDConnectedRemoteDam.Application("DBeaver Test", "1.0")), now, now, null, CLIENT_ID, List.of(), name);
    }
}
