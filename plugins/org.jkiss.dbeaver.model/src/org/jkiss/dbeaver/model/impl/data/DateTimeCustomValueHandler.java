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
import java.util.Date;

/**
 * Customizable date/time value handler
 */
public abstract class DateTimeCustomValueHandler extends DateTimeValueHandler {

    protected static final Log log = Log.getLog(DateTimeCustomValueHandler.class);

    private DBDDataFormatterProfile formatterProfile;
    protected DBDDataFormatter formatter;

    public DateTimeCustomValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        this.formatterProfile = formatterProfile;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof Date) {
            return (Date) (copy ? ((Date)object).clone() : object);
        } else if (object instanceof String) {
            String strValue = (String)object;
            try {
                return getFormatter(type).parseValue(strValue, null);
            } catch (ParseException e) {
                // Try to parse with standard date/time formats

                //DateFormat.get
                try {
                    // Try to parse as java date
                    @SuppressWarnings("deprecation")
                    Date result = new Date(strValue);
                    return result;
                } catch (Exception e1) {
                    log.debug("Can't parse string value [" + strValue + "] to date/time value", e);
                    return null;
                }
            }
        } else {
            log.warn("Unrecognized type '" + object.getClass().getName() + "' - can't convert to date/time value");
            return null;
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value == null) {
            return super.getValueDisplayString(column, null, format);
        }
        try {
            return getFormatter(column).formatValue(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    protected DBDDataFormatter getFormatter(String typeId)
    {
        try {
            return formatterProfile.createFormatter(typeId);
        } catch (Exception e) {
            log.error("Can't create formatter for datetime value handler", e); //$NON-NLS-1$
            return DefaultDataFormatter.INSTANCE;
        }
    }

    @NotNull
    protected DBDDataFormatter getFormatter(DBSTypedObject column)
    {
        if (formatter == null) {
            formatter = getFormatter(getFormatterId(column));
        }
        return formatter;
    }

    @NotNull
    protected abstract String getFormatterId(DBSTypedObject column);

    protected static String getTwoDigitValue(int value)
    {
        if (value < 10) {
            return "0" + value;
        } else {
            return String.valueOf(value);
        }
    }

}