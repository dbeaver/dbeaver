/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.dbc.*;
import org.jkiss.dbeaver.model.impl.struct.AbstractTable;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC abstract table mplementation
 */
public abstract class JDBCTable<DATASOURCE extends DBPDataSource, CONTAINER extends DBSEntityContainer>
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
        return DATA_COUNT | DATA_INSERT | DATA_UPDATE | DATA_DELETE;
    }

    public long readData(DBCExecutionContext context, DBDDataReceiver dataReceiver, long firstRow, long maxRows)
        throws DBException
    {
        if (!(context instanceof JDBCExecutionContext)) {
            throw new IllegalArgumentException("Bad execution context");
        }
        boolean hasLimits = firstRow >= 0 && maxRows > 0;

        JDBCExecutionContext jdbcContext = (JDBCExecutionContext)context;
        DBRProgressMonitor monitor = context.getProgressMonitor();
        readRequiredMeta(monitor);

        DBCQueryTransformer limitTransformer = null, fetchAllTransformer = null;
        if (getDataSource() instanceof DBCQueryTransformProvider) {
            if (hasLimits) {
                limitTransformer = ((DBCQueryTransformProvider) getDataSource()).createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
            } else {
                fetchAllTransformer = ((DBCQueryTransformProvider) getDataSource()).createQueryTransformer(DBCQueryTransformType.FETCH_ALL_TABLE);
            }
        }

        String query = "SELECT * FROM " + getFullQualifiedName();
        if (hasLimits && limitTransformer != null) {
            limitTransformer.setParameters(firstRow, maxRows);
            query = limitTransformer.transformQueryString(query);
        } else if (fetchAllTransformer != null) {
            query = fetchAllTransformer.transformQueryString(query);
        }
        boolean fetchStarted = false;
        monitor.subTask("Fetch table data");
        JDBCStatement dbStat = jdbcContext.prepareStatement(query, false, false, false);
        try {
            dbStat.setDataContainer(this);
            if (hasLimits) {
                if (limitTransformer == null) {
                    dbStat.setLimit(firstRow, maxRows);
                } else {
                    limitTransformer.transformStatement(dbStat, 0);
                }
            } else if (fetchAllTransformer != null) {
                fetchAllTransformer.transformStatement(dbStat, 0);
            }
            if (!dbStat.executeStatement()) {
                return 0;
            }
            JDBCResultSet dbResult = dbStat.openResultSet();
            if (dbResult == null) {
                return 0;
            }
            try {
                dataReceiver.fetchStart(monitor, dbResult);
                fetchStarted = true;

                long rowCount = 0;
                while (dbResult.nextRow()) {
                    if (monitor.isCanceled() || (hasLimits && rowCount >= maxRows)) {
                        // Fetch not more than max rows
                        break;
                    }
                    dataReceiver.fetchRow(monitor, dbResult);
                    rowCount++;
                    if (rowCount % 100 == 0) {
                        monitor.subTask(rowCount + " rows fetched");
                        monitor.worked(100);
                    }

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
                dataReceiver.fetchEnd(monitor);
            }
        }
    }

    public long readDataCount(DBCExecutionContext context) throws DBException
    {
        if (!(context instanceof JDBCExecutionContext)) {
            throw new IllegalArgumentException("Bad execution context");
        }
        JDBCExecutionContext jdbcContext = (JDBCExecutionContext)context;
        DBRProgressMonitor monitor = context.getProgressMonitor();

        String query = "SELECT COUNT(*) FROM " + getFullQualifiedName();
        monitor.subTask("Fetch table row count");
        JDBCStatement dbStat = jdbcContext.prepareStatement(query, false, false, false);
        try {
            dbStat.setDataContainer(this);
            if (!dbStat.executeStatement()) {
                return 0;
            }
            JDBCResultSet dbResult = dbStat.openResultSet();
            if (dbResult == null) {
                return 0;
            }
            try {
                if (dbResult.nextRow()) {
                    try {
                        return dbResult.getLong(1);
                    } catch (SQLException e) {
                        throw new DBException(e);
                    }
                } else {
                    return 0;
                }
            }
            finally {
                dbResult.close();
            }
        }
        finally {
            dbStat.close();
        }
    }

    public long insertData(DBCExecutionContext context, List<DBDColumnValue> columns, DBDDataReceiver keysReceiver)
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
        DBCStatement dbStat = context.prepareStatement(query.toString(), false, false, keysReceiver != null);
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
            long rowCount = dbStat.getUpdateRowCount();
            if (keysReceiver != null) {
                readKeys(context, dbStat, keysReceiver);
            }
            return rowCount;
        }
        finally {
            dbStat.close();
        }

    }

    public long updateData(
        DBCExecutionContext context,
        List<DBDColumnValue> keyColumns,
        List<DBDColumnValue> updateColumns,
        DBDDataReceiver keysReceiver)
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
        DBCStatement dbStat = context.prepareStatement(query.toString(), false, false, keysReceiver != null);
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
            long rowCount = dbStat.getUpdateRowCount();
            if (keysReceiver != null) {
                readKeys(context, dbStat, keysReceiver);
            }
            return rowCount;
        }
        finally {
            dbStat.close();
        }
    }

    public long deleteData(DBCExecutionContext context, List<DBDColumnValue> keyColumns)
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

    private void readKeys(DBCExecutionContext context, DBCStatement dbStat, DBDDataReceiver keysReceiver)
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
            keysReceiver.fetchStart(context.getProgressMonitor(), dbResult);
            try {
                while (dbResult.nextRow()) {
                    keysReceiver.fetchRow(context.getProgressMonitor(), dbResult);
                }
            }
            finally {
                keysReceiver.fetchEnd(context.getProgressMonitor());
            }
        }
        finally {
            dbResult.close();
        }
    }

    /**
     * Reads and caches metadata which is required for data requests
     * @param monitor progress monitor
     * @throws DBException on error
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
