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
import org.jkiss.dbeaver.model.impl.data.DateTimeCustomValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;

/**
 * JDBC string value handler
 */
public class JDBCDateTimeValueHandler extends DateTimeCustomValueHandler {

    public static final SimpleDateFormat DEFAULT_DATETIME_FORMAT = new SimpleDateFormat("''" + DBConstants.DEFAULT_TIMESTAMP_FORMAT + "''");
    public static final SimpleDateFormat DEFAULT_DATETIME_TZ_FORMAT = new SimpleDateFormat("''" + DBConstants.DEFAULT_TIMESTAMP_TZ_FORMAT + "''");
    public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("''" + DBConstants.DEFAULT_DATE_FORMAT + "''");
    public static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("''" + DBConstants.DEFAULT_TIME_FORMAT + "''");
    public static final SimpleDateFormat DEFAULT_TIME_TZ_FORMAT = new SimpleDateFormat("''" + DBConstants.DEFAULT_TIME_TZ_FORMAT + "''");

    public JDBCDateTimeValueHandler(DBDFormatSettings formatSettings) {
        super(formatSettings);
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
        Object value = super.getValueFromObject(session, type, object, copy, validateValue);
        if (value instanceof Date || value instanceof LocalDate || value instanceof LocalDateTime) {
            return switch (type.getTypeID()) {
                case Types.TIME, Types.TIME_WITH_TIMEZONE -> getTimeValue(value);
                case Types.DATE -> getDateValue(value);
                default -> getTimestampValue(value);
            };
        }
        return value;
    }

    @Override
    public Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index) throws DBCException {
        try {
            if (resultSet instanceof JDBCResultSet dbResults) {

                // check for native format
                if (formatSettings.isUseNativeDateTimeFormat()) {
                    try {
                        return dbResults.getString(index + 1);
                    } catch (SQLException e) {
                        log.debug("Can't read date/time value as string: " + e.getMessage());
                    }
                }

                // It seems that some drivers doesn't support reading date/time values with explicit calendar
                // So let's use simple version
                return switch (type.getTypeID()) {
                    case Types.TIME, Types.TIME_WITH_TIMEZONE -> dbResults.getTime(index + 1);
                    case Types.DATE -> {
                        if (isReadDateAsObject()) {
                            yield getValueFromObject(session, type, dbResults.getObject(index + 1), false, false);
                        }
                        yield dbResults.getDate(index + 1);
                    }
                    default -> {
                        Object value = dbResults.getObject(index + 1);
                        yield getValueFromObject(session, type, value, false, false);
                    }
                };
            } else {
                return resultSet.getAttributeValue(index);
            }
        } catch (SQLException e) {
            try {
                if (e.getCause() instanceof ParseException ||
                    e.getCause() instanceof UnsupportedOperationException) {
                    // [SQLite] workaround.
                    Object objectValue = ((JDBCResultSet) resultSet).getObject(index + 1);
                    if (objectValue instanceof Date) {
                        return objectValue;
                    } else if (objectValue instanceof String) {
                        // Do not convert to Date object because table column has STRING type
                        // and it will be converted in string at late binding stage making incorrect string value: Date.toString()
                        return objectValue;
                    } else if (objectValue != null) {
                        // Perhaps some database-specific timestamp representation. E.lg. H2 TimestampWithTimezone
                        return objectValue.toString();
                    } else {
                        return null;
                    }
                } else if (
                    SQLState.SQL_42000.getCode().equals(e.getSQLState()) ||
                        SQLState.SQL_S1009.getCode().equals(e.getSQLState()) ||
                        SQLState.SQL_HY000.getCode().equals(e.getSQLState())) {
                    // [MySQL, Netezza] workaround. Time value may be interval (should be read as string)
                    return ((JDBCResultSet) resultSet).getString(index + 1);
                }
            } catch (SQLException e1) {
                // Ignore
                log.debug("Can't retrieve datetime object", e1);
                return null;
            }
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    /**
     * Allow to read datatime from LocalTime object
     */
    protected boolean isReadDateAsObject() {
        return false;
    }

    @Override
    public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, @Nullable Object value) throws DBCException {
        try {
            JDBCPreparedStatement dbStat = (JDBCPreparedStatement) statement;
            // JDBC uses 1-based indexes
            if (value == null) {
                dbStat.setNull(index + 1, type.getTypeID());
            } else if (value instanceof String) {
                // Some custom value format.
                dbStat.setString(index + 1, (String) value);
            } else {
                switch (type.getTypeID()) {
                    case Types.TIME:
                    case Types.TIME_WITH_TIMEZONE:
                        dbStat.setTime(index + 1, getTimeValue(value));
                        break;
                    case Types.DATE:
                        dbStat.setDate(index + 1, getDateValue(value));
                        break;
                    default:
                        dbStat.setTimestamp(index + 1, getTimestampValue(value));
                        break;
                }
            }
        } catch (SQLException e) {
            throw new DBCException(ModelMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (format == DBDDisplayFormat.NATIVE) {
            if (value instanceof LocalDate localDate) {
                value = getDateValue(localDate);
            } else if (value instanceof LocalDateTime localDateTime) {
                value = getTimestampValue(localDateTime);
            }
            if (value instanceof Date) {
                Format nativeFormat = getNativeValueFormat(column);
                if (nativeFormat != null) {
                    try {
                        return nativeFormat.format(value);
                    } catch (Exception e) {
                        log.error("Error formatting date", e);
                    }
                }
            } else if (value instanceof String strValue) {
                if (!strValue.startsWith("'") && !strValue.endsWith("'")) {
                    strValue = "'" + strValue + "'";
                }
                return super.getValueDisplayString(column, strValue, format);
            }
        }
        return super.getValueDisplayString(column, value, format);
    }

    @Nullable
    protected Format getNativeValueFormat(DBSTypedObject type) {
        return switch (type.getTypeID()) {
            case Types.TIMESTAMP -> DEFAULT_DATETIME_FORMAT;
            case Types.TIMESTAMP_WITH_TIMEZONE -> DEFAULT_DATETIME_FORMAT;
            case Types.TIME -> DEFAULT_TIME_FORMAT;
            case Types.TIME_WITH_TIMEZONE -> DEFAULT_TIME_TZ_FORMAT;
            case Types.DATE -> DEFAULT_DATE_FORMAT;
            default -> null;
        };
    }

    @NotNull
    protected String getFormatterId(DBSTypedObject column) {
        return switch (column.getTypeID()) {
            case Types.TIME -> DBDDataFormatter.TYPE_NAME_TIME;
            case Types.DATE -> DBDDataFormatter.TYPE_NAME_DATE;
            case Types.TIME_WITH_TIMEZONE -> DBDDataFormatter.TYPE_NAME_TIME_TZ;
            case Types.TIMESTAMP_WITH_TIMEZONE -> DBDDataFormatter.TYPE_NAME_TIMESTAMP_TZ;
            default -> DBDDataFormatter.TYPE_NAME_TIMESTAMP;
        };
    }

    @Nullable
    protected static java.sql.Time getTimeValue(Object value) {
        if (value instanceof java.sql.Time time) {
            return time;
        } else if (value instanceof Date date) {
            return new java.sql.Time(date.getTime());
        } else if (value instanceof LocalDate localDate) {
            return java.sql.Time.valueOf(localDate.atStartOfDay().toLocalTime());
        } else if (value instanceof LocalDateTime localDateTime) {
            return java.sql.Time.valueOf(localDateTime.toLocalTime());
        } else if (value != null) {
            return java.sql.Time.valueOf(value.toString());
        } else {
            return null;
        }
    }

    @Nullable
    protected static java.sql.Date getDateValue(Object value) {
        if (value instanceof java.sql.Date date) {
            return date;
        } else if (value instanceof Date date) {
            return new java.sql.Date(date.getTime());
        } else if (value instanceof LocalDate localDate) {
            return java.sql.Date.valueOf(localDate);
        } else if (value instanceof LocalDateTime localDateTime) {
            return java.sql.Date.valueOf(localDateTime.toLocalDate());
        } else if (value != null) {
            return java.sql.Date.valueOf(value.toString());
        } else {
            return null;
        }
    }

    @Nullable
    protected static java.sql.Timestamp getTimestampValue(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        } else if (value instanceof Date date) {
            return new Timestamp(date.getTime());
        } else if (value instanceof LocalDate localDate) {
            return Timestamp.valueOf(localDate.atStartOfDay());
        } else if (value instanceof LocalDateTime localDateTime) {
            return Timestamp.valueOf(localDateTime);
        } else if (value instanceof OffsetDateTime offsetDateTime) {
            return Timestamp.valueOf((offsetDateTime.toLocalDateTime()));
        } else if (value != null) {
            return Timestamp.valueOf(value.toString());
        } else {
            return null;
        }
    }

    protected static String getTwoDigitValue(int value) {
        if (value < 10) {
            return "0" + value;
        } else {
            return String.valueOf(value);
        }
    }

}