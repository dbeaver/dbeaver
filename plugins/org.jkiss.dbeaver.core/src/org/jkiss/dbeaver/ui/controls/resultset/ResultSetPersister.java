/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
* Result set data updater
*/
class ResultSetPersister {

    static final Log log = Log.getLog(ResultSetPersister.class);
    @NotNull
    private final ResultSetViewer viewer;
    @NotNull
    private final ResultSetModel model;
    @NotNull
    private final DBDAttributeBinding[] columns;

    private final List<ResultSetRow> deletedRows = new ArrayList<ResultSetRow>();
    private final List<ResultSetRow> addedRows = new ArrayList<ResultSetRow>();
    private final List<ResultSetRow> changedRows = new ArrayList<ResultSetRow>();
    private final Map<ResultSetRow, DBDRowIdentifier> rowIdentifiers = new LinkedHashMap<ResultSetRow, DBDRowIdentifier>();
    private final List<DataStatementInfo> insertStatements = new ArrayList<DataStatementInfo>();
    private final List<DataStatementInfo> deleteStatements = new ArrayList<DataStatementInfo>();
    private final List<DataStatementInfo> updateStatements = new ArrayList<DataStatementInfo>();

    ResultSetPersister(@NotNull ResultSetViewer viewer)
    {
        this.viewer = viewer;
        this.model = viewer.getModel();
        this.columns = model.getAttributes();
    }

    /**
     * Applies changes.
     * @throws org.jkiss.dbeaver.DBException
     * @param monitor progress monitor
     * @param listener value listener
     */
    void applyChanges(@Nullable DBRProgressMonitor monitor, @Nullable DataUpdateListener listener)
        throws DBException
    {
        collectChanges();

        prepareDeleteStatements();
        prepareInsertStatements();
        prepareUpdateStatements();
        execute(monitor, listener);
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

    private void prepareDeleteStatements()
        throws DBException
    {
        // Make delete statements
        DBDRowIdentifier rowIdentifier = getDefaultRowIdentifier();
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
    }

    private void prepareInsertStatements()
        throws DBException
    {
        // Make insert statements
        DBSEntity table = getDefaultRowIdentifier().getEntity();
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

    private void prepareUpdateStatements()
        throws DBException
    {
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
                    }
                    statement.keyAttributes.add(new DBDAttributeValue(metaColumn, keyValue));
                }
                updateStatements.add(statement);
            }
        }
    }

    private void execute(@Nullable DBRProgressMonitor monitor, @Nullable final DataUpdateListener listener)
        throws DBException
    {
        DBCExecutionContext executionContext = viewer.getContainer().getExecutionContext();
        if (executionContext == null) {
            throw new DBCException("No execution context");
        }
        DataUpdaterJob job = new DataUpdaterJob(listener, executionContext);
        if (monitor == null) {
            job.schedule();
        } else {
            job.run(monitor);
        }
    }

    public void rejectChanges()
    {
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

        viewer.redrawData(rowsChanged);
        viewer.fireResultSetChange();
        viewer.updateEditControls();
        viewer.getActivePresentation().updateValueView();
    }

    // Reflect data changes in viewer
    // Changes affects only rows which statements executed successfully
    private boolean reflectChanges()
    {
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

    private void reflectKeysUpdate(DataStatementInfo stat)
    {
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
    private DBDRowIdentifier getDefaultRowIdentifier() throws DBCException {
        DBDRowIdentifier rowIdentifier = columns[0].getRowIdentifier();
        if (rowIdentifier == null) {
            throw new DBCException("Internal error: can't obtain entity identifier");
        }
        return rowIdentifier;
    }

    @NotNull
    private DBSDataManipulator getDataManipulator(DBSEntity entity) throws DBCException
    {
        if (entity instanceof DBSDataManipulator) {
            return (DBSDataManipulator)entity;
        } else {
            throw new DBCException("Entity " + entity.getName() + " doesn't support data manipulation");
        }
    }

    private class DataUpdaterJob extends DataSourceJob {
        private final DataUpdateListener listener;
        private boolean autocommit;
        private DBCStatistics updateStats, insertStats, deleteStats;
        private DBCSavepoint savepoint;

        protected DataUpdaterJob(@Nullable DataUpdateListener listener, @NotNull DBCExecutionContext executionContext)
        {
            super(CoreMessages.controls_resultset_viewer_job_update, DBeaverIcons.getImageDescriptor(UIIcon.SQL_EXECUTE), executionContext);
            this.listener = listener;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            final Throwable error;
            model.setUpdateInProgress(true);
            updateStats = new DBCStatistics();
            insertStats = new DBCStatistics();
            deleteStats = new DBCStatistics();
            try {
                error = executeStatements(monitor);
            }
            finally {
                model.setUpdateInProgress(false);
            }

            // Reflect changes
            UIUtils.runInUI(viewer.getSite().getShell(), new Runnable() {
                @Override
                public void run()
                {
                    boolean rowsChanged = false;
                    if (DataUpdaterJob.this.autocommit || error == null) {
                        rowsChanged = reflectChanges();
                    }
                    if (!viewer.getControl().isDisposed()) {
                        //releaseStatements();
                        viewer.redrawData(rowsChanged);
                        viewer.updateEditControls();
                        if (error == null) {
                            viewer.setStatus(
                                NLS.bind(
                                    CoreMessages.controls_resultset_viewer_status_inserted_,
                                    new Object[]{
                                        DataUpdaterJob.this.insertStats.getRowsUpdated(),
                                        DataUpdaterJob.this.deleteStats.getRowsUpdated(),
                                        DataUpdaterJob.this.updateStats.getRowsUpdated()}));
                        } else {
                            UIUtils.showErrorDialog(viewer.getSite().getShell(), "Data error", "Error synchronizing data with database", error);
                            viewer.setStatus(GeneralUtils.getFirstMessage(error), true);
                        }
                    }
                    viewer.fireResultSetChange();
                }
            });
            if (this.listener != null) {
                this.listener.onUpdate(error == null);
            }

            return Status.OK_STATUS;
        }

        private Throwable executeStatements(DBRProgressMonitor monitor)
        {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(getExecutionContext());
            DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, CoreMessages.controls_resultset_viewer_job_update);
            try {
                monitor.beginTask(
                    CoreMessages.controls_resultset_viewer_monitor_aply_changes,
                    ResultSetPersister.this.deleteStatements.size() + ResultSetPersister.this.insertStatements.size() + ResultSetPersister.this.updateStatements.size() + 1);
                if (txnManager != null) {
                    monitor.subTask(CoreMessages.controls_resultset_check_autocommit_state);
                    try {
                        this.autocommit = txnManager.isAutoCommit();
                    } catch (DBCException e) {
                        log.warn("Can't determine autocommit state", e);
                        this.autocommit = true;
                    }
                }
                monitor.worked(1);
                if (txnManager != null) {
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
                            DBSDataManipulator.ExecuteBatch batch = dataContainer.deleteData(
                                session,
                                DBDAttributeValue.getAttributes(statement.keyAttributes));
                            try {
                                batch.add(DBDAttributeValue.getValues(statement.keyAttributes));
                                deleteStats.accumulate(batch.execute(session));
                            } finally {
                                batch.close();
                            }
                            processStatementChanges(statement);
                        }
                        catch (DBException e) {
                            processStatementError(statement, session);
                            return e;
                        }
                        monitor.worked(1);
                    }
                    for (DataStatementInfo statement : ResultSetPersister.this.insertStatements) {
                        if (monitor.isCanceled()) break;
                        try {
                            DBSDataManipulator dataContainer = getDataManipulator(statement.entity);
                            DBSDataManipulator.ExecuteBatch batch = dataContainer.insertData(
                                session,
                                DBDAttributeValue.getAttributes(statement.keyAttributes),
                                statement.needKeys() ? new KeyDataReceiver(statement) : null);
                            try {
                                batch.add(DBDAttributeValue.getValues(statement.keyAttributes));
                                insertStats.accumulate(batch.execute(session));
                            } finally {
                                batch.close();
                            }
                            processStatementChanges(statement);
                        }
                        catch (DBException e) {
                            processStatementError(statement, session);
                            return e;
                        }
                        monitor.worked(1);
                    }
                    for (DataStatementInfo statement : ResultSetPersister.this.updateStatements) {
                        if (monitor.isCanceled()) break;
                        try {
                            DBSDataManipulator dataContainer = getDataManipulator(statement.entity);
                            DBSDataManipulator.ExecuteBatch batch = dataContainer.updateData(
                                session,
                                DBDAttributeValue.getAttributes(statement.updateAttributes),
                                DBDAttributeValue.getAttributes(statement.keyAttributes),
                                null);
                            try {
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
                                updateStats.accumulate(batch.execute(session));
                            } finally {
                                batch.close();
                            }
                            processStatementChanges(statement);
                        }
                        catch (DBException e) {
                            processStatementError(statement, session);
                            return e;
                        }
                        monitor.worked(1);
                    }

                    return null;
                }
                finally {
                    if (txnManager != null && this.savepoint != null) {
                        try {
                            txnManager.releaseSavepoint(monitor, this.savepoint);
                        }
                        catch (Throwable e) {
                            // Maybe savepoints not supported
                            log.debug("Can't release savepoint", e);
                        }
                    }
                }
            }
            finally {
                monitor.done();
                session.close();
            }
        }

        private void processStatementChanges(DataStatementInfo statement)
        {
            statement.executed = true;
        }

        private void processStatementError(DataStatementInfo statement, DBCSession session)
        {
            statement.executed = false;
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(getExecutionContext());
            if (txnManager != null) {
                try {
                    txnManager.rollback(session, savepoint);
                } catch (Throwable e) {
                    log.debug("Error during transaction rollback", e);
                }
            }
        }

    }

    /**
    * Key data receiver
    */
    class KeyDataReceiver implements DBDDataReceiver {
        DataStatementInfo statement;

        public KeyDataReceiver(DataStatementInfo statement)
        {
            this.statement = statement;
        }

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows)
            throws DBCException
        {

        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet)
            throws DBCException
        {
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
        public void fetchEnd(DBCSession session, DBCResultSet resultSet)
            throws DBCException
        {

        }

        @Override
        public void close()
        {
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
        final List<DBDAttributeValue> keyAttributes = new ArrayList<DBDAttributeValue>();
        final List<DBDAttributeValue> updateAttributes = new ArrayList<DBDAttributeValue>();
        boolean executed = false;
        final Map<Integer, Object> updatedCells = new HashMap<Integer, Object>();

        DataStatementInfo(@NotNull DBSManipulationType type, @NotNull ResultSetRow row, @NotNull DBSEntity entity)
        {
            this.type = type;
            this.row = row;
            this.entity = entity;
        }
        boolean needKeys()
        {
            for (DBDAttributeValue col : keyAttributes) {
                if (col.getAttribute().isAutoGenerated() && DBUtils.isNullValue(col.getValue())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Data update listener
     */
    static interface DataUpdateListener {

        void onUpdate(boolean success);

    }
}
