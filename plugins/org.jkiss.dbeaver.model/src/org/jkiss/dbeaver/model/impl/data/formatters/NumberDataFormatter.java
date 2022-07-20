/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
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

    public static final int MAX_DEFAULT_FRACTIONS_DIGITS = 16;

    private static final Log log = Log.getLog(NumberDataFormatter.class);

    private DecimalFormat numberFormat;
    private StringBuffer buffer;
    private FieldPosition position;

    public NumberDataFormatter() {
    }

    @Override
    public void init(DBSTypedObject type, Locale locale, Map<String, Object> properties)
    {
        numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
        Object useGrouping = properties.get(NumberFormatSample.PROP_USE_GROUPING);
        if (useGrouping != null) {
            numberFormat.setGroupingUsed(CommonUtils.toBoolean(useGrouping));
        }
        Object groupingSize = properties.get(NumberFormatSample.PROP_GROUPING_SIZE);
        if (groupingSize != null) {
            numberFormat.setGroupingSize(CommonUtils.toInt(groupingSize, numberFormat.getGroupingSize()));
        }
        Object maxIntDigits = properties.get(NumberFormatSample.PROP_MAX_INT_DIGITS);
        if (maxIntDigits != null) {
            numberFormat.setMaximumIntegerDigits(CommonUtils.toInt(maxIntDigits));
        }
        Object minIntDigits = properties.get(NumberFormatSample.PROP_MIN_INT_DIGITS);
        if (minIntDigits != null) {
            numberFormat.setMinimumIntegerDigits(CommonUtils.toInt(minIntDigits));
        }
        if (type != null && type.getScale() != null) {
            int typeScale = type.getScale();
            // #6111 + #6914.
            // Here is a trick. We can't set max digiter bigger than scale (otherwise long numbers are corrupted)
            Object maxFractDigits = properties.get(NumberFormatSample.PROP_MAX_FRACT_DIGITS);
            if (maxFractDigits != null) {
                int maxFD = CommonUtils.toInt(maxFractDigits);
                if (typeScale > 0 && maxFD > typeScale) {
                    maxFD = typeScale;
                }
                numberFormat.setMaximumFractionDigits(maxFD);
            }
            Object minFractDigits = properties.get(NumberFormatSample.PROP_MIN_FRACT_DIGITS);
            if (minFractDigits != null) {
                numberFormat.setMinimumFractionDigits(CommonUtils.toInt(minFractDigits));
            } else {
                numberFormat.setMinimumFractionDigits(0);
            }
        }
        String roundingMode = CommonUtils.toString(properties.get(NumberFormatSample.PROP_ROUNDING_MODE));
        if (!CommonUtils.isEmpty(roundingMode)) {
            try {
                numberFormat.setRoundingMode(RoundingMode.valueOf(roundingMode));
            } catch (Exception e) {
                // just skip it
            }
        }
        if (type != null && CommonUtils.toBoolean(properties.get(NumberFormatSample.PROP_USE_TYPE_SCALE))) {
            if (type.getScale() != null && type.getScale() > 0) {
                int fractionDigits = type.getScale();
                if (fractionDigits > MAX_DEFAULT_FRACTIONS_DIGITS) fractionDigits = MAX_DEFAULT_FRACTIONS_DIGITS;
                numberFormat.setMinimumFractionDigits(fractionDigits);
            }
        }
        if (type != null && CommonUtils.isBitSet(type.getTypeModifiers(), DBSTypedObject.TYPE_MOD_NUMBER_LEADING_ZEROES)) {
            // Override number format style set from properties in favor of type's precision
            if (type.getPrecision() != null && type.getPrecision() > 0) {
                if (type.getScale() != null && type.getScale() > 0) {
                    numberFormat.setMinimumIntegerDigits(type.getPrecision() - type.getScale());
                    numberFormat.setMinimumFractionDigits(type.getScale());
                } else {
                    numberFormat.setMinimumIntegerDigits(type.getPrecision());
                }

                numberFormat.setGroupingUsed(false);
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
                try {
                    return numberFormat.format(value, buffer, position).toString();
                } catch (ArithmeticException e) {
                    if (numberFormat.getRoundingMode() == RoundingMode.UNNECESSARY) {
                        // This type can't use UNNECESSARY rounding. Let's set default one
                        log.debug("Disabling UNNECESSARY rounding for numbers (" + e.getMessage() + ")");
                        numberFormat.setRoundingMode(RoundingMode.HALF_EVEN);
                    }
                    return numberFormat.format(value, buffer, position).toString();
                }
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
                boolean isFloat = number instanceof Double || number instanceof Float;
                if (typeHint == Byte.class) {
                    if (isFloat) {
                        return number;
                    }
                    return number.byteValue();
                } else if (typeHint == Short.class) {
                    if (isFloat) {
                        return number;
                    }
                    return number.shortValue();
                } else if (typeHint == Integer.class) {
                    if (isFloat) {
                        return number;
                    }
                    return number.intValue();
                } else if (typeHint == Long.class) {
                    if (isFloat) {
                        return number;
                    }
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
