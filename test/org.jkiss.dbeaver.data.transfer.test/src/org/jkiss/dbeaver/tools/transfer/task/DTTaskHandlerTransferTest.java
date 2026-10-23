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
package org.jkiss.dbeaver.tools.transfer.task;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.runtime.DBInterruptedException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.*;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class DTTaskHandlerTransferTest {
    private DataTransferSettings settings;
    private DBTTask task;
    private DBTTaskExecutionListener listener;
    private DBRRunnableContext context;
    private IDataTransferProducer<?> producer;
    private IDataTransferConsumer<?, ?> consumer;
    private DBRProgressMonitor monitor;
    private NullProgressMonitor nestedMonitor;

    @BeforeAll
    static void initializePlatform() {
        // initialize shared services before transfer workers access them concurrently
        DBWorkbench.getPlatform();
    }

    @BeforeEach
    void setUp() throws Exception {
        settings = Mockito.mock(DataTransferSettings.class);
        task = Mockito.mock(DBTTask.class);
        listener = Mockito.mock(DBTTaskExecutionListener.class);
        context = Mockito.mock(DBRRunnableContext.class);
        producer = Mockito.mock(IDataTransferProducer.class);
        consumer = Mockito.mock(IDataTransferConsumer.class);
        monitor = Mockito.mock(DBRProgressMonitor.class);
        nestedMonitor = new NullProgressMonitor();
        Mockito.when(monitor.getNestedMonitor()).thenReturn(nestedMonitor);
        Mockito.when(monitor.isCanceled()).thenAnswer(invocation -> nestedMonitor.isCanceled());

        DataTransferNodeDescriptor consumerNode = Mockito.mock(DataTransferNodeDescriptor.class);
        Mockito.when(consumerNode.getName()).thenReturn("CSV");
        Mockito.when(settings.getConsumer()).thenReturn(consumerNode);
        Mockito.when(settings.getNodeSettings(consumerNode)).thenReturn(Mockito.mock(IDataTransferSettings.class));
        DataTransferPipe pipe = new DataTransferPipe(producer, consumer);
        Mockito.when(settings.getDataPipes()).thenReturn(List.of(pipe));
        Mockito.when(settings.getMaxJobCount()).thenReturn(1);
        Mockito.when(settings.acquireDataPipe(Mockito.any(), Mockito.eq(task))).thenReturn(pipe, (DataTransferPipe) null);
        Mockito.doAnswer(invocation -> {
            DBRRunnableWithProgress runnable = invocation.getArgument(2);
            runnable.run(monitor);
            return null;
        }).when(context).run(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any());
    }

    @Test
    void reportsMonitorCancellationAsError() throws Exception {
        Mockito.doAnswer(invocation -> {
            nestedMonitor.setCanceled(true);
            return null;
        }).when(producer).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());

        executeTransfer();

        assertCancellation();
        Mockito.verify(consumer, Mockito.never()).finishTransfer(Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.eq(true));
    }

    @Test
    void reportsCanceledWorkerAsError() throws Exception {
        Mockito.doThrow(new DBInterruptedException("Producer canceled")).when(producer).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());

        executeTransfer();

        Assertions.assertFalse(monitor.isCanceled());
        assertCancellation();
        Mockito.verify(consumer, Mockito.never()).finishTransfer(Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.eq(true));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void reportsCancellationOfQueuedWorkers(int jobCount) throws Exception {
        DataTransferPipe pipe = new DataTransferPipe(producer, consumer);
        Mockito.when(settings.getDataPipes()).thenReturn(Collections.nCopies(jobCount, pipe));
        Mockito.when(settings.getMaxJobCount()).thenReturn(jobCount);
        JobChangeAdapter cancellationListener = new JobChangeAdapter() {
            @Override
            public void scheduled(@NotNull IJobChangeEvent event) {
                if (event.getJob() instanceof DataTransferJob job && job.getSettings() == settings) {
                    job.cancel();
                }
            }
        };
        Job.getJobManager().suspend();
        Job.getJobManager().addJobChangeListener(cancellationListener);
        try {
            executeTransfer();
        } finally {
            Job.getJobManager().removeJobChangeListener(cancellationListener);
            Job.getJobManager().resume();
        }

        Assertions.assertFalse(monitor.isCanceled());
        assertCancellation();
        Mockito.verify(producer, Mockito.never()).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());
        Mockito.verify(consumer, Mockito.never()).finishTransfer(Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.eq(true));
    }

    @Test
    void reportsCanceledParallelWorkerAsError() throws Exception {
        Mockito.when(settings.getDataPipes()).thenReturn(List.of(
            new DataTransferPipe(producer, consumer), new DataTransferPipe(producer, consumer)));
        Mockito.when(settings.getMaxJobCount()).thenReturn(2);
        Mockito.doThrow(new DBInterruptedException("Producer canceled")).when(producer).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());

        executeTransfer();

        Assertions.assertFalse(monitor.isCanceled());
        assertCancellation();
        Mockito.verify(consumer, Mockito.never()).finishTransfer(Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.eq(true));
    }

    @Test
    void reportsInterruptedContextAsError() throws Exception {
        Mockito.doThrow(new InterruptedException()).when(context).run(Mockito.eq(true), Mockito.eq(true), Mockito.any());

        executeTransfer();

        assertCancellation();
    }

    @Test
    void reportsCanceledContextAsError() throws Exception {
        Mockito.doThrow(new OperationCanceledException()).when(context).run(Mockito.eq(true), Mockito.eq(true), Mockito.any());

        executeTransfer();

        assertCancellation();
    }

    @Test
    void reportsSuccessfulTransferWithoutError() throws Exception {
        executeTransfer();

        Mockito.verify(listener).taskFinished(Mockito.eq(task), Mockito.isNull(), Mockito.isNull(), Mockito.eq(settings));
    }

    @Test
    void preservesTransferError() throws Exception {
        DBException error = new DBException("Transfer failed");
        Mockito.doThrow(error).when(producer).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());

        executeTransfer();

        Mockito.verify(listener).taskFinished(Mockito.eq(task), Mockito.isNull(), Mockito.same(error), Mockito.eq(settings));
    }

    @ParameterizedTest
    @CsvSource({"0, false", "1, false", "0, true", "1, true"})
    void preservesParallelFailureOnCancellation(int canceledIndex, boolean cancelMonitor) throws Exception {
        IDataTransferProducer<?> canceledProducer = Mockito.mock(IDataTransferProducer.class);
        IDataTransferProducer<?> failedProducer = Mockito.mock(IDataTransferProducer.class);
        DataTransferPipe canceledPipe = new DataTransferPipe(canceledProducer, consumer);
        DataTransferPipe failedPipe = new DataTransferPipe(failedProducer, consumer);
        Mockito.when(settings.getDataPipes()).thenReturn(List.of(canceledPipe, failedPipe));
        Mockito.when(settings.getMaxJobCount()).thenReturn(2);
        Set<Job> acquired = ConcurrentHashMap.newKeySet();
        String canceledJobName = "Data transfer job [" + canceledIndex + "]: CSV";
        Mockito.when(settings.acquireDataPipe(Mockito.any(), Mockito.eq(task))).thenAnswer(invocation -> {
            Job job = Job.getJobManager().currentJob();
            Assertions.assertNotNull(job);
            if (!acquired.add(job)) {
                return null;
            }
            return job.getName().equals(canceledJobName) ? canceledPipe : failedPipe;
        });

        CountDownLatch failureDone = new CountDownLatch(1);
        JobChangeAdapter jobListener = new JobChangeAdapter() {
            @Override
            public void done(@NotNull IJobChangeEvent event) {
                if (event.getJob() instanceof DataTransferJob job
                    && job.getSettings() == settings && !job.getName().equals(canceledJobName)) {
                    failureDone.countDown();
                }
            }
        };
        DBException failure = new DBException("Output disk is full");
        Mockito.doThrow(failure).when(failedProducer).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());
        Mockito.doAnswer(invocation -> {
            Assertions.assertTrue(failureDone.await(5, TimeUnit.SECONDS));
            if (cancelMonitor) {
                nestedMonitor.setCanceled(true);
            }
            throw new DBInterruptedException("Producer canceled");
        }).when(canceledProducer).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());

        Job.getJobManager().addJobChangeListener(jobListener);
        try {
            executeTransfer();
        } finally {
            Job.getJobManager().removeJobChangeListener(jobListener);
        }

        Mockito.verify(listener).taskFinished(Mockito.eq(task), Mockito.isNull(), Mockito.same(failure), Mockito.eq(settings));
        Assertions.assertEquals(1, failure.getSuppressed().length);
        Assertions.assertInstanceOf(DBInterruptedException.class, failure.getSuppressed()[0]);
        Mockito.verify(consumer, Mockito.never()).finishTransfer(Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.eq(true));
    }

    private void executeTransfer() throws DBException {
        new DTTaskHandlerTransfer().executeWithSettings(context, task, Locale.ENGLISH, Mockito.mock(Log.class), null, listener, settings);
    }

    private void assertCancellation() {
        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(listener).taskFinished(Mockito.eq(task), Mockito.isNull(), error.capture(), Mockito.eq(settings));
        Assertions.assertInstanceOf(DBInterruptedException.class, error.getValue());
        Assertions.assertEquals("Data transfer was canceled", error.getValue().getMessage());
    }
}
