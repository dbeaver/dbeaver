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
package org.jkiss.dbeaver.model.datadam.auth.desktop;

import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.jkiss.dbeaver.model.datadam.transport.DDRemoteDamDesktopChannel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Timeout(60)
class DDDesktopAccessLifecycleTest {

    @Test
    void completesTheAccessChangeEvenWhenEncryptionKeysCannotBeLoaded() throws Exception {
        DDDesktopAccessService access = access();
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        try (var keys = Mockito.mockStatic(DDKeyStore.class)) {
            keys.when(DDKeyStore::load).thenThrow(new DBException("Keys unavailable"));

            Assertions.assertThrows(DBException.class, () -> access.signIn(Mockito.mock(DBRProgressMonitor.class)));
        }

        Assertions.assertEquals(List.of("changing", "changed"), events);
    }

    @Test
    void completesTheLogoutChangeEvenWhenEncryptionKeysCannotBeCleared() throws Exception {
        DDDesktopAccessService access = access();
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        try (var keys = Mockito.mockStatic(DDKeyStore.class)) {
            keys.when(DDKeyStore::clear).thenThrow(new DBException("Keys unavailable"));

            Assertions.assertThrows(DBException.class, access::signOut);
        }

        Assertions.assertEquals(List.of("changing", "changed"), events);
    }

    @Test
    void isolatesAccessListenersAndAllowsThemToBeRemoved() throws Exception {
        DDDesktopAccessService access = access();
        DDDesktopAccessListener failing = Mockito.mock(DDDesktopAccessListener.class);
        Mockito.doThrow(new IllegalStateException("Listener failed")).when(failing).accessChanging();
        Mockito.doThrow(new IllegalStateException("Listener failed")).when(failing).accessChanged();
        access.addAccessListener(failing);
        List<String> events = new ArrayList<>();
        DDDesktopAccessListener recording = listener(events);
        access.addAccessListener(recording);
        try (var keys = Mockito.mockStatic(DDKeyStore.class)) {
            access.signOut();
            access.removeAccessListener(recording);
            access.signOut();
        }

        Assertions.assertEquals(List.of("changing", "changed"), events);
    }

    @Test
    void expiryClosesRpcAndPausesTheChannelWithoutAnotherAccessOperation() throws Exception {
        var status = new AtomicReference<>(DDDesktopTokenManager.Status.SIGNED_IN);
        DDDesktopAccessService access = access(status);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        DDRemoteDamDesktopChannel channel = Mockito.mock(DDRemoteDamDesktopChannel.class);
        Mockito.when(channel.getStatus()).thenReturn(new DDRemoteDamChannelStatus(
            DDRemoteDamChannelState.CONNECTED, new DDRemoteDamSessionInfo("deployment", "instance", "session")));
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var session = new DDRemoteDamDesktopSessionService(access, channel);
             var jobs = new ExpiryJobs()) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            session.start();
            final var endpoint = session.getRpc();
            Job expiry = jobs.latest();

            status.set(DDDesktopTokenManager.Status.EXPIRED);
            wake(expiry);

            Assertions.assertEquals(List.of("changing", "changed", "changed"), events);
            Assertions.assertEquals(1, jobs.count());
            Assertions.assertTrue(endpoint.isCloseComplete());
            Assertions.assertThrows(DBException.class, session::getRpc);
            Mockito.verify(channel).pause();
            Mockito.verify(channel).start();
            keys.verify(DDKeyStore::clear, Mockito.never());
        }
    }

    @Test
    void replacedExpiryCannotInvalidateTheNewSession() throws Exception {
        var status = new AtomicReference<>(DDDesktopTokenManager.Status.SIGNED_IN);
        DDDesktopAccessService access = access(status);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var jobs = new ExpiryJobs()) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            Job previous = jobs.latest();
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            Job current = jobs.latest();
            Assertions.assertNotSame(previous, current);
            Assertions.assertEquals(Job.NONE, previous.getState());

            status.set(DDDesktopTokenManager.Status.EXPIRED);
            runCancelled(previous);

            Assertions.assertNotEquals(Job.NONE, current.getState());
            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed"), events);
            wake(current);
            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed", "changed"), events);
            Assertions.assertEquals(3, jobs.count());
        }
    }

    @Test
    void logoutCancelsExpiryAndIgnoresItsLateExecution() throws Exception {
        var status = new AtomicReference<>(DDDesktopTokenManager.Status.SIGNED_IN);
        DDDesktopAccessService access = access(status);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var jobs = new ExpiryJobs()) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            Job expiry = jobs.latest();

            status.set(DDDesktopTokenManager.Status.SIGNED_OUT);
            access.signOut();
            Assertions.assertEquals(Job.NONE, expiry.getState());
            runCancelled(expiry);

            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed"), events);
            Assertions.assertEquals(2, jobs.count());
        }
    }

    @Test
    void earlyExpiryExecutionRechecksTheCurrentTokenDeadline() throws Exception {
        var status = new AtomicReference<>(DDDesktopTokenManager.Status.SIGNED_IN);
        DDDesktopAccessService access = access(status);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var jobs = new ExpiryJobs()) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            Job expiry = jobs.latest();

            wake(expiry);

            Assertions.assertNotSame(expiry, jobs.latest());
            Assertions.assertEquals(2, jobs.count());
            Assertions.assertEquals(List.of("changing", "changed"), events);
        }
    }

    @NotNull
    private static DDDesktopAccessService access() {
        return access(new AtomicReference<>(DDDesktopTokenManager.Status.SIGNED_OUT));
    }

    @NotNull
    private static DDDesktopAccessService access(@NotNull AtomicReference<DDDesktopTokenManager.Status> status) {
        DDDesktopTokenManager manager = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(manager.getStatus()).thenAnswer(invocation -> status.get());
        Mockito.when(manager.getExpiresAt()).thenAnswer(invocation -> Instant.now().plusSeconds(3600));
        return new DDDesktopAccessService(manager);
    }

    private static void wake(@NotNull Job job) throws InterruptedException {
        job.wakeUp();
        job.join();
    }

    private static void runCancelled(@NotNull Job job) throws InterruptedException {
        job.schedule(1);
        job.join();
    }

    private static final class ExpiryJobs extends JobChangeAdapter implements AutoCloseable {
        private final List<Job> scheduled = new CopyOnWriteArrayList<>();

        private ExpiryJobs() {
            Job.getJobManager().addJobChangeListener(this);
        }

        @Override
        public void scheduled(@NotNull IJobChangeEvent event) {
            if ("Expire DataDam desktop access".equals(event.getJob().getName())) {
                scheduled.add(event.getJob());
            }
        }

        @NotNull
        private Job latest() {
            Assertions.assertFalse(scheduled.isEmpty());
            return scheduled.get(scheduled.size() - 1);
        }

        private int count() {
            return scheduled.size();
        }

        @Override
        public void close() {
            Job.getJobManager().removeJobChangeListener(this);
            for (Job job : scheduled) {
                job.cancel();
            }
        }
    }

    @NotNull
    private static DDDesktopAccessListener listener(@NotNull List<String> events) {
        return new DDDesktopAccessListener() {
            @Override
            public void accessChanging() {
                events.add("changing");
            }

            @Override
            public void accessChanged() {
                events.add("changed");
            }
        };
    }
}
