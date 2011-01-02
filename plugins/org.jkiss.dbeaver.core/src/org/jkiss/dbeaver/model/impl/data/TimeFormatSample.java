/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class TimeFormatSample implements DBDDataFormatterSample {

    public Map<String, String> getDefaultProperties(Locale locale)
    {
        SimpleDateFormat tmp = (SimpleDateFormat)DateFormat.getTimeInstance(DateFormat.LONG, locale);
        String pattern = tmp.toPattern();
        return Collections.singletonMap(DateTimeDataFormatter.PROP_PATTERN, pattern);
    }

    public Object getSampleValue()
    {
        return new Date();
    }

}
