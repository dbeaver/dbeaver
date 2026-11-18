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
import com.google.gson.JsonObject;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.jkiss.dbeaver.model.datadam.rpc.DDDesktopRpcRegistrationRegistry;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.jkiss.dbeaver.model.datadam.transport.DDRemoteDamDesktopChannel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Timeout(60)
class DDDesktopAccessLifecycleTest {

    @Test
    void readsLoginIdentityLocallyWithoutAccountRequestsOrTokenRefresh() {
        var manager = Mockito.mock(DDDesktopTokenManager.class);
        var userId = UUID.randomUUID();
        JsonObject claims = new JsonObject();
        claims.addProperty("sub", userId.toString());
        claims.addProperty("email", "user@example.com");
        Mockito.when(manager.getUnverifiedTokenClaims()).thenReturn(claims);
        try (var accounts = Mockito.mockConstruction(DDAccountClient.class)) {
            var identity = new DDDesktopAccessService(manager).getLoggedUserIdentity();
            Assertions.assertEquals(userId, identity.userId());
            Assertions.assertEquals("user@example.com", identity.userName());
            Assertions.assertNull(identity.accountName());
            for (var account : accounts.constructed()) {
                Mockito.verifyNoInteractions(account);
            }
        }
        Mockito.verify(manager).getUnverifiedTokenClaims();
        Mockito.verifyNoMoreInteractions(manager);
    }

    @Test
    void completesTheAccessChangeEvenWhenEncryptionKeysCannotBeLoaded() throws Exception {
        DDDesktopAccessService access = access(new AtomicReference<>(DDDesktopTokenManager.Status.SIGNED_IN));
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        try (var jobs = new ExpiryJobs();
             var keys = Mockito.mockStatic(DDKeyStore.class)) {
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
    void failedRefreshClosesRpcAndPausesTheChannelWithoutAnotherAccessOperation() throws Exception {
        var status = new AtomicReference<>(DDDesktopTokenManager.Status.SIGNED_IN);
        DDDesktopAccessService access = access(status);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        DDRemoteDamDesktopChannel channel = Mockito.mock(DDRemoteDamDesktopChannel.class);
        Mockito.when(channel.getMonitor()).thenReturn(new DDConnectionMonitor());
        Mockito.when(channel.getStatus()).thenReturn(new DDRemoteDamChannelStatus(
            DDRemoteDamChannelState.CONNECTED, new DDRemoteDamSessionInfo("deployment", "instance", "session")));
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var session = new DDRemoteDamDesktopSessionService(
                 access, channel, Mockito.mock(DDDesktopRpcRegistrationRegistry.class));
             var jobs = new ExpiryJobs()) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            session.start();
            final var endpoint = session.getRpc();
            Job expiry = jobs.latest();

            status.set(DDDesktopTokenManager.Status.INVALID_TOKEN);
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
    void rotatedTokenReconnectsTheChannelAndSchedulesItsNewExpiry() throws Exception {
        var status = new AtomicReference<>(DDDesktopTokenManager.Status.SIGNED_IN);
        var expiresAt = new AtomicReference<>(Instant.now().plusSeconds(3600));
        DDDesktopAccessService access = access(status, expiresAt);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        DDRemoteDamDesktopChannel channel = Mockito.mock(DDRemoteDamDesktopChannel.class);
        Mockito.when(channel.getMonitor()).thenReturn(new DDConnectionMonitor());
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var session = new DDRemoteDamDesktopSessionService(access, channel, Mockito.mock(DDDesktopRpcRegistrationRegistry.class));
             var jobs = new ExpiryJobs()) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            session.start();
            Job expiry = jobs.latest();

            expiresAt.set(expiresAt.get().plusSeconds(3600));
            wake(expiry);

            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed"), events);
            Assertions.assertEquals(2, jobs.count());
            Mockito.verify(channel).pause();
            Mockito.verify(channel, Mockito.times(2)).start();
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

            runCancelled(previous);

            Assertions.assertNotEquals(Job.NONE, current.getState());
            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed"), events);
            wake(current);
            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed"), events);
            Assertions.assertEquals(4, jobs.count());
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

    @Test
    void retriesIncompleteKeyInitializationWithoutRequiringAnotherLogin() throws Exception {
        DDDesktopTokenManager manager = Mockito.mock(DDDesktopTokenManager.class);
        Instant expires = Instant.now().plusSeconds(3600);
        Mockito.when(manager.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        Mockito.when(manager.getExpiresAt()).thenReturn(expires);
        Mockito.when(manager.getRefreshAt()).thenReturn(expires);
        DDDesktopAccessService access = new DDDesktopAccessService(manager);
        AtomicReference<ICoreRunnable> recovery = new AtomicReference<>();
        Job job = Mockito.mock(Job.class);
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var jobs = Mockito.mockStatic(Job.class)) {
            keys.when(DDKeyStore::load).thenThrow(new DBException("Key storage unavailable")).thenReturn(null);
            jobs.when(() -> Job.createSystem(Mockito.anyString(), Mockito.any(ICoreRunnable.class))).thenAnswer(invocation -> {
                recovery.set(invocation.getArgument(1));
                return job;
            });

            Assertions.assertThrows(DBException.class, access::load);
            Assertions.assertEquals(DDDesktopAccessService.Status.SIGNED_IN, access.getStatus());
            Mockito.verify(job).schedule(Mockito.longThat(delay -> delay >= 500 && delay <= 1000));

            recovery.get().run(new NullProgressMonitor());
            keys.verify(DDKeyStore::load, Mockito.times(2));
            Mockito.verify(manager, Mockito.times(1)).load(Mockito.any());
            Mockito.verify(job).schedule(Mockito.longThat(delay -> delay > 3_000_000));
            Mockito.verify(manager, Mockito.never()).signIn(Mockito.any(), Mockito.any(), Mockito.any());
        }
    }

    @Test
    void failedLocalCredentialLoadIsRetriedAutomatically() throws Exception {
        DDDesktopTokenManager manager = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(manager.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_OUT);
        Mockito.doThrow(new DBException("Secret storage unavailable")).doNothing().when(manager).load(Mockito.any());
        DDDesktopAccessService access = new DDDesktopAccessService(manager);
        AtomicReference<ICoreRunnable> recovery = new AtomicReference<>();
        try (var jobs = Mockito.mockStatic(Job.class)) {
            jobs.when(() -> Job.createSystem(Mockito.anyString(), Mockito.any(ICoreRunnable.class))).thenAnswer(invocation -> {
                recovery.set(invocation.getArgument(1));
                return Mockito.mock(Job.class);
            });

            Assertions.assertThrows(DBException.class, access::load);
            Assertions.assertEquals(DDDesktopAccessService.Status.UNLOADED, access.getStatus());
            recovery.get().run(new NullProgressMonitor());
            Assertions.assertEquals(DDDesktopAccessService.Status.SIGNED_OUT, access.getStatus());
            Mockito.verify(manager, Mockito.times(2)).load(Mockito.any());
        }
    }

    @Test
    void retriesAccountValidationBeforeActivatingSavedEncryptionKeys() throws Exception {
        DDDesktopTokenManager manager = Mockito.mock(DDDesktopTokenManager.class);
        Instant expires = Instant.now().plusSeconds(3600);
        Mockito.when(manager.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        Mockito.when(manager.getExpiresAt()).thenReturn(expires);
        Mockito.when(manager.getRefreshAt()).thenReturn(expires);
        DDKeyBundle bundle = new DDKeyBundle(UUID.randomUUID(), "signing-key", "data-key", 1);
        AtomicReference<ICoreRunnable> recovery = new AtomicReference<>();
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var clients = Mockito.mockConstruction(DDAccountClient.class, (client, context) -> {
                 Mockito.when(client.getAccountId()).thenThrow(new DBException("Account offline")).thenReturn(bundle.accountId());
             });
             var jobs = Mockito.mockStatic(Job.class)) {
            keys.when(DDKeyStore::load).thenReturn(bundle);
            jobs.when(() -> Job.createSystem(Mockito.anyString(), Mockito.any(ICoreRunnable.class))).thenAnswer(invocation -> {
                recovery.set(invocation.getArgument(1));
                return Mockito.mock(Job.class);
            });
            DDDesktopAccessService access = new DDDesktopAccessService(manager);

            Assertions.assertThrows(DBException.class, access::load);
            Assertions.assertNull(access.getKeyBundle());
            recovery.get().run(new NullProgressMonitor());
            Assertions.assertEquals(bundle, access.getKeyBundle());
            Assertions.assertEquals(DDDesktopAccessService.Status.SIGNED_IN, access.getStatus());
            keys.verify(DDKeyStore::clear, Mockito.never());
            Mockito.verify(manager, Mockito.never()).signIn(Mockito.any(), Mockito.any(), Mockito.any());
        }
    }

    @Test
    void scheduledRefreshKeepsValidatedKeysAvailableToConcurrentReaders() throws Exception {
        DDDesktopTokenManager manager = Mockito.mock(DDDesktopTokenManager.class);
        Instant expires = Instant.now().plusSeconds(3600);
        Mockito.when(manager.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        Mockito.when(manager.getExpiresAt()).thenReturn(expires);
        Mockito.when(manager.getRefreshAt()).thenReturn(expires);
        DDKeyBundle bundle = new DDKeyBundle(UUID.randomUUID(), "signing-key", "data-key", 1);
        AtomicReference<ICoreRunnable> renewal = new AtomicReference<>();
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var clients = Mockito.mockConstruction(DDAccountClient.class, (client, context) -> {
                 Mockito.when(client.getAccountId()).thenReturn(bundle.accountId());
             });
             var jobs = Mockito.mockStatic(Job.class);
             var reader = Executors.newSingleThreadExecutor()) {
            keys.when(DDKeyStore::load).thenReturn(bundle);
            jobs.when(() -> Job.createSystem(Mockito.anyString(), Mockito.any(ICoreRunnable.class))).thenAnswer(invocation -> {
                renewal.set(invocation.getArgument(1));
                return Mockito.mock(Job.class);
            });
            DDDesktopAccessService access = new DDDesktopAccessService(manager);
            access.load();
            Mockito.when(manager.getAccessTokenForResource(Mockito.any())).thenAnswer(invocation -> {
                Assertions.assertEquals(bundle, reader.submit(access::getKeyBundle).get(1, TimeUnit.SECONDS));
                throw new DBException("SSO temporarily unavailable");
            });

            renewal.get().run(new NullProgressMonitor());
            Assertions.assertEquals(bundle, access.getKeyBundle());
        }
    }

    @NotNull
    private static DDDesktopAccessService access() throws DBException {
        return access(new AtomicReference<>(DDDesktopTokenManager.Status.SIGNED_OUT));
    }

    @NotNull
    private static DDDesktopAccessService access(@NotNull AtomicReference<DDDesktopTokenManager.Status> status) throws DBException {
        return access(status, new AtomicReference<>(Instant.now().plusSeconds(3600)));
    }

    @NotNull
    private static DDDesktopAccessService access(
        @NotNull AtomicReference<DDDesktopTokenManager.Status> status,
        @NotNull AtomicReference<Instant> expiresAt
    ) throws DBException {
        DDDesktopTokenManager manager = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(manager.getStatus()).thenAnswer(invocation -> status.get());
        Mockito.when(manager.getExpiresAt()).thenAnswer(invocation -> expiresAt.get());
        Mockito.when(manager.getRefreshAt()).thenAnswer(invocation -> expiresAt.get());
        DDDesktopAccessService access = new DDDesktopAccessService(manager);
        DDDesktopTokenManager.Status initial = status.get();
        status.set(DDDesktopTokenManager.Status.SIGNED_OUT);
        try {
            access.load();
        } finally {
            status.set(initial);
        }
        return access;
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
