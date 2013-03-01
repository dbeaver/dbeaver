/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

/**
 * Data container transfer producer
 */
public class ContainerTransferProducer implements IDataTransferProducer {

    private DBSDataContainer dataContainer;
    private DBDDataFilter dataFilter;

    public ContainerTransferProducer(DBSDataContainer dataContainer)
    {
        this.dataContainer = dataContainer;
    }

    public ContainerTransferProducer(DBSDataContainer dataContainer, DBDDataFilter dataFilter)
    {
        this.dataContainer = dataContainer;
        this.dataFilter = dataFilter;
    }

    public DBSDataContainer getDataContainer()
    {
        return dataContainer;
    }

    public DBDDataFilter getDataFilter()
    {
        return dataFilter;
    }

    @Override
    public DBSDataContainer getSourceObject()
    {
        return dataContainer;
    }

    @Override
    public void transferData(
        DBCExecutionContext context,
        IDataTransferConsumer consumer,
        IDataTransferSettings settings)
        throws DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();
        long totalRows = 0;
        if (settings.isQueryRowCount() && (dataContainer.getSupportedFeatures() & DBSDataContainer.DATA_COUNT) != 0) {
            monitor.beginTask(CoreMessages.dialog_export_wizard_job_task_retrieve, 1);
            try {
                totalRows = dataContainer.countData(context, dataFilter);
            } finally {
                monitor.done();
            }
        }

        monitor.beginTask(CoreMessages.dialog_export_wizard_job_task_export_table_data, (int)totalRows);

        try {
            // Perform export
            if (settings.getExtractType() == IDataTransferSettings.ExtractType.SINGLE_QUERY) {
                // Just do it in single query
                dataContainer.readData(context, consumer, dataFilter, -1, -1);
            } else {
                // Read all data by segments
                long offset = 0;
                int segmentSize = settings.getSegmentSize();
                for (;;) {
                    long rowCount = dataContainer.readData(
                        context, consumer, dataFilter, offset, segmentSize);
                    if (rowCount < segmentSize) {
                        // Done
                        break;
                    }
                    offset += rowCount;
                }
            }
        } finally {
            monitor.done();
        }

        //dataContainer.readData(context, consumer, dataFilter, -1, -1);
    }
}
