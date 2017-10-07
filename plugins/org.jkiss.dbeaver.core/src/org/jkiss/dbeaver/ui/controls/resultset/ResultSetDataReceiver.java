/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data pump for SQL queries
 */
class ResultSetDataReceiver implements DBDDataReceiver {

    private static final Log log = Log.getLog(ResultSetDataReceiver.class);

    private ResultSetViewer resultSetViewer;
    private int columnsCount;
    private DBDAttributeBindingMeta[] metaColumns;
    private List<Object[]> rows = new ArrayList<>();
    private boolean hasMoreData;
    private boolean nextSegmentRead;
    private long offset;
    private long maxRows;

    // Attribute fetching errors. Collect them to avoid tons of similar error in log
    private Map<DBCAttributeMetaData, List<String>> attrErrors = new HashMap<>();
    // All (unique) errors happened during fetch
    private List<Throwable> errorList = new ArrayList<>();
    private int focusRow;

    ResultSetDataReceiver(ResultSetViewer resultSetViewer)
    {
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

    public List<Throwable> getErrorList() {
        return errorList;
    }

    @Override
    public void fetchStart(DBCSession session, final DBCResultSet resultSet, long offset, long maxRows)
        throws DBCException
    {
        this.errorList.clear();
        this.rows.clear();
        this.offset = offset;
        this.maxRows = maxRows;

        if (!nextSegmentRead) {
            // Get columns metadata
            DBCResultSetMetaData metaData = resultSet.getMeta();

            List<DBCAttributeMetaData> rsAttributes = metaData.getAttributes();
            columnsCount = rsAttributes.size();

            // Extract column info
            metaColumns = new DBDAttributeBindingMeta[columnsCount];
            for (int i = 0; i < columnsCount; i++) {
                metaColumns[i] = DBUtils.getAttributeBinding(session, rsAttributes.get(i));
            }

            resultSetViewer.setMetaData(resultSet, metaColumns);
        }
    }

    @Override
    public void fetchRow(DBCSession session, DBCResultSet resultSet)
        throws DBCException
    {
        Object[] row = new Object[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            try {
                row[i] = metaColumns[i].getValueHandler().fetchValueObject(
                    session,
                    resultSet,
                    metaColumns[i].getAttribute(),
                    metaColumns[i].getOrdinalPosition());
            }
            catch (Throwable e) {
                // Do not reports the same error multiple times
                // There are a lot of error could occur during result set fetch
                // We report certain error only once
                List<String> attrErrors = this.attrErrors.get(metaColumns[i].getMetaAttribute());
                if (attrErrors == null) {
                    attrErrors = new ArrayList<>();
                    this.attrErrors.put(metaColumns[i].getMetaAttribute(), attrErrors);
                }
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
    public void fetchEnd(DBCSession session, final DBCResultSet resultSet)
        throws DBCException
    {
        if (!nextSegmentRead) {
            try {
                // Read locators' metadata
                ResultSetUtils.bindAttributes(session, resultSet, metaColumns, rows);
            } catch (Throwable e) {
                errorList.add(e);
            }
        }

        final List<Object[]> tmpRows = rows;

        final boolean nextSegmentRead = this.nextSegmentRead;
        DBeaverUI.syncExec(new Runnable() {
            @Override
            public void run() {
                // Push data into viewer
                if (!nextSegmentRead) {
                    resultSetViewer.updatePresentation(resultSet);
                    resultSetViewer.setData(tmpRows, focusRow);
                    resultSetViewer.getActivePresentation().refreshData(true, false, !resultSetViewer.getModel().isMetadataChanged());
                } else {
                    resultSetViewer.appendData(tmpRows);
                    resultSetViewer.getActivePresentation().refreshData(false, true, true);
                }
                resultSetViewer.updateStatusMessage();
                // Check for more data
                hasMoreData = maxRows > 0 && tmpRows.size() >= maxRows;
            }
        });
    }

    @Override
    public void close()
    {
        nextSegmentRead = false;

        attrErrors.clear();
        rows = new ArrayList<>();
    }

    public void setFocusRow(int focusRow) {
        this.focusRow = focusRow;
    }
}
