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

import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.transport.DDRemoteDamDesktopChannel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Timeout(15)
class DDRemoteDamDesktopSessionServiceTest {
    private final DDDesktopAccessService access = Mockito.mock(DDDesktopAccessService.class);
    private final DDRemoteDamDesktopChannel channel = Mockito.mock(DDRemoteDamDesktopChannel.class);
    private final DDDesktopRpcRegistrationRegistry registrations = Mockito.mock(DDDesktopRpcRegistrationRegistry.class);
    private final DDRemoteDamDesktopSessionService service = new DDRemoteDamDesktopSessionService(
        access, channel, registrations);
    private final AtomicReference<DDRemoteDamChannelStatus> status = new AtomicReference<>(
        new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null));

    @BeforeEach
    void configureChannel() {
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

    @Test
    void waitsForRegistrationsBeforeReturningTheEndpoint() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
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
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
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
    void retriesInitialRegistrationFailureWhileTheChannelStaysReady() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
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
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
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
            DDRemoteDamDesktopSessionService.formatClientDamId("workspace", "HOST123"));
    }

    @Test
    void clientDamIdentityChangesWithWorkspaceOrLocalHostId() {
        String clientDamId = DDRemoteDamDesktopSessionService.formatClientDamId("workspace", "HOST123");

        Assertions.assertNotEquals(clientDamId, DDRemoteDamDesktopSessionService.formatClientDamId("other-workspace", "HOST123"));
        Assertions.assertNotEquals(clientDamId, DDRemoteDamDesktopSessionService.formatClientDamId("workspace", "HOST456"));
    }

    @Test
    void clientDamIdentityUsesUtf8AndUnpaddedUrlSafeBase64() {
        Assertions.assertEquals("5bel5L2c5Yy68J-ZgjpIT1NUMTIz",
            DDRemoteDamDesktopSessionService.formatClientDamId("工作区🙂", "HOST123"));
    }

    @Test
    void startsTheChannelWithoutRequiringImportedKeys() {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);

        service.start();
        service.start();

        Mockito.verify(channel).start();
        Mockito.verify(access, Mockito.never()).getKeyBundle();
        Assertions.assertSame(channel, service.getChannel());
    }

    @ParameterizedTest
    @EnumSource(value = DDDesktopAccessService.Status.class, names = {"SIGNED_OUT", "INVALID_TOKEN"})
    void doesNotStartLoginOrTransportWhenAuthorizationIsUnavailable(@NotNull DDDesktopAccessService.Status status) {
        Mockito.when(access.getStatus()).thenReturn(status);

        service.start();

        Mockito.verify(channel).pause();
        Mockito.verify(channel, Mockito.never()).start();
        ready("unauthorized");
        Assertions.assertThrows(DBException.class, service::getRpc);
    }

    @Test
    void pausesBeforeAnIdentityChangeAndResumesAfterASuccessfulLogin() {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
        service.start();
        DDDesktopAccessListener listener = listener();

        listener.accessChanging();
        Mockito.verify(channel).pause();
        listener.accessChanged();

        Mockito.verify(channel, Mockito.times(2)).start();
    }

    @Test
    void pausesOnLogoutAndCanResumeAfterANewLogin() {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
        service.start();
        DDDesktopAccessListener listener = listener();

        listener.accessChanging();
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_OUT);
        listener.accessChanged();
        Mockito.verify(channel, Mockito.times(2)).pause();
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
        listener.accessChanged();

        Mockito.verify(channel, Mockito.times(2)).start();
    }

    @Test
    void shutdownDetachesTheListenerAndCannotBeReversedByLateAccessEvents() {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
        service.start();
        DDDesktopAccessListener listener = listener();

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
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
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
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
        service.start();
        ready("first");
        MessagingRpcEndpoint endpoint = service.getRpc();
        Mockito.doAnswer(invocation -> {
            Assertions.assertTrue(endpoint.isCloseComplete());
            Assertions.assertThrows(DBException.class, service::getRpc);
            setStatus(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null));
            return null;
        }).when(channel).pause();

        DDDesktopAccessListener listener = listener();
        listener.accessChanging();
        listener.accessChanged();
        Assertions.assertThrows(DBException.class, service::getRpc);
        ready("second");

        Assertions.assertNotSame(endpoint, service.getRpc());
    }

    @Test
    void shutdownClosesRpcBeforeThePhysicalChannel() throws Exception {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
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
    @EnumSource(value = DDDesktopAccessService.Status.class, names = {"SIGNED_IN", "SIGNED_OUT"})
    void identityChangeDuringStartupCannotRestoreAnOutdatedAuthorizationSnapshot(
        @NotNull DDDesktopAccessService.Status initialStatus
    ) throws Exception {
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger reads = new AtomicInteger();
        DDDesktopAccessService.Status nextStatus = initialStatus == DDDesktopAccessService.Status.SIGNED_IN
            ? DDDesktopAccessService.Status.SIGNED_OUT : DDDesktopAccessService.Status.SIGNED_IN;
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
                DDDesktopAccessListener listener = listener();
                listener.accessChanging();
                listener.accessChanged();
            } finally {
                release.countDown();
            }
            starting.get(3, TimeUnit.SECONDS);
        }
        if (nextStatus == DDDesktopAccessService.Status.SIGNED_IN) {
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
    private DDDesktopAccessListener listener() {
        ArgumentCaptor<DDDesktopAccessListener> listener = ArgumentCaptor.forClass(DDDesktopAccessListener.class);
        Mockito.verify(access).addAccessListener(listener.capture());
        return listener.getValue();
    }
}
