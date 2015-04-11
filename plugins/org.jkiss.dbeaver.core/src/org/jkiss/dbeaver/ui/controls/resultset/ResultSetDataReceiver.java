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

import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.core.Log;
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

    static final Log log = Log.getLog(ResultSetDataReceiver.class);

    private ResultSetViewer resultSetViewer;
    private int columnsCount;
    private DBDAttributeBindingMeta[] metaColumns;
    private List<Object[]> rows = new ArrayList<Object[]>();
    private boolean hasMoreData;
    private boolean nextSegmentRead;
    private long offset;
    private long maxRows;

    private Map<DBCAttributeMetaData, List<DBCException>> errors = new HashMap<DBCAttributeMetaData, List<DBCException>>();

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

    @Override
    public void fetchStart(DBCSession session, final DBCResultSet resultSet, long offset, long maxRows)
        throws DBCException
    {
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

            resultSetViewer.setMetaData(metaColumns);
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
                    metaColumns[i].getMetaAttribute(),
                    metaColumns[i].getOrdinalPosition());
            }
            catch (DBCException e) {
                // Do not reports the same error multiple times
                // There are a lot of error could occur during result set fetch
                // We report certain error only once
                List<DBCException> errorList = errors.get(metaColumns[i].getMetaAttribute());
                if (errorList == null) {
                    errorList = new ArrayList<DBCException>();
                    errors.put(metaColumns[i].getMetaAttribute(), errorList);
                }
                if (!errorList.contains(e)) {
                    log.warn("Could not read column '" + metaColumns[i].getName() + "' value", e);
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
            // Read locators' metadata
            ResultSetUtils.findValueLocators(session, metaColumns, rows);
        }

        final List<Object[]> tmpRows = rows;

        final boolean nextSegmentRead = this.nextSegmentRead;
        runInUI(new Runnable() {
            @Override
            public void run() {
                // Push data into viewer
                if (!nextSegmentRead) {
                    resultSetViewer.updatePresentation(resultSet);
                    resultSetViewer.setData(tmpRows);
                } else {
                    resultSetViewer.appendData(tmpRows);
                }
                // Check for more data
                hasMoreData = maxRows > 0 && tmpRows.size() >= maxRows;
            }
        });
    }

    @Override
    public void close()
    {
        nextSegmentRead = false;

        errors.clear();
        rows = new ArrayList<Object[]>();
    }

    private void runInUI(Runnable runnable) {
        Control control = resultSetViewer.getControl();
        if (!control.isDisposed()) {
            control.getDisplay().asyncExec(runnable);
        }
    }

}
