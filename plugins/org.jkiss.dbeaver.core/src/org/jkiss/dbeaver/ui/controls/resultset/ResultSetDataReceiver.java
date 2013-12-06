/*
 * Copyright (C) 2010-2013 Serge Rieder
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data pump for SQL queries
 */
class ResultSetDataReceiver implements DBDDataReceiver {

    static final Log log = LogFactory.getLog(ResultSetDataReceiver.class);

    private ResultSetViewer resultSetViewer;
    private int columnsCount;
    private DBDAttributeBinding[] metaColumns;
    private List<Object[]> rows = new ArrayList<Object[]>();
    private boolean hasMoreData;
    private boolean nextSegmentRead;

    Map<DBCAttributeMetaData, List<DBCException>> errors = new HashMap<DBCAttributeMetaData, List<DBCException>>();
    private boolean updateMetaData;

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
    public void fetchStart(DBCSession session, DBCResultSet resultSet)
        throws DBCException
    {
        rows.clear();

        if (!nextSegmentRead) {
            // Get columns metadata
            DBCResultSetMetaData metaData = resultSet.getResultSetMetaData();

            List<DBCAttributeMetaData> rsAttributes = metaData.getAttributes();
            columnsCount = rsAttributes.size();

            // Determine type handlers for all columns
            //DBPDataSource dataSource = resultSet.getContext().getDataSource();

            // Extract column info
            metaColumns = new DBDAttributeBinding[columnsCount];
            for (int i = 0; i < columnsCount; i++) {
                metaColumns[i] = DBUtils.getColumnBinding(session, rsAttributes.get(i), i);
            }

            updateMetaData = resultSetViewer.setMetaData(metaColumns);
        } else {
            updateMetaData = false;
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
                    metaColumns[i].getAttributeIndex());
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
                    log.warn("Could not read column '" + metaColumns[i].getMetaAttribute().getName() + "' value", e);
                    errorList.add(e);
                }
            }
        }
        rows.add(row);
    }

    @Override
    public void fetchEnd(DBCSession session)
        throws DBCException
    {
        if (updateMetaData) {
            // Read locators' metadata
            DBUtils.findValueLocators(session.getProgressMonitor(), metaColumns);
        }

        final List<Object[]> tmpRows = rows;
        int segmentSize = resultSetViewer.getSegmentMaxRows();
        hasMoreData = segmentSize > 0 && tmpRows.size() >= segmentSize;

        final boolean nextSegmentRead = this.nextSegmentRead;
        final boolean updateMetaData = this.updateMetaData;
        resultSetViewer.getControl().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run()
            {
                // Check for more data
                // Push data into viewer
                if (!nextSegmentRead) {
                    resultSetViewer.setData(tmpRows, updateMetaData);
                } else {
                    resultSetViewer.appendData(tmpRows);
                }
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

}
