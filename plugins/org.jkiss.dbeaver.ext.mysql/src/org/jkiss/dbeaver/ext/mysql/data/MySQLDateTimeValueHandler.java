/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;

/**
 * MySQL datetime handler
 */
public class MySQLDateTimeValueHandler extends JDBCDateTimeValueHandler {

    private static final Date ZERO_DATE = new Date(0l);
    private static final Date ZERO_TIMESTAMP = new Date(0l);

    private static final String ZERO_DATE_STRING = "0000-00-00";
    private static final String ZERO_TIMESTAMP_STRING = "0000-00-00 00:00:00";

    public MySQLDateTimeValueHandler(DBDFormatSettings formatSettings)
    {
        super(formatSettings);
    }

    @Override
    public Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index) throws DBCException {
        if (resultSet instanceof JDBCResultSet dbResults) {
            boolean isMariaDB = MySQLUtils.isMariaDB(session.getDataSource().getContainer().getDriver());
            try {
                if (MySQLConstants.TYPE_YEAR.equalsIgnoreCase(type.getTypeName())) {
                    int year = dbResults.getInt(index + 1);
                    if (dbResults.wasNull()) {
                        return null;
                    }
                    return year;
                }
                /*
                  We want to handle time as a String for MariaDB due to it silently cutting the values
                  after 24H. We only want this by default for Maria because MySQL5 will
                  fail regardless of used method for value bigger than 24h. And MySQL8 will
                  try to getTime(). If it fails, we will get value via getString()
                 */
                if (isMariaDB && type.getTypeID() == Types.TIME) {
                    return dbResults.getString(index + 1);
                }
            } catch (SQLException e) {
                log.debug("Exception caught when fetching date/time value", e);
            }
            // In MySQL driver, negative dates aren't returned when using #getString.
            // Weirdly enough, the expected value is returned when using #getBytes().
            if (!isMariaDB && type.getTypeID() == Types.TIME && formatSettings.isUseNativeDateTimeFormat()) {
                byte[] bytes = null;
                try {
                    bytes = dbResults.getBytes(index + 1);
                } catch (SQLException e) {
                    // ignored
                }
                if (ContentUtils.isAsciiText(bytes)) {
                    // We want to be extra sure about bytes containing actual text and not binary data
                    return new String(bytes, StandardCharsets.ISO_8859_1);
                }
            }
        }

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
        } else if (MySQLConstants.TYPE_YEAR.equalsIgnoreCase(type.getTypeName())) {
            try {
                JDBCPreparedStatement dbStat = (JDBCPreparedStatement)statement;
                if (value instanceof Number) {
                    dbStat.setInt(index + 1, ((Number) value).intValue());
                } else if (value instanceof Date) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime((Date) value);
                    dbStat.setInt(index + 1, cal.get(Calendar.YEAR));
                } else if (value instanceof String) {
                    dbStat.setString(index + 1, (String) value);
                } else {
                    dbStat.setObject(index + 1, value);
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
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
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
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }
}
