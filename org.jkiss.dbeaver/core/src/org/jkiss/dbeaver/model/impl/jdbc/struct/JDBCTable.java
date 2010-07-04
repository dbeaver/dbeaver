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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;

import java.util.List;
import java.util.ArrayList;

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

    public int readData(DBRProgressMonitor monitor, DBDDataReciever dataReciever, int firstRow, int maxRows)
        throws DBException
    {
        if (firstRow > 0) {
            throw new DBException("First row number must be 0");
        }
        DBCExecutionContext context = getDataSource().openContext(monitor, "Query table " + getFullQualifiedName() + " data");
        try {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM ").append(getFullQualifiedName());
            DBCStatement dbStat = context.prepareStatement(query.toString(), false, false, false);
            try {
                dbStat.setDataContainer(this);
                if (maxRows > 0) {
                    dbStat.setLimit(0, maxRows);
                }
                if (!dbStat.executeStatement()) {
                    return 0;
                }
                DBCResultSet dbResult = dbStat.openResultSet();
                if (dbResult == null) {
                    return 0;
                }
                try {
                    dataReciever.fetchStart(monitor, dbResult);
                    try {
                        int rowCount = 0;
                        while (dbResult.nextRow()) {
                            dataReciever.fetchRow(monitor, dbResult);
                            rowCount++;
                        }
                        return rowCount;
                    }
                    finally {
                        dataReciever.fetchEnd(monitor);
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
        finally {
            context.close();
        }
    }

    public int insertData(DBRProgressMonitor monitor, List<DBDColumnValue> columns, DBDDataReciever keysReciever)
        throws DBException
    {
        DBCExecutionContext context = getDataSource().openContext(monitor, "Insert data in table " + getFullQualifiedName() + "");
        try {
            // Make query
            StringBuilder query = new StringBuilder();
            query.append("INSERT INTO ").append(getFullQualifiedName());

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
                for (int i = 0; i < columns.size(); i++) {
                    DBDColumnValue column = columns.get(i);
                    if (DBUtils.isNullValue(column.getValue())) {
                        continue;
                    }
                    DBDValueHandler valueHandler = DBUtils.getColumnValueHandler(getDataSource(), column.getColumn());
                    valueHandler.bindValueObject(dbStat, column.getColumn(), i, column.getValue());
                }

                // Execute statement
                dbStat.executeStatement();
                int rowCount = dbStat.getUpdateRowCount();
                if (keysReciever != null) {
                    readKeys(monitor, dbStat, keysReciever);
                }
                return rowCount;
            }
            finally {
                dbStat.close();
            }

        }
        finally {
            context.close();
        }
    }

    public int updateData(
        DBRProgressMonitor monitor,
        List<DBDColumnValue> keyColumns,
        List<DBDColumnValue> updateColumns,
        DBDDataReciever keysReciever)
        throws DBException
    {
        DBCExecutionContext context = getDataSource().openContext(monitor, "Update data in table " + getFullQualifiedName() + "");
        try {
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
                    valueHandler.bindValueObject(dbStat, column.getColumn(), i, column.getValue());
                }

                // Execute statement
                dbStat.executeStatement();
                int rowCount = dbStat.getUpdateRowCount();
                if (keysReciever != null) {
                    readKeys(monitor, dbStat, keysReciever);
                }
                return rowCount;
            }
            finally {
                dbStat.close();
            }

        }
        finally {
            context.close();
        }
    }

    public int deleteData(DBRProgressMonitor monitor, List<DBDColumnValue> keyColumns)
        throws DBException
    {
        DBCExecutionContext context = getDataSource().openContext(monitor, "Update data in table " + getFullQualifiedName() + "");
        try {
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
                    valueHandler.bindValueObject(dbStat, column.getColumn(), i, column.getValue());
                }

                // Execute statement
                dbStat.executeStatement();
                return dbStat.getUpdateRowCount();
            }
            finally {
                dbStat.close();
            }

        }
        finally {
            context.close();
        }
    }

    private void readKeys(DBRProgressMonitor monitor, DBCStatement dbStat, DBDDataReciever keysReciever)
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
            keysReciever.fetchStart(monitor, dbResult);
            try {
                while (dbResult.nextRow()) {
                    keysReciever.fetchRow(monitor, dbResult);
                }
            }
            finally {
                keysReciever.fetchEnd(monitor);
            }
        }
        finally {
            dbResult.close();
        }
    }

}
