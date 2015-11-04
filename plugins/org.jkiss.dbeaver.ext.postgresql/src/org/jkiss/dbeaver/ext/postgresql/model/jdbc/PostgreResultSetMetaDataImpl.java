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
package org.jkiss.dbeaver.ext.postgresql.model.jdbc;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetMetaDataImpl;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * PostgreResultSetMetaDataImpl
 */
public class PostgreResultSetMetaDataImpl extends JDBCResultSetMetaDataImpl
{

    public PostgreResultSetMetaDataImpl(JDBCResultSet resultSet) throws SQLException {
        super(resultSet);
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        String schemaName;
        try {
            schemaName = (String)original.getClass().getMethod("getBaseSchemaName", Integer.TYPE).invoke(original, column);
        } catch (InvocationTargetException e) {
            throw new SQLException("Error getting schema name", e.getTargetException());
        } catch (Exception e) {
            throw new SQLException("Error getting schema name", e);
        }
        return JDBCUtils.normalizeIdentifier(schemaName);
    }

    @Override
    public String getTableName(int column) throws SQLException {
        String tableName;
        try {
            tableName = (String)original.getClass().getMethod("getBaseTableName", Integer.TYPE).invoke(original, column);
        } catch (InvocationTargetException e) {
            throw new SQLException("Error getting table name", e.getTargetException());
        } catch (Exception e) {
            throw new SQLException("Error getting table name", e);
        }
        return JDBCUtils.normalizeIdentifier(tableName);
    }
}
