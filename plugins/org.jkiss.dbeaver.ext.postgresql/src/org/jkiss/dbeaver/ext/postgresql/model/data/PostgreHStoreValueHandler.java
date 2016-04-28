/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreHStoreValueHandler
 */
public class PostgreHStoreValueHandler extends JDBCStringValueHandler {
    private static final Log log = Log.getLog(PostgreHStoreValueHandler.class);
    public static final PostgreHStoreValueHandler INSTANCE = new PostgreHStoreValueHandler();

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws SQLException {
        return super.fetchColumnValue(session, resultSet, type, index);
    }

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            statement.setObject(paramIndex, value.toString(), Types.OTHER);
        }
    }
}
