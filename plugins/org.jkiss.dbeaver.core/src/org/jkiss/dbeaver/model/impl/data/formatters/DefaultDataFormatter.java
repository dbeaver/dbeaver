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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

public class DefaultDataFormatter implements DBDDataFormatter {

    public static final DBDDataFormatter INSTANCE = new DefaultDataFormatter();

    private  DefaultDataFormatter()
    {
    }

    @Override
    public void init(Locale locale, Map<Object, Object> properties)
    {
    }

    @Override
    public String getPattern()
    {
        return null;
    }

    @Override
    public String formatValue(Object value)
    {
        return value == null ? null : value.toString();
    }

    @Override
    public Object parseValue(String value, Class<?> typeHint) throws ParseException
    {
        return DateFormat.getInstance().parse(value);
    }

}