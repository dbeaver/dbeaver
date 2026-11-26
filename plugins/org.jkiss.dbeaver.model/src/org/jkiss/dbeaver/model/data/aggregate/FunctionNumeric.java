/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.data.aggregate;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jkiss.code.Nullable;

/**
 * FunctionSum
 */
public abstract class FunctionNumeric implements IAggregateFunction {

    @Nullable
    protected static Number getNumeric(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number)) {
            String strValue = value.toString();
            if (strValue == null) {
                return null;
            }
            try {
                value = Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
                // Not a number. Its ok, do not warn  
            }
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        return null;
    }

    @Nullable
    protected static Comparable<?> getComparable(@Nullable Object value, boolean aggregateAsStrings) {
        if (!aggregateAsStrings) {
            Number num = FunctionNumeric.getNumeric(value);
            if (num != null) {
                value = num;
            }
        }
        if (value instanceof Comparable) {
            return (Comparable<?>)value;
        }
        return null;
    }

    @Nullable
    protected static BigDecimal getBigDecimal(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof BigInteger) {
                return new BigDecimal((BigInteger) value);
            }
            if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
                return BigDecimal.valueOf(((Number) value).longValue());
            }

            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    protected static Number getNumericForAggregation(@Nullable Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }

        BigDecimal decimal = getBigDecimal(value);
        if (decimal != null) {
            return decimal;
        }

        // Preserve support for special numeric strings such as NaN and Infinity.
        return getNumeric(value);
    }
}
