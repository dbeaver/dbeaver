package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDDataFormatter;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

public class NumberDataFormatter implements DBDDataFormatter {

    private NumberFormat numberFormat;

    public Map<String, String> getDefaultProperties(Locale locale)
    {
        //NumberFormat tmpFormat = NumberFormat.getInstance(locale);
        return null;
    }

    public void init(Locale locale, Map<String, String> properties)
    {
        numberFormat = NumberFormat.getInstance(locale);
    }

    public Object getSampleValue()
    {
        return 1234567890.012345;
    }

    public String formatValue(Object value)
    {
        return numberFormat.format(value);
    }

    public Object parseValue(String value) throws ParseException
    {
        return numberFormat.parse(value);
    }

}
