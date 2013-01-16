/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/**
 * JDBCUtils
 */
public class JDBCUtils {
    static final Log log = LogFactory.getLog(JDBCUtils.class);

    public static String safeGetString(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getString(columnName);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return null;
        }
    }

    public static String safeGetStringTrimmed(ResultSet dbResult, String columnName)
    {
        try {
            final String value = dbResult.getString(columnName);
            if (value != null && !value.isEmpty()) {
                return value.trim();
            } else {
                return value;
            }
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return null;
        }
    }

    public static String safeGetString(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getString(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(columnIndex, e);
            return null;
        }
    }

    public static String safeGetStringTrimmed(ResultSet dbResult, int columnIndex)
    {
        try {
            final String value = dbResult.getString(columnIndex);
            if (value != null && !value.isEmpty()) {
                return value.trim();
            } else {
                return value;
            }
        } catch (SQLException e) {
            debugColumnRead(columnIndex, e);
            return null;
        }
    }

    public static int safeGetInt(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getInt(columnName);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return 0;
        }
    }

    public static Integer safeGetInteger(ResultSet dbResult, String columnName)
    {
        try {
            final int result = dbResult.getInt(columnName);
            if (dbResult.wasNull()) {
                return null;
            } else {
                return result;
            }
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return null;
        }
    }

    public static long safeGetLong(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getLong(columnName);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return 0;
        }
    }

    public static Long safeGetLongNullable(ResultSet dbResult, String columnName)
    {
        try {
            final long result = dbResult.getLong(columnName);
            return dbResult.wasNull() ? null : result;
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return null;
        }
    }

    public static double safeGetDouble(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getDouble(columnName);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return 0.0;
        }
    }

    public static BigDecimal safeGetBigDecimal(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getBigDecimal(columnName);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return null;
        }
    }

    public static boolean safeGetBoolean(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getBoolean(columnName);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return false;
        }
    }

    public static boolean safeGetBoolean(ResultSet dbResult, String columnName, String trueValue)
    {
        try {
            final String strValue = dbResult.getString(columnName);
            return strValue != null && strValue.startsWith(trueValue);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return false;
        }
    }

    public static byte[] safeGetBytes(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getBytes(columnName);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return null;
        }
    }

    public static Timestamp safeGetTimestamp(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getTimestamp(columnName);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return null;
        }
    }

    public static Timestamp safeGetTimestamp(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getTimestamp(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(columnIndex, e);
            return null;
        }
    }

    public static Object safeGetObject(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getObject(columnName);
        } catch (SQLException e) {
            debugColumnRead(columnName, e);
            return null;
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

    private static DBRBlockingObject makeBlockingObject(final Connection connection)
    {
        return new DBRBlockingObject() {
            @Override
            public void cancelBlock()
                throws DBException
            {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new DBCException("Can't close connection", e);
                }
            }
        };
    }

    public static String normalizeIdentifier(String value)
    {
        return value == null ? null : value.trim();
    }

    public static void dumpResultSet(ResultSet dbResult)
    {
        try {
            ResultSetMetaData md = dbResult.getMetaData();
            int count = md.getColumnCount();
            dumpResultSetMetaData(dbResult);
            while (dbResult.next()) {
                for (int i = 1; i <= count; i++) {
                    String colValue = dbResult.getString(i);
                    System.out.print(colValue + "\t");
                }
                System.out.println();
            }
            System.out.println();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void dumpResultSetMetaData(ResultSet dbResult)
    {
        try {
            ResultSetMetaData md = dbResult.getMetaData();
            int count = md.getColumnCount();
            for (int i = 1; i <= count; i++) {
                System.out.print(md.getColumnName(i) + " [" + md.getColumnTypeName(i)+ "]\t");
            }
            System.out.println();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isConnectionAlive(Connection connection)
    {
        try {
            if (connection.isClosed()) {
                return false;
            }
            connection.getMetaData().getTables(null, null, "UN_EXIST_DBEAVER_TBL_NAME1978", null);
            return true;
        } catch (SQLException e) {
            log.debug(e);
            return false;
        }
    }

    public static void scrollResultSet(ResultSet dbResult, long offset)
        throws SQLException
    {
        // Scroll to first row
        boolean scrolled = false;
        try {
            scrolled = dbResult.absolute((int) offset);
        } catch (SQLException e) {
            // Seems to be not supported
            log.debug(e.getMessage());
        } catch (AbstractMethodError e) {
            // Seems to be legacy JDBC
            log.debug(e.getMessage());
        } catch (UnsupportedOperationException e) {
            // Seems to be legacy JDBC
            log.debug(e.getMessage());
        }
        if (!scrolled) {
            // Just fetch first 'firstRow' rows
            for (long i = 1; i <= offset; i++) {
                try {
                    dbResult.next();
                } catch (SQLException e) {
                    throw new SQLException("Could not scroll result set to " + offset + " row", e);
                }
            }
        }
    }

    public static void reportWarnings(JDBCExecutionContext context, SQLWarning rootWarning)
    {
        for (SQLWarning warning = rootWarning; warning != null; warning = warning.getNextWarning()) {
            log.warn(
                "SQL Warning (DataSource: " + context.getDataSource().getContainer().getName() +
                    "; Code: " + warning.getErrorCode() +
                    "; State: " + warning.getSQLState() + "): " +
                    warning.getLocalizedMessage());
        }
    }

    public static String limitQueryLength(String query, int maxLength)
    {
        return query == null || query.length() <= maxLength ? query : query.substring(0, maxLength);
    }

/*
    public static boolean isDriverODBC(DBCExecutionContext context)
    {
        return context.getDataSource().getContainer().getDriver().getDriverClassName().contains("Odbc");
    }
*/

    public static DBSForeignKeyModifyRule getCascadeFromNum(int num)
    {
        switch (num) {
            case DatabaseMetaData.importedKeyNoAction: return DBSForeignKeyModifyRule.NO_ACTION;
            case DatabaseMetaData.importedKeyCascade: return DBSForeignKeyModifyRule.CASCADE;
            case DatabaseMetaData.importedKeySetNull: return DBSForeignKeyModifyRule.SET_NULL;
            case DatabaseMetaData.importedKeySetDefault: return DBSForeignKeyModifyRule.SET_DEFAULT;
            case DatabaseMetaData.importedKeyRestrict: return DBSForeignKeyModifyRule.RESTRICT;
            default: return DBSForeignKeyModifyRule.UNKNOWN;
        }
    }

    public static void executeSQL(JDBCExecutionContext context, String sql) throws SQLException
    {
        final JDBCPreparedStatement dbStat = context.prepareStatement(sql);
        try {
            dbStat.execute();
        } finally {
            dbStat.close();
        }
    }

    public static String queryString(JDBCExecutionContext context, String sql, Object ... args) throws SQLException
    {
        final JDBCPreparedStatement dbStat = context.prepareStatement(sql);
        try {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    dbStat.setObject(i + 1, args[i]);
                }
            }
            JDBCResultSet resultSet = dbStat.executeQuery();
            try {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                } else {
                    return null;
                }
            } finally {
                resultSet.close();
            }
        } finally {
            dbStat.close();
        }
    }

    private static void debugColumnRead(String columnName, SQLException error)
    {
        log.debug("Can't get column '" + columnName + "': " + error.getMessage());
    }

    private static void debugColumnRead(int columnIndex, SQLException error)
    {
        log.debug("Can't get column #" + columnIndex + ": " + error.getMessage());
    }

    public static void appendFilterClause(StringBuilder sql, DBSObjectFilter filter, String columnAlias, boolean firstClause)
    {
        if (filter.isEmpty()) {
            return;
        }
        if (filter.hasSingleMask()) {
            firstClause = SQLUtils.appendFirstClause(sql, firstClause);
            sql.append(columnAlias);
            SQLUtils.appendLikeCondition(sql, filter.getSingleMask(), false);
            return;
        }
        List<String> include = filter.getInclude();
        if (!CommonUtils.isEmpty(include)) {
            firstClause = SQLUtils.appendFirstClause(sql, firstClause);
            sql.append("(");
            for (int i = 0, includeSize = include.size(); i < includeSize; i++) {
                if (i > 0) sql.append(" OR ");
                sql.append(columnAlias);
                SQLUtils.appendLikeCondition(sql, include.get(i), false);
            }
            sql.append(")");
        }
        List<String> exclude = filter.getExclude();
        if (!CommonUtils.isEmpty(exclude)) {
            SQLUtils.appendFirstClause(sql, firstClause);
            sql.append("NOT (");
            for (int i = 0, excludeSize = exclude.size(); i < excludeSize; i++) {
                if (i > 0) sql.append(" OR ");
                sql.append(columnAlias);
                SQLUtils.appendLikeCondition(sql, exclude.get(i), false);
            }
            sql.append(")");
        }
    }

    public static void setFilterParameters(JDBCPreparedStatement statement, int paramIndex, DBSObjectFilter filter) throws SQLException
    {
        if (filter.isEmpty()) {
            return;
        }
        for (String inc : CommonUtils.safeCollection(filter.getInclude())) {
            statement.setString(paramIndex++, inc);
        }
        for (String exc : CommonUtils.safeCollection(filter.getExclude())) {
            statement.setString(paramIndex++, exc);
        }
    }

    public static void rethrowSQLException(Throwable e) throws SQLException
    {
        if (e instanceof InvocationTargetException) {
            Throwable targetException = ((InvocationTargetException) e).getTargetException();
            if (targetException instanceof SQLException) {
                throw (SQLException)targetException;
            } else {
                throw new SQLException(targetException);
            }
        }
    }

}