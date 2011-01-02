/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

/**
 * Data formatter
 */
public interface DBDDataFormatter {

    void init(Locale locale, Map<String, String> properties);

    String formatValue(Object value);

    Object parseValue(String value) throws ParseException;
    
}
