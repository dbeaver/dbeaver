package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.runtime.sql.SQLQueryDataPump;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.eclipse.swt.widgets.Display;

import java.util.List;
import java.util.ArrayList;

/**
 * Data pump for SQL queries
 */
class ResultSetDataPump implements SQLQueryDataPump {

    private ResultSetViewer resultSetViewer;
    private Display display;
    private DBCResultSetMetaData metaData;
    private List<Object[]> rows = new ArrayList<Object[]>();

    ResultSetDataPump(ResultSetViewer resultSetViewer)
    {
        this.resultSetViewer = resultSetViewer;
        this.display = resultSetViewer.getControl().getShell().getDisplay();
    }

    public void fetchStart(DBCResultSet resultSet)
        throws DBCException
    {
        metaData = resultSet.getMetaData();
        rows.clear();
    }

    public void fetchRow(DBCResultSet resultSet)
        throws DBCException
    {
        int columnsCount = metaData.getColumns().size();
        Object[] row = new Object[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            row[i] = resultSet.getObject(i + 1);
            if (resultSet.wasNull()) {
                row[i] = null;
            }
        }
        rows.add(row);
    }

    public void fetchEnd(DBCResultSet resultSet)
        throws DBCException
    {
        display.asyncExec(new Runnable() {
            public void run()
            {
                resultSetViewer.setData(metaData, rows);
            }
        });
    }
}
