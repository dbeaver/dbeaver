/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.db;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.connection.InternalDatabaseConfig;
import org.jkiss.utils.CommonUtils;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Proxy for internal connection.
 * Normalizes part of schema name in SQL queries.
 */
public class InternalProxyConnection implements Connection {
    @NotNull
    private final Connection connection;
    @NotNull
    private final InternalDatabaseConfig config;

    public InternalProxyConnection(@NotNull Connection connection, @NotNull InternalDatabaseConfig config) {
        this.connection = connection;
        this.config = config;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return new StatementProxy(connection.createStatement());
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return connection.prepareStatement(normalizeTableNames(sql));
    }

    private String normalizeTableNames(@NotNull String sql) {
        return CommonUtils.normalizeTableNames(sql, config.getSchema());
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return connection.prepareCall(normalizeTableNames(sql));
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return connection.nativeSQL(normalizeTableNames(sql));
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        connection.setAutoCommit(autoCommit);
    }

    @Override
    public void commit() throws SQLException {
        connection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        connection.rollback();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return connection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return connection.isReadOnly();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        connection.setReadOnly(readOnly);
    }

    @Override
    public String getCatalog() throws SQLException {
        return connection.getCatalog();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        connection.setCatalog(catalog);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return connection.getTransactionIsolation();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        connection.setTransactionIsolation(level);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return connection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        connection.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return new StatementProxy(connection.createStatement(resultSetType, resultSetConcurrency));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return connection.prepareStatement(normalizeTableNames(sql), resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return connection.prepareCall(normalizeTableNames(sql), resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        connection.setTypeMap(map);
    }

    @Override
    public int getHoldability() throws SQLException {
        return connection.getHoldability();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        connection.setHoldability(holdability);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return connection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        connection.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        connection.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new StatementProxy(connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(
        String sql,
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability
    ) throws SQLException {
        return connection.prepareStatement(normalizeTableNames(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(
        String sql,
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability
    ) throws SQLException {
        return connection.prepareCall(normalizeTableNames(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return connection.prepareStatement(normalizeTableNames(sql), autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return connection.prepareStatement(normalizeTableNames(sql), columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return connection.prepareStatement(normalizeTableNames(sql), columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        return connection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return connection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return connection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return connection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        connection.setClientInfo(name, value);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return connection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return connection.getClientInfo();
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        connection.setClientInfo(properties);
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return connection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return connection.createStruct(typeName, attributes);
    }

    @Override
    public String getSchema() throws SQLException {
        return connection.getSchema();
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        connection.setSchema(schema);
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        connection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        connection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return connection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return connection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return connection.isWrapperFor(iface);
    }

    private class StatementProxy implements Statement {
        @NotNull
        private final Statement statement;

        public StatementProxy(@NotNull Statement statement) {
            this.statement = statement;
        }

        // SQL query methods with table name normalization
        @Override
        public ResultSet executeQuery(String sql) throws SQLException {
            return statement.executeQuery(normalizeTableNames(sql));
        }

        @Override
        public int executeUpdate(String sql) throws SQLException {
            return statement.executeUpdate(normalizeTableNames(sql));
        }

        @Override
        public boolean execute(String sql) throws SQLException {
            return statement.execute(normalizeTableNames(sql));
        }

        @Override
        public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
            return statement.executeUpdate(normalizeTableNames(sql), autoGeneratedKeys);
        }

        @Override
        public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
            return statement.executeUpdate(normalizeTableNames(sql), columnIndexes);
        }

        @Override
        public int executeUpdate(String sql, String[] columnNames) throws SQLException {
            return statement.executeUpdate(normalizeTableNames(sql), columnNames);
        }

        @Override
        public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
            return statement.execute(normalizeTableNames(sql), autoGeneratedKeys);
        }

        @Override
        public boolean execute(String sql, int[] columnIndexes) throws SQLException {
            return statement.execute(normalizeTableNames(sql), columnIndexes);
        }

        @Override
        public boolean execute(String sql, String[] columnNames) throws SQLException {
            return statement.execute(normalizeTableNames(sql), columnNames);
        }

        @Override
        public void addBatch(String sql) throws SQLException {
            statement.addBatch(normalizeTableNames(sql));
        }

        @Override
        public int[] executeBatch() throws SQLException {
            return statement.executeBatch();
        }

        @Override
        public void close() throws SQLException {
            statement.close();
        }

        @Override
        public int getMaxFieldSize() throws SQLException {
            return statement.getMaxFieldSize();
        }

        @Override
        public void setMaxFieldSize(int max) throws SQLException {
            statement.setMaxFieldSize(max);
        }

        @Override
        public int getMaxRows() throws SQLException {
            return statement.getMaxRows();
        }

        @Override
        public void setMaxRows(int max) throws SQLException {
            statement.setMaxRows(max);
        }

        @Override
        public void setEscapeProcessing(boolean enable) throws SQLException {
            statement.setEscapeProcessing(enable);
        }

        @Override
        public int getQueryTimeout() throws SQLException {
            return statement.getQueryTimeout();
        }

        @Override
        public void setQueryTimeout(int seconds) throws SQLException {
            statement.setQueryTimeout(seconds);
        }

        @Override
        public void cancel() throws SQLException {
            statement.cancel();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return statement.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            statement.clearWarnings();
        }

        @Override
        public void setCursorName(String name) throws SQLException {
            statement.setCursorName(name);
        }

        @Override
        public ResultSet getResultSet() throws SQLException {
            return statement.getResultSet();
        }

        @Override
        public int getUpdateCount() throws SQLException {
            return statement.getUpdateCount();
        }

        @Override
        public boolean getMoreResults() throws SQLException {
            return statement.getMoreResults();
        }

        @Override
        public void setFetchDirection(int direction) throws SQLException {
            statement.setFetchDirection(direction);
        }

        @Override
        public int getFetchDirection() throws SQLException {
            return statement.getFetchDirection();
        }

        @Override
        public void setFetchSize(int rows) throws SQLException {
            statement.setFetchSize(rows);
        }

        @Override
        public int getFetchSize() throws SQLException {
            return statement.getFetchSize();
        }

        @Override
        public int getResultSetConcurrency() throws SQLException {
            return statement.getResultSetConcurrency();
        }

        @Override
        public int getResultSetType() throws SQLException {
            return statement.getResultSetType();
        }

        @Override
        public void clearBatch() throws SQLException {
            statement.clearBatch();
        }

        @Override
        public Connection getConnection() throws SQLException {
            return statement.getConnection();
        }

        @Override
        public boolean getMoreResults(int current) throws SQLException {
            return statement.getMoreResults(current);
        }

        @Override
        public ResultSet getGeneratedKeys() throws SQLException {
            return statement.getGeneratedKeys();
        }

        @Override
        public int getResultSetHoldability() throws SQLException {
            return statement.getResultSetHoldability();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return statement.isClosed();
        }

        @Override
        public void setPoolable(boolean poolable) throws SQLException {
            statement.setPoolable(poolable);
        }

        @Override
        public boolean isPoolable() throws SQLException {
            return statement.isPoolable();
        }

        @Override
        public void closeOnCompletion() throws SQLException {
            statement.closeOnCompletion();
        }

        @Override
        public boolean isCloseOnCompletion() throws SQLException {
            return statement.isCloseOnCompletion();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return statement.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return statement.isWrapperFor(iface);
        }
    }
}
