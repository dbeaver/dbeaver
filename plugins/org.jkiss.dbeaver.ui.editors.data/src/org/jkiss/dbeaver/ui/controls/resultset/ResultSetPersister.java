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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.UIConfirmation;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Result set data updater
 */
class ResultSetPersister {

    private static final Log log = Log.getLog(ResultSetPersister.class);

    /**
     * Data update listener
     */
    interface DataUpdateListener {

        void onUpdate(boolean success);

    }

    class ExecutionSource implements DBCExecutionSource {

        private final DBSDataContainer dataContainer;

        ExecutionSource(DBSDataContainer dataContainer) {
            this.dataContainer = dataContainer;
        }

        @Nullable
        @Override
        public DBSDataContainer getDataContainer() {
            return dataContainer;
        }

        @NotNull
        @Override
        public Object getExecutionController() {
            return viewer;
        }

        @Nullable
        @Override
        public Object getSourceDescriptor() {
            return ResultSetPersister.this;
        }

        @Nullable
        @Override
        public DBCScriptContext getScriptContext() {
            return null;
        }
    }

    @NotNull
    private final ResultSetViewer viewer;
    @NotNull
    private final ResultSetModel model;
    @NotNull
    private final DBDAttributeBinding[] columns;

    private final List<ResultSetRow> deletedRows = new ArrayList<>();
    private final List<ResultSetRow> addedRows = new ArrayList<>();
    private final List<ResultSetRow> changedRows = new ArrayList<>();
    private final Map<ResultSetRow, DBDRowIdentifier> rowIdentifiers = new LinkedHashMap<>();
    private final List<DataStatementInfo> insertStatements = new ArrayList<>();
    private final List<DataStatementInfo> deleteStatements = new ArrayList<>();
    private final List<DataStatementInfo> updateStatements = new ArrayList<>();

    private final List<DBEPersistAction> script = new ArrayList<>();

    ResultSetPersister(@NotNull ResultSetViewer viewer) {
        this.viewer = viewer;
        this.model = viewer.getModel();
        this.columns = model.getAttributes();

        collectChanges();
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

    public List<DBDAttributeBinding> getUpdatedAttributes() {
        Set<DBDAttributeBinding> attrs = new LinkedHashSet<>();
        for (ResultSetRow row : changedRows) {
            attrs.addAll(row.changes.keySet());
        }
        return new ArrayList<>(attrs);
    }

    /**
     * Applies changes.
     *
     * @param monitor  progress monitor
     * @param settings
     * @param listener value listener
     */
    boolean applyChanges(@Nullable DBRProgressMonitor monitor, boolean generateScript, ResultSetSaveSettings settings, @Nullable DataUpdateListener listener)
        throws DBException
    {
        if (monitor == null) {
            try {
                UIUtils.runInProgressService(monitor1 -> {
                    try {
                        prepareStatements(monitor1, settings);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                throw new DBException("Error preparing update statements", e.getTargetException());
            } catch (InterruptedException e) {
                return false;
            }
        } else {
            prepareStatements(monitor, settings);
        }

        return execute(monitor, generateScript, settings, listener);
    }

    private void prepareStatements(@NotNull DBRProgressMonitor monitor, ResultSetSaveSettings settings) throws DBException {
        if (hasDeletes()) {
            prepareDeleteStatements(monitor, settings.isDeleteCascade(), settings.isDeepCascade());
        }
        if (hasInserts()) {
            prepareInsertStatements(monitor);
        }
        prepareUpdateStatements(monitor);
    }

    boolean refreshInsertedRows() throws DBCException {
        if (!viewer.getModel().isSingleSource()) {
            return false;
        }
        List<ResultSetRow> refreshRows = new ArrayList<>();
        refreshRows.addAll(addedRows);
        refreshRows.addAll(changedRows);
        if (refreshRows.isEmpty()) {
            // Nothing to refresh
            return false;
        }
        final DBDRowIdentifier rowIdentifier = model.getDefaultRowIdentifier();
        if (rowIdentifier == null || rowIdentifier.getAttributes().isEmpty()) {
            // No key - can't refresh
            return false;
        }

        DBCExecutionContext executionContext = viewer.getContainer().getExecutionContext();
        if (executionContext == null) {
            throw new DBCException("No execution context");
        }

        RowRefreshJob job = new RowRefreshJob(executionContext, viewer.getDataContainer(), rowIdentifier, refreshRows);
        job.schedule();
        return true;
    }

    public ResultSetSaveReport generateReport() {
        ResultSetSaveReport report = new ResultSetSaveReport();
        report.setDeletes(deletedRows.size());
        report.setInserts(addedRows.size());
        int changedRows = 0;
        for (ResultSetRow row : this.rowIdentifiers.keySet()) {
            if (row.changes != null) changedRows++;
        }
        report.setUpdates(changedRows);

        DBPDataSource dataSource = viewer.getDataSource();
        report.setHasReferences(dataSource != null && dataSource.getInfo().supportsReferentialIntegrity());

        return report;
    }

    public List<DBEPersistAction> getScript() {
        return script;
    }

    private void collectChanges() {
        deletedRows.clear();
        addedRows.clear();
        changedRows.clear();
        for (ResultSetRow row : model.getAllRows()) {
            switch (row.getState()) {
                case ResultSetRow.STATE_NORMAL:
                    if (row.isChanged()) {
                        changedRows.add(row);
                    }
                    break;
                case ResultSetRow.STATE_ADDED:
                    addedRows.add(row);
                    break;
                case ResultSetRow.STATE_REMOVED:
                    deletedRows.add(row);
                    break;
            }
        }

        // Prepare rows
        for (ResultSetRow row : changedRows) {
            if (row.changes == null || row.changes.isEmpty()) {
                continue;
            }
            DBDAttributeBinding changedAttr = row.changes.keySet().iterator().next();
            rowIdentifiers.put(row, changedAttr.getRowIdentifier());
        }
    }

    private void prepareDeleteStatements(@NotNull DBRProgressMonitor monitor, boolean deleteCascade, boolean deepCascade)
        throws DBException {
        // Make delete statements
        DBDRowIdentifier rowIdentifier = model.getDefaultRowIdentifier();
        if (rowIdentifier == null) {
            throw new DBCException("Internal error: can't find entity identifier, delete is not possible");
        }
        DBSDataManipulator dataManipulator = getDataManipulator(rowIdentifier.getEntity());
        boolean supportsRI = dataManipulator.getDataSource().getInfo().supportsReferentialIntegrity();

        for (ResultSetRow row : deletedRows) {
            DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.DELETE, row, rowIdentifier.getEntity());
            List<DBDAttributeBinding> keyColumns = rowIdentifier.getAttributes();
            for (DBDAttributeBinding binding : keyColumns) {
                statement.keyAttributes.add(
                    new DBDAttributeValue(
                        binding,
                        model.getCellValue(binding, row)));
            }
            deleteStatements.add(statement);
        }

        if (supportsRI && deleteCascade) {
            try {
                List<DataStatementInfo> cascadeStats = prepareDeleteCascade(monitor, rowIdentifier, deleteStatements, deepCascade);
                deleteStatements.clear();
                deleteStatements.addAll(cascadeStats);
            } catch (DBException e) {
                log.debug(e);
            }
        }
    }

    private List<DataStatementInfo> prepareDeleteCascade(@NotNull DBRProgressMonitor monitor, DBDRowIdentifier rowIdentifier, List<DataStatementInfo> statements, boolean deepCascade) throws DBException {
        List<DataStatementInfo> result = new ArrayList<>();

        DBSEntity entity = rowIdentifier.getEntity();
        Collection<? extends DBSEntityAssociation> references = entity.getReferences(monitor);
        if (references != null) {
            // Now iterate over all statements and make cascade delete for each
            for (DataStatementInfo stat : statements) {

                List<DataStatementInfo> cascadeStats = new ArrayList<>();

                for (DBSEntityAssociation ref : references) {
                    if (ref instanceof DBSTableForeignKey && ((DBSTableForeignKey) ref).getDeleteRule() == DBSForeignKeyModifyRule.CASCADE) {
                        // It is already delete cascade - just ignore it
                        continue;
                    }
                    DBSEntity refEntity = ref.getParentObject();
                    if (ref instanceof DBSEntityReferrer) {
                        List<? extends DBSEntityAttributeRef> attrRefs = ((DBSEntityReferrer) ref).getAttributeReferences(monitor);
                        if (attrRefs != null) {

                            List<DBDAttributeValue> refKeyValues = new ArrayList<>();

                            for (DBSEntityAttributeRef attrRef : attrRefs) {
                                DBSEntityAttribute attribute = attrRef.getAttribute();
                                if (attribute != null) {
                                    DBDAttributeValue value = DBDAttributeValue.getAttributeValue(stat.keyAttributes, attribute);
                                    if (value == null) {
                                        log.debug("Can't find attribute value for '" + attribute.getName() + "' recursive delete");
                                    } else {
                                        refKeyValues.add(value);
                                    }
                                }
                            }

                            if (refKeyValues.size() > 0) {
                                // We have a key. Let's delete
                                DataStatementInfo cascadeStat = new DataStatementInfo(DBSManipulationType.DELETE, stat.row, refEntity);
                                cascadeStat.keyAttributes.addAll(refKeyValues);
                                cascadeStats.add(cascadeStat);
/*
                                System.out.println("DELETE! " + entity.getName());
                                for (DBDAttributeValue kv : refKeyValues) {
                                    System.out.println("\tATTR: " + DBUtils.getObjectFullName(kv.getAttribute(), DBPEvaluationContext.UI) + "=" + kv.getValue());
                                }
*/
                            }
                        }
                    }
                }
                result.addAll(cascadeStats);
                result.add(stat);
            }
        }
        return result;
    }

    private void prepareInsertStatements(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        // Make insert statements
        final DBSEntity table = viewer.getModel().getSingleSource();
        if (table == null) {
            throw new DBCException("Internal error: can't get single entity metadata, insert is not possible");
        }
        for (ResultSetRow row : addedRows) {
            DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.INSERT, row, table);
            DBDAttributeBinding docAttr = model.getDocumentAttribute();
            if (docAttr != null) {
                statement.keyAttributes.add(new DBDAttributeValue(docAttr, model.getCellValue(docAttr, row)));
            } else {
                for (int i = 0; i < columns.length; i++) {
                    DBDAttributeBinding column = columns[i];
                    statement.keyAttributes.add(new DBDAttributeValue(column, model.getCellValue(column, row)));
                }
            }
            insertStatements.add(statement);
        }
    }

    private void prepareUpdateStatements(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        // Make statements
        for (ResultSetRow row : this.rowIdentifiers.keySet()) {
            if (row.changes == null) continue;

            DBDRowIdentifier rowIdentifier = this.rowIdentifiers.get(row);
            DBSEntity table = rowIdentifier.getEntity();
            {
                DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.UPDATE, row, table);
                // Updated columns
                for (DBDAttributeBinding changedAttr : row.changes.keySet()) {
                    statement.updateAttributes.add(
                        new DBDAttributeValue(
                            changedAttr,
                            model.getCellValue(changedAttr, row)));
                }
                // Key columns
                List<DBDAttributeBinding> idColumns = rowIdentifier.getAttributes();
                for (DBDAttributeBinding metaColumn : idColumns) {
                    Object keyValue = model.getCellValue(metaColumn, row);
                    // Try to find old key oldValue
                    if (row.changes != null && row.changes.containsKey(metaColumn)) {
                        keyValue = row.changes.get(metaColumn);
                        if (keyValue instanceof DBDContent) {
                            if (keyValue instanceof DBDValueCloneable) {
                                keyValue = ((DBDValueCloneable) keyValue).cloneValue(monitor);
                                ((DBDContent) keyValue).resetContents();
                            } else {
                                throw new DBCException("Column '" + metaColumn.getFullyQualifiedName(DBPEvaluationContext.UI) + "' can't be used as a key. Value clone is not supported.");
                            }
                        }
                    }
                    statement.keyAttributes.add(new DBDAttributeValue(metaColumn, keyValue));
                }
                updateStatements.add(statement);
            }
        }
    }

    private boolean execute(@Nullable DBRProgressMonitor monitor, boolean generateScript, @NotNull ResultSetSaveSettings settings, @Nullable final DataUpdateListener listener)
        throws DBException {
        DBCExecutionContext executionContext = viewer.getContainer().getExecutionContext();
        if (executionContext == null) {
            throw new DBCException("No execution context");
        }
        DataUpdaterJob job = new DataUpdaterJob(generateScript, settings, listener, executionContext);
        if (monitor == null) {
            job.schedule();
            return true;
        } else {
            job.run(monitor);
            return job.getError() == null;
        }
    }

    public void rejectChanges() {
        collectChanges();
        for (ResultSetRow row : changedRows) {
            if (row.changes != null) {
                for (Map.Entry<DBDAttributeBinding, Object> changedValue : row.changes.entrySet()) {
                    Object curValue = model.getCellValue(changedValue.getKey(), row);
                    // If new value and old value are the same - do not release it
                    if (curValue != changedValue.getValue()) {
                        DBUtils.releaseValue(curValue);
                        model.updateCellValue(changedValue.getKey(), row, changedValue.getValue(), false);
                    }
                }
                row.changes = null;
            }
        }

        boolean rowsChanged = model.cleanupRows(addedRows);
        // Remove deleted rows
        for (ResultSetRow row : deletedRows) {
            row.setState(ResultSetRow.STATE_NORMAL);
        }
        model.refreshChangeCount();

        viewer.redrawData(false, rowsChanged);
        viewer.fireResultSetChange();
        viewer.updateEditControls();
        viewer.updatePanelsContent(false);
        viewer.getActivePresentation().updateValueView();
    }

    // Reflect data changes in viewer
    // Changes affects only rows which statements executed successfully
    private boolean reflectChanges() {
        boolean rowsChanged = false;
        for (ResultSetRow row : changedRows) {
            for (DataStatementInfo stat : updateStatements) {
                if (stat.executed && stat.row == row) {
                    reflectKeysUpdate(stat);
                    row.changes = null;
                    break;
                }
            }
        }
        for (ResultSetRow row : addedRows) {
            for (DataStatementInfo stat : insertStatements) {
                if (stat.executed && stat.row == row) {
                    reflectKeysUpdate(stat);
                    row.setState(ResultSetRow.STATE_NORMAL);
                    break;
                }
            }
        }
        for (ResultSetRow row : deletedRows) {
            for (DataStatementInfo stat : deleteStatements) {
                if (stat.executed && stat.row == row) {
                    model.cleanupRow(row);
                    rowsChanged = true;
                    break;
                }
            }
        }
        model.refreshChangeCount();
        return rowsChanged;
    }

    private void reflectKeysUpdate(DataStatementInfo stat) {
        // Update keys
        if (!stat.updatedCells.isEmpty()) {
            for (Map.Entry<Integer, Object> entry : stat.updatedCells.entrySet()) {
                ResultSetRow row = stat.row;
                DBUtils.releaseValue(row.values[entry.getKey()]);
                row.values[entry.getKey()] = entry.getValue();
            }
        }
    }

    @NotNull
    private DBSDataManipulator getDataManipulator(DBSEntity entity) throws DBCException {
        if (entity instanceof DBSDataManipulator) {
            return (DBSDataManipulator) entity;
        } else {
            throw new DBCException("Entity " + entity.getName() + " doesn't support data manipulation");
        }
    }

    void checkEntityIdentifiers() throws DBException
    {

        final DBCExecutionContext executionContext = viewer.getExecutionContext();
        if (executionContext == null) {
            throw new DBCException("Can't persist data - not connected to database");
        }

        boolean needsSingleEntity = this.hasInserts() || this.hasDeletes();

        DBSEntity entity = model.getSingleSource();
        if (needsSingleEntity) {
            if (entity == null) {
                throw new DBCException("Can't detect source entity");
            }
        }

        if (entity != null) {
            // Check for value locators
            // Probably we have only virtual one with empty attribute set
            DBDRowIdentifier identifier = viewer.getVirtualEntityIdentifier();
            if (identifier != null) {
                if (CommonUtils.isEmpty(identifier.getAttributes())) {
                    // Empty identifier. We have to define it
                    if (!UIConfirmation.run(() -> ValidateUniqueKeyUsageDialog.validateUniqueKey(viewer, executionContext))) {
                        throw new DBCException("No unique key defined");
                    }
                }
            }
        }

        List<DBDAttributeBinding> updatedAttributes = this.getUpdatedAttributes();
        if (this.hasDeletes()) {
            DBDRowIdentifier defIdentifier = model.getDefaultRowIdentifier();
            if (defIdentifier == null) {
                throw new DBCException("No unique row identifier is result set. Cannot proceed with row(s) delete.");
            } else if (!defIdentifier.isValidIdentifier()) {
                throw new DBCException("Attributes of unique key '" + DBUtils.getObjectFullName(defIdentifier.getUniqueKey(), DBPEvaluationContext.UI) + "' are missing in result set. Cannot proceed with row(s) delete.");
            }
        }

        {
            for (DBDAttributeBinding attr : updatedAttributes) {
                // Check attributes of non-virtual identifier
                DBDRowIdentifier rowIdentifier = attr.getRowIdentifier();
                if (rowIdentifier == null) {
                    // We shouldn't be here ever!
                    // Virtual id should be created if we missing natural one
                    throw new DBCException("Attribute " + attr.getName() + " was changed but it hasn't associated unique key");
                } else if (!rowIdentifier.isValidIdentifier()) {
                    throw new DBCException(
                        "Can't update attribute '" + attr.getName() +
                            "' - attributes of key '" + DBUtils.getObjectFullName(rowIdentifier.getUniqueKey(), DBPEvaluationContext.UI) + "' are missing in result set");
                }
            }
        }
    }

    private class DataUpdaterJob extends DataSourceJob {
        private final boolean generateScript;
        private final ResultSetSaveSettings settings;
        private final DataUpdateListener listener;
        private boolean autocommit;
        private DBCStatistics updateStats, insertStats, deleteStats;
        private DBCSavepoint savepoint;
        private Throwable error;

        DataUpdaterJob(boolean generateScript, @NotNull ResultSetSaveSettings settings, @Nullable DataUpdateListener listener, @NotNull DBCExecutionContext executionContext) {
            super(ResultSetMessages.controls_resultset_viewer_job_update, executionContext);
            this.generateScript = generateScript;
            this.settings = settings;
            this.listener = listener;
        }

        void notifyContainer(DBCExecutionResult result) {
            if (viewer.getContainer() instanceof IResultSetExecuteListener) {
                ((IResultSetExecuteListener) viewer.getContainer()).handleExecuteResult(result);
            }
        }

        public Throwable getError() {
            return error;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            model.setUpdateInProgress(true);
            updateStats = new DBCStatistics();
            insertStats = new DBCStatistics();
            deleteStats = new DBCStatistics();
            try {
                error = executeStatements(monitor);
            } finally {
                model.setUpdateInProgress(false);
            }

            if (!generateScript) {
                // Reflect changes
                UIUtils.syncExec(() -> {
                    boolean rowsChanged = false;
                    if (DataUpdaterJob.this.autocommit || error == null) {
                        rowsChanged = reflectChanges();
                    }
                    if (!viewer.getControl().isDisposed()) {
                        //releaseStatements();
                        viewer.redrawData(false, rowsChanged);
                        viewer.updateEditControls();
                        if (error == null) {
                            viewer.setStatus(
                                NLS.bind(
                                    ResultSetMessages.controls_resultset_viewer_status_inserted_,
                                    new Object[]{
                                        ResultSetUtils.formatRowCount(DataUpdaterJob.this.insertStats.getRowsUpdated()),
                                        ResultSetUtils.formatRowCount(DataUpdaterJob.this.deleteStats.getRowsUpdated()),
                                        ResultSetUtils.formatRowCount(DataUpdaterJob.this.updateStats.getRowsUpdated())}));
                        } else {
                            DBWorkbench.getPlatformUI().showError("Data error", "Error synchronizing data with database", error);
                            viewer.setStatus(GeneralUtils.getFirstMessage(error), DBPMessageType.ERROR);
                        }
                    }
                    viewer.fireResultSetChange();
                });
                if (this.listener != null) {
                    this.listener.onUpdate(error == null);
                }
            } else if (error != null) {
                DBWorkbench.getPlatformUI().showError("Data error", "Error generating script", error);
            }

            return Status.OK_STATUS;
        }

        private Throwable executeStatements(DBRProgressMonitor monitor) {
            monitor.beginTask(
                ResultSetMessages.controls_resultset_viewer_monitor_aply_changes,
                ResultSetPersister.this.deleteStatements.size() + ResultSetPersister.this.insertStatements.size() + ResultSetPersister.this.updateStatements.size() + 1);

            try (DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.USER, ResultSetMessages.controls_resultset_viewer_job_update)) {

                if (!generateScript) {
                    IResultSetContainer container = viewer.getContainer();
                    if (container instanceof ISmartTransactionManager) {
                        if (((ISmartTransactionManager) container).isSmartAutoCommit()) {
                            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
                            if (txnManager != null && txnManager.isAutoCommit()) {
                                monitor.subTask("Disable auto-commit mode");
                                txnManager.setAutoCommit(monitor, false);
                            }
                        }
                    }
                }

                Throwable[] error = new Throwable[1];
                DBExecUtils.tryExecuteRecover(monitor, session.getDataSource(), param -> {
                    error[0] = executeStatements(session);
                });
                return error[0];

            } catch (DBException e) {
                return e;
            } finally {
                monitor.done();
            }
        }

        private Throwable executeStatements(DBCSession session) {
            Map<String, Object> options = new LinkedHashMap<>();
            options.put(DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, settings.isUseFullyQualifiedNames());

            DBRProgressMonitor monitor = session.getProgressMonitor();
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(getExecutionContext());
            if (!generateScript && txnManager != null) {
                monitor.subTask(ResultSetMessages.controls_resultset_check_autocommit_state);
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
                for (DataStatementInfo statement : ResultSetPersister.this.deleteStatements) {
                    if (monitor.isCanceled()) break;
                    try {
                        DBSDataManipulator dataContainer = getDataManipulator(statement.entity);
                        try (DBSDataManipulator.ExecuteBatch batch = dataContainer.deleteData(
                            session,
                            DBDAttributeValue.getAttributes(statement.keyAttributes),
                            new ExecutionSource(dataContainer))) {
                            batch.add(DBDAttributeValue.getValues(statement.keyAttributes));
                            if (generateScript) {
                                batch.generatePersistActions(session, script, options);
                            } else {
                                DBCStatistics bs = batch.execute(session);
                                // Notify rsv container about statement execute
                                this.notifyContainer(bs);

                                deleteStats.accumulate(bs);
                            }
                        }
                        processStatementChanges(statement);
                    } catch (DBException e) {
                        processStatementError(statement, session);
                        return e;
                    }
                    monitor.worked(1);
                }
                for (DataStatementInfo statement : ResultSetPersister.this.insertStatements) {
                    if (monitor.isCanceled()) break;
                    try {
                        DBSDataManipulator dataContainer = getDataManipulator(statement.entity);
                        try (DBSDataManipulator.ExecuteBatch batch = dataContainer.insertData(
                            session,
                            DBDAttributeValue.getAttributes(statement.keyAttributes),
                            statement.needKeys() ? new KeyDataReceiver(statement) : null,
                            new ExecutionSource(dataContainer))) {
                            batch.add(DBDAttributeValue.getValues(statement.keyAttributes));
                            if (generateScript) {
                                batch.generatePersistActions(session, script, options);
                            } else {
                                DBCStatistics bs = batch.execute(session);
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
                for (DataStatementInfo statement : ResultSetPersister.this.updateStatements) {
                    if (monitor.isCanceled()) break;
                    try {
                        DBSDataManipulator dataContainer = getDataManipulator(statement.entity);
                        try (DBSDataManipulator.ExecuteBatch batch = dataContainer.updateData(
                            session,
                            DBDAttributeValue.getAttributes(statement.updateAttributes),
                            DBDAttributeValue.getAttributes(statement.keyAttributes),
                            null,
                            new ExecutionSource(dataContainer))) {
                            // Make single array of values
                            Object[] attributes = new Object[statement.updateAttributes.size() + statement.keyAttributes.size()];
                            for (int i = 0; i < statement.updateAttributes.size(); i++) {
                                attributes[i] = statement.updateAttributes.get(i).getValue();
                            }
                            for (int i = 0; i < statement.keyAttributes.size(); i++) {
                                attributes[statement.updateAttributes.size() + i] = statement.keyAttributes.get(i).getValue();
                            }
                            // Execute
                            batch.add(attributes);
                            if (generateScript) {
                                batch.generatePersistActions(session, script, options);
                            } else {
                                DBCStatistics bs = batch.execute(session);
                                // Notify rsv container about statement execute
                                this.notifyContainer(bs);

                                updateStats.accumulate(bs);
                            }
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

        private void processStatementChanges(DataStatementInfo statement) {
            statement.executed = true;
        }

        private void processStatementError(DataStatementInfo statement, DBCSession session) {
            statement.executed = false;
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

    }

    /**
     * Key data receiver
     */
    class KeyDataReceiver implements DBDDataReceiver {
        DataStatementInfo statement;

        KeyDataReceiver(DataStatementInfo statement) {
            this.statement = statement;
        }

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) {

        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet)
            throws DBCException {
            DBCResultSetMetaData rsMeta = resultSet.getMeta();
            List<DBCAttributeMetaData> keyAttributes = rsMeta.getAttributes();
            for (int i = 0; i < keyAttributes.size(); i++) {
                DBCAttributeMetaData keyAttribute = keyAttributes.get(i);
                DBDValueHandler valueHandler = DBUtils.findValueHandler(session, keyAttribute);
                Object keyValue = valueHandler.fetchValueObject(session, resultSet, keyAttribute, i);
                if (keyValue == null) {
                    // [MSSQL] Sometimes driver returns empty list of generated keys if
                    // table has auto-increment columns and user performs simple row update
                    // Just ignore such empty keys. We can't do anything with them anyway
                    continue;
                }
                boolean updated = false;
                if (!CommonUtils.isEmpty(keyAttribute.getName())) {
                    DBDAttributeBinding binding = model.getAttributeBinding(statement.entity, keyAttribute.getName());
                    if (binding != null) {
                        // Got it. Just update column oldValue
                        statement.updatedCells.put(binding.getOrdinalPosition(), keyValue);
                        //curRows.get(statement.row.row)[colIndex] = keyValue;
                        updated = true;
                    }
                }
                if (!updated) {
                    // Key not found
                    // Try to find and update auto-increment column
                    for (int k = 0; k < columns.length; k++) {
                        DBDAttributeBinding column = columns[k];
                        if (column.isAutoGenerated()) {
                            // Got it
                            statement.updatedCells.put(k, keyValue);
                            //curRows.get(statement.row.row)[k] = keyValue;
                            updated = true;
                            break;
                        }
                    }
                }

                if (!updated) {
                    // Auto-generated key not found
                    // Just skip it..
                    log.debug("Can't find target column for auto-generated key '" + keyAttribute.getName() + "'");
                }
            }
        }

        @Override
        public void fetchEnd(DBCSession session, DBCResultSet resultSet) {

        }

        @Override
        public void close() {
        }
    }

    /**
     * Data statement
     */
    static class DataStatementInfo {
        @NotNull
        final DBSManipulationType type;
        @NotNull
        final ResultSetRow row;
        @NotNull
        final DBSEntity entity;
        final List<DBDAttributeValue> keyAttributes = new ArrayList<>();
        final List<DBDAttributeValue> updateAttributes = new ArrayList<>();
        boolean executed = false;
        final Map<Integer, Object> updatedCells = new HashMap<>();

        DataStatementInfo(@NotNull DBSManipulationType type, @NotNull ResultSetRow row, @NotNull DBSEntity entity) {
            this.type = type;
            this.row = row;
            this.entity = entity;
        }

        boolean needKeys() {
            for (DBDAttributeValue col : keyAttributes) {
                if (col.getAttribute().isAutoGenerated() && DBUtils.isNullValue(col.getValue())) {
                    return true;
                }
            }
            return false;
        }
    }

    class RowDataReceiver implements DBDDataReceiver {
        private final DBDAttributeBinding[] curAttributes;
        private Object[] rowValues;

        RowDataReceiver(DBDAttributeBinding[] curAttributes) {
            this.curAttributes = curAttributes;
        }

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) {

        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet)
            throws DBCException {
            DBCResultSetMetaData rsMeta = resultSet.getMeta();
            // Compare attributes with existing model attributes
            List<DBCAttributeMetaData> attributes = rsMeta.getAttributes();
            if (attributes.size() != curAttributes.length) {
                log.debug("Wrong meta attributes count (" + attributes.size() + " <> " + curAttributes.length + ") - can't refresh");
                return;
            }
            for (int i = 0; i < curAttributes.length; i++) {
                DBCAttributeMetaData metaAttribute = curAttributes[i].getMetaAttribute();
                if (metaAttribute == null ||
                    !CommonUtils.equalObjects(metaAttribute.getName(), attributes.get(i).getName())) {
                    log.debug("Attribute '" + metaAttribute + "' doesn't match '" + attributes.get(i).getName() + "'");
                    return;
                }
            }

            rowValues = new Object[curAttributes.length];
            for (int i = 0; i < curAttributes.length; i++) {
                final DBDAttributeBinding attr = curAttributes[i];
                DBDValueHandler valueHandler = attr.getValueHandler();
                Object attrValue = valueHandler.fetchValueObject(session, resultSet, attr, i);
                rowValues[i] = attrValue;
            }

        }

        @Override
        public void fetchEnd(DBCSession session, DBCResultSet resultSet) {

        }

        @Override
        public void close() {
        }
    }

    private class RowRefreshJob extends DataSourceJob {

        private DBSDataContainer dataContainer;
        private DBDRowIdentifier rowIdentifier;
        private List<ResultSetRow> rows;

        RowRefreshJob(DBCExecutionContext context, DBSDataContainer dataContainer, DBDRowIdentifier rowIdentifier, List<ResultSetRow> rows) {
            super("Refresh rows", context);
            this.dataContainer = dataContainer;
            this.rowIdentifier = rowIdentifier;
            this.rows = new ArrayList<>(rows);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                final Object[][] refreshValues = new Object[rows.size()][];

                final DBDAttributeBinding[] curAttributes = viewer.getModel().getAttributes();
                final AbstractExecutionSource executionSource = new AbstractExecutionSource(dataContainer, getExecutionContext(), this);
                List<DBDAttributeBinding> idAttributes = rowIdentifier.getAttributes();
                if (idAttributes.isEmpty()) {
                    return Status.OK_STATUS;
                }
                try (DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, "Refresh row(s) after insert/update")) {
                    for (int i = 0; i < rows.size(); i++) {
                        ResultSetRow row = rows.get(i);
                        List<DBDAttributeConstraint> constraints = new ArrayList<>();
                        boolean hasKey = true;
                        for (DBDAttributeBinding keyAttr : idAttributes) {
                            final Object keyValue = viewer.getModel().getCellValue(keyAttr, row);
                            if (DBUtils.isNullValue(keyValue)) {
                                hasKey = false;
                                break;
                            }
                            final DBDAttributeConstraint constraint = new DBDAttributeConstraint(keyAttr);
                            constraint.setOperator(DBCLogicalOperator.EQUALS);
                            constraint.setValue(keyValue);
                            constraints.add(constraint);
                        }
                        if (!hasKey) {
                            // No key value for this row
                            continue;
                        }
                        DBDDataFilter filter = new DBDDataFilter(constraints);

                        RowDataReceiver dataReceiver = new RowDataReceiver(curAttributes);
                        final DBCStatistics stats = dataContainer.readData(executionSource, session, dataReceiver, filter, 0, 0, DBSDataContainer.FLAG_NONE, 0);
                        refreshValues[i] = dataReceiver.rowValues;
                    }
                }

                // Ok, now we have refreshed values. Let's update real model
                UIUtils.syncExec(() -> {
                    // Update only if metadata wasn't changed
                    if (!viewer.getControl().isDisposed() && viewer.getModel().getAttributes() == curAttributes) {
                        for (int i = 0; i < rows.size(); i++) {
                            if (refreshValues[i] != null) {
                                rows.get(i).values = refreshValues[i];
                            }
                        }
                        viewer.redrawData(false, true);
                    }
                });
            } catch (Throwable ex) {
                log.warn("Error refreshing rows", ex);
                // Error happened during data refresh
                // Let's rollback if we are in transaction
                if (viewer.getPreferenceStore().getBoolean(ModelPreferences.QUERY_ROLLBACK_ON_ERROR)) {
                    DBCTransactionManager txnManager = DBUtils.getTransactionManager(getExecutionContext());
                    try {
                        if (txnManager != null && !txnManager.isAutoCommit()) {
                            try (DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, "Rollback after data refresh failure")) {
                                txnManager.rollback(session, null);
                            }
                        }
                    } catch (DBCException e) {
                        log.warn("Error rolling back after data refresh failure", ex);
                    }
                }
            }
            return Status.OK_STATUS;
        }
    }

}
