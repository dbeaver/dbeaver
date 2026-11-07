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
package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.model.qm.QMEventAction;
import org.jkiss.dbeaver.model.qm.QMMetaEvent;
import org.jkiss.dbeaver.model.qm.QMMetaListener;
import org.jkiss.dbeaver.model.qm.meta.QMMObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class QMMCollectorFlushTest extends DBeaverUnitTest {

    @Test
    public void testFlushWaitsForEventPersistence() throws Exception {
        QMMCollectorImpl collector = new QMMCollectorImpl();
        QMMetaEvent event = new QMMetaEvent(
            Mockito.mock(QMMObject.class), QMEventAction.END, System.currentTimeMillis(), "test-session"
        );
        QMMetaEvent laterEvent = new QMMetaEvent(
            Mockito.mock(QMMObject.class), QMEventAction.END, System.currentTimeMillis(), "test-session"
        );
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<QMMetaEvent> persisted = new ArrayList<>();
        QMMetaListener listener = (monitor, events) -> {
            started.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to persist QM events");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
            persisted.addAll(events);
        };
        collector.addListener(listener);

        Field poolField = QMMCollectorImpl.class.getDeclaredField("eventPool");
        poolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<QMMetaEvent> eventPool = (List<QMMetaEvent>) poolField.get(collector);
        synchronized (eventPool) {
            eventPool.add(event);
        }

        try {
            CompletableFuture<Void> flush = CompletableFuture.runAsync(collector::flushPendingEvents);
            Assertions.assertTrue(started.await(10, TimeUnit.SECONDS));
            Assertions.assertFalse(flush.isDone(), "Flush completed before event persistence");
            synchronized (eventPool) {
                eventPool.add(laterEvent);
            }
            release.countDown();
            flush.get(10, TimeUnit.SECONDS);
            Assertions.assertEquals(List.of(event, laterEvent), persisted);
        } finally {
            release.countDown();
            collector.removeListener(listener);
            collector.dispose();
        }
    }
}
