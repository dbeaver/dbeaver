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

import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamMessage;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.rpc.DDRemoteDamGsonRpcCodec;
import com.dbeaver.datadam.dam.remote.api.rpc.DDRemoteDamRpcTransport;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannel;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamMessageListener;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSessionListener;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import com.dbeaver.messaging.rpc.MessagingRpcCodec;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import com.dbeaver.messaging.rpc.MessagingRpcException;
import com.dbeaver.messaging.rpc.MessagingRpcMessage;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.transport.DDRemoteDamDesktopChannel;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Timeout(15)
class DDRemoteDamDesktopRpcServiceTest extends DBeaverUnitTest {
    private static final String TOPIC = "desktop/echo";
    private static final String OTHER_TOPIC = "desktop/storage";

    @Test
    void oneLazyEndpointUsesExactDeveloperTopicsInBothDirections() throws Exception {
        MemoryChannel desktop = new MemoryChannel();
        MemoryChannel backend = new MemoryChannel();
        desktop.forward = backend::receive;
        backend.forward = desktop::receive;
        backend.ready("backend");
        try (var rpc = service(desktop)) {
            Assertions.assertTrue(desktop.subscriptions.isEmpty());
            Assertions.assertTrue(desktop.observers.isEmpty());
            Assertions.assertThrows(DBException.class, rpc::getRpc);

            desktop.ready("desktop");

            MessagingRpcEndpoint endpoint = rpc.getRpc();
            Assertions.assertSame(endpoint, rpc.getRpc());
            Assertions.assertTrue(desktop.subscriptions.isEmpty());
            Assertions.assertTrue(desktop.observers.isEmpty());
            try (var remote = remote(backend)) {
                Assertions.assertTrue(backend.subscriptions.isEmpty());
                endpoint.register(TOPIC, Echo.class, value -> "desktop-echo:" + value, codec());
                endpoint.register(OTHER_TOPIC, Echo.class, value -> "desktop-storage:" + value, codec());
                remote.register(TOPIC, Echo.class, value -> "echo:" + value, codec());
                remote.register(OTHER_TOPIC, Echo.class, value -> "storage:" + value, codec());

                Assertions.assertEquals("echo:a", call(() -> endpoint.proxy(TOPIC, Echo.class, codec()).echo("a")));
                Assertions.assertEquals("storage:s", call(() -> endpoint.proxy(OTHER_TOPIC, Echo.class, codec()).echo("s")));
                Assertions.assertEquals("desktop-echo:a", call(() -> remote.proxy(TOPIC, Echo.class, codec()).echo("a")));
                Assertions.assertEquals("desktop-storage:s", call(() -> remote.proxy(OTHER_TOPIC, Echo.class, codec()).echo("s")));
                Assertions.assertEquals(TOPIC, desktop.sentMessages.getFirst().topic());
                Assertions.assertEquals(4, desktop.subscriptions.size());
                Assertions.assertEquals(2, desktop.observers.size());
                Assertions.assertEquals(1L, desktop.subscribedTopics.stream().filter(TOPIC::equals).count());
                Assertions.assertEquals(1L, desktop.subscribedTopics.stream().filter(OTHER_TOPIC::equals).count());
            }
        }
        Assertions.assertTrue(desktop.subscriptions.isEmpty());
        Assertions.assertTrue(desktop.observers.isEmpty());
        Assertions.assertTrue(desktop.closed);
    }

    @Test
    void defaultCodecOverloadsExchangeOrdinaryRecordsInBothDirections() throws Exception {
        MemoryChannel desktop = new MemoryChannel();
        MemoryChannel backend = new MemoryChannel();
        desktop.forward = backend::receive;
        backend.forward = desktop::receive;
        desktop.ready("desktop");
        backend.ready("backend");
        try (var rpc = service(desktop);
             var remote = remote(backend)) {
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            endpoint.register(TOPIC, Files.class, id -> new FileInfo(id, 4));
            remote.register(TOPIC, Files.class, id -> new FileInfo(id, 8));
            Files backendFiles = endpoint.proxy(TOPIC, Files.class);
            Files desktopFiles = remote.proxy(TOPIC, Files.class);

            Assertions.assertEquals(new FileInfo("backend-file", 8), call(() -> backendFiles.info("backend-file")));
            Assertions.assertEquals(new FileInfo("desktop-file", 4), call(() -> desktopFiles.info("desktop-file")));
        }
    }

    @Test
    void handlersAndProxiesOwnTheirCodecsEvenOnTheSameTopic() throws Exception {
        MemoryChannel desktop = new MemoryChannel();
        MemoryChannel backend = new MemoryChannel();
        desktop.forward = backend::receive;
        backend.forward = desktop::receive;
        desktop.ready("desktop");
        backend.ready("backend");
        try (var rpc = service(desktop);
             var remote = remote(backend)) {
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            endpoint.register(TOPIC, Echo.class, value -> "desktop:" + value, new TaggedCodec("desktop:"));
            remote.register(TOPIC, Echo.class, value -> "backend:" + value, new TaggedCodec("backend:"));
            Echo backendCalls = endpoint.proxy(TOPIC, Echo.class, new TaggedCodec("backend:"));
            Echo desktopCalls = remote.proxy(TOPIC, Echo.class, new TaggedCodec("desktop:"));
            Echo incompatible = endpoint.proxy(TOPIC, Echo.class, new TaggedCodec("incompatible:"));

            Assertions.assertEquals("backend:a", call(() -> backendCalls.echo("a")));
            Assertions.assertEquals("desktop:b", call(() -> desktopCalls.echo("b")));
            MessagingRpcException failure = Assertions.assertThrows(MessagingRpcException.class,
                () -> call(() -> incompatible.echo("c")));
            Assertions.assertEquals(MessagingRpcMessage.Error.CODEC_FAILED, failure.remoteError());
            Assertions.assertEquals("backend:d", call(() -> backendCalls.echo("d")));
            Assertions.assertEquals(2, desktop.subscriptions.size());
        }
    }

    @Test
    void blockedDesktopHandlerAllowsOrderedRepliesAndConcurrentRequests() throws Exception {
        MemoryChannel desktop = new MemoryChannel();
        MemoryChannel backend = new MemoryChannel();
        desktop.forward = backend::receive;
        desktop.ready("desktop");
        backend.ready("backend");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        Set<Job> jobs = ConcurrentHashMap.newKeySet();
        try (var delivery = Executors.newSingleThreadExecutor();
             var executor = Executors.newVirtualThreadPerTaskExecutor();
             var rpc = service(desktop);
             var remote = remote(backend)) {
            backend.forward = message -> delivery.execute(() -> desktop.receive(message));
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            endpoint.register(TOPIC, Echo.class, value -> {
                Job job = Job.getJobManager().currentJob();
                Assertions.assertNotNull(job);
                Assertions.assertTrue(job.isSystem());
                jobs.add(job);
                int sequence = calls.incrementAndGet();
                if ("blocked".equals(value)) {
                    entered.countDown();
                    await(release);
                }
                return "desktop:" + value + ":" + sequence;
            });
            remote.register(TOPIC, Echo.class, value -> "backend:" + value);
            Echo desktopCalls = remote.proxy(TOPIC, Echo.class);
            Echo backendCalls = endpoint.proxy(TOPIC, Echo.class);
            Future<String> blocked = executor.submit(() -> desktopCalls.echo("blocked"));
            try {
                await(entered);
                Assertions.assertEquals("backend:reply", call(() -> backendCalls.echo("reply")));
                Assertions.assertEquals("desktop:fast:2", call(() -> desktopCalls.echo("fast")));
                Assertions.assertFalse(blocked.isDone());
                Assertions.assertEquals(2, calls.get());
                Assertions.assertEquals(2, jobs.size());
            } finally {
                release.countDown();
            }
            Assertions.assertEquals("desktop:blocked:1", blocked.get(3, TimeUnit.SECONDS));
        }
    }

    @Test
    void synchronousHandlerCanCallBackendWhichCallsDesktopBackOnOrderedDelivery() throws Exception {
        MemoryChannel desktop = new MemoryChannel();
        MemoryChannel backend = new MemoryChannel();
        desktop.forward = backend::receive;
        desktop.ready("desktop");
        backend.ready("backend");
        try (var delivery = Executors.newSingleThreadExecutor();
             var rpc = service(desktop);
             var remote = remote(backend)) {
            backend.forward = message -> delivery.execute(() -> desktop.receive(message));
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            Echo backendCalls = endpoint.proxy(TOPIC, Echo.class);
            Echo desktopCalls = remote.proxy(TOPIC, Echo.class);
            endpoint.register(TOPIC, Echo.class, value -> "callback".equals(value)
                ? "desktop:" + value : backendCalls.echo(value));
            remote.register(TOPIC, Echo.class, value -> desktopCalls.echo("callback"));

            Assertions.assertEquals("desktop:callback", call(() -> desktopCalls.echo("request")));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void suspendingOrClosingCancelsDesktopRequestsBeforeTheirJobsRun(boolean close) throws Exception {
        MemoryChannel desktop = new MemoryChannel();
        MemoryChannel backend = new MemoryChannel();
        desktop.forward = backend::receive;
        backend.forward = desktop::receive;
        desktop.ready("desktop");
        backend.ready("backend");
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<Job> delayed = new CompletableFuture<>();
        CountDownLatch finished = new CountDownLatch(1);
        JobChangeAdapter listener = new JobChangeAdapter() {
            @Override
            public void aboutToRun(@NotNull IJobChangeEvent event) {
                Job job = event.getJob();
                if ("Handle DataDam RPC request".equals(job.getName()) && !delayed.isDone() && job.sleep()) {
                    delayed.complete(job);
                }
            }

            @Override
            public void done(@NotNull IJobChangeEvent event) {
                if (event.getJob() == delayed.getNow(null)) {
                    finished.countDown();
                }
            }
        };
        try (var executor = Executors.newVirtualThreadPerTaskExecutor();
             var rpc = service(desktop);
             var remote = remote(backend)) {
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            endpoint.register(TOPIC, Echo.class, value -> {
                calls.incrementAndGet();
                return "old:" + value;
            });
            Echo desktopCalls = remote.proxy(TOPIC, Echo.class);
            Job.getJobManager().addJobChangeListener(listener);
            try {
                final Future<String> pending = executor.submit(() -> desktopCalls.echo("delayed"));
                final Job job = delayed.get(3, TimeUnit.SECONDS);
                if (close) {
                    rpc.close();
                } else {
                    rpc.suspend();
                }
                Assertions.assertTrue(endpoint.isCloseComplete());
                Assertions.assertTrue(desktop.subscriptions.isEmpty());
                Assertions.assertTrue(desktop.observers.isEmpty());
                Assertions.assertThrows(DBException.class, rpc::getRpc);
                backend.disconnected();
                Assertions.assertEquals(MessagingRpcException.Reason.DISCONNECTED, failure(pending).reason());
                job.wakeUp();
                await(finished);
                Assertions.assertEquals(0, calls.get());
                if (!close) {
                    rpc.resume();
                    desktop.ready("next-desktop");
                    backend.ready("next-backend");
                    MessagingRpcEndpoint replacement = rpc.getRpc();
                    Assertions.assertNotSame(endpoint, replacement);
                    replacement.register(TOPIC, Echo.class, value -> "new:" + value);
                    Assertions.assertEquals("new:current", call(() -> desktopCalls.echo("current")));
                    Assertions.assertEquals(0, calls.get());
                }
            } finally {
                Job.getJobManager().removeJobChangeListener(listener);
                delayed.thenAccept(Job::wakeUp);
            }
        }
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
             var remote = remote(backend);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            endpoint.register(TOPIC, Echo.class, value -> "retained:" + value, codec());
            remote.register(TOPIC, Echo.class, value -> {
                entered.countDown();
                await(release);
                return "late:" + value;
            }, codec());
            Echo proxy = endpoint.proxy(TOPIC, Echo.class, codec());
            Future<String> pending = executor.submit(() -> proxy.echo("pending"));
            try {
                await(entered);
                desktop.disconnected();
                Assertions.assertEquals(MessagingRpcException.Reason.DISCONNECTED, failure(pending).reason());
                Assertions.assertSame(endpoint, rpc.getRpc());
                Assertions.assertFalse(endpoint.isClosed());
                Assertions.assertThrows(MessagingRpcException.class, () -> endpoint.proxy(OTHER_TOPIC, Echo.class, codec()));
                Assertions.assertThrows(MessagingRpcException.class, () -> proxy.echo("offline"));

                backend.disconnected();
                desktop.ready("second");
                backend.ready("backend-second");

                Assertions.assertSame(endpoint, rpc.getRpc());
                Assertions.assertEquals(2, desktop.subscriptions.size());
                Assertions.assertEquals(1, desktop.observers.size());
                Assertions.assertEquals("retained:new", call(() -> remote.proxy(TOPIC, Echo.class, codec()).echo("new")));
            } finally {
                release.countDown();
            }
            remote.register(OTHER_TOPIC, Echo.class, value -> "reconnected:" + value, codec());
            Assertions.assertEquals("reconnected:new", call(() -> endpoint.proxy(OTHER_TOPIC, Echo.class, codec()).echo("new")));
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
             var remote = remote(backend)) {
            MessagingRpcEndpoint old = rpc.getRpc();
            final var registration = old.register(TOPIC, Echo.class, value -> "old:" + value, codec());
            final Echo oldProxy = old.proxy(TOPIC, Echo.class, codec());
            final Echo remoteProxy = remote.proxy(TOPIC, Echo.class, codec());
            rpc.suspend();
            Assertions.assertTrue(old.isCloseComplete());
            Assertions.assertTrue(desktop.subscriptions.isEmpty());
            Assertions.assertTrue(desktop.observers.isEmpty());
            Assertions.assertEquals(MessagingRpcException.Reason.CLOSED,
                Assertions.assertThrows(MessagingRpcException.class, () -> oldProxy.echo("old")).reason());
            Assertions.assertThrows(DBException.class, rpc::getRpc);
            rpc.resume();
            desktop.ready("second");
            MessagingRpcEndpoint current = rpc.getRpc();
            Assertions.assertNotSame(old, current);
            Echo currentProxy = current.proxy(TOPIC, Echo.class, codec());
            Assertions.assertNotSame(oldProxy, currentProxy);
            MessagingRpcException missing = Assertions.assertThrows(MessagingRpcException.class,
                () -> call(() -> remoteProxy.echo("missing")));
            Assertions.assertEquals(MessagingRpcMessage.Error.UNKNOWN_SERVICE, missing.remoteError());
            current.register(TOPIC, Echo.class, value -> "new:" + value, codec());
            registration.close();

            Assertions.assertEquals("new:value", call(() -> remoteProxy.echo("value")));
            Assertions.assertEquals(MessagingRpcException.Reason.CLOSED,
                Assertions.assertThrows(MessagingRpcException.class, () -> oldProxy.echo("still-old")).reason());
        }
    }

    @Test
    void callerClosedEndpointsAreReplacedOnlyWhenReady() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        try (var rpc = service(channel)) {
            MessagingRpcEndpoint old = rpc.getRpc();
            old.proxy(TOPIC, Echo.class, codec());
            old.close();
            MessagingRpcEndpoint replacement = rpc.getRpc();
            Assertions.assertNotSame(old, replacement);
            Assertions.assertFalse(replacement.isClosed());
            Assertions.assertTrue(channel.subscriptions.isEmpty());
            Assertions.assertTrue(channel.observers.isEmpty());
            channel.disconnected();
            replacement.close();
            Assertions.assertThrows(DBException.class, rpc::getRpc);
            channel.ready("second");
            Assertions.assertNotSame(replacement, rpc.getRpc());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void externallyClosingEndpointStaysReservedThroughReadyAndIdentityChangesUntilCleanupCompletes(boolean identityChange)
            throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        channel.onUnsubscribe = closingTopic -> {
            if (TOPIC.equals(closingTopic)) {
                entered.countDown();
                await(release);
            }
        };
        try (var rpc = service(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            endpoint.proxy(TOPIC, Echo.class, codec());
            var closing = executor.submit(endpoint::close);
            try {
                await(entered);
                Assertions.assertTrue(endpoint.isClosed());
                Assertions.assertFalse(endpoint.isCloseComplete());
                if (identityChange) {
                    rpc.suspend();
                    Assertions.assertThrows(DBException.class, rpc::getRpc);
                    rpc.resume();
                }
                channel.ready("second");
                Assertions.assertThrows(DBException.class, rpc::getRpc);
                Assertions.assertEquals(1L, channel.subscribedTopics.stream().filter(TOPIC::equals).count());
            } finally {
                release.countDown();
            }
            closing.get(3, TimeUnit.SECONDS);
            Assertions.assertTrue(endpoint.isCloseComplete());
            MessagingRpcEndpoint replacement = rpc.getRpc();
            Assertions.assertNotSame(endpoint, replacement);
            Assertions.assertFalse(replacement.isClosed());
            Assertions.assertTrue(channel.subscriptions.isEmpty());
            replacement.proxy(TOPIC, Echo.class, codec());
            Assertions.assertEquals(2L, channel.subscribedTopics.stream().filter(TOPIC::equals).count());
        }
    }

    @Test
    void failedBindingOpenRollsBackSubscriptionsAndRetainsTheSharedEndpoint() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        try (var rpc = service(channel)) {
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            channel.failTopic = TOPIC;
            for (int attempt = 0; attempt < 40; attempt++) {
                Assertions.assertThrows(MessagingRpcException.class, () -> endpoint.proxy(TOPIC, Echo.class, codec()));
                Assertions.assertTrue(channel.subscriptions.isEmpty());
                Assertions.assertTrue(channel.observers.isEmpty());
                Assertions.assertSame(endpoint, rpc.getRpc());
            }
            channel.failTopic = null;
            endpoint.proxy(TOPIC, Echo.class, codec());
            Assertions.assertFalse(endpoint.isClosed());
            Assertions.assertEquals(2, channel.subscriptions.size());
            Assertions.assertEquals(1, channel.observers.size());
        }
    }

    @Test
    void concurrentAccessPublishesOneLazyEndpointAndOneSessionPerTopic() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        try (var rpc = service(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CyclicBarrier barrier = new CyclicBarrier(8);
            List<Future<MessagingRpcEndpoint>> pending = new ArrayList<>();
            for (int index = 0; index < 8; index++) {
                pending.add(executor.submit(() -> {
                    barrier.await(3, TimeUnit.SECONDS);
                    return rpc.getRpc();
                }));
            }
            MessagingRpcEndpoint endpoint = pending.getFirst().get(3, TimeUnit.SECONDS);
            for (Future<MessagingRpcEndpoint> result : pending) {
                Assertions.assertSame(endpoint, result.get(3, TimeUnit.SECONDS));
            }
            Assertions.assertTrue(channel.subscriptions.isEmpty());
            Assertions.assertTrue(channel.observers.isEmpty());
            List<Future<Echo>> proxies = new ArrayList<>();
            for (int index = 0; index < 8; index++) {
                proxies.add(executor.submit(() -> {
                    barrier.await(3, TimeUnit.SECONDS);
                    return endpoint.proxy(TOPIC, Echo.class, codec());
                }));
            }
            for (Future<Echo> proxy : proxies) {
                Assertions.assertNotNull(proxy.get(3, TimeUnit.SECONDS));
            }
            Assertions.assertEquals(2, channel.subscriptions.size());
            Assertions.assertEquals(1, channel.observers.size());
        }
    }

    @Test
    void shutdownClosesAllBindingsBeforeClosingThePhysicalChannel() throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        try (var rpc = service(channel)) {
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            endpoint.register(TOPIC, Echo.class, value -> value, codec());
            endpoint.proxy(OTHER_TOPIC, Echo.class, codec());
            Mockito.doAnswer(invocation -> {
                Assertions.assertTrue(endpoint.isCloseComplete());
                Assertions.assertTrue(channel.subscriptions.isEmpty());
                Assertions.assertTrue(channel.observers.isEmpty());
                Assertions.assertThrows(DBException.class, rpc::getRpc);
                channel.close();
                return null;
            }).when(rpc.channel).close();

            rpc.close();

            Assertions.assertTrue(channel.closed);
            Assertions.assertEquals(MessagingRpcException.Reason.CLOSED,
                Assertions.assertThrows(MessagingRpcException.class, () -> endpoint.proxy(TOPIC, Echo.class, codec())).reason());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void identityChangeDuringBindingOpenReservesTheEndpointUntilRollbackCompletes(boolean handler) throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        blockTopicOpen(channel, entered, release);
        try (var rpc = service(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            MessagingRpcEndpoint old = rpc.getRpc();
            Future<?> opening = executor.submit(() -> bind(old, handler));
            try {
                await(entered);
                rpc.suspend();
                Assertions.assertTrue(old.isClosed());
                Assertions.assertFalse(old.isCloseComplete());
                rpc.resume();
                channel.ready("second");
                Assertions.assertThrows(DBException.class, rpc::getRpc);
                Assertions.assertEquals(1L,
                    channel.subscribedTopics.stream().filter(TOPIC::equals).count());
                release.countDown();

                ExecutionException failure = Assertions.assertThrows(ExecutionException.class,
                    () -> opening.get(3, TimeUnit.SECONDS));
                Assertions.assertInstanceOf(MessagingRpcException.class, failure.getCause());
                Assertions.assertTrue(old.isCloseComplete());
                Assertions.assertTrue(channel.subscriptions.isEmpty());
                Assertions.assertTrue(channel.observers.isEmpty());
                MessagingRpcEndpoint current = rpc.getRpc();
                Assertions.assertNotSame(old, current);
                Assertions.assertSame(current, rpc.getRpc());
                current.proxy(TOPIC, Echo.class, codec());
                Assertions.assertEquals(2, channel.subscriptions.size());
                Assertions.assertEquals(1, channel.observers.size());
            } finally {
                release.countDown();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shutdownDuringBindingOpenDisposesTheLateSessionAndSubscriptions(boolean handler) throws Exception {
        MemoryChannel channel = new MemoryChannel();
        channel.ready("first");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        blockTopicOpen(channel, entered, release);
        try (var rpc = service(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            Future<?> opening = executor.submit(() -> bind(endpoint, handler));
            try {
                await(entered);
                rpc.close();
                release.countDown();
                ExecutionException failure = Assertions.assertThrows(ExecutionException.class,
                    () -> opening.get(3, TimeUnit.SECONDS));
                Assertions.assertInstanceOf(MessagingRpcException.class, failure.getCause());
                Assertions.assertTrue(endpoint.isCloseComplete());
                Assertions.assertTrue(channel.subscriptions.isEmpty());
                Assertions.assertTrue(channel.observers.isEmpty());
                Assertions.assertThrows(DBException.class, rpc::getRpc);
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
        channel.ready("first");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        channel.onObserve = () -> {
            entered.countDown();
            await(release);
        };
        try (var rpc = service(channel);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            MessagingRpcEndpoint endpoint = rpc.getRpc();
            Future<?> opening = executor.submit(() -> endpoint.proxy(TOPIC, Echo.class, codec()));
            try {
                await(entered);
                rpc.close();
            } finally {
                release.countDown();
            }
            ExecutionException failure = Assertions.assertThrows(ExecutionException.class,
                () -> opening.get(3, TimeUnit.SECONDS));
            Assertions.assertInstanceOf(MessagingRpcException.class, failure.getCause());
            Assertions.assertTrue(endpoint.isCloseComplete());
            Assertions.assertTrue(channel.subscriptions.isEmpty());
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
    private static MessagingRpcEndpoint remote(@NotNull MemoryChannel channel) {
        return MessagingRpcEndpoint.open(topic -> DDRemoteDamRpcTransport.forChannel(channel, topic, Thread::startVirtualThread), codec());
    }

    @NotNull
    private static MessagingRpcCodec<JsonElement> codec() {
        return new DDRemoteDamGsonRpcCodec(new Gson());
    }

    @NotNull
    private static Object bind(@NotNull MessagingRpcEndpoint endpoint, boolean handler) {
        return handler
            ? endpoint.register(TOPIC, Echo.class, value -> value, codec())
            : endpoint.proxy(TOPIC, Echo.class, codec());
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
    private static MessagingRpcException failure(@NotNull Future<?> pending) {
        ExecutionException failure = Assertions.assertThrows(ExecutionException.class, () -> pending.get(3, TimeUnit.SECONDS));
        return Assertions.assertInstanceOf(MessagingRpcException.class, failure.getCause());
    }

    private static void await(@NotNull CountDownLatch latch) {
        try {
            Assertions.assertTrue(latch.await(3, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void blockTopicOpen(
        @NotNull MemoryChannel channel,
        @NotNull CountDownLatch entered,
        @NotNull CountDownLatch release
    ) {
        AtomicBoolean first = new AtomicBoolean(true);
        channel.onSubscribe = topic -> {
            if (TOPIC.equals(topic) && first.compareAndSet(true, false)) {
                entered.countDown();
                await(release);
            }
        };
    }

    /** Exchanges a single value over an RPC topic. */
    public interface Echo {
        @NotNull
        String echo(@NotNull String value);
    }

    /** Reads file metadata over RPC. */
    public interface Files {
        @NotNull
        FileInfo info(@NotNull String id);
    }

    public record FileInfo(String id, long size) {
    }

    private static final class Desktop implements AutoCloseable {
        private final DDDesktopAccessService access = Mockito.mock(DDDesktopAccessService.class);
        private final DDRemoteDamDesktopChannel channel = Mockito.mock(DDRemoteDamDesktopChannel.class);
        private final DDRemoteDamDesktopSessionService session;

        private Desktop(@NotNull MemoryChannel messages) {
            Mockito.when(channel.getStatus()).thenAnswer(invocation -> messages.getStatus());
            Mockito.when(channel.observeState(Mockito.any())).thenAnswer(invocation ->
                messages.observeState(invocation.getArgument(0)));
            Mockito.when(channel.subscribe(Mockito.anyString(), Mockito.any())).thenAnswer(invocation ->
                messages.subscribe(invocation.getArgument(0), invocation.getArgument(1)));
            Mockito.when(channel.send(Mockito.any())).thenAnswer(invocation -> messages.send(invocation.getArgument(0)));
            Mockito.when(access.getStatus()).thenReturn(DDDesktopAccessService.Status.SIGNED_IN);
            Mockito.doAnswer(invocation -> {
                messages.disconnected();
                return null;
            }).when(channel).pause();
            Mockito.doAnswer(invocation -> {
                messages.close();
                return null;
            }).when(channel).close();
            session = new DDRemoteDamDesktopSessionService(access, channel);
        }

        private void start() {
            session.start();
        }

        private void suspend() {
            listener().accessChanging();
        }

        private void resume() {
            listener().accessChanged();
        }

        @NotNull
        private MessagingRpcEndpoint getRpc() throws DBException {
            return session.getRpc();
        }

        @NotNull
        private DDDesktopAccessListener listener() {
            ArgumentCaptor<DDDesktopAccessListener> listener = ArgumentCaptor.forClass(DDDesktopAccessListener.class);
            Mockito.verify(access).addAccessListener(listener.capture());
            return listener.getValue();
        }

        @Override
        public void close() {
            session.close();
        }
    }

    private static final class MemoryChannel implements DDRemoteDamChannel {
        private final List<TopicSubscription> subscriptions = new CopyOnWriteArrayList<>();
        private final List<String> subscribedTopics = new CopyOnWriteArrayList<>();
        private final List<DDRemoteDamMessage> sentMessages = new CopyOnWriteArrayList<>();
        private final List<Consumer<DDRemoteDamChannelStatus>> observers = new CopyOnWriteArrayList<>();
        private volatile DDRemoteDamChannelStatus status = new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null);
        @Nullable
        private Consumer<DDRemoteDamMessage> forward;
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
        public DDRemoteDamChannelStatus getStatus() {
            return status;
        }

        @Override
        @NotNull
        public DDRemoteDamSubscription observeState(@NotNull Consumer<DDRemoteDamChannelStatus> observer) {
            observers.add(observer);
            observer.accept(status);
            if (onObserve != null) {
                onObserve.run();
            }
            return () -> observers.remove(observer);
        }

        @Override
        @NotNull
        public CompletionStage<Void> send(@NotNull DDRemoteDamMessage message) {
            if (!status.isReady()) {
                return CompletableFuture.failedFuture(new DBException("Channel disconnected"));
            }
            sentMessages.add(message);
            if (forward != null) {
                forward.accept(message);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        @NotNull
        public DDRemoteDamSubscription subscribe(@NotNull String topic, @NotNull DDRemoteDamMessageListener listener) {
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
        public DDRemoteDamSubscription addSessionListener(@NotNull DDRemoteDamSessionListener listener) {
            throw new UnsupportedOperationException();
        }

        private void ready(@NotNull String damSessionId) {
            setStatus(new DDRemoteDamChannelStatus(
                DDRemoteDamChannelState.CONNECTED, new DDRemoteDamSessionInfo("deployment", "instance", damSessionId)));
        }

        private void disconnected() {
            setStatus(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.DISCONNECTED, null));
        }

        private void setStatus(@NotNull DDRemoteDamChannelStatus next) {
            status = next;
            List.copyOf(observers).forEach(observer -> observer.accept(next));
        }

        @SuppressWarnings("PMD.UnusedPrivateMethod")
        private void receive(@NotNull DDRemoteDamMessage message) {
            DDRemoteDamChannelStatus current = status;
            if (current.isReady()) {
                for (TopicSubscription subscription : subscriptions) {
                    if (subscription.topic().equals(message.topic())) {
                        subscription.listener().onMessage(Objects.requireNonNull(current.session()), message);
                    }
                }
            }
        }

        @Override
        public void close() {
            closed = true;
            setStatus(new DDRemoteDamChannelStatus(DDRemoteDamChannelState.CLOSED, null));
        }
    }

    private record TopicSubscription(@NotNull String topic, @NotNull DDRemoteDamMessageListener listener) {
    }

    private record TaggedCodec(@NotNull String tag) implements MessagingRpcCodec<JsonElement> {
        @Override
        @NotNull
        public JsonElement encode(@Nullable Object value, @NotNull Type type) {
            return new JsonPrimitive(tag + Objects.requireNonNull(value));
        }

        @Override
        @NotNull
        public Object decode(@Nullable JsonElement payload, @NotNull Type type) {
            String encoded = Objects.requireNonNull(payload).getAsString();
            if (!encoded.startsWith(tag)) {
                throw new IllegalArgumentException("Unexpected codec tag");
            }
            return encoded.substring(tag.length());
        }
    }
}
