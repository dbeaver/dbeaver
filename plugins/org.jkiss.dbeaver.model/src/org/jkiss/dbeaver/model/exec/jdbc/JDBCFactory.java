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
package org.jkiss.dbeaver.model.exec.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.sql.*;

/**
 * JDBC implementation factory
 */
public interface JDBCFactory {

    JDBCDatabaseMetaData createMetaData(@NotNull JDBCSession session, @NotNull DatabaseMetaData original)
        throws SQLException;

    JDBCStatement createStatement(@NotNull JDBCSession session, @NotNull Statement original, boolean disableLogging)
        throws SQLException;

    JDBCPreparedStatement createPreparedStatement(@NotNull JDBCSession session, @NotNull PreparedStatement original, @Nullable String sql, boolean disableLogging)
        throws SQLException;

    JDBCCallableStatement createCallableStatement(@NotNull JDBCSession session, @NotNull CallableStatement original, @Nullable String sql, boolean disableLogging)
        throws SQLException;

    JDBCResultSet createResultSet(@NotNull JDBCSession session, @Nullable JDBCStatement statement, @NotNull ResultSet original, String description, boolean disableLogging)
        throws SQLException;

    JDBCResultSetMetaData createResultSetMetaData(@NotNull JDBCResultSet resultSet)
        throws SQLException;

}
