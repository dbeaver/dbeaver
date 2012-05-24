/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

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
    public String formatValue(Object value)
    {
        return value == null ? null : value.toString();
    }

    @Override
    public Object parseValue(String value) throws ParseException
    {
        return DateFormat.getInstance().parse(value);
    }

}