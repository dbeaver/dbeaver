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

package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;

/**
 * Data container transfer producer
 */
public class DatabaseTransferProducer implements IDataTransferProducer<DatabaseProducerSettings> {

    private static final Log log = Log.getLog(DatabaseTransferProducer.class);

    @NotNull
    private DBSDataContainer dataContainer;
    @Nullable
    private DBDDataFilter dataFilter;

    public DatabaseTransferProducer(@NotNull DBSDataContainer dataContainer)
    {
        this.dataContainer = dataContainer;
    }

    public DatabaseTransferProducer(@NotNull DBSDataContainer dataContainer, @Nullable DBDDataFilter dataFilter)
    {
        this.dataContainer = dataContainer;
        this.dataFilter = dataFilter;
    }

    @Override
    public DBSDataContainer getSourceObject()
    {
        return dataContainer;
    }

    @Override
    public void transferData(
        DBRProgressMonitor monitor,
        IDataTransferConsumer consumer,
        DatabaseProducerSettings settings)
        throws DBException {
        String contextTask = CoreMessages.data_transfer_wizard_job_task_export;
        DBPDataSource dataSource = getSourceObject().getDataSource();
        assert (dataSource != null);
        boolean newConnection = settings.isOpenNewConnections();
        DBCExecutionContext context = newConnection ?
            dataSource.openIsolatedContext(monitor, "Data transfer producer") : dataSource.getDefaultContext(false);
        try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, contextTask)) {
            try {
                AbstractExecutionSource transferSource = new AbstractExecutionSource(dataContainer, context, consumer);
                session.enableLogging(false);
                if (newConnection) {
                    // Turn off auto-commit in source DB
                    // Auto-commit has to be turned off because some drivers allows to read LOBs and
                    // other complex structures only in transactional mode
                    try {
                        DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
                        if (txnManager != null) {
                            txnManager.setAutoCommit(monitor, false);
                        }
                    } catch (DBCException e) {
                        log.warn("Can't change auto-commit", e);
                    }

                }
                long totalRows = 0;
                if (settings.isQueryRowCount() && (dataContainer.getSupportedFeatures() & DBSDataContainer.DATA_COUNT) != 0) {
                    monitor.beginTask(CoreMessages.data_transfer_wizard_job_task_retrieve, 1);
                    try {
                        totalRows = dataContainer.countData(transferSource, session, dataFilter);
                    } catch (Throwable e) {
                        log.warn("Can't retrieve row count from '" + dataContainer.getName() + "'", e);
                    } finally {
                        monitor.done();
                    }
                }

                monitor.beginTask(CoreMessages.data_transfer_wizard_job_task_export_table_data, (int) totalRows);

                try {
                    // Perform export
                    if (settings.getExtractType() == DatabaseProducerSettings.ExtractType.SINGLE_QUERY) {
                        // Just do it in single query
                        dataContainer.readData(transferSource, session, consumer, dataFilter, -1, -1, DBSDataContainer.FLAG_NONE);
                    } else {
                        // Read all data by segments
                        long offset = 0;
                        int segmentSize = settings.getSegmentSize();
                        for (; ; ) {
                            DBCStatistics statistics = dataContainer.readData(
                                transferSource, session, consumer, dataFilter, offset, segmentSize, DBSDataContainer.FLAG_NONE);
                            if (statistics == null || statistics.getRowsFetched() < segmentSize) {
                                // Done
                                break;
                            }
                            offset += statistics.getRowsFetched();
                        }
                    }
                } finally {
                    monitor.done();
                }

                //dataContainer.readData(context, consumer, dataFilter, -1, -1);
            } finally {
                if (newConnection) {
                    DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
                    if (txnManager != null) {
                        try {
                            txnManager.commit(session);
                        } catch (DBCException e) {
                            log.error("Can't finish transaction in data producer connection", e);
                        }
                    }
                }
                if (newConnection) {
                    context.close();
                }
            }
        }
    }

}
