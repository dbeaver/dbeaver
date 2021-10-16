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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.data.TemporalAccessorValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * JDBC zoned datetime value handler
 */
public class JDBCTemporalAccessorValueHandler extends TemporalAccessorValueHandler {

    public static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern("''" + DBConstants.DEFAULT_DATE_FORMAT + "''");
    public static final DateTimeFormatter DEFAULT_TIME_FORMAT = DateTimeFormatter.ofPattern("''" + DBConstants.DEFAULT_TIME_FORMAT + "''");
    public static final DateTimeFormatter DEFAULT_TIME_TZ_FORMAT = DateTimeFormatter.ofPattern("''" + DBConstants.DEFAULT_TIME_TZ_FORMAT + "''");
    public static final DateTimeFormatter DEFAULT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("''" + DBConstants.DEFAULT_TIMESTAMP_FORMAT + "''");
    public static final DateTimeFormatter DEFAULT_TIMESTAMP_TZ_FORMAT = DateTimeFormatter.ofPattern("''" + DBConstants.DEFAULT_TIMESTAMP_TZ_FORMAT + "''");

    public JDBCTemporalAccessorValueHandler(DBDFormatSettings formatSettings)
    {
        super(formatSettings);
    }

    @Override
    public Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index) throws DBCException {
        try {
            if (resultSet instanceof JDBCResultSet) {
                JDBCResultSet dbResults = (JDBCResultSet) resultSet;

                if (session.isUseNativeDateTimeFormat()) {
                    try {
                        return dbResults.getString(index + 1);
                    } catch (SQLException e) {
                        log.debug("Can't read date/time value as string: " + e.getMessage());
                    }
                }

                if (isZonedType(type)) {
                    return dbResults.getObject(index + 1, ZonedDateTime.class);
                } else {
                    return dbResults.getObject(index + 1, LocalDateTime.class);
                }
            } else {
                return resultSet.getAttributeValue(index);
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, @Nullable Object value) throws DBCException {
        try {
            JDBCPreparedStatement dbStat = (JDBCPreparedStatement)statement;
            // JDBC uses 1-based indexes
            if (value == null) {
                dbStat.setNull(index + 1, type.getTypeID());
            } else if (value instanceof TemporalAccessor) {
                dbStat.setObject(index + 1, value);
            }
        }
        catch (SQLException e) {
            throw new DBCException(ModelMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof TemporalAccessor && format == DBDDisplayFormat.NATIVE) {
            DateTimeFormatter nativeFormat = getNativeValueFormat(column);
            if (nativeFormat != null) {
                try {
                    return nativeFormat.format((TemporalAccessor) value);
                } catch (Exception e) {
                    log.error("Error formatting date", e);
                }
            }
        }
        return super.getValueDisplayString(column, value, format);
    }

    @Nullable
    protected DateTimeFormatter getNativeValueFormat(DBSTypedObject type) {
        switch (type.getTypeID()) {
            case Types.TIMESTAMP:
                return DEFAULT_TIMESTAMP_FORMAT;
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return DEFAULT_TIMESTAMP_TZ_FORMAT;
            case Types.TIME:
                return DEFAULT_TIME_FORMAT;
            case Types.TIME_WITH_TIMEZONE:
                return DEFAULT_TIME_TZ_FORMAT;
            case Types.DATE:
                return DEFAULT_DATE_FORMAT;
        }
        return null;
    }

    @NotNull
    protected String getFormatterId(DBSTypedObject column)
    {
        switch (column.getTypeID()) {
            case Types.TIME:
                return DBDDataFormatter.TYPE_NAME_TIME;
            case Types.DATE:
                return DBDDataFormatter.TYPE_NAME_DATE;
            case Types.TIME_WITH_TIMEZONE:
                return DBDDataFormatter.TYPE_NAME_TIME_TZ;
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return DBDDataFormatter.TYPE_NAME_TIMESTAMP_TZ;
            default:
                return DBDDataFormatter.TYPE_NAME_TIMESTAMP;
        }
    }

    @Override
    protected boolean isZonedType(DBSTypedObject type) {
        switch (type.getTypeID()) {
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return true;
            default:
                return false;
        }
    }


}