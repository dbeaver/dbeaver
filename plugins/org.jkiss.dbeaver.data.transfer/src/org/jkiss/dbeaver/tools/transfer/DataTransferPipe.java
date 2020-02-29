/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;

import java.util.Date;

/**
 * Data transfer pipe is tuple of produces and consumer
 */
public class DataTransferPipe {

    private IDataTransferProducer producer;
    private IDataTransferConsumer consumer;

    public DataTransferPipe(IDataTransferProducer producer, IDataTransferConsumer consumer)
    {
        this.producer = producer;
        this.consumer = consumer;
    }

    public IDataTransferProducer getProducer()
    {
        return producer;
    }

    public void setProducer(IDataTransferProducer producer)
    {
        this.producer = producer;
    }

    public IDataTransferConsumer getConsumer()
    {
        return consumer;
    }

    public void setConsumer(IDataTransferConsumer consumer)
    {
        this.consumer = consumer;
    }

    public void initPipe(DataTransferSettings settings, int pipeIndex, int totalPipes) throws DBException {
        if (consumer == null || producer == null) {
            throw new DBException("Empty pipe");
        }
        IDataTransferSettings consumerSettings = settings.getNodeSettings(settings.getConsumer());
        DataTransferProcessorDescriptor processorDescriptor = settings.getProcessor();
        IDataTransferProcessor processor = processorDescriptor == null ? null : processorDescriptor.getInstance();

        IDataTransferConsumer.TransferParameters parameters = new IDataTransferConsumer.TransferParameters(
            processorDescriptor != null && processorDescriptor.isBinaryFormat(),
            processorDescriptor != null && processorDescriptor.isHTMLFormat());
        parameters.orderNumber = pipeIndex;
        parameters.totalConsumers = totalPipes;
        parameters.startTimestamp = new Date();
        consumer.initTransfer(
            producer.getDatabaseObject(),
            consumerSettings,
            parameters,
            processor,
            processor == null ?
                null :
                settings.getProcessorProperties());

    }
}
