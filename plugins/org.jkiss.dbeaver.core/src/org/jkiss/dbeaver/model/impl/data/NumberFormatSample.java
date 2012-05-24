/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NumberFormatSample implements DBDDataFormatterSample {

    public static final String PROP_USE_GROUPING ="useGrouping";
    public static final String PROP_MAX_INT_DIGITS ="maxIntegerDigits";
    public static final String PROP_MIN_INT_DIGITS ="minIntegerDigits";
    public static final String PROP_MAX_FRACT_DIGITS ="maxFractionDigits";
    public static final String PROP_MIN_FRACT_DIGITS  ="minFractionDigits";
    public static final String PROP_ROUNDING_MODE ="roundingMode";

    @Override
    public Map<Object, Object> getDefaultProperties(Locale locale)
    {
        NumberFormat tmp = NumberFormat.getNumberInstance(locale);
        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put(PROP_USE_GROUPING, tmp.isGroupingUsed());
        props.put(PROP_MAX_INT_DIGITS, tmp.getMaximumIntegerDigits());
        props.put(PROP_MIN_INT_DIGITS, tmp.getMinimumIntegerDigits());
        props.put(PROP_MAX_FRACT_DIGITS, tmp.getMaximumFractionDigits());
        props.put(PROP_MIN_FRACT_DIGITS, tmp.getMinimumFractionDigits());
        props.put(PROP_ROUNDING_MODE, tmp.getRoundingMode().name());
        return props;
    }

    @Override
    public Object getSampleValue()
    {
        return 1234567890.012345;
    }

}
