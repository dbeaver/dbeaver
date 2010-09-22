/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

public class NumberDataFormatter implements DBDDataFormatter {

    private NumberFormat numberFormat;

    public void init(Locale locale, Map<String, String> properties)
    {
        numberFormat = NumberFormat.getNumberInstance(locale);
        String propValue = properties.get(NumberFormatSample.PROP_USE_GROUPING);
        if (!CommonUtils.isEmpty(propValue)) {
            numberFormat.setGroupingUsed(Boolean.valueOf(propValue));
        }
        propValue = properties.get(NumberFormatSample.PROP_MAX_INT_DIGITS);
        if (!CommonUtils.isEmpty(propValue)) {
            numberFormat.setMaximumIntegerDigits(Integer.valueOf(propValue));
        }
        propValue = properties.get(NumberFormatSample.PROP_MIN_INT_DIGITS);
        if (!CommonUtils.isEmpty(propValue)) {
            numberFormat.setMinimumIntegerDigits(Integer.valueOf(propValue));
        }
        propValue = properties.get(NumberFormatSample.PROP_MAX_FRACT_DIGITS);
        if (!CommonUtils.isEmpty(propValue)) {
            numberFormat.setMaximumFractionDigits(Integer.valueOf(propValue));
        }
        propValue = properties.get(NumberFormatSample.PROP_MIN_FRACT_DIGITS);
        if (!CommonUtils.isEmpty(propValue)) {
            numberFormat.setMinimumFractionDigits(Integer.valueOf(propValue));
        }
        propValue = properties.get(NumberFormatSample.PROP_ROUNDING_MODE);
        if (!CommonUtils.isEmpty(propValue)) {
            try {
                numberFormat.setRoundingMode(RoundingMode.valueOf(propValue));
            } catch (Exception e) {
                // just skip it
            }
        }
    }

    public String formatValue(Object value)
    {
        if (value == null) {
            return null;
        }
        try {
            return numberFormat.format(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    public Object parseValue(String value) throws ParseException
    {
        return numberFormat.parse(value);
    }

}
