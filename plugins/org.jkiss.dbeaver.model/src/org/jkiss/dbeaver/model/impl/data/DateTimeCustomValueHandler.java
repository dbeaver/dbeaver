/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandlerConfigurable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.text.ParseException;
import java.util.Date;

/**
 * Customizable date/time value handler
 */
public abstract class DateTimeCustomValueHandler extends DateTimeValueHandler implements DBDValueHandlerConfigurable {

    protected static final Log log = Log.getLog(DateTimeCustomValueHandler.class);

    private final DBDDataFormatterProfile formatterProfile;
    protected DBDDataFormatter formatter;

    public DateTimeCustomValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        this.formatterProfile = formatterProfile;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof Date) {
            return copy ? ((Date)object).clone() : object;
        } else if (object instanceof String) {
            String strValue = (String)object;
            if (strValue.isEmpty()) {
                // NULL date
                return null;
            }
            if (session != null && session.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_NATIVE_DATETIME_FORMAT)) {
                // Do not use formatter for native format
                return object;
            }

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
                    //log.debug("Can't parse string value [" + strValue + "] to date/time value", e);
                    return object;
                }
            }
        } else {
            //log.warn("Unrecognized type '" + object.getClass().getName() + "' - can't convert to date/time value");
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value == null || value instanceof String || value instanceof Number) {
            return super.getValueDisplayString(column, value, format);
        }
        try {
            return getFormatter(column).formatValue(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private DBDDataFormatter getFormatter(DBSTypedObject typedObject, String typeId)
    {
        try {
            return formatterProfile.createFormatter(typeId, typedObject);
        } catch (Exception e) {
            log.error("Can't create formatter for datetime value handler", e); //$NON-NLS-1$
            return DefaultDataFormatter.INSTANCE;
        }
    }

    @NotNull
    protected DBDDataFormatter getFormatter(DBSTypedObject column)
    {
        if (formatter == null) {
            formatter = getFormatter(column, getFormatterId(column));
        }
        return formatter;
    }

    @Override
    public void refreshValueHandlerConfiguration(DBSTypedObject type) {
        this.formatter = null;
    }

    @NotNull
    protected abstract String getFormatterId(DBSTypedObject column);

}