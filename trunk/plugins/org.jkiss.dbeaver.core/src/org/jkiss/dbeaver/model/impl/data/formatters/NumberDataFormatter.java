/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

public class NumberDataFormatter implements DBDDataFormatter {

    private DecimalFormat numberFormat;
    private StringBuffer buffer;
    private FieldPosition position;

    @Override
    public void init(Locale locale, Map<Object, Object> properties)
    {
        numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
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
        buffer = new StringBuffer();
        position = new FieldPosition(0);
    }

    @Nullable
    @Override
    public String getPattern()
    {
        return null;
    }

    @Nullable
    @Override
    public String formatValue(Object value)
    {
        if (value == null) {
            return null;
        }
        try {
            synchronized (this) {
                buffer.setLength(0);
                return numberFormat.format(value, buffer, position).toString();
            }
        } catch (Exception e) {
            return value.toString();
        }
    }

    @Override
    public Object parseValue(String value, @Nullable Class<?> typeHint) throws ParseException
    {
        synchronized (this) {
            numberFormat.setParseBigDecimal(typeHint == BigDecimal.class || typeHint == BigInteger.class);
            Number number = numberFormat.parse(value);
            if (number != null && typeHint != null) {
                if (typeHint == Byte.class) {
                    return number.byteValue();
                } else if (typeHint == Short.class) {
                    return number.shortValue();
                } else if (typeHint == Integer.class) {
                    return number.intValue();
                } else if (typeHint == Long.class) {
                    return number.longValue();
                } else if (typeHint == Float.class) {
                    return number.floatValue();
                } else if (typeHint == Double.class) {
                    return number.doubleValue();
                }
            }
            return number;
        }
    }

}
