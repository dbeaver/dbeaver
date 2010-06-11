/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

/**
 * JDBCUtils
 */
public class JDBCUtils
{

    public static String safeGetString(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getString(columnName);
        } catch (SQLException e) {
            return null;
        }
    }

    public static int safeGetInt(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getInt(columnName);
        } catch (SQLException e) {
            return 0;
        }
    }

    public static long safeGetLong(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getLong(columnName);
        } catch (SQLException e) {
            return 0;
        }
    }

    public static boolean safeGetBoolean(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getBoolean(columnName);
        } catch (SQLException e) {
            return false;
        }
    }

    public static DBSDataKind getDataKind(int type)
    {
        switch (type) {
            case java.sql.Types.BOOLEAN:
                return DBSDataKind.BOOLEAN;
            case java.sql.Types.CHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.NVARCHAR:
            case java.sql.Types.LONGVARCHAR:
            case java.sql.Types.LONGNVARCHAR:
                return DBSDataKind.STRING;
            case java.sql.Types.BIGINT:
            case java.sql.Types.BIT:
            case java.sql.Types.DECIMAL:
            case java.sql.Types.DOUBLE:
            case java.sql.Types.FLOAT:
            case java.sql.Types.INTEGER:
            case java.sql.Types.NUMERIC:
            case java.sql.Types.REAL:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.TINYINT:
                return DBSDataKind.NUMERIC;
            case java.sql.Types.DATE:
            case java.sql.Types.TIME:
            case java.sql.Types.TIMESTAMP:
                return DBSDataKind.DATETIME;
            case java.sql.Types.BINARY:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
                return DBSDataKind.BINARY;
            case java.sql.Types.BLOB:
            case java.sql.Types.CLOB:
            case java.sql.Types.NCLOB:
                return DBSDataKind.LOB;
            case java.sql.Types.STRUCT:
                return DBSDataKind.STRUCT;
            case java.sql.Types.ARRAY:
                return DBSDataKind.ARRAY;
            case java.sql.Types.ROWID:
                // thread ROWID as string
                return DBSDataKind.STRING;
        }
        return DBSDataKind.UNKNOWN;
    }

    public static Object getParameter(ResultSet dbResult, int columnIndex, int columnType)
        throws DBCException
    {
        try {
            switch (columnType) {
                case java.sql.Types.BOOLEAN:
                case java.sql.Types.BIT:
                    try {
                        return dbResult.getByte(columnIndex);
                    } catch (SQLException e) {
                        // Try to get as int
                        return dbResult.getInt(columnIndex);
                    }
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.LONGVARCHAR:
                    return dbResult.getString(columnIndex);
                case java.sql.Types.BIGINT:
                case java.sql.Types.DECIMAL:
                    return dbResult.getBigDecimal(columnIndex);
                case java.sql.Types.DOUBLE:
                    return dbResult.getDouble(columnIndex);
                case java.sql.Types.FLOAT:
                    return dbResult.getFloat(columnIndex);
                case java.sql.Types.INTEGER:
                    return dbResult.getInt(columnIndex);
                case java.sql.Types.NUMERIC:
                case java.sql.Types.REAL:
                    return dbResult.getBigDecimal(columnIndex);
                case java.sql.Types.SMALLINT:
                    return dbResult.getShort(columnIndex);
                case java.sql.Types.TINYINT:
                    return dbResult.getByte(columnIndex);
                case java.sql.Types.DATE:
                    return dbResult.getDate(columnIndex);
                case java.sql.Types.TIME:
                    return dbResult.getTime(columnIndex);
                case java.sql.Types.TIMESTAMP:
                    return dbResult.getTimestamp(columnIndex);
                case java.sql.Types.BLOB:
                    return dbResult.getBlob(columnIndex);
                case java.sql.Types.CLOB:
                    return dbResult.getClob(columnIndex);
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    // Try to use BLOB wrapper
                    return dbResult.getBlob(columnIndex);
                case java.sql.Types.STRUCT:
                    return dbResult.getObject(columnIndex);
                case java.sql.Types.ARRAY:
                    return dbResult.getArray(columnIndex);
                default:
                    return new JDBCUnknownType(
                        columnType,
                        dbResult.getObject(columnIndex));
            }
        } catch (SQLException ex) {
            //throw new DBCException(ex);
            // get by parameter type failed - try to use generic getter
        }
        try {
            return dbResult.getObject(columnIndex);
        } catch (SQLException ex) {
            throw new DBCException(ex);
        }
    }


    public static void startBlockingOperation(
        DBRProgressMonitor monitor,
        JDBCConnector connector,
        String taskName)
    {
        startBlockingOperation(monitor, makeBlockingObject(connector.getConnection()), taskName, 1);
    }

    public static void startBlockingOperation(
        DBRProgressMonitor monitor,
        JDBCConnector connector,
        String taskName,
        int subTasks)
    {
        startBlockingOperation(monitor, makeBlockingObject(connector.getConnection()), taskName, subTasks);
    }

    public static void startBlockingOperation(
        DBRProgressMonitor monitor,
        Connection connection,
        String taskName)
    {
        startBlockingOperation(monitor, makeBlockingObject(connection), taskName, 1);
    }

    public static void startBlockingOperation(
        DBRProgressMonitor monitor,
        Statement statement,
        String taskName)
    {
        startBlockingOperation(monitor, makeBlockingObject(statement), taskName, 1);
    }

    public static void startBlockingOperation(
        DBRProgressMonitor monitor,
        Statement statement,
        String taskName,
        int subTasks)
    {
        startBlockingOperation(monitor, makeBlockingObject(statement), taskName, subTasks);
    }

    private static void startBlockingOperation(
        DBRProgressMonitor monitor,
        DBRBlockingObject operation,
        String taskName,
        int subTasks)
    {
        monitor.startBlock(operation);
        if (monitor.getBlockCount() > 1) {
            monitor.subTask(taskName);
        } else {
            monitor.beginTask(taskName, subTasks);
        }
    }

    public static void endBlockingOperation(
        DBRProgressMonitor monitor)
    {
        if (monitor.getBlockCount() == 1) {
            monitor.done();
        }
        monitor.endBlock();
    }

    public static DBRBlockingObject makeBlockingObject(Statement statement)
    {
        return new StatementBlockingObject(statement);
    }

    public static DBRBlockingObject makeBlockingObject(Connection connection)
    {
        return new ConnectionBlockingObject(connection);
    }

    private static class StatementBlockingObject implements DBRBlockingObject {
        private final Statement statement;

        public StatementBlockingObject(Statement statement)
        {
            this.statement = statement;
        }

        public void cancelBlock()
            throws DBException
        {
            try {
                statement.cancel();
            }
            catch (SQLException e) {
                throw new DBCException("Coud not cancel statement", e);
            }
        }
    }

    private static class ConnectionBlockingObject implements DBRBlockingObject {
        private final Connection connection;

        public ConnectionBlockingObject(Connection connection)
        {
            this.connection = connection;
        }

        public void cancelBlock()
            throws DBException
        {
            try {
                connection.close();
            }
            catch (SQLException e) {
                throw new DBCException("Coud not close connection", e);
            }
        }
    }

}