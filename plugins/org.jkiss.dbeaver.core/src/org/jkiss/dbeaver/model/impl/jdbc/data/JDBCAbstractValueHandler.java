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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.BaseValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;

/**
 * Base JDBC value handler
 */
public abstract class JDBCAbstractValueHandler extends BaseValueHandler {

    static final Log log = Log.getLog(JDBCAbstractValueHandler.class);

    @Override
    public final Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index)
        throws DBCException
    {
        try {
            if (resultSet instanceof JDBCResultSet) {
                // JDBC uses 1-based indexes
                return fetchColumnValue(session, (JDBCResultSet) resultSet, type, index + 1);
            } else {
                return resultSet.getAttributeValue(index);
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Override
    public final void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject columnMetaData,
                                      int index, Object value) throws DBCException {
        try {
            // JDBC uses 1-based indexes
            this.bindParameter((JDBCSession) session, (JDBCPreparedStatement) statement, columnMetaData, index + 1, value);
        }
        catch (SQLException e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @Nullable
    protected abstract Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index)
        throws DBCException, SQLException;

    /**
     * Binds parameter value
     * @param session       session
     * @param statement     statement
     * @param paramType     parameter type
     * @param paramIndex    parameter index (1-based)
     * @param value         parameter value
     * @throws DBCException
     * @throws SQLException
     */
    protected abstract void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException;

}