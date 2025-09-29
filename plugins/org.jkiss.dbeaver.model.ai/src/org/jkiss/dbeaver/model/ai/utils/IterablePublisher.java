/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class IterablePublisher<T> implements Flow.Publisher<T> {
    private static final Log log = Log.getLog(IterablePublisher.class);
    private final Executor executor;

    private final Iterable<T> source;


    /**
     * Publish all items from the given iterable using the provided executor.
     */
    public IterablePublisher(@NotNull Iterable<T> source, @NotNull Executor executor) {
        this.source = source;
        this.executor = executor;
    }

    @Override
    public void subscribe(@NotNull Flow.Subscriber<? super T> subscriber) {
        // One subscriber per publisher instance is fine; create a fresh Subscription per subscribe().
        IterableSubscription<T> s = new IterableSubscription<>(subscriber, source.iterator(), executor);
        subscriber.onSubscribe(s);
    }

    private static final class IterableSubscription<T> implements Flow.Subscription {
        private final Flow.Subscriber<? super T> downstream;
        private final Iterator<T> it;
        private final Executor executor;

        private final AtomicLong requested = new AtomicLong(0);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final AtomicInteger wip = new AtomicInteger(0); // drain reentry guard

        IterableSubscription(
            @NotNull Flow.Subscriber<? super T> downstream,
            @NotNull Iterator<T> it,
            @NotNull Executor executor
        ) {
            this.downstream = downstream;
            this.it = it;
            this.executor = executor;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                signalError(new IllegalArgumentException("Invalid request: " + n));
                return;
            }
            addCap(requested, n);
            scheduleDrain();
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }

        private void scheduleDrain() {
            if (wip.getAndIncrement() == 0) {
                executor.execute(this::drain);
            }
        }

        private void drain() {
            int missed = 1;
            while (!cancelled.get()) {

                long r = requested.get();
                long e = 0L;

                while (e != r && !cancelled.get()) {
                    final T next;
                    synchronized (this) { // iterator isn’t thread-safe; guard it
                        if (!it.hasNext()) {
                            completeOnce();
                            return;
                        }
                        next = it.next();
                    }

                    try {
                        downstream.onNext(next);
                    } catch (Throwable t) {
                        signalError(t);
                        return;
                    }
                    e++;
                }

                if (e != 0L) {
                    requested.addAndGet(-e);
                }

                if (!itHasMore() && !done.get()) {
                    completeOnce();
                    return;
                }

                int w = wip.addAndGet(-missed);
                if (w == 0) {
                    break;
                }
                missed = 1;
            }
        }

        private boolean itHasMore() {
            synchronized (this) {
                return it.hasNext();
            }
        }

        private void completeOnce() {
            if (done.compareAndSet(false, true)) {
                if (!cancelled.get()) {
                    try {
                        downstream.onComplete();
                    } catch (Throwable throwable) {
                        log.error("Error in subscriber onComplete", throwable);
                    }
                }
            }
        }

        private void signalError(Throwable t) {
            if (done.compareAndSet(false, true)) {
                cancelled.set(true);
                try {
                    downstream.onError(t);
                } catch (Throwable throwable) {
                    log.error("Error in subscriber", throwable);
                }
            }
        }

        // Saturating add like Reactive Streams do
        private static long addCap(AtomicLong requested, long n) {
            while (true) {
                long r = requested.get();
                long u = r + n;
                if (u < 0L) {
                    u = Long.MAX_VALUE;
                }
                if (requested.compareAndSet(r, u)) {
                    return u;
                }
            }
        }
    }
}
