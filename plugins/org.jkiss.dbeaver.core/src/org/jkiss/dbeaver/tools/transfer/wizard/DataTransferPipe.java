/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
