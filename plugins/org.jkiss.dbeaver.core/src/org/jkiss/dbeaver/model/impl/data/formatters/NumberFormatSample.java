/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.data.formatters;

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
