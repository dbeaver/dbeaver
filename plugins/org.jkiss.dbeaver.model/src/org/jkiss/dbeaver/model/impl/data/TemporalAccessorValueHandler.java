/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * Date/time value handler
 */
public abstract class TemporalAccessorValueHandler extends BaseValueHandler {

    protected static final Log log = Log.getLog(TemporalAccessorValueHandler.class);

    private final DBDDataFormatterProfile formatterProfile;
    protected DBDDataFormatter formatter;

    public TemporalAccessorValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        this.formatterProfile = formatterProfile;
    }

    @NotNull
    @Override
    public Class<Date> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return Date.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof TemporalAccessor) {
            return object;
        } else if (object instanceof String) {
            String strValue = (String)object;
            try {
                return getFormatter(type).parseValue(strValue, isZonedType(type) ? ZonedDateTime.class : LocalDateTime.class);
            } catch (ParseException e) {
                // Try to parse with standard date/time formats
                try {
                    return ZonedDateTime.parse((CharSequence) object);
                } catch (Exception e1) {
                    log.debug("Can't parse string value [" + strValue + "] to date/time value", e);
                    return object;
                }
            }
        } else {
            throw new DBCException("Bad temporal accessor value: " + object);
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (value == null || value instanceof String) {
            return super.getValueDisplayString(column, null, format);
        }
        try {
            return getFormatter(column).formatValue(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    @NotNull
    protected DBDDataFormatter getFormatter(DBSTypedObject column)
    {
        if (formatter == null) {
            try {
                formatter = formatterProfile.createFormatter(getFormatterId(column), column);
            } catch (Exception e) {
                log.error("Can't create formatter for zoned datetime value handler", e); //$NON-NLS-1$
                formatter = DefaultDataFormatter.INSTANCE;
            }
        }
        return formatter;
    }

    protected abstract boolean isZonedType(DBSTypedObject type);

    @NotNull
    protected abstract String getFormatterId(DBSTypedObject column);

}