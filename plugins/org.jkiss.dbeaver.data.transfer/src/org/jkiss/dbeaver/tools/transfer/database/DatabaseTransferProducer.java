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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.meta.DBSerializable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNodePrimary;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data container transfer producer
 */
@DBSerializable("databaseTransferProducer")
public class DatabaseTransferProducer implements IDataTransferProducer<DatabaseProducerSettings>, IDataTransferNodePrimary {

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
        return dataContainer == null ? null : DBUtils.getObjectFullName(dataContainer, DBPEvaluationContext.DML);
    }

    @Override
    public DBPImage getObjectIcon() {
        if (dataContainer instanceof DBPImageProvider) {
            return DBValueFormatting.getObjectImage(dataContainer);
        }
        return DBIcon.TREE_TABLE;
    }

    @Override
    public String getObjectContainerName() {
        DBPDataSourceContainer container = getDataSourceContainer();
        return container != null ? container.getName() : "?";
    }

    @Override
    public DBPImage getObjectContainerIcon() {
        DBPDataSourceContainer container = getDataSourceContainer();
        return container != null ? container.getDriver().getIcon() : null;
    }

    private DBPDataSourceContainer getDataSourceContainer() {
        if (dataContainer != null) {
            return dataContainer.getDataSource().getContainer();
        }
        return null;
    }

    @Override
    public void transferData(
        @NotNull DBRProgressMonitor monitor1,
        @NotNull IDataTransferConsumer consumer,
        @Nullable IDataTransferProcessor processor,
        @NotNull DatabaseProducerSettings settings, DBTTask task)
        throws DBException {
        String contextTask = DTMessages.data_transfer_wizard_job_task_export;

        DBSDataContainer databaseObject = getDatabaseObject();
        if (databaseObject == null) {
            throw new DBException("No input database object found");
        }
        DBPDataSource dataSource = databaseObject.getDataSource();
        assert (dataSource != null);

        DBExecUtils.tryExecuteRecover(monitor1, dataSource, monitor -> {
            long readFlags = DBSDataContainer.FLAG_NONE;
            if (settings.isSelectedColumnsOnly()) {
                readFlags |= DBSDataContainer.FLAG_USE_SELECTED_COLUMNS;
            }
            if (settings.isSelectedRowsOnly()) {
                readFlags |= DBSDataContainer.FLAG_USE_SELECTED_ROWS;
            }

            boolean newConnection = settings.isOpenNewConnections() && !getDatabaseObject().getDataSource().getContainer().getDriver().isEmbedded();
            boolean forceDataReadTransactions = Boolean.TRUE.equals(dataSource.getDataSourceFeature(DBConstants.FEATURE_LOB_REQUIRE_TRANSACTIONS));
            boolean selectiveExportFromUI = settings.isSelectedColumnsOnly() || settings.isSelectedRowsOnly();

            try {
                DBCExecutionContext context;
                if (dataContainer instanceof DBPContextProvider) {
                    context = ((DBPContextProvider) dataContainer).getExecutionContext();
                } else {
                    context = DBUtils.getDefaultContext(dataContainer, false);
                }
                if (context == null) {
                    throw new DBCException("Can't retrieve execution context from data container " + dataContainer);
                }
                if (!selectiveExportFromUI && newConnection) {
                    context = DBUtils.getObjectOwnerInstance(getDatabaseObject()).openIsolatedContext(monitor, "Data transfer producer", context);
                }
                if (task != null) {
                    DBTUtils.initFromContext(monitor, task, context);
                }

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
                            monitor.subTask("Read data");

                            // Perform export
                            if (settings.getExtractType() == DatabaseProducerSettings.ExtractType.SINGLE_QUERY) {
                                // Just do it in single query
                                dataContainer.readData(transferSource, session, consumer, dataFilter, -1, -1, readFlags, settings.getFetchSize());
                            } else {
                                // Read all data by segments
                                long offset = 0;
                                int segmentSize = settings.getSegmentSize();
                                for (; ; ) {
                                    DBCStatistics statistics = dataContainer.readData(
                                        transferSource, session, consumer, dataFilter, offset, segmentSize, readFlags, settings.getFetchSize());
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
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        });
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DatabaseTransferProducer &&
            CommonUtils.equalObjects(dataContainer, ((DatabaseTransferProducer) obj).dataContainer) &&
            CommonUtils.equalObjects(dataFilter, ((DatabaseTransferProducer) obj).dataFilter);
    }

    public static class ObjectSerializer implements DBPObjectSerializer<DBTTask, DatabaseTransferProducer> {

        @Override
        public void serializeObject(DBRRunnableContext runnableContext, DBTTask context, DatabaseTransferProducer object, Map<String, Object> state) {
            DBSDataContainer dataContainer = object.dataContainer;
            if (dataContainer instanceof IAdaptable) {
                DBSDataContainer nestedDataContainer = ((IAdaptable) dataContainer).getAdapter(DBSDataContainer.class);
                if (nestedDataContainer != null) {
                    dataContainer = nestedDataContainer;
                }
            }
            if (dataContainer instanceof DBSEntity) {
                state.put("type", "entity");
                if (dataContainer.getDataSource() != null) {
                    state.put("project", dataContainer.getDataSource().getContainer().getProject().getName());
                }
                state.put("entityId", DBUtils.getObjectFullId(dataContainer));
            } else if (dataContainer instanceof SQLQueryContainer) {
                state.put("type", "query");
                SQLQueryContainer queryContainer = (SQLQueryContainer) dataContainer;
                DBPDataSourceContainer dataSource = queryContainer.getDataSourceContainer();
                if (dataSource != null) {
                    state.put("project", dataSource.getProject().getName());
                    state.put("dataSource", dataSource.getId());
                }
                SQLScriptElement query = queryContainer.getQuery();
                state.put("query", query.getOriginalText());
            } else {
                state.put("type", "unknown");
                log.error("Unsupported producer data container: " + dataContainer);
            }
            if (object.dataFilter != null) {
                Map<String, Object> dataFilterState = new LinkedHashMap<>();
                object.dataFilter.serialize(dataFilterState);
                state.put("dataFilter", dataFilterState);
            }
        }

        @Override
        public DatabaseTransferProducer deserializeObject(DBRRunnableContext runnableContext, DBTTask objectContext, Map<String, Object> state) throws DBCException {
            DatabaseTransferProducer producer = new DatabaseTransferProducer();
            try {
                runnableContext.run(true, true, monitor -> {
                    try {
                        String selType = CommonUtils.toString(state.get("type"));
                        String projectName = CommonUtils.toString(state.get("project"));
                        DBPProject project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
                        if (project == null) {
                            project = objectContext.getProject();
                        }
                        switch (selType) {
                            case "entity": {
                                String id = CommonUtils.toString(state.get("entityId"));
                                producer.dataContainer = (DBSDataContainer) DBUtils.findObjectById(monitor, project, id);
                                break;
                            }
                            case "query": {
                                String dsId = CommonUtils.toString(state.get("dataSource"));
                                String queryText = CommonUtils.toString(state.get("query"));
                                DBPDataSourceContainer ds = project.getDataSourceRegistry().getDataSource(dsId);
                                if (ds == null) {
                                    log.debug("Can't find datasource "+ dsId);
                                    return;
                                }
                                if (!ds.isConnected()) {
                                    ds.connect(monitor, true, true);
                                }
                                SQLQuery query = new SQLQuery(ds.getDataSource(), queryText);
                                SQLScriptContext scriptContext = new SQLScriptContext(null,
                                    new DataSourceContextProvider(ds), null, new PrintWriter(System.err, true), null);
                                scriptContext.setVariables(DBTUtils.getVariables(objectContext));
                                producer.dataContainer = new SQLQueryDataContainer(new DataSourceContextProvider(ds), query, scriptContext, log);
                                break;
                            }
                            default:
                                log.warn("Unsupported selector type: " + selType);
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                throw new DBCException("Error instantiating data producer", e.getTargetException());
            } catch (InterruptedException e) {
                throw new DBCException("Deserialization canceled", e);
            }

            return producer;
        }
    }

}
