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
import org.jkiss.dbeaver.model.data.DBDDataReciever;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data pump for SQL queries
 */
class ResultSetDataReciever implements DBDDataReciever {

    static Log log = LogFactory.getLog(ResultSetDataReciever.class);

    private ResultSetViewer resultSetViewer;
    private Display display;
    private int columnsCount;
    private DBDColumnBinding[] metaColumns;
    private List<Object[]> rows = new ArrayList<Object[]>();

    Map<DBCColumnMetaData, List<DBCException>> errors = new HashMap<DBCColumnMetaData, List<DBCException>>();

    ResultSetDataReciever(ResultSetViewer resultSetViewer)
    {
        this.resultSetViewer = resultSetViewer;
        this.display = resultSetViewer.getControl().getShell().getDisplay();
    }

    public void fetchStart(DBRProgressMonitor monitor, DBCResultSet resultSet)
        throws DBCException
    {
        rows.clear();
        DBCResultSetMetaData metaData = resultSet.getResultSetMetaData();

        List<DBCColumnMetaData> rsColumns = metaData.getColumns();
        columnsCount = rsColumns.size();

        // Determine type handlers for all columns
        DBPDataSource dataSource = resultSet.getContext().getDataSource();

        // Extrat column info
        metaColumns = new DBDColumnBinding[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            metaColumns[i] = DBUtils.getColumnBinding(dataSource, rsColumns.get(i));
        }

        resultSetViewer.setColumnsInfo(metaColumns);
    }

    public void fetchRow(DBRProgressMonitor monitor, DBCResultSet resultSet)
        throws DBCException
    {
        Object[] row = new Object[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            try {
                row[i] = metaColumns[i].getValueHandler().getValueObject(
                    monitor, resultSet,
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

    public void fetchEnd(DBRProgressMonitor monitor)
        throws DBCException
    {
        DBUtils.findValueLocators(monitor, metaColumns);

        display.asyncExec(new Runnable() {
            public void run()
            {
                resultSetViewer.setData(rows);
            }
        });
        errors.clear();
    }

}
