/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.DBCDefaultValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueLocator;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.model.dbc.*;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSUtils;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderDescriptor;
import org.jkiss.dbeaver.runtime.sql.ISQLQueryDataPump;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data pump for SQL queries
 */
class ResultSetDataPump implements ISQLQueryDataPump {

    static Log log = LogFactory.getLog(ResultSetDataPump.class);

    private ResultSetViewer resultSetViewer;
    private Display display;
    private int columnsCount;
    private DBDColumnBinding[] metaColumns;
    private List<Object[]> rows = new ArrayList<Object[]>();

    Map<DBCColumnMetaData, List<DBCException>> errors = new HashMap<DBCColumnMetaData, List<DBCException>>();

    ResultSetDataPump(ResultSetViewer resultSetViewer)
    {
        this.resultSetViewer = resultSetViewer;
        this.display = resultSetViewer.getControl().getShell().getDisplay();
    }

    public void fetchStart(DBCResultSet resultSet, DBRProgressMonitor monitor)
        throws DBCException
    {
        rows.clear();
        DBCResultSetMetaData metaData = resultSet.getResultSetMetaData();

        List<DBCColumnMetaData> rsColumns = metaData.getColumns();
        columnsCount = rsColumns.size();

        // Determine type handlers for all columns
        DataSourceRegistry dsRegistry = DataSourceRegistry.getDefault();
        DBPDataSource dataSource = resultSet.getContext().getDataSource();

        // Extrat column info
        metaColumns = new DBDColumnBinding[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            metaColumns[i] = DBSUtils.getColumnBinding(dataSource, rsColumns.get(i));
        }

        resultSetViewer.setColumnsInfo(metaColumns);
    }

    public void fetchRow(DBCResultSet resultSet, DBRProgressMonitor monitor)
        throws DBCException
    {
        Object[] row = new Object[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            try {
                row[i] = metaColumns[i].getValueHandler().getValueObject(
                    resultSet,
                    metaColumns[i].getMetaData(),
                    i);
            }
            catch (DBCException e) {
                // Do not reports the same error multiple times
                // There are a lot of error could occur during result set fetch
                // We report certain error only once
                List<DBCException> errorList = errors.get(metaColumns[i].getMetaData());
                if (errorList == null) {
                    errorList = new ArrayList<DBCException>();
                    errors.put(metaColumns[i].getMetaData(), errorList);
                }
                if (!errorList.contains(e)) {
                    log.warn("Could not read column '" + metaColumns[i].getMetaData().getColumnName() + "' value", e);
                    errorList.add(e);
                }
            }
        }
        rows.add(row);
    }

    public void fetchEnd(DBRProgressMonitor monitor)
        throws DBCException
    {
        DBSUtils.findValueLocators(monitor, metaColumns);

        display.asyncExec(new Runnable() {
            public void run()
            {
                resultSetViewer.setData(rows);
            }
        });
        errors.clear();
    }

}
