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

package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Data container transfer producer
 */
public class DatabaseTransferProducer implements IDataTransferProducer<DatabaseProducerSettings> {

    private static final Log log = Log.getLog(DatabaseTransferProducer.class);

    @NotNull
    private DBSDataContainer dataContainer;
    @Nullable
    private DBDDataFilter dataFilter;

    public DatabaseTransferProducer() {
    }

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
    public DBSDataContainer getDatabaseObject()
    {
        return dataContainer;
    }

    @Override
    public String getObjectName() {
        return DBUtils.getObjectFullName(dataContainer, DBPEvaluationContext.DML);
    }

    @Override
    public String getObjectContainerName() {
        DBPDataSourceContainer container = getDataSourceContainer();
        return container != null ? container.getName() : "?";
    }

    @Override
    public Color getObjectColor() {
        DBPDataSourceContainer container = getDataSourceContainer();
        return container != null ? UIUtils.getConnectionColor(container.getConnectionConfiguration()) : null;
    }

    private DBPDataSourceContainer getDataSourceContainer() {
        if (dataContainer != null) {
            return dataContainer.getDataSource().getContainer();
        }
        return null;
    }

    @Override
    public void transferData(
        DBRProgressMonitor monitor,
        IDataTransferConsumer consumer,
        IDataTransferProcessor processor,
        DatabaseProducerSettings settings)
        throws DBException {
        String contextTask = DTMessages.data_transfer_wizard_job_task_export;

        DBPDataSource dataSource = getDatabaseObject().getDataSource();
        assert (dataSource != null);

        boolean selectiveExportFromUI = settings.isSelectedColumnsOnly() || settings.isSelectedRowsOnly();

        long readFlags = DBSDataContainer.FLAG_NONE;
        if (settings.isSelectedColumnsOnly()) {
            readFlags |= DBSDataContainer.FLAG_USE_SELECTED_COLUMNS;
        }
        if (settings.isSelectedRowsOnly()) {
            readFlags |= DBSDataContainer.FLAG_USE_SELECTED_ROWS;
        }

        boolean newConnection = settings.isOpenNewConnections() && !getDatabaseObject().getDataSource().getContainer().getDriver().isEmbedded();
        boolean forceDataReadTransactions = Boolean.TRUE.equals(dataSource.getDataSourceFeature(DBConstants.FEATURE_LOB_REQUIRE_TRANSACTIONS));
        DBCExecutionContext context = !selectiveExportFromUI && newConnection ?
            DBUtils.getObjectOwnerInstance(getDatabaseObject()).openIsolatedContext(monitor, "Data transfer producer") :
            DBUtils.getDefaultContext(getDatabaseObject(), false);
        try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, contextTask)) {
            try {
                AbstractExecutionSource transferSource = new AbstractExecutionSource(dataContainer, context, consumer);
                session.enableLogging(false);
                if (!selectiveExportFromUI && (newConnection || forceDataReadTransactions)) {
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
                    monitor.beginTask(DTMessages.data_transfer_wizard_job_task_retrieve, 1);
                    try {
                        totalRows = dataContainer.countData(transferSource, session, dataFilter, readFlags);
                    } catch (Throwable e) {
                        log.warn("Can't retrieve row count from '" + dataContainer.getName() + "'", e);
                        try {
                            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
                            if (txnManager != null && !txnManager.isAutoCommit()) {
                                txnManager.rollback(session, null);
                            }
                        } catch (Throwable e1) {
                            log.warn("Error rolling back transaction", e1);
                        }
                    } finally {
                        monitor.done();
                    }
                }

                monitor.beginTask(DTMessages.data_transfer_wizard_job_task_export_table_data, (int) totalRows);

                try {
                    // Perform export
                    if (settings.getExtractType() == DatabaseProducerSettings.ExtractType.SINGLE_QUERY) {
                        // Just do it in single query
                        dataContainer.readData(transferSource, session, consumer, dataFilter, -1, -1, readFlags);
                    } else {
                        // Read all data by segments
                        long offset = 0;
                        int segmentSize = settings.getSegmentSize();
                        for (; ; ) {
                            DBCStatistics statistics = dataContainer.readData(
                                transferSource, session, consumer, dataFilter, offset, segmentSize, readFlags);
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

            } finally {
                if (!selectiveExportFromUI && (newConnection || forceDataReadTransactions)) {
                    DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
                    if (txnManager != null) {
                        try {
                            txnManager.commit(session);
                        } catch (DBCException e) {
                            log.error("Can't finish transaction in data producer connection", e);
                        }
                    }
                }
                if (!selectiveExportFromUI && newConnection) {
                    context.close();
                }
            }
        }
    }

}
