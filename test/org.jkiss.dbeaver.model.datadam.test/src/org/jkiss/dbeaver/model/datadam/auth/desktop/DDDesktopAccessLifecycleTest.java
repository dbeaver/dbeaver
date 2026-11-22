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
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.auth.DDCryptoState;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Timeout(60)
class DDDesktopAccessLifecycleTest {

    @Test
    void failedRecoveryCleanupPreservesKeysForBothResetAndReplacement() throws Exception {
        var accountId = UUID.randomUUID();
        var current = new DDKeyBundle(accountId, "signing", "data", 1, UUID.randomUUID());
        var replacement = new DDKeyBundle(accountId, "new-signing", "new-data", 1, UUID.randomUUID());
        var tokens = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(tokens.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var accounts = Mockito.mockConstruction(DDAccountClient.class, (account, context) ->
                 Mockito.when(account.getAccountId()).thenReturn(accountId))) {
            keys.when(DDKeyStore::load).thenReturn(current);
            var access = new DDDesktopAccessService(tokens);
            access.load();
            var failure = new DBException("Cannot delete a project");

            for (DDKeyBundle candidate : new DDKeyBundle[] { null, replacement }) {
                Assertions.assertSame(failure, Assertions.assertThrows(DBException.class,
                    () -> access.recoverProjectKeys(current, candidate, () -> { throw failure; })));
                Assertions.assertSame(current, access.getKeyBundle());
            }
            keys.verify(DDKeyStore::clear, Mockito.never());
            keys.verify(() -> DDKeyStore.save(Mockito.any()), Mockito.never());
        }
    }

    @Test
    void staleRecoveryCannotDeleteProjectsOrForgetNewerKeys() throws Exception {
        var accountId = UUID.randomUUID();
        var current = new DDKeyBundle(accountId, "signing", "data", 1, UUID.randomUUID());
        var replacement = new DDKeyBundle(accountId, "new-signing", "new-data", 1, UUID.randomUUID());
        var tokens = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(tokens.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var accounts = Mockito.mockConstruction(DDAccountClient.class, (account, context) ->
                 Mockito.when(account.getAccountId()).thenReturn(accountId))) {
            keys.when(DDKeyStore::load).thenReturn(current);
            var access = new DDDesktopAccessService(tokens);
            access.load();
            access.completeKeyImport(current, replacement);
            var cleanupCalled = new AtomicBoolean();

            Assertions.assertThrows(DBException.class,
                () -> access.recoverProjectKeys(current, null, () -> cleanupCalled.set(true)));
            Assertions.assertFalse(cleanupCalled.get());
            Assertions.assertSame(replacement, access.getKeyBundle());
            keys.verify(DDKeyStore::clear, Mockito.never());
        }
    }

    @Test
    void recoverySerializesOtherAccessOperationsUntilCleanupAndCommitFinish() throws Exception {
        var accountId = UUID.randomUUID();
        var current = new DDKeyBundle(accountId, "signing", "data", 1, UUID.randomUUID());
        var replacement = new DDKeyBundle(accountId, "new-signing", "new-data", 1, UUID.randomUUID());
        var competing = new DDKeyBundle(UUID.randomUUID(), "other-signing", "other-data", 1, UUID.randomUUID());
        var tokens = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(tokens.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var accounts = Mockito.mockConstruction(DDAccountClient.class, (account, context) ->
                 Mockito.when(account.getAccountId()).thenReturn(accountId));
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            keys.when(DDKeyStore::load).thenReturn(current);
            var access = new DDDesktopAccessService(tokens);
            access.load();
            var started = new CountDownLatch(1);
            var transition = new AtomicReference<Future<?>>();

            access.recoverProjectKeys(current, replacement, () -> {
                transition.set(executor.submit(() -> {
                    started.countDown();
                    access.completeKeyImport(current, competing);
                    return null;
                }));
                try {
                    Assertions.assertTrue(started.await(1, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DBException("Interrupted while checking recovery serialization", e);
                }
                Assertions.assertThrows(TimeoutException.class, () -> transition.get().get(100, TimeUnit.MILLISECONDS));
                Assertions.assertSame(current, access.getKeyBundle());
            });

            Assertions.assertSame(replacement, access.getKeyBundle());
            var rejected = Assertions.assertThrows(ExecutionException.class, () -> transition.get().get(1, TimeUnit.SECONDS));
            Assertions.assertInstanceOf(DBException.class, rejected.getCause());
            keys.verify(() -> DDKeyStore.save(replacement));
        }
    }

    @Test
    void preparedReplacementDoesNotChangeSavedKeysUntilCommitted() throws Exception {
        var accountId = UUID.randomUUID();
        var current = new DDKeyBundle(accountId, "old-signing", "old-data", 1, UUID.randomUUID());
        var replacement = new DDKeyBundle(accountId, "new-signing", "new-data", 1, UUID.randomUUID());
        var state = new DDCryptoState(accountId, true, "bundle", 1L, "salt", 1, replacement.apiKeyId());
        var tokens = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(tokens.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var accounts = Mockito.mockConstruction(DDAccountClient.class, (account, context) -> {
                 Mockito.when(account.getAccountId()).thenReturn(accountId);
                 Mockito.when(account.getCryptoState()).thenReturn(state);
             })) {
            keys.when(DDKeyStore::load).thenReturn(current);
            keys.when(() -> DDKeyStore.unpack(state, "replacement-phrase")).thenReturn(replacement);
            var access = new DDDesktopAccessService(tokens);
            access.load();

            Assertions.assertSame(replacement, access.prepareKeyImport("replacement-phrase"));
            Assertions.assertSame(current, access.getKeyBundle());
            keys.verify(() -> DDKeyStore.save(Mockito.any()), Mockito.never());
            keys.verify(DDKeyStore::clear, Mockito.never());

            access.completeKeyImport(current, replacement);
            Assertions.assertSame(replacement, access.getKeyBundle());
            keys.verify(() -> DDKeyStore.save(replacement));

            Assertions.assertThrows(DBException.class, () -> access.completeKeyImport(current, replacement));
            keys.verify(() -> DDKeyStore.save(replacement));
        }
    }

    @Test
    void preparedReplacementCannotOverwriteKeysAfterAnExplicitReset() throws Exception {
        var accountId = UUID.randomUUID();
        var current = new DDKeyBundle(accountId, "signing", "data", 1, UUID.randomUUID());
        var replacement = new DDKeyBundle(accountId, "replacement-signing", "replacement-data", 1, UUID.randomUUID());
        var tokens = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(tokens.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var accounts = Mockito.mockConstruction(DDAccountClient.class, (account, context) ->
                 Mockito.when(account.getAccountId()).thenReturn(accountId))) {
            keys.when(DDKeyStore::load).thenReturn(current);
            var access = new DDDesktopAccessService(tokens);
            access.load();
            access.forgetKeys();

            Assertions.assertThrows(DBException.class, () -> access.completeKeyImport(current, replacement));
            Assertions.assertNull(access.getKeyBundle());
            keys.verify(() -> DDKeyStore.save(Mockito.any()), Mockito.never());
        }
    }

    @Test
    void failedReplacementValidationPreservesTheSavedKeys() throws Exception {
        var accountId = UUID.randomUUID();
        var keyId = UUID.randomUUID();
        var bundle = new DDKeyBundle(accountId, "signing", "data", 1, keyId);
        var state = new DDCryptoState(accountId, true, "bundle", 1L, "salt", 1, keyId);
        var tokens = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(tokens.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var accounts = Mockito.mockConstruction(DDAccountClient.class, (account, context) -> {
                 Mockito.when(account.getAccountId()).thenReturn(accountId);
                 Mockito.when(account.getCryptoState()).thenReturn(state);
             })) {
            keys.when(DDKeyStore::load).thenReturn(bundle);
            keys.when(() -> DDKeyStore.unpack(state, "invalid-phrase")).thenThrow(new DBException("Incorrect recovery phrase"));
            var access = new DDDesktopAccessService(tokens);
            access.load();

            Assertions.assertThrows(DBException.class, () -> access.importKeys("invalid-phrase"));
            Assertions.assertSame(bundle, access.getKeyBundle());

            Mockito.when(accounts.constructed().getLast().getCryptoState()).thenThrow(new DBException("Unavailable"));
            Assertions.assertThrows(DBException.class, () -> access.importKeys("replacement-phrase"));
            Assertions.assertSame(bundle, access.getKeyBundle());
            keys.verify(DDKeyStore::clear, Mockito.never());
            keys.verify(() -> DDKeyStore.save(Mockito.any()), Mockito.never());
        }
    }

    @Test
    void detectsEncryptionResetWithoutDiscardingKeysBeforeUserRecovery() throws Exception {
        var accountId = UUID.randomUUID();
        var keyId = UUID.randomUUID();
        var bundle = new DDKeyBundle(accountId, "signing", "data", 1, keyId);
        var state = new AtomicReference<>(new DDCryptoState(accountId, true, "bundle", 1L, "salt", 1, keyId));
        var tokens = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(tokens.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var accounts = Mockito.mockConstruction(DDAccountClient.class, (account, context) -> {
                 Mockito.when(account.getAccountId()).thenReturn(accountId);
                 Mockito.when(account.getCryptoState()).thenAnswer(invocation -> state.get());
             })) {
            keys.when(DDKeyStore::load).thenReturn(bundle);
            var access = new DDDesktopAccessService(tokens);
            access.load();
            Assertions.assertTrue(access.validateProjectKeys());

            state.set(new DDCryptoState(accountId, true, "new-bundle", 1L, "salt", 1, UUID.randomUUID()));
            Assertions.assertFalse(access.validateProjectKeys());
            Assertions.assertSame(bundle, access.getKeyBundle());
            keys.verify(DDKeyStore::clear, Mockito.never());

            state.set(new DDCryptoState(accountId, false, null, null, null, null, null));
            Assertions.assertFalse(access.validateProjectKeys());
            state.set(new DDCryptoState(UUID.randomUUID(), true, "bundle", 1L, "salt", 1, keyId));
            Assertions.assertFalse(access.validateProjectKeys());

            Mockito.when(accounts.constructed().getLast().getCryptoState()).thenThrow(new DBException("Offline"));
            Assertions.assertThrows(DBException.class, access::validateProjectKeys);
            Assertions.assertSame(bundle, access.getKeyBundle());
        }
    }

    @Test
    void missingProjectKeysDoNotTriggerAnAccountRequest() throws Exception {
        try (var accounts = Mockito.mockConstruction(DDAccountClient.class)) {
            Assertions.assertFalse(access().validateProjectKeys());
            accounts.constructed().forEach(Mockito::verifyNoInteractions);
        }
    }

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
    void transientRefreshRetainsKeysAndRetriesUntilRecovery() throws Exception {
        var bundle = new DDKeyBundle(UUID.randomUUID(), "signing", "data", 1, UUID.randomUUID());
        var expiresAt = new AtomicReference<>(Instant.now().minusSeconds(60));
        var manager = Mockito.mock(DDDesktopTokenManager.class);
        Mockito.when(manager.getStatus()).thenReturn(DDDesktopTokenManager.Status.SIGNED_IN);
        Mockito.when(manager.getExpiresAt()).thenAnswer(invocation -> expiresAt.get());
        Mockito.when(manager.getAccessTokenForResource(Mockito.any()))
            .thenThrow(new DBException("Offline"))
            .thenAnswer(invocation -> {
                expiresAt.set(Instant.now().plusSeconds(3600));
                return "rotated-access";
            });
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var accounts = Mockito.mockConstruction(DDAccountClient.class, (account, context) ->
                 Mockito.when(account.getAccountId()).thenReturn(bundle.accountId()));
             var jobs = new ExpiryJobs()) {
            keys.when(DDKeyStore::load).thenReturn(bundle);
            var access = new DDDesktopAccessService(manager);
            List<String> events = new CopyOnWriteArrayList<>();
            access.addAccessListener(listener(events));

            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            Assertions.assertSame(bundle, access.getKeyBundle());
            Assertions.assertEquals(DDDesktopAccessService.Status.SIGNED_IN, access.getStatus());
            Assertions.assertEquals(30_000L, jobs.latestDelay());

            wake(jobs.latest());
            Assertions.assertEquals(2, jobs.count());
            Assertions.assertEquals(30_000L, jobs.latestDelay());
            Assertions.assertSame(bundle, access.getKeyBundle());
            Assertions.assertEquals(List.of("changing", "changed"), events);

            wake(jobs.latest());
            Assertions.assertEquals(3, jobs.count());
            Assertions.assertTrue(jobs.latestDelay() > 30_000L);
            Assertions.assertSame(bundle, access.getKeyBundle());
            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed"), events);
            keys.verify(DDKeyStore::clear, Mockito.never());
        }
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
        private final List<Long> delays = new CopyOnWriteArrayList<>();

        private ExpiryJobs() {
            Job.getJobManager().addJobChangeListener(this);
        }

        @Override
        public void scheduled(@NotNull IJobChangeEvent event) {
            if ("Expire DataDam desktop access".equals(event.getJob().getName())) {
                delays.add(event.getDelay());
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

        private long latestDelay() {
            return delays.getLast();
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
