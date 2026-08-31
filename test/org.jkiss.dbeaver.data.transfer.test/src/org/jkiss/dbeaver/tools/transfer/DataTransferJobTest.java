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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class DataTransferJobTest {

    private DBTTask task;
    private Log log;
    private IDataTransferProducer producer;
    private IDataTransferConsumer consumer;
    private DataTransferJob job;

    @BeforeEach
    void setUp() throws Exception {
        DataTransferSettings settings = Mockito.mock(DataTransferSettings.class);
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
}
