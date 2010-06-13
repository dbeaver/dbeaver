/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

/**
 * JDBCUtils
 */
public class JDBCUtils
{
    static Log log = LogFactory.getLog(JDBCUtils.class);

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

    /**
     * Preapres jdbc statement using specified connector.
     * Adds blocking object to monitor.
     * Begins task in monitor.
     * Adds record to Query console.
     * @param monitor progress monitor (can not be null)
     * @param connector connection provider
     * @param query SQL query
     * @param taskName task name
     * @return prepared statement
     * @throws SQLException may be throws by underslying JDBC driver
     */
    public static PreparedStatement prepareStatement(
        DBRProgressMonitor monitor,
        JDBCConnector connector,
        String query,
        String taskName)
        throws SQLException
    {
        PreparedStatement dbStat = connector.getConnection().prepareStatement(query);
        monitor.startBlock(makeBlockingObject(dbStat), taskName);
        return dbStat;
    }

    /**
     * Closes specified statement.
     * This statement MUST be prepared with {@link #prepareStatement} method.
     * @param monitor progress monitor (the same as in prepareStatement)
     * @param statement statement
     */
    public static void closeStatement(
        DBRProgressMonitor monitor,
        PreparedStatement statement)
    {
        safeClose(statement);
        monitor.endBlock();
    }

    public static void safeClose(PreparedStatement statement)
    {
        try {
            statement.close();
        }
        catch (SQLException e) {
            log.error("Could not close statement", e);
        }
    }

    public static void safeClose(
        ResultSet resultSet)
    {
        try {
            resultSet.close();
        }
        catch (SQLException e) {
            log.error("Could not close result set", e);
        }
    }

    public static void startConnectionBlock(
        DBRProgressMonitor monitor,
        JDBCConnector connector,
        String taskName)
    {
        monitor.startBlock(makeBlockingObject(connector.getConnection()), taskName);
    }

    public static void endConnectionBlock(
        DBRProgressMonitor monitor)
    {
        monitor.endBlock();
    }

    public static DBRBlockingObject makeBlockingObject(final Statement statement)
    {
        return new DBRBlockingObject() {
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
        };
    }

    private static DBRBlockingObject makeBlockingObject(final Connection connection)
    {
        return new DBRBlockingObject() {
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
        };
    }

}