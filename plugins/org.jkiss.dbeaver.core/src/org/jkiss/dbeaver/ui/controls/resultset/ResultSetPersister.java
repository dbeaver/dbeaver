/*
 * Copyright (C) 2010-2014 Serge Rieder
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
* Result set data updater
*/
class ResultSetPersister {

    private final Map<Integer, Map<DBSEntity, ResultSetViewer.TableRowInfo>> updatedRows = new TreeMap<Integer, Map<DBSEntity, ResultSetViewer.TableRowInfo>>();

    private final ResultSetViewer viewer;
    private final ResultSetModel model;

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
        prepareDeleteStatements();
        prepareInsertStatements();
        prepareUpdateStatements();
        execute(monitor, listener);
    }

    private void prepareUpdateRows()
        throws DBException
    {
        // Prepare rows
        for (GridPos cell : model.getEditedValues().keySet()) {
            Map<DBSEntity, ResultSetViewer.TableRowInfo> tableMap = updatedRows.get(cell.row);
            if (tableMap == null) {
                tableMap = new HashMap<DBSEntity, ResultSetViewer.TableRowInfo>();
                updatedRows.put(cell.row, tableMap);
            }

            DBDAttributeBinding metaColumn = columns[cell.col];
            DBSEntity metaTable = metaColumn.getRowIdentifier().getEntity();
            ResultSetViewer.TableRowInfo tableRowInfo = tableMap.get(metaTable);
            if (tableRowInfo == null) {
                tableRowInfo = new ResultSetViewer.TableRowInfo(metaTable, metaColumn.getRowIdentifier().getEntityIdentifier());
                tableMap.put(metaTable, tableRowInfo);
            }
            tableRowInfo.tableCells.add(cell);
        }
    }

    private void prepareDeleteStatements()
        throws DBException
    {
        // Make delete statements
        for (RowInfo rowNum : model.getRemovedRows()) {
            DBSEntity table = columns[0].getRowIdentifier().getEntity();
            DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.DELETE, rowNum, table);
            Collection<? extends DBSEntityAttribute> keyColumns = columns[0].getRowIdentifier().getEntityIdentifier().getAttributes();
            for (DBSEntityAttribute column : keyColumns) {
                DBDAttributeBinding binding = model.getAttributeBinding(column);
                if (binding == null) {
                    throw new DBCException("Can't find meta column for ID column " + column.getName());
                }
                statement.keyAttributes.add(
                    new DBDAttributeValue(
                        column,
                        model.getRowData(rowNum.row)[binding.getAttributeIndex()]));
            }
            deleteStatements.add(statement);
        }
    }

    private void prepareInsertStatements()
        throws DBException
    {
        // Make insert statements
        for (RowInfo rowNum : model.getAddedRows()) {
            Object[] cellValues = model.getRowData(rowNum.row);
            DBSEntity table = columns[0].getRowIdentifier().getEntity();
            DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.INSERT, rowNum, table);
            for (int i = 0; i < columns.length; i++) {
                DBDAttributeBinding column = columns[i];
                statement.keyAttributes.add(new DBDAttributeValue(column.getAttribute(), cellValues[i]));
            }
            insertStatements.add(statement);
        }
    }

    private void prepareUpdateStatements()
        throws DBException
    {
        prepareUpdateRows();

        if (updatedRows == null) {
            return;
        }

        // Make statements
        for (Integer rowNum : updatedRows.keySet()) {
            Map<DBSEntity, ResultSetViewer.TableRowInfo> tableMap = updatedRows.get(rowNum);
            for (DBSEntity table : tableMap.keySet()) {
                ResultSetViewer.TableRowInfo rowInfo = tableMap.get(table);
                DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.UPDATE, new RowInfo(rowNum), table);
                // Updated columns
                for (int i = 0; i < rowInfo.tableCells.size(); i++) {
                    GridPos cell = rowInfo.tableCells.get(i);
                    DBDAttributeBinding metaColumn = columns[cell.col];
                    statement.updateAttributes.add(new DBDAttributeValue(metaColumn.getAttribute(), model.getRowData(rowNum)[cell.col]));
                }
                // Key columns
                Collection<? extends DBCAttributeMetaData> idColumns = rowInfo.id.getResultSetColumns();
                for (DBCAttributeMetaData idAttribute : idColumns) {
                    // Find meta column and add statement parameter
                    DBDAttributeBinding metaColumn = model.getAttributeBinding(idAttribute);
                    if (metaColumn == null) {
                        throw new DBCException("Can't find meta column for ID column " + idAttribute.getName());
                    }
                    Object keyValue = model.getCellValue(rowNum, metaColumn.getAttributeIndex());
                    // Try to find old key oldValue
                    for (Map.Entry<GridPos, Object> cell : model.getEditedValues().entrySet()) {
                        if (cell.getKey().equals(metaColumn.getAttributeIndex(), rowNum)) {
                            keyValue = cell.getValue();
                        }
                    }
                    statement.keyAttributes.add(new DBDAttributeValue(metaColumn.getAttribute(), keyValue));
                }
                updateStatements.add(statement);
            }
        }
    }

    private void execute(DBRProgressMonitor monitor, final DataUpdateListener listener)
        throws DBException
    {
        DataUpdaterJob job = new DataUpdaterJob(listener);
        if (monitor == null) {
            job.schedule();
        } else {
            job.run(monitor);
        }
    }

    public void rejectChanges()
    {
        for (Map.Entry<GridPos, Object> cell : model.getEditedValues().entrySet()) {
            Object[] row = model.getRowData(cell.getKey().row);
            ResultSetModel.releaseValue(row[cell.getKey().col]);
            row[cell.getKey().col] = cell.getValue();
        }
        model.getEditedValues().clear();

        boolean rowsChanged = model.cleanupRows(model.getAddedRows());
        // Remove deleted rows
        model.getRemovedRows().clear();

        viewer.refreshSpreadsheet(false, rowsChanged);
        viewer.fireResultSetChange();
        viewer.updateEditControls();
        viewer.previewValue();
    }

    // Reflect data changes in viewer
    // Changes affects only rows which statements executed successfully
    private boolean reflectChanges()
    {
        boolean rowsChanged = false;
        for (Iterator<Map.Entry<GridPos, Object>> iter = model.getEditedValues().entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<GridPos, Object> entry = iter.next();
            for (DataStatementInfo stat : updateStatements) {
                if (stat.executed && stat.row.row == entry.getKey().row && stat.hasUpdateColumn(columns[entry.getKey().col])) {
                    reflectKeysUpdate(stat);
                    iter.remove();
                    break;
                }
            }
        }
        for (Iterator<RowInfo> iter = model.getAddedRows().iterator(); iter.hasNext(); ) {
            RowInfo row = iter.next();
            for (DataStatementInfo stat : insertStatements) {
                if (stat.executed && stat.row.equals(row)) {
                    reflectKeysUpdate(stat);
                    iter.remove();
                    break;
                }
            }
        }
        for (Iterator<RowInfo> iter = model.getRemovedRows().iterator(); iter.hasNext(); ) {
            RowInfo row = iter.next();
            for (DataStatementInfo stat : deleteStatements) {
                if (stat.executed && stat.row.equals(row)) {
                    model.cleanupRow(row.row);
                    iter.remove();
                    rowsChanged = true;
                    break;
                }
            }
        }
        return rowsChanged;
    }

    private void reflectKeysUpdate(DataStatementInfo stat)
    {
        // Update keys
        if (!stat.updatedCells.isEmpty()) {
            for (Map.Entry<Integer, Object> entry : stat.updatedCells.entrySet()) {
                Object[] row = model.getRowData(stat.row.row);
                ResultSetModel.releaseValue(row[entry.getKey()]);
                row[entry.getKey()] = entry.getValue();
            }
        }
    }

/*
    private void releaseStatements()
    {
        for (DataStatementInfo stat : updateStatements) releaseStatement(stat);
        for (DataStatementInfo stat : insertStatements) releaseStatement(stat);
        for (DataStatementInfo stat : deleteStatements) releaseStatement(stat);
    }

    private void releaseStatement(DataStatementInfo stat)
    {
        for (DBDColumnValue value : stat.keyColumns) releaseValue(value.getValue());
        for (DBDColumnValue value : stat.updateColumns) releaseValue(value.getValue());
    }

*/
    private class DataUpdaterJob extends DataSourceJob {
        private final DataUpdateListener listener;
        private boolean autocommit;
        private DBCStatistics updateStats, insertStats, deleteStats;
        private DBCSavepoint savepoint;

        protected DataUpdaterJob(DataUpdateListener listener)
        {
            super(CoreMessages.controls_resultset_viewer_job_update, DBIcon.SQL_EXECUTE.getImageDescriptor(), viewer.getDataSource());
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
                        viewer.refreshSpreadsheet(false, rowsChanged);
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
                                deleteStats.accumulate(batch.execute());
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
                                insertStats.accumulate(batch.execute());
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
                                updateStats.accumulate(batch.execute());
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
        public void fetchStart(DBCSession session, DBCResultSet resultSet)
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
                        statement.updatedCells.put(binding.getAttributeIndex(), keyValue);
                        //curRows.get(statement.row.row)[colIndex] = keyValue;
                        updated = true;
                    }
                }
                if (!updated) {
                    // Key not found
                    // Try to find and update auto-increment column
                    for (int k = 0; k < columns.length; k++) {
                        DBDAttributeBinding column = columns[k];
                        if (column.getAttribute().isAutoGenerated()) {
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
        RowInfo row;
        DBSEntity entity;
        List<DBDAttributeValue> keyAttributes = new ArrayList<DBDAttributeValue>();
        List<DBDAttributeValue> updateAttributes = new ArrayList<DBDAttributeValue>();
        boolean executed = false;
        Map<Integer, Object> updatedCells = new HashMap<Integer, Object>();

        DataStatementInfo(DBSManipulationType type, RowInfo row, DBSEntity entity)
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
        boolean hasUpdateColumn(DBDAttributeBinding column)
        {
            for (DBDAttributeValue col : updateAttributes) {
                if (col.getAttribute() == column.getMetaAttribute() || col.getAttribute() == column.getEntityAttribute()) {
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
