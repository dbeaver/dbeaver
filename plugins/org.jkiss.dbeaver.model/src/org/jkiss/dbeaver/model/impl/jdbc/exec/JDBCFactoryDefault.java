/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.*;

import java.sql.*;


/**
 * Default JDBC factory
 */
public class JDBCFactoryDefault implements JDBCFactory {

    @Override
    public JDBCDatabaseMetaData createMetaData(@NotNull JDBCSession session, @NotNull DatabaseMetaData original) throws SQLException {
        return new JDBCDatabaseMetaDataImpl(session, original);
    }

    @Override
    public JDBCStatement createStatement(@NotNull JDBCSession session, @NotNull Statement original, boolean disableLogging) throws SQLException {
        return new JDBCStatementImpl<>(session, original, disableLogging);
    }

    @Override
    public JDBCPreparedStatement createPreparedStatement(@NotNull JDBCSession session, @NotNull PreparedStatement original, @Nullable String sql, boolean disableLogging) throws SQLException {
        return new JDBCPreparedStatementImpl(session, original, sql, disableLogging);
    }

    @Override
    public JDBCCallableStatement createCallableStatement(@NotNull JDBCSession session, @NotNull CallableStatement original, @Nullable String sql, boolean disableLogging) throws SQLException {
        return new JDBCCallableStatementImpl(session, original, sql, disableLogging);
    }

    @Override
    public JDBCResultSet createResultSet(@NotNull JDBCSession session, @Nullable JDBCStatement statement, @NotNull ResultSet original, String description, boolean disableLogging) throws SQLException {
        return new JDBCResultSetImpl(session, statement, original, description, disableLogging);
    }

    @Override
    public JDBCResultSetMetaData createResultSetMetaData(@NotNull JDBCResultSet resultSet) throws SQLException {
        return new JDBCResultSetMetaDataImpl(resultSet);
    }
}
