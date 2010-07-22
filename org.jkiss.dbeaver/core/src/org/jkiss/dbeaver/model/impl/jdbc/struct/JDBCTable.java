/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.data.DBDDataReciever;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.impl.meta.AbstractTable;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * JDBC abstract table mplementation
 */
public abstract class JDBCTable<DATASOURCE extends DBPDataSource, CONTAINER extends DBSStructureContainer>
    extends AbstractTable<DATASOURCE, CONTAINER>
    implements DBSDataContainer
{

    protected JDBCTable(CONTAINER container)
    {
        super(container);
    }

    protected JDBCTable(CONTAINER container, String tableName, String tableType, String description)
    {
        super(container, tableName, tableType, description);
    }

    public int getSupportedFeatures()
    {
        return DATA_INSERT | DATA_UPDATE | DATA_DELETE;
    }

    public int readData(DBCExecutionContext context, DBDDataReciever dataReciever, int firstRow, int maxRows)
        throws DBException
    {
        if (!(context instanceof JDBCExecutionContext)) {
            throw new IllegalArgumentException("Bad execution context");
        }
        JDBCExecutionContext jdbcContext = (JDBCExecutionContext)context;
        readRequiredMeta(context.getProgressMonitor());

        String query = "SELECT * FROM " + getFullQualifiedName();
        if (this instanceof JDBCScrollableTable) {
            query = ((JDBCScrollableTable)this).makeScrollableQuery(query, firstRow, maxRows);
        }
        boolean fetchStarted = false;
        JDBCStatement dbStat = jdbcContext.prepareStatement(query.toString(), false, false, false);
        try {
            dbStat.setDataContainer(this);
            if (!(this instanceof JDBCScrollableTable)) {
                dbStat.setLimit(firstRow, maxRows);
            }
            if (!dbStat.executeStatement()) {
                return 0;
            }
            JDBCResultSet dbResult = dbStat.openResultSet();
            if (dbResult == null) {
                return 0;
            }
            try {
                dataReciever.fetchStart(context.getProgressMonitor(), dbResult);
                fetchStarted = true;

                int rowCount = 0;
                while (dbResult.nextRow()) {
                    if (rowCount >= maxRows) {
                        // Fetch not more than max rows
                        break;
                    }
                    dataReciever.fetchRow(context.getProgressMonitor(), dbResult);
                    rowCount++;
                }
                return rowCount;
            }
            finally {
                dbResult.close();
            }
        }
        finally {
            dbStat.close();
            if (fetchStarted) {
                dataReciever.fetchEnd(context.getProgressMonitor());
            }
        }
    }

    public int insertData(DBCExecutionContext context, List<DBDColumnValue> columns, DBDDataReciever keysReciever)
        throws DBException
    {
        readRequiredMeta(context.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ").append(getFullQualifiedName()).append(" (");

        boolean hasKey = false;
        for (DBDColumnValue column : columns) {
            if (DBUtils.isNullValue(column.getValue())) {
                // do not use null values
                continue;
            }
            if (hasKey) query.append(",");
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), column.getColumn().getName()));
        }
        query.append(") VALUES (");
        hasKey = false;
        for (DBDColumnValue column1 : columns) {
            if (DBUtils.isNullValue(column1.getValue())) {
                continue;
            }
            if (hasKey) query.append(",");
            hasKey = true;
            query.append("?");
        }
        query.append(")");

        // Execute
        DBCStatement dbStat = context.prepareStatement(query.toString(), false, false, keysReciever != null);
        try {
            dbStat.setDataContainer(this);

            // Set parameters
            int paramNum = 0;
            for (DBDColumnValue column : columns) {
                if (DBUtils.isNullValue(column.getValue())) {
                    continue;
                }
                DBDValueHandler valueHandler = DBUtils.getColumnValueHandler(getDataSource(), column.getColumn());
                valueHandler.bindValueObject(context.getProgressMonitor(), dbStat, column.getColumn(), paramNum++, column.getValue());
            }

            // Execute statement
            dbStat.executeStatement();
            int rowCount = dbStat.getUpdateRowCount();
            if (keysReciever != null) {
                readKeys(context, dbStat, keysReciever);
            }
            return rowCount;
        }
        finally {
            dbStat.close();
        }

    }

    public int updateData(
        DBCExecutionContext context,
        List<DBDColumnValue> keyColumns,
        List<DBDColumnValue> updateColumns,
        DBDDataReciever keysReciever)
        throws DBException
    {
        readRequiredMeta(context.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder();
        query.append("UPDATE ").append(getFullQualifiedName()).append(" SET ");

        boolean hasKey = false;
        for (DBDColumnValue column : updateColumns) {
            if (hasKey) query.append(",");
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), column.getColumn().getName())).append("=?");
        }
        query.append(" WHERE ");
        hasKey = false;
        for (DBDColumnValue column : keyColumns) {
            if (hasKey) query.append(" AND ");
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), column.getColumn().getName())).append("=?");
        }

        // Execute
        DBCStatement dbStat = context.prepareStatement(query.toString(), false, false, keysReciever != null);
        try {
            dbStat.setDataContainer(this);

            // Set parameters
            List<DBDColumnValue> allColumn = new ArrayList<DBDColumnValue>(updateColumns.size() + keyColumns.size());
            allColumn.addAll(updateColumns);
            allColumn.addAll(keyColumns);
            for (int i = 0; i < allColumn.size(); i++) {
                DBDColumnValue column = allColumn.get(i);
                DBDValueHandler valueHandler = DBUtils.getColumnValueHandler(getDataSource(), column.getColumn());
                valueHandler.bindValueObject(context.getProgressMonitor(), dbStat, column.getColumn(), i, column.getValue());
            }

            // Execute statement
            dbStat.executeStatement();
            int rowCount = dbStat.getUpdateRowCount();
            if (keysReciever != null) {
                readKeys(context, dbStat, keysReciever);
            }
            return rowCount;
        }
        finally {
            dbStat.close();
        }
    }

    public int deleteData(DBCExecutionContext context, List<DBDColumnValue> keyColumns)
        throws DBException
    {
        readRequiredMeta(context.getProgressMonitor());

        // Make query
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ").append(getFullQualifiedName()).append(" WHERE ");

        boolean hasKey = false;
        for (DBDColumnValue column : keyColumns) {
            if (hasKey) query.append(" AND ");
            hasKey = true;
            query.append(DBUtils.getQuotedIdentifier(getDataSource(), column.getColumn().getName())).append("=?");
        }

        // Execute
        DBCStatement dbStat = context.prepareStatement(query.toString(), false, false, false);
        try {
            dbStat.setDataContainer(this);

            // Set parameters
            for (int i = 0; i < keyColumns.size(); i++) {
                DBDColumnValue column = keyColumns.get(i);
                DBDValueHandler valueHandler = DBUtils.getColumnValueHandler(getDataSource(), column.getColumn());
                valueHandler.bindValueObject(context.getProgressMonitor(), dbStat, column.getColumn(), i, column.getValue());
            }

            // Execute statement
            dbStat.executeStatement();
            return dbStat.getUpdateRowCount();
        }
        finally {
            dbStat.close();
        }
    }

    private void readKeys(DBCExecutionContext context, DBCStatement dbStat, DBDDataReciever keysReciever)
        throws DBCException
    {
        DBCResultSet dbResult;
        try {
            dbResult = dbStat.openGeneratedKeysResultSet();
        }
        catch (IncompatibleClassChangeError e) {
            return;
        }
        if (dbResult == null) {
            return;
        }
        try {
            keysReciever.fetchStart(context.getProgressMonitor(), dbResult);
            try {
                while (dbResult.nextRow()) {
                    keysReciever.fetchRow(context.getProgressMonitor(), dbResult);
                }
            }
            finally {
                keysReciever.fetchEnd(context.getProgressMonitor());
            }
        }
        finally {
            dbResult.close();
        }
    }

    /**
     * Reads and caches metadata which is required for data requests
     * @param monitor
     * @throws DBException
     */
    private void readRequiredMeta(DBRProgressMonitor monitor)
        throws DBException
    {
        try {
            getColumns(monitor);
        }
        catch (DBException e) {
            throw new DBException("Could not cache table columns", e);
        }
    }

}
