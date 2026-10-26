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
package org.jkiss.dbeaver.model.datadam;

import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.transport.DDRemoteDamDesktopChannel;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Timeout(15)
class DDRemoteDamDesktopSessionServiceTest extends DBeaverUnitTest {
    private final DDDesktopAccessService access = Mockito.mock(DDDesktopAccessService.class);
    private final DDRemoteDamDesktopChannel channel = Mockito.mock(DDRemoteDamDesktopChannel.class);
    private final DDRemoteDamDesktopSessionService service = new DDRemoteDamDesktopSessionService(access, channel);
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
    void startsTheChannelWithoutRequiringImportedKeys() {
        Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);

        service.start();
        service.start();

        Mockito.verify(channel).start();
        Mockito.verify(access, Mockito.never()).getKeyBundle();
        Assertions.assertSame(channel, service.getChannel());
    }

    @ParameterizedTest
    @EnumSource(value = DDDesktopAccessService.Status.class, names = {"SIGNED_OUT", "EXPIRED", "UNLOADED"})
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
        Mockito.verify(channel, Mockito.never()).observeState(Mockito.any());
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
        Mockito.verify(channel, Mockito.never()).observeState(Mockito.any());
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
