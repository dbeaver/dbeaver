/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public void init(Locale locale, Map<Object, Object> properties)
    {
        numberFormat = NumberFormat.getNumberInstance(locale);
        Boolean useGrouping = (Boolean)properties.get(NumberFormatSample.PROP_USE_GROUPING);
        if (useGrouping != null) {
            numberFormat.setGroupingUsed(useGrouping);
        }
        Integer maxIntDigits = (Integer) properties.get(NumberFormatSample.PROP_MAX_INT_DIGITS);
        if (maxIntDigits != null) {
            numberFormat.setMaximumIntegerDigits(maxIntDigits);
        }
        Integer minIntDigits = (Integer) properties.get(NumberFormatSample.PROP_MIN_INT_DIGITS);
        if (minIntDigits != null) {
            numberFormat.setMinimumIntegerDigits(minIntDigits);
        }
        Integer maxFractDigits = (Integer) properties.get(NumberFormatSample.PROP_MAX_FRACT_DIGITS);
        if (maxFractDigits != null) {
            numberFormat.setMaximumFractionDigits(maxFractDigits);
        }
        Integer minFractDigits = (Integer) properties.get(NumberFormatSample.PROP_MIN_FRACT_DIGITS);
        if (minFractDigits != null) {
            numberFormat.setMinimumFractionDigits(minFractDigits);
        }
        String roundingMode = CommonUtils.toString(properties.get(NumberFormatSample.PROP_ROUNDING_MODE));
        if (!CommonUtils.isEmpty(roundingMode)) {
            try {
                numberFormat.setRoundingMode(RoundingMode.valueOf(roundingMode));
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
