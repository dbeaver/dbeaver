package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class DateFormatSample implements DBDDataFormatterSample {

    public Map<String, String> getDefaultProperties(Locale locale)
    {
        SimpleDateFormat tmp = (SimpleDateFormat)DateFormat.getDateInstance(DateFormat.LONG, locale);
        String pattern = tmp.toPattern();
        return Collections.singletonMap(DateTimeDataFormatter.PROP_PATTERN, pattern);
    }

    public Object getSampleValue()
    {
        return new Date();
    }

}
