/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.data.type;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.utils.CommonUtils;

public class PostgreIntervalTypeHandler extends PostgreTypeHandler {

    public static final PostgreIntervalTypeHandler INSTANCE = new PostgreIntervalTypeHandler();

    // Intervals are made by combining consecutive units.
    // For example: 'day to second' => DAY | HOUR | MINUTE | SECOND
    private static final int INTERVAL_TYPE_YEAR         = 0x0004_0000;
    private static final int INTERVAL_TYPE_MONTH        = 0x0002_0000;
    private static final int INTERVAL_TYPE_DAY          = 0x0008_0000;
    private static final int INTERVAL_TYPE_HOUR         = 0x0400_0000;
    private static final int INTERVAL_TYPE_MINUTE       = 0x0800_0000;
    private static final int INTERVAL_TYPE_SECOND       = 0x1000_0000;
    private static final int INTERVAL_TYPE_NONE         = 0x7fff_0000;
    private static final int INTERVAL_MASK_TYPE         = 0xffff_0000;
    private static final int INTERVAL_MASK_PRECISION    = 0x0000_ffff;

    private PostgreIntervalTypeHandler() {
        // disallow constructing singleton class
    }

    @Override
    public int getTypeModifiers(@NotNull PostgreDataType type, @NotNull String typeName, @NotNull String[] typmod) throws DBException {
        switch (typmod.length) {
            case 0:
                return getIntervalModifiers(typeName, 0);
            case 1:
                return getIntervalModifiers(typeName, CommonUtils.toInt(typmod[0]));
            default:
                return super.getTypeModifiers(type, typeName, typmod);
        }
    }

    @NotNull
    @Override
    public String getTypeModifiersString(@NotNull PostgreDataType type, int typmod) {
        final StringBuilder sb = new StringBuilder();
        if (typmod > 0) {
            if (type.getName().endsWith(PostgreConstants.TYPE_INTERVAL) && isTypedInterval(typmod)) {
                sb.append(' ').append(getIntervalType(typmod));
            }
            final Integer precision = getTypePrecision(type, typmod);
            if (precision != null && precision > 0) {
                sb.append('(').append(precision).append(')');
            }
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public Integer getTypePrecision(@NotNull PostgreDataType type, int typmod) {
        if (isPreciseInterval(typmod)) {
            return (int) (short) (typmod & INTERVAL_MASK_PRECISION);
        }
        return null;
    }

    @Nullable
    public static String getIntervalType(int typmod) {
        if (typmod < 0) {
            return null;
        }
        switch (typmod & INTERVAL_MASK_TYPE) {
            case INTERVAL_TYPE_YEAR:
                return "year";
            case INTERVAL_TYPE_YEAR | INTERVAL_TYPE_MONTH:
                return "year to month";
            case INTERVAL_TYPE_MONTH:
                return "month";
            case INTERVAL_TYPE_DAY:
                return "day";
            case INTERVAL_TYPE_DAY | INTERVAL_TYPE_HOUR:
                return "day to hour";
            case INTERVAL_TYPE_DAY | INTERVAL_TYPE_HOUR | INTERVAL_TYPE_MINUTE:
                return "day to minute";
            case INTERVAL_TYPE_DAY | INTERVAL_TYPE_HOUR | INTERVAL_TYPE_MINUTE | INTERVAL_TYPE_SECOND:
                return "day to second";
            case INTERVAL_TYPE_HOUR:
                return "hour";
            case INTERVAL_TYPE_HOUR | INTERVAL_TYPE_MINUTE:
                return "hour to minute";
            case INTERVAL_TYPE_HOUR | INTERVAL_TYPE_MINUTE | INTERVAL_TYPE_SECOND:
                return "hour to second";
            case INTERVAL_TYPE_MINUTE:
                return "minute";
            case INTERVAL_TYPE_MINUTE | INTERVAL_TYPE_SECOND:
                return "minute to second";
            case INTERVAL_TYPE_SECOND:
                return "second";
            default:
                throw new IllegalArgumentException("Error obtaining interval type from typmod: " + Integer.toHexString(typmod));
        }
    }

    private static boolean isPreciseInterval(int typmod) {
        // Only intervals with 'second' have precision
        return (typmod & INTERVAL_TYPE_SECOND) > 0;
    }

    private static boolean isTypedInterval(int typmod) {
        // Intervals may be untyped (-1)
        return (typmod & INTERVAL_TYPE_NONE) != INTERVAL_TYPE_NONE;
    }

    private static int getIntervalModifiers(@NotNull String name, int precision) throws DBException {
        if (precision < 0 || precision > 6) {
            throw new DBException("Interval precision " + precision + " must be between 0 and 6");
        }
        int typmod = precision;
        switch (name) {
            case "interval year":
                typmod |= INTERVAL_TYPE_YEAR;
                break;
            case "interval year to month":
                typmod |= INTERVAL_TYPE_YEAR | INTERVAL_TYPE_MONTH;
                break;
            case "interval month":
                typmod |= INTERVAL_TYPE_MONTH;
                break;
            case "interval day":
                typmod |= INTERVAL_TYPE_DAY;
                break;
            case "interval day to hour":
                typmod |= INTERVAL_TYPE_DAY | INTERVAL_TYPE_HOUR;
                break;
            case "interval day to minute":
                typmod |= INTERVAL_TYPE_DAY | INTERVAL_TYPE_HOUR | INTERVAL_TYPE_MINUTE;
                break;
            case "interval day to second":
                typmod |= INTERVAL_TYPE_DAY | INTERVAL_TYPE_HOUR | INTERVAL_TYPE_MINUTE | INTERVAL_TYPE_SECOND;
                break;
            case "interval hour":
                typmod |= INTERVAL_TYPE_HOUR;
                break;
            case "interval hour to minute":
                typmod |= INTERVAL_TYPE_HOUR | INTERVAL_TYPE_MINUTE;
                break;
            case "interval hour to second":
                typmod |= INTERVAL_TYPE_HOUR | INTERVAL_TYPE_MINUTE | INTERVAL_TYPE_SECOND;
                break;
            case "interval minute":
                typmod |= INTERVAL_TYPE_MINUTE;
                break;
            case "interval minute to second":
                typmod |= INTERVAL_TYPE_MINUTE | INTERVAL_TYPE_SECOND;
                break;
            case "interval second":
                typmod |= INTERVAL_TYPE_SECOND;
                break;
            case "interval":
                typmod |= INTERVAL_TYPE_NONE;
                break;
            default:
                throw new DBException("Unsupported interval type: '" + name + "'");
        }
        if (!isPreciseInterval(typmod) && precision != 0) {
            throw new DBException("Interval '" + name + "' may not have precision");
        }
        return typmod;
    }
}
