/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.model.exec.jdbc;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC statement
 */
public interface JDBCStatement extends Statement, DBCStatement, DBRBlockingObject {

    @Override
    JDBCSession getConnection();

    @Nullable
    @Override
    JDBCResultSet openResultSet() throws DBCException;

    @Nullable
    @Override
    JDBCResultSet openGeneratedKeysResultSet() throws DBCException;

    @Nullable
    @Override
    JDBCResultSet executeQuery(String sql)
        throws SQLException;

    @Nullable
    @Override
    JDBCResultSet getResultSet()
        throws SQLException;

    @Override
    void close();
}
