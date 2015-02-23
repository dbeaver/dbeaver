/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
* Result set data updater
*/
class ResultSetPersister {

    private final ResultSetViewer viewer;
    private final ResultSetModel model;

    private final List<RowData> deletedRows = new ArrayList<RowData>();
    private final List<RowData> addedRows = new ArrayList<RowData>();
    private final List<RowData> changedRows = new ArrayList<RowData>();
    private final Map<RowData, DBDRowIdentifier> rowIdentifiers = new LinkedHashMap<RowData, DBDRowIdentifier>();
    private final List<DataStatementInfo> insertStatements = new ArrayList<DataStatementInfo>();
    private final List<DataStatementInfo> deleteStatements = new ArrayList<DataStatementInfo>();
    private final List<DataStatementInfo> updateStatements = new ArrayList<DataStatementInfo>();
    private final DBDAttributeBinding[] columns;

    ResultSetPersister(ResultSetViewer viewer)
    {
        this.viewer = viewer;
        this.model = viewer.getModel();
        this.columns = model.getColumns();
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
        for (RowData row : model.getAllRows()) {
            switch (row.getState()) {
                case RowData.STATE_NORMAL:
                    if (row.isChanged()) {
                        changedRows.add(row);
                    }
                    break;
                case RowData.STATE_ADDED:
                    addedRows.add(row);
                    break;
                case RowData.STATE_REMOVED:
                    deletedRows.add(row);
                    break;
            }
        }

        // Prepare rows
        for (RowData row : changedRows) {
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
        for (RowData row : deletedRows) {
            DBSEntity table = columns[0].getRowIdentifier().getEntity();
            DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.DELETE, row, table);
            List<DBDAttributeBinding> keyColumns = columns[0].getRowIdentifier().getEntityIdentifier().getAttributes();
            for (DBDAttributeBinding binding : keyColumns) {
                statement.keyAttributes.add(
                    new DBDAttributeValue(
                        binding,
                        row.values[binding.getOrdinalPosition()]));
            }
            deleteStatements.add(statement);
        }
    }

    private void prepareInsertStatements()
        throws DBException
    {
        // Make insert statements
        for (RowData row : addedRows) {
            Object[] cellValues = row.values;
            DBSEntity table = columns[0].getRowIdentifier().getEntity();
            DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.INSERT, row, table);
            for (int i = 0; i < columns.length; i++) {
                DBDAttributeBinding column = columns[i];
                statement.keyAttributes.add(new DBDAttributeValue(column, cellValues[i]));
            }
            insertStatements.add(statement);
        }
    }

    private void prepareUpdateStatements()
        throws DBException
    {
        // Make statements
        for (RowData row : this.rowIdentifiers.keySet()) {
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
                List<DBDAttributeBinding> idColumns = rowIdentifier.getEntityIdentifier().getAttributes();
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
        DBPDataSource dataSource = viewer.getDataSource();
        if (dataSource == null) {
            throw new DBCException("Not connected to data source");
        }
        DataUpdaterJob job = new DataUpdaterJob(listener, dataSource);
        if (monitor == null) {
            job.schedule();
        } else {
            job.run(monitor);
        }
    }

    public void rejectChanges()
    {
        collectChanges();
        for (RowData row : changedRows) {
            if (row.changes != null) {
                for (Map.Entry<DBDAttributeBinding, Object> changedValue : row.changes.entrySet()) {
                    Object curValue = model.getCellValue(changedValue.getKey(), row);
                    DBUtils.releaseValue(curValue);
                    model.updateCellValue(changedValue.getKey(), row, changedValue.getValue(), false);
                }
                row.changes = null;
            }
        }

        boolean rowsChanged = model.cleanupRows(addedRows);
        // Remove deleted rows
        for (RowData row : deletedRows) {
            row.setState(RowData.STATE_NORMAL);
        }
        model.refreshChangeCount();

        viewer.refreshSpreadsheet(rowsChanged);
        viewer.fireResultSetChange();
        viewer.updateEditControls();
        viewer.previewValue();
    }

    // Reflect data changes in viewer
    // Changes affects only rows which statements executed successfully
    private boolean reflectChanges()
    {
        boolean rowsChanged = false;
        for (RowData row : changedRows) {
            for (DataStatementInfo stat : updateStatements) {
                if (stat.executed && stat.row == row) {
                    reflectKeysUpdate(stat);
                    row.changes = null;
                    break;
                }
            }
        }
        for (RowData row : addedRows) {
            for (DataStatementInfo stat : insertStatements) {
                if (stat.executed && stat.row == row) {
                    reflectKeysUpdate(stat);
                    row.setState(RowData.STATE_NORMAL);
                    break;
                }
            }
        }
        for (RowData row : deletedRows) {
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
                RowData row = stat.row;
                DBUtils.releaseValue(row.values[entry.getKey()]);
                row.values[entry.getKey()] = entry.getValue();
            }
        }
    }

    private class DataUpdaterJob extends DataSourceJob {
        private final DataUpdateListener listener;
        private boolean autocommit;
        private DBCStatistics updateStats, insertStats, deleteStats;
        private DBCSavepoint savepoint;

        protected DataUpdaterJob(@Nullable DataUpdateListener listener, @NotNull DBPDataSource dataSource)
        {
            super(CoreMessages.controls_resultset_viewer_job_update, DBIcon.SQL_EXECUTE.getImageDescriptor(), dataSource);
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
                        viewer.refreshSpreadsheet(rowsChanged);
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
                            viewer.setStatus(error.getMessage(), true);
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
            DBCSession session = getDataSource().openSession(monitor, DBCExecutionPurpose.UTIL, CoreMessages.controls_resultset_viewer_job_update);
            try {
                monitor.beginTask(
                    CoreMessages.controls_resultset_viewer_monitor_aply_changes,
                    ResultSetPersister.this.deleteStatements.size() + ResultSetPersister.this.insertStatements.size() + ResultSetPersister.this.updateStatements.size() + 1);
                monitor.subTask(CoreMessages.controls_resultset_check_autocommit_state);
                try {
                    this.autocommit = session.getTransactionManager().isAutoCommit();
                }
                catch (DBCException e) {
                    ResultSetViewer.log.warn("Could not determine autocommit state", e);
                    this.autocommit = true;
                }
                monitor.worked(1);
                if (!this.autocommit && session.getTransactionManager().supportsSavepoints()) {
                    try {
                        this.savepoint = session.getTransactionManager().setSavepoint(null);
                    }
                    catch (Throwable e) {
                        // May be savepoints not supported
                        ResultSetViewer.log.debug("Could not set savepoint", e);
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
                    if (this.savepoint != null) {
                        try {
                            session.getTransactionManager().releaseSavepoint(this.savepoint);
                        }
                        catch (Throwable e) {
                            // Maybe savepoints not supported
                            ResultSetViewer.log.debug("Could not release savepoint", e);
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
            try {
                session.getTransactionManager().rollback(savepoint);
            }
            catch (Throwable e) {
                ResultSetViewer.log.debug("Error during transaction rollback", e);
            }
        }

    }

    private DBSDataManipulator getDataManipulator(DBSEntity entity) throws DBCException
    {
        if (entity instanceof DBSDataManipulator) {
            return (DBSDataManipulator)entity;
        } else {
            throw new DBCException("Entity " + entity.getName() + " doesn't support data manipulation");
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
            DBCResultSetMetaData rsMeta = resultSet.getResultSetMetaData();
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
                    ResultSetViewer.log.debug("Could not find target column for autogenerated key '" + keyAttribute.getName() + "'");
                }
            }
        }

        @Override
        public void fetchEnd(DBCSession session)
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
        DBSManipulationType type;
        RowData row;
        DBSEntity entity;
        List<DBDAttributeValue> keyAttributes = new ArrayList<DBDAttributeValue>();
        List<DBDAttributeValue> updateAttributes = new ArrayList<DBDAttributeValue>();
        boolean executed = false;
        Map<Integer, Object> updatedCells = new HashMap<Integer, Object>();

        DataStatementInfo(DBSManipulationType type, RowData row, DBSEntity entity)
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
