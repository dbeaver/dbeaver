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
package org.jkiss.dbeaver.model.data.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDNull;
import org.jkiss.dbeaver.model.data.messages.DataMessages;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public abstract class DBDResultSetDataUpdater {
    private static final Log log = Log.getLog(DBDResultSetDataUpdater.class);

    private final DBCExecutionContext executionContext;
    private final List<DBEPersistAction> actions;
    private final boolean generateScript;
    private boolean autocommit;
    private DBCSavepoint savepoint;
    private final List<? extends DBDDataStatementInfo> updateStatements;
    private final List<? extends DBDDataStatementInfo> insertStatements;
    private final List<? extends DBDDataStatementInfo> deleteStatements;
    private final Map<String, Object> options;
    protected final DBCStatistics updateStats = new DBCStatistics();
    protected final DBCStatistics insertStats = new DBCStatistics();
    protected final DBCStatistics deleteStats = new DBCStatistics();

    public DBDResultSetDataUpdater(
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull List<? extends DBDDataStatementInfo> updateStatements,
        @NotNull List<? extends DBDDataStatementInfo> insertStatements,
        @NotNull List<? extends DBDDataStatementInfo> deleteStatements,
        @NotNull Map<String, Object> options,
        boolean generateScript
    ) {
        this.executionContext = executionContext;
        this.actions = actions;
        this.updateStatements = updateStatements;
        this.insertStatements = insertStatements;
        this.deleteStatements = deleteStatements;
        this.generateScript = generateScript;
        this.options = options;
    }

    @NotNull
    public DBCStatistics getUpdateStats() {
        return updateStats;
    }

    @NotNull
    public DBCStatistics getInsertStats() {
        return insertStats;
    }

    @NotNull
    public DBCStatistics getDeleteStats() {
        return deleteStats;
    }

    @Nullable
    public Throwable executeStatements(@NotNull DBRProgressMonitor monitor) {
        return executeStatements(monitor, null);
    }

    @Nullable
    public Throwable executeStatements(@NotNull DBRProgressMonitor monitor, @Nullable ISmartTransactionManager stm) {
        monitor.beginTask(
            DataMessages.controls_resultset_viewer_monitor_aply_changes,
            deleteStatements.size()
                + insertStatements.size()
                + updateStatements.size() + 1
        );

        try (
            DBCSession session = getExecutionContext().openSession(
                monitor,
                DBCExecutionPurpose.USER,
                DataMessages.controls_resultset_viewer_job_update
            )
        ) {
            if (!generateScript) {
                if (stm != null && stm.isSmartAutoCommit()) {
                    DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
                    if (txnManager != null && txnManager.isSupportsTransactions() && txnManager.isAutoCommit()) {
                        monitor.subTask("Disable auto-commit mode");
                        txnManager.setAutoCommit(monitor, false);
                    }
                }
            }

            Throwable[] error = new Throwable[1];
            DBExecUtils.tryExecuteRecover(
                monitor, session.getDataSource(), param -> {
                    error[0] = executeStatements(session, options);
                    if (error[0] != null) {
                        throw new InvocationTargetException(error[0]);
                    }
                }
            );
            return error[0];

        } catch (DBException e) {
            return e;
        } finally {
            monitor.done();
        }
    }

    @Nullable
    private Throwable executeStatements(@NotNull DBCSession session, @NotNull Map<String, Object> options) {
        DBRProgressMonitor monitor = session.getProgressMonitor();
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(getExecutionContext());
        if (!generateScript && txnManager != null) {
            monitor.subTask(DataMessages.controls_resultset_check_autocommit_state);
            try {
                this.autocommit = txnManager.isAutoCommit();
            } catch (DBCException e) {
                log.warn("Can't determine autocommit state", e);
                this.autocommit = true;
            }
        }
        monitor.worked(1);
        if (!generateScript && txnManager != null) {
            if (!this.autocommit && txnManager.supportsSavepoints()) {
                try {
                    this.savepoint = txnManager.setSavepoint(monitor, null);
                } catch (Throwable e) {
                    // May be savepoints not supported
                    log.debug("Can't set savepoint", e);
                }
            }
        }
        try {
            for (DBDDataStatementInfo statement : deleteStatements) {
                if (monitor.isCanceled()) {
                    break;
                }
                try {
                    DBSDataManipulator dataContainer = getDataManipulator(statement.getEntity());
                    try (
                        DBSDataManipulator.ExecuteBatch batch = dataContainer.deleteData(
                            session,
                            DBDAttributeValue.getAttributes(statement.getKeyAttributes()),
                            createExecutionSource(dataContainer)
                        )
                    ) {
                        Object[] attributes = new Object[statement.getKeyAttributes().size()];
                        extractDataAndProcessBatch(session, options, statement, batch, attributes, deleteStats);
                    }
                    processStatementChanges(statement);
                } catch (DBException e) {
                    processStatementError(statement, session);
                    return e;
                }
                monitor.worked(1);
            }
            for (DBDDataStatementInfo statement : insertStatements) {
                if (monitor.isCanceled()) {
                    break;
                }
                try {
                    DBSDataManipulator dataContainer = getDataManipulator(statement.getEntity());
                    try (
                        DBSDataManipulator.ExecuteBatch batch = dataContainer.insertData(
                            session,
                            DBDAttributeValue.getAttributes(statement.getKeyAttributes()),
                            statement.needKeys() ? getKeyReceiver(statement) : null,
                            createExecutionSource(dataContainer),
                            options
                        )
                    ) {
                        batch.add(DBDAttributeValue.getValues(statement.getKeyAttributes()));
                        if (generateScript) {
                            batch.generatePersistActions(session, actions, options);
                        } else {
                            DBCStatistics bs = batch.execute(session, options);
                            // Notify rsv container about statement execute
                            this.notifyContainer(bs);

                            insertStats.accumulate(bs);
                        }
                    }
                    processStatementChanges(statement);
                } catch (DBException e) {
                    processStatementError(statement, session);
                    return e;
                }
                monitor.worked(1);
            }
            for (DBDDataStatementInfo statement : updateStatements) {
                if (monitor.isCanceled()) {
                    break;
                }
                try {
                    DBSDataManipulator dataContainer = getDataManipulator(statement.getEntity());
                    try (
                        DBSDataManipulator.ExecuteBatch batch = dataContainer.updateData(
                            session,
                            DBDAttributeValue.getAttributes(statement.getUpdateAttributes()),
                            DBDAttributeValue.getAttributes(statement.getKeyAttributes()),
                            null,
                            createExecutionSource(dataContainer)
                        )
                    ) {
                        // Make single array of values
                        Object[] attributes = new Object[statement.getUpdateAttributes().size() + statement.getKeyAttributes().size()];
                        for (int i = 0; i < statement.getUpdateAttributes().size(); i++) {
                            attributes[i] = statement.getUpdateAttributes().get(i).getValue();
                        }
                        extractDataAndProcessBatch(session, options, statement, batch, attributes, updateStats);
                    }
                    processStatementChanges(statement);
                } catch (DBException e) {
                    processStatementError(statement, session);
                    return e;
                }
                monitor.worked(1);
            }

            return null;
        } finally {
            if (!generateScript && txnManager != null && this.savepoint != null) {
                try {
                    txnManager.releaseSavepoint(monitor, this.savepoint);
                } catch (Throwable e) {
                    // Maybe savepoints not supported
                    log.debug("Can't release savepoint", e);
                }
            }
        }
    }

    public boolean isAutoCommitEnabled() {
        return autocommit;
    }

    private void extractDataAndProcessBatch(
        @NotNull DBCSession session,
        @NotNull Map<String, Object> options,
        @NotNull DBDDataStatementInfo statement,
        @NotNull DBSDataManipulator.ExecuteBatch batch,
        @NotNull Object[] attributes,
        @NotNull DBCStatistics stats
    ) throws DBException {
        for (int i = 0; i < statement.getKeyAttributes().size(); i++) {
            if (DBUtils.isNullValue(statement.getKeyAttributes().get(i).getValue())) {
                attributes[statement.getUpdateAttributes().size() + i] = DBDNull.INSTANCE;
            } else {
                attributes[statement.getUpdateAttributes().size() + i] = statement.getKeyAttributes().get(i).getValue();
            }
        }
        batch.add(attributes);
        if (generateScript) {
            batch.generatePersistActions(session, actions, options);
        } else {
            DBCStatistics bs = batch.execute(session, options);
            // Notify rsv container about statement execute
            this.notifyContainer(bs);

            stats.accumulate(bs);
        }
    }

    private void processStatementChanges(@NotNull DBDDataStatementInfo statement) {
        statement.setExecuted(true);
    }

    private void processStatementError(@NotNull DBDDataStatementInfo statement, @NotNull DBCSession session) {
        statement.setExecuted(false);
        if (!generateScript) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(getExecutionContext());
            if (txnManager != null) {
                try {
                    if (!txnManager.isAutoCommit()) {
                        txnManager.rollback(session, savepoint);
                    }
                } catch (Throwable e) {
                    log.debug("Error during transaction rollback", e);
                }
            }
        }
    }

    @NotNull
    protected DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    private Map<String, Object> getOptions() {
        return Map.of();
    }

    @NotNull
    private DBSDataManipulator getDataManipulator(@NotNull DBSEntity entity) throws DBCException {
        if (entity instanceof DBSDataManipulator dm) {
            return dm;
        } else {
            throw new DBCException("Entity " + entity.getName() + " doesn't support data manipulation");
        }
    }

    protected abstract void notifyContainer(@NotNull DBCStatistics statistics);

    @NotNull
    protected abstract DBCExecutionSource createExecutionSource(@NotNull DBSDataManipulator dataContainer);

    @Nullable
    protected abstract DBDDataReceiver getKeyReceiver(@NotNull DBDDataStatementInfo statement);


}
