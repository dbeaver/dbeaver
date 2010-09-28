/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
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

    static final Log log = LogFactory.getLog(ResultSetDataReceiver.class);

    private ResultSetViewer resultSetViewer;
    private Display display;
    private int columnsCount;
    private DBDColumnBinding[] metaColumns;
    private List<Object[]> rows = new ArrayList<Object[]>();
    private boolean hasMoreData;
    private boolean nextSegmentRead;

    Map<DBCColumnMetaData, List<DBCException>> errors = new HashMap<DBCColumnMetaData, List<DBCException>>();

    ResultSetDataReceiver(ResultSetViewer resultSetViewer)
    {
        this.resultSetViewer = resultSetViewer;
        this.display = resultSetViewer.getControl().getShell().getDisplay();
    }

    boolean isHasMoreData() {
        return hasMoreData;
    }

    void setHasMoreData(boolean hasMoreData) {
        this.hasMoreData = hasMoreData;
    }

    boolean isNextSegmentRead() {
        return nextSegmentRead;
    }

    void setNextSegmentRead(boolean nextSegmentRead) {
        this.nextSegmentRead = nextSegmentRead;
    }

    public void fetchStart(DBCExecutionContext context, DBCResultSet resultSet)
        throws DBCException
    {
        rows.clear();

        if (!nextSegmentRead) {
            // Get columns metadata
            DBCResultSetMetaData metaData = resultSet.getResultSetMetaData();

            List<DBCColumnMetaData> rsColumns = metaData.getColumns();
            columnsCount = rsColumns.size();

            // Determine type handlers for all columns
            DBPDataSource dataSource = resultSet.getContext().getDataSource();

            // Extrat column info
            metaColumns = new DBDColumnBinding[columnsCount];
            for (int i = 0; i < columnsCount; i++) {
                metaColumns[i] = DBUtils.getColumnBinding(context, rsColumns.get(i));
            }

            resultSetViewer.setColumnsInfo(metaColumns);
        }
    }

    public void fetchRow(DBCExecutionContext context, DBCResultSet resultSet)
        throws DBCException
    {
        Object[] row = new Object[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            try {
                row[i] = metaColumns[i].getValueHandler().getValueObject(
                    context,
                    resultSet,
                    metaColumns[i].getColumn(),
                    i);
            }
            catch (DBCException e) {
                // Do not reports the same error multiple times
                // There are a lot of error could occur during result set fetch
                // We report certain error only once
                List<DBCException> errorList = errors.get(metaColumns[i].getColumn());
                if (errorList == null) {
                    errorList = new ArrayList<DBCException>();
                    errors.put(metaColumns[i].getColumn(), errorList);
                }
                if (!errorList.contains(e)) {
                    log.warn("Could not read column '" + metaColumns[i].getColumn().getName() + "' value", e);
                    errorList.add(e);
                }
            }
        }
        rows.add(row);
    }

    public void fetchEnd(DBCExecutionContext context)
        throws DBCException
    {
        if (!nextSegmentRead) {
            // Read locators metadata
            DBUtils.findValueLocators(context.getProgressMonitor(), metaColumns);
        }

        display.syncExec(new Runnable() {
            public void run()
            {
                if (!nextSegmentRead) {
                    resultSetViewer.setData(rows);
                } else {
                    resultSetViewer.appendData(rows);
                }

                // Check for more data
                hasMoreData = rows.size() >= resultSetViewer.getSegmentMaxRows();
                nextSegmentRead = false;

                errors.clear();
                rows = new ArrayList<Object[]>();
            }
        });
    }

}
