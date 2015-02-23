/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
