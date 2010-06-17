/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueLocator;
import org.jkiss.dbeaver.model.dbc.*;
import org.jkiss.dbeaver.model.impl.data.JDBCUnsupportedValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTable;
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

    private static DBDValueHandler DEFAULT_VALUE_HANDLER = JDBCUnsupportedValueHandler.INSTANCE;

    static Log log = LogFactory.getLog(ResultSetDataPump.class);

    private ResultSetViewer resultSetViewer;
    private Display display;
    private int columnsCount;
    private ResultSetColumn[] metaColumns;
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

    public void fetchRow(DBCResultSet resultSet, DBRProgressMonitor monitor)
        throws DBCException
    {
        Object[] row = new Object[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            try {
                row[i] = metaColumns[i].valueHandler.getValueObject(
                    resultSet,
                    metaColumns[i].metaData,
                    i);
            }
            catch (DBCException e) {
                // Do not reports the same error multiple times
                // There are a lot of error could occur during result set fetch
                // We report certain error only once
                List<DBCException> errorList = errors.get(metaColumns[i].metaData);
                if (errorList == null) {
                    errorList = new ArrayList<DBCException>();
                    errors.put(metaColumns[i].metaData, errorList);
                }
                if (!errorList.contains(e)) {
                    log.warn("Could not read column '" + metaColumns[i].metaData.getColumnName() + "' value", e);
                    errorList.add(e);
                }
            }
        }
        rows.add(row);
    }

    public void fetchEnd(DBRProgressMonitor monitor)
        throws DBCException
    {
        extractRowIdentifiers(monitor);

        display.asyncExec(new Runnable() {
            public void run()
            {
                resultSetViewer.setData(rows);
            }
        });
        errors.clear();
    }

    // Find value locators for columns
    private void extractRowIdentifiers(DBRProgressMonitor monitor) {
        Map<DBSTable, DBDValueLocator> locatorMap = new HashMap<DBSTable, DBDValueLocator>();
        try {
            for (ResultSetColumn column : metaColumns) {
                DBCColumnMetaData meta = column.metaData;
                if (meta.getTable() == null || !meta.getTable().isIdentitied(monitor)) {
                    continue;
                }
                // We got table name and column name
                // To be editable we need this result set contain set of columns from the same table
                // which construct any unique key
                column.valueLocator = locatorMap.get(meta.getTable().getTable(monitor));
                if (column.valueLocator == null) {
                    DBCTableIdentifier tableIdentifier = meta.getTable().getBestIdentifier(monitor);
                    if (tableIdentifier == null) {
                        continue;
                    }
                    DBDValueLocator valueLocator = new DBDValueLocator(
                        meta.getTable().getTable(monitor),
                        tableIdentifier);
                    locatorMap.put(meta.getTable().getTable(monitor), valueLocator);
                    column.valueLocator = valueLocator;
                }
                column.editable = true;
            }
        }
        catch (DBException e) {
            log.error("Can't extract column identifier info", e);
        }
    }
}
