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
package org.jkiss.dbeaver.ext.mssql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.BeanUtils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


public class SQLServerDateTimeOffsetHandler extends JDBCDateTimeValueHandler {
    private static final Log log = Log.getLog(JDBCStringValueHandler.class);

    public SQLServerDateTimeOffsetHandler(DBDFormatSettings formatSettings) {
        super(formatSettings);
    }

    /**
     * {@link <a href="https://learn.microsoft.com/en-us/sql/connect/jdbc/reference/datetimeoffset-members?view=sql-server-ver16">...</a>}
     * custom SQL Server type
     */
    @Override
    public Object getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        if (
            object != null
            && object.getClass().getName().equals("microsoft.sql.DateTimeOffset")
            && !formatSettings.isUseNativeDateTimeFormat()
        ) {
            try {
                Timestamp timestamp = (Timestamp) BeanUtils.invokeObjectMethod(object, "getTimestamp");
                int offset = (int) BeanUtils.invokeObjectMethod(object, "getMinutesOffset");
                int offsetSeconds = offset * 60;
                if (timestamp == null) {
                    log.debug("Extracted timestamp is null");
                    return null;
                }
                return timestamp.toInstant().atOffset(ZoneOffset.ofTotalSeconds(offsetSeconds));
            } catch (Throwable e) {
                log.debug("error extracting datetimeoffset timestamp", e);
            }
        }
        if (object instanceof String) {
            DBDDataFormatter formatter = getFormatter(type);
            try {
                return formatter.parseValue(((String) object), OffsetDateTime.class);
            } catch (ParseException e) {
                log.debug("Error parsing offset datetime value", e);
                return null;
            }
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @NotNull
    @Override
    protected String getFormatterId(DBSTypedObject column) {
        return DBDDataFormatter.TYPE_NAME_TIMESTAMP_TZ;
    }

    @NotNull
    @Override
    protected DBDDataFormatter getFormatter(DBSTypedObject column) {
        return super.getFormatter(column);
    }

    @Override
    public void bindValueObject(
        @NotNull DBCSession session,
        @NotNull DBCStatement statement,
        @NotNull DBSTypedObject type,
        int index,
        @Nullable Object value
    ) throws DBCException {
        if (value instanceof OffsetDateTime) {
            String s = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(((OffsetDateTime) value));
            super.bindValueObject(session, statement, type, index, s);
            return;
        }
        super.bindValueObject(session, statement, type, index, value);
    }
}
