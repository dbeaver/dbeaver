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
