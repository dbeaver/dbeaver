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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCTemporalAccessorValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;
import java.time.format.DateTimeFormatter;

/**
 * Object type support
 */
public class OracleTemporalAccessorValueHandler extends JDBCTemporalAccessorValueHandler {

    private static final DateTimeFormatter DEFAULT_DATETIME_FORMAT = DateTimeFormatter.ofPattern("'TIMESTAMP '''yyyy-MM-dd HH:mm:ss.nnnnnn''");
    private static final DateTimeFormatter DEFAULT_DATETIME_TZ_FORMAT = DateTimeFormatter.ofPattern("'TIMESTAMP '''yyyy-MM-dd HH:mm:ss.nnnnnn X''");
    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern("'DATE '''yyyy-MM-dd''");
    private static final DateTimeFormatter DEFAULT_TIME_FORMAT = DateTimeFormatter.ofPattern("'TIME '''HH:mm:ss.SSS''");

    public OracleTemporalAccessorValueHandler(DBDFormatSettings formatSettings)
    {
        super(formatSettings);
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
    public DateTimeFormatter getNativeValueFormat(DBSTypedObject type) {
        switch (type.getTypeID()) {
            case Types.TIMESTAMP:
                return DEFAULT_DATETIME_FORMAT;
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case OracleConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE:
            case OracleConstants.DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE:
                return DEFAULT_DATETIME_TZ_FORMAT;
            case Types.TIME:
                return DEFAULT_TIME_FORMAT;
            case Types.TIME_WITH_TIMEZONE:
                return DEFAULT_TIME_TZ_FORMAT;
            case Types.DATE:
                return DEFAULT_DATE_FORMAT;
        }
        return super.getNativeValueFormat(type);
    }

    @NotNull
    @Override
    protected String getFormatterId(DBSTypedObject column) {
        switch (column.getTypeID()) {
            case OracleConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE:
            case OracleConstants.DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE:
                return DBDDataFormatter.TYPE_NAME_TIMESTAMP_TZ;
            default:
                return super.getFormatterId(column);
        }
    }

    @Override
    protected boolean isZonedType(DBSTypedObject type) {
        if (type.getTypeID() == OracleConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE) {
            return true;
        }
        return super.isZonedType(type);
    }

}
