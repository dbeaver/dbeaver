package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.runtime.sql.SQLQueryDataPump;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.data.JDBCUnsupportedValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderDescriptor;
import org.eclipse.swt.widgets.Display;

import java.util.List;
import java.util.ArrayList;

/**
 * Data pump for SQL queries
 */
class ResultSetDataPump implements SQLQueryDataPump {

    private static DBDValueHandler DEFAULT_VALUE_HANDLER = JDBCUnsupportedValueHandler.INSTANCE;

    private ResultSetViewer resultSetViewer;
    private Display display;
    private int columnsCount;
    private ResultSetColumn[] metaColumns;
    private List<Object[]> rows = new ArrayList<Object[]>();

    ResultSetDataPump(ResultSetViewer resultSetViewer)
    {
        this.resultSetViewer = resultSetViewer;
        this.display = resultSetViewer.getControl().getShell().getDisplay();
    }

    public void fetchStart(DBCResultSet resultSet)
        throws DBCException
    {
        rows.clear();
        DBCResultSetMetaData metaData = resultSet.getMetaData();

        List<DBCColumnMetaData> rsColumns = metaData.getColumns();
        columnsCount = rsColumns.size();

        // Determine type handlers for all columns
        DataSourceRegistry dsRegistry = DataSourceRegistry.getDefault();
        DBPDataSource dataSource = resultSet.getStatement().getSession().getDataSource();

        metaColumns = new ResultSetColumn[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            DBCColumnMetaData columnMeta = rsColumns.get(i);
            DBDValueHandler typeHandler = null;
            DataTypeProviderDescriptor typeProvider = dsRegistry.getDataTypeProvider(dataSource, columnMeta);
            if (typeProvider != null) {
                typeHandler = typeProvider.getInstance().getHandler(dataSource, columnMeta);
            }
            if (typeHandler == null) {
                typeHandler = DEFAULT_VALUE_HANDLER;
            }
            metaColumns[i] = new ResultSetColumn(columnMeta, typeHandler);
        }
        resultSetViewer.setColumnsInfo(metaColumns);
    }

    public void fetchRow(DBCResultSet resultSet)
        throws DBCException
    {
        Object[] row = new Object[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            row[i] = metaColumns[i].valueHandler.getValueObject(resultSet, i + 1);
        }
        rows.add(row);
    }

    public void fetchEnd(DBCResultSet resultSet)
        throws DBCException
    {
        display.asyncExec(new Runnable() {
            public void run()
            {
                resultSetViewer.setData(rows);
            }
        });
    }
}
