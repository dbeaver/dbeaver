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
package org.jkiss.dbeaver.model.datadam.auth;

import com.dbeaver.datadam.messaging.api.model.DDMessagingConnectionInfo;
import com.dbeaver.datadam.messaging.api.transport.DDMessagingChannelState;
import com.dbeaver.datadam.messaging.api.transport.DDMessagingChannelStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDDesktopConnectionService;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.transport.DDDesktopMessageChannel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Timeout(15)
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
        var status = new AtomicReference<>("SIGNED_IN");
        DDDesktopAccessService access = access(status);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        DDDesktopMessageChannel channel = Mockito.mock(DDDesktopMessageChannel.class);
        Mockito.when(channel.getStatus()).thenReturn(new DDMessagingChannelStatus(
            DDMessagingChannelState.CONNECTED, new DDMessagingConnectionInfo("deployment", "run", "connection")));
        try (var keys = Mockito.mockStatic(DDKeyStore.class);
             var connection = new DDDesktopConnectionService(access, channel)) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            connection.start();
            final var endpoint = connection.getRpc();
            Job expiry = Assertions.assertInstanceOf(Job.class, expiryJob(access));

            status.set("EXPIRED");
            run(expiry);

            Assertions.assertEquals(List.of("changing", "changed", "changed"), events);
            Assertions.assertNull(expiryJob(access));
            Assertions.assertTrue(endpoint.isCloseComplete());
            Assertions.assertThrows(DBException.class, connection::getRpc);
            Mockito.verify(channel).pause();
            Mockito.verify(channel).start();
            keys.verify(DDKeyStore::clear, Mockito.never());
        } finally {
            cancelExpiry(access);
        }
    }

    @Test
    void replacedExpiryCannotInvalidateTheNewSession() throws Exception {
        var status = new AtomicReference<>("SIGNED_IN");
        DDDesktopAccessService access = access(status);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        try (var keys = Mockito.mockStatic(DDKeyStore.class)) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            Job previous = Assertions.assertInstanceOf(Job.class, expiryJob(access));
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            Job current = Assertions.assertInstanceOf(Job.class, expiryJob(access));

            status.set("EXPIRED");
            run(previous);

            Assertions.assertSame(current, expiryJob(access));
            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed"), events);
            run(current);
            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed", "changed"), events);
            Assertions.assertNull(expiryJob(access));
        } finally {
            cancelExpiry(access);
        }
    }

    @Test
    void logoutCancelsExpiryAndIgnoresItsLateExecution() throws Exception {
        var status = new AtomicReference<>("SIGNED_IN");
        DDDesktopAccessService access = access(status);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        try (var keys = Mockito.mockStatic(DDKeyStore.class)) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            Job expiry = Assertions.assertInstanceOf(Job.class, expiryJob(access));

            status.set("SIGNED_OUT");
            access.signOut();
            run(expiry);

            Assertions.assertEquals(List.of("changing", "changed", "changing", "changed"), events);
            Assertions.assertNull(expiryJob(access));
        } finally {
            cancelExpiry(access);
        }
    }

    @Test
    void earlyExpiryExecutionRechecksTheCurrentTokenDeadline() throws Exception {
        var status = new AtomicReference<>("SIGNED_IN");
        DDDesktopAccessService access = access(status);
        List<String> events = new ArrayList<>();
        access.addAccessListener(listener(events));
        try (var keys = Mockito.mockStatic(DDKeyStore.class)) {
            access.signIn(Mockito.mock(DBRProgressMonitor.class));
            Job expiry = Assertions.assertInstanceOf(Job.class, expiryJob(access));

            run(expiry);

            Assertions.assertNotSame(expiry, expiryJob(access));
            Assertions.assertNotNull(expiryJob(access));
            Assertions.assertEquals(List.of("changing", "changed"), events);
        } finally {
            cancelExpiry(access);
        }
    }

    @NotNull
    private static DDDesktopAccessService access() throws Exception {
        return access(new AtomicReference<>("SIGNED_OUT"));
    }

    @NotNull
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static DDDesktopAccessService access(@NotNull AtomicReference<String> status) throws Exception {
        Class<?> managerType = Class.forName("org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopTokenManager");
        var statuses = new HashMap<String, Object>();
        for (Object value : managerType.getMethod("getStatus").getReturnType().getEnumConstants()) {
            statuses.put(value.toString(), value);
        }
        Object manager = Mockito.mock(managerType, invocation -> switch (invocation.getMethod().getName()) {
            case "getStatus" -> statuses.get(status.get());
            case "getExpiresAt" -> Instant.now().plusSeconds(3600);
            default -> Mockito.RETURNS_DEFAULTS.answer(invocation);
        });
        var constructor = DDDesktopAccessService.class.getDeclaredConstructor(managerType);
        constructor.setAccessible(true);
        return constructor.newInstance(manager);
    }

    @Nullable
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static Job expiryJob(@NotNull DDDesktopAccessService access) throws Exception {
        var field = DDDesktopAccessService.class.getDeclaredField("expiryJob");
        field.setAccessible(true);
        return (Job) field.get(access);
    }

    private static void run(@NotNull Job job) throws InterruptedException {
        job.cancel();
        job.schedule();
        job.join();
    }

    private static void cancelExpiry(@NotNull DDDesktopAccessService access) throws Exception {
        Job job = expiryJob(access);
        if (job != null) {
            job.cancel();
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
