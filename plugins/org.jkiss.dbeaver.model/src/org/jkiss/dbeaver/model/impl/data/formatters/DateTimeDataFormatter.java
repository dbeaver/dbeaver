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
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Map;

public class DateTimeDataFormatter implements DBDDataFormatter {

    public static final String PROP_PATTERN = "pattern";

    private String pattern;
    private DateFormat dateFormat;
    private StringBuffer buffer;
    private FieldPosition position;
    private DateTimeFormatter dateTimeFormatter;

    @Override
    public void init(DBSTypedObject type, Locale locale, Map<Object, Object> properties)
    {
        pattern = CommonUtils.toString(properties.get(PROP_PATTERN));
        dateFormat = new ExtendedDateFormat(
            pattern,
            locale);
        // We shouldn't use lanient formatter (#7244)
        dateFormat.setLenient(false);
        buffer = new StringBuffer();
        position = new FieldPosition(0);
        // DateTimeFormatter pattern for nanoseconds is "n" but old "f" (ExtendedDateFormat)
        String java8DatePattern = pattern.replaceAll("f+", "n");
        dateTimeFormatter = DateTimeFormatter.ofPattern(java8DatePattern);
    }

    @Override
    public String getPattern()
    {
        return pattern;
    }

    @Override
    public String formatValue(Object value)
    {
        if (value instanceof TemporalAccessor) {
            return dateTimeFormatter.format((TemporalAccessor) value);
        }
        synchronized (dateFormat) {
            buffer.setLength(0);
            return value == null ? null : dateFormat.format(value, buffer, position).toString();
        }
    }

    @Override
    public Object parseValue(String value, Class<?> typeHint) throws ParseException
    {
        if (typeHint != null && TemporalAccessor.class.isAssignableFrom(typeHint)) {
            try {
                return LocalDateTime.parse(value, dateTimeFormatter);
            } catch (DateTimeParseException e) {
                throw new ParseException(e.getParsedString(), e.getErrorIndex());
            }
        }
        return dateFormat.parse(value);
    }

}
