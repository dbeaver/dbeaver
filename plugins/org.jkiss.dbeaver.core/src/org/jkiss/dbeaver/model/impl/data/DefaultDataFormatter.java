/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDDataFormatter;

import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

public class DefaultDataFormatter implements DBDDataFormatter {

    public static final DBDDataFormatter INSTANCE = new DefaultDataFormatter();

    private  DefaultDataFormatter()
    {
    }

    public void init(Locale locale, Map<String, String> properties)
    {
    }

    public Object getSampleValue()
    {
        return "";
    }

    public String formatValue(Object value)
    {
        return value == null ? null : value.toString();
    }

    public Object parseValue(String value) throws ParseException
    {
        return null;
    }

}