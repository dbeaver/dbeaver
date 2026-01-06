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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.sql.SQLException;
import java.sql.Types;
import java.text.Format;
import java.text.SimpleDateFormat;

/**
 * Object type support
 */
public class OracleTimestampValueHandler extends JDBCDateTimeValueHandler {

    private static final SimpleDateFormat DEFAULT_DATETIME_FORMAT = new ExtendedDateFormat("'TIMESTAMP '''yyyy-MM-dd HH:mm:ss.ffffff''");
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("'DATE '''yyyy-MM-dd''");
    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("'TIME '''HH:mm:ss.SSS''");

    @NotNull
    private DBPDataSource dataSource;

    OracleTimestampValueHandler(DBDFormatSettings formatSettings, @NotNull DBPDataSource dataSource) {
        super(formatSettings);
        this.dataSource = dataSource;
    }

    @Override
    public Object fetchValueObject(
        @NotNull DBCSession session,
        @NotNull DBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException {
        boolean showDateAsDate = CommonUtils.getBoolean(
            session.getDataSource().getContainer().getConnectionConfiguration().getProviderProperty(OracleConstants.PROP_SHOW_DATE_AS_DATE),
            false);
        if (resultSet instanceof JDBCResultSet) {
            if (showDateAsDate && OracleConstants.TYPE_NAME_DATE.equals(type.getTypeName())
                && !formatSettings.isUseNativeDateTimeFormat()) {
                try {
                    return ((JDBCResultSet) resultSet).getDate(index + 1);
                } catch (SQLException e) {
                    log.debug("Exception caught when fetching date value", e);
                }
            }
        }

        return super.fetchValueObject(session, resultSet, type, index);
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
        if (object != null && object.getClass().getName().startsWith(OracleConstants.TIMESTAMP_CLASS_NAME)) {
            try {
                return OracleTimestampConverter.toTimestamp(object, ((JDBCSession) session).getOriginal());
            } catch (Exception e) {
                throw new DBCException("Error extracting Oracle TIMESTAMP value", e);
            }
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (format == DBDDisplayFormat.NATIVE && value instanceof String) {
            if (!((String) value).startsWith("TIMESTAMP")) {
                return "TIMESTAMP'" + value + "'";
            } else {
                return (String) value;
            }
        }
        return super.getValueDisplayString(column, value, format);
    }

    @Nullable
    @Override
    public Format getNativeValueFormat(DBSTypedObject type) {
        switch (type.getTypeID()) {
            case Types.TIMESTAMP:
                return DEFAULT_DATETIME_FORMAT;
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case OracleConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE:
            case OracleConstants.DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE:
                return DEFAULT_DATETIME_FORMAT;
            case Types.TIME:
                return DEFAULT_TIME_FORMAT;
            case Types.TIME_WITH_TIMEZONE:
                return DEFAULT_TIME_FORMAT;
            case Types.DATE:
                return DEFAULT_DATE_FORMAT;
        }
        // Have to revert DATE format. I can't realize what is difference between TIMESTAMP and DATE without time part.
        // Column types and lengths are the same. Data type name is the same. Oh, Oracle...
/*
        if (type.getMaxLength() == OracleConstants.DATE_TYPE_LENGTH) {
            return DEFAULT_DATE_FORMAT;
        }
*/
        return super.getNativeValueFormat(type);
    }

    @Override
    public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, @Nullable Object value) throws DBCException {
        try {
            JDBCPreparedStatement dbStat = (JDBCPreparedStatement) statement;
            if (value == null) {
                dbStat.setNull(index + 1, type.getTypeID());
            }
            if (value instanceof String) {
                // It can be date/timestamp column. Check it and try to set date/timestamp value. Oracle driver doesn't want to receive a string as an argument #11685
                int typeID = type.getTypeID();
                if (typeID == Types.DATE) {
                    try {
                        dbStat.setDate(index + 1, java.sql.Date.valueOf(value.toString()));
                    } catch (IllegalArgumentException e) {
                        dbStat.setString(index + 1, (String) value);
                    }
                } else if (typeID == Types.TIMESTAMP) {
                    try {
                        dbStat.setTimestamp(index + 1, java.sql.Timestamp.valueOf(value.toString()));
                    } catch (IllegalArgumentException e) {
                        dbStat.setString(index + 1, (String) value);
                    }
                } else {
                    dbStat.setString(index + 1, (String) value);
                }
            } else {
                super.bindValueObject(session, statement, type, index, value);
            }
        } catch (SQLException e) {
            throw new DBCException(ModelMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @NotNull
    protected String getFormatterId(DBSTypedObject column) {
        boolean showDateAsDate = CommonUtils.getBoolean(
                dataSource.getContainer().getConnectionConfiguration().getProviderProperty(OracleConstants.PROP_SHOW_DATE_AS_DATE),
                false);
        if (showDateAsDate && OracleConstants.TYPE_NAME_DATE.equals(column.getTypeName())) {
            return DBDDataFormatter.TYPE_NAME_DATE;
        }

        return super.getFormatterId(column);
    }

}
