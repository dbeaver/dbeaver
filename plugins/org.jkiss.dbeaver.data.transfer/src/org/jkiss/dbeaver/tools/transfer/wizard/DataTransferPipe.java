/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;

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

}
