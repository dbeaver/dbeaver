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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectJobTest {
    @Test
    public void cancelingMonitorInterruptsSynchronousConnectionWithoutOwnerJob() throws Exception {
        AtomicBoolean canceled = new AtomicBoolean();
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(monitor.isCanceled()).thenAnswer(invocation -> canceled.get());

        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getName()).thenReturn("Test connection");
        Mockito.when(container.getDriver()).thenReturn(Mockito.mock(DBPDriver.class));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Mockito.when(container.connect(monitor, true, false)).thenAnswer(invocation -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                interrupted.countDown();
            }
            return false;
        });

        Thread worker = Thread.ofVirtual().start(() -> new ConnectJob(container).runSync(monitor));
        try {
            Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
            canceled.set(true);
            Assertions.assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            worker.join(5000);
        }
        Assertions.assertFalse(worker.isAlive());
    }

    @Test
    public void canceledConnectionThatIgnoresInterruptionIsDisconnected() throws Exception {
        AtomicBoolean canceled = new AtomicBoolean();
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(monitor.isCanceled()).thenAnswer(invocation -> canceled.get());

        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getName()).thenReturn("Test connection");
        Mockito.when(container.getDriver()).thenReturn(Mockito.mock(DBPDriver.class));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finishInitialization = new CountDownLatch(1);
        Mockito.when(container.connect(monitor, true, false)).thenAnswer(invocation -> {
            started.countDown();
            // Simulate a driver which completes initialization despite cancellation.
            while (true) {
                try {
                    finishInitialization.await();
                    return true;
                } catch (InterruptedException ignored) {
                }
            }
        });

        ConnectJob job = new ConnectJob(container);
        Thread worker = Thread.ofVirtual().start(() -> job.runSync(monitor));
        try {
            Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
            canceled.set(true);
        } finally {
            finishInitialization.countDown();
            worker.join(5000);
        }
        Assertions.assertFalse(worker.isAlive());
        Assertions.assertEquals(IStatus.CANCEL, job.getConnectStatus().getSeverity());
        Mockito.verify(container).disconnect(Mockito.any(VoidProgressMonitor.class));
    }

    @Test
    public void canceledConnectionExceptionIsReportedAsError() throws Exception {
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(monitor.isCanceled()).thenReturn(true);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getName()).thenReturn("Test connection");
        Mockito.when(container.getDriver()).thenReturn(Mockito.mock(DBPDriver.class));
        DBException cancellation = new DBException("Connection has been canceled");
        Mockito.when(container.connect(monitor, true, false)).thenThrow(cancellation);

        ConnectJob job = new ConnectJob(container);
        job.runSync(monitor);

        Assertions.assertEquals(IStatus.ERROR, job.getConnectStatus().getSeverity());
        Assertions.assertSame(cancellation, job.getConnectError());
        Mockito.verify(container, Mockito.never()).disconnect(Mockito.any());
    }
}
