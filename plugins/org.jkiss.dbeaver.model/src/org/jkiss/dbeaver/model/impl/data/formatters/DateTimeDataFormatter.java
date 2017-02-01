/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

public class DateTimeDataFormatter implements DBDDataFormatter {

    public static final String PROP_PATTERN = "pattern";

    private String pattern;
    private DateFormat dateFormat;
    private StringBuffer buffer;
    private FieldPosition position;

    @Override
    public void init(Locale locale, Map<Object, Object> properties)
    {
        pattern = CommonUtils.toString(properties.get(PROP_PATTERN));
        dateFormat = new ExtendedDateFormat(
            pattern,
            locale);
        buffer = new StringBuffer();
        position = new FieldPosition(0);
    }

    @Override
    public String getPattern()
    {
        return pattern;
    }

    @Override
    public String formatValue(Object value)
    {
        synchronized (dateFormat) {
            buffer.setLength(0);
            return value == null ? null : dateFormat.format(value, buffer, position).toString();
        }
    }

    @Override
    public Object parseValue(String value, Class<?> typeHint) throws ParseException
    {
        return dateFormat.parse(value);
    }

}
