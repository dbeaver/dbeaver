/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
