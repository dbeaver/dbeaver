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
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDLoggedUserView;
import org.jkiss.dbeaver.model.datadam.transport.DBDamChannel;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Timeout(15)
class DBDamSessionServiceTest {
    private final DBDamAccessService access = Mockito.mock(DBDamAccessService.class);
    private final DBDamChannel channel = Mockito.mock(DBDamChannel.class);
    private final DBDamRpcRegistrationRegistry registrations = Mockito.mock(DBDamRpcRegistrationRegistry.class);
    private final DBDamSessionService service = new DBDamSessionService(
        access, channel, registrations);
    private final AtomicReference<DDRemoteDamChannelStatus> status = new AtomicReference<>(
        new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null));

    @BeforeEach
    void configureChannel() {
        Mockito.when(access.getLoggedUserIdentity()).thenReturn(new DDLoggedUserView(null, null, null, null));
        Mockito.when(channel.getMonitor()).thenReturn(new DDConnectionMonitor());
        Mockito.when(channel.getStatus()).thenAnswer(invocation -> status.get());
        Mockito.doAnswer(invocation -> {
            setStatus(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null));
            return null;
        }).when(channel).pause();
    }

    @AfterEach
    void close() {
        service.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"first", "second"})
    void reusesDamInformationOnlyForTheCurrentConnectedSession(@NotNull String recoveredSessionId) {
        ready("first");
        var first = status.get().session();
        var dam = namedDam("Desktop");
        service.cacheDam(first, dam);
        Assertions.assertSame(dam, service.getCachedDam());

        status.set(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null));
        Assertions.assertNull(service.getCachedDam());

        ready(recoveredSessionId);
        Assertions.assertNull(service.getCachedDam());
        var renamed = namedDam("Renamed desktop");
        service.cacheDam(status.get().session(), renamed);
        service.cacheDam(first, dam);
        Assertions.assertSame(renamed, service.getCachedDam(), "A late response must not overwrite the current session");
        Assertions.assertEquals("Renamed desktop", channel.getMonitor().getEvents().getLast().details());
    }

    @Test
    void cachesRenamedDamAndIgnoresUnavailableResults() {
        ready("session");
        var session = status.get().session();
        service.cacheDam(session, namedDam("Desktop"));
        var renamed = namedDam("Renamed desktop");
        service.cacheDam(session, renamed);
        service.cacheDam(session, renamed);
        service.cacheDam(session, null);
        Assertions.assertSame(renamed, service.getCachedDam());
        Assertions.assertEquals(List.of("Desktop", "Renamed desktop"),
            channel.getMonitor().getEvents().stream()
                .filter(event -> event.type() == DDConnectionMonitor.EventType.NAME_CHANGED)
                .map(DDConnectionMonitor.Event::details).toList());
        service.close();
        Assertions.assertNull(service.getCachedDam());
    }

    @NotNull
    private static DDConnectedRemoteDam namedDam(@NotNull String name) {
        var now = OffsetDateTime.now();
        return new DDConnectedRemoteDam("dam", "deployment", "instance", "workspace", null, true, 1,
            List.of(new DDConnectedRemoteDam.Application("DBeaver Test", "1.0")), now, now, null, "client", List.of(), name);
    }

    @Test
    void recordsLoginIdentityWithoutDuplicatingTokenRefreshAndDetectsUserChanges() {
        var firstId = UUID.randomUUID();
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        Mockito.when(access.getLoggedUserIdentity()).thenReturn(new DDLoggedUserView(firstId, "user@example.com", null, null));
        service.start();
        listener().accessChanged();
        listener().accessChanged();
        var events = channel.getMonitor().getEvents();
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(DDConnectionMonitor.EventType.SIGNED_IN, events.getFirst().type());
        Assertions.assertEquals("user@example.com", events.getFirst().details());

        var secondId = UUID.randomUUID();
        Mockito.when(access.getLoggedUserIdentity()).thenReturn(new DDLoggedUserView(secondId, null, null, null));
        listener().accessChanged();
        Assertions.assertEquals(2, channel.getMonitor().getEvents().size());
        Assertions.assertEquals("", channel.getMonitor().getEvents().getLast().details());
        Assertions.assertFalse(channel.getMonitor().getEvents().toString().contains(firstId.toString()));
        Assertions.assertFalse(channel.getMonitor().getEvents().toString().contains(secondId.toString()));
        Mockito.verify(access, Mockito.never()).getLoggedUserView();
    }

    @Test
    void disablingConnectionPausesRpcAndCannotBeUndoneByAnAccessChange() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        Mockito.verify(channel).start();
        service.setConnectionEnabled(false);
        listener().accessChanged();
        Mockito.verify(channel, Mockito.times(1)).start();
        Mockito.verify(channel, Mockito.atLeastOnce()).pause();
        service.setConnectionEnabled(true);
        Mockito.verify(channel, Mockito.times(2)).start();
    }

    @Test
    void disabledConnectionDoesNotStartAtStartup() {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.setConnectionEnabled(false);
        service.start();
        Mockito.verify(channel, Mockito.never()).start();
    }

    @Test
    void waitsForRegistrationsBeforeReturningTheEndpoint() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        ready("session");
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            entered.countDown();
            Assertions.assertTrue(release.await(3, TimeUnit.SECONDS));
            return null;
        }).when(registrations).registerEndpoint(Mockito.any(), Mockito.any());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var opening = executor.submit(service::getRpc);
            try {
                Assertions.assertTrue(entered.await(3, TimeUnit.SECONDS));
                Assertions.assertFalse(opening.isDone());
            } finally {
                release.countDown();
            }
            var endpoint = opening.get(3, TimeUnit.SECONDS);
            Assertions.assertSame(endpoint, service.getRpc());
            Mockito.verify(registrations).registerEndpoint(endpoint, DDRemoteDamSettings.EMPTY);
        }
    }

    @Test
    void failedRegistrationClosesTheEndpointAndRetriesOnTheNextCall() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        ready("session");
        var failed = new AtomicReference<MessagingRpcEndpoint>();
        Mockito.doAnswer(invocation -> {
            failed.set(invocation.getArgument(0));
            throw new DBException("Registration failed");
        }).doNothing().when(registrations).registerEndpoint(Mockito.any(), Mockito.any());

        Assertions.assertThrows(DBException.class, service::getRpc);
        Assertions.assertTrue(failed.get().isCloseComplete());
        Assertions.assertNotSame(failed.get(), service.getRpc());
        Mockito.verify(registrations, Mockito.times(2)).registerEndpoint(Mockito.any(), Mockito.any());
    }

    @Test
    void successfulRpcInitializationRetryClearsItsErrorWithoutReconnect() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        ready("session");
        var observer = new AtomicReference<Consumer<DDRemoteDamChannelStatus>>();
        Mockito.when(channel.observeState(Mockito.any())).thenAnswer(invocation -> {
            observer.set(invocation.getArgument(0));
            return (DDRemoteDamSubscription) () -> {};
        });
        Mockito.doThrow(new DBException("Registration failed")).doThrow(new DBException("Still unavailable")).doNothing()
            .when(registrations).registerEndpoint(Mockito.any(), Mockito.any());
        service.start();

        observer.get().accept(status.get());
        Assertions.assertNotNull(channel.getMonitor().snapshot().error());
        observer.get().accept(status.get());
        Assertions.assertEquals(1, channel.getMonitor().getEvents().stream()
            .filter(event -> event.type() == DDConnectionMonitor.EventType.ERROR).count());
        observer.get().accept(status.get());
        Assertions.assertNull(channel.getMonitor().snapshot().error());
        Assertions.assertTrue(channel.getMonitor().getEvents().stream()
            .anyMatch(event -> event.type() == DDConnectionMonitor.EventType.ERROR));
    }

    @Test
    void retriesInitialRegistrationFailureWhileTheChannelStaysReady() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        ready("session");
        Mockito.when(channel.observeState(Mockito.any())).thenAnswer(invocation -> {
            Consumer<DDRemoteDamChannelStatus> observer = invocation.getArgument(0);
            observer.accept(status.get());
            return (DDRemoteDamSubscription) () -> {};
        });
        var registered = new CountDownLatch(1);
        Mockito.doThrow(new DBException("Registration failed")).doAnswer(invocation -> {
            registered.countDown();
            return null;
        }).when(registrations).registerEndpoint(Mockito.any(), Mockito.any());

        service.start();

        Assertions.assertTrue(registered.await(3, TimeUnit.SECONDS));
        Assertions.assertFalse(service.getRpc().isClosed());
        Mockito.verify(registrations, Mockito.times(2)).registerEndpoint(Mockito.any(), Mockito.any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidatingDuringRegistrationDoesNotBlockAndRejectsTheLateEndpoint(boolean close) throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        ready("session");
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var opened = new AtomicReference<MessagingRpcEndpoint>();
        Mockito.doAnswer(invocation -> {
            opened.set(invocation.getArgument(0));
            entered.countDown();
            Assertions.assertTrue(release.await(3, TimeUnit.SECONDS));
            return null;
        }).when(registrations).registerEndpoint(Mockito.any(), Mockito.any());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var opening = executor.submit(service::getRpc);
            try {
                Assertions.assertTrue(entered.await(3, TimeUnit.SECONDS));
                if (close) {
                    service.close();
                } else {
                    listener().accessChanging();
                }
                Assertions.assertTrue(opened.get().isClosed());
            } finally {
                release.countDown();
            }
            var error = Assertions.assertThrows(ExecutionException.class, () -> opening.get(3, TimeUnit.SECONDS));
            Assertions.assertInstanceOf(DBException.class, error.getCause());
        }
    }

    @Test
    void formatsClientDamIdentity() {
        Assertions.assertEquals("d29ya3NwYWNlOkhPU1QxMjM",
            DBDamSessionService.formatClientDamId("workspace", "HOST123"));
    }

    @Test
    void clientDamIdentityChangesWithWorkspaceOrLocalHostId() {
        String clientDamId = DBDamSessionService.formatClientDamId("workspace", "HOST123");

        Assertions.assertNotEquals(clientDamId, DBDamSessionService.formatClientDamId("other-workspace", "HOST123"));
        Assertions.assertNotEquals(clientDamId, DBDamSessionService.formatClientDamId("workspace", "HOST456"));
    }

    @Test
    void clientDamIdentityUsesUtf8AndUnpaddedUrlSafeBase64() {
        Assertions.assertEquals("5bel5L2c5Yy68J-ZgjpIT1NUMTIz",
            DBDamSessionService.formatClientDamId("工作区🙂", "HOST123"));
    }

    @Test
    void startsTheChannelWithoutRequiringImportedKeys() {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);

        service.start();
        service.start();

        Mockito.verify(channel).start();
        Mockito.verify(access, Mockito.never()).getKeyBundle();
        Assertions.assertSame(channel, service.getChannel());
    }

    @ParameterizedTest
    @EnumSource(value = DBDamAccessService.Status.class, names = {"SIGNED_OUT", "INVALID_TOKEN"})
    void doesNotStartLoginOrTransportWhenAuthorizationIsUnavailable(@NotNull DBDamAccessService.Status status) {
        Mockito.when(access.getStatus()).thenReturn(status);

        service.start();

        Mockito.verify(channel).pause();
        Mockito.verify(channel, Mockito.never()).start();
        ready("unauthorized");
        Assertions.assertThrows(DBException.class, service::getRpc);
    }

    @Test
    void pausesBeforeAnIdentityChangeAndResumesAfterASuccessfulLogin() {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        DBDamAccessListener listener = listener();

        listener.accessChanging();
        Mockito.verify(channel).pause();
        listener.accessChanged();

        Mockito.verify(channel, Mockito.times(2)).start();
    }

    @Test
    void pausesOnLogoutAndCanResumeAfterANewLogin() {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        DBDamAccessListener listener = listener();

        listener.accessChanging();
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_OUT);
        listener.accessChanged();
        Mockito.verify(channel, Mockito.times(2)).pause();
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        listener.accessChanged();

        Mockito.verify(channel, Mockito.times(2)).start();
    }

    @Test
    void shutdownDetachesTheListenerAndCannotBeReversedByLateAccessEvents() {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        DBDamAccessListener listener = listener();

        service.close();
        service.close();
        listener.accessChanged();
        service.start();

        Mockito.verify(channel).close();
        Mockito.verify(channel).start();
        Mockito.verify(access).removeAccessListener(listener);
        Mockito.verify(channel).observeState(Mockito.any());
    }

    @Test
    void opensOneLazyRpcEndpointOnlyAfterReadyWithoutStartingLogin() throws Exception {
        Assertions.assertThrows(DBException.class, service::getRpc);
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        Assertions.assertThrows(DBException.class, service::getRpc);

        ready("first");

        MessagingRpcEndpoint endpoint = service.getRpc();
        Assertions.assertSame(endpoint, service.getRpc());
        Mockito.verify(channel, Mockito.never()).subscribe(Mockito.anyString(), Mockito.any());
        Mockito.verify(channel).observeState(Mockito.any());
        Mockito.verify(access, Mockito.never()).getAccessTokenForResource(Mockito.any());
    }

    @Test
    void identityChangeClosesRpcBeforePausingAndRequiresFreshEndpoints() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        ready("first");
        MessagingRpcEndpoint endpoint = service.getRpc();
        Mockito.doAnswer(invocation -> {
            Assertions.assertTrue(endpoint.isCloseComplete());
            Assertions.assertThrows(DBException.class, service::getRpc);
            setStatus(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null));
            return null;
        }).when(channel).pause();

        DBDamAccessListener listener = listener();
        listener.accessChanging();
        listener.accessChanged();
        Assertions.assertThrows(DBException.class, service::getRpc);
        ready("second");

        Assertions.assertNotSame(endpoint, service.getRpc());
    }

    @Test
    void shutdownClosesRpcBeforeThePhysicalChannel() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DBDamAccessService.Status.SIGNED_IN);
        service.start();
        ready("first");
        MessagingRpcEndpoint endpoint = service.getRpc();
        Mockito.doAnswer(invocation -> {
            Assertions.assertTrue(endpoint.isCloseComplete());
            return null;
        }).when(channel).close();

        service.close();

        Assertions.assertThrows(DBException.class, service::getRpc);
    }

    @ParameterizedTest
    @EnumSource(value = DBDamAccessService.Status.class, names = {"SIGNED_IN", "SIGNED_OUT"})
    void identityChangeDuringStartupCannotRestoreAnOutdatedAuthorizationSnapshot(
        @NotNull DBDamAccessService.Status initialStatus
    ) throws Exception {
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger reads = new AtomicInteger();
        DBDamAccessService.Status nextStatus = initialStatus == DBDamAccessService.Status.SIGNED_IN
            ? DBDamAccessService.Status.SIGNED_OUT : DBDamAccessService.Status.SIGNED_IN;
        Mockito.when(access.getStatus()).thenAnswer(invocation -> {
            if (reads.incrementAndGet() == 1) {
                reading.countDown();
                Assertions.assertTrue(release.await(3, TimeUnit.SECONDS));
                return initialStatus;
            }
            return nextStatus;
        });
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var starting = executor.submit(service::start);
            try {
                Assertions.assertTrue(reading.await(3, TimeUnit.SECONDS));
                DBDamAccessListener listener = listener();
                listener.accessChanging();
                listener.accessChanged();
            } finally {
                release.countDown();
            }
            starting.get(3, TimeUnit.SECONDS);
        }
        if (nextStatus == DBDamAccessService.Status.SIGNED_IN) {
            Mockito.verify(channel).start();
            ready("new-identity");
            Assertions.assertFalse(service.getRpc().isClosed());
        } else {
            Mockito.verify(channel, Mockito.never()).start();
            Assertions.assertThrows(DBException.class, service::getRpc);
        }
    }

    private void ready(@NotNull String damSessionId) {
        setStatus(new DDRemoteDamChannelStatus(
            DDRemoteDamChannelState.CONNECTED, new DDRemoteDamSessionInfo("deployment", "instance", damSessionId)));
    }

    private void setStatus(@NotNull DDRemoteDamChannelStatus next) {
        status.set(next);
    }

    @NotNull
    private DBDamAccessListener listener() {
        ArgumentCaptor<DBDamAccessListener> listener = ArgumentCaptor.forClass(DBDamAccessListener.class);
        Mockito.verify(access).addAccessListener(listener.capture());
        return listener.getValue();
    }
}
