/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDDataReceiverInteractive;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data pump for SQL queries
 */
class ResultSetDataReceiver implements DBDDataReceiver, DBDDataReceiverInteractive {

    private static final Log log = Log.getLog(ResultSetDataReceiver.class);

    private ResultSetViewer resultSetViewer;
    private int columnsCount;
    private DBDAttributeBinding[] metaColumns;
    private List<Object[]> rows = new ArrayList<>();
    private boolean hasMoreData;
    private boolean nextSegmentRead;
    private long offset;
    private long maxRows;

    private boolean paused;

    // Attribute fetching errors. Collect them to avoid tons of similar error in log
    private Map<DBCAttributeMetaData, List<String>> attrErrors = new HashMap<>();
    // All (unique) errors happened during fetch
    private List<Throwable> errorList = new ArrayList<>();
    private int focusRow;
    private DBSDataContainer targetDataContainer;
    
    ResultSetDataReceiver(@NotNull ResultSetViewer resultSetViewer) {
        this.resultSetViewer = resultSetViewer;
    }

    boolean isHasMoreData() {
        return hasMoreData;
    }

    void setHasMoreData(boolean hasMoreData) {
        this.hasMoreData = hasMoreData;
    }

    void setNextSegmentRead(boolean nextSegmentRead) {
        this.nextSegmentRead = nextSegmentRead;
    }

    void setFocusRow(int focusRow) {
        this.focusRow = focusRow;
    }

    void setTargetDataContainer(DBSDataContainer targetDataContainer) {
        this.targetDataContainer = targetDataContainer;
    }

    List<Throwable> getErrorList() {
        return errorList;
    }

    @Override
    public void fetchStart(@NotNull DBCSession session, @NotNull final DBCResultSet resultSet, long offset, long maxRows)
        throws DBCException {
        this.errorList.clear();
        this.rows.clear();
        this.offset = offset;
        this.maxRows = maxRows;

        if (!nextSegmentRead) {
            // Get columns metadata
            DBCResultSetMetaData metaData = resultSet.getMeta();
            if (metaData == null) {
                throw new DBCException("Null resultset metadata");
            }

            List<? extends DBCAttributeMetaData> rsAttributes = metaData.getAttributes();
            columnsCount = rsAttributes.size();

            // Extract column info
            metaColumns = DBUtils.getAttributeBindings(session, getDataContainer(), metaData);

            resultSetViewer.setMetaData(resultSet, metaColumns);
        }
    }

    @Override
    public void fetchRow(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) {
        Object[] row = new Object[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            try {
                DBSAttributeBase metaAttribute = metaColumns[i].getAttribute();
                if (metaAttribute == null) {
                    continue;
                }
                row[i] = metaColumns[i].getValueHandler().fetchValueObject(
                    session,
                    resultSet,
                    metaAttribute,
                    metaColumns[i].getOrdinalPosition());
            } catch (Throwable e) {
                // Do not reports the same error multiple times
                // There are a lot of error could occur during result set fetch
                // We report certain error only once

                row[i] = new DBDValueError(e);

                List<String> attrErrors = this.attrErrors.computeIfAbsent(
                    metaColumns[i].getMetaAttribute(),
                    k -> new ArrayList<>());
                String errMessage = e.getClass().getName();
                if (!errMessage.startsWith("java.lang.")) {
                    errMessage += ":" + e.getMessage();
                }
                if (!attrErrors.contains(errMessage)) {
                    log.warn("Can't read column '" + metaColumns[i].getName() + "' value", e);
                    attrErrors.add(errMessage);
                    errorList.add(e);
                }
            }
        }
        rows.add(row);
    }

    @Override
    public void fetchEnd(@NotNull DBCSession session, @NotNull final DBCResultSet resultSet) {
        if (!nextSegmentRead) {
            if (metaColumns != null) {
                try {
                    // Read locators' metadata
                    DBSEntity entity = null;
                    DBSDataContainer dataContainer = getDataContainer();
                    if (dataContainer instanceof DBSEntity) {
                        entity = (DBSEntity) dataContainer;
                    }
                    DBExecUtils.bindAttributes(session, entity, resultSet, metaColumns, rows);
                } catch (Throwable e) {
                    errorList.add(e);
                }
            } else {
                // fetchStart was failed
            }
        }

        final List<Object[]> tmpRows = rows;

        final boolean nextSegmentRead = this.nextSegmentRead;

        // Push data into viewer
        DBRProgressMonitor monitor = session.getProgressMonitor();
        monitor.beginTask("Populate data", 1);
        if (!nextSegmentRead) {
            monitor.subTask("Set data");
            resultSetViewer.setData(monitor, tmpRows, focusRow);
        } else {
            monitor.subTask("Append data");
            boolean resetOldRows = getDataContainer().getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_REREAD_ON_SCROLLING);
            resultSetViewer.appendData(monitor, tmpRows, resetOldRows);
        }
        // Check for more data
        hasMoreData = maxRows > 0 && tmpRows.size() >= maxRows;
        monitor.done();

        UIUtils.syncExec(() -> {
            // Push data into viewer
            if (resultSetViewer.getControl().isDisposed()) {
                return;
            }
            if (!nextSegmentRead) {
                boolean metadataChanged = resultSetViewer.getModel().isMetadataChanged();
                resultSetViewer.updatePresentation(resultSet, metadataChanged);
                resultSetViewer.getActivePresentation().refreshData(true, false, !metadataChanged);
                resultSetViewer.updateStatusMessage();
            } else {
                resultSetViewer.getActivePresentation().refreshData(false, true, true);
            }
        });
    }

    private DBSDataContainer getDataContainer() {
        return targetDataContainer != null ? targetDataContainer : resultSetViewer.getDataContainer();
    }

    @Override
    public void close() {
        nextSegmentRead = false;

        attrErrors.clear();
        rows = new ArrayList<>();
    }

    @Override
    public boolean isDataReceivePaused() {
        return paused;
    }

    @Override
    public void setDataReceivePaused(boolean paused) {
        this.paused = paused;
    }

}
