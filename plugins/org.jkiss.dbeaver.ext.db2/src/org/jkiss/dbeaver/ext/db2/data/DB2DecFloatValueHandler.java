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
package org.jkiss.dbeaver.ext.db2.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCNumberValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * DECFLOAT type support
 */
public class DB2DecFloatValueHandler extends JDBCNumberValueHandler {

    final static int DECFLOAT_SPECIALVALUE_ENCOUNTERED = -4231;

    public DB2DecFloatValueHandler(DBDDataFormatterProfile formatterProfile) {
        super(formatterProfile);
    }

    @Nullable
    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException {
        try {
            return resultSet.getBigDecimal(index);
        } catch (SQLException e) {
            if (e.getErrorCode() == DECFLOAT_SPECIALVALUE_ENCOUNTERED) {
                return resultSet.getDouble(index);
            } else {
                throw e;
            }
        }
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws SQLException {
        if (value instanceof BigDecimal) {
            statement.setBigDecimal(paramIndex, (BigDecimal) value);
        } else if (value instanceof Double) {
            statement.setDouble(paramIndex, (Double) value);
        } else {
            super.bindParameter(session, statement, paramType, paramIndex, value);
        }
    }
}
