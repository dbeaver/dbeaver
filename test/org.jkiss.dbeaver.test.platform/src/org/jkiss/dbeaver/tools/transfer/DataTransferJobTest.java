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
package org.jkiss.dbeaver.tools.transfer;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DataTransferJobTest {
    @Test
    public void cancelingSingleWorkerCancelsBlockOnParentMonitor() throws Exception {
        DataTransferSettings settings = Mockito.mock(DataTransferSettings.class);
        DataTransferNodeDescriptor consumer = Mockito.mock(DataTransferNodeDescriptor.class);
        Mockito.when(consumer.getName()).thenReturn("Database");
        Mockito.when(settings.getConsumer()).thenReturn(consumer);
        Mockito.when(settings.getDataPipes()).thenReturn(List.of());

        DBRProgressMonitor parentMonitor = Mockito.mock(DBRProgressMonitor.class);
        DBRBlockingObject block = Mockito.mock(DBRBlockingObject.class);
        Mockito.when(parentMonitor.getActiveBlocks()).thenReturn(List.of(block));
        CountDownLatch blockCanceled = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            blockCanceled.countDown();
            return null;
        }).when(block).cancelBlock(Mockito.eq(parentMonitor), Mockito.nullable(Thread.class));

        DBTTask task = Mockito.mock(DBTTask.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Mockito.when(settings.acquireDataPipe(Mockito.any(), Mockito.eq(task))).thenAnswer(invocation -> {
            started.countDown();
            release.await();
            return null;
        });

        DataTransferJob job = new DataTransferJob(settings, task, Log.getLog(getClass()), null, parentMonitor, 0);
        try {
            job.schedule();
            Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
            job.cancel();
            Assertions.assertTrue(blockCanceled.await(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            job.join(5000, new NullProgressMonitor());
        }
    }
}
