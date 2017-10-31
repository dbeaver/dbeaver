/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBCUtils
 */
public class JDBCUtils {
    private static final Log log = Log.getLog(JDBCUtils.class);
    public static final int CONNECTION_VALIDATION_TIMEOUT = 5000;

    private static final Map<String, Integer> badColumnNames = new HashMap<>();

    @Nullable
    public static String safeGetString(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getString(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
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
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static String safeGetString(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getString(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
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
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    public static int safeGetInt(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getInt(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return 0;
        }
    }

    public static int safeGetInt(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getInt(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return 0;
        }
    }

    @Nullable
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
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Integer safeGetInteger(ResultSet dbResult, int columnIndex)
    {
        try {
            final int result = dbResult.getInt(columnIndex);
            if (dbResult.wasNull()) {
                return null;
            } else {
                return result;
            }
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    public static long safeGetLong(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getLong(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return 0;
        }
    }

    public static long safeGetLong(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getLong(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return 0;
        }
    }

    @Nullable
    public static Long safeGetLongNullable(ResultSet dbResult, String columnName)
    {
        try {
            final long result = dbResult.getLong(columnName);
            return dbResult.wasNull() ? null : result;
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    public static double safeGetDouble(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getDouble(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return 0.0;
        }
    }

    public static double safeGetDouble(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getDouble(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return 0.0;
        }
    }

    public static float safeGetFloat(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getFloat(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return 0;
        }
    }

    public static float safeGetFloat(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getFloat(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return 0;
        }
    }

    @Nullable
    public static BigDecimal safeGetBigDecimal(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getBigDecimal(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static BigDecimal safeGetBigDecimal(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getBigDecimal(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    public static boolean safeGetBoolean(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getBoolean(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return false;
        }
    }

    public static boolean safeGetBoolean(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getBoolean(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return false;
        }
    }

    public static boolean safeGetBoolean(ResultSet dbResult, String columnName, String trueValue)
    {
        try {
            final String strValue = dbResult.getString(columnName);
            return strValue != null && strValue.startsWith(trueValue);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return false;
        }
    }

    @Nullable
    public static byte[] safeGetBytes(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getBytes(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Timestamp safeGetTimestamp(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getTimestamp(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Timestamp safeGetTimestamp(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getTimestamp(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static Date safeGetDate(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getDate(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Date safeGetDate(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getDate(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static Time safeGetTime(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getTime(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Time safeGetTime(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getTime(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static SQLXML safeGetXML(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getSQLXML(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static SQLXML safeGetXML(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getSQLXML(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static Object safeGetObject(ResultSet dbResult, String columnName)
    {
        try {
            return dbResult.getObject(columnName);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Object safeGetObject(ResultSet dbResult, int columnIndex)
    {
        try {
            return dbResult.getObject(columnIndex);
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static <T> T safeGetArray(ResultSet dbResult, String columnName)
    {
        try {
            Array array = dbResult.getArray(columnName);
            return array == null ? null : (T) array.getArray();
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Object safeGetArray(ResultSet dbResult, int columnIndex)
    {
        try {
            Array array = dbResult.getArray(columnIndex);
            return array == null ? null : array.getArray();
        } catch (SQLException e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static String normalizeIdentifier(@Nullable String value)
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
                System.out.print(md.getColumnName(i) + " [" + md.getColumnTypeName(i) + "]\t");
            }
            System.out.println();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isConnectionAlive(DBPDataSource dataSource, Connection connection)
    {
        try {
            if (connection == null || connection.isClosed()) {
                return false;
            }
        } catch (SQLException e) {
            log.debug(e);
            return false;
        }
        String testSQL = null;
        if (dataSource instanceof SQLDataSource) {
            testSQL = ((SQLDataSource) dataSource).getSQLDialect().getTestSQL();
        }
        try {
            if (!CommonUtils.isEmpty(testSQL)) {
                // Execute test SQL
                try (Statement dbStat = connection.createStatement()) {
                    dbStat.execute(testSQL);
                }
            } else {
                try {
                    return connection.isValid(CONNECTION_VALIDATION_TIMEOUT);
                } catch (Throwable e) {
                    // isValid may be unsupported by driver
                    // Let's try to read table list
                    connection.getMetaData().getTables(null, null, "DBEAVER_FAKE_TABLE_NAME_FOR_PING", null);
                }
            }
            return true;
        } catch (Throwable e1) {
            log.debug("Connection seems to be broken", e1);
            return false;
        }
    }

    public static void scrollResultSet(ResultSet dbResult, long offset, boolean forceFetch) throws SQLException
    {
        // Scroll to first row
        boolean scrolled = false;
        if (!forceFetch) {
            try {
                scrolled = dbResult.absolute((int) offset);
            } catch (SQLException | UnsupportedOperationException | IncompatibleClassChangeError e) {
                // Seems to be not supported
                log.debug(e.getMessage());
            }
        }
        if (!scrolled) {
            // Just fetch first 'firstRow' rows
            for (long i = 1; i <= offset; i++) {
                try {
                    dbResult.next();
                } catch (SQLException e) {
                    throw new SQLException("Can't scroll result set to row " + offset, e);
                }
            }
        }
    }

    public static void reportWarnings(JDBCSession session, SQLWarning rootWarning)
    {
        for (SQLWarning warning = rootWarning; warning != null; warning = warning.getNextWarning()) {
            if (warning.getMessage() == null && warning.getErrorCode() == 0) {
                // Skip trash [Excel driver]
                continue;
            }
            log.warn("SQL Warning (DataSource: " + session.getDataSource().getContainer().getName() + "; Code: "
                + warning.getErrorCode() + "; State: " + warning.getSQLState() + "): " + warning.getLocalizedMessage());
        }
    }

    @NotNull
    public static String limitQueryLength(@NotNull String query, int maxLength)
    {
        return query.length() <= maxLength ? query : query.substring(0, maxLength);
    }

    public static DBSForeignKeyModifyRule getCascadeFromNum(int num)
    {
        switch (num) {
        case DatabaseMetaData.importedKeyNoAction:
            return DBSForeignKeyModifyRule.NO_ACTION;
        case DatabaseMetaData.importedKeyCascade:
            return DBSForeignKeyModifyRule.CASCADE;
        case DatabaseMetaData.importedKeySetNull:
            return DBSForeignKeyModifyRule.SET_NULL;
        case DatabaseMetaData.importedKeySetDefault:
            return DBSForeignKeyModifyRule.SET_DEFAULT;
        case DatabaseMetaData.importedKeyRestrict:
            return DBSForeignKeyModifyRule.RESTRICT;
        default:
            return DBSForeignKeyModifyRule.UNKNOWN;
        }
    }

    public static void executeSQL(Connection session, String sql, Object ... params) throws SQLException
    {
        try (PreparedStatement dbStat = session.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    dbStat.setObject(i + 1, params[i]);
                }
            }
            dbStat.execute();
        }
    }

    public static void executeProcedure(Connection session, String sql) throws SQLException
    {
        try (PreparedStatement dbStat = session.prepareCall(sql)) {
            dbStat.execute();
        }
    }

    public static <T> T executeQuery(Connection session, String sql, Object ... params) throws SQLException
    {
        try (PreparedStatement dbStat = session.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    dbStat.setObject(i + 1, params[i]);
                }
            }
            try (ResultSet resultSet = dbStat.executeQuery()) {
                if (resultSet.next()) {
                    return (T) resultSet.getObject(1);
                } else {
                    return null;
                }
            }
        }
    }

    @Nullable
    public static String queryString(JDBCSession session, String sql, Object... args) throws SQLException
    {
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    dbStat.setObject(i + 1, args[i]);
                }
            }
            try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                } else {
                    return null;
                }
            }
        }
    }

    @Nullable
    public static <T> T queryObject(JDBCSession session, String sql, Object... args) throws SQLException
    {
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    dbStat.setObject(i + 1, args[i]);
                }
            }
            try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                if (resultSet.next()) {
                    return (T) resultSet.getObject(1);
                } else {
                    return null;
                }
            }
        }
    }

    private static void debugColumnRead(ResultSet dbResult, String columnName, SQLException error)
    {
        String colFullId = columnName;
        if (dbResult instanceof JDBCResultSet) {
            colFullId += ":" + ((JDBCResultSet) dbResult).getSession().getDataSource().getContainer().getId();
        }
        synchronized (badColumnNames) {
            final Integer errorCount = badColumnNames.get(colFullId);
            if (errorCount == null) {
                log.debug("Can't get column '" + columnName + "': " + error.getMessage());
            }
            badColumnNames.put(colFullId, errorCount == null ? 0 : errorCount + 1);
        }
    }

    private static void debugColumnRead(ResultSet dbResult, int columnIndex, SQLException error)
    {
        debugColumnRead(dbResult, "#" + columnIndex, error);
    }

    public static void appendFilterClause(StringBuilder sql, DBSObjectFilter filter, String columnAlias, boolean firstClause)
    {
        if (filter.isNotApplicable()) {
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
                if (i > 0)
                    sql.append(" OR ");
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
                if (i > 0)
                    sql.append(" OR ");
                sql.append(columnAlias);
                SQLUtils.appendLikeCondition(sql, exclude.get(i), false);
            }
            sql.append(")");
        }
    }

    public static void setFilterParameters(JDBCPreparedStatement statement, int paramIndex, DBSObjectFilter filter)
        throws SQLException
    {
        if (filter.isNotApplicable()) {
            return;
        }
        for (String inc : CommonUtils.safeCollection(filter.getInclude())) {
            statement.setString(paramIndex++, SQLUtils.makeSQLLike(inc));
        }
        for (String exc : CommonUtils.safeCollection(filter.getExclude())) {
            statement.setString(paramIndex++, SQLUtils.makeSQLLike(exc));
        }
    }

    public static void rethrowSQLException(Throwable e) throws SQLException
    {
        if (e instanceof InvocationTargetException) {
            Throwable targetException = ((InvocationTargetException) e).getTargetException();
            if (targetException instanceof SQLException) {
                throw (SQLException) targetException;
            } else {
                throw new SQLException(targetException);
            }
        }
    }

    public static DBPDataKind resolveDataKind(@Nullable DBPDataSource dataSource, String typeName, int typeID)
    {
        if (dataSource == null) {
            return JDBCDataSource.getDataKind(typeName, typeID);
        } else if (dataSource instanceof DBPDataTypeProvider) {
            return ((DBPDataTypeProvider) dataSource).resolveDataKind(typeName, typeID);
        } else {
            return DBPDataKind.UNKNOWN;
        }
    }

    /**
     * Invoke JDBC method from Java 1.7 API
     * 
     * @param object
     *            object
     * @param methodName
     *            method name
     * @param resultType
     *            result type or null
     * @param paramTypes
     *            parameter type array or null
     * @param paramValues
     *            parameter value array or null
     * @return result or null
     * @throws SQLException
     *             on error. Throws SQLFeatureNotSupportedException if specified method is not implemented
     */
    @Nullable
    public static <T> T callMethod17(Object object, String methodName, @Nullable Class<T> resultType, @Nullable Class[] paramTypes,
        Object... paramValues) throws SQLException
    {
        try {
            Object result = object.getClass().getMethod(methodName, paramTypes).invoke(object, paramValues);
            if (result == null || resultType == null) {
                return null;
            } else {
                return resultType.cast(result);
            }
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SQLException) {
                throw (SQLException) e.getTargetException();
            } else {
                throw new SQLException(e.getTargetException());
            }
        } catch (Throwable e) {
            throw new SQLFeatureNotSupportedException(JDBCConstants.ERROR_API_NOT_SUPPORTED_17, e);
        }
    }

    public static String generateTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull JDBCTable table, Map<String, Object> options, boolean addComments) throws DBException {
        final DBERegistry editorsRegistry = table.getDataSource().getContainer().getPlatform().getEditorsRegistry();
        final SQLObjectEditor entityEditor = editorsRegistry.getObjectManager(table.getClass(), SQLObjectEditor.class);
        if (entityEditor instanceof SQLTableManager) {
            DBEPersistAction[] ddlActions = ((SQLTableManager) entityEditor).getTableDDL(monitor, table, options);
            return SQLUtils.generateScript(table.getDataSource(), ddlActions, addComments);
        }
        log.debug("Table editor not found for " + table.getClass().getName());
        return SQLUtils.generateCommentLine(table.getDataSource(), "Can't generate DDL: table editor not found for " + table.getClass().getName());
    }

}
