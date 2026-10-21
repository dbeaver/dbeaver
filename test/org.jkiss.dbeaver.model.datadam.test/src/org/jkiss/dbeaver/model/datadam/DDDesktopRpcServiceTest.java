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

import com.dbeaver.datadam.external.api.model.DDConnectionInfo;
import com.dbeaver.datadam.external.api.model.DDMessage;
import com.dbeaver.datadam.external.api.rpc.DDGsonRpcCodec;
import com.dbeaver.datadam.external.api.rpc.DDRpcServices;
import com.dbeaver.datadam.external.api.rpc.DDRpcTransport;
import com.dbeaver.datadam.external.api.transport.DDChannelState;
import com.dbeaver.datadam.external.api.transport.DDChannelStatus;
import com.dbeaver.datadam.external.api.transport.DDConnectionListener;
import com.dbeaver.datadam.external.api.transport.DDMessageChannel;
import com.dbeaver.datadam.external.api.transport.DDMessageListener;
import com.dbeaver.datadam.external.api.transport.DDSubscription;
import com.dbeaver.messaging.rpc.RpcEndpoint;
import com.dbeaver.messaging.rpc.RpcException;
import com.dbeaver.messaging.rpc.RpcMessage;
import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.transport.DDDesktopMessageChannel;
import org.jkiss.dbeaver.osgi.test.runner.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Timeout(15)
class DDDesktopRpcServiceTest extends DBeaverUnitTest {
    private static final String BINDING = "echo";

    @Test
    void readyCreatesIndependentAccountAndStorageLanesThatWorkInBothDirections() throws Exception {
        MemoryChannel desktop = new MemoryChannel();
        MemoryChannel backend = new MemoryChannel();
        desktop.forward = backend::receive;
        backend.forward = desktop::receive;
        backend.ready("backend");
        try (var rpc = service(desktop)) {
            Assertions.assertTrue(desktop.subscriptions.isEmpty());
            Assertions.assertThrows(DBException.class, () -> rpc.getRpc(DDRpcServices.ACCOUNT));

            desktop.ready("desktop");

            Assertions.assertEquals(4, desktop.subscriptions.size());
            Assertions.assertEquals(3, desktop.observers.size());
            RpcEndpoint account = rpc.getRpc(DDRpcServices.ACCOUNT);
            RpcEndpoint storage = rpc.getRpc(DDRpcServices.STORAGE);
            Assertions.assertNotSame(account, storage);
            Assertions.assertSame(account, rpc.getRpc(DDRpcServices.ACCOUNT));
            try (var remoteAccount = remote(backend, DDRpcServices.ACCOUNT);
                 var remoteStorage = remote(backend, DDRpcServices.STORAGE)) {
                account.register(BINDING, Echo.class, value -> "desktop-account:" + value);
                storage.register(BINDING, Echo.class, value -> "desktop-storage:" + value);
                remoteAccount.register(BINDING, Echo.class, value -> "account:" + value);
                remoteStorage.register(BINDING, Echo.class, value -> "storage:" + value);

                Assertions.assertEquals("account:a", call(() -> account.proxy(BINDING, Echo.class).echo("a")));
                Assertions.assertEquals("storage:s", call(() -> storage.proxy(BINDING, Echo.class).echo("s")));
                Assertions.assertEquals("desktop-account:a", call(() -> remoteAccount.proxy(BINDING, Echo.class).echo("a")));
                Assertions.assertEquals("desktop-storage:s", call(() -> remoteStorage.proxy(BINDING, Echo.class).echo("s")));
            }
        }
        Assertions.assertTrue(desktop.subscriptions.isEmpty());
        Assertions.assertTrue(desktop.observers.isEmpty());
        Assertions.assertFalse(desktop.closed);
    }

    @Test
    void reconnectRetainsEndpointsAndRegistrationsWhileFailingPendingCalls() throws Exception {
        MemoryChannel desktop = new MemoryChannel();
        MemoryChannel backend = new MemoryChannel();
        desktop.forward = backend::receive;
        backend.forward = desktop::receive;
        desktop.ready("first");
        backend.ready("backend-first");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var rpc = service(desktop);
             var remote = remote(backend, DDRpcServices.ACCOUNT);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            RpcEndpoint account = rpc.getRpc(DDRpcServices.ACCOUNT);
            account.register(BINDING, Echo.class, value -> "retained:" + value);
            remote.register(BINDING, Echo.class, value -> {
                entered.countDown();
                await(release);
                return "late:" + value;
            });
            Future<String> pending = executor.submit(() -> account.proxy(BINDING, Echo.class).echo("pending"));
            try {
                await(entered);
                desktop.disconnected();
                Assertions.assertEquals(RpcException.Reason.DISCONNECTED, failure(pending).reason());
                Assertions.assertSame(account, rpc.getRpc(DDRpcServices.ACCOUNT));
                Assertions.assertFalse(account.isClosed());
                Assertions.assertThrows(DBException.class, () -> rpc.getRpc("unopened"));
                Assertions.assertThrows(RpcException.class, () -> account.proxy(BINDING, Echo.class).echo("offline"));

                backend.disconnected();
                desktop.ready("second");
                backend.ready("backend-second");

                Assertions.assertSame(account, rpc.getRpc(DDRpcServices.ACCOUNT));
                Assertions.assertEquals(4, desktop.subscriptions.size());
                Assertions.assertEquals("retained:new", call(() -> remote.proxy(BINDING, Echo.class).echo("new")));
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void newIdentityCannotUseOldHandlersOrProxiesAndOldRegistrationCannotRemoveItsReplacement() throws Exception {
        MemoryChannel desktop = new MemoryChannel();
        MemoryChannel backend = new MemoryChannel();
        desktop.forward = backend::receive;
        backend.forward = desktop::receive;
        desktop.ready("first");
        backend.ready("backend");
        try (var rpc = service(desktop);
             var remote = remote(backend, DDRpcServices.ACCOUNT)) {
            RpcEndpoint old = rpc.getRpc(DDRpcServices.ACCOUNT);
            final var registration = old.register(BINDING, Echo.class, value -> "old:" + value);
            Echo oldProxy = old.proxy(BINDING, Echo.class);
            rpc.suspend();
            Assertions.assertTrue(old.isClosed());
            Assertions.assertEquals(RpcException.Reason.CLOSED,
                Assertions.assertThrows(RpcException.class, () -> oldProxy.echo("old")).reason());
            Assertions.assertThrows(DBException.class, () -> rpc.getRpc(DDRpcServices.ACCOUNT));
            desktop.disconnected();
            rpc.resume();
            desktop.ready("second");
            RpcEndpoint current = rpc.getRpc(DDRpcServices.ACCOUNT);
            Assertions.assertNotSame(old, current);
            RpcException missing = Assertions.assertThrows(RpcException.class,
                () -> call(() -> remote.proxy(BINDING, Echo.class).echo("missing")));
            Assertions.assertEquals(RpcMessage.Error.UNKNOWN_SERVICE, missing.remoteError());
            current.register(BINDING, Echo.class, value -> "new:" + value);
            registration.close();

            Assertions.assertEquals("new:value", call(() -> remote.proxy(BINDING, Echo.class).echo("value")));
        }
    }

    @Test
    void callerClosedEndpointsAreReplacedOnlyWhenReady() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        try (var rpc = service(channel)) {
            RpcEndpoint old = rpc.getRpc(DDRpcServices.ACCOUNT);
            old.close();
            RpcEndpoint replacement = rpc.getRpc(DDRpcServices.ACCOUNT);
            Assertions.assertNotSame(old, replacement);
            Assertions.assertFalse(replacement.isClosed());
            channel.disconnected();
            replacement.close();
            Assertions.assertThrows(DBException.class, () -> rpc.getRpc(DDRpcServices.ACCOUNT));
            channel.ready("second");
            Assertions.assertNotSame(replacement, rpc.getRpc(DDRpcServices.ACCOUNT));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void closingLaneStaysReservedThroughReadyAndIdentityChangesUntilSubscriptionCleanupCompletes(boolean identityChange)
            throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        final String topic = DDRpcServices.topic(DDRpcServices.ACCOUNT);
        channel.onUnsubscribe = closingTopic -> {
            if (topic.equals(closingTopic)) {
                entered.countDown();
                await(release);
            }
        };
        try (var rpc = service(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            RpcEndpoint endpoint = rpc.getRpc(DDRpcServices.ACCOUNT);
            var closing = executor.submit(endpoint::close);
            try {
                await(entered);
                Assertions.assertTrue(endpoint.isClosed());
                Assertions.assertFalse(endpoint.isCloseComplete());
                if (identityChange) {
                    rpc.suspend();
                    Assertions.assertThrows(DBException.class, () -> rpc.getRpc(DDRpcServices.ACCOUNT));
                    rpc.resume();
                }
                channel.ready("second");
                Assertions.assertThrows(DBException.class, () -> rpc.getRpc(DDRpcServices.ACCOUNT));
                Assertions.assertEquals(1L, channel.subscribedTopics.stream().filter(topic::equals).count());
            } finally {
                release.countDown();
            }
            closing.get(3, TimeUnit.SECONDS);
            Assertions.assertTrue(endpoint.isCloseComplete());
            RpcEndpoint replacement = rpc.getRpc(DDRpcServices.ACCOUNT);
            Assertions.assertNotSame(endpoint, replacement);
            Assertions.assertFalse(replacement.isClosed());
            Assertions.assertEquals(2L, channel.subscribedTopics.stream().filter(topic::equals).count());
        }
    }

    @Test
    void failedOpenRollsBackSubscriptionsAndCacheCapacity() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        try (var rpc = service(channel)) {
            channel.failTopic = DDRpcServices.topic("custom");
            for (int attempt = 0; attempt < 40; attempt++) {
                Assertions.assertThrows(DBException.class, () -> rpc.getRpc("custom"));
                Assertions.assertEquals(4, channel.subscriptions.size());
                Assertions.assertEquals(3, channel.observers.size());
            }
            channel.failTopic = null;
            Assertions.assertFalse(rpc.getRpc("custom").isClosed());
            Assertions.assertEquals(6, channel.subscriptions.size());
        }
    }

    @Test
    void customLaneCacheIsBoundedAndReclaimsCallerClosedEndpoints() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        try (var rpc = service(channel)) {
            final RpcEndpoint first = rpc.getRpc("custom-0");
            for (int index = 1; index < 32; index++) {
                rpc.getRpc("custom-" + index);
            }
            Assertions.assertThrows(DBException.class, () -> rpc.getRpc("overflow"));
            Assertions.assertFalse(rpc.getRpc(DDRpcServices.ACCOUNT).isClosed());
            Assertions.assertFalse(rpc.getRpc(DDRpcServices.STORAGE).isClosed());
            first.close();
            Assertions.assertFalse(rpc.getRpc("replacement").isClosed());
            Assertions.assertThrows(DBException.class, () -> rpc.getRpc("bad/id"));
            Assertions.assertEquals(68, channel.subscriptions.size());
        }
    }

    @Test
    void concurrentAccessPublishesOneEndpointPerLane() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        try (var rpc = service(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CyclicBarrier barrier = new CyclicBarrier(8);
            List<Future<RpcEndpoint>> pending = new ArrayList<>();
            for (int index = 0; index < 8; index++) {
                pending.add(executor.submit(() -> {
                    barrier.await(3, TimeUnit.SECONDS);
                    return rpc.getRpc("concurrent");
                }));
            }
            RpcEndpoint endpoint = pending.getFirst().get(3, TimeUnit.SECONDS);
            for (Future<RpcEndpoint> result : pending) {
                Assertions.assertSame(endpoint, result.get(3, TimeUnit.SECONDS));
            }
            Assertions.assertEquals(6, channel.subscriptions.size());
            Assertions.assertEquals(4, channel.observers.size());
        }
    }

    @Test
    void identityChangeDuringOpenReservesTheLaneUntilRollbackCompletes() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        blockCustomOpen(channel, entered, release);
        try (var rpc = service(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<RpcEndpoint> opening = executor.submit(() -> rpc.getRpc("custom"));
            try {
                await(entered);
                rpc.suspend();
                channel.disconnected();
                rpc.resume();
                channel.ready("second");
                Assertions.assertThrows(DBException.class, () -> rpc.getRpc("custom"));
                Assertions.assertEquals(1L,
                    channel.subscribedTopics.stream().filter(DDRpcServices.topic("custom")::equals).count());
                release.countDown();

                ExecutionException failure = Assertions.assertThrows(ExecutionException.class,
                    () -> opening.get(3, TimeUnit.SECONDS));
                Assertions.assertInstanceOf(DBException.class, failure.getCause());
                RpcEndpoint current = rpc.getRpc("custom");
                Assertions.assertSame(current, rpc.getRpc("custom"));
                Assertions.assertEquals(6, channel.subscriptions.size());
                Assertions.assertEquals(4, channel.observers.size());
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void shutdownDuringOpenDisposesUnpublishedEndpointAndSubscriptions() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        blockCustomOpen(channel, entered, release);
        try (var rpc = service(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<RpcEndpoint> opening = executor.submit(() -> rpc.getRpc("custom"));
            try {
                await(entered);
                rpc.close();
                release.countDown();
                ExecutionException failure = Assertions.assertThrows(ExecutionException.class,
                    () -> opening.get(3, TimeUnit.SECONDS));
                Assertions.assertInstanceOf(DBException.class, failure.getCause());
                Assertions.assertTrue(channel.subscriptions.isEmpty());
                Assertions.assertTrue(channel.observers.isEmpty());
                Assertions.assertThrows(DBException.class, () -> rpc.getRpc(DDRpcServices.ACCOUNT));
                channel.ready("late");
                Assertions.assertTrue(channel.subscriptions.isEmpty());
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void shutdownDuringObserverInstallationClosesTheLateSubscription() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        channel.onObserve = () -> {
            entered.countDown();
            await(release);
        };
        try (var rpc = new Desktop(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> starting = executor.submit(rpc::start);
            try {
                await(entered);
                rpc.close();
            } finally {
                release.countDown();
            }
            starting.get(3, TimeUnit.SECONDS);
            Assertions.assertTrue(channel.observers.isEmpty());
        }
    }

    @NotNull
    private static Desktop service(@NotNull MemoryChannel channel) {
        Desktop rpc = new Desktop(channel);
        rpc.start();
        return rpc;
    }

    @NotNull
    private static RpcEndpoint remote(@NotNull MemoryChannel channel, @NotNull String serviceId) throws Exception {
        return RpcEndpoint.open(DDRpcTransport.forChannel(channel, DDRpcServices.topic(serviceId)), new DDGsonRpcCodec(new Gson()));
    }

    @NotNull
    private static <T> T call(@NotNull Callable<T> callable) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                return executor.submit(callable).get(3, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Exception failure) {
                    throw failure;
                }
                throw e;
            }
        }
    }

    @NotNull
    private static RpcException failure(@NotNull Future<?> pending) {
        ExecutionException failure = Assertions.assertThrows(ExecutionException.class, () -> pending.get(3, TimeUnit.SECONDS));
        return Assertions.assertInstanceOf(RpcException.class, failure.getCause());
    }

    private static void await(@NotNull CountDownLatch latch) {
        try {
            Assertions.assertTrue(latch.await(3, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void blockCustomOpen(
        @NotNull MemoryChannel channel,
        @NotNull CountDownLatch entered,
        @NotNull CountDownLatch release
    ) {
        AtomicBoolean first = new AtomicBoolean(true);
        channel.onSubscribe = topic -> {
            if (DDRpcServices.topic("custom").equals(topic) && first.compareAndSet(true, false)) {
                entered.countDown();
                await(release);
            }
        };
    }

    /** Exchanges a single value over an RPC lane. */
    public interface Echo {
        @NotNull
        String echo(@NotNull String value);
    }

    private static final class Desktop implements AutoCloseable {
        private final DDDesktopAccessService access = Mockito.mock(DDDesktopAccessService.class);
        private final DDDesktopConnectionService connection;

        private Desktop(@NotNull MemoryChannel messages) {
            DDDesktopMessageChannel channel = Mockito.mock(DDDesktopMessageChannel.class);
            Mockito.when(channel.getStatus()).thenAnswer(invocation -> messages.getStatus());
            Mockito.when(channel.observeState(Mockito.any())).thenAnswer(invocation ->
                messages.observeState(invocation.getArgument(0)));
            Mockito.when(channel.subscribe(Mockito.anyString(), Mockito.any())).thenAnswer(invocation ->
                messages.subscribe(invocation.getArgument(0), invocation.getArgument(1)));
            Mockito.when(channel.send(Mockito.any())).thenAnswer(invocation -> messages.send(invocation.getArgument(0)));
            Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
            connection = new DDDesktopConnectionService(access, channel, new DDGsonRpcCodec(new Gson()));
        }

        private void start() {
            connection.start();
        }

        private void suspend() {
            listener().accessChanging();
        }

        private void resume() {
            listener().accessChanged();
        }

        @NotNull
        private RpcEndpoint getRpc(@NotNull String serviceId) throws DBException {
            return connection.getRpc(serviceId);
        }

        @NotNull
        private DDDesktopAccessListener listener() {
            ArgumentCaptor<DDDesktopAccessListener> listener = ArgumentCaptor.forClass(DDDesktopAccessListener.class);
            Mockito.verify(access).addAccessListener(listener.capture());
            return listener.getValue();
        }

        @Override
        public void close() {
            connection.close();
        }
    }

    private static final class MemoryChannel implements DDMessageChannel {
        private final List<TopicSubscription> subscriptions = new CopyOnWriteArrayList<>();
        private final List<String> subscribedTopics = new CopyOnWriteArrayList<>();
        private final List<Consumer<DDChannelStatus>> observers = new CopyOnWriteArrayList<>();
        private volatile DDChannelStatus status = new DDChannelStatus(DDChannelState.DISCONNECTED, null);
        @Nullable
        private Consumer<DDMessage> forward;
        @Nullable
        private Consumer<String> onSubscribe;
        @Nullable
        private Consumer<String> onUnsubscribe;
        @Nullable
        private Runnable onObserve;
        @Nullable
        private String failTopic;
        private boolean closed;

        @Override
        @NotNull
        public DDChannelStatus getStatus() {
            return status;
        }

        @Override
        @NotNull
        public DDSubscription observeState(@NotNull Consumer<DDChannelStatus> observer) {
            observers.add(observer);
            observer.accept(status);
            if (onObserve != null) {
                onObserve.run();
            }
            return () -> observers.remove(observer);
        }

        @Override
        @NotNull
        public CompletionStage<Void> send(@NotNull DDMessage message) {
            if (!status.isReady()) {
                return CompletableFuture.failedFuture(new DBException("Channel disconnected"));
            }
            if (forward != null) {
                forward.accept(message);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        @NotNull
        public DDSubscription subscribe(@NotNull String topic, @NotNull DDMessageListener listener) {
            if (topic.equals(failTopic)) {
                throw new IllegalStateException("Subscription unavailable");
            }
            TopicSubscription subscription = new TopicSubscription(topic, listener);
            subscribedTopics.add(topic);
            subscriptions.add(subscription);
            if (onSubscribe != null) {
                onSubscribe.accept(topic);
            }
            return () -> {
                if (onUnsubscribe != null) {
                    onUnsubscribe.accept(topic);
                }
                subscriptions.remove(subscription);
            };
        }

        @Override
        @NotNull
        public DDSubscription addConnectionListener(@NotNull DDConnectionListener listener) {
            throw new UnsupportedOperationException();
        }

        private void ready(@NotNull String connectionId) {
            setStatus(new DDChannelStatus(DDChannelState.CONNECTED, new DDConnectionInfo("deployment", "run", connectionId)));
        }

        private void disconnected() {
            setStatus(new DDChannelStatus(DDChannelState.DISCONNECTED, null));
        }

        private void setStatus(@NotNull DDChannelStatus next) {
            status = next;
            List.copyOf(observers).forEach(observer -> observer.accept(next));
        }

        private void receive(@NotNull DDMessage message) {
            DDChannelStatus current = status;
            if (current.isReady()) {
                for (TopicSubscription subscription : subscriptions) {
                    if (subscription.topic().equals(message.topic())) {
                        subscription.listener().onMessage(Objects.requireNonNull(current.connection()), message);
                    }
                }
            }
        }

        @Override
        public void close() {
            closed = true;
            setStatus(new DDChannelStatus(DDChannelState.CLOSED, null));
        }
    }

    private record TopicSubscription(@NotNull String topic, @NotNull DDMessageListener listener) {
    }
}
