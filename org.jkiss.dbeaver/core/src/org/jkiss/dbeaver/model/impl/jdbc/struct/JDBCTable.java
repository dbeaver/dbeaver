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
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.Collection;
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
        return DATA_INSERT | DATA_UPDATE | DATA_DELETE;
    }

    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
        return getColumns(monitor);
    }

    public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
        return getColumn(monitor, childName);
    }

    public int readData(DBCExecutionContext context, DBDDataReceiver dataReceiver, int firstRow, int maxRows)
        throws DBException
    {
        if (!(context instanceof JDBCExecutionContext)) {
            throw new IllegalArgumentException("Bad execution context");
        }
        boolean hasLimits = firstRow >= 0 && maxRows > 0;

        JDBCExecutionContext jdbcContext = (JDBCExecutionContext)context;
        readRequiredMeta(context.getProgressMonitor());

        DBCQueryTransformer limitTransformer = null;
        if (hasLimits && getDataSource() instanceof DBCQueryTransformProvider) {
            limitTransformer = ((DBCQueryTransformProvider) getDataSource()).createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
        }
        boolean scrollWithQuery = limitTransformer != null;

        String query = "SELECT * FROM " + getFullQualifiedName();
        if (hasLimits && scrollWithQuery) {
            limitTransformer.setParameters(firstRow, maxRows);
            query = limitTransformer.transformQueryString(query);
        }
        boolean fetchStarted = false;
        JDBCStatement dbStat = jdbcContext.prepareStatement(query, false, false, false);
        try {
            dbStat.setDataContainer(this);
            if (hasLimits) {
                if (!scrollWithQuery) {
                    dbStat.setLimit(firstRow, maxRows);
                } else {
                    limitTransformer.transformStatement(dbStat, 0);
                }
            }
            if (!dbStat.executeStatement()) {
                return 0;
            }
            JDBCResultSet dbResult = dbStat.openResultSet();
            if (dbResult == null) {
                return 0;
            }
            try {
                dataReceiver.fetchStart(context.getProgressMonitor(), dbResult);
                fetchStarted = true;

                int rowCount = 0;
                while (dbResult.nextRow()) {
                    if (hasLimits && rowCount >= maxRows) {
                        // Fetch not more than max rows
                        break;
                    }
                    dataReceiver.fetchRow(context.getProgressMonitor(), dbResult);
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
                dataReceiver.fetchEnd(context.getProgressMonitor());
            }
        }
    }

    public int insertData(DBCExecutionContext context, List<DBDColumnValue> columns, DBDDataReceiver keysReceiver)
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
            int rowCount = dbStat.getUpdateRowCount();
            if (keysReceiver != null) {
                readKeys(context, dbStat, keysReceiver);
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
            int rowCount = dbStat.getUpdateRowCount();
            if (keysReceiver != null) {
                readKeys(context, dbStat, keysReceiver);
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
