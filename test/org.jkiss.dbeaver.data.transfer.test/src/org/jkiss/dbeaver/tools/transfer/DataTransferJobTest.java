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

import org.eclipse.core.runtime.IStatus;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.List;

class DataTransferJobTest {

    private DataTransferSettings settings;
    private DBTTask task;
    private Log log;
    private IDataTransferProducer<?> producer;
    private IDataTransferConsumer<?, ?> consumer;
    private DataTransferJob job;

    @BeforeEach
    void setUp() throws Exception {
        settings = Mockito.mock(DataTransferSettings.class);
        DataTransferNodeDescriptor consumerNode = Mockito.mock(DataTransferNodeDescriptor.class);
        Mockito.when(consumerNode.getName()).thenReturn("test consumer node");
        Mockito.when(settings.getConsumer()).thenReturn(consumerNode);

        task = Mockito.mock(DBTTask.class);
        log = Mockito.mock(Log.class);

        producer = Mockito.mock(IDataTransferProducer.class);
        consumer = Mockito.mock(IDataTransferConsumer.class);
        Mockito.when(producer.getObjectFullName(ArgumentMatchers.any())).thenReturn("test-producer");
        Mockito.when(consumer.getObjectFullName(ArgumentMatchers.any())).thenReturn("test-consumer");

        job = new DataTransferJob(settings, task, log, null, null, 0);
    }

    @Test
    void returnsCanceledStatusBeforeAcquiringPipe() {
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(monitor.isCanceled()).thenReturn(true);

        Assertions.assertEquals(IStatus.CANCEL, job.run(monitor).getSeverity());

        Mockito.verify(settings, Mockito.never()).acquireDataPipe(Mockito.any(), Mockito.any());
    }

    @Test
    void returnsCanceledStatusWhenCancellationOccursWhileAcquiringPipe() {
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(monitor.isCanceled()).thenReturn(false, true);

        Assertions.assertEquals(IStatus.CANCEL, job.run(monitor).getSeverity());
    }

    @Test
    void returnsCanceledStatusWhenDriverThrowsAfterWorkerCancellation() throws Exception {
        prepareDataPipe();
        DBException error = new DBException("Query canceled", new SQLException("canceling statement due to user request", "57014"));
        Mockito.doAnswer(invocation -> {
            job.canceling();
            throw error;
        }).when(producer).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());

        IStatus result = job.run(Mockito.mock(DBRProgressMonitor.class));

        Assertions.assertTrue(job.isCanceled());
        Assertions.assertEquals(IStatus.CANCEL, result.getSeverity());
        Mockito.verify(producer).close();
    }

    @Test
    void returnsCanceledStatusWhenDriverThrowsAfterMonitorCancellation() throws Exception {
        prepareDataPipe();
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.doAnswer(invocation -> {
            Mockito.when(monitor.isCanceled()).thenReturn(true);
            throw new DBException("Query canceled");
        }).when(producer).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());

        Assertions.assertEquals(IStatus.CANCEL, job.run(monitor).getSeverity());
        Mockito.verify(producer).close();
    }

    @Test
    void preservesDriverErrorWhenTransferIsNotCanceled() throws Exception {
        prepareDataPipe();
        DBException error = new DBException("Query failed");
        Mockito.doThrow(error).when(producer).transferData(
            Mockito.any(), Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.eq(task), Mockito.anyLong());

        IStatus result = job.run(Mockito.mock(DBRProgressMonitor.class));

        Assertions.assertEquals(IStatus.OK, result.getSeverity());
        Assertions.assertSame(error, result.getException());
        Mockito.verify(producer).close();
    }

    @Test
    void closesProducerAfterSuccessfulTransfer() throws Exception {
        DataTransferPipe pipe = new DataTransferPipe(producer, consumer);

        job.transferData(new VoidProgressMonitor(), pipe);

        Mockito.verify(producer, Mockito.times(1)).close();
    }

    @Test
    void closesProducerEvenWhenTransferFails() throws Exception {
        DataTransferPipe pipe = new DataTransferPipe(producer, consumer);
        DBException transferError = new DBException("boom");
        Mockito.doThrow(transferError).when(producer).transferData(
            ArgumentMatchers.any(), ArgumentMatchers.eq(consumer), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.eq(task), ArgumentMatchers.anyLong());

        DBException thrown = Assertions.assertThrows(
            DBException.class, () -> job.transferData(new VoidProgressMonitor(), pipe));

        Assertions.assertSame(transferError, thrown);
        Mockito.verify(producer, Mockito.times(1)).close();
    }

    @Test
    void closesProducerWhenConsumerIsMissing() throws Exception {
        DataTransferPipe pipe = new DataTransferPipe(producer, null);

        Assertions.assertThrows(
            DBException.class, () -> job.transferData(new VoidProgressMonitor(), pipe));

        Mockito.verify(producer, Mockito.times(1)).close();
    }

    @Test
    void closeFailureIsLoggedNotThrown() throws Exception {
        DataTransferPipe pipe = new DataTransferPipe(producer, consumer);
        DBException closeError = new DBException("close failed");
        Mockito.doThrow(closeError).when(producer).close();

        Assertions.assertDoesNotThrow(() -> job.transferData(new VoidProgressMonitor(), pipe));

        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(log).error(messageCaptor.capture(), ArgumentMatchers.eq(closeError));
        Assertions.assertTrue(messageCaptor.getValue().toString().contains("test-producer"));
    }

    @Test
    void closeFailureDoesNotMaskTransferFailure() throws Exception {
        DataTransferPipe pipe = new DataTransferPipe(producer, consumer);
        DBException transferError = new DBException("transfer failed");
        DBException closeError = new DBException("close also failed");
        Mockito.doThrow(transferError).when(producer).transferData(
            ArgumentMatchers.any(), ArgumentMatchers.eq(consumer), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.eq(task), ArgumentMatchers.anyLong());
        Mockito.doThrow(closeError).when(producer).close();

        DBException thrown = Assertions.assertThrows(
            DBException.class, () -> job.transferData(new VoidProgressMonitor(), pipe));

        Assertions.assertSame(transferError, thrown);
        Mockito.verify(log).error(ArgumentMatchers.any(), ArgumentMatchers.eq(closeError));
    }

    @Test
    void closesProducerWhenProcessorConstructionFails() throws Exception {
        DataTransferPipe pipe = new DataTransferPipe(producer, consumer);
        DataTransferProcessorDescriptor processorDescriptor = Mockito.mock(DataTransferProcessorDescriptor.class);
        Mockito.when(processorDescriptor.getInstance()).thenThrow(new IllegalStateException("Can't instantiate data exporter"));
        Mockito.when(settings.getProcessor()).thenReturn(processorDescriptor);

        Assertions.assertThrows(
            IllegalStateException.class, () -> job.transferData(new VoidProgressMonitor(), pipe));

        Mockito.verify(producer, Mockito.never()).transferData(
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        Mockito.verify(producer, Mockito.times(1)).close();
    }

    private void prepareDataPipe() {
        DataTransferPipe pipe = new DataTransferPipe(producer, consumer);
        Mockito.when(settings.getDataPipes()).thenReturn(List.of(pipe));
        Mockito.when(settings.acquireDataPipe(Mockito.any(), Mockito.eq(task))).thenReturn(pipe, (DataTransferPipe) null);
    }
}
