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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

/**
 * MySQL datetime handler
 */
public class MySQLDateTimeValueHandler extends JDBCDateTimeValueHandler {

    private static final Date ZERO_DATE = new Date(0l);
    private static final Date ZERO_TIMESTAMP = new Date(0l);

    private static final String ZERO_DATE_STRING = "0000-00-00";
    private static final String ZERO_TIMESTAMP_STRING = "0000-00-00 00:00:00";

    public MySQLDateTimeValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        super(formatterProfile);
    }

    @Override
    public Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index) throws DBCException {
        return super.fetchValueObject(session, resultSet, type, index);
    }

    @Override
    public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, @Nullable Object value) throws DBCException {
        if (value == ZERO_DATE || value == ZERO_TIMESTAMP) {
            // Workaround for zero values (#1127)
            try {
                JDBCPreparedStatement dbStat = (JDBCPreparedStatement)statement;
                if (value == ZERO_DATE) {
                    dbStat.setString(index + 1, ZERO_DATE_STRING);
                } else {
                    dbStat.setString(index + 1, ZERO_TIMESTAMP_STRING);
                }
            }
            catch (SQLException e) {
                throw new DBCException(ModelMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
            }
        } else {
            super.bindValueObject(session, statement, type, index, value);
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (value == ZERO_DATE) {
            return ZERO_DATE_STRING;
        } else if (value == ZERO_TIMESTAMP) {
            return ZERO_TIMESTAMP_STRING;
        }
        return super.getValueDisplayString(column, value, format);
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException {
        if (object instanceof String) {
            switch (type.getTypeID()) {
                case Types.DATE:
                    if (object.equals(ZERO_DATE_STRING)) {
                        return ZERO_DATE;
                    }
                    break;
                default:
                    if (object.equals(ZERO_TIMESTAMP_STRING)) {
                        return ZERO_TIMESTAMP;
                    }
                    break;
            }
        }
        return super.getValueFromObject(session, type, object, copy);
    }
}
