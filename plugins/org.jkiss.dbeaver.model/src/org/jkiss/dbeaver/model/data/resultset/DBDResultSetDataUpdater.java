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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.data.messages.DataMessages;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class DBDResultSetDataUpdater<T extends DBDDataStatementInfo, R extends DBDValueRow, M extends DBDResultSetModel> {
    private static final Log log = Log.getLog(DBDResultSetDataUpdater.class);

    private final DBCExecutionContext executionContext;
    private final List<DBEPersistAction> actions;
    protected boolean autocommit;
    protected DBCSavepoint savepoint;
    protected final M model;
    protected final List<R> deletedRows = new ArrayList<>();
    protected final List<R> addedRows = new ArrayList<>();
    protected final List<R> changedRows = new ArrayList<>();
    protected final List<DBDValue> clonedValues = new ArrayList<>();
    protected final Map<R, Map<DBDRowIdentifier, List<DBDAttributeBinding>>> rowIdentifiers = new LinkedHashMap<>();
    protected final List<T> updateStatements;
    protected final List<T> insertStatements;
    protected final List<T> deleteStatements;
    protected final DBCStatistics updateStats = new DBCStatistics();
    protected final DBCStatistics insertStats = new DBCStatistics();
    protected final DBCStatistics deleteStats = new DBCStatistics();

    public DBDResultSetDataUpdater(@NotNull M model, @Nullable DBCExecutionContext executionContext) {
        this.model = model;
        this.executionContext = executionContext;
        this.actions = new ArrayList<>();
        this.updateStatements = new ArrayList<>();
        this.insertStatements = new ArrayList<>();
        this.deleteStatements = new ArrayList<>();
        collectChanges();
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

    @NotNull
    public List<DBEPersistAction> getActions() {
        return actions;
    }

    public void prepareStatements(@NotNull DBRProgressMonitor monitor, @NotNull ResultSetSaveSettings settings) throws DBException {
        if (hasDeletes()) {
            prepareDeleteStatements(monitor, settings.isDeleteCascade(), settings.isDeepCascade());
        }
        if (hasInserts()) {
            prepareInsertStatements(monitor);
        }
        prepareUpdateStatements(monitor);
    }


    @NotNull
    protected abstract T getDataStatementInfo(
        @NotNull DBSManipulationType type,
        @NotNull R row,
        @NotNull DBSEntity entity
    );

    protected void prepareUpdateStatements(@NotNull DBRProgressMonitor monitor) throws DBException {
        for (var rowEntry : rowIdentifiers.entrySet()) {
            R row = rowEntry.getKey();
            loadFinalRowValues(row);
            Map<DBDAttributeBinding, Object> changes = collectUpdateChanges(row);

            for (var identifierEntry : rowEntry.getValue().entrySet()) {
                DBDRowIdentifier rowIdentifier = identifierEntry.getKey();
                List<DBDAttributeBinding> changedAttrsForTable = identifierEntry.getValue();

                DBSEntity table = rowIdentifier.getEntity();
                T statement = getDataStatementInfo(DBSManipulationType.UPDATE, row, table);

                for (DBDAttributeBinding changedAttr : changedAttrsForTable) {
                    if (!isVirtualColumn(changedAttr)) {
                        statement.getUpdateAttributes().add(new DBDAttributeValue(changedAttr, model.getCellValue(changedAttr, row)));
                    }
                }

                List<DBDAttributeBinding> idColumns = rowIdentifier.getAttributes();
                for (DBDAttributeBinding metaColumn : idColumns) {
                    Object keyValue = model.getCellValue(metaColumn, row);
                    if (changes != null && changes.containsKey(metaColumn)) {
                        keyValue = changes.get(metaColumn);
                        if (keyValue instanceof DBDContent) {
                            if (keyValue instanceof DBDValueCloneable vc) {
                                keyValue = vc.cloneValue(monitor);
                                if (keyValue instanceof DBDContent copiedContext) {
                                    clonedValues.add(copiedContext);
                                    copiedContext.resetContents();
                                }
                            } else {
                                throw new DBCException("Column '" + metaColumn.getFullyQualifiedName(DBPEvaluationContext.UI)
                                    + "' can't be used as a key. Value clone is not supported.");
                            }
                        }
                    }
                    statement.getKeyAttributes().add(new DBDAttributeValue(metaColumn, keyValue));
                }
                updateStatements.add(statement);
            }
        }
    }

    @Nullable
    protected abstract Map<DBDAttributeBinding, Object> collectUpdateChanges(@NotNull R row);

    protected void prepareInsertStatements(@NotNull DBRProgressMonitor monitor) throws DBException {
        // Make insert statements
        final DBSEntity table = model.getSingleSource();
        if (table == null) {
            throw new DBCException("Internal error: can't get single entity metadata, insert is not possible");
        }
        for (R row : addedRows) {
            loadFinalRowValues(row);
            T statement = getDataStatementInfo(DBSManipulationType.INSERT, row, table);
            DBDAttributeBinding docAttr = model.getDocumentAttribute();
            if (docAttr != null) {
                statement.getKeyAttributes().add(new DBDAttributeValue(docAttr, model.getCellValue(docAttr, row)));
            } else {
                for (DBDAttributeBinding column : model.getAttributes()) {
                    if (!isVirtualColumn(column)) {
                        Object value = model.getCellValue(column, row);
                        if (value != null) {
                            statement.getKeyAttributes().add(new DBDAttributeValue(column, value));
                        }
                    }
                }
            }
            insertStatements.add(statement);
        }
    }

    protected void loadFinalRowValues(@NotNull R row) throws DBException {

    }

    protected void prepareDeleteStatements(
        @NotNull DBRProgressMonitor monitor,
        boolean deleteCascade,
        boolean deepCascade
    ) throws DBException {
        // Make delete statements
        DBDRowIdentifier rowIdentifier = model.getDefaultRowIdentifier();
        if (rowIdentifier == null) {
            throw new DBCException("Internal error: can't find entity identifier, delete is not possible");
        }
        DBSDataManipulator dataManipulator = getDataManipulator(rowIdentifier.getEntity());
        boolean supportsRI = dataManipulator.getDataSource().getInfo().supportsReferentialIntegrity();

        for (R row : deletedRows) {
            loadFinalRowValues(row);
            T statement = getDataStatementInfo(DBSManipulationType.DELETE, row, rowIdentifier.getEntity());
            List<DBDAttributeBinding> keyColumns = rowIdentifier.getAttributes();
            for (DBDAttributeBinding binding : keyColumns) {
                statement.getKeyAttributes().add(
                    new DBDAttributeValue(
                        binding,
                        model.getCellValue(binding, row)
                    ));
            }
            deleteStatements.add(statement);
        }

        if (supportsRI && deleteCascade) {
            try {
                List<T> cascadeStats = prepareDeleteCascade(monitor, rowIdentifier, deleteStatements, deepCascade);
                deleteStatements.clear();
                deleteStatements.addAll(cascadeStats);
            } catch (DBException e) {
                log.debug(e);
            }
        }
    }

    @NotNull
    protected List<T> prepareDeleteCascade(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBDRowIdentifier rowIdentifier,
        @NotNull List<T> statements,
        boolean deepCascade
    ) throws DBException {
        return List.of();
    }

    public boolean execute(
        @Nullable DBRProgressMonitor monitor,
        boolean generateScript,
        @NotNull ResultSetSaveSettings settings,
        @Nullable DBDDataUpdateListener listener
    ) throws DBException {
        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            throw new DBCException("No execution context");
        }
        DataUpdaterJob job = new DataUpdaterJob(this, generateScript, settings, listener, executionContext);
        if (monitor == null) {
            job.schedule();
            return true;
        } else {
            job.run(monitor);
            return job.getError() == null;
        }
    }

    @Nullable
    public Throwable executeStatements(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Map<String, Object> options,
        @Nullable ISmartTransactionManager stm,
        boolean generateScript
    ) {
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
                    error[0] = executeStatements(session, options, generateScript);
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

    public abstract void processReflectChanges(@Nullable Throwable error);

    public abstract void showError(@NotNull Throwable error);

    public abstract void before(@NotNull DataUpdaterJob job);

    public abstract void after();

    @Nullable
    protected abstract ISmartTransactionManager getSmartTransactionManager();

    @Nullable
    private Throwable executeStatements(@NotNull DBCSession session, @NotNull Map<String, Object> options, boolean generateScript) {
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
                        extractDataAndProcessBatch(session, options, statement, batch, attributes, deleteStats, generateScript);
                    }
                    processStatementChanges(statement);
                } catch (DBException e) {
                    processStatementError(statement, session, generateScript);
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
                    processStatementError(statement, session, generateScript);
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
                        extractDataAndProcessBatch(session, options, statement, batch, attributes, updateStats, generateScript);
                    }
                    processStatementChanges(statement);
                } catch (DBException e) {
                    processStatementError(statement, session, generateScript);
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
        @NotNull DBCStatistics stats,
        boolean generateScript
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

    private void processStatementError(@NotNull DBDDataStatementInfo statement, @NotNull DBCSession session, boolean generateScript) {
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

    @Nullable
    protected DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    private Map<String, Object> getOptions() {
        return Map.of();
    }

    public boolean hasInserts() {
        return !addedRows.isEmpty();
    }

    public boolean hasDeletes() {
        return !deletedRows.isEmpty();
    }

    public boolean hasUpdates() {
        return !changedRows.isEmpty();
    }

    @NotNull
    private DBSDataManipulator getDataManipulator(@NotNull DBSEntity entity) throws DBCException {
        if (entity instanceof DBSDataManipulator dm) {
            return dm;
        } else {
            throw new DBCException("Entity " + entity.getName() + " doesn't support data manipulation");
        }
    }

    private boolean isVirtualColumn(@Nullable DBDAttributeBinding column) {
        return column instanceof DBDAttributeBindingCustom;
    }

    protected void collectChanges() {
        collectUpdatedRows();

        // Prepare rows
        for (R row : changedRows) {
            Map<DBDAttributeBinding, Object> changes = collectUpdateChanges(row);
            if (changes == null) {
                continue;
            }
            Map<DBDRowIdentifier, List<DBDAttributeBinding>> identifierGroups = new LinkedHashMap<>();
            for (DBDAttributeBinding changedAttr : changes.keySet()) {
                DBDRowIdentifier rowIdentifier = changedAttr.getRowIdentifier();
                if (rowIdentifier != null) {
                    identifierGroups.computeIfAbsent(rowIdentifier, k -> new ArrayList<>()).add(changedAttr);
                }
            }
            if (!identifierGroups.isEmpty()) {
                rowIdentifiers.put(row, identifierGroups);
            }
        }
    }

    protected abstract void collectUpdatedRows();

    protected abstract void notifyContainer(@NotNull DBCStatistics statistics);

    @NotNull
    protected abstract DBCExecutionSource createExecutionSource(@NotNull DBSDataManipulator dataContainer);

    @Nullable
    protected abstract DBDDataReceiver getKeyReceiver(@NotNull DBDDataStatementInfo statement);


}
